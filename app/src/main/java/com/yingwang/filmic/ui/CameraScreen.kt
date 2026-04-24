package com.yingwang.filmic.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Handler
import android.os.Looper
import android.view.Surface
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
import androidx.camera.view.PreviewView
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
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.yingwang.filmic.R
import com.yingwang.filmic.io.MediaSaver
import com.yingwang.filmic.lut.LutProcessor
import com.yingwang.filmic.lut.Style
import com.yingwang.filmic.lut.Styles
import com.yingwang.filmic.settings.ExportSettings
import com.yingwang.filmic.settings.rememberExportSettings
import java.util.concurrent.Executors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Live camera preview. The bottom layer is a real CameraX [PreviewView] so the
 * frame is never black while we wait for analysis. On top, we overlay a
 * LUT-processed image built from ImageAnalysis frames — it fades in once
 * frames start arriving, and is just decoration; the shutter applies the style
 * to the full-res capture.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val settings = rememberExportSettings()

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
    var overlayFrame by remember { mutableStateOf<Bitmap?>(null) }
    var capturing by remember { mutableStateOf(false) }
    val imageCapture = remember { ImageCapture.Builder().build() }
    val previewUseCase = remember { Preview.Builder().build() }
    val imageAnalysis = remember {
        ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setResolutionSelector(
                ResolutionSelector.Builder()
                    .setResolutionStrategy(
                        ResolutionStrategy(
                            Size(720, 960),
                            ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER,
                        ),
                    )
                    .build(),
            )
            .build()
    }
    val analyzerExecutor = remember { Executors.newSingleThreadExecutor() }
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    DisposableEffect(Unit) {
        previewUseCase.setSurfaceProvider(previewView.surfaceProvider)
        onDispose { analyzerExecutor.shutdown() }
    }

    // Update target rotation when device orientation changes
    val orientation = LocalConfiguration.current.orientation
    LaunchedEffect(orientation) {
        val rotation = (context as? Activity)?.display?.rotation ?: Surface.ROTATION_0
        imageCapture.targetRotation = rotation
        imageAnalysis.targetRotation = rotation
    }

    LaunchedEffect(hasPermission) {
        if (!hasPermission) return@LaunchedEffect
        val provider = ProcessCameraProvider.getInstance(context).awaitInstance()
        provider.unbindAll()

        imageAnalysis.setAnalyzer(analyzerExecutor) { image ->
            val bmp = image.toRotatedBitmap()
            image.close()
            if (bmp == null) return@setAnalyzer
            val styled = try {
                LutProcessor.apply(bmp, selectedStyle, context)
            } catch (t: Throwable) {
                null
            }
            bmp.recycle()
            overlayFrame = styled
        }

        try {
            provider.bindToLifecycle(
                lifecycle,
                CameraSelector.DEFAULT_BACK_CAMERA,
                previewUseCase,
                imageAnalysis,
                imageCapture,
            )
        } catch (t: Throwable) {
            // Some devices can't run Preview + Analysis + Capture together.
            // Fall back to Preview + Capture only (no live LUT overlay).
            provider.unbindAll()
            provider.bindToLifecycle(
                lifecycle,
                CameraSelector.DEFAULT_BACK_CAMERA,
                previewUseCase,
                imageCapture,
            )
        }
    }

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = { Text(selectedStyle.name.uppercase(), style = MaterialTheme.typography.titleMedium) },
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
        val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
        val mainHandler = remember { Handler(Looper.getMainLooper()) }

        val previewBox: @Composable (Modifier) -> Unit = { mod ->
            Box(
                modifier = mod
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF111111)),
                contentAlignment = Alignment.Center,
            ) {
                if (!hasPermission) {
                    Text(
                        text = stringResource(R.string.camera_permission_required),
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    AndroidView(
                        factory = { previewView },
                        modifier = Modifier.fillMaxSize(),
                    )
                    val f = overlayFrame
                    if (f != null) {
                        Image(
                            bitmap = f.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }

        val onShutter: () -> Unit = {
            capturing = true
            captureToGallery(
                context = context,
                imageCapture = imageCapture,
                style = selectedStyle,
                settings = settings.value,
                onComplete = { ok ->
                    mainHandler.post {
                        capturing = false
                        Toast.makeText(
                            context,
                            context.getString(
                                if (ok) R.string.toast_saved else R.string.toast_capture_failed,
                            ),
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                },
            )
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
                        .padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                )
                Column(
                    modifier = Modifier
                        .width(160.dp)
                        .fillMaxHeight()
                        .padding(end = 12.dp, top = 8.dp, bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    ) {
                        items(Styles.all, key = { it.id }) { s ->
                            DarkChip(
                                style = s,
                                selected = s.id == selectedStyle.id,
                                onClick = { selectedStyle = s },
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    ShutterButton(enabled = hasPermission && !capturing, onClick = onShutter)
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
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )

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
                    ShutterButton(enabled = hasPermission && !capturing, onClick = onShutter)
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
        Box(modifier = Modifier.size(60.dp).clip(CircleShape).background(Color.Black))
        Box(modifier = Modifier.size(54.dp).clip(CircleShape).background(Color.White))
    }
}

/**
 * Take an in-memory capture, apply the LUT, and write the styled JPEG to the
 * gallery. We don't use ImageCapture's file output path because we'd then have
 * to re-decode and re-encode just to apply the style — instead, capture the
 * raw bitmap, do LUT in one shot, and save with MediaSaver.
 */
private fun captureToGallery(
    context: Context,
    imageCapture: ImageCapture,
    style: Style,
    settings: ExportSettings,
    onComplete: (Boolean) -> Unit,
) {
    imageCapture.takePicture(
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onError(exception: ImageCaptureException) {
                onComplete(false)
            }

            override fun onCaptureSuccess(image: ImageProxy) {
                val rotation = image.imageInfo.rotationDegrees
                val raw = try { image.toBitmap() } catch (t: Throwable) { null }
                image.close()
                if (raw == null) {
                    onComplete(false); return
                }
                Thread {
                    val oriented = rotate(raw, rotation)
                    val styled = LutProcessor.apply(oriented, style, context)
                    if (oriented !== styled) oriented.recycle()
                    val ok = MediaSaver.saveBitmap(context, styled, style, settings.jpegQuality)
                    styled.recycle()
                    onComplete(ok)
                }.start()
            }
        },
    )
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
