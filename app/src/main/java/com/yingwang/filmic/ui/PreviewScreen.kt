package com.yingwang.filmic.ui

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import com.yingwang.filmic.R
import com.yingwang.filmic.io.MediaSaver
import com.yingwang.filmic.lut.Adjustments
import com.yingwang.filmic.lut.LutProcessor
import com.yingwang.filmic.lut.Style
import com.yingwang.filmic.lut.Styles
import com.yingwang.filmic.settings.ExportSettings
import com.yingwang.filmic.settings.rememberExportSettings
import java.io.File
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val PREVIEW_LONG_EDGE = 1600

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun PreviewScreen(
    sourceUri: Uri?,
    styleId: String?,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedStyle by remember { mutableStateOf(styleId?.let(Styles::byId) ?: Styles.all.first()) }
    var adjustments by remember { mutableStateOf(Adjustments()) }
    var preview by remember { mutableStateOf<Bitmap?>(null) }
    var processed by remember { mutableStateOf<Bitmap?>(null) }
    var compareOn by remember { mutableStateOf(false) }
    var adjustOn by remember { mutableStateOf(false) }
    var settingsOn by remember { mutableStateOf(false) }
    var exporting by remember { mutableStateOf(false) }
    val exportSettings = rememberExportSettings()

    LaunchedEffect(sourceUri) {
        preview = withContext(Dispatchers.IO) { sourceUri?.let { loadPreviewBitmap(context, it) } }
    }
    LaunchedEffect(preview, selectedStyle, adjustments) {
        val src = preview ?: return@LaunchedEffect
        processed = withContext(Dispatchers.Default) {
            LutProcessor.apply(src, selectedStyle, context, adjustments)
        }
    }
    val ready by remember { derivedStateOf { preview != null && processed != null } }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(selectedStyle.name.uppercase(), style = MaterialTheme.typography.titleMedium)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(enabled = ready, onClick = { adjustOn = !adjustOn }) {
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = "Adjust",
                            tint = if (adjustOn) MaterialTheme.colorScheme.secondary
                            else MaterialTheme.colorScheme.onBackground,
                        )
                    }
                    IconButton(enabled = ready, onClick = { compareOn = !compareOn }) {
                        Icon(
                            Icons.Default.CompareArrows,
                            contentDescription = "Compare",
                            tint = if (compareOn) MaterialTheme.colorScheme.secondary
                            else MaterialTheme.colorScheme.onBackground,
                        )
                    }
                    IconButton(onClick = { settingsOn = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Export settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
    ) { inner ->
        val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

        val previewBox: @Composable (Modifier) -> Unit = { mod ->
            Box(
                modifier = mod
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                val src = preview
                val out = processed
                when {
                    out == null -> CircularProgressIndicator(strokeWidth = 2.dp)
                    compareOn && src != null -> CompareView(
                        original = src.asImageBitmap(),
                        processed = out.asImageBitmap(),
                        modifier = Modifier.fillMaxSize(),
                    )
                    else -> Image(
                        bitmap = out.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        val stylesPanel: @Composable (Modifier) -> Unit = { mod ->
            if (adjustOn) {
                AdjustPanel(
                    adjustments = adjustments,
                    onChange = { adjustments = it },
                    onReset = { adjustments = Adjustments() },
                    modifier = mod,
                )
            } else {
                Column(modifier = mod.padding(horizontal = 24.dp)) {
                    Text(
                        text = "${selectedStyle.brand.display} · ${selectedStyle.localizedDescription()}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.styles_heading).uppercase(),
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
                            StyleChip(
                                style = s,
                                selected = s.id == selectedStyle.id,
                                onClick = { selectedStyle = s },
                            )
                        }
                    }
                }
            }
        }

        val actionRow: @Composable (Modifier) -> Unit = { mod ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = mod,
            ) {
                OutlinedButton(
                    enabled = ready && !exporting,
                    onClick = {
                        exporting = true
                        scope.launch {
                            val uri = withContext(Dispatchers.IO) {
                                val full = sourceUri?.let { loadFullBitmap(context, it) } ?: return@withContext null
                                val styled = LutProcessor.apply(full, selectedStyle, context, adjustments)
                                writeShareCache(context, styled, selectedStyle, exportSettings.value)
                            }
                            exporting = false
                            if (uri != null) startShare(context, uri)
                            else Toast.makeText(
                                context,
                                context.getString(R.string.toast_share_failed),
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    },
                    shape = RoundedCornerShape(2.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.share).uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                Button(
                    enabled = ready && !exporting,
                    onClick = {
                        exporting = true
                        scope.launch {
                            val saved = withContext(Dispatchers.IO) {
                                val full = sourceUri?.let { loadFullBitmap(context, it) } ?: return@withContext false
                                val styled = LutProcessor.apply(full, selectedStyle, context, adjustments)
                                saveToGallery(context, styled, selectedStyle, exportSettings.value)
                            }
                            exporting = false
                            Toast.makeText(
                                context,
                                context.getString(
                                    if (saved) R.string.toast_saved else R.string.toast_save_failed,
                                ),
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    },
                    shape = RoundedCornerShape(2.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                ) {
                    Text(stringResource(R.string.export).uppercase(), style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inner),
            ) {
                previewBox(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
                )
                Column(
                    modifier = Modifier
                        .width(320.dp)
                        .fillMaxHeight()
                        .padding(end = 8.dp, top = 12.dp, bottom = 12.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    stylesPanel(Modifier.fillMaxWidth())
                    Spacer(Modifier.height(16.dp))
                    actionRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp))
                    Spacer(Modifier.height(8.dp))
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inner),
            ) {
                previewBox(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                )
                stylesPanel(Modifier.fillMaxWidth())
                Spacer(Modifier.height(20.dp))
                actionRow(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 16.dp),
                )
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

@Composable
private fun StyleChip(style: Style, selected: Boolean, onClick: () -> Unit) {
    val border = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(2.dp))
            .border(if (selected) 1.5.dp else 1.dp, border, RoundedCornerShape(2.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(chipColor(style), RoundedCornerShape(1.dp)),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = style.name.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun chipColor(style: Style): Color = when (style.brand) {
    Style.Brand.Hasselblad -> Color(0xFFE8DCC5)
    Style.Brand.Fujifilm -> if (style.id == "fj_velvia") Color(0xFF5E8C3B) else Color(0xFFB7A07E)
    Style.Brand.Leica -> if (style.monochrome) Color(0xFF1A1A1A) else Color(0xFFC8102E)
}

private fun loadFullBitmap(context: Context, uri: Uri): Bitmap? = decodeOriented(context, uri, sampleSize = 1)

private fun loadPreviewBitmap(context: Context, uri: Uri): Bitmap? {
    // Read dimensions first to choose a sample size that lands near PREVIEW_LONG_EDGE.
    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
    val long = max(opts.outWidth, opts.outHeight)
    var sample = 1
    while (long / (sample * 2) >= PREVIEW_LONG_EDGE) sample *= 2
    return decodeOriented(context, uri, sample)
}

private fun decodeOriented(context: Context, uri: Uri, sampleSize: Int): Bitmap? {
    val resolver = context.contentResolver
    val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    val raw = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) } ?: return null
    val orientation = resolver.openInputStream(uri)?.use {
        ExifInterface(it).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL,
        )
    } ?: ExifInterface.ORIENTATION_NORMAL
    return applyExifOrientation(raw, orientation)
}

private fun applyExifOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
    val m = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> m.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> m.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> m.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> m.postScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> m.postScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> { m.postRotate(90f); m.postScale(-1f, 1f) }
        ExifInterface.ORIENTATION_TRANSVERSE -> { m.postRotate(270f); m.postScale(-1f, 1f) }
        else -> return bitmap
    }
    val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, m, true)
    if (rotated != bitmap) bitmap.recycle()
    return rotated
}

private fun saveToGallery(context: Context, bitmap: Bitmap, style: Style, settings: ExportSettings): Boolean =
    MediaSaver.saveBitmap(context, bitmap, style, settings.jpegQuality)

private fun writeShareCache(context: Context, bitmap: Bitmap, style: Style, settings: ExportSettings): Uri? {
    val dir = File(context.cacheDir, "share").apply { mkdirs() }
    val file = File(dir, "Filmic_${style.id}_${System.currentTimeMillis()}.jpg")
    return try {
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, settings.jpegQuality, it) }
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    } catch (t: Throwable) {
        null
    }
}

private fun startShare(context: Context, uri: Uri) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/jpeg"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val chooser = Intent.createChooser(intent, null).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(chooser)
}
