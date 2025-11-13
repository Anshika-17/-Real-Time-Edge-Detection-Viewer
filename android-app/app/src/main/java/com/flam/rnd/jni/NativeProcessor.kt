package com.flam.rnd.jni

import android.graphics.Bitmap

object NativeProcessor {
    init {
        System.loadLibrary("native-lib")
    }

    external fun cannyEdges(input: Bitmap): Bitmap
}
