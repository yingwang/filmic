package com.yingwang.filmic.lut

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import kotlin.math.pow
import kotlin.random.Random

/**
 * Applies a [Style] to a source bitmap.
 *
 * If [Style.cubeAsset] is set, the style is applied as a 3D LUT via trilinear
 * interpolation. Otherwise we take the matrix path:
 *   1. Brand color matrix (channel mixer + white-point shift)
 *   2. Saturation
 *   3. Contrast around mid-grey
 *   4. Per-pixel tone curve (shadow lift + highlight roll-off)
 *   5. Monochrome grain
 *
 * Steps 1–3 compose into a single ColorMatrix. Step 4 is a 256-entry CPU LUT.
 * Step 5 is additive noise for monochrome-only styles.
 */
object LutProcessor {

    fun apply(source: Bitmap, style: Style, context: Context? = null): Bitmap {
        if (style.cubeAsset != null && context != null) {
            val cube = LutCache.load(context, style.cubeAsset)
            return applyCube(source, cube, style)
        }
        return applyMatrix(source, style)
    }

    // region Matrix path

    private fun applyMatrix(source: Bitmap, style: Style): Bitmap {
        val out = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)

        val matrix = ColorMatrix(style.matrix)
        matrix.postConcat(saturationMatrix(style.saturation))
        matrix.postConcat(contrastMatrix(style.contrast))

        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(matrix)
            isAntiAlias = false
            isFilterBitmap = true
        }
        Canvas(out).drawBitmap(source, 0f, 0f, paint)

        if (style.shadowLift != 0f || style.highlightRoll != 0f) {
            applyToneCurve(out, style.shadowLift, style.highlightRoll)
        }
        if (style.grain > 0f) applyGrain(out, style.grain)
        return out
    }

    private fun saturationMatrix(s: Float): ColorMatrix = ColorMatrix().apply { setSaturation(s) }

    private fun contrastMatrix(c: Float): ColorMatrix {
        val t = 128f * (1f - c)
        return ColorMatrix(
            floatArrayOf(
                c, 0f, 0f, 0f, t,
                0f, c, 0f, 0f, t,
                0f, 0f, c, 0f, t,
                0f, 0f, 0f, 1f, 0f,
            )
        )
    }

    private fun applyToneCurve(bmp: Bitmap, lift: Float, roll: Float) {
        val lut = IntArray(256)
        for (i in 0..255) {
            val x = i / 255f
            var y = x.pow(1f - lift.coerceIn(-0.2f, 0.2f))
            if (roll > 0f && y > 0.7f) {
                val over = (y - 0.7f) / 0.3f
                y = 0.7f + (0.3f - roll) * (1f - (1f - over).pow(2f))
            }
            lut[i] = (y.coerceIn(0f, 1f) * 255f).toInt()
        }
        val w = bmp.width
        val h = bmp.height
        val pixels = IntArray(w * h)
        bmp.getPixels(pixels, 0, w, 0, 0, w, h)
        for (i in pixels.indices) {
            val p = pixels[i]
            val a = (p ushr 24) and 0xFF
            val r = lut[(p ushr 16) and 0xFF]
            val g = lut[(p ushr 8) and 0xFF]
            val b = lut[p and 0xFF]
            pixels[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
        }
        bmp.setPixels(pixels, 0, w, 0, 0, w, h)
    }

    private fun applyGrain(bmp: Bitmap, amount: Float) {
        val rand = Random(bmp.width * 73856093L xor bmp.height * 19349663L)
        val strength = (amount.coerceIn(0f, 1f) * 48f).toInt()
        if (strength <= 0) return
        val w = bmp.width
        val h = bmp.height
        val pixels = IntArray(w * h)
        bmp.getPixels(pixels, 0, w, 0, 0, w, h)
        for (i in pixels.indices) {
            val p = pixels[i]
            val n = rand.nextInt(-strength, strength + 1)
            val a = (p ushr 24) and 0xFF
            val r = ((p ushr 16) and 0xFF) + n
            val g = ((p ushr 8) and 0xFF) + n
            val b = (p and 0xFF) + n
            pixels[i] = (a shl 24) or
                (r.coerceIn(0, 255) shl 16) or
                (g.coerceIn(0, 255) shl 8) or
                b.coerceIn(0, 255)
        }
        bmp.setPixels(pixels, 0, w, 0, 0, w, h)
    }

    // endregion

    // region Cube path

    private fun applyCube(source: Bitmap, cube: CubeLut, style: Style): Bitmap {
        val w = source.width
        val h = source.height
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(w * h)
        source.getPixels(pixels, 0, w, 0, 0, w, h)
        val tmp = FloatArray(3)
        for (i in pixels.indices) {
            val p = pixels[i]
            val a = (p ushr 24) and 0xFF
            val r = ((p ushr 16) and 0xFF) / 255f
            val g = ((p ushr 8) and 0xFF) / 255f
            val b = (p and 0xFF) / 255f
            cube.sample(r, g, b, tmp)
            val or = (tmp[0] * 255f).toInt().coerceIn(0, 255)
            val og = (tmp[1] * 255f).toInt().coerceIn(0, 255)
            val ob = (tmp[2] * 255f).toInt().coerceIn(0, 255)
            pixels[i] = (a shl 24) or (or shl 16) or (og shl 8) or ob
        }
        out.setPixels(pixels, 0, w, 0, 0, w, h)
        if (style.grain > 0f) applyGrain(out, style.grain)
        return out
    }

    // endregion
}
