package com.ketotracker.data.notifications

/**
 * Motivational reminder body copy. Grouped by which meal(s) are still missing so the
 * notification stays context-aware (same completeness logic `ReminderReceiver` already
 * used), with 25 variants per state (100 total) so the daily nudge doesn't repeat for
 * months. [pick] also excludes anything in `ReminderReceiver`'s recently-shown history —
 * see [RECENT_HISTORY_SIZE] — so the same line can't coincidentally repeat within the
 * current rolling week even though the draw is random.
 */
object ReminderMessages {

    /** How many of the most-recently-shown messages to avoid repeating — roughly a week's worth of daily fires. */
    const val RECENT_HISTORY_SIZE = 7

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
        "The best time to log was after breakfast. The second best time is right now. ⏳",
        "Consistency isn't built in a day, but it's broken in one. Don't let today be that day. 🧱",
        "You didn't come this far to stop tracking now. Log today. 🏔️",
        "Every empty day in the log is a question mark. Turn it into an answer. ❓",
        "Ketosis rewards the consistent, not the perfect. Log something today. 🔥",
        "Zero entries, zero excuses. Start with whatever you remember. 📝",
        "The scale doesn't lie, and neither does the log. Fill it in. ⚖️",
        "One log today keeps the guesswork away. 🧠",
        "Your commitment shows up in the small, boring moments — like this one. Log now. 🎯",
        "Nobody regrets a log entry. Plenty regret skipping one. Go. 📓",
        "The days you don't feel like logging are exactly the days that matter most. 💪",
        "Momentum is a log entry away. Don't let it stall today. 🚀",
        "Today's still open. Close it out with a log before it closes on you. 🕓",
        "A tracked day is a day you own. An untracked one just happens to you. 🏆",
        "You made the food choices — now make them count. Log today. 🥑",
        "Progress hides in the data. Give it something to work with. 📈",
        "Log it now, forget the guilt later. Your log, your rules, your win. ✅",
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
        "You've done the hard part twice today. Do it a third time and be done. 💯",
        "Dinner's the final rep of the day. Don't skip the last one. 🏋️",
        "The finish line is right there — log dinner and cross it. 🎯",
        "Two-thirds of a perfect day is good. All of it is better. Log dinner. 🥇",
        "Don't leave the job half-finished when it's this close to done. 🔧",
        "Tonight's meal is the closing argument. Make your case. Log it. ⚖️",
        "One tap between you and a fully logged day. Take it. ✅",
        "The day's almost written — dinner's the last sentence. 📖",
        "You've earned the finish. Log dinner and take the win. 🏆",
        "Last call for today's log — dinner won't log itself. 🔔",
        "Close strong. Whatever's for dinner, log it before it's forgotten. 🍽️",
        "This is the easy part — you already did the hard meals. Log dinner. 💪",
        "Don't let the day end on an open question. Log dinner. ❓",
        "The streak lives or dies on this last entry. Keep it alive. 🔥",
        "Nearly a clean sweep today. Log dinner and make it official. 🧹",
        "Every perfect day starts with logging the last meal. This is that moment. ⭐",
        "You're one log away from calling today a win. Do it. ✅",
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
        "The middle of the day still needs its moment. Log lunch. 🕛",
        "Half-logged is half-finished. Close the gap with lunch. 🔄",
        "Memory fades fast — log lunch before you forget what it even was. ⏳",
        "Your log has a gap right in the middle. Patch it with lunch. 🩹",
        "Dinner doesn't tell the whole story. Add lunch and complete it. 📚",
        "A day's log is only as strong as its weakest gap. Fix lunch. 🔗",
        "You're not done yet — lunch is still unaccounted for. 🔍",
        "Two out of three isn't the goal. Log lunch and make it three. 🎯",
        "The middle meal matters too. Give lunch its due. 🥙",
        "Don't let lunch become the meal that got away. Log it now. 🎣",
        "Every gap in the log is a gap in the story. Fill it with lunch. 📝",
        "You circled back for a reason — finish the job with lunch. 🔁",
        "The record's incomplete without lunch. One tap fixes that. ✅",
        "Consistency means the middle counts as much as the ends. Log lunch. ⚖️",
        "Lunch is the quiet meal that's easy to forget. Don't. 🤫",
        "A full picture needs all three frames. Add the missing one. 🖼️",
        "You're this close to a complete day — lunch is the last piece. 🧩",
        "Don't let lunch slip through the cracks. Log it now. 🕳️",
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
        "The day began with breakfast — make sure the log remembers that too. 📓",
        "Two meals down, one still missing from the beginning. Log breakfast. 🕰️",
        "Every story needs its opening line. Yours is breakfast. Log it. 📖",
        "You wrapped up the day — now go back and open it properly. 🔓",
        "Breakfast set the tone for today. Give it credit in the log. 🎬",
        "The morning meal is still an open item. Close it out. ✅",
        "A day logged end-to-end beats one with a missing start. Add breakfast. 🏁",
        "Don't let the first meal be the forgotten one. Log breakfast. 🧠",
        "You handled lunch and dinner — breakfast is the easy one left. 🍳",
        "Fill in where today began. Breakfast is still unlogged. 🌄",
        "The record's missing its first chapter. Write it in. 📚",
        "One last gap to close — breakfast, and then today's fully yours. 🔒",
        "Don't skip the beginning just because the ending's already written. 🖋️",
        "Breakfast set your macros in motion — log it and complete the picture. 📊",
        "The day's bookends matter. You've got the end — now log the start. 📕",
        "A quick note on breakfast finishes what you already started. ✍️",
        "You remembered the big meals — breakfast deserves the same. 🍳",
        "Go back and give this morning its due. Log breakfast. ⏮️",
    )

    fun forNothingLogged(recentlyShown: List<String> = emptyList()): String = pick(NOTHING_LOGGED, recentlyShown)
    fun forDinnerMissing(recentlyShown: List<String> = emptyList()): String = pick(DINNER_MISSING, recentlyShown)
    fun forLunchMissing(recentlyShown: List<String> = emptyList()): String = pick(LUNCH_MISSING, recentlyShown)
    fun forBreakfastMissing(recentlyShown: List<String> = emptyList()): String = pick(BREAKFAST_MISSING, recentlyShown)

    /**
     * Picks randomly from [pool], excluding anything in [recentlyShown] — this is what
     * keeps a message from repeating within the current rolling week. Falls back to the
     * full pool if every candidate happens to be excluded (never actually happens with a
     * 25-message pool and a 7-message history, but stays correct if either changes).
     */
    private fun pick(pool: List<String>, recentlyShown: List<String>): String {
        val candidates = pool.filter { it !in recentlyShown }
        return candidates.ifEmpty { pool }.random()
    }
}
