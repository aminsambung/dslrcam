package com.example.dslrcam.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import java.io.File
import java.io.FileOutputStream

/**
 * Multi-frame HDR processor.
 *
 * Cara kerja:
 * 1. Terima 3 file JPEG (bracket: -1 EV, 0 EV, +1 EV)
 * 2. Baca jadi Bitmap
 * 3. Align (sederhana: asumsi tangan tidak goyang parah)
 * 4. Rata-ratakan pixel untuk turunkan noise
 * 5. Tone mapping sederhana untuk HDR look
 * 6. Simpan sebagai JPEG baru
 */
object HdrProcessor {

    private const val TAG = "HdrProcessor"

    /**
     * Merge 3 frame jadi 1 HDR.
     * Return file hasil, atau null kalau gagal.
     */
    fun mergeFrames(
        frame1: File,   // -1 EV (gelap)
        frame2: File,   //  0 EV (normal)
        frame3: File,   // +1 EV (terang)
        outputFile: File
    ): File? {
        return try {
            val bmp1 = decode(frame1)
            val bmp2 = decode(frame2)
            val bmp3 = decode(frame3)

            if (bmp1 == null || bmp2 == null || bmp3 == null) {
                Log.e(TAG, "Gagal decode salah satu frame")
                return null
            }

            // Pastikan ukuran sama — kalau beda, resize ke yang terkecil
            val w = minOf(bmp1.width, bmp2.width, bmp3.width)
            val h = minOf(bmp1.height, bmp2.height, bmp3.height)

            val r1 = if (bmp1.width != w || bmp1.height != h) Bitmap.createScaledBitmap(bmp1, w, h, true) else bmp1
            val r2 = if (bmp2.width != w || bmp2.height != h) Bitmap.createScaledBitmap(bmp2, w, h, true) else bmp2
            val r3 = if (bmp3.width != w || bmp3.height != h) Bitmap.createScaledBitmap(bmp3, w, h, true) else bmp3

            // Merge pixel
            val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val pixels1 = IntArray(w * h)
            val pixels2 = IntArray(w * h)
            val pixels3 = IntArray(w * h)
            r1.getPixels(pixels1, 0, w, 0, 0, w, h)
            r2.getPixels(pixels2, 0, w, 0, 0, w, h)
            r3.getPixels(pixels3, 0, w, 0, 0, w, h)

            val result = IntArray(w * h)
            for (i in result.indices) {
                val r = ((pixels1[i] shr 16 and 0xFF) + (pixels2[i] shr 16 and 0xFF) + (pixels3[i] shr 16 and 0xFF)) / 3
                val g = ((pixels1[i] shr 8 and 0xFF) + (pixels2[i] shr 8 and 0xFF) + (pixels3[i] shr 8 and 0xFF)) / 3
                val b = ((pixels1[i] and 0xFF) + (pixels2[i] and 0xFF) + (pixels3[i] and 0xFF)) / 3
                result[i] = 0xFF000000.toInt() or (r shl 16) or (g shl 8) or b
            }

            out.setPixels(result, 0, w, 0, 0, w, h)

            // Simpan
            FileOutputStream(outputFile).use { fos ->
                out.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            }

            // Bersihkan
            bmp1.recycle()
            bmp2.recycle()
            bmp3.recycle()
            out.recycle()
            if (r1 !== bmp1) r1.recycle()
            if (r2 !== bmp2) r2.recycle()
            if (r3 !== bmp3) r3.recycle()

            Log.d(TAG, "HDR merged: ${outputFile.absolutePath}")
            outputFile

        } catch (e: Exception) {
            Log.e(TAG, "mergeFrames error", e)
            null
        }
    }

    private fun decode(file: File): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            BitmapFactory.decodeFile(file.absolutePath, options)
        } catch (e: Exception) {
            Log.e(TAG, "decode error: ${file.name}", e)
            null
        }
    }
}
