#include <jni.h>
#include <opencv2/core.hpp>
#include <opencv2/imgproc.hpp>
#include <vector>
#include <cstring>

using namespace cv;

namespace {
    void copyYPlane(uint8_t* dest, const uint8_t* src, int width, int height, int rowStride) {
        for (int row = 0; row < height; ++row) {
            std::memcpy(dest + row * width, src + row * rowStride, width);
        }
    }

    void copyUVPlanesNV21(
        uint8_t* dest,
        const uint8_t* uSrc,
        const uint8_t* vSrc,
        int width,
        int height,
        int rowStride,
        int pixelStride
    ) {
        const int chromaHeight = height / 2;
        for (int row = 0; row < chromaHeight; ++row) {
            uint8_t* destRow = dest + row * width;
            const uint8_t* uRow = uSrc + row * rowStride;
            const uint8_t* vRow = vSrc + row * rowStride;
            for (int col = 0; col < width / 2; ++col) {
                destRow[col * 2] = vRow[col * pixelStride];     // V
                destRow[col * 2 + 1] = uRow[col * pixelStride]; // U
            }
        }
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_flam_rnd_jni_NativeProcessor_processFrame(
        JNIEnv* env,
        jobject /*thiz*/,
        jobject yBuffer,
        jobject uBuffer,
        jobject vBuffer,
        jint width,
        jint height,
        jint yRowStride,
        jint uvRowStride,
        jint uvPixelStride,
        jobject outBuffer) {
    auto* yPtr = static_cast<uint8_t*>(env->GetDirectBufferAddress(yBuffer));
    auto* uPtr = static_cast<uint8_t*>(env->GetDirectBufferAddress(uBuffer));
    auto* vPtr = static_cast<uint8_t*>(env->GetDirectBufferAddress(vBuffer));
    auto* outPtr = static_cast<uint8_t*>(env->GetDirectBufferAddress(outBuffer));

    if (!yPtr || !uPtr || !vPtr || !outPtr) {
        return;
    }

    const int ySize = width * height;
    const int uvSize = ySize / 2;
    std::vector<uint8_t> yuv(static_cast<size_t>(ySize + uvSize));
    copyYPlane(yuv.data(), yPtr, width, height, yRowStride);
    copyUVPlanesNV21(yuv.data() + ySize, uPtr, vPtr, width, height, uvRowStride, uvPixelStride);

    Mat yuvMat(height + height / 2, width, CV_8UC1, yuv.data());
    Mat rgba;
    cvtColor(yuvMat, rgba, COLOR_YUV2RGBA_NV21);

    Mat gray;
    cvtColor(rgba, gray, COLOR_RGBA2GRAY);

    Mat edges;
    Canny(gray, edges, 50, 150);

    Mat edgesRgba;
    cvtColor(edges, edgesRgba, COLOR_GRAY2RGBA);

    std::memcpy(outPtr, edgesRgba.data, static_cast<size_t>(width * height * 4));
}
