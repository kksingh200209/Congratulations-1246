# App1 — Android Document & Image Toolkit

Android-only, offline-first utility app. Selected files are processed locally; this MVP has no login, backend, analytics, or network permission.

## Implemented
- Image compressor (JPEG quality 65)
- Single image to PDF
- Save outputs to Pictures/App1 and Documents/App1
- Jetpack Compose home screen

PDF merge/split, PDF compression and OCR are planned, not implemented yet.

## Build
Open in Android Studio with JDK 17 and Android SDK 35, allow Gradle sync, then run the app on an Android 8.0+ device. Build and device verification are pending. This repository contains source code, not a tested APK.

## Privacy
Uses Android's system photo picker. Selected files are processed on-device and are not uploaded.
