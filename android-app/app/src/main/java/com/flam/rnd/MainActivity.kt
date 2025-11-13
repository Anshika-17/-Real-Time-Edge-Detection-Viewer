package com.flam.rnd

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.flam.rnd.gl.SimpleGLRenderer
import com.flam.rnd.jni.NativeProcessor
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.File
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var previewView: androidx.camera.view.PreviewView
    private lateinit var glSurfaceView: android.opengl.GLSurfaceView
    private lateinit var toggleButton: FloatingActionButton
    private lateinit var saveButton: FloatingActionButton
    private lateinit var statsText: TextView

    private var showProcessed = true

    private val glRenderer = SimpleGLRenderer()
    private val analysisExecutor = Executors.newSingleThreadExecutor()

    private var lastFpsTime = 0L
    private var frameCount = 0
    private var currentFps = 0

    private var outputWidth = 0
    private var outputHeight = 0
    private var workingBuffer: ByteBuffer? = null
    @Volatile private var lastProcessedFrame: ByteBuffer? = null

    private val requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startCamera()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        previewView = findViewById(R.id.previewView)
        glSurfaceView = findViewById(R.id.glSurfaceView)
        toggleButton = findViewById(R.id.toggleButton)
        saveButton = findViewById(R.id.saveButton)
        statsText = findViewById(R.id.statsText)

        glSurfaceView.setEGLContextClientVersion(2)
        glSurfaceView.setRenderer(glRenderer)
        glSurfaceView.renderMode = android.opengl.GLSurfaceView.RENDERMODE_CONTINUOUSLY

        toggleButton.setOnClickListener {
            showProcessed = !showProcessed
            updateModeUI()
        }
        saveButton.setOnClickListener { onSaveFrame() }
        updateModeUI()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestPermission.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            analysis.setAnalyzer(analysisExecutor) { image ->
                try {
                    onFrame(image)
                } finally {
                    image.close()
                }
            }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun ensureBuffers(width: Int, height: Int) {
        val capacity = width * height * 4
        if (workingBuffer == null || workingBuffer!!.capacity() != capacity) {
            workingBuffer = ByteBuffer.allocateDirect(capacity)
        }
        if (outputWidth != width || outputHeight != height) {
            outputWidth = width
            outputHeight = height
        }
    }

    private fun onFrame(image: ImageProxy) {
        frameCount += 1
        val now = System.currentTimeMillis()
        if (lastFpsTime == 0L) lastFpsTime = now
        val elapsed = now - lastFpsTime
        if (elapsed >= 1000) {
            currentFps = frameCount
            frameCount = 0
            lastFpsTime = now
        }

        if (!showProcessed) {
            runOnUiThread {
                statsText.text = "FPS: $currentFps  Res: ${image.width}x${image.height}  Mode: Raw"
            }
            return
        }

        ensureBuffers(image.width, image.height)
        val buffer = workingBuffer ?: return

        val planes = image.planes
        val yPlane = planes[0]
        val uPlane = planes[1]
        val vPlane = planes[2]

        val yBuffer = yPlane.buffer
        val uBuffer = uPlane.buffer
        val vBuffer = vPlane.buffer

        yBuffer.position(0)
        uBuffer.position(0)
        vBuffer.position(0)
        buffer.position(0)

        NativeProcessor.processFrame(
            yBuffer,
            uBuffer,
            vBuffer,
            image.width,
            image.height,
            yPlane.rowStride,
            uPlane.rowStride,
            uPlane.pixelStride,
            buffer
        )

        buffer.position(0)
        val snapshot = ByteBuffer.allocateDirect(buffer.capacity())
        snapshot.put(buffer)
        snapshot.position(0)
        lastProcessedFrame = snapshot.duplicate().apply { position(0) }

        val glBuffer = snapshot.duplicate().apply { position(0) }
        glSurfaceView.queueEvent {
            glRenderer.updateFrame(glBuffer, outputWidth, outputHeight)
        }
        glSurfaceView.requestRender()

        runOnUiThread {
            statsText.text = "FPS: $currentFps  Res: ${image.width}x${image.height}  Mode: Processed"
        }
    }

    private fun onSaveFrame() {
        if (!showProcessed) {
            Toast.makeText(this, R.string.saving_not_available, Toast.LENGTH_SHORT).show()
            return
        }
        val buffer = lastProcessedFrame?.duplicate()?.apply { position(0) }
        if (buffer == null || outputWidth == 0 || outputHeight == 0) {
            Toast.makeText(this, R.string.no_frame, Toast.LENGTH_SHORT).show()
            return
        }
        val bitmap = createBitmapFromBuffer(buffer, outputWidth, outputHeight)
        val uri = saveBitmapToGallery(bitmap)
        val base64Path = saveBitmapBase64(bitmap)
        val message = buildString {
            append(getString(R.string.saved_to, uri?.toString() ?: "app storage"))
            base64Path?.let {
                append("\nBase64: ")
                append(it)
            }
        }
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun createBitmapFromBuffer(buffer: ByteBuffer, width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        buffer.position(0)
        bitmap.copyPixelsFromBuffer(buffer)
        buffer.position(0)
        return bitmap
    }

    private fun saveBitmapToGallery(bitmap: Bitmap): Uri? {
        val name = "edge_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
            .format(System.currentTimeMillis()) + ".png"
        val resolver = contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/FlamEdgeViewer")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        val uri = resolver.insert(collection, contentValues) ?: return null
        resolver.openOutputStream(uri)?.use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.clear()
            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)
        }
        return uri
    }

    private fun saveBitmapBase64(bitmap: Bitmap): String? {
        val file = File(filesDir, "edge_frame_base64.txt")
        return try {
            val byteStream = java.io.ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteStream)
            val base64 = android.util.Base64.encodeToString(
                byteStream.toByteArray(),
                android.util.Base64.NO_WRAP
            )
            file.writeText(base64)
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun updateModeUI() {
        if (showProcessed) {
            glSurfaceView.visibility = android.view.View.VISIBLE
            previewView.visibility = android.view.View.GONE
            saveButton.alpha = 1.0f
        } else {
            glSurfaceView.visibility = android.view.View.GONE
            previewView.visibility = android.view.View.VISIBLE
            saveButton.alpha = 0.5f
        }
    }
}
