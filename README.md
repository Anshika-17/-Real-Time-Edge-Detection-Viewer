# Flam Edge Viewer

Real-time Edge Detection • Android (CameraX + OpenCV C++ + OpenGL ES) • Web Viewer (TypeScript)

This project provides a complete real-time edge-detection pipeline:

✔ Android camera processing (YUV → OpenCV Canny → OpenGL ES)
✔ Native C++ acceleration
✔ Real-time visualization
✔ Save PNG + Base64 export
✔ Web viewer for previewing edge frames

## Structure
- **`android-app/`**
  - CameraX preview + YUV_420_888 frame capture pipeline
  - JNI bridge converting YUV → NV21 → RGBA using OpenCV (C++)
  - OpenGL ES 2.0 renderer for real-time display of processed edge frames
  - Save-to-gallery PNG output (scoped storage)
  - Base64 export for loading frames in the web viewer

- **`web/`**
  - TypeScript viewer for displaying Base64-encoded PNG frames
  - Live FPS and resolution readout
  - "Load Frame" input plus sample demo frame support

## Requirements

- **Android Studio (Giraffe+)** with **Android SDK 34**
- **NDK** and **CMake** installed via SDK Manager
- **OpenCV for Android (C++ SDK)**  
  - Set environment variable `OpenCV_DIR` pointing to:  
    `OpenCV-android-sdk/sdk/native/jni`  
    **or**
  - Update `app/src/main/cpp/CMakeLists.txt` to reference your local OpenCV SDK path
- **Native OpenCV libraries**  
  (Ensure at least `arm64-v8a` and `armeabi-v7a` are included)
- **Node.js 18+** for running the web viewer


## Android Setup

1. Open the `android-app/` folder in Android Studio.
2. Install **NDK** and **CMake** from the SDK Manager.
3. Provide the OpenCV native SDK path (`OpenCV_DIR`) as described in the Requirements section.
4. Sync the project with Gradle and build the app.
5. Deploy to a physical Android device (camera required) and grant camera/storage permissions.


### Runtime Behaviour

- **Floating action buttons** (bottom-right):
  - **Gallery Icon** — toggles between Raw CameraX preview and Processed (OpenCV + OpenGL) mode.
  - **Save Icon** — enabled only in processed mode; saves the latest processed edge frame.

- **Processed Mode Pipeline:**
  1. CameraX provides frames as `ImageProxy` in **YUV_420_888** format.
  2. JNI layer converts YUV planes → NV21, applies **Canny edge detection** using OpenCV (C++), and returns an **RGBA buffer**.
  3. `GLSurfaceView` uploads the RGBA buffer directly as a texture and renders a full-screen quad using OpenGL ES 2.0.
  4. The latest RGBA frame is cached for both PNG saving and Base64 export.

- **Saving Output:**
  - **PNG file:**  
    `Pictures/FlamEdgeViewer/edge_YYYYMMDD_HHmmss.png`  
    (Uses scoped storage, compatible with Android 10+)
  - **Base64 export:**  
    `files/edge_frame_base64.txt`  
    Contains the same processed frame encoded as Base64.  
    A toast displays the full file path after saving.

- **Overlay Information:**
  - Live FPS (frames per second)
  - Current frame resolution
  - Active mode (Raw / Processed)


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

## Notes & Next Ideas

- The current native pipeline copies YUV planes into an NV21 buffer every frame for simplicity.   For production, consider reusing buffers or using the CameraX NDK ImageReader for **zero-copy** conversions.
- Experiment with Canny thresholds or extend the rendering pipeline with additional **OpenGL fragment shaders**  
  (invert, grayscale, Sobel, blur, thresholding, etc.).
- Add a lightweight **HTTP/WebSocket bridge** in the Android app to stream processed frames to the web viewer in real time.
- Add instrumentation such as **frame timings**, **dropped-frame counters**, and **JNI interop tests** to improve robustness.

