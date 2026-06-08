package com.ketotracker.data.repository

import com.ketotracker.data.DayEntry

interface IDayRepository {
    suspend fun load(date: String): DayEntry
    suspend fun loadAll(): List<DayEntry>
    suspend fun save(entry: DayEntry)
    suspend fun deleteAll()
}
