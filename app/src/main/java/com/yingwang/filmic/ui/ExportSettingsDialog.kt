package com.yingwang.filmic.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.yingwang.filmic.R
import com.yingwang.filmic.settings.ExportSettings

@Composable
fun ExportSettingsDialog(
    settings: ExportSettings,
    onChange: (ExportSettings) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_done), style = MaterialTheme.typography.labelLarge)
            }
        },
        title = {
            Text(
                text = stringResource(R.string.export_settings_title),
                style = MaterialTheme.typography.headlineMedium,
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.export_quality_label, settings.jpegQuality),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Slider(
                    value = settings.jpegQuality.toFloat(),
                    onValueChange = { onChange(settings.copy(jpegQuality = it.toInt())) },
                    valueRange = ExportSettings.MIN_QUALITY.toFloat()..ExportSettings.MAX_QUALITY.toFloat(),
                    steps = ExportSettings.MAX_QUALITY - ExportSettings.MIN_QUALITY - 1,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.onSurface,
                        activeTrackColor = MaterialTheme.colorScheme.onSurface,
                        inactiveTrackColor = MaterialTheme.colorScheme.outline,
                    ),
                )
            }
        },
    )
}
