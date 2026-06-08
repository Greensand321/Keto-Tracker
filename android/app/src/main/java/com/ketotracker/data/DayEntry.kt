package com.ketotracker.data

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

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
    @Serializable(with = HeartSerializer::class) val heart: Heart? = null,
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

// Not annotated with @Serializable — that annotation on an enum generates a companion
// object referencing kotlinx.serialization internal APIs, which Android Studio's layoutlib
// preview renderer stubs out (causing a NoClassDefFoundError on Heart.<clinit>).
// HeartSerializer achieves the same encoding using only the public API, so previews work.
enum class Heart { GOOD, MILD, BAD }

internal object HeartSerializer : KSerializer<Heart> {
    override val descriptor = PrimitiveSerialDescriptor("Heart", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: Heart) = encoder.encodeString(value.name)
    override fun deserialize(decoder: Decoder): Heart =
        Heart.entries.firstOrNull { it.name == decoder.decodeString() } ?: Heart.GOOD
}

enum class Meal(val field: String) {
    BREAKFAST("breakfast"),
    LUNCH("lunch"),
    DINNER("dinner"),
}
