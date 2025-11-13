package com.flam.rnd.jni

import java.nio.ByteBuffer

object NativeProcessor {
    init {
        System.loadLibrary("native-lib")
    }

    external fun processFrame(
        yPlane: ByteBuffer,
        uPlane: ByteBuffer,
        vPlane: ByteBuffer,
        width: Int,
        height: Int,
        yRowStride: Int,
        uvRowStride: Int,
        uvPixelStride: Int,
        outRgba: ByteBuffer
    )
}
