package com.ketotracker.data.repository

import com.ketotracker.data.DayEntry
import kotlinx.coroutines.flow.Flow

interface IDayRepository {
    suspend fun load(date: String): DayEntry
    suspend fun save(entry: DayEntry)
    suspend fun deleteAll()
    fun observeAll(): Flow<List<DayEntry>>
}
