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

    val filterArray = FloatArray(20)
    base.getArray(filterArray)

    val blended = FloatArray(20) { i ->
        identity[i] * (1f - strength) + filterArray[i] * strength
    }

    return ColorMatrix(blended)
}
