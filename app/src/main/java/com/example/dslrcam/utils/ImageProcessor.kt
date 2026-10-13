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
        filter: PhotoFilters.Filter
    ): File? {
        return try {
            if (filter.matrix == null) {
                return sourceFile
            }

            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val bitmap = BitmapFactory.decodeFile(sourceFile.absolutePath, options)
                ?: return null

            val filtered = Bitmap.createBitmap(
                bitmap.width,
                bitmap.height,
                Bitmap.Config.ARGB_8888
            )

            val canvas = Canvas(filtered)
            val paint = Paint().apply {
                colorFilter = ColorMatrixColorFilter(filter.matrix)
            }
            canvas.drawBitmap(bitmap, 0f, 0f, paint)

            val filteredFile = File(
                sourceFile.parent,
                "filtered_${sourceFile.name}"
            )

            FileOutputStream(filteredFile).use { output ->
                filtered.compress(Bitmap.CompressFormat.JPEG, 100, output)
            }

            bitmap.recycle()
            filtered.recycle()

            Log.d(TAG, "Filter applied: ${filter.name}")
            filteredFile

        } catch (e: Exception) {
            Log.e(TAG, "applyFilter error", e)
            null
        }
    }
}
