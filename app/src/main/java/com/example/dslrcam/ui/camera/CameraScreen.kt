package com.example.dslrcam.ui.camera

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.TextureView
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.dslrcam.ui.camera.components.ManualPanel
import com.example.dslrcam.ui.theme.Accent
import com.example.dslrcam.ui.theme.BackgroundDark
import com.example.dslrcam.ui.theme.SurfaceDark
import com.example.dslrcam.ui.theme.TextPrimary
import com.example.dslrcam.ui.theme.TextSecondary
import com.example.dslrcam.utils.Permissions
import com.example.dslrcam.utils.PhotoFilters
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    onOpenSettings: () -> Unit,
    viewModel: CameraViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraPermission = rememberPermissionState(Permissions.CAMERA)

    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var alreadyRequested by remember { mutableStateOf(false) }
    var textureView by remember { mutableStateOf<TextureView?>(null) }

    LaunchedEffect(cameraPermission.status.isGranted) {
        if (!cameraPermission.status.isGranted && !alreadyRequested) {
            alreadyRequested = true
            cameraPermission.launchPermissionRequest()
        }
    }

    val controller = remember {
        viewModel.initController(context)
        viewModel.getController()
    }

    // Lifecycle observer
    DisposableEffect(lifecycleOwner, cameraPermission.status.isGranted) {
        val observer = LifecycleEventObserver { _, event ->
            if (cameraPermission.status.isGranted) {
                when (event) {
                    Lifecycle.Event.ON_RESUME -> {
                        textureView?.let { tv ->
                            if (tv.isAvailable) {
                                controller?.startCamera(
                                    textureView = tv,
                                    onReady = {},
                                    onError = { e ->
                                        Toast.makeText(context, "Kamera: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }
                    Lifecycle.Event.ON_PAUSE -> {
                        controller?.closeCamera()
                    }
                    else -> {}
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Auto-hide kotak fokus
    LaunchedEffect(viewModel.focusPoint) {
        if (viewModel.focusPoint != null) {
            delay(5000)
            viewModel.clearFocusPoint()
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        val density = androidx.compose.ui.platform.LocalDensity.current
        val cW = with(density) { maxWidth.toPx() }.roundToInt()
        val cH = with(density) { maxHeight.toPx() }.roundToInt()

        LaunchedEffect(cW, cH) {
            if (cW > 0 && cH > 0) {
                containerSize = IntSize(cW, cH)
            }
        }

        if (cameraPermission.status.isGranted && controller != null) {
            // PREVIEW
            AndroidView(
                factory = { ctx ->
                    TextureView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        textureView = this
                        controller.startCamera(
                            textureView = this,
                            onReady = {},
                            onError = { e ->
                                Toast.makeText(ctx, "Kamera error: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(containerSize) {
                        detectTapGestures(
                            onTap = { offset ->
                                if (viewModel.manualModeEnabled) viewModel.toggleManualMode()
                                if (viewModel.filterPanelEnabled) viewModel.toggleFilterPanel()

                                viewModel.setFocusPoint(offset)

                                if (containerSize.width > 0 && containerSize.height > 0) {
                                    controller.tapToFocus(
                                        viewX = offset.x,
                                        viewY = offset.y,
                                        viewWidth = containerSize.width,
                                        viewHeight = containerSize.height
                                    )
                                }
                            },
                            onLongPress = {
                                if (viewModel.manualModeEnabled) {
                                    viewModel.resetManual()
                                    Toast.makeText(context, "Manual direset", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
            )

            // Kotak fokus
            viewModel.focusPoint?.let { point ->
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = (point.x - 40.dp.toPx()).roundToInt().coerceAtLeast(0),
                                y = (point.y - 40.dp.toPx()).roundToInt().coerceAtLeast(0)
                            )
                        }
                        .size(80.dp)
                        .border(2.dp, Accent, RoundedCornerShape(4.dp))
                )
            }

            // === TOP BAR ===
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Info manual (kalau ada)
                val hasManualSetting = viewModel.manualSettings.iso != null ||
                        viewModel.manualSettings.shutterSpeedNs != null ||
                        viewModel.manualSettings.whiteBalanceK != null

                if (hasManualSetting) {
                    val info = buildString {
                        append("ISO ${viewModel.manualSettings.iso ?: "Auto"}")
                        append("  |  ")
                        append(viewModel.manualSettings.shutterSpeedNs?.let { formatShutterShort(it) } ?: "Auto")
                        append("  |  ")
                        append(viewModel.manualSettings.whiteBalanceK?.let { "${it}K" } ?: "Auto")
                    }
                    Text(
                        text = info,
                        color = Accent,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .background(Color(0xCC000000), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Spacer(Modifier.weight(1f))

                // === TOMBOL HDR ===
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "HDR",
                        color = if (viewModel.hdrEnabled) BackgroundDark else TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(
                                if (viewModel.hdrEnabled) Accent else Color(0xCC000000),
                                RoundedCornerShape(20.dp)
                            )
                            .clickable { viewModel.toggleHdr() }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    )

                    // Indikator "3 frame" kalau HDR aktif
                    if (viewModel.hdrEnabled) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "3 frame",
                            color = Accent,
                            fontSize = 8.sp,
                            modifier = Modifier
                                .background(Color(0xCC000000), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))

                // Tombol M
                Text(
                    text = "M",
                    color = if (viewModel.manualModeEnabled) BackgroundDark else TextPrimary,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .background(
                            if (viewModel.manualModeEnabled) Accent else Color(0xCC000000),
                            RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .clickable { viewModel.toggleManualMode() }
                )
            }

            // PANEL MANUAL
            if (viewModel.manualModeEnabled) {
                ManualPanel(
                    iso = viewModel.manualSettings.iso,
                    shutterNs = viewModel.manualSettings.shutterSpeedNs,
                    whiteBalanceK = viewModel.manualSettings.whiteBalanceK,
                    focusDistance = viewModel.manualSettings.focusDistance,
                    onIsoChange = { viewModel.updateIso(it) },
                    onShutterChange = { viewModel.updateShutter(it) },
                    onWhiteBalanceChange = { viewModel.updateWhiteBalance(it) },
                    onFocusChange = { viewModel.updateFocus(it) },
                    onReset = { viewModel.resetManual() },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 140.dp)
                )
            }

            // PANEL FILTER
            if (viewModel.filterPanelEnabled) {
                FilterPanelWithSlider(
                    currentFilterId = viewModel.currentFilter.id,
                    currentStrength = viewModel.filterStrength,
                    onSelect = { id -> viewModel.setFilter(id) },
                    onStrengthChange = { strength -> viewModel.setFilterStrength(strength) },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 140.dp)
                )
            }

            // SHUTTER
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Accent)
                    .clickable {
                        controller.takePhoto(
                            onSaved = { uri ->
                                viewModel.applyFilterToUri(context, uri)
                                val label = if (viewModel.hdrEnabled) "HDR + ${viewModel.currentFilter.name}" else viewModel.currentFilter.name
                                Toast.makeText(
                                    context,
                                    "Foto ($label)",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            onError = { e ->
                                Toast.makeText(context, "Gagal: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        )
                    }
            )

            // THUMBNAIL
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(24.dp)
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0x66000000))
                    .clickable { openSystemGallery(context, viewModel.lastPhotoUri) },
                contentAlignment = Alignment.Center
            ) {
                val lastUri = viewModel.lastPhotoUri
                if (lastUri != null) {
                    AsyncImage(
                        model = lastUri,
                        contentDescription = "Foto terakhir",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.PhotoLibrary,
                        contentDescription = "Galeri",
                        tint = TextPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // TOMBOL FILTER
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 88.dp, bottom = 32.dp)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (viewModel.filterPanelEnabled) Accent else Color(0xCC000000)
                    )
                    .clickable { viewModel.toggleFilterPanel() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoFixHigh,
                    contentDescription = "Filter",
                    tint = if (viewModel.filterPanelEnabled) BackgroundDark else TextPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            // TOMBOL SETTINGS
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Pengaturan",
                    tint = TextPrimary
                )
            }

        } else {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Izin kamera dibutuhkan",
                    color = TextPrimary,
                    fontSize = 16.sp
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Ketuk untuk memberi izin",
                    color = Accent,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .background(Color(0x33FFC107), RoundedCornerShape(8.dp))
                        .clickable {
                            cameraPermission.launchPermissionRequest()
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

// === FILTER PANEL ===
@Composable
private fun FilterPanelWithSlider(
    currentFilterId: String,
    currentStrength: Float,
    onSelect: (String) -> Unit,
    onStrengthChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(SurfaceDark.copy(alpha = 0.95f), RoundedCornerShape(16.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "FILTER",
            color = TextSecondary,
            fontSize = 10.sp,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Slider intensitas
        if (currentFilterId != "normal") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${(currentStrength * 100).toInt()}%",
                    color = Accent,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.width(48.dp)
                )

                Slider(
                    value = currentStrength,
                    onValueChange = onStrengthChange,
                    valueRange = 0f..1f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = Accent,
                        activeTrackColor = Accent,
                        inactiveTrackColor = Color(0x33FFFFFF)
                    )
                )

                Spacer(Modifier.width(8.dp))

                Text(
                    text = "100%",
                    color = TextSecondary,
                    fontSize = 10.sp
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Grid filter
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PhotoFilters.ALL.forEach { filter ->
                val selected = filter.id == currentFilterId
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { onSelect(filter.id) }
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (selected) Accent else Color(0x33FFFFFF)
                            )
                            .border(
                                width = if (selected) 2.dp else 0.dp,
                                color = if (selected) TextPrimary else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = filter.name.take(2),
                            color = if (selected) BackgroundDark else TextPrimary,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = filter.name,
                        color = if (selected) Accent else TextPrimary,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

private fun formatShutterShort(ns: Long): String {
    if (ns >= 1_000_000_000L) {
        return "%.1fs".format(ns / 1_000_000_000.0)
    }
    val denom = (1_000_000_000L / ns).toInt()
    return "1/$denom"
}

private fun openSystemGallery(context: Context, lastPhotoUri: Uri?) {
    try {
        val intent = if (lastPhotoUri != null) {
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(lastPhotoUri, "image/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_APP_GALLERY)
            }
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Tidak ada aplikasi galeri", Toast.LENGTH_SHORT).show()
    }
}
