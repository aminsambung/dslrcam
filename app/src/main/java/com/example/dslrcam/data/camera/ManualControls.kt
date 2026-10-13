package com.example.dslrcam.data.camera

import android.hardware.camera2.CaptureRequest
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.core.ImageCapture

/**
 * Wrapper untuk kontrol manual Camera2 lewat CameraX.
 * Semua nilai harus sudah di-clamp ke range device.
 */
data class ManualSettings(
    val iso: Int? = null,                    // null = auto
    val shutterSpeedNs: Long? = null,        // dalam nanosecond, null = auto
    val whiteBalanceK: Int? = null,          // Kelvin, null = auto
    val focusDistance: Float? = null,        // 0.0 (dekat) - 1.0 (jauh), null = auto
    val exposureCompensation: Int? = null    // -12..12 (langkah 1/6 EV), null = auto
)

object ManualControls {

    /**
     * Terapkan setting manual ke ImageCapture via Camera2Interop.
     */
    fun applyToCapture(imageCapture: ImageCapture.Builder, settings: ManualSettings): ImageCapture.Builder {
        val extender = Camera2Interop.Extender(imageCapture)

        settings.iso?.let { iso ->
            extender.setCaptureRequestOption(
                CaptureRequest.SENSOR_SENSITIVITY,
                iso
            )
        }

        settings.shutterSpeedNs?.let { ns ->
            extender.setCaptureRequestOption(
                CaptureRequest.SENSOR_EXPOSURE_TIME,
                ns
            )
        }

        settings.whiteBalanceK?.let { k ->
            // Matikan auto WB dulu
            extender.setCaptureRequestOption(
                CaptureRequest.CONTROL_AWB_MODE,
                CaptureRequest.CONTROL_AWB_MODE_OFF
            )
            // Set warna via COLOR_CORRECTION_GAINS (butuh konversi Kelvin ke RGB gain)
            val gains = kelvinToRgbGains(k)
            extender.setCaptureRequestOption(
                CaptureRequest.COLOR_CORRECTION_MODE,
                CaptureRequest.COLOR_CORRECTION_MODE_HIGH_QUALITY
            )
            extender.setCaptureRequestOption(
                CaptureRequest.COLOR_CORRECTION_GAINS,
                gains
            )
        }

        settings.focusDistance?.let { dist ->
            extender.setCaptureRequestOption(
                CaptureRequest.CONTROL_AF_MODE,
                CaptureRequest.CONTROL_AF_MODE_OFF
            )
            extender.setCaptureRequestOption(
                CaptureRequest.LENS_FOCUS_DISTANCE,
                dist
            )
        }

        return imageCapture
    }

    /**
     * Konversi Kelvin ke RGB gains (perkiraan sederhana).
     * Range: 2000K (hangat) - 8000K (dingin).
     */
    private fun kelvinToRgbGains(kelvin: Int): android.hardware.camera2.params.RggbChannelVector {
        val temp = kelvin / 100f

        val red: Float
        val green: Float
        val blue: Float

        if (temp <= 66) {
            red = 1.0f
            green = (99.4708025861 * Math.log(temp.toDouble()) - 161.1195681661).toFloat() / 255f
            blue = if (temp <= 19) 0f else ((138.5177312231 * Math.log((temp - 10).toDouble()) - 305.0447927307).toFloat() / 255f)
        } else {
            red = (329.698727446 * Math.pow((temp - 60).toDouble(), -0.1332047592)).toFloat() / 255f
            green = (288.1221695283 * Math.pow((temp - 60).toDouble(), -0.0755148492)).toFloat() / 255f
            blue = 1.0f
        }

        // Inverse: warna hangat = gain merah tinggi
        return android.hardware.camera2.params.RggbChannelVector(
            red.coerceIn(0.5f, 3f),
            green.coerceIn(0.5f, 3f),
            blue.coerceIn(0.5f, 3f),
            green.coerceIn(0.5f, 3f)
        )
    }
}
