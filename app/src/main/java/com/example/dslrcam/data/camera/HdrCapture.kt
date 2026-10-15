package com.example.dslrcam.data.camera

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CaptureFailure
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.media.ImageReader
import android.net.Uri
import android.os.Handler
import android.util.Log
import android.view.Surface
import com.example.dslrcam.data.storage.MediaStoreHelper
import com.example.dslrcam.utils.HdrProcessor
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Multi-frame HDR capture.
 *
 * Ambil 3 frame dengan exposure berbeda:
 *   Frame 1: -1 EV (gelap)
 *   Frame 2: 0 EV (normal)
 *   Frame 3: +1 EV (terang)
 *
 * Lalu merge jadi 1 foto dengan HdrProcessor.
 */
class HdrCapture(
    private val context: Context,
    private val characteristics: CameraCharacteristics
) {

    companion object {
        private const val TAG = "HdrCapture"
        private const val FRAME_COUNT = 3
    }

    private val frames = mutableListOf<File>()
    private var currentIndex = 0

    /**
     * Mulai capture 3 frame.
     * Setiap frame punya exposure berbeda.
     */
    @SuppressLint("MissingPermission")
    fun capture(
        device: CameraDevice,
        session: CameraCaptureSession,
        reader: ImageReader,
        baseSettings: ManualSettings,
        handler: Handler,
        onDone: (Uri) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        frames.clear()
        currentIndex = 0

        try {
            // Hitung base exposure dari setting manual atau auto
            val baseExposure = getBaseExposure(baseSettings)
            val baseIso = baseSettings.iso ?: 400

            Log.d(TAG, "HDR base exposure: ${baseExposure}ns, ISO: $baseIso")

            // Array exposure offset: -1 EV, 0 EV, +1 EV
            val exposures = longArrayOf(
                baseExposure / 2,      // -1 EV (setengah waktu = lebih gelap)
                baseExposure,          // 0 EV
                baseExposure * 2       // +1 EV (dua kali waktu = lebih terang)
            )

            // Setup listener untuk tiap frame
            reader.setOnImageAvailableListener({ r ->
                val image = r.acquireLatestImage()
                if (image != null) {
                    try {
                        saveFrame(image)
                        currentIndex++
                        Log.d(TAG, "Frame $currentIndex/$FRAME_COUNT captured")

                        if (currentIndex >= FRAME_COUNT) {
                            // Semua frame selesai → merge
                            mergeAndSave(onDone, onError)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Frame save error", e)
                        onError(e)
                    } finally {
                        image.close()
                    }
                }
            }, handler)

            // Ambil frame pertama
            captureFrame(device, session, reader, exposures[0], baseIso, handler, onError)

        } catch (e: Exception) {
            Log.e(TAG, "capture error", e)
            onError(e)
        }
    }

    /**
     * Ambil 1 frame dengan exposure & ISO tertentu.
     */
    private fun captureFrame(
        device: CameraDevice,
        session: CameraCaptureSession,
        reader: ImageReader,
        exposureNs: Long,
        iso: Int,
        handler: Handler,
        onError: (Throwable) -> Unit
    ) {
        try {
            val request = device.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
                addTarget(reader.surface)
                set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
                set(CaptureRequest.SENSOR_EXPOSURE_TIME, exposureNs)
                set(CaptureRequest.SENSOR_SENSITIVITY, iso)
                set(CaptureRequest.JPEG_QUALITY, 100)
                set(CaptureRequest.JPEG_ORIENTATION, 90)

                // Quality settings
                set(CaptureRequest.NOISE_REDUCTION_MODE, CaptureRequest.NOISE_REDUCTION_MODE_HIGH_QUALITY)
                set(CaptureRequest.EDGE_MODE, CaptureRequest.EDGE_MODE_HIGH_QUALITY)
                set(CaptureRequest.COLOR_CORRECTION_ABERRATION_MODE, CaptureRequest.COLOR_CORRECTION_ABERRATION_MODE_HIGH_QUALITY)
            }

            session.capture(request.build(), object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    result: TotalCaptureResult
                ) {
                    Log.d(TAG, "Frame captured at ${exposureNs}ns")
                    // Frame berikutnya diambil setelah ini selesai
                    if (currentIndex < FRAME_COUNT) {
                        handler.postDelayed({
                            val nextExposure = when (currentIndex) {
                                1 -> exposureNs * 2
                                2 -> exposureNs * 4
                                else -> exposureNs
                            }
                            captureFrame(device, session, reader, nextExposure, iso, handler, onError)
                        }, 150)
                    }
                }

                override fun onCaptureFailed(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    failure: CaptureFailure
                ) {
                    Log.e(TAG, "Frame capture failed: ${failure.reason}")
                    onError(IllegalStateException("HDR frame failed: ${failure.reason}"))
                }
            }, handler)

        } catch (e: Exception) {
            Log.e(TAG, "captureFrame error", e)
            onError(e)
        }
    }

    /**
     * Simpan frame ke file temporary.
     */
    private fun saveFrame(image: android.media.Image) {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)

        val dir = File(context.cacheDir, "hdr_frames").also { it.mkdirs() }
        val file = File(dir, "frame_${frames.size}.jpg")

        FileOutputStream(file).use { it.write(bytes) }
        frames.add(file)

        Log.d(TAG, "Frame saved: ${file.absolutePath}")
    }

    /**
     * Merge semua frame jadi 1 HDR.
     */
    private fun mergeAndSave(onDone: (Uri) -> Unit, onError: (Throwable) -> Unit) {
        try {
            if (frames.size < 3) {
                onError(IllegalStateException("Frame kurang: ${frames.size}"))
                return
            }

            val dir = File(context.cacheDir, "hdr_output").also { it.mkdirs() }
            val name = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                .format(System.currentTimeMillis())
            val outputFile = File(dir, "HDR_$name.jpg")

            val result = HdrProcessor.mergeFrames(
                frames[0],
                frames[1],
                frames[2],
                outputFile
            )

            if (result != null && result.exists()) {
                // Simpan ke MediaStore
                val uri = MediaStoreHelper.saveToGallery(context, result)

                // Bersihkan frame temporary
                frames.forEach { it.delete() }
                frames.clear()

                if (uri != null) {
                    Log.d(TAG, "HDR saved: $uri")
                    onDone(uri)
                } else {
                    onError(IllegalStateException("Gagal simpan HDR ke MediaStore"))
                }
            } else {
                onError(IllegalStateException("Gagal merge frame"))
            }

        } catch (e: Exception) {
            Log.e(TAG, "mergeAndSave error", e)
            onError(e)
        }
    }

    /**
     * Hitung base exposure dari setting manual.
     * Kalau tidak ada, pakai default 1/60s.
     */
    private fun getBaseExposure(settings: ManualSettings): Long {
        settings.shutterSpeedNs?.let { return it }

        // Default: 1/60s
        return 1_000_000_000L / 60
    }
}
