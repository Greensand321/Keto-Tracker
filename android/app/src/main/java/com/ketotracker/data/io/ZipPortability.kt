package com.ketotracker.data.io

import android.content.Context
import android.net.Uri
import com.ketotracker.data.DayEntry
import com.ketotracker.data.SupplementSchedule
import com.ketotracker.data.photo.PhotoStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class ZipImportResult(
    val entries: Map<String, DayEntry>,
    val photos: Map<String, ByteArray>, // filename → raw JPEG bytes
    val schedules: List<SupplementSchedule> = emptyList(),
    val activeScheduleId: String? = null,
)

@Serializable
private data class ScheduleBundle(val schedules: List<SupplementSchedule>, val activeScheduleId: String? = null)

/**
 * Full-backup ZIP export/import: bundles `data.json` (all day entries in
 * DataPortability format), `schedules.json` (supplement schedules + the active
 * one), and every stored JPEG in a `photos/` directory. The native equivalent
 * of manually combining JSON export + photo transfer.
 */
object ZipPortability {

    private val scheduleJson = Json { ignoreUnknownKeys = true }

    suspend fun export(
        context: Context,
        uri: Uri,
        entries: Map<String, DayEntry>,
        photoStore: PhotoStore,
        schedules: List<SupplementSchedule> = emptyList(),
        activeScheduleId: String? = null,
    ): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val os = context.contentResolver.openOutputStream(uri) ?: error("no output stream")
            ZipOutputStream(os.buffered()).use { zip ->
                zip.putNextEntry(ZipEntry("data.json"))
                zip.write(DataPortability.encode(entries).toByteArray())
                zip.closeEntry()

                if (schedules.isNotEmpty()) {
                    zip.putNextEntry(ZipEntry("schedules.json"))
                    zip.write(
                        scheduleJson.encodeToString(ScheduleBundle.serializer(), ScheduleBundle(schedules, activeScheduleId))
                            .toByteArray(),
                    )
                    zip.closeEntry()
                }

                photoStore.listAllPhotoFiles().forEach { file ->
                    zip.putNextEntry(ZipEntry("photos/${file.name}"))
                    zip.write(file.readBytes())
                    zip.closeEntry()
                }
            }
        }.isSuccess
    }

    suspend fun import(context: Context, uri: Uri): ZipImportResult? = withContext(Dispatchers.IO) {
        runCatching {
            val ins = context.contentResolver.openInputStream(uri) ?: error("no input stream")
            var entries = emptyMap<String, DayEntry>()
            var bundle: ScheduleBundle? = null
            val photos = mutableMapOf<String, ByteArray>()

            ZipInputStream(ins.buffered()).use { zip ->
                var ze = zip.nextEntry
                while (ze != null) {
                    when {
                        ze.name == "data.json" ->
                            entries = DataPortability.decode(zip.readBytes().decodeToString())
                        ze.name == "schedules.json" ->
                            bundle = runCatching {
                                scheduleJson.decodeFromString(ScheduleBundle.serializer(), zip.readBytes().decodeToString())
                            }.getOrNull()
                        ze.name.startsWith("photos/") && !ze.isDirectory -> {
                            val name = ze.name.removePrefix("photos/")
                            if (name.isNotEmpty()) photos[name] = zip.readBytes()
                        }
                    }
                    zip.closeEntry()
                    ze = zip.nextEntry
                }
            }
            ZipImportResult(
                entries = entries,
                photos = photos,
                schedules = bundle?.schedules ?: emptyList(),
                activeScheduleId = bundle?.activeScheduleId,
            )
        }.getOrNull()
    }
}
