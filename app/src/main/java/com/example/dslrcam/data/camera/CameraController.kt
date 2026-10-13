package com.example.dslrcam.data.camera

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.dslrcam.data.storage.MediaStoreHelper
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
    private var currentSettings: ManualSettings = ManualSettings()

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

                rebuildImageCapture()

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

    private fun rebuildImageCapture() {
        val builder = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)

        imageCapture = ManualControls.applyToCapture(builder, currentSettings).build()
    }

    fun updateManualSettings(settings: ManualSettings) {
        currentSettings = settings
        if (isReady) {
            rebuildImageCapture()
        }
        Log.d(TAG, "Manual settings updated: $settings")
    }

    fun takePhoto(onSaved: (Uri) -> Unit, onError: (Throwable) -> Unit) {
        val capture = imageCapture

        if (capture == null || !isReady) {
            val err = IllegalStateException("Kamera belum siap")
            Log.e(TAG, "takePhoto: $err")
            onError(err)
            return
        }

        val dir = File(context.cacheDir, "photos").also { it.mkdirs() }

        val name = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
            .format(System.currentTimeMillis())
        val tempFile = File(dir, "DSLRCam_$name.jpg")

        val options = ImageCapture.OutputFileOptions.Builder(tempFile).build()

        capture.takePicture(
            options,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    Log.d(TAG, "Temp tersimpan: ${tempFile.absolutePath}")
                    val uri = MediaStoreHelper.saveToGallery(context, tempFile)
                    if (uri != null) {
                        onSaved(uri)
                    } else {
                        onError(IllegalStateException("Gagal simpan ke MediaStore"))
                    }
                }

                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Gagal capture", exc)
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
