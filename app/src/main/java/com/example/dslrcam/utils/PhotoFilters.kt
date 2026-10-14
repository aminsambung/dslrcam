package com.example.dslrcam.utils

import android.graphics.ColorMatrix

object PhotoFilters {

    data class Filter(
        val id: String,
        val name: String,
        val matrix: ColorMatrix?
    )

    val ALL = listOf(
        Filter("normal", "Normal", null),
        Filter("iphone", "iPhone", iphone()),
        Filter("pixel", "Pixel", pixel()),
        Filter("samsung", "Samsung", samsung()),
        Filter("glowing", "Glowing", glowing()),
        Filter("cinematic", "Cinematic", cinematic()),
        Filter("mono", "Mono", mono())
    )

    fun findById(id: String): Filter {
        return ALL.firstOrNull { it.id == id } ?: ALL.first()
    }

    /**
     * Buat matrix dengan intensitas tertentu.
     * strength: 0.0 (tanpa efek) - 1.0 (efek penuh)
     */
    fun withStrength(filter: Filter, strength: Float): ColorMatrix? {
        val base = filter.matrix ?: return null
        if (strength >= 1f) return base
        if (strength <= 0f) return null

        val identity = floatArrayOf(
            1f, 0f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )

        val filterArray = FloatArray = base.array

        val blended = FloatArray(20) { i ->
            identity[i] * (1f - strength) + filterArray[i] * strength
        }

        return ColorMatrix(blended)
    }

    // === iPhone: natural, hangat, kontras sedang ===
    private fun iphone() = ColorMatrix(floatArrayOf(
        1.05f, 0.00f, 0.00f, 0f, 3f,
        0.00f, 1.02f, 0.00f, 0f, 2f,
        0.00f, 0.00f, 0.98f, 0f, -1f,
        0f, 0f, 0f, 1f, 0f
    ))

    // === Pixel: kontras tinggi, vivid, tegas ===
    private fun pixel() = ColorMatrix(floatArrayOf(
        1.25f, -0.10f, -0.05f, 0f, -10f,
        -0.05f, 1.25f, -0.10f, 0f, -10f,
        -0.10f, -0.05f, 1.25f, 0f, -10f,
        0f, 0f, 0f, 1f, 0f
    ))

    // === Samsung: saturasi tinggi, warna "pop" ===
    private fun samsung() = ColorMatrix(floatArrayOf(
        1.35f, -0.15f, -0.10f, 0f, 0f,
        -0.10f, 1.35f, -0.15f, 0f, 0f,
        -0.15f, -0.10f, 1.35f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    ))

    // === Glowing (dari config GCam "Colorful Photo") ===
    // - Color mode: colorful (saturasi tinggi)
    // - Green boost (hijau vibrant)
    // - Highlight soft (offset kecil positif → efek glow)
    // - Kontras sedang
    private fun glowing() = ColorMatrix(floatArrayOf(
        1.15f, -0.05f, 0.00f, 0f, 5f,       // R naik sedang, hangat tipis
        -0.05f, 1.25f, -0.05f, 0f, 8f,      // G paling naik (green boost), glow
        -0.05f, -0.05f, 1.20f, 0f, 5f,      // B naik sedang
        0f, 0f, 0f, 1f, 0f
    ))

    // === Cinematic: biru pekat, hijau vibrant, kontras tinggi ===
    private fun cinematic() = ColorMatrix(floatArrayOf(
        0.95f, -0.10f, -0.05f, 0f, -5f,     // R turun, dingin
        -0.05f, 1.15f, -0.05f, 0f, 0f,      // G naik sedikit
        -0.10f, -0.10f, 1.40f, 0f, 10f,     // B naik banyak (langit pekat)
        0f, 0f, 0f, 1f, 0f
    ))

    // === Mono: hitam putih ===
    private fun mono() = ColorMatrix().apply { setSaturation(0f) }
}
