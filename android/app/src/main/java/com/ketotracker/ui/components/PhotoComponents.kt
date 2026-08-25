package com.ketotracker.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.ketotracker.data.Meal
import com.ketotracker.data.photo.DevicePhoto
import com.ketotracker.data.photo.DeviceGalleryQuery
import com.ketotracker.data.photo.MAX_MEAL_PHOTOS
import com.ketotracker.data.photo.MealPhoto
import com.ketotracker.data.photo.CaptureTarget
import com.ketotracker.data.photo.createCaptureTarget
import com.ketotracker.data.photo.importUriToTempFile
import com.ketotracker.ui.theme.KetoTheme
import kotlinx.coroutines.launch
import java.io.File

/**
 * Photo area shown below a meal step's action row — the native counterpart of
 * the web app's `#photo-area` (CLAUDE.md "Photos" / index.html `loadMealPhoto`):
 * a thumbnail per stored photo (tap to view full-screen, ✕ to delete) plus
 * three ways to add one: camera capture, the system photo picker, or a
 * same-day sweep of the device's own gallery for [date].
 *
 * Capture is delegated entirely to the system camera app via
 * `ActivityResultContracts.TakePicture()` — no CAMERA permission, no CameraX.
 * The gallery picker uses `PickVisualMedia()`, which needs no storage
 * permission at all. Only the same-day sweep needs a real runtime permission,
 * since it queries `MediaStore` directly rather than going through a
 * system-mediated picker (see [DeviceDayPhotosButton]).
 */
@Composable
fun MealPhotoArea(
    meal: Meal,
    date: String,
    photos: List<MealPhoto>,
    onCaptured: (File) -> Unit,
    onView: (MealPhoto) -> Unit,
    onRemove: (MealPhoto) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingCapture by remember { mutableStateOf<CaptureTarget?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val capture = pendingCapture
        pendingCapture = null
        if (capture != null) {
            // Either path consumes the temp file: a successful capture is
            // handed to PhotoStore (which deletes it once compressed), a
            // cancelled one has nothing to compress so we delete it directly.
            if (success) onCaptured(capture.file) else capture.file.delete()
        }
    }
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                importUriToTempFile(context, uri)?.let(onCaptured)
            }
        }
    }

    val canAddMore = photos.size < MAX_MEAL_PHOTOS

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        photos.forEach { photo ->
            PhotoThumb(photo = photo, onClick = { onView(photo) }, onDelete = { onRemove(photo) })
        }
        CameraButton(
            label = if (photos.isEmpty()) "📷 Add Photo" else "📷 Add Another",
            enabled = canAddMore,
        ) {
            val target = createCaptureTarget(context)
            pendingCapture = target
            cameraLauncher.launch(target.uri)
        }
        CameraButton(label = "🖼️ Choose from Gallery", enabled = canAddMore) {
            galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        DeviceDayPhotosButton(date = date, enabled = canAddMore, onImport = onCaptured)
    }
}

@Composable
private fun PhotoThumb(photo: MealPhoto, onClick: () -> Unit, onDelete: () -> Unit) {
    Box(Modifier.fillMaxWidth()) {
        AsyncImage(
            model = photo.file,
            contentDescription = "Meal photo",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 260.dp)
                .clip(RoundedCornerShape(13.dp))
                .clickable(onClick = onClick),
        )
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .size(30.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable(onClick = onDelete),
            contentAlignment = Alignment.Center,
        ) {
            KText("✕", size = 13, color = Color.White)
        }
    }
}

@Composable
private fun CameraButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    val c = KetoTheme.colors
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(c.surf2)
            .border(1.5.dp, c.bdI, RoundedCornerShape(13.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(14.dp),
        contentAlignment = Alignment.Center,
    ) {
        KText(label, size = 14, color = if (enabled) c.txtM else c.txtD, weight = FontWeight.Medium)
    }
}

/**
 * "📅 Photos from this day" — sweeps the device's own gallery (not the app's
 * photo store) for images taken on [date], so photos shot outside the app
 * (with the stock camera app, before this feature existed, etc.) can be
 * attached retroactively. Needs `READ_MEDIA_IMAGES` (API 33+, or its
 * `READ_MEDIA_VISUAL_USER_SELECTED` partial-access counterpart on API 34+)
 * or `READ_EXTERNAL_STORAGE` (below that) — unlike the gallery picker above,
 * this queries `MediaStore` directly rather than going through a
 * system-mediated picker, so it's the one path that needs a real permission.
 */
@Composable
private fun DeviceDayPhotosButton(date: String, enabled: Boolean, onImport: (File) -> Unit) {
    val context = LocalContext.current
    var showPicker by remember { mutableStateOf(false) }
    var permissionDenied by remember { mutableStateOf(false) }

    val permissions = remember { devicePhotoPermissions() }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        if (results.values.any { it }) {
            permissionDenied = false
            showPicker = true
        } else {
            permissionDenied = true
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        CameraButton(label = "📅 Photos from This Day", enabled = enabled) {
            if (hasDevicePhotoPermission(context)) {
                showPicker = true
            } else {
                permissionLauncher.launch(permissions)
            }
        }
        if (permissionDenied) {
            val c = KetoTheme.colors
            KText(
                "Photo access was denied — enable it from Android's app settings to use this.",
                size = 11, color = c.txtD,
            )
        }
    }

    if (showPicker) {
        DevicePhotoGridDialog(
            date = date,
            onImport = onImport,
            onClose = { showPicker = false },
        )
    }
}

private fun devicePhotoPermissions(): Array<String> = when {
    Build.VERSION.SDK_INT >= 34 -> arrayOf(
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
    )
    Build.VERSION.SDK_INT >= 33 -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
    else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
}

private fun hasDevicePhotoPermission(context: Context): Boolean =
    devicePhotoPermissions().any {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

/** Grid of the device's own photos taken on [date]; tapping one imports it via [onImport]. */
@Composable
private fun DevicePhotoGridDialog(
    date: String,
    onImport: (File) -> Unit,
    onClose: () -> Unit,
) {
    val c = KetoTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var photos by remember { mutableStateOf<List<DevicePhoto>?>(null) }
    var importing by remember { mutableStateOf(false) }

    LaunchedEffect(date) {
        photos = DeviceGalleryQuery.photosForDate(context, date)
    }

    Dialog(onDismissRequest = onClose) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 480.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(c.surf)
                .border(1.dp, c.bdI, RoundedCornerShape(18.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            KText("📅 Photos from This Day", size = 16, color = c.gold, weight = FontWeight.Bold)
            when {
                importing -> KText("Importing…", size = 13, color = c.txtM)
                photos == null -> KText("Searching your device gallery…", size = 13, color = c.txtM)
                photos!!.isEmpty() -> KText(
                    "No photos found on your device for this date.",
                    size = 13, color = c.txtM,
                )
                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp),
                ) {
                    items(photos!!) { photo ->
                        AsyncImage(
                            model = photo.uri,
                            contentDescription = "Device photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    importing = true
                                    scope.launch {
                                        importUriToTempFile(context, photo.uri)?.let(onImport)
                                        importing = false
                                        onClose()
                                    }
                                },
                        )
                    }
                }
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !importing, onClick = onClose)
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                KText("Close", size = 14, color = c.txtM, weight = FontWeight.SemiBold)
            }
        }
    }
}

/**
 * Small "📷 2" badge shown next to a meal's label in the summary — mirrors the
 * web app's `loadSummaryPhotoIcons` (`#ph-ic-{meal}`). Tapping it opens the
 * first photo full-screen, same as on the web.
 */
@Composable
fun PhotoIndicator(count: Int, onClick: () -> Unit) {
    if (count == 0) return
    val c = KetoTheme.colors
    Box(
        Modifier
            .padding(start = 6.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(c.inp)
            .clickable(onClick = onClick)
            .padding(horizontal = 7.dp, vertical = 2.dp),
    ) {
        KText("📷${if (count > 1) " $count" else ""}", size = 11, color = c.txtM)
    }
}

/**
 * Full-screen photo viewer — native counterpart of the web app's `#photoModal`
 * (tap anywhere to dismiss).
 */
@Composable
fun PhotoViewer(photo: MealPhoto, onClose: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.92f))
            .clickable(onClick = onClose),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = photo.file,
            contentDescription = "Meal photo",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .clip(RoundedCornerShape(13.dp)),
        )
    }
}
