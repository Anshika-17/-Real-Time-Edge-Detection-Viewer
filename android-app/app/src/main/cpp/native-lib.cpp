#include <jni.h>
#include <android/bitmap.h>
#include <opencv2/core.hpp>
#include <opencv2/imgproc.hpp>

using namespace cv;

static void bitmapToMat(JNIEnv* env, jobject bitmap, Mat& mat) {
    AndroidBitmapInfo info;
    void* pixels = nullptr;
    AndroidBitmap_getInfo(env, bitmap, &info);
    AndroidBitmap_lockPixels(env, bitmap, &pixels);
    mat = Mat(info.height, info.width, CV_8UC4, pixels);
    AndroidBitmap_unlockPixels(env, bitmap);
}

static jobject matToBitmap(JNIEnv* env, const Mat& src) {
    jclass bitmapCls = env->FindClass("android/graphics/Bitmap");
    jmethodID createBitmap = env->GetStaticMethodID(
        bitmapCls,
        "createBitmap",
        "(IILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;"
    );

    jclass configCls = env->FindClass("android/graphics/Bitmap$Config");
    jmethodID rgba8888 = env->GetStaticMethodID(configCls, "valueOf", "(Ljava/lang/String;)Landroid/graphics/Bitmap$Config;");
    jstring argb = env->NewStringUTF("ARGB_8888");
    jobject config = env->CallStaticObjectMethod(configCls, rgba8888, argb);
    env->DeleteLocalRef(argb);

    jobject bmp = env->CallStaticObjectMethod(bitmapCls, createBitmap, src.cols, src.rows, config);

    void* pixels = nullptr;
    AndroidBitmapInfo info;
    AndroidBitmap_getInfo(env, bmp, &info);
    AndroidBitmap_lockPixels(env, bmp, &pixels);
    Mat dst(info.height, info.width, CV_8UC4, pixels);
    if (src.type() == CV_8UC1) {
        cvtColor(src, dst, COLOR_GRAY2RGBA);
    } else if (src.type() == CV_8UC4) {
        src.copyTo(dst);
    }
    AndroidBitmap_unlockPixels(env, bmp);
    return bmp;
}

extern "C"
JNIEXPORT jobject JNICALL
Java_com_flam_rnd_jni_NativeProcessor_cannyEdges(
        JNIEnv* env,
        jobject /*thiz*/,
        jobject inputBitmap) {
    Mat rgba;
    bitmapToMat(env, inputBitmap, rgba);
    Mat gray;
    cvtColor(rgba, gray, COLOR_RGBA2GRAY);
    Mat edges;
    Canny(gray, edges, 50, 150);
    return matToBitmap(env, edges);
}
