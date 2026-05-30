package com.ketotracker.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.ketotracker.data.DateUtils
import com.ketotracker.data.DayEntry
import com.ketotracker.data.DemoRepository
import com.ketotracker.data.Heart
import com.ketotracker.data.Meal
import com.ketotracker.data.Step
import java.time.LocalTime

/**
 * Holds the same three pieces of state the web app keeps as globals — the
 * viewed date (`vk`), the step index (`si`), and the in-memory entry (`ent`) —
 * but as Compose state so the UI recomposes on change. The mutate → save →
 * render cycle from index.html becomes mutate → save → (state change triggers
 * recompose) here.
 */
class AppViewModel(
    private val repo: DemoRepository = DemoRepository(),
) : ViewModel() {

    var themeId by mutableStateOf("midnight")
        private set

    var viewedKey by mutableStateOf(DateUtils.todayKey())
        private set

    var stepIndex by mutableStateOf(0)
        private set

    var entry by mutableStateOf(repo.load(DateUtils.todayKey()))
        private set

    val step: Step get() = Step.entries[stepIndex]
    val isToday: Boolean get() = DateUtils.isToday(viewedKey)
    val isFuture: Boolean get() = DateUtils.isFuture(viewedKey)

    init {
        stepIndex = defStep(entry)
    }

    fun loggedKeys(): List<String> = repo.loggedKeys()
    fun hasEntry(key: String): Boolean = repo.hasEntry(key)
    fun entryFor(key: String): DayEntry = repo.load(key)

    // ── Theme ────────────────────────────────────────────────────────────
    fun setTheme(id: String) { themeId = id }

    // ── Day navigation ─────────────────────────────────────────────────────
    fun goToday() = jumpTo(DateUtils.todayKey())

    fun changeDay(delta: Long) = jumpTo(DateUtils.offKey(viewedKey, delta))

    fun jumpTo(key: String) {
        if (DateUtils.isFuture(key)) return
        viewedKey = key
        entry = repo.load(key)
        stepIndex = defStep(entry)
    }

    // ── Step navigation ─────────────────────────────────────────────────────
    fun next() { if (stepIndex < Step.entries.lastIndex) stepIndex++ }
    fun back() { if (stepIndex > 0) stepIndex-- }
    fun skip() = next()
    fun editAt(index: Int) { stepIndex = index.coerceIn(0, Step.entries.lastIndex) }

    // ── Field updates (mutate copy → save → state replace) ───────────────────
    private fun update(transform: (DayEntry) -> DayEntry) {
        val updated = transform(entry)
        entry = updated
        repo.save(updated)
    }

    fun setMealText(meal: Meal, value: String) = update {
        when (meal) {
            Meal.BREAKFAST -> it.copy(breakfast = value)
            Meal.LUNCH -> it.copy(lunch = value)
            Meal.DINNER -> it.copy(dinner = value)
        }
    }

    fun setNotes(value: String) = update { it.copy(notes = value) }
    fun setHeartNotes(value: String) = update { it.copy(heartNotes = value) }

    fun pickRating(field: RatingField, value: Int) = update {
        when (field) {
            RatingField.ENERGY -> it.copy(energy = value)
            RatingField.HAPPINESS -> it.copy(happiness = value)
            RatingField.PORTION -> it.copy(portion = value)
        }
    }

    fun selectHeart(h: Heart) {
        update { if (h == Heart.GOOD) it.copy(heart = h, heartNotes = "") else it.copy(heart = h) }
        // Mirror web app: selHeart('good') → setTimeout(next, 380)
        if (h == Heart.GOOD) {
            viewModelScope.launch { delay(380); next() }
        }
    }

    fun toggleNotInKeto() = update { it.copy(notInKeto = !it.notInKeto) }
    fun toggleTested() = update { it.copy(tested = !it.tested) }

    /** Marks the meal keto (with a timestamp) and advances, like markKeto(). */
    fun markKeto(meal: Meal) {
        val now = LocalTime.now().let { "%02d:%02d".format(it.hour, it.minute) }
        update {
            when (meal) {
                Meal.BREAKFAST -> it.copy(breakfastKeto = true, breakfastTime = now)
                Meal.LUNCH -> it.copy(lunchKeto = true, lunchTime = now)
                Meal.DINNER -> it.copy(dinnerKeto = true, dinnerTime = now)
            }
        }
        next()
    }

    fun setSupplement(name: String, count: Int) = update {
        val m = it.supplements.toMutableMap()
        if (count <= 0) m.remove(name) else m[name] = count
        it.copy(supplements = m)
    }

    // ── Smart step (defStep + smartStep ports) ───────────────────────────────
    private fun smartStep(): Step {
        val h = LocalTime.now().hour
        return when {
            h < 10 -> Step.BREAKFAST
            h < 14 -> Step.LUNCH
            h < 20 -> Step.DINNER
            else -> Step.RATINGS
        }
    }

    private fun isIncomplete(s: Step, e: DayEntry): Boolean = when (s) {
        Step.BREAKFAST -> e.breakfast.isEmpty()
        Step.LUNCH -> e.lunch.isEmpty()
        Step.DINNER -> e.dinner.isEmpty()
        Step.RATINGS -> e.energy == null || e.happiness == null || e.portion == null
        Step.HEART -> e.heart == null
        else -> false
    }

    private fun defStep(e: DayEntry): Int {
        if (!DateUtils.isToday(viewedKey)) return Step.SUMMARY.ordinal
        val candidates = Step.entries.filter {
            it != Step.FLAGS && it != Step.NOTES && it != Step.SUMMARY
        }
        val incomplete = candidates.filter { isIncomplete(it, e) }
        if (incomplete.isEmpty()) return Step.SUMMARY.ordinal

        val ss = smartStep()
        if (ss in incomplete) return ss.ordinal
        for (i in 0..ss.ordinal) {
            if (Step.entries[i] in incomplete) return i
        }
        return incomplete.first().ordinal
    }
}

enum class RatingField { ENERGY, HAPPINESS, PORTION }
