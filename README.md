# Flam Edge Viewer

Android + OpenCV (C++) + OpenGL ES + TypeScript web viewer.

## Structure

- `android-app/`
  - CameraX preview + analysis pipeline (raw preview and processed GL path)
  - JNI bridge to native OpenCV Canny edge detector
  - OpenGL ES 2.0 renderer that draws processed textures
  - Save-to-gallery + Base64 export for sharing frames with the web viewer
- `web/`
  - TypeScript viewer to preview processed frames from a base64 PNG string
  - FPS tracker and resolution readout

## Requirements

- Android Studio (Giraffe+), SDK 34
- NDK, CMake
- OpenCV for Android (C++), set `OpenCV_DIR` in CMake cache or environment
- Node.js 18+

## Android setup

1. Open `android-app` in Android Studio.
2. Ensure NDK and CMake are installed (SDK Manager).
3. Provide OpenCV C++ SDK path so `find_package(OpenCV REQUIRED)` resolves. For example add environment variable `OpenCV_DIR` pointing to `OpenCV-android-sdk/sdk/native/jni`.
4. Sync Gradle and build.
5. Deploy to a physical device, grant camera permission at runtime.

### App usage

- The floating toggle button switches between raw preview (CameraX) and processed edge view (OpenGL).
- When in processed mode, tap the save button to:
  - Store a PNG under `Pictures/FlamEdgeViewer/edge_YYYYMMDD_HHMMSS.png` (scoped storage aware).
  - Write the same frame as base64 to `files/edge_frame_base64.txt` (shown in the toast). Copy this string for the web viewer.
- On-screen overlay shows FPS, resolution, and current mode.

## Web viewer

```bash
cd web
npm install
npm run build
npm run start
```

Open `http://localhost:5174` and:
- Paste the PNG base64 from `edge_frame_base64.txt` into the textarea.
- Click **Load Frame** to view the processed image with stats.
- **Use Sample** loads a bundled demo edge image if you do not have device output yet.

## Notes

- `YuvUtils.toBitmap` uses NV21 → JPEG → Bitmap for clarity; replace with a direct YUV → RGBA conversion for higher throughput.
- Bundle OpenCV native libs (or use Android packaging options) when preparing a release build.
- GLSL shaders can be extended for additional visual effects (invert, grayscale, etc.).
- To expose frames live to the web tool, add an HTTP or WebSocket bridge that streams base64 from the Android layer.
