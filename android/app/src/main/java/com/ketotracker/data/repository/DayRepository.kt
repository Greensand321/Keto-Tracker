package com.ketotracker.data.repository

import com.ketotracker.data.DayEntry
import com.ketotracker.data.db.DayEntryDao
import com.ketotracker.data.db.DayEntryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

class DayRepository(private val dao: DayEntryDao) : IDayRepository {

    private val json = Json {
        ignoreUnknownKeys = true  // safe to add new DayEntry fields without breaking old records
        encodeDefaults = true     // write default-valued fields so records are self-contained
    }

    override suspend fun load(date: String): DayEntry {
        val entity = dao.get(date) ?: return DayEntry(date = date)
        return runCatching { json.decodeFromString<DayEntry>(entity.data) }
            .getOrDefault(DayEntry(date = date))
    }

    override suspend fun save(entry: DayEntry) {
        dao.upsert(DayEntryEntity(date = entry.date, data = json.encodeToString(DayEntry.serializer(), entry)))
    }

    override suspend fun deleteAll() = dao.deleteAll()

    override fun observeAll(): Flow<List<DayEntry>> = dao.observeAll().map { entities ->
        entities.mapNotNull { entity ->
            runCatching { json.decodeFromString<DayEntry>(entity.data) }.getOrNull()
        }
    }
}
