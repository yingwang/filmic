package com.yingwang.filmic.lut

import android.graphics.Bitmap
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Applies [Adjustments] to a bitmap in place. Designed to run after the style
 * pass — the input is expected to already be in display-referred sRGB.
 *
 * Order of operations follows the Lightroom Develop module convention:
 *   1. Exposure
 *   2. Contrast (around mid-grey)
 *   3. Temperature / tint (RGB scaling)
 *   4. Global saturation
 *   5. Per-band HSL (hue / sat / luminance)
 */
object AdjustmentProcessor {

    fun applyInPlace(bitmap: Bitmap, adj: Adjustments) {
        if (adj.isIdentity) return

        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        val expGain = 2f.pow(adj.exposure * 2f)               // ±2 EV
        val contrast = 1f + adj.contrast * 0.5f               // 0.5..1.5
        val tempR = 1f + adj.temperature * 0.18f
        val tempB = 1f - adj.temperature * 0.18f
        val tintG = 1f - adj.tint * 0.12f
        val tintRB = 1f + adj.tint * 0.06f
        val satGain = 1f + adj.saturation                     // 0..2
        val hslActive = adj.hueShifts.any { it != 0f } ||
            adj.satShifts.any { it != 0f } ||
            adj.lumShifts.any { it != 0f }

        val hsl = FloatArray(3)
        val rgb = FloatArray(3)

        for (i in pixels.indices) {
            val p = pixels[i]
            val a = (p ushr 24) and 0xFF
            var r = ((p ushr 16) and 0xFF) / 255f
            var g = ((p ushr 8) and 0xFF) / 255f
            var b = (p and 0xFF) / 255f

            // 1. Exposure
            r *= expGain; g *= expGain; b *= expGain

            // 2. Contrast around 0.5
            r = (r - 0.5f) * contrast + 0.5f
            g = (g - 0.5f) * contrast + 0.5f
            b = (b - 0.5f) * contrast + 0.5f

            // 3. Temp / tint
            r *= tempR * tintRB
            g *= tintG
            b *= tempB * tintRB

            // 4. Global saturation around luma
            val l = 0.299f * r + 0.587f * g + 0.114f * b
            r = l + (r - l) * satGain
            g = l + (g - l) * satGain
            b = l + (b - l) * satGain

            // 5. HSL bands
            if (hslActive) {
                rgbToHsl(r, g, b, hsl)
                applyHsl(hsl, adj)
                hslToRgb(hsl[0], hsl[1], hsl[2], rgb)
                r = rgb[0]; g = rgb[1]; b = rgb[2]
            }

            val ir = (r * 255f).toInt().coerceIn(0, 255)
            val ig = (g * 255f).toInt().coerceIn(0, 255)
            val ib = (b * 255f).toInt().coerceIn(0, 255)
            pixels[i] = (a shl 24) or (ir shl 16) or (ig shl 8) or ib
        }
        bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
    }

    private fun applyHsl(hsl: FloatArray, adj: Adjustments) {
        val hue = hsl[0]
        val bandSize = 360f / Adjustments.BAND_COUNT
        // Find the two nearest band centres and blend.
        val pos = hue / bandSize
        val lower = pos.toInt() % Adjustments.BAND_COUNT
        val upper = (lower + 1) % Adjustments.BAND_COUNT
        val t = pos - pos.toInt()

        val hShift = lerp(adj.hueShifts[lower], adj.hueShifts[upper], t) * 30f
        val sShift = lerp(adj.satShifts[lower], adj.satShifts[upper], t)
        val lShift = lerp(adj.lumShifts[lower], adj.lumShifts[upper], t)

        // Only act on pixels with meaningful chroma so HSL tweaks don't tint neutrals.
        val sWeight = hsl[1]
        hsl[0] = (hsl[0] + hShift * sWeight + 360f) % 360f
        hsl[1] = (hsl[1] * (1f + sShift)).coerceIn(0f, 1f)
        hsl[2] = (hsl[2] + lShift * 0.4f * sWeight).coerceIn(0f, 1f)
    }

    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

    private fun rgbToHsl(r: Float, g: Float, b: Float, out: FloatArray) {
        val max = max(r, max(g, b))
        val min = min(r, min(g, b))
        val l = (max + min) / 2f
        val d = max - min
        val s = when {
            d == 0f -> 0f
            l < 0.5f -> d / (max + min).coerceAtLeast(1e-6f)
            else -> d / (2f - max - min).coerceAtLeast(1e-6f)
        }
        val h = when {
            d == 0f -> 0f
            max == r -> 60f * (((g - b) / d) % 6f)
            max == g -> 60f * (((b - r) / d) + 2f)
            else -> 60f * (((r - g) / d) + 4f)
        }
        out[0] = (h + 360f) % 360f
        out[1] = s.coerceIn(0f, 1f)
        out[2] = l.coerceIn(0f, 1f)
    }

    private fun hslToRgb(h: Float, s: Float, l: Float, out: FloatArray) {
        if (s == 0f) {
            out[0] = l; out[1] = l; out[2] = l
            return
        }
        val q = if (l < 0.5f) l * (1f + s) else l + s - l * s
        val p = 2f * l - q
        out[0] = hueToRgb(p, q, h + 120f)
        out[1] = hueToRgb(p, q, h)
        out[2] = hueToRgb(p, q, h - 120f)
    }

    private fun hueToRgb(p: Float, q: Float, hueDeg: Float): Float {
        var h = ((hueDeg % 360f) + 360f) % 360f
        return when {
            h < 60f -> p + (q - p) * (h / 60f)
            h < 180f -> q
            h < 240f -> p + (q - p) * ((240f - h) / 60f)
            else -> p
        }
    }

}
