# V Store

Native Android app catalogue and APK library with a graphite-and-gold Material-inspired interface.

## Build APK

GitHub Actions builds the Android APK on pushes to `main` and via manual workflow dispatch. Download the APK artifact from a successful run in the Actions page.

## Local APK library
- Import APK files through Android's system file picker.
- Keep imported APKs privately on the current device.
- Search local files, calculate SHA-256, install, or remove them.

## Publish to everyone
- Open **Мои приложения** and tap **Опубликовать для всех** on an imported APK.
- Enter its public catalogue name, version, description, category, and the publisher token.
- The app uploads the APK to the V Store server; after a successful response, it is listed in the shared catalogue and can be downloaded by other users.
- The publisher token is stored only in the app's private preferences on that device. Do not share it or embed it in APK source/build configuration.
- Configure `VSTORE_PUBLISH_TOKEN` as a secret environment variable for the Render service. If it is missing, publishing returns HTTP 503; a mismatched token returns HTTP 401.
- Published APKs are limited to 200 MB. The server records the file's SHA-256 hash and returns it in the public catalogue.

## Server
The Flask service in `server/app.py` exposes `GET /api/apps`, `POST /api/apps`, and `GET /api/apps/<id>/download`. Render's persistent disk is mounted at `/var/data` for APK files and SQLite metadata. Existing catalogues are migrated to include app categories on startup.

SHA-256 helps verify file integrity but is not, by itself, proof that an APK is trustworthy. Publish only APKs you have the right to distribute.
