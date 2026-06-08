package com.ketotracker.data.io

import android.content.Context
import android.net.Uri
import com.ketotracker.data.DayEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement

private val DATE_KEY_REGEX = Regex("""^\d{4}-\d{2}-\d{2}$""")

/**
 * Pure JSON ⇄ `Map<dateKey, DayEntry>` conversion, key normalisation, and
 * field-level merge logic — the native counterpart to the web app's
 * `exportAll`/`handleImport`/`mergeEntries` (CLAUDE.md "Export / Import").
 * Also wraps the Storage Access Framework read/write that replaces the
 * browser's Blob download / FileReader for getting JSON on and off the device.
 */
object DataPortability {

    private val json = Json {
        ignoreUnknownKeys = true // tolerate exports from older/newer schema versions
        encodeDefaults = true    // keep exported entries self-contained
        prettyPrint = true       // human-readable, matches the web app's JSON.stringify(obj,null,2)
    }

    /** Flat `{ "2025-01-15": {...}, ... }` — same shape as the web app's `exportAll()` output. */
    fun encode(entries: Map<String, DayEntry>): String = json.encodeToString(entries)

    /**
     * Parses a flat date-keyed JSON object, accepting keys with or without the
     * web app's legacy `kt_d_`/`kt_` prefixes, validates the `YYYY-MM-DD`
     * format, and decodes each value — forcing `date` to the normalised outer
     * key so every result is self-consistent even if a malformed export had a
     * mismatched inner `date` field. Invalid keys and undecodable entries are
     * skipped rather than failing the whole import (mirrors `handleImport`'s
     * per-entry validation in CLAUDE.md "Import").
     */
    fun decode(text: String): Map<String, DayEntry> {
        val root = runCatching { json.parseToJsonElement(text) }.getOrNull() as? JsonObject
            ?: return emptyMap()
        val result = LinkedHashMap<String, DayEntry>()
        for ((rawKey, element) in root) {
            val key = normalizeKey(rawKey) ?: continue
            val obj = element as? JsonObject ?: continue
            val withDate = JsonObject(obj + ("date" to JsonPrimitive(key)))
            val entry = runCatching { json.decodeFromJsonElement(DayEntry.serializer(), withDate) }.getOrNull()
                ?: continue
            result[key] = entry
        }
        return result
    }

    private fun normalizeKey(rawKey: String): String? {
        val stripped = when {
            rawKey.startsWith("kt_d_") -> rawKey.removePrefix("kt_d_")
            rawKey.startsWith("kt_") -> rawKey.removePrefix("kt_")
            else -> rawKey
        }
        return stripped.takeIf { DATE_KEY_REGEX.matches(it) }
    }

    /**
     * Field-by-field merge: an imported value only fills a field that's empty
     * in [existing] (`""`, `null`, or `false` — every boolean here defaults to
     * `false`). [existing] values are never overwritten — see CLAUDE.md
     * "Import" for the exact `mergeEntries` semantics this mirrors.
     */
    fun merge(existing: DayEntry, imported: DayEntry): DayEntry = existing.copy(
        breakfast = existing.breakfast.ifEmpty { imported.breakfast },
        lunch = existing.lunch.ifEmpty { imported.lunch },
        dinner = existing.dinner.ifEmpty { imported.dinner },
        energy = existing.energy ?: imported.energy,
        happiness = existing.happiness ?: imported.happiness,
        portion = existing.portion ?: imported.portion,
        notInKeto = existing.notInKeto || imported.notInKeto,
        tested = existing.tested || imported.tested,
        notes = existing.notes.ifEmpty { imported.notes },
        breakfastKeto = existing.breakfastKeto || imported.breakfastKeto,
        lunchKeto = existing.lunchKeto || imported.lunchKeto,
        dinnerKeto = existing.dinnerKeto || imported.dinnerKeto,
        breakfastTime = existing.breakfastTime ?: imported.breakfastTime,
        lunchTime = existing.lunchTime ?: imported.lunchTime,
        dinnerTime = existing.dinnerTime ?: imported.dinnerTime,
        heart = existing.heart ?: imported.heart,
        heartNotes = existing.heartNotes.ifEmpty { imported.heartNotes },
        supplements = existing.supplements.ifEmpty { imported.supplements },
    )

    // ── SAF I/O — replaces the browser's anchor-download / FileReader dance ──

    suspend fun read(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val stream = context.contentResolver.openInputStream(uri) ?: error("no input stream")
            stream.use { it.readBytes() }.decodeToString()
        }.getOrNull()
    }

    suspend fun write(context: Context, uri: Uri, text: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val stream = context.contentResolver.openOutputStream(uri) ?: error("no output stream")
            stream.use { it.write(text.toByteArray()) }
        }.isSuccess
    }
}
