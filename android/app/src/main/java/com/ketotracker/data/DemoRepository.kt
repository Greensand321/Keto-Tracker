package com.ketotracker.data

/**
 * In-memory store for the interface demo. Mirrors the web app's load()/save()
 * contract (get a day or a blank default; write a day back) so swapping in a
 * Room-backed implementation later is a drop-in change behind this interface.
 *
 * Seeded with a few recent days so the calendar, history, and summary screens
 * have something to show on first launch.
 */
class DemoRepository {
    private val days = mutableMapOf<String, DayEntry>()

    init {
        val today = DateUtils.todayKey()
        save(
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
            )
        )
        save(
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
            )
        )
    }

    fun load(key: String): DayEntry = days[key] ?: DayEntry(date = key)

    fun save(entry: DayEntry) {
        days[entry.date] = entry
    }

    /** All logged day keys, newest first. */
    fun loggedKeys(): List<String> = days.keys.sortedDescending()

    fun hasEntry(key: String): Boolean = days.containsKey(key)
}
