package com.example.dslrcam.ui.camera

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.dslrcam.data.camera.Camera2Controller
import com.example.dslrcam.data.camera.ManualSettings

class CameraViewModel : ViewModel() {

    private var controller: Camera2Controller? = null

    var manualSettings by mutableStateOf(ManualSettings())
        private set

    var manualModeEnabled by mutableStateOf(false)
        private set

    var lastPhotoUri by mutableStateOf<Uri?>(null)
        private set

    fun initController(context: Context) {
        if (controller == null) {
            controller = Camera2Controller(context.applicationContext)
        }
    }

    fun getController(): Camera2Controller? = controller

    fun onPhotoSaved(uri: Uri) {
        lastPhotoUri = uri
    }

    fun toggleManualMode() {
        manualModeEnabled = !manualModeEnabled
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

    fun resetManual() {
        manualSettings = ManualSettings()
        controller?.updateManualSettings(manualSettings)
    }

    override fun onCleared() {
        controller?.shutdown()
        super.onCleared()
    }
}
