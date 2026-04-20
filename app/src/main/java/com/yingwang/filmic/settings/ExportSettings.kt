package com.yingwang.filmic.settings

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext

/**
 * Export pipeline tuning — applied at save/share time, not to the live preview.
 *
 * - [jpegQuality]: 60..100, controls `Bitmap.compress` quality.
 * - [maxLongEdge]: 0 means keep the source's native resolution. Any positive
 *   value caps the long edge (aspect ratio preserved).
 */
data class ExportSettings(
    val jpegQuality: Int = 94,
    val maxLongEdge: Int = 0,
) {
    companion object {
        val SIZE_OPTIONS = listOf(
            0 to "原始",
            4000 to "4000 px",
            2000 to "2000 px",
            1000 to "1000 px",
        )
        const val MIN_QUALITY = 60
        const val MAX_QUALITY = 100
    }
}

private const val PREFS = "export_settings"
private const val KEY_QUALITY = "jpeg_quality"
private const val KEY_SIZE = "max_long_edge"

fun ExportSettings.Companion.load(context: Context): ExportSettings {
    val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    return ExportSettings(
        jpegQuality = prefs.getInt(KEY_QUALITY, 94).coerceIn(MIN_QUALITY, MAX_QUALITY),
        maxLongEdge = prefs.getInt(KEY_SIZE, 0).coerceAtLeast(0),
    )
}

fun ExportSettings.save(context: Context) {
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        .putInt(KEY_QUALITY, jpegQuality)
        .putInt(KEY_SIZE, maxLongEdge)
        .apply()
}

/**
 * Compose helper: hold [ExportSettings] in memory, auto-persist on change.
 */
@Composable
fun rememberExportSettings(): MutableState<ExportSettings> {
    val context = LocalContext.current
    val state = remember { mutableStateOf(ExportSettings.load(context)) }
    LaunchedEffect(state) {
        snapshotFlow { state.value }.collect { it.save(context) }
    }
    return state
}

/**
 * Returns a bitmap scaled so its long edge ≤ [maxLongEdge]. If [maxLongEdge] is
 * 0 or the source is already small enough, returns the input unchanged.
 */
fun Bitmap.scaleForExport(maxLongEdge: Int): Bitmap {
    if (maxLongEdge <= 0) return this
    val long = maxOf(width, height)
    if (long <= maxLongEdge) return this
    val scale = maxLongEdge.toFloat() / long
    val w = (width * scale).toInt().coerceAtLeast(1)
    val h = (height * scale).toInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(this, w, h, true)
}
