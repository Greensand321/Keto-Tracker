package com.ketotracker.data.notifications

/**
 * Motivational reminder body copy. Grouped by which meal(s) are still missing so the
 * notification stays context-aware (same completeness logic `ReminderReceiver` already
 * used), but each state now has a pool of variants instead of one fixed line, so the
 * daily reminder doesn't read identically forever. A random variant is drawn each time
 * the reminder actually fires — see `ReminderReceiver.maybeShowReminder`.
 */
object ReminderMessages {

    /** Nothing logged yet today. */
    private val NOTHING_LOGGED = listOf(
        "A blank log is a blank slate — fill in today before it fills itself with excuses. 🥑",
        "Discipline is remembering what you want. Open Keto Tracker and log today. 💪",
        "You can't manage what you don't measure. Two minutes, one log, zero regrets. 📊",
        "Future you is checking this log later — give them something to be proud of. 🔥",
        "The streak doesn't keep itself. Log today's meals and keep it alive. ⏰",
        "Small daily logs beat big monthly regrets. Start now. 🍳",
        "Nothing logged yet today — your ketones won't track themselves. Let's go. 🥩",
        "Show up for yourself today. One tap logs the whole day. ✅",
    )

    /** Breakfast + lunch done, dinner is the only thing left. */
    private val DINNER_MISSING = listOf(
        "One meal left. Finish strong — log tonight's dinner. 🍽️",
        "You're one entry away from a perfect day. Don't stop now. 🔥",
        "Almost there — dinner's the last box to check tonight. ✅",
        "The hardest part is already done. Close it out with dinner. 💪",
        "Two down, one to go. Log dinner and call it a win. 🥗",
        "Finish what you started — tonight's meal is the last mile. 🏁",
        "Don't let a perfect streak slip on the last meal. Log dinner. ⏰",
        "So close. Log dinner and end the day undefeated. 🍽️",
    )

    /** Dinner already logged, lunch is the gap (breakfast may or may not be done). */
    private val LUNCH_MISSING = listOf(
        "Dinner's in, but lunch is still an open question. Fill the gap. 🥗",
        "Don't leave a hole in the middle of your day's story. Log lunch. 📖",
        "A complete log tells the real story — lunch is still missing. 🍽️",
        "Backfill lunch now before the details fade. 🧠",
        "One quick entry stands between you and a complete day. Log lunch. ✅",
        "Lunch is the missing piece — snap it into place. 🧩",
        "You remembered dinner — now give lunch the same respect. 🥗",
    )

    /** Lunch + dinner done, only breakfast is unlogged. */
    private val BREAKFAST_MISSING = listOf(
        "Circle back and log breakfast — every meal counts toward the full picture. 🍳",
        "A day's log isn't complete without where it started. Add breakfast. 🌅",
        "Don't let this morning's meal go unrecorded. Log breakfast now. 🍳",
        "The first meal deserves the same discipline as the last. Log it. 💪",
        "One tap fills the gap — breakfast is still waiting. ✅",
        "You finished the day strong — now close the loop with breakfast. 🔁",
        "Good logs start at the start. Add this morning's breakfast. 🌤️",
    )

    fun forNothingLogged(): String = NOTHING_LOGGED.random()
    fun forDinnerMissing(): String = DINNER_MISSING.random()
    fun forLunchMissing(): String = LUNCH_MISSING.random()
    fun forBreakfastMissing(): String = BREAKFAST_MISSING.random()
}
