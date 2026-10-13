package com.example.dslrcam.data.camera

import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class CameraController(private val context: Context) {

    companion object {
        private const val TAG = "CameraController"
    }

    private var imageCapture: ImageCapture? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var isReady = false

    fun startCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        onReady: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        val future = ProcessCameraProvider.getInstance(context)

        future.addListener({
            try {
                val provider = future.get()
                cameraProvider = provider

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                    .build()

                val selector = CameraSelector.DEFAULT_BACK_CAMERA

                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    selector,
                    preview,
                    imageCapture
                )

                isReady = true
                Log.d(TAG, "Camera ready")
                onReady()
            } catch (e: Exception) {
                Log.e(TAG, "startCamera error", e)
                isReady = false
                onError(e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun takePhoto(onSaved: (File) -> Unit, onError: (Throwable) -> Unit) {
        val capture = imageCapture

        if (capture == null || !isReady) {
            val err = IllegalStateException("Kamera belum siap")
            Log.e(TAG, "takePhoto: $err")
            onError(err)
            return
        }

        // Pastikan folder ada (jangan sampai null)
        val dir = context.getExternalFilesDir("photos")
            ?: File(context.filesDir, "photos").also { it.mkdirs() }

        if (!dir.exists()) {
            dir.mkdirs()
        }

        val name = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
            .format(System.currentTimeMillis())
        val file = File(dir, "DSLRCam_$name.jpg")

        Log.d(TAG, "Saving to: ${file.absolutePath}")

        val options = ImageCapture.OutputFileOptions.Builder(file).build()

        capture.takePicture(
            options,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    Log.d(TAG, "Foto tersimpan: ${file.absolutePath}")
                    onSaved(file)
                }

                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Gagal simpan foto", exc)
                    onError(exc)
                }
            }
        )
    }

    fun shutdown() {
        cameraProvider?.unbindAll()
        isReady = false
    }
}
