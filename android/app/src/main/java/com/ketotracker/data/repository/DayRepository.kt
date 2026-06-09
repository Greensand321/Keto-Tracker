package com.ketotracker.data.repository

import com.ketotracker.data.DayEntry
import com.ketotracker.data.DayEntrySurrogate
import com.ketotracker.data.toSurrogate
import com.ketotracker.data.db.DayEntryDao
import com.ketotracker.data.db.DayEntryEntity
import kotlinx.serialization.json.Json

class DayRepository(private val dao: DayEntryDao) : IDayRepository {

    private val json = Json {
        ignoreUnknownKeys = true  // safe to add new DayEntry fields without breaking old records
        encodeDefaults = true     // write default-valued fields so records are self-contained
    }

    override suspend fun load(date: String): DayEntry {
        val entity = dao.get(date) ?: return DayEntry(date = date)
        return decode(entity) ?: DayEntry(date = date)
    }

    override suspend fun loadAll(): List<DayEntry> = dao.getAll().mapNotNull(::decode)

    override suspend fun save(entry: DayEntry) {
        dao.upsert(DayEntryEntity(date = entry.date, data = json.encodeToString(DayEntrySurrogate.serializer(), entry.toSurrogate())))
    }

    override suspend fun saveAll(entries: List<DayEntry>) {
        dao.upsertAll(entries.map { DayEntryEntity(date = it.date, data = json.encodeToString(DayEntrySurrogate.serializer(), it.toSurrogate())) })
    }

    override suspend fun delete(key: String) = dao.deleteByDate(key)
    override suspend fun deleteAll() = dao.deleteAll()

    private fun decode(entity: DayEntryEntity): DayEntry? =
        runCatching { json.decodeFromString(DayEntrySurrogate.serializer(), entity.data).toDomain() }.getOrNull()
}
