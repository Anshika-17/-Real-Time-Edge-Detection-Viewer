package com.flam.rnd

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.flam.rnd.gl.SimpleGLRenderer
import android.opengl.GLSurfaceView
import androidx.camera.view.PreviewView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.flam.rnd.camera.YuvUtils
import com.flam.rnd.jni.NativeProcessor
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var previewView: PreviewView
    private lateinit var glSurfaceView: GLSurfaceView
    private lateinit var toggleButton: FloatingActionButton
    private lateinit var statsText: TextView

    private var showProcessed = true

    private val glRenderer = SimpleGLRenderer()
    private val analysisExecutor = Executors.newSingleThreadExecutor()

    private var lastFpsTime = 0L
    private var frameCount = 0
    private var currentFps = 0

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
        statsText = findViewById(R.id.statsText)

        glSurfaceView.setEGLContextClientVersion(2)
        glSurfaceView.setRenderer(glRenderer)
        glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY

        toggleButton.setOnClickListener {
            showProcessed = !showProcessed
            updateModeUI()
        }
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

        if (showProcessed) {
            val bmp = YuvUtils.toBitmap(image)
            val output = NativeProcessor.cannyEdges(bmp)
            glSurfaceView.queueEvent { glRenderer.updateFrame(output) }
            glSurfaceView.requestRender()
            runOnUiThread {
                statsText.text = "FPS: $currentFps  Res: ${image.width}x${image.height}  Mode: Processed"
            }
        } else {
            // Raw mode: show camera preview, skip JNI/GL upload
            runOnUiThread {
                statsText.text = "FPS: $currentFps  Res: ${image.width}x${image.height}  Mode: Raw"
            }
        }
    }

    private fun updateModeUI() {
        if (showProcessed) {
            glSurfaceView.visibility = android.view.View.VISIBLE
            previewView.visibility = android.view.View.GONE
        } else {
            glSurfaceView.visibility = android.view.View.GONE
            previewView.visibility = android.view.View.VISIBLE
        }
    }
}
