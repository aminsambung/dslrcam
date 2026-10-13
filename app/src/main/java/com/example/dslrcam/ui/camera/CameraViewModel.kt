package com.example.dslrcam.ui.camera

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.dslrcam.data.camera.CameraController
import com.example.dslrcam.data.camera.ManualSettings

class CameraViewModel : ViewModel() {

    private var controller: CameraController? = null

    var manualSettings by mutableStateOf(ManualSettings())
        private set

    var manualModeEnabled by mutableStateOf(false)
        private set

    var lastPhotoUri: Uri? = null
        private set

    fun initController(context: Context) {
        if (controller == null) {
            controller = CameraController(context.applicationContext)
        }
    }

    fun getController(): CameraController? = controller

    fun onPhotoSaved(uri: Uri) {
        lastPhotoUri = uri
    }

    fun toggleManualMode() {
        manualModeEnabled = !manualModeEnabled
        if (!manualModeEnabled) {
            // Reset ke auto
            manualSettings = ManualSettings()
            controller?.updateManualSettings(manualSettings)
        }
    }

    fun updateIso(iso: Int?) {
        manualSettings = manualSettings.copy(iso = iso)
        controller?.updateManualSettings(manualSettings)
    }

    fun updateShutter(ns: Long?) {
        manualSettings = manualSettings.copy(shutterSpeedNs = ns)
        controller?.updateManualSettings(manualSettings)
    }

    fun updateWhiteBalance(k: Int?) {
        manualSettings = manualSettings.copy(whiteBalanceK = k)
        controller?.updateManualSettings(manualSettings)
    }

    fun updateFocus(distance: Float?) {
        manualSettings = manualSettings.copy(focusDistance = distance)
        controller?.updateManualSettings(manualSettings)
    }

    fun updateExposureCompensation(ev: Int?) {
        manualSettings = manualSettings.copy(exposureCompensation = ev)
        controller?.updateManualSettings(manualSettings)
    }

    fun resetManual() {
        manualSettings = ManualSettings()
        controller?.updateManualSettings(manualSettings)
    }

    override fun onCleared() {
        controller?.shutdown()
        super.onCleared()
    }
}
