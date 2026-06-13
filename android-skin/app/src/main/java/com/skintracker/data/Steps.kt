package com.skintracker.data

/**
 * The 7-step wizard, mirroring the STEPS / META constants in index.html —
 * except FLAGS also carries the web app's standalone "notes" step, combined
 * onto one page so finishing the day takes one fewer Next tap:
 *   breakfast, lunch, dinner, ratings, heart, flags+notes, summary
 */
enum class Step(
    val id: String,
    val icon: String,
    val label: String,
    val title: String,
    val sub: String,
) {
    BREAKFAST("breakfast", "🍳", "Meal 1 of 3", "Breakfast", "What did you eat this morning?"),
    LUNCH("lunch", "🥗", "Meal 2 of 3", "Lunch", "What did you have for lunch?"),
    DINNER("dinner", "🍽️", "Meal 3 of 3", "Dinner", "What did you eat for dinner?"),
    RATINGS("ratings", "📊", "Daily Check-in", "Daily Ratings", "Rate your energy, mood, and portions."),
    HEART("heart", "💗", "Health Check", "Heart Health", "How did your heart feel today?"),
    FLAGS("flags", "🚩", "Daily Flags", "Flags & Notes", "Anything else to log before you finish?"),
    SUMMARY("summary", "✅", "Done!", "Day Summary", ""),
    ;

    val isMeal: Boolean get() = this == BREAKFAST || this == LUNCH || this == DINNER

    val meal: Meal?
        get() = when (this) {
            BREAKFAST -> Meal.BREAKFAST
            LUNCH -> Meal.LUNCH
            DINNER -> Meal.DINNER
            else -> null
        }

    companion object {
        /** Steps that show a progress dot (everything except the summary). */
        val dotted: List<Step> = entries.filter { it != SUMMARY }
    }
}

// Rating descriptors, matching RLBL / PLBL in the web app.
val RATING_LABELS = mapOf(1 to "Low", 2 to "Poor", 3 to "Okay", 4 to "Good", 5 to "Great")
val PORTION_LABELS = mapOf(1 to "Tiny", 2 to "Small", 3 to "Normal", 4 to "Large", 5 to "Huge")

// Placeholder examples, matching PH in the web app.
val PLACEHOLDERS = mapOf(
    "breakfast" to "2 eggs, bacon, avocado",
    "lunch" to "Grilled chicken salad",
    "dinner" to "Steak with broccoli",
    "notes" to "Had cravings at 3pm...",
)

// Default supplement chips (SUPP_DEFAULTS in index.html).
val SUPPLEMENT_DEFAULTS = listOf(
    "Vitamin D", "Magnesium", "Omega-3", "Zinc", "Vitamin C",
    "Probiotics", "B12", "Collagen", "CoQ10", "Electrolytes",
)
