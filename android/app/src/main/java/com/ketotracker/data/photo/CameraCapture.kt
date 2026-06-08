package com.ketotracker.data.photo

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * Creates a fresh content:// URI in the app's cache dir for the system camera
 * app to write a full-resolution JPEG into (handed to
 * `ActivityResultContracts.TakePicture()`). The file is consumed and discarded
 * immediately after capture — [PhotoStore.addFromUri] reads, compresses, and
 * stores a separate copy, so this one is safe to delete right after.
 */
fun createCaptureUri(context: Context): Uri {
    val dir = File(context.cacheDir, "captures").apply { mkdirs() }
    val file = File(dir, "capture_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
