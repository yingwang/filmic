package com.yingwang.filmic.lut

/**
 * A color-grading preset. Applied to a bitmap via a 4x5 ColorMatrix plus optional
 * tone-curve and grain pass. Hand-tuned to approximate the overall mood of each
 * camera brand rather than faithfully reproduce their LUTs.
 */
data class Style(
    val id: String,
    val brand: Brand,
    val name: String,
    val description: String,
    val matrix: FloatArray = IDENTITY_MATRIX,
    val contrast: Float = 1f,
    val saturation: Float = 1f,
    val shadowLift: Float = 0f,
    val highlightRoll: Float = 0f,
    val grain: Float = 0f,
    val monochrome: Boolean = false,
    /** Optional path to a .cube LUT under `assets/`. When set, takes precedence over [matrix]. */
    val cubeAsset: String? = null,
) {
    enum class Brand(val display: String) {
        Hasselblad("Hasselblad"),
        Fujifilm("Fujifilm"),
        Leica("Leica"),
    }

    companion object {
        val IDENTITY_MATRIX = floatArrayOf(
            1f, 0f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f,
        )
    }
}
