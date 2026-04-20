package com.yingwang.filmic.lut

import android.content.Context
import java.util.concurrent.ConcurrentHashMap

/**
 * Parses and caches .cube LUTs loaded from the app's `assets/` directory.
 * One entry per asset path — LUTs are effectively immutable.
 */
object LutCache {
    private val cache = ConcurrentHashMap<String, CubeLut>()

    fun load(context: Context, assetPath: String): CubeLut =
        cache.getOrPut(assetPath) {
            context.assets.open(assetPath).use(CubeParser::parse)
        }

    fun preload(context: Context, assetPaths: Collection<String>) {
        assetPaths.forEach { runCatching { load(context, it) } }
    }
}
