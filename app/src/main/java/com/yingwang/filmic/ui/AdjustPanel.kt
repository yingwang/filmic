package com.yingwang.filmic.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yingwang.filmic.lut.Adjustments
import kotlin.math.roundToInt

/**
 * Two tabs: Tone (exposure / contrast / saturation / temp / tint) and HSL
 * (per-band hue/sat/lum). Each slider emits a continuous Adjustments update so
 * the preview can re-process live.
 */
@Composable
fun AdjustPanel(
    adjustments: Adjustments,
    onChange: (Adjustments) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var tab by remember { mutableStateOf(Tab.Tone) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TabHeader("TONE", tab == Tab.Tone) { tab = Tab.Tone }
            Spacer(Modifier.size(20.dp))
            TabHeader("HSL", tab == Tab.Hsl) { tab = Tab.Hsl }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onReset) {
                Text(
                    "重置",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        when (tab) {
            Tab.Tone -> ToneTab(adjustments, onChange)
            Tab.Hsl -> HslTab(adjustments, onChange)
        }
    }
}

private enum class Tab { Tone, Hsl }

@Composable
private fun TabHeader(label: String, selected: Boolean, onClick: () -> Unit) {
    val color = if (selected) MaterialTheme.colorScheme.onSurface
    else MaterialTheme.colorScheme.onSurfaceVariant
    Box(modifier = Modifier.clickable(onClick = onClick)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = color)
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .height(2.dp)
                    .size(width = 24.dp, height = 2.dp)
                    .background(if (selected) MaterialTheme.colorScheme.secondary else Color.Transparent),
            )
        }
    }
}

@Composable
private fun ToneTab(adj: Adjustments, onChange: (Adjustments) -> Unit) {
    AdjSlider("曝光", adj.exposure) { onChange(adj.copy(exposure = it)) }
    AdjSlider("对比", adj.contrast) { onChange(adj.copy(contrast = it)) }
    AdjSlider("饱和", adj.saturation) { onChange(adj.copy(saturation = it)) }
    AdjSlider("色温", adj.temperature) { onChange(adj.copy(temperature = it)) }
    AdjSlider("色调", adj.tint) { onChange(adj.copy(tint = it)) }
}

@Composable
private fun HslTab(adj: Adjustments, onChange: (Adjustments) -> Unit) {
    var band by remember { mutableStateOf(0) }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(end = 24.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        items(Adjustments.BAND_NAMES.size) { i ->
            BandChip(
                label = Adjustments.BAND_NAMES[i],
                color = bandColor(i),
                selected = i == band,
                onClick = { band = i },
            )
        }
    }
    Spacer(Modifier.height(12.dp))
    AdjSlider("色相", adj.hueShifts[band]) { v ->
        onChange(adj.copy(hueShifts = adj.hueShifts.replace(band, v)))
    }
    AdjSlider("饱和", adj.satShifts[band]) { v ->
        onChange(adj.copy(satShifts = adj.satShifts.replace(band, v)))
    }
    AdjSlider("明度", adj.lumShifts[band]) { v ->
        onChange(adj.copy(lumShifts = adj.lumShifts.replace(band, v)))
    }
}

@Composable
private fun BandChip(label: String, color: Color, selected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(2.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Box(modifier = Modifier.size(10.dp).background(color, RoundedCornerShape(1.dp)))
        Spacer(Modifier.size(width = 6.dp, height = 0.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun AdjSlider(label: String, value: Float, onChange: (Float) -> Unit) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.weight(1f))
            Text(
                text = "%+d".format((value * 100).roundToInt()),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.End,
            )
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = -1f..1f,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.onSurface,
                activeTrackColor = MaterialTheme.colorScheme.onSurface,
                inactiveTrackColor = MaterialTheme.colorScheme.outline,
            ),
        )
    }
}

private fun bandColor(i: Int): Color = when (i) {
    0 -> Color(0xFFE03A3A) // red
    1 -> Color(0xFFE08A3A) // orange
    2 -> Color(0xFFE0D03A) // yellow
    3 -> Color(0xFF4FAE3E) // green
    4 -> Color(0xFF3EB0AE) // aqua
    5 -> Color(0xFF3A6BE0) // blue
    6 -> Color(0xFF8E3AE0) // purple
    7 -> Color(0xFFE03ABF) // magenta
    else -> Color.Gray
}

private fun FloatArray.replace(index: Int, value: Float): FloatArray {
    val copy = copyOf()
    copy[index] = value
    return copy
}
