# NEXA CAM

Native Android camera application with a cinematic black/red interface.

## Implemented

- CameraX preview with lifecycle-safe binding
- Photo capture to `Pictures/NEXA CAM`
- Video recording with microphone audio to `Movies/NEXA CAM`
- Front/rear camera switching with capability detection
- Photo flash and video torch controls
- Tap-to-focus and auto-cancel focus metering
- Smooth zoom slider based on the active camera's actual zoom range
- Rule-of-thirds composition grid toggle
- Recording timer and clear save/error feedback
- Gradle Wrapper and GitHub Actions debug APK artifact

## Build locally

```bash
./gradlew assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

## Requirements

Android Studio with Android SDK 35, JDK 17, and a device/emulator with a camera. The app requests camera and microphone permissions at runtime because audio video recording is supported.

## Package

`com.nexa.cam`
