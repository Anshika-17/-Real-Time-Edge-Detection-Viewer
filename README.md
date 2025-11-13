# Flam Edge Viewer

Android + OpenCV (C++) + OpenGL ES + TypeScript web viewer.

## Structure

- `android-app/`
  - CameraX preview + analysis pipeline (raw preview and processed GL path)
  - JNI bridge accepting YUV_420_888 planes and producing RGBA edge frames in native code
  - OpenGL ES 2.0 renderer that uploads RGBA buffers without intermediate bitmaps
  - Save-to-gallery + Base64 export for sharing frames with the web viewer
- `web/`
  - TypeScript viewer to preview processed frames from a base64 PNG string
  - FPS tracker and resolution readout

## Requirements

- Android Studio (Giraffe+), SDK 34
- NDK, CMake
- OpenCV for Android (C++). Make sure the native SDK is available to CMake:
  - Add environment variable `OpenCV_DIR` that points to `OpenCV-android-sdk/sdk/native/jni` **or**
  - Edit `app/src/main/cpp/CMakeLists.txt` to point to your unpacked OpenCV SDK path
- Copy OpenCV native binaries (`sdk/native/libs/<abi>/libopencv_java4.so` and friends) into `android-app/app/src/main/jniLibs/<abi>/` or package them via Gradle so they are available at runtime.
- Node.js 18+

## Android setup

1. Open `android-app` in Android Studio.
2. Install NDK + CMake from SDK Manager.
3. Provide OpenCV native SDK (`OpenCV_DIR`) as described above.
4. Sync Gradle and build.
5. Deploy to a physical device (camera required) and grant permission.

### Runtime behaviour

- Floating buttons (bottom-right):
  - **Gallery icon** toggles Raw (CameraX preview) vs Processed (OpenCV + OpenGL) modes.
  - **Save icon** (enabled only in processed mode) saves the most recent edge frame.
- Processed mode path:
  1. CameraX delivers `ImageProxy` in YUV_420_888.
  2. JNI layer converts planes → NV21, runs Canny in OpenCV, returns RGBA buffer.
  3. GLSurfaceView uploads the RGBA buffer directly as a texture and renders a full-screen quad.
  4. The same RGBA snapshot is cached for saving/export.
- Saving produces:
  - PNG stored under `Pictures/FlamEdgeViewer/edge_YYYYMMDD_HHmmss.png` (scoped storage aware).
  - `files/edge_frame_base64.txt` containing the identical frame encoded as Base64 (toast displays the file path).
- Overlay shows live FPS, resolution, and mode state.

## Web viewer

```bash
cd web
npm install
npm run build
npm run start
```

Open `http://localhost:5174`:
- Paste the Base64 from `edge_frame_base64.txt` → **Load Frame** renders it.
- **Use Sample** loads a bundled demo frame if you do not yet have device output.

## Notes & next ideas

- Current native pipeline copies YUV planes into an NV21 buffer each frame for clarity. For production, reuse buffers or integrate with the CameraX NDK image reader for zero-copy conversions.
- Tune Canny thresholds or add additional OpenGL shaders (invert, grayscale, etc.) by extending the fragment shader.
- Add a lightweight HTTP/WebSocket bridge in Android if you need to stream frames to the web viewer in real time.
- Consider adding simple instrumentation (frame timings, dropped-frame counter) and unit tests around JNI interop for robustness.
