package com.ketotracker.data

import kotlinx.serialization.Serializable

/**
 * One day's log. Field-for-field port of the web app's daily entry object.
 * `@Serializable` lets kotlinx.serialization convert it to/from the JSON text
 * column in Room. Adding new fields here (with defaults) requires no DB migration.
 */
@Serializable
data class DayEntry(
    val date: String,
    val breakfast: String = "",
    val lunch: String = "",
    val dinner: String = "",
    val energy: Int? = null,
    val happiness: Int? = null,
    val portion: Int? = null,
    val notInKeto: Boolean = false,
    val tested: Boolean = false,
    val notes: String = "",
    val breakfastKeto: Boolean = false,
    val lunchKeto: Boolean = false,
    val dinnerKeto: Boolean = false,
    val breakfastTime: String? = null,
    val lunchTime: String? = null,
    val dinnerTime: String? = null,
    val heart: Heart? = null,
    val heartNotes: String = "",
    val supplements: Map<String, Int> = emptyMap(),
) {
    /** Generic getter used by the wizard so step code can stay declarative. */
    fun mealText(meal: Meal): String = when (meal) {
        Meal.BREAKFAST -> breakfast
        Meal.LUNCH -> lunch
        Meal.DINNER -> dinner
    }

    fun mealKeto(meal: Meal): Boolean = when (meal) {
        Meal.BREAKFAST -> breakfastKeto
        Meal.LUNCH -> lunchKeto
        Meal.DINNER -> dinnerKeto
    }
}

@Serializable
enum class Heart { GOOD, MILD, BAD }

enum class Meal(val field: String) {
    BREAKFAST("breakfast"),
    LUNCH("lunch"),
    DINNER("dinner"),
}
