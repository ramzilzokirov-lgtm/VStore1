import hashlib
import os
import sqlite3
import uuid
import hmac
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
        con.execute("""CREATE TABLE IF NOT EXISTS apps (
            id TEXT PRIMARY KEY, name TEXT NOT NULL, package_name TEXT NOT NULL DEFAULT '',
            version_name TEXT NOT NULL DEFAULT '', size INTEGER NOT NULL, sha256 TEXT NOT NULL,
            filename TEXT NOT NULL, description TEXT NOT NULL DEFAULT '',
            created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, downloads INTEGER NOT NULL DEFAULT 0
        )""")
init_db()

@app.get("/health")
def health():
    return jsonify({"ok": True, "service": "V Store catalog"})

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
