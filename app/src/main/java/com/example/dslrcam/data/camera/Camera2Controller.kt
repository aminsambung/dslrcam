package com.example.dslrcam.data.camera

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.hardware.camera2.params.RggbChannelVector
import android.media.ImageReader
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import android.view.TextureView
import com.example.dslrcam.data.storage.MediaStoreHelper
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale

class Camera2Controller(private val context: Context) {

    companion object {
        private const val TAG = "Camera2Controller"
    }

    private var cameraManager: CameraManager? = null
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null
    private var cameraId: String? = null
    private var characteristics: CameraCharacteristics? = null

    private var previewSurface: Surface? = null
    private var previewSize: android.util.Size? = null
    private var currentSettings: ManualSettings = ManualSettings()

    private var pendingSavedCallback: ((Uri) -> Unit)? = null
    private var pendingErrorCallback: ((Throwable) -> Unit)? = null

    var isReady = false
        private set

    /**
     * Panggil saat app dibuka. Buka kamera.
     */
    @SuppressLint("MissingPermission")
    fun startCamera(
        textureView: TextureView,
        onReady: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        try {
            // Kalau sudah ready, tidak perlu buka lagi
            if (isReady) {
                onReady()
                return
            }

            cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

            cameraId = cameraManager!!.cameraIdList.firstOrNull { id ->
                val chars = cameraManager!!.getCameraCharacteristics(id)
                chars.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
            } ?: cameraManager!!.cameraIdList.firstOrNull()

            if (cameraId == null) {
                onError(IllegalStateException("Tidak ada kamera"))
                return
            }

            characteristics = cameraManager!!.getCameraCharacteristics(cameraId!!)

            startBackgroundThread()

            textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, h: Int) {
                    openCamera(st, onReady, onError)
                }

                override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) {}

                override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                    // PENTING: tutup kamera saat surface di-destroy
                    closeCamera()
                    return true
                }

                override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
            }

            if (textureView.isAvailable) {
                openCamera(textureView.surfaceTexture!!, onReady, onError)
            }

        } catch (e: Exception) {
            Log.e(TAG, "startCamera error", e)
            onError(e)
        }
    }

    private fun startBackgroundThread() {
        if (backgroundThread == null) {
            backgroundThread = HandlerThread("Camera2Background").also { it.start() }
            backgroundHandler = Handler(backgroundThread!!.looper)
        }
    }

    @SuppressLint("MissingPermission")
    private fun openCamera(
        surfaceTexture: SurfaceTexture,
        onReady: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        try {
            val chars = characteristics ?: return
            val map = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP) ?: return
            val camId = cameraId ?: run {
                onError(IllegalStateException("Camera ID null"))
                return
            }

            // Pilih ukuran preview
            val previewSizes = map.getOutputSizes(SurfaceTexture::class.java)
            previewSize = previewSizes
                .filter { it.width <= 1920 && it.height <= 1080 }
                .maxByOrNull { it.width * it.height }
                ?: previewSizes.first()

            surfaceTexture.setDefaultBufferSize(previewSize!!.width, previewSize!!.height)
            previewSurface = Surface(surfaceTexture)

            // ImageReader untuk capture JPEG
            val captureSize = map.getOutputSizes(ImageFormat.JPEG)
                .maxByOrNull { it.width * it.height } ?: previewSize!!

            imageReader = ImageReader.newInstance(
                captureSize.width,
                captureSize.height,
                ImageFormat.JPEG,
                2
            )

            imageReader!!.setOnImageAvailableListener({ reader ->
                val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
                try {
                    val uri = saveImage(image)
                    if (uri != null) {
                        pendingSavedCallback?.invoke(uri)
                    } else {
                        pendingErrorCallback?.invoke(IllegalStateException("Gagal simpan"))
                    }
                } catch (e: Exception) {
                    pendingErrorCallback?.invoke(e)
                } finally {
                    image.close()
                    pendingSavedCallback = null
                    pendingErrorCallback = null
                }
            }, backgroundHandler)

            cameraManager!!.openCamera(camId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera
                    createSession(onReady, onError)
                }

                override fun onDisconnected(camera: CameraDevice) {
                    camera.close()
                    cameraDevice = null
                    isReady = false
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    camera.close()
                    cameraDevice = null
                    isReady = false
                    onError(IllegalStateException("Camera error: $error"))
                }
            }, backgroundHandler)

        } catch (e: Exception) {
            Log.e(TAG, "openCamera error", e)
            onError(e)
        }
    }

    private fun createSession(onReady: () -> Unit, onError: (Throwable) -> Unit) {
        try {
            val device = cameraDevice ?: return
            val preview = previewSurface ?: return
            val reader = imageReader ?: return

            val surfaceList = listOf(preview, reader.surface)

            device.createCaptureSession(surfaceList, object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    captureSession = session
                    try {
                        val previewRequest = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                            addTarget(preview)
                            applyManualSettings(this)
                        }

                        session.setRepeatingRequest(previewRequest.build(), null, backgroundHandler)
                        isReady = true
                        Log.d(TAG, "Camera2 ready")
                        onReady()
                    } catch (e: Exception) {
                        Log.e(TAG, "setRepeatingRequest error", e)
                        onError(e)
                    }
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    onError(IllegalStateException("Gagal configure session"))
                }
            }, backgroundHandler)

        } catch (e: Exception) {
            Log.e(TAG, "createSession error", e)
            onError(e)
        }
    }

    private fun applyManualSettings(builder: CaptureRequest.Builder) {
        val chars = characteristics ?: return

        // AE mode
        if (currentSettings.iso != null || currentSettings.shutterSpeedNs != null) {
            builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)

            currentSettings.iso?.let { iso ->
                val range = chars.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
                if (range != null && range.contains(iso)) {
                    builder.set(CaptureRequest.SENSOR_SENSITIVITY, iso)
                }
            }

            currentSettings.shutterSpeedNs?.let { ns ->
                val range = chars.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
                if (range != null && range.contains(ns)) {
                    builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, ns)
                }
            }
        } else {
            builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
        }

        // WB
        currentSettings.whiteBalanceK?.let { k ->
            builder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF)
            builder.set(CaptureRequest.COLOR_CORRECTION_MODE, CaptureRequest.COLOR_CORRECTION_MODE_HIGH_QUALITY)
            builder.set(CaptureRequest.COLOR_CORRECTION_GAINS, kelvinToRggb(k))
        } ?: run {
            builder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO)
        }

        // Focus
        currentSettings.focusDistance?.let { dist ->
            builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
            builder.set(CaptureRequest.LENS_FOCUS_DISTANCE, dist)
        } ?: run {
            builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
        }
    }

    fun updateManualSettings(settings: ManualSettings) {
        currentSettings = settings

        try {
            val device = cameraDevice ?: return
            val session = captureSession ?: return
            val preview = previewSurface ?: return

            val request = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                addTarget(preview)
                applyManualSettings(this)
            }

            session.setRepeatingRequest(request.build(), null, backgroundHandler)
            Log.d(TAG, "Settings updated: $settings")
        } catch (e: Exception) {
            Log.e(TAG, "updateSettings error", e)
        }
    }

    fun takePhoto(onSaved: (Uri) -> Unit, onError: (Throwable) -> Unit) {
        val device = cameraDevice
        val session = captureSession
        val reader = imageReader

        if (device == null || session == null || reader == null || !isReady) {
            onError(IllegalStateException("Kamera belum siap"))
            return
        }

        // Simpan callback untuk dipanggil saat image ready
        pendingSavedCallback = onSaved
        pendingErrorCallback = onError

        try {
            val captureRequest = device.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
                addTarget(reader.surface)
                applyManualSettings(this)
                set(CaptureRequest.JPEG_ORIENTATION, 90)
            }

            session.capture(captureRequest.build(), null, backgroundHandler)

        } catch (e: Exception) {
            Log.e(TAG, "takePhoto error", e)
            pendingSavedCallback = null
            pendingErrorCallback = null
            onError(e)
        }
    }

    private fun saveImage(image: android.media.Image): Uri? {
        return try {
            val buffer = image.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)

            val name = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                .format(System.currentTimeMillis())
            val dir = File(context.cacheDir, "photos").also { it.mkdirs() }
            val file = File(dir, "DSLRCam_$name.jpg")

            FileOutputStream(file).use { it.write(bytes) }

            val uri = MediaStoreHelper.saveToGallery(context, file)
            Log.d(TAG, "Saved: $uri")
            uri
        } catch (e: Exception) {
            Log.e(TAG, "saveImage error", e)
            null
        }
    }

    private fun kelvinToRggb(kelvin: Int): RggbChannelVector {
        val temp = kelvin / 100f
        val red: Float
        val green: Float
        val blue: Float

        if (temp <= 66) {
            red = 1.0f
            green = (99.4708025861 * Math.log(temp.toDouble()) - 161.1195681661).toFloat() / 255f
            blue = if (temp <= 19) 0f
            else ((138.5177312231 * Math.log((temp - 10).toDouble()) - 305.0447927307).toFloat() / 255f)
        } else {
            red = (329.698727446 * Math.pow((temp - 60).toDouble(), -0.1332047592)).toFloat() / 255f
            green = (288.1221695283 * Math.pow((temp - 60).toDouble(), -0.0755148492)).toFloat() / 255f
            blue = 1.0f
        }

        return RggbChannelVector(
            red.coerceIn(0.5f, 3f),
            green.coerceIn(0.5f, 3f),
            blue.coerceIn(0.5f, 3f),
            green.coerceIn(0.5f, 3f)
        )
    }

    fun shutdown() {
        closeCamera()
        backgroundThread?.quitSafely()
        backgroundThread = null
        backgroundHandler = null
    }

    private fun closeCamera() {
        try {
            captureSession?.close()
            cameraDevice?.close()
            imageReader?.close()
        } catch (e: Exception) {
            Log.e(TAG, "closeCamera error", e)
        }
        captureSession = null
        cameraDevice = null
        imageReader = null
        previewSurface = null
        isReady = false
        Log.d(TAG, "Camera closed")
    }
}
