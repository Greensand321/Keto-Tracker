package com.ketotracker.data

import kotlinx.serialization.Serializable

/**
 * Serialization surrogate for [DayEntry]: identical field layout, but annotated
 * with @Serializable so kotlinx.serialization can encode/decode it for Room
 * storage and JSON export/import.
 *
 * Kept separate from [DayEntry] so the domain class carries no kotlinx.serialization
 * dependency in its JVM static initializer (<clinit>). That dependency causes a
 * NoClassDefFoundError in Android Studio's layoutlib-based Compose Preview renderer,
 * which stubs the serialization runtime with non-functional mocks.
 *
 * Only [com.ketotracker.data.repository.DayRepository] and
 * [com.ketotracker.data.io.DataPortability] should ever reference this class.
 */
@Serializable
internal data class DayEntrySurrogate(
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
    @Serializable(with = HeartSerializer::class) val heart: Heart? = null,
    val heartNotes: String = "",
    val supplements: Map<String, Int> = emptyMap(),
) {
    fun toDomain(): DayEntry = DayEntry(
        date = date,
        breakfast = breakfast,
        lunch = lunch,
        dinner = dinner,
        energy = energy,
        happiness = happiness,
        portion = portion,
        notInKeto = notInKeto,
        tested = tested,
        notes = notes,
        breakfastKeto = breakfastKeto,
        lunchKeto = lunchKeto,
        dinnerKeto = dinnerKeto,
        breakfastTime = breakfastTime,
        lunchTime = lunchTime,
        dinnerTime = dinnerTime,
        heart = heart,
        heartNotes = heartNotes,
        supplements = supplements,
    )
}

internal fun DayEntry.toSurrogate(): DayEntrySurrogate = DayEntrySurrogate(
    date = date,
    breakfast = breakfast,
    lunch = lunch,
    dinner = dinner,
    energy = energy,
    happiness = happiness,
    portion = portion,
    notInKeto = notInKeto,
    tested = tested,
    notes = notes,
    breakfastKeto = breakfastKeto,
    lunchKeto = lunchKeto,
    dinnerKeto = dinnerKeto,
    breakfastTime = breakfastTime,
    lunchTime = lunchTime,
    dinnerTime = dinnerTime,
    heart = heart,
    heartNotes = heartNotes,
    supplements = supplements,
)
