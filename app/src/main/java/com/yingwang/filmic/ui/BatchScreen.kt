package com.yingwang.filmic.ui

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.exifinterface.media.ExifInterface
import coil.compose.AsyncImage
import com.yingwang.filmic.R
import com.yingwang.filmic.lut.LutProcessor
import com.yingwang.filmic.lut.Style
import com.yingwang.filmic.lut.Styles
import com.yingwang.filmic.settings.ExportSettings
import com.yingwang.filmic.settings.rememberExportSettings
import java.io.OutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Pick many photos, apply a single style to all, save to the gallery with a
 * per-item progress meter.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun BatchScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val items = remember { mutableStateListOf<Uri>() }
    var selectedStyle by remember { mutableStateOf(Styles.all.first()) }
    var processingIndex by remember { mutableStateOf<Int?>(null) }
    var settingsOn by remember { mutableStateOf(false) }
    val exportSettings = rememberExportSettings()
    val results = remember { mutableStateListOf<BatchItemState>() }
    val running by remember { derivedStateOf { processingIndex != null } }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = 50),
    ) { uris ->
        items.clear()
        results.clear()
        items.addAll(uris)
        results.addAll(uris.map { BatchItemState.Pending })
    }

    LaunchedEffect(Unit) {
        if (items.isEmpty()) {
            picker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
            )
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.batch_title, items.size).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { settingsOn = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Export settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
        ) {
            if (items.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.batch_no_selection),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    items(items.size) { i ->
                        val state = results.getOrNull(i) ?: BatchItemState.Pending
                        BatchTile(
                            uri = items[i],
                            state = state,
                            isCurrent = processingIndex == i,
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
                Text(
                    text = stringResource(R.string.batch_style_heading).uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(end = 24.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    items(Styles.all, key = { it.id }) { s ->
                        BatchStyleChip(
                            style = s,
                            selected = s.id == selectedStyle.id,
                            onClick = { selectedStyle = s },
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))

                if (running) {
                    val done = results.count { it is BatchItemState.Done || it is BatchItemState.Failed }
                    LinearProgressIndicator(
                        progress = { done.toFloat() / items.size.coerceAtLeast(1) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.batch_processing, done, items.size),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 16.dp),
            ) {
                Button(
                    enabled = items.isNotEmpty() && !running,
                    onClick = {
                        picker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                    shape = RoundedCornerShape(2.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    modifier = Modifier.weight(1f).height(52.dp),
                ) {
                    Text(stringResource(R.string.batch_reselect), style = MaterialTheme.typography.labelLarge)
                }

                Button(
                    enabled = items.isNotEmpty() && !running,
                    onClick = {
                        scope.launch {
                            for (i in items.indices) results[i] = BatchItemState.Pending
                            for (i in items.indices) {
                                processingIndex = i
                                val ok = withContext(Dispatchers.IO) {
                                    processOne(context, items[i], selectedStyle, exportSettings.value)
                                }
                                results[i] = if (ok) BatchItemState.Done else BatchItemState.Failed
                            }
                            processingIndex = null
                            val good = results.count { it is BatchItemState.Done }
                            Toast.makeText(
                                context,
                                context.getString(R.string.batch_toast_done, good, items.size),
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    },
                    shape = RoundedCornerShape(2.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    modifier = Modifier.weight(1f).height(52.dp),
                ) {
                    Text(
                        text = stringResource(
                            if (running) R.string.batch_export_running else R.string.batch_export_all,
                        ),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }

    if (settingsOn) {
        ExportSettingsDialog(
            settings = exportSettings.value,
            onChange = { exportSettings.value = it },
            onDismiss = { settingsOn = false },
        )
    }
}

private sealed class BatchItemState {
    data object Pending : BatchItemState()
    data object Done : BatchItemState()
    data object Failed : BatchItemState()
}

@Composable
private fun BatchTile(uri: Uri, state: BatchItemState, isCurrent: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(2.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        AsyncImage(
            model = uri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        val overlay: Color? = when (state) {
            BatchItemState.Done -> Color(0x803A8C3F)
            BatchItemState.Failed -> Color(0x80B33F3F)
            BatchItemState.Pending -> if (isCurrent) Color(0x60FFFFFF) else null
        }
        if (overlay != null) {
            Box(modifier = Modifier.fillMaxSize().background(overlay))
        }
        val tag = when (state) {
            BatchItemState.Done -> "✓"
            BatchItemState.Failed -> "×"
            BatchItemState.Pending -> if (isCurrent) "…" else null
        }
        if (tag != null) {
            Text(
                text = tag,
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier
                    .align(Alignment.Center),
            )
        }
    }
}

@Composable
private fun BatchStyleChip(style: Style, selected: Boolean, onClick: () -> Unit) {
    val border = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(2.dp))
            .border(if (selected) 1.5.dp else 1.dp, border, RoundedCornerShape(2.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Box(modifier = Modifier.size(10.dp).background(chipColorFor(style), RoundedCornerShape(1.dp)))
        Spacer(Modifier.width(8.dp))
        Text(
            text = style.name.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun chipColorFor(style: Style): Color = when (style.brand) {
    Style.Brand.Hasselblad -> Color(0xFFE8DCC5)
    Style.Brand.Fujifilm -> if (style.id == "fj_velvia") Color(0xFF5E8C3B) else Color(0xFFB7A07E)
    Style.Brand.Leica -> if (style.monochrome) Color(0xFF1A1A1A) else Color(0xFFC8102E)
}

private fun processOne(context: Context, uri: Uri, style: Style, settings: ExportSettings): Boolean {
    val raw = decodeOriented(context, uri) ?: return false
    val styled = LutProcessor.apply(raw, style, context)
    raw.recycle()
    val saved = saveBitmap(context, styled, style, settings)
    styled.recycle()
    return saved
}

private fun decodeOriented(context: Context, uri: Uri): Bitmap? {
    val resolver = context.contentResolver
    val raw = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) } ?: return null
    val orientation = resolver.openInputStream(uri)?.use {
        ExifInterface(it).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL,
        )
    } ?: ExifInterface.ORIENTATION_NORMAL
    val m = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> m.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> m.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> m.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> m.postScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> m.postScale(1f, -1f)
        else -> return raw
    }
    val rotated = Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, m, true)
    if (rotated != raw) raw.recycle()
    return rotated
}

private fun saveBitmap(context: Context, bitmap: Bitmap, style: Style, settings: ExportSettings): Boolean {
    val fileName = "Filmic_${style.id}_${System.currentTimeMillis()}.jpg"
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Filmic")
        }
    }
    val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
    return try {
        context.contentResolver.openOutputStream(uri)?.use { out: OutputStream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, settings.jpegQuality, out)
        }
        true
    } catch (t: Throwable) {
        false
    }
}
