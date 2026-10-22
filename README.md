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

## Adaptive camera profile

NEXA CAM now detects low-RAM devices and automatically uses a lighter profile: SD video quality and lower JPEG processing cost to reduce lag, heat, and memory pressure. Devices with more memory use HD video and higher JPEG quality. Flash controls are disabled when the active camera has no flash unit.

## 4K video

Video quality now defaults to `4K AUTO` on devices that are not detected as low-RAM. CameraX requests UHD and automatically falls back to the nearest supported quality when the active camera cannot record 4K. The button can switch between `4K AUTO` and `HD AUTO`; low-RAM devices keep the lighter SD profile for stability.

## Exposure control

The camera now exposes a real EV compensation slider. It maps to the active camera's supported compensation range and disables itself with `EV N/A` when the hardware does not support it. Stronger devices use CameraX maximum-quality still capture while low-RAM devices keep minimize-latency capture.

## Focus lock

Tap the viewfinder to refocus. Hold for 550 ms to lock focus and metering at that point; tap again to unlock and refocus. The status changes to `AF LOCK` while the lock is active.

## Electronic image stabilization

Video mode now requests CameraX electronic video stabilization. When the device/camera combination rejects stabilization, NEXA CAM automatically rebinds without EIS instead of crashing and does not display `EIS ON`.

## Camera modes

The mode picker now provides five functional modes: Photo Auto, Photo Pro, Video HD + EIS, Video 4K + EIS, and Video Low Power. Each mode changes capture quality and camera behavior; unsupported 4K or EIS still uses the safe fallback path.

## Live filters and color effects

The viewfinder now has a lightweight live overlay picker: Original, Warm, Cool, Cinema, Nexa Red, Fade, and Noir Tint. The effect updates immediately without adding heavy frame-by-frame processing, so low-end phones remain responsive. In this phase the saved photo/video stays the original camera capture; baked-in export filters are intentionally reserved for a later processing phase.

## Gallery access

An `OPEN GALLERY` button now opens the device media gallery for both images and videos. If the device has no gallery handler, the app falls back to the Android media picker. Captures continue to save into `Pictures/NEXA CAM` and `Movies/NEXA CAM` through MediaStore.
