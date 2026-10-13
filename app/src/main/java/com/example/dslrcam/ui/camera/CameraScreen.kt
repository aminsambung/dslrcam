package com.example.dslrcam.ui.camera

import android.view.TextureView
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.dslrcam.ui.camera.components.ManualPanel
import com.example.dslrcam.ui.theme.Accent
import com.example.dslrcam.ui.theme.BackgroundDark
import com.example.dslrcam.ui.theme.TextPrimary
import com.example.dslrcam.utils.Permissions
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    onOpenGallery: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: CameraViewModel = viewModel()
) {
    val context = LocalContext.current
    val cameraPermission = rememberPermissionState(Permissions.CAMERA)

    LaunchedEffect(Unit) {
        if (!cameraPermission.status.isGranted) {
            cameraPermission.launchPermissionRequest()
        }
    }

    val controller = remember {
        viewModel.initController(context)
        viewModel.getController()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        if (cameraPermission.status.isGranted && controller != null) {
            AndroidView(
                factory = { ctx ->
                    TextureView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )

                        controller.startCamera(
                            textureView = this,
                            onReady = {},
                            onError = { e ->
                                Toast.makeText(
                                    ctx,
                                    "Kamera error: ${e.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                if (viewModel.manualModeEnabled) {
                                    viewModel.toggleManualMode()
                                }
                            },
                            onLongPress = {
                                if (viewModel.manualModeEnabled) {
                                    viewModel.resetManual()
                                    Toast.makeText(
                                        context,
                                        "Manual direset ke Auto",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        )
                    }
            )

            // TopBar
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
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

            // Panel manual
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

            // Tombol shutter
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
                                viewModel.onPhotoSaved(uri)
                                Toast.makeText(
                                    context,
                                    "Foto tersimpan",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            onError = { e ->
                                Toast.makeText(
                                    context,
                                    "Gagal: ${e.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        )
                    }
            )

            // Tombol Galeri — thumbnail
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(24.dp)
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0x66000000))
                    .clickable { onOpenGallery() },
                contentAlignment = Alignment.Center
            ) {
                val lastUri = viewModel.lastPhotoUri
                if (lastUri != null) {
                    AsyncImage(
                        model = lastUri,
                        contentDescription = "Foto terakhir",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
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

            // Tombol Pengaturan
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
            Text(
                text = "Izin kamera dibutuhkan.\nTekan untuk minta izin.",
                color = TextPrimary,
                modifier = Modifier
                    .align(Alignment.Center)
                    .clickable {
                        cameraPermission.launchPermissionRequest()
                    }
            )
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
