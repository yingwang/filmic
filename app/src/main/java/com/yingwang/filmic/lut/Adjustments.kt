package com.yingwang.filmic.lut

/**
 * User-applied tweaks layered on top of a [Style]. All sliders are normalised
 * to [-1, +1]; identity is the all-zero state.
 *
 * Colour bands for HSL match Lightroom's eight: red, orange, yellow, green,
 * aqua, blue, purple, magenta — each centered every 45° on the hue wheel.
 */
data class Adjustments(
    val exposure: Float = 0f,    // -1..+1 → -2..+2 EV
    val contrast: Float = 0f,    // -1..+1 → 0.5..1.5×
    val saturation: Float = 0f,  // -1..+1 → 0..2×
    val temperature: Float = 0f, // -1..+1 → cool..warm
    val tint: Float = 0f,        // -1..+1 → green..magenta
    val hueShifts: FloatArray = FloatArray(BAND_COUNT),
    val satShifts: FloatArray = FloatArray(BAND_COUNT),
    val lumShifts: FloatArray = FloatArray(BAND_COUNT),
) {
    val isIdentity: Boolean
        get() = exposure == 0f && contrast == 0f && saturation == 0f &&
            temperature == 0f && tint == 0f &&
            hueShifts.all { it == 0f } && satShifts.all { it == 0f } && lumShifts.all { it == 0f }

    companion object {
        const val BAND_COUNT = 8
        val BAND_NAMES = listOf("红", "橙", "黄", "绿", "青", "蓝", "紫", "品")
    }
}
