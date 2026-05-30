package com.ketotracker.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [DayEntryEntity::class], version = 1, exportSchema = false)
abstract class KetoDatabase : RoomDatabase() {

    abstract fun dayEntryDao(): DayEntryDao

    companion object {
        @Volatile private var INSTANCE: KetoDatabase? = null

        fun get(context: Context): KetoDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                KetoDatabase::class.java,
                "keto_tracker.db",
            ).build().also { INSTANCE = it }
        }
    }
}
