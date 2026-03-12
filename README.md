# Prodoc Scanner

Prodoc Scanner is an Android document scanning application that allows users to capture, process, and manage document scans. It supports automatic edge detection, image enhancement filters, OCR (Optical Character Recognition), and PDF export.

## Features

- **Document Scanning** – Capture documents using the device camera with automatic edge/corner detection powered by OpenCV.
- **Image Cropping & Editing** – Manually adjust crop points and apply perspective correction after capture.
- **Image Filters / Effects** – Apply various enhancement filters (e.g., grayscale, black & white, color) to scanned pages.
- **PDF Export** – Combine one or more scanned pages into a PDF file using PDFBox for Android.
- **OCR (Text Recognition)** – Extract text from scanned images using Tesseract4Android.
- **Gallery Import** – Import existing images from the device gallery in addition to live camera capture.
- **In-App Update** – Supports Google Play in-app update prompts to keep the app current.
- **Firebase Integration** – Analytics and crash reporting via Firebase Analytics and Crashlytics.

## Screenshots

> _Add screenshots here._

## Tech Stack

| Component | Library / Version |
|---|---|
| Language | Java |
| Min SDK | 23 (Android 6.0) |
| Target / Compile SDK | 35 |
| Build Tools | Android Gradle Plugin 8.7.3, Gradle 8.9 |
| Computer Vision | OpenCV 4.3.0 (native, included in `/sdk`) |
| Camera | CameraX 1.3.4 |
| OCR | Tesseract4Android 4.7.0 |
| PDF | pdfbox-android 2.0.27.0 |
| Image Loading | Glide 4.16.0 |
| Firebase | firebase-bom 32.7.0 (Analytics + Crashlytics) |
| In-App Update | android-inapp-update 1.0.5 |

## Project Structure

```
Prodoc-Scanner/
├── app/                        # Application module
│   └── src/main/
│       ├── java/com/aaindia/prodocscanner/
│       │   ├── activity/       # Core activities (Main, Camera, Scan Preview, Crop, …)
│       │   ├── activityExtenders/  # Feature-specific activity extensions
│       │   ├── ocr/            # OCR activity and helpers
│       │   ├── views/          # Custom views (PolygonView, TouchImageView, …)
│       │   ├── wrappers/       # Data/model wrappers
│       │   └── constants/      # App-wide constants
│       ├── cpp/                # JNI / native code (CMakeLists.txt + native-lib.cpp)
│       ├── res/                # Android resources (layouts, drawables, strings, …)
│       └── AndroidManifest.xml
├── sdk/                        # OpenCV 4.3.0 Android SDK (native + Java)
├── build.gradle                # Root build file
├── settings.gradle
└── gradle.properties
```

## Getting Started

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or newer
- Android NDK **21.3.6528147** (set in `app/build.gradle`)
- CMake **3.22.1** (install via SDK Manager → SDK Tools)

### Clone & Open

```bash
git clone https://github.com/noman-barb/Prodoc-Scanner.git
```

Open the cloned directory in Android Studio. The IDE will sync Gradle automatically.

### Build

```bash
# Debug build
./gradlew assembleDebug

# Release build (requires a signed keystore – see Signing section below)
./gradlew assembleRelease
```

APKs are generated per ABI (`x86`, `x86_64`, `armeabi-v7a`, `arm64-v8a`).

### Signing (Release)

Place your keystore file in `app/release/` and configure the signing credentials in `app/build.gradle` (or via environment variables / `keystore.properties`) before running `assembleRelease`.

### Firebase Setup

The project uses Firebase services. Replace `app/google-services.json` with your own project's `google-services.json` file obtained from the [Firebase Console](https://console.firebase.google.com/).

## Permissions

The app declares the following permissions in `AndroidManifest.xml`:

- `CAMERA` – required for document scanning
- `READ_EXTERNAL_STORAGE` / `WRITE_EXTERNAL_STORAGE` – required for saving scans and importing images
- `INTERNET` – required for Firebase and in-app updates
- `REQUEST_INSTALL_PACKAGES` – required for in-app update flow

## Licenses

- OpenCV is licensed under the **Apache 2.0** License. See `app/src/main/assets/License/` for the full text.
- Tesseract is licensed under the **Apache 2.0** License.
- All other third-party libraries are subject to their respective licenses.

## Contributing

1. Fork the repository.
2. Create a feature branch: `git checkout -b feature/my-feature`
3. Commit your changes: `git commit -m "Add my feature"`
4. Push to the branch: `git push origin feature/my-feature`
5. Open a Pull Request.

## Version

Current version: **3.2.0** (version code 44)
