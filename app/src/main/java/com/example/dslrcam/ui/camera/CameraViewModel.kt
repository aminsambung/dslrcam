package com.example.dslrcam.ui.camera

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import com.example.dslrcam.data.camera.Camera2Controller
import com.example.dslrcam.data.camera.ManualSettings
import com.example.dslrcam.data.storage.MediaStoreHelper
import com.example.dslrcam.utils.ImageProcessor
import com.example.dslrcam.utils.PhotoFilters
import java.io.File

class CameraViewModel : ViewModel() {

    private var controller: Camera2Controller? = null

    var manualSettings by mutableStateOf(ManualSettings())
        private set

    var manualModeEnabled by mutableStateOf(false)
        private set

    var filterPanelEnabled by mutableStateOf(false)
        private set

    var currentFilter by mutableStateOf(PhotoFilters.findById("normal"))
        private set

    var lastPhotoUri by mutableStateOf<Uri?>(null)
        private set

    var focusPoint by mutableStateOf<Offset?>(null)
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
        if (manualModeEnabled) filterPanelEnabled = false
    }

    fun toggleFilterPanel() {
        filterPanelEnabled = !filterPanelEnabled
        if (filterPanelEnabled) manualModeEnabled = false
    }

    fun setFilter(filterId: String) {
        currentFilter = PhotoFilters.findById(filterId)
    }

    fun onTapFocus(offset: Offset, viewWidth: Int, viewHeight: Int) {
        focusPoint = offset
        controller?.tapToFocus(offset.x, offset.y, viewWidth, viewHeight)
    }

    fun clearFocusPoint() {
        focusPoint = null
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

    /**
     * Apply filter ke foto hasil capture.
     * Dipanggil dari CameraScreen setelah takePhoto berhasil.
     */
    fun applyFilterToUri(context: Context, uri: Uri) {
        try {
            // Kalau Normal — langsung pakai
            if (currentFilter.matrix == null) {
                onPhotoSaved(uri)
                return
            }

            // Baca isi uri ke file temporary
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                onPhotoSaved(uri)
                return
            }

            val tempFile = File(context.cacheDir, "temp_original.jpg")
            tempFile.outputStream().use { output ->
                inputStream.copyTo(output)
            }
            inputStream.close()

            // Apply filter
            val filteredFile = ImageProcessor.applyFilterToFile(tempFile, currentFilter)

            if (filteredFile != null && filteredFile.exists()) {
                // Simpan ke MediaStore
                val newUri = MediaStoreHelper.saveToGallery(context, filteredFile)

                // Hapus file original dari MediaStore (opsional)
                try {
                    context.contentResolver.delete(uri, null, null)
                } catch (e: Exception) {
                    // Abaikan kalau gagal hapus
                }

                if (newUri != null) {
                    onPhotoSaved(newUri)
                } else {
                    onPhotoSaved(uri)
                }
            } else {
                onPhotoSaved(uri)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            onPhotoSaved(uri)
        }
    }

    override fun onCleared() {
        controller?.shutdown()
        super.onCleared()
    }
}
