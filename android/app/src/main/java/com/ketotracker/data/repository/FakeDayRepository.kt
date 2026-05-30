package com.ketotracker.data.repository

import com.ketotracker.data.DateUtils
import com.ketotracker.data.DayEntry
import com.ketotracker.data.Heart
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory repository for Compose Previews and unit tests. Seeded with the
 * same two past days that DemoRepository used. MutableStateFlow means
 * `observeAll()` re-emits whenever a save or delete occurs, keeping previews
 * reactive even in interactive mode.
 */
class FakeDayRepository : IDayRepository {

    private val days = mutableMapOf<String, DayEntry>()
    private val stateFlow = MutableStateFlow<Map<String, DayEntry>>(emptyMap())

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
        ).forEach { put(it) }
    }

    private fun put(entry: DayEntry) {
        days[entry.date] = entry
        stateFlow.value = days.toMap()
    }

    override suspend fun load(date: String): DayEntry = days[date] ?: DayEntry(date = date)

    override suspend fun save(entry: DayEntry) = put(entry)

    override suspend fun deleteAll() {
        days.clear()
        stateFlow.value = emptyMap()
    }

    override fun observeAll(): Flow<List<DayEntry>> =
        stateFlow.map { map -> map.values.sortedByDescending { it.date } }

    fun loadSync(date: String): DayEntry = days[date] ?: DayEntry(date = date)
    fun allSync(): Map<String, DayEntry> = days.toMap()
}
