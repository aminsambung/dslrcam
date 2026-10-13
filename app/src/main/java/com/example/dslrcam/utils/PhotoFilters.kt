package com.example.dslrcam.utils

import android.graphics.ColorMatrix

/**
 * Filter warna dengan karakter ala HP flagship.
 */
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
        Filter("mono", "Mono", mono())
    )

    fun findById(id: String): Filter {
        return ALL.firstOrNull { it.id == id } ?: ALL.first()
    }

    // iPhone: natural, hangat, kontras sedang
    private fun iphone() = ColorMatrix(floatArrayOf(
        1.05f, 0.00f, 0.00f, 0f, 3f,
        0.00f, 1.02f, 0.00f, 0f, 2f,
        0.00f, 0.00f, 0.98f, 0f, -1f,
        0f, 0f, 0f, 1f, 0f
    ))

    // Pixel: kontras tinggi, vivid, tegas
    private fun pixel() = ColorMatrix(floatArrayOf(
        1.25f, -0.10f, -0.05f, 0f, -10f,
        -0.05f, 1.25f, -0.10f, 0f, -10f,
        -0.10f, -0.05f, 1.25f, 0f, -10f,
        0f, 0f, 0f, 1f, 0f
    ))

    // Samsung: saturasi tinggi, warna "pop"
    private fun samsung() = ColorMatrix(floatArrayOf(
        1.35f, -0.15f, -0.10f, 0f, 0f,
        -0.10f, 1.35f, -0.15f, 0f, 0f,
        -0.15f, -0.10f, 1.35f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    ))

    // Mono: hitam putih
    private fun mono() = ColorMatrix().apply { setSaturation(0f) }
}
