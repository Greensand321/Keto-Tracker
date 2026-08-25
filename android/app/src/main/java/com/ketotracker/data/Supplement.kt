package com.ketotracker.data

import kotlinx.serialization.Serializable

/** One recommended supplement + its dosage on a given rotation day (e.g. "Vitamin D", "5000 IU"). */
@Serializable
data class SupplementDose(
    val name: String,
    val dosage: String = "",
)

/**
 * A named, repeating supplement rotation. [days] has exactly [cycleLengthDays] entries —
 * `days[0]` is "Day 1", the day that falls on [anchorDate]. The rotation day for any date is
 * computed live from [anchorDate] (see `AppViewModel.scheduleDayIndex`) rather than stored per
 * `DayEntry`, so editing a schedule changes what every date — past or future — recommends.
 */
@Serializable
data class SupplementSchedule(
    val id: String,
    val name: String,
    val cycleLengthDays: Int,
    val days: List<List<SupplementDose>>,
    val anchorDate: String,
)

/** Bounds enforced by both the schedule editor and the importer. */
const val MIN_CYCLE_LENGTH_DAYS = 1
const val MAX_CYCLE_LENGTH_DAYS = 31

/**
 * Prompt + schema shown by Settings' "Copy AI Prompt" button — paste into any AI chat tool
 * along with your actual supplement list to get back a file "Import Schedule" can read.
 * Kept as one literal string (rather than generating it from the data classes) since it's
 * meant to be human-edited prose handed to an external tool, not app-internal serialization.
 */
val SUPPLEMENT_SCHEDULE_AI_PROMPT = """
    I take supplements on a repeating rotation. Turn the list below into a single JSON file
    matching this exact schema, and reply with ONLY the JSON — no other text:

    {
      "name": "<short schedule name>",
      "cycleLengthDays": <number of days in the rotation>,
      "anchorDate": "<YYYY-MM-DD, the date that should count as Day 1>",
      "days": [
        [ { "name": "<supplement name>", "dosage": "<amount, e.g. \"5000 IU\">" }, ... ],
        ...
      ]
    }

    "days" must have exactly cycleLengthDays entries, one array per day, in order starting
    from Day 1. Here is my rotation:

    <describe your supplements, which day(s) of the cycle you take each one, and their dosages>
""".trimIndent()
