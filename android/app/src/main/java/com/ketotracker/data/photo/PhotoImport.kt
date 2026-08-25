package com.ketotracker.data.photo

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Copies a `content://` [Uri] — from the system photo picker or a same-day
 * device-gallery match (see [DeviceGalleryQuery]) — into a fresh temp file in
 * the same `cacheDir/captures` directory [createCaptureTarget] uses for
 * camera captures. This lets both paths feed the same downstream pipeline:
 * [com.ketotracker.data.photo.PhotoStore.addFromCapture] reads and deletes
 * whatever [File] it's handed, regardless of where it came from.
 */
suspend fun importUriToTempFile(context: Context, uri: Uri): File? = withContext(Dispatchers.IO) {
    val file = File(capturesDir(context), "import_${System.currentTimeMillis()}.jpg")
    val copied = runCatching {
        val input = context.contentResolver.openInputStream(uri) ?: return@runCatching false
        input.use { i -> file.outputStream().use { o -> i.copyTo(o) } }
        true
    }.getOrDefault(false)
    if (copied) file else { file.delete(); null }
}
