package com.example.dslrcam.ui.camera

import android.content.Context
import androidx.lifecycle.ViewModel
import com.example.dslrcam.data.camera.CameraController
import java.io.File

class CameraViewModel : ViewModel() {

    private var controller: CameraController? = null

    fun initController(context: Context) {
        if (controller == null) {
            controller = CameraController(context.applicationContext)
        }
    }

    fun getController(): CameraController? = controller

    var lastPhoto: File? = null
        private set

    fun onPhotoSaved(file: File) {
        lastPhoto = file
    }

    override fun onCleared() {
        controller?.shutdown()
        super.onCleared()
    }
}
