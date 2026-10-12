package com.example.dslrcam.ui.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.dslrcam.ui.theme.BackgroundDark
import com.example.dslrcam.ui.theme.TextPrimary
import com.example.dslrcam.ui.theme.TextSecondary

@Composable
fun CameraScreen(
    onOpenGallery: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        // Area tengah: preview kamera (nanti diisi)
        Text(
            text = "Preview kamera akan muncul di sini",
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.Center)
        )

        // Tombol kiri bawah: Galeri
        IconButton(
            onClick = onOpenGallery,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(24.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.PhotoLibrary,
                contentDescription = "Galeri",
                tint = TextPrimary
            )
        }

        // Tombol kanan bawah: Pengaturan
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
    }
}
