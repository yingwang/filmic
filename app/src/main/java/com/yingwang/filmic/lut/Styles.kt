package com.yingwang.filmic.lut

import com.yingwang.filmic.lut.Style.Brand

/**
 * Built-in presets.
 *
 * Matrix-based styles are hand-tuned approximations. Cube-based styles pull a
 * bundled .cube LUT from `assets/lut/` — those are generated with non-linear
 * operations (split-tone, S-curve, selective desat) that a 3×4 matrix can't
 * reproduce.
 */
object Styles {

    // region Matrix presets

    val Hasselblad_Natural = Style(
        id = "hb_natural",
        brand = Brand.Hasselblad,
        name = "Natural Colour",
        description = "清雅、肤色准、高光从容。",
        matrix = floatArrayOf(
            1.04f, 0.02f, 0.00f, 0f, 2f,
            0.00f, 1.02f, 0.01f, 0f, 1f,
            0.00f, 0.00f, 0.98f, 0f, -2f,
            0f, 0f, 0f, 1f, 0f,
        ),
        contrast = 1.05f,
        saturation = 0.96f,
        shadowLift = 0.02f,
        highlightRoll = 0.04f,
    )

    val Fuji_ClassicChrome = Style(
        id = "fj_classic_chrome",
        brand = Brand.Fujifilm,
        name = "Classic Chrome",
        description = "灰调、抬影、青橙分离。",
        matrix = floatArrayOf(
            0.98f, 0.04f, 0.02f, 0f, -4f,
            0.02f, 0.94f, 0.04f, 0f, -2f,
            0.00f, 0.02f, 0.92f, 0f, 4f,
            0f, 0f, 0f, 1f, 0f,
        ),
        contrast = 1.12f,
        saturation = 0.82f,
        shadowLift = 0.08f,
        highlightRoll = 0.06f,
    )

    val Fuji_Velvia = Style(
        id = "fj_velvia",
        brand = Brand.Fujifilm,
        name = "Velvia",
        description = "浓郁、反差大、绿红尤盛。",
        matrix = floatArrayOf(
            1.10f, -0.04f, -0.02f, 0f, 2f,
            -0.02f, 1.08f, -0.04f, 0f, 0f,
            -0.04f, -0.02f, 1.06f, 0f, 4f,
            0f, 0f, 0f, 1f, 0f,
        ),
        contrast = 1.15f,
        saturation = 1.22f,
        shadowLift = 0f,
        highlightRoll = 0.02f,
    )

    val Fuji_Acros = Style(
        id = "fj_acros",
        brand = Brand.Fujifilm,
        name = "Acros",
        description = "细腻黑白、微粒精致。",
        matrix = floatArrayOf(
            0.25f, 0.62f, 0.13f, 0f, 0f,
            0.25f, 0.62f, 0.13f, 0f, 0f,
            0.25f, 0.62f, 0.13f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f,
        ),
        contrast = 1.12f,
        saturation = 0f,
        shadowLift = 0.02f,
        highlightRoll = 0.06f,
        grain = 0.06f,
        monochrome = true,
    )

    val Leica_Standard = Style(
        id = "lc_standard",
        brand = Brand.Leica,
        name = "Leica Standard",
        description = "克制、微暖、层次见内。",
        matrix = floatArrayOf(
            1.02f, 0.00f, 0.00f, 0f, 4f,
            0.00f, 1.00f, 0.00f, 0f, 1f,
            0.00f, 0.00f, 0.96f, 0f, -3f,
            0f, 0f, 0f, 1f, 0f,
        ),
        contrast = 1.08f,
        saturation = 1.02f,
        shadowLift = 0.01f,
        highlightRoll = 0.05f,
    )

    val Leica_Monochrom = Style(
        id = "lc_mono",
        brand = Brand.Leica,
        name = "Monochrom",
        description = "黑白、深黑、银盐颗粒。",
        matrix = floatArrayOf(
            0.30f, 0.59f, 0.11f, 0f, 0f,
            0.30f, 0.59f, 0.11f, 0f, 0f,
            0.30f, 0.59f, 0.11f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f,
        ),
        contrast = 1.18f,
        saturation = 0f,
        shadowLift = -0.02f,
        highlightRoll = 0.03f,
        grain = 0.12f,
        monochrome = true,
    )

    // endregion

    // region Cube-based presets

    val Hasselblad_XPan = Style(
        id = "hb_xpan",
        brand = Brand.Hasselblad,
        name = "XPan",
        description = "泛广—冷影暖高，褪色胶片。",
        cubeAsset = "lut/hasselblad_xpan.cube",
        grain = 0.04f,
    )

    val Fuji_Astia = Style(
        id = "fj_astia",
        brand = Brand.Fujifilm,
        name = "Astia",
        description = "柔和、肤色温润、淡彩。",
        cubeAsset = "lut/fuji_astia.cube",
    )

    val Leica_Chrome = Style(
        id = "lc_chrome",
        brand = Brand.Leica,
        name = "Chrome",
        description = "浓郁反差、暖中冷影、红更沉。",
        cubeAsset = "lut/leica_chrome.cube",
    )

    // endregion

    val all: List<Style> = listOf(
        Hasselblad_Natural,
        Hasselblad_XPan,
        Fuji_ClassicChrome,
        Fuji_Velvia,
        Fuji_Astia,
        Fuji_Acros,
        Leica_Standard,
        Leica_Chrome,
        Leica_Monochrom,
    )

    fun byId(id: String): Style? = all.firstOrNull { it.id == id }
}
