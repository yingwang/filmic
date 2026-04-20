package com.yingwang.filmic.lut

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

/**
 * Parses the Adobe Cube LUT specification (version 1.0).
 *
 * Supported directives: TITLE, LUT_3D_SIZE, DOMAIN_MIN, DOMAIN_MAX.
 * 1D LUT (LUT_1D_SIZE) and shaper LUTs are not supported.
 */
object CubeParser {

    fun parse(stream: InputStream): CubeLut = BufferedReader(InputStreamReader(stream)).use(::parse)

    fun parse(text: String): CubeLut = parse(BufferedReader(text.reader()))

    private fun parse(reader: BufferedReader): CubeLut {
        var size = -1
        val domainMin = floatArrayOf(0f, 0f, 0f)
        val domainMax = floatArrayOf(1f, 1f, 1f)
        val points = ArrayList<Float>(1024 * 3)

        reader.lineSequence().forEach { rawLine ->
            val line = rawLine.substringBefore('#').trim()
            if (line.isEmpty()) return@forEach
            val tokens = line.split(Regex("\\s+"))
            when (tokens[0].uppercase()) {
                "TITLE" -> Unit
                "LUT_3D_SIZE" -> size = tokens[1].toInt()
                "LUT_1D_SIZE" -> error("1D LUTs are not supported")
                "DOMAIN_MIN" -> {
                    domainMin[0] = tokens[1].toFloat()
                    domainMin[1] = tokens[2].toFloat()
                    domainMin[2] = tokens[3].toFloat()
                }
                "DOMAIN_MAX" -> {
                    domainMax[0] = tokens[1].toFloat()
                    domainMax[1] = tokens[2].toFloat()
                    domainMax[2] = tokens[3].toFloat()
                }
                else -> {
                    if (tokens.size >= 3) {
                        points += tokens[0].toFloat()
                        points += tokens[1].toFloat()
                        points += tokens[2].toFloat()
                    }
                }
            }
        }

        require(size > 0) { "LUT_3D_SIZE missing" }
        require(points.size == size * size * size * 3) {
            "Expected ${size * size * size} entries, got ${points.size / 3}"
        }

        return CubeLut(size, points.toFloatArray(), domainMin, domainMax)
    }
}
