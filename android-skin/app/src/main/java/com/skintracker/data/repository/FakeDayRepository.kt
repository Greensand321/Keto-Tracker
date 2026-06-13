package com.skintracker.data.repository

import com.skintracker.data.DateUtils
import com.skintracker.data.DayEntry
import com.skintracker.data.Heart

/**
 * In-memory repository for Compose Previews and unit tests. Seeded with two
 * past days so the calendar, history, and summary screens have something to
 * show on first launch.
 */
class FakeDayRepository : IDayRepository {

    private val days = mutableMapOf<String, DayEntry>()

    init {
        val today = DateUtils.todayKey()
        listOf(
            DayEntry(
                date = DateUtils.offKey(today, -1),
                breakfast = "3 eggs, bacon, half an avocado",
                lunch = "Chicken Caesar (no croutons)",
                dinner = "Ribeye + buttered asparagus",
                energy = 4, happiness = 5, portion = 3,
                tested = true,
                breakfastKeto = true, lunchKeto = true, dinnerKeto = true,
                breakfastTime = "08:12", lunchTime = "12:40", dinnerTime = "19:05",
                heart = Heart.GOOD,
                supplements = mapOf("Magnesium" to 1, "Omega-3" to 2),
                notes = "Felt great all day, steady energy.",
            ),
            DayEntry(
                date = DateUtils.offKey(today, -2),
                breakfast = "Skipped (fasting)",
                lunch = "Tuna salad",
                dinner = "Pizza night 🍕",
                energy = 2, happiness = 3, portion = 5,
                notInKeto = true,
                lunchKeto = true,
                heart = Heart.MILD,
                heartNotes = "Slight flutter after dinner.",
            ),
        ).forEach { days[it.date] = it }
    }

    override suspend fun load(date: String): DayEntry = days[date] ?: DayEntry(date = date)

    override suspend fun loadAll(): List<DayEntry> = days.values.sortedByDescending { it.date }

    override suspend fun save(entry: DayEntry) { days[entry.date] = entry }

    override suspend fun saveAll(entries: List<DayEntry>) { entries.forEach { days[it.date] = it } }

    override suspend fun delete(key: String) { days.remove(key) }
    override suspend fun deleteAll() { days.clear() }

    fun loadSync(date: String): DayEntry = days[date] ?: DayEntry(date = date)
    fun allSync(): Map<String, DayEntry> = days.toMap()
}
