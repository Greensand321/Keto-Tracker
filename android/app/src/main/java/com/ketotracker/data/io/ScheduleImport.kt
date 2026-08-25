package com.ketotracker.data.io

import com.ketotracker.data.MAX_CYCLE_LENGTH_DAYS
import com.ketotracker.data.MIN_CYCLE_LENGTH_DAYS
import com.ketotracker.data.SupplementDose
import com.ketotracker.data.SupplementSchedule
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.util.UUID

/**
 * Parses and validates a user-imported supplement schedule JSON file — the format documented
 * in `SUPPLEMENT_SCHEDULE_AI_PROMPT` so any AI chat tool can produce it. Deliberately strict
 * and collects every problem rather than silently coercing, since the file is often
 * AI-generated and not hand-checked before import.
 */
object ScheduleImport {

    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class DoseDraft(val name: String = "", val dosage: String = "")

    @Serializable
    private data class ScheduleDraft(
        val name: String = "",
        val cycleLengthDays: Int = 0,
        val anchorDate: String = "",
        val days: List<List<DoseDraft>> = emptyList(),
    )

    sealed interface Result {
        data class Success(val schedule: SupplementSchedule) : Result
        data class Failure(val errors: List<String>) : Result
    }

    fun parse(text: String): Result {
        val draft = runCatching { json.decodeFromString(ScheduleDraft.serializer(), text) }
            .getOrElse { return Result.Failure(listOf("Not valid JSON: ${it.message ?: "parse error"}")) }

        val errors = mutableListOf<String>()
        if (draft.name.isBlank()) errors += "\"name\" is missing or blank"
        if (draft.cycleLengthDays !in MIN_CYCLE_LENGTH_DAYS..MAX_CYCLE_LENGTH_DAYS) {
            errors += "\"cycleLengthDays\" must be between $MIN_CYCLE_LENGTH_DAYS and $MAX_CYCLE_LENGTH_DAYS"
        }
        if (draft.days.size != draft.cycleLengthDays) {
            errors += "\"days\" has ${draft.days.size} entr${if (draft.days.size == 1) "y" else "ies"}, " +
                "but cycleLengthDays is ${draft.cycleLengthDays}"
        }
        if (runCatching { LocalDate.parse(draft.anchorDate) }.getOrNull() == null) {
            errors += "\"anchorDate\" must be an ISO date (YYYY-MM-DD)"
        }
        draft.days.forEachIndexed { i, day ->
            day.forEachIndexed { j, dose ->
                if (dose.name.isBlank()) errors += "Day ${i + 1}, item ${j + 1}: \"name\" is missing or blank"
            }
        }

        if (errors.isNotEmpty()) return Result.Failure(errors)

        return Result.Success(
            SupplementSchedule(
                id = UUID.randomUUID().toString(),
                name = draft.name.trim(),
                cycleLengthDays = draft.cycleLengthDays,
                days = draft.days.map { day -> day.map { SupplementDose(it.name.trim(), it.dosage.trim()) } },
                anchorDate = draft.anchorDate,
            ),
        )
    }
}
