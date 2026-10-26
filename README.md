# V Store

Standalone native Android APK library with a plum/graphite UI and lavender accents.

## Build APK

GitHub Actions runs `V Store Android Build` on pushes to `main` and can also be started manually from the Actions tab. Download the `v-store-debug-apk` artifact from a successful run.

## Features
- Import APK files using Android's system file picker.
- Keep imported APKs in the app's private storage.
- Search the local library.
- Calculate SHA-256 fingerprints.
- Start Android's package installer.
- Remove imported apps from the local library.

## Current scope
This version is a local APK library, not yet a public marketplace. Remote publishing, user accounts, a server catalogue, and update distribution are not implemented. SHA-256 is an integrity fingerprint, not a trust/signature guarantee.