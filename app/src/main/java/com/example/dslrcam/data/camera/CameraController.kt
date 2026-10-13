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
    private var lifecycleOwner: LifecycleOwner? = null
    private var previewView: PreviewView? = null
    private var isReady = false
    private var currentSettings: ManualSettings = ManualSettings()

    fun startCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        onReady: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        // Simpan reference untuk rebind nanti
        this.lifecycleOwner = lifecycleOwner
        this.previewView = previewView

        val future = ProcessCameraProvider.getInstance(context)

        future.addListener({
            try {
                val provider = future.get()
                cameraProvider = provider

                bindAll()

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

    /**
     * Bind ulang Preview + ImageCapture ke lifecycle.
     * Dipanggil saat start & saat ganti setting manual.
     */
    private fun bindAll() {
        val provider = cameraProvider ?: return
        val owner = lifecycleOwner ?: return
        val pv = previewView ?: return

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(pv.surfaceProvider)
        }

        val builder = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)

        imageCapture = ManualControls.applyToCapture(builder, currentSettings).build()

        val selector = CameraSelector.DEFAULT_BACK_CAMERA

        provider.unbindAll()
        provider.bindToLifecycle(
            owner,
            selector,
            preview,
            imageCapture
        )

        Log.d(TAG, "Bound camera with settings: $currentSettings")
    }

    fun updateManualSettings(settings: ManualSettings) {
        currentSettings = settings

        if (isReady) {
            // Rebind biar ImageCapture baru aktif
            try {
                bindAll()
            } catch (e: Exception) {
                Log.e(TAG, "rebind error", e)
            }
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
