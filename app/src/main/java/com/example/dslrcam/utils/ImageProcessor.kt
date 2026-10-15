package com.example.dslrcam.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.util.Log
import java.io.File
import java.io.FileOutputStream

object ImageProcessor {

    private const val TAG = "ImageProcessor"

    fun applyFilterToFile(
        sourceFile: File,
        filter: PhotoFilters.Filter,
        strength: Float = 1f
    ): File? {
        return try {
            // Strength 0 atau filter normal — tidak perlu proses
            if (filter.matrix == null || strength <= 0f) {
                return sourceFile
            }

            // Baca bitmap
            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val bitmap = BitmapFactory.decodeFile(sourceFile.absolutePath, options)
                ?: return null

            // Dapatkan matrix dengan strength
            val matrix = PhotoFilters.withStrength(filter, strength)
                ?: return sourceFile

            // Apply filter
            val filtered = Bitmap.createBitmap(
                bitmap.width,
                bitmap.height,
                Bitmap.Config.ARGB_8888
            )

            val canvas = Canvas(filtered)
            val paint = Paint().apply {
                colorFilter = ColorMatrixColorFilter(matrix)
            }
            canvas.drawBitmap(bitmap, 0f, 0f, paint)

            // Simpan ke file baru
            val filteredFile = File(
                sourceFile.parent,
                "filtered_${sourceFile.name}"
            )

            FileOutputStream(filteredFile).use { output ->
                filtered.compress(Bitmap.CompressFormat.JPEG, 100, output)
            }

            bitmap.recycle()
            filtered.recycle()

            Log.d(TAG, "Filter applied: ${filter.name} @ ${(strength * 100).toInt()}%")
            filteredFile

        } catch (e: Exception) {
            Log.e(TAG, "applyFilter error", e)
            null
        }
    }
}
