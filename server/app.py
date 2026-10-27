import hashlib
import os
import sqlite3
import uuid
import hmac
import time
from pathlib import Path
from flask import Flask, jsonify, request, send_from_directory, abort

app = Flask(__name__)
DATA = Path(os.environ.get("VSTORE_DATA_DIR", "/var/data"))
UPLOADS = DATA / "uploads"
DB = DATA / "vstore.db"
UPLOADS.mkdir(parents=True, exist_ok=True)
app.config["MAX_CONTENT_LENGTH"] = 200 * 1024 * 1024

def connect():
    con = sqlite3.connect(DB)
    con.row_factory = sqlite3.Row
    return con

def init_db():
    with connect() as con:
        con.execute("""CREATE TABLE IF NOT EXISTS app_reviews (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            app_id TEXT NOT NULL,
            install_hash TEXT NOT NULL,
            rating INTEGER NOT NULL CHECK(rating BETWEEN 1 AND 5),
            comment TEXT NOT NULL DEFAULT '',
            created_at INTEGER NOT NULL,
            updated_at INTEGER NOT NULL,
            UNIQUE(app_id, install_hash)
        )""")
        con.execute("CREATE INDEX IF NOT EXISTS app_reviews_app_date_idx ON app_reviews(app_id, created_at DESC)")
        con.execute("""CREATE TABLE IF NOT EXISTS apps (
            id TEXT PRIMARY KEY, name TEXT NOT NULL, package_name TEXT NOT NULL DEFAULT '',
            version_name TEXT NOT NULL DEFAULT '', size INTEGER NOT NULL, sha256 TEXT NOT NULL,
            filename TEXT NOT NULL, description TEXT NOT NULL DEFAULT '',
            created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, downloads INTEGER NOT NULL DEFAULT 0
        )""")
init_db()

@app.get("/health")
def health():
    return jsonify({"ok": True, "service": "V Store catalog", "reviews": True})

def review_install_hash():
    install_id = request.headers.get("X-VStore-Install", "").strip()
    if not (16 <= len(install_id) <= 128) or not all(c.isalnum() or c in "_-" for c in install_id):
        return None
    salt = os.environ.get("REVIEW_HASH_SALT", "vstore-review-v1")
    return hashlib.sha256((salt + ":" + install_id).encode()).hexdigest()

@app.get("/reviews")
def list_reviews():
    app_id = request.args.get("app_id", "").strip()
    if not app_id or len(app_id) > 100 or not all(c.isalnum() or c in "_.-" for c in app_id):
        return jsonify({"error": "Некорректный app_id"}), 400
    limit = min(max(request.args.get("limit", default=100, type=int), 1), 100)
    offset = max(request.args.get("offset", default=0, type=int), 0)
    with connect() as con:
        stats = con.execute("SELECT COUNT(*) AS count, COALESCE(AVG(rating),0) AS average FROM app_reviews WHERE app_id=?", (app_id,)).fetchone()
        rows = con.execute("SELECT rating,comment,created_at FROM app_reviews WHERE app_id=? ORDER BY created_at DESC LIMIT ? OFFSET ?", (app_id,limit,offset)).fetchall()
    return jsonify({"app_id": app_id, "count": stats["count"], "average": round(stats["average"],2),
                    "reviews": [{"rating": row["rating"], "comment": row["comment"], "time": row["created_at"]*1000} for row in rows]})

@app.post("/reviews")
def submit_review():
    owner = review_install_hash()
    if not owner:
        return jsonify({"error": "Не удалось определить установку приложения"}), 400
    body = request.get_json(silent=True) or {}
    app_id = str(body.get("app_id", "")).strip()
    try:
        rating = int(body.get("rating", 0))
    except (TypeError, ValueError):
        rating = 0
    comment = str(body.get("comment", "")).strip()
    if not app_id or len(app_id) > 100 or not all(c.isalnum() or c in "_.-" for c in app_id):
        return jsonify({"error": "Некорректный app_id"}), 400
    if rating < 1 or rating > 5:
        return jsonify({"error": "Оценка должна быть от 1 до 5"}), 400
    if len(comment) > 1000:
        return jsonify({"error": "Отзыв слишком длинный (максимум 1000 символов)"}), 400
    now = int(time.time())
    with connect() as con:
        con.execute("""INSERT INTO app_reviews(app_id,install_hash,rating,comment,created_at,updated_at)
                    VALUES(?,?,?,?,?,?) ON CONFLICT(app_id,install_hash) DO UPDATE SET
                    rating=excluded.rating, comment=excluded.comment, updated_at=excluded.updated_at""",
                    (app_id,owner,rating,comment,now,now))
        con.commit()
    return jsonify({"status": "saved"}), 200

@app.get("/api/apps")
def list_apps():
    q = request.args.get("q", "").strip()
    with connect() as con:
        if q:
            rows = con.execute("SELECT * FROM apps WHERE name LIKE ? OR package_name LIKE ? OR description LIKE ? ORDER BY created_at DESC",
                               (f"%{q}%", f"%{q}%", f"%{q}%")).fetchall()
        else:
            rows = con.execute("SELECT * FROM apps ORDER BY created_at DESC").fetchall()
    return jsonify([dict(r, download_url=f"/api/apps/{r['id']}/download") for r in rows])

@app.post("/api/apps")
def publish_app():
    expected_token = os.environ.get("VSTORE_PUBLISH_TOKEN", "").strip()
    supplied_token = request.headers.get("X-VSTORE-PUBLISH-TOKEN", "").strip()
    if not expected_token:
        return jsonify({"error": "Публикация отключена: VSTORE_PUBLISH_TOKEN не настроен"}), 503
    if not supplied_token or not hmac.compare_digest(expected_token, supplied_token):
        return jsonify({"error": "Требуется авторизация автора"}), 401
    uploaded = request.files.get("apk")
    name = request.form.get("name", "").strip()
    description = request.form.get("description", "").strip()[:1000]
    package_name = request.form.get("package_name", "").strip()
    version_name = request.form.get("version_name", "").strip()
    if not uploaded or not uploaded.filename.lower().endswith(".apk"):
        return jsonify({"error": "Выберите APK-файл"}), 400
    if not name:
        name = Path(uploaded.filename).stem[:120]
    app_id = uuid.uuid4().hex
    filename = f"{app_id}.apk"
    path = UPLOADS / filename
    digest = hashlib.sha256()
    size = 0
    with path.open("wb") as out:
        while True:
            chunk = uploaded.stream.read(1024 * 1024)
            if not chunk:
                break
            size += len(chunk)
            if size > 200 * 1024 * 1024:
                path.unlink(missing_ok=True)
                return jsonify({"error": "APK превышает лимит 200 МБ"}), 413
            digest.update(chunk)
            out.write(chunk)
    try:
        with path.open("rb") as check:
            if check.read(4) != b"PK\\x03\\x04":
                path.unlink(missing_ok=True)
                return jsonify({"error": "Файл не похож на APK/ZIP"}), 400
        with connect() as con:
            con.execute("INSERT INTO apps(id,name,package_name,version_name,size,sha256,filename,description) VALUES(?,?,?,?,?,?,?,?)",
                        (app_id, name, package_name, version_name, size, digest.hexdigest(), filename, description))
    except Exception:
        path.unlink(missing_ok=True)
        raise
    return jsonify({"id": app_id, "name": name, "sha256": digest.hexdigest()}), 201

@app.get("/api/apps/<app_id>/download")
def download_app(app_id):
    with connect() as con:
        row = con.execute("SELECT filename FROM apps WHERE id=?", (app_id,)).fetchone()
        if not row:
            abort(404)
        con.execute("UPDATE apps SET downloads=downloads+1 WHERE id=?", (app_id,))
    return send_from_directory(UPLOADS, row["filename"], as_attachment=True, download_name=f"{app_id}.apk")

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=int(os.environ.get("PORT", "10000")))
