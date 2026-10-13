package com.example.dslrcam.ui.gallery

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.exifinterface.media.ExifInterface
import coil.compose.AsyncImage
import com.example.dslrcam.ui.theme.BackgroundDark
import com.example.dslrcam.ui.theme.TextPrimary
import com.example.dslrcam.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun GalleryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var photos by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var selectedPhoto by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(Unit) {
        photos = loadPhotosFromMediaStore(context)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        if (photos.isEmpty()) {
            Text(
                text = "Belum ada foto",
                color = TextSecondary,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(
                    top = 64.dp,
                    start = 8.dp,
                    end = 8.dp,
                    bottom = 8.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(photos) { uri ->
                    AsyncImage(
                        model = uri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .aspectRatio(1f)
                            .background(Color.DarkGray)
                            .clickable { selectedPhoto = uri }
                    )
                }
            }
        }

        // Tombol back dari grid
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.ArrowBack,
                contentDescription = "Kembali",
                tint = TextPrimary
            )
        }

        // Viewer full screen
        selectedPhoto?.let { uri ->
            FullScreenPhoto(
                uri = uri,
                onClose = { selectedPhoto = null }
            )
        }
    }
}

@Composable
private fun FullScreenPhoto(uri: Uri, onClose: () -> Unit) {
    val context = LocalContext.current

    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var showInfo by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 5f)
                    if (scale > 1f) {
                        offsetX += pan.x
                        offsetY += pan.y
                    } else {
                        offsetX = 0f
                        offsetY = 0f
                    }
                }
            }
    ) {
        AsyncImage(
            model = uri,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY
                )
        )

        // Tombol close (kiri atas)
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Tutup",
                tint = Color.White
            )
        }

        // Tombol buka di album bawaan (kanan atas, kedua dari kanan)
        IconButton(
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "image/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    // Tidak ada app galeri
                }
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 56.dp, top = 16.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.OpenInNew,
                contentDescription = "Buka di Album",
                tint = Color.White
            )
        }

        // Tombol info (kanan atas)
        IconButton(
            onClick = { showInfo = !showInfo },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = "Info",
                tint = Color.White
            )
        }

        // Panel Info
        if (showInfo) {
            ExifInfoPanel(
                uri = uri,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp)
            )
        }
    }
}

@Composable
private fun ExifInfoPanel(uri: Uri, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val info = remember(uri) { readExifInfo(context, uri) }

    Column(
        modifier = modifier
            .background(
                Color(0xCC1C1C1C),
                RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Info Foto",
            color = TextPrimary,
            fontSize = 16.sp
        )
        Spacer(Modifier.height(12.dp))

        info.forEach { (label, value) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = label,
                    color = TextSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.width(120.dp)
                )
                Text(
                    text = value,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

/**
 * Baca EXIF + info file dari Uri.
 * Return list pasangan (label, value) untuk ditampilkan.
 */
private fun readExifInfo(context: Context, uri: Uri): List<Pair<String, String>> {
    val result = mutableListOf<Pair<String, String>>()

    try {
        // Nama file
        var displayName = ""
        var sizeBytes = 0L
        var dateModified = 0L

        context.contentResolver.query(
            uri,
            arrayOf(
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.SIZE,
                MediaStore.MediaColumns.DATE_MODIFIED
            ),
            null, null, null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                displayName = cursor.getString(0) ?: ""
                sizeBytes = cursor.getLong(1)
                dateModified = cursor.getLong(2) * 1000L
            }
        }

        if (displayName.isNotEmpty()) {
            result.add("Nama" to displayName)
        }

        if (dateModified > 0) {
            val fmt = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.US)
            result.add("Waktu" to fmt.format(Date(dateModified)))
        }

        if (sizeBytes > 0) {
            val mb = sizeBytes / 1024f / 1024f
            result.add("Ukuran" to String.format(Locale.US, "%.2f MB", mb))
        }

        // Baca EXIF
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val exif = ExifInterface(stream)

            val width = exif.getAttribute(ExifInterface.TAG_IMAGE_WIDTH)
            val height = exif.getAttribute(ExifInterface.TAG_IMAGE_LENGTH)
            if (width != null && height != null) {
                result.add("Resolusi" to "$width × $height")
            }

            val make = exif.getAttribute(ExifInterface.TAG_MAKE)
            val model = exif.getAttribute(ExifInterface.TAG_MODEL)
            if (make != null || model != null) {
                result.add("Kamera" to listOfNotNull(make, model).joinToString(" "))
            }

            val focal = exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH)
            val focal35 = exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM)
            if (focal != null) {
                val fVal = parseRational(focal)
                val text = if (focal35 != null) {
                    String.format(Locale.US, "%.1f mm (35mm: %s mm)", fVal, focal35)
                } else {
                    String.format(Locale.US, "%.1f mm", fVal)
                }
                result.add("Fokus" to text)
            }

            val fNumber = exif.getAttribute(ExifInterface.TAG_F_NUMBER)
            if (fNumber != null) {
                result.add("Bukaan" to "f/$fNumber")
            }

            val exposure = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)
            if (exposure != null) {
                val sec = parseRational(exposure)
                val text = if (sec < 1.0) {
                    "1/${(1.0 / sec).toInt()}"
                } else {
                    String.format(Locale.US, "%.1f s", sec)
                }
                result.add("Rana" to text)
            }

            val iso = exif.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY)
            if (iso != null) {
                result.add("ISO" to iso)
            }

            val ev = exif.getAttribute(ExifInterface.TAG_EXPOSURE_BIAS_VALUE)
            if (ev != null) {
                result.add("EV" to parseRational(ev).toString())
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        result.add("Error" to (e.message ?: "Gagal baca info"))
    }

    return result
}

private fun parseRational(value: String): Double {
    return try {
        if (value.contains("/")) {
            val parts = value.split("/")
            parts[0].toDouble() / parts[1].toDouble()
        } else {
            value.toDouble()
        }
    } catch (e: Exception) {
        0.0
    }
}

private fun loadPhotosFromMediaStore(context: Context): List<Uri> {
    val photos = mutableListOf<Uri>()

    val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
    } else {
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    }

    val projection = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DISPLAY_NAME
    )

    val selection = "${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?"
    val selectionArgs = arrayOf("DSLRCam_%")

    val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

    context.contentResolver.query(
        collection,
        projection,
        selection,
        selectionArgs,
        sortOrder
    )?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)

        while (cursor.moveToNext()) {
            val id = cursor.getLong(idColumn)
            val uri = ContentUris.withAppendedId(collection, id)
            photos.add(uri)
        }
    }

    return photos
}
