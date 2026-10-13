package com.example.dslrcam.data.camera

import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.RggbChannelVector
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.core.ImageCapture

data class ManualSettings(
    val iso: Int? = null,
    val shutterSpeedNs: Long? = null,
    val whiteBalanceK: Int? = null,
    val focusDistance: Float? = null,
    val exposureCompensation: Int? = null
)

object ManualControls {

    fun applyToCapture(
        imageCapture: ImageCapture.Builder,
        settings: ManualSettings
    ): ImageCapture.Builder {
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
            extender.setCaptureRequestOption(
                CaptureRequest.CONTROL_AWB_MODE,
                CaptureRequest.CONTROL_AWB_MODE_OFF
            )
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

    private fun kelvinToRgbGains(kelvin: Int): RggbChannelVector {
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
}
