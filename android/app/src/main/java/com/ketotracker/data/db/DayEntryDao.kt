package com.ketotracker.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface DayEntryDao {

    @Upsert
    suspend fun upsert(entity: DayEntryEntity)

    @Query("SELECT * FROM day_entries WHERE date = :date LIMIT 1")
    suspend fun get(date: String): DayEntryEntity?

    @Query("SELECT * FROM day_entries ORDER BY date DESC")
    suspend fun getAll(): List<DayEntryEntity>

    @Query("DELETE FROM day_entries")
    suspend fun deleteAll()
}
