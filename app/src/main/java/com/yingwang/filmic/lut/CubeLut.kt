package com.yingwang.filmic.lut

import kotlin.math.max
import kotlin.math.min

/**
 * A 3D lookup table stored as a flat RGB float grid of size N³.
 *
 * Indexing order follows the Adobe .cube spec: R is the innermost axis, G middle,
 * B outermost. `data[0..2]` is the entry at (R=0, G=0, B=0); stride between R
 * entries is 3, between G is 3*N, between B is 3*N*N.
 */
class CubeLut(
    val size: Int,
    val data: FloatArray,
    val domainMin: FloatArray = floatArrayOf(0f, 0f, 0f),
    val domainMax: FloatArray = floatArrayOf(1f, 1f, 1f),
) {
    init {
        require(size in 2..65) { "LUT_3D_SIZE must be 2..65, got $size" }
        require(data.size == size * size * size * 3) { "data size ${data.size} != ${size * size * size * 3}" }
    }

    /** Samples the LUT at normalised (r,g,b) in [0,1] via trilinear interpolation. */
    fun sample(r: Float, g: Float, b: Float, out: FloatArray) {
        val nr = remap(r, 0) * (size - 1)
        val ng = remap(g, 1) * (size - 1)
        val nb = remap(b, 2) * (size - 1)

        val r0 = nr.toInt().coerceIn(0, size - 1)
        val g0 = ng.toInt().coerceIn(0, size - 1)
        val b0 = nb.toInt().coerceIn(0, size - 1)
        val r1 = min(r0 + 1, size - 1)
        val g1 = min(g0 + 1, size - 1)
        val b1 = min(b0 + 1, size - 1)

        val fr = nr - r0
        val fg = ng - g0
        val fb = nb - b0

        // Fetch 8 corners and blend. Index = (b * N * N + g * N + r) * 3.
        val n = size
        fun idx(rr: Int, gg: Int, bb: Int) = (bb * n * n + gg * n + rr) * 3

        val c000 = idx(r0, g0, b0)
        val c100 = idx(r1, g0, b0)
        val c010 = idx(r0, g1, b0)
        val c110 = idx(r1, g1, b0)
        val c001 = idx(r0, g0, b1)
        val c101 = idx(r1, g0, b1)
        val c011 = idx(r0, g1, b1)
        val c111 = idx(r1, g1, b1)

        for (c in 0..2) {
            val v00 = lerp(data[c000 + c], data[c100 + c], fr)
            val v10 = lerp(data[c010 + c], data[c110 + c], fr)
            val v01 = lerp(data[c001 + c], data[c101 + c], fr)
            val v11 = lerp(data[c011 + c], data[c111 + c], fr)
            val v0 = lerp(v00, v10, fg)
            val v1 = lerp(v01, v11, fg)
            out[c] = lerp(v0, v1, fb).coerceIn(0f, 1f)
        }
    }

    private fun remap(v: Float, axis: Int): Float {
        val lo = domainMin[axis]
        val hi = domainMax[axis]
        if (hi == lo) return 0f
        return ((v - lo) / (hi - lo)).coerceIn(0f, 1f)
    }

    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
}
