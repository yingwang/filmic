package com.yingwang.filmic.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

/**
 * Stacks [original] under [processed] and reveals the original to the left of a
 * draggable vertical handle. Both bitmaps are drawn at [ContentScale.Crop] so
 * the crops match exactly across the divide.
 */
@Composable
fun CompareView(
    original: ImageBitmap,
    processed: ImageBitmap,
    modifier: Modifier = Modifier,
) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    var splitPx by remember { mutableFloatStateOf(-1f) }

    Box(
        modifier = modifier.onSizeChanged {
            size = it
            if (splitPx < 0f && it.width > 0) splitPx = it.width / 2f
        },
    ) {
        Image(
            bitmap = processed,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Image(
            bitmap = original,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    val w = splitPx.coerceAtLeast(0f).coerceAtMost(this.size.width)
                    clipRect(left = 0f, top = 0f, right = w, bottom = this.size.height) {
                        this@drawWithContent.drawContent()
                    }
                },
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(size.width) {
                    detectDragGestures { change, _ ->
                        splitPx = change.position.x.coerceIn(0f, size.width.toFloat())
                    }
                }
                .drawWithContent {
                    val x = splitPx.coerceAtLeast(0f).coerceAtMost(this.size.width)
                    drawRect(
                        color = Color.White,
                        topLeft = Offset(x - 1f, 0f),
                        size = Size(2f, this.size.height),
                    )
                    val cy = this.size.height / 2f
                    drawCircle(color = Color.White, radius = 18f, center = Offset(x, cy))
                    drawCircle(color = Color(0xFF1A1A1A), radius = 14f, center = Offset(x, cy))
                    drawCircle(color = Color.White, radius = 4f, center = Offset(x, cy))
                },
        )

        Tag("ORIGINAL", Alignment.TopStart)
        Tag("FILMIC", Alignment.TopEnd)
    }
}

@Composable
private fun Tag(text: String, alignment: Alignment) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        contentAlignment = alignment,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            modifier = Modifier
                .background(Color(0xAA000000))
                .padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}
