package com.example.dslrcam.ui.camera

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import com.example.dslrcam.data.camera.CameraController

class CameraViewModel : ViewModel() {

    private var controller: CameraController? = null

    fun initController(context: Context) {
        if (controller == null) {
            controller = CameraController(context.applicationContext)
        }
    }

    fun getController(): CameraController? = controller

    var lastPhotoUri: Uri? = null
        private set

    fun onPhotoSaved(uri: Uri) {
        lastPhotoUri = uri
    }

    override fun onCleared() {
        controller?.shutdown()
        super.onCleared()
    }
}
