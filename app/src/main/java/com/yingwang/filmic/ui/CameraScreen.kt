package com.yingwang.filmic.ui

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.yingwang.filmic.lut.LutProcessor
import com.yingwang.filmic.lut.Style
import com.yingwang.filmic.lut.Styles
import java.util.concurrent.Executors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Live camera preview with the active style applied to each analysis frame.
 *
 * Frames flow ImageAnalysis → YUV→ARGB Bitmap → LutProcessor → Compose Image.
 * Capture button triggers ImageCapture (full-res JPEG), then we re-decode and
 * apply the same style before saving to the gallery.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> hasPermission = granted }
    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    var selectedStyle by remember { mutableStateOf(Styles.all.first()) }
    var frame by remember { mutableStateOf<Bitmap?>(null) }
    var capturing by remember { mutableStateOf(false) }
    val imageCapture = remember { ImageCapture.Builder().build() }
    val analyzerExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose { analyzerExecutor.shutdown() }
    }

    LaunchedEffect(hasPermission) {
        if (!hasPermission) return@LaunchedEffect
        val provider = ProcessCameraProvider.getInstance(context).awaitInstance()
        provider.unbindAll()

        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setResolutionSelector(
                ResolutionSelector.Builder()
                    .setResolutionStrategy(
                        ResolutionStrategy(Size(720, 960), ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER),
                    )
                    .build(),
            )
            .build()

        analysis.setAnalyzer(analyzerExecutor) { image ->
            val bmp = image.toRotatedBitmap() ?: run { image.close(); return@setAnalyzer }
            image.close()
            val styled = LutProcessor.apply(bmp, selectedStyle, context)
            bmp.recycle()
            frame = styled
        }

        provider.bindToLifecycle(
            lifecycle,
            CameraSelector.DEFAULT_BACK_CAMERA,
            analysis,
            imageCapture,
            Preview.Builder().build(), // attach a no-op preview so 3A converges
        )
    }

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = {
                    Text(selectedStyle.name.uppercase(), style = MaterialTheme.typography.titleMedium)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                ),
            )
        },
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF111111)),
                contentAlignment = Alignment.Center,
            ) {
                if (!hasPermission) {
                    Text("需要相机权限", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                } else {
                    val f = frame
                    if (f != null) {
                        Image(
                            bitmap = f.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Text("正在启动相机…", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
            ) {
                items(Styles.all, key = { it.id }) { s ->
                    DarkChip(
                        style = s,
                        selected = s.id == selectedStyle.id,
                        onClick = { selectedStyle = s },
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                ShutterButton(enabled = hasPermission && !capturing) {
                    capturing = true
                    captureToGallery(
                        context = context,
                        imageCapture = imageCapture,
                        style = selectedStyle,
                        onComplete = { ok ->
                            capturing = false
                            scope.launch {
                                Toast.makeText(
                                    context,
                                    if (ok) "已保存到相册" else "拍摄失败",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun DarkChip(style: Style, selected: Boolean, onClick: () -> Unit) {
    val border = if (selected) MaterialTheme.colorScheme.secondary else Color(0xFF555555)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(2.dp))
            .border(if (selected) 1.5.dp else 1.dp, border, RoundedCornerShape(2.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            text = style.name.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
        )
    }
}

@Composable
private fun ShutterButton(enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = if (enabled) 1f else 0.4f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(Color.Black),
        )
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(Color.White),
        )
    }
}

private fun captureToGallery(
    context: Context,
    imageCapture: ImageCapture,
    style: Style,
    onComplete: (Boolean) -> Unit,
) {
    val outputOptions = buildOutputOptions(context, style)
    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onError(exception: ImageCaptureException) {
                onComplete(false)
            }

            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                val saved = output.savedUri
                if (saved == null) {
                    onComplete(false)
                    return
                }
                Thread {
                    try {
                        val raw = context.contentResolver.openInputStream(saved)?.use {
                            BitmapFactory.decodeStream(it)
                        }
                        if (raw == null) {
                            onComplete(false); return@Thread
                        }
                        val styled = LutProcessor.apply(raw, style, context)
                        context.contentResolver.openOutputStream(saved, "wt")?.use { out ->
                            styled.compress(Bitmap.CompressFormat.JPEG, 94, out)
                        }
                        styled.recycle()
                        onComplete(true)
                    } catch (t: Throwable) {
                        onComplete(false)
                    }
                }.start()
            }
        },
    )
}

private fun buildOutputOptions(context: Context, style: Style): ImageCapture.OutputFileOptions {
    val name = "Filmic_${style.id}_${System.currentTimeMillis()}.jpg"
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, name)
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Filmic")
        }
    }
    return ImageCapture.OutputFileOptions.Builder(
        context.contentResolver,
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        values,
    ).build()
}

private fun ImageProxy.toRotatedBitmap(): Bitmap? {
    val bmp = try {
        toBitmap()
    } catch (t: Throwable) {
        return null
    }
    return rotate(bmp, imageInfo.rotationDegrees)
}

private fun rotate(bitmap: Bitmap, degrees: Int): Bitmap {
    if (degrees == 0) return bitmap
    val m = Matrix().apply { postRotate(degrees.toFloat()) }
    val out = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, m, true)
    if (out != bitmap) bitmap.recycle()
    return out
}

private suspend fun <T> com.google.common.util.concurrent.ListenableFuture<T>.awaitInstance(): T =
    withContext(Dispatchers.IO) { get() }
