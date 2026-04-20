package com.yingwang.filmic.settings

import android.content.Context
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
 * Only JPEG quality (60..100). Resolution is always kept at the source's native
 * size — we decided against a pixel cap because the usual reason to want
 * smaller files is file size, and quality controls that directly.
 */
data class ExportSettings(
    val jpegQuality: Int = 94,
) {
    companion object {
        const val MIN_QUALITY = 60
        const val MAX_QUALITY = 100
    }
}

private const val PREFS = "export_settings"
private const val KEY_QUALITY = "jpeg_quality"

fun ExportSettings.Companion.load(context: Context): ExportSettings {
    val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    return ExportSettings(
        jpegQuality = prefs.getInt(KEY_QUALITY, 94).coerceIn(MIN_QUALITY, MAX_QUALITY),
    )
}

fun ExportSettings.save(context: Context) {
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        .putInt(KEY_QUALITY, jpegQuality)
        .apply()
}

@Composable
fun rememberExportSettings(): MutableState<ExportSettings> {
    val context = LocalContext.current
    val state = remember { mutableStateOf(ExportSettings.load(context)) }
    LaunchedEffect(state) {
        snapshotFlow { state.value }.collect { it.save(context) }
    }
    return state
}
