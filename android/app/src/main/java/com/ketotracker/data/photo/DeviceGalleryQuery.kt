package com.ketotracker.data.photo

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId

/** One photo already on the device (outside the app's own photo store), found via [DeviceGalleryQuery]. */
data class DevicePhoto(val uri: Uri)

/**
 * Queries the device's `MediaStore` for photos taken on a given app date key
 * (`YYYY-MM-DD`) — lets a user retroactively attach photos they already took
 * with the system camera app instead of re-shooting through Keto Tracker.
 * Requires `READ_MEDIA_IMAGES` (API 33+) or `READ_EXTERNAL_STORAGE` (below
 * that); callers must confirm the permission is granted before calling this.
 */
object DeviceGalleryQuery {

    suspend fun photosForDate(context: Context, dateKey: String): List<DevicePhoto> =
        withContext(Dispatchers.IO) {
            val zone = ZoneId.systemDefault()
            val day = LocalDate.parse(dateKey)
            val startMillis = day.atStartOfDay(zone).toInstant().toEpochMilli()
            val endMillis = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

            val results = mutableListOf<Pair<Long, Long>>() // id to takenAt, for sorting
            val projection = arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DATE_TAKEN)
            // DATE_TAKEN is null for some images (e.g. screenshots/edited copies) — fall back
            // to DATE_ADDED (seconds, not millis) so those aren't silently excluded.
            val selection = "(${MediaStore.Images.Media.DATE_TAKEN} BETWEEN ? AND ?) OR " +
                "(${MediaStore.Images.Media.DATE_TAKEN} IS NULL AND ${MediaStore.Images.Media.DATE_ADDED} BETWEEN ? AND ?)"
            val args = arrayOf(
                startMillis.toString(), (endMillis - 1).toString(),
                (startMillis / 1000).toString(), (endMillis / 1000 - 1).toString(),
            )
            val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} ASC"

            runCatching {
                context.contentResolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    args,
                    sortOrder,
                )?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                    val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
                    while (cursor.moveToNext()) {
                        results += cursor.getLong(idCol) to cursor.getLong(dateCol)
                    }
                }
            }

            results.map { (id, _) ->
                DevicePhoto(ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id))
            }
        }
}
