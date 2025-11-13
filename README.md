# Flam Edge Viewer

Android + OpenCV (C++) + OpenGL ES + TypeScript web viewer.

## Structure

- `android-app/`
  - Android app with CameraX preview, GLES 2.0 renderer, JNI bridge
  - Native C++ library using OpenCV for Canny edge detection
- `web/`
  - Minimal TS viewer that displays a sample processed frame and FPS/resolution

## Requirements

- Android Studio (Giraffe+), SDK 34
- NDK, CMake
- OpenCV for Android (C++), set `OpenCV_DIR` in CMake cache or environment
- Node.js 18+

## Android setup

1. Open `android-app` in Android Studio.
2. Ensure NDK and CMake are installed (SDK Manager).
3. Provide OpenCV C++ SDK path so `find_package(OpenCV REQUIRED)` succeeds. For example:
   - Add environment variable `OpenCV_DIR` pointing to `OpenCV-android-sdk/sdk/native/jni` (or the CMake package suffix as applicable).
4. Sync Gradle and build.
5. Run on a device. Grant camera permission.

Notes:
- Java OpenCV dependency is declared, but C++ uses the native SDK via CMake.
- The renderer currently clears the screen; texture upload and rendering should be added next.

## Web viewer

```bash
cd web
npm install
npm run build
npm run start
```
Open `http://localhost:5174`.

## Next steps (suggested)
- Wire `ImageAnalysis` in CameraX to feed frames to JNI and receive processed frames.
- Upload processed frames as textures to the GL renderer.
- Add a toggle between raw and processed output.
- Export a processed frame (PNG/base64) to share with the web viewer.
