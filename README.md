# YouTube Music App for Android

An open-source Android wrapper client for the YouTube Music web player, designed to provide a premium listening experience with full background playback support, system media session integration, and custom features like a sleep timer.

## Features

- **Background Audio Playback**: Keep listening to your music even when the app is minimized, backgrounded, or the screen is off.
- **System Media Controls**: Seamless integration with Android's Media3 library, enabling lock screen controls, notification controls, and playback metadata tracking.
- **Sleep Timer**: A built-in sleep timer dialog that automatically stops playback after a user-defined duration.
- **Material 3 Interface**: Modern, responsive Jetpack Compose-based UI matching Android's latest design guidelines.
- **CI/CD Pipeline**: GitHub Actions workflow that automatically builds, optionally signs, and creates GitHub releases for APKs on pushes to the `master` branch.

## Tech Stack

- **UI Framework**: Jetpack Compose (Material 3)
- **Media Playback**: AndroidX Media3 Session & Service
- **Language**: Kotlin with Kotlin Coroutines
- **Build System**: Gradle Kotlin DSL (`.gradle.kts`)

## Building Locally

### Prerequisites
- JDK 17 (or newer)
- Android SDK 37 (configured in Gradle)

### Command Line
To build the unsigned release APK:
```bash
./gradlew assembleRelease
```
The outputs will be generated in `app/build/outputs/apk/release/`.

## CI/CD Deployment

The repository includes a GitHub Actions workflow that automates the release pipeline on every push to the `master` branch.

### Setting Up Secrets (Optional for Signing)
To automatically sign the release APK, configure the following secrets in your GitHub Repository settings (**Settings > Secrets and variables > Actions**):
- `KEYSTORE`: The base64-encoded representation of your release `.jks` file (e.g., `base64 -w 0 release.jks`).
- `KEY_ALIAS`: The key alias defined in your keystore.
- `KEYSTORE_PASSWORD`: The password for the keystore.
- `KEY_PASSWORD`: The password for your key alias.

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.
