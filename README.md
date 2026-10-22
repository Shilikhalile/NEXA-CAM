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

The viewfinder now has a lightweight live overlay picker: Original, Warm, Cool, Cinema, Nexa Red, Fade, and Noir Tint. The effect updates immediately without adding heavy frame-by-frame processing, so low-end phones remain responsive. The selected effect is now applied by CameraX to Preview, ImageCapture, and VideoCapture, so the saved photo/video contains the selected filter.

## Gallery access

An `OPEN GALLERY` button now opens the device media gallery for both images and videos. If the device has no gallery handler, the app falls back to the Android media picker. Captures continue to save into `Pictures/NEXA CAM` and `Movies/NEXA CAM` through MediaStore.

## Photo Pro manual exposure

Photo Pro now exposes manual shutter speed and ISO controls. Shutter choices range from 1/30 to 1/1000 and ISO from 100 to 3200. Camera2Interop sends `CONTROL_AE_MODE_OFF`, `SENSOR_EXPOSURE_TIME`, and `SENSOR_SENSITIVITY` to the CameraX capture pipeline, while Auto mode keeps normal AE/EV behavior. Devices that reject manual requests safely fall back to the regular camera path.

## Manual white balance

Photo Pro now includes Camera2 manual white-balance presets: Auto, Daylight, Cloudy, Fluorescent, Tungsten, and Shade. The selected mode is applied alongside manual shutter speed and ISO through the CameraX capture request pipeline.

## Smart computational photo modes

The mode picker now includes Photo Smart HDR and Photo Night. These modes use CameraX Extensions to request the phone manufacturer's HDR or Night processing when supported by the camera. Unsupported devices fall back to Photo Auto without crashing. Photo Pro remains available for manual shutter, ISO, and white balance control.

## Portrait photo mode

Photo Portrait uses the CameraX Bokeh extension when the device exposes a supported portrait/depth implementation. Unsupported cameras fall back to Photo Auto without crashing.

## Portrait blur intensity

Photo Portrait now shows a `BLUR` slider from 0% to 100%. It uses CameraX `CameraExtensionsControl.setExtensionStrength()` so supported OEM Bokeh implementations adjust the actual background blur in the captured photo. On devices without extension-strength support, the standard Portrait result remains available and the app stays stable.

## Manual focus

Photo Pro now includes a focus-distance slider from infinity to the lens minimum focus distance. Camera2Interop applies `CONTROL_AF_MODE_OFF` and `LENS_FOCUS_DISTANCE` when the selected camera reports manual-focus support; fixed-focus cameras keep normal autofocus behavior.

## Face Retouch mode

Photo Face Retouch uses the CameraX `FACE_RETOUCH` extension when supported by the OEM camera. Its 0-100% strength control adjusts the amount of cosmetic enhancement and skin smoothing. Unsupported devices fall back safely.

## Expanded powerful photo modes

The mode picker now includes Photo Smart Scene (`AUTO` extension), Photo HDR Boost, Photo Night Boost, Photo Portrait, Photo Face Retouch, and Photo Pro. HDR and Night expose a real extension-strength slider because CameraX uses that value to control fusion and final brightness; Portrait controls blur and Face Retouch controls cosmetic enhancement. Unsupported OEM modes fall back safely.

## Filters and cinematic effects

The filter picker now includes Vivid, Golden Hour, Teal & Orange, Matte, Vintage, Cyberpunk, Cinematic, and Noir in addition to the original presets. Effects are rendered through the shared overlay renderer so the selected look appears on the live preview and is composited into saved photos and videos. Cinematic adds a teal/blue shadow grade, warm highlight tint, vignette-like darkening, and letterbox bars.

## Solar Horror effect

Solar Horror adds a dramatic red-orange solar glow, radial rays, dark vignette, and cinematic bars for a frightening sunlit look. It is a creative overlay only, not a solar-viewing safety filter; users should never point the phone at the sun for prolonged periods or look directly at the sun.

## Film Stock filter

Film Stock adds a scanned-film look with lifted blacks, warm highlights, cooler shadows, halation, vignette, deterministic fine grain, and letterbox bars. It is rendered by the same shared effect path as the other filters, so it appears in preview and is composited into saved photos and videos.

## Dark night filters

Cyberpunk Night and Neon Night are optimized for dark scenes: they deepen navy shadows, emphasize cyan and magenta light sources, add colored bloom/glow, and apply a controlled vignette. Cyberpunk Night also adds subtle scan lines; Neon Night keeps a cleaner neon-cinema look. Both effects are rendered in preview and composited into saved photos and videos.
