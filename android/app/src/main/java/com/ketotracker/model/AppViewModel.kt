package com.ketotracker.model

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import com.ketotracker.data.DateUtils
import com.ketotracker.data.DayEntry
import com.ketotracker.data.Heart
import com.ketotracker.data.Meal
import com.ketotracker.data.Step
import com.ketotracker.data.db.KetoDatabase
import com.ketotracker.data.prefs.PrefsStore
import com.ketotracker.data.repository.DayRepository
import com.ketotracker.data.repository.FakeDayRepository
import com.ketotracker.data.repository.IDayRepository
import java.time.LocalTime

class AppViewModel(
    private val repo: IDayRepository,
    private val prefs: PrefsStore?,
) : ViewModel() {

    var themeId by mutableStateOf("midnight")
        private set

    var viewedKey by mutableStateOf(DateUtils.todayKey())
        private set

    var stepIndex by mutableStateOf(0)
        private set

    var entry by mutableStateOf(DayEntry(date = DateUtils.todayKey()))
        private set

    // All logged entries kept in memory for the overview/history screens.
    var allEntries by mutableStateOf<Map<String, DayEntry>>(emptyMap())
        private set

    val step: Step get() = Step.entries[stepIndex]
    val isToday: Boolean get() = DateUtils.isToday(viewedKey)
    val isFuture: Boolean get() = DateUtils.isFuture(viewedKey)

    /**
     * One-shot, user-facing error notifications (e.g. "couldn't save your entry").
     * A Channel — not a state — so each message is delivered exactly once and
     * doesn't replay on recomposition/rotation. The UI collects this as a Snackbar.
     */
    private val _errors = Channel<String>(Channel.BUFFERED)
    val errors: Flow<String> = _errors.receiveAsFlow()

    init {
        // Observe the persisted theme preference.
        if (prefs != null) {
            viewModelScope.launch {
                prefs.theme.collect { id -> themeId = id }
            }
        }

        // Load the full log ONCE. allEntries is then a plain in-memory cache
        // that `update()` keeps in sync directly — we deliberately do NOT
        // reactively re-query+re-decode the whole table on every write (that
        // would mean every keystroke triggers an O(total days logged) reload).
        viewModelScope.launch {
            val all = repo.loadAll().associateBy { it.date }
            allEntries = all
            val today = DateUtils.todayKey()
            val todayEntry = all[today] ?: DayEntry(date = today)
            entry = todayEntry
            stepIndex = defStep(todayEntry)
        }
    }

    // ── Companion: factories ─────────────────────────────────────────────────

    companion object {
        /** Production factory — wires Room + DataStore. */
        fun factory(app: Application): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val db = KetoDatabase.get(app)
                val repo = DayRepository(db.dayEntryDao())
                val prefs = PrefsStore(app)
                AppViewModel(repo, prefs)
            }
        }

        /** Preview factory — in-memory repo, no DataStore, seeded with demo data. */
        fun preview(): AppViewModel {
            val repo = FakeDayRepository()
            val vm = AppViewModel(repo, null)
            // Populate state synchronously so Compose Previews have data to render.
            val today = DateUtils.todayKey()
            vm.allEntries = repo.allSync()
            vm.entry = repo.loadSync(today)
            vm.stepIndex = vm.defStep(vm.entry)
            return vm
        }
    }

    // ── Accessors ────────────────────────────────────────────────────────────

    fun loggedKeys(): List<String> = allEntries.keys.sortedDescending()
    fun hasEntry(key: String): Boolean = allEntries.containsKey(key)
    fun entryFor(key: String): DayEntry = allEntries[key] ?: DayEntry(date = key)

    // ── Theme ────────────────────────────────────────────────────────────────

    fun setTheme(id: String) {
        themeId = id
        if (prefs != null) {
            viewModelScope.launch {
                runCatching { prefs.setTheme(id) }
                    .onFailure { reportError("Couldn't save theme choice", it) }
            }
        }
    }

    // ── Day navigation ────────────────────────────────────────────────────────

    fun goToday() = jumpTo(DateUtils.todayKey())

    fun changeDay(delta: Long) = jumpTo(DateUtils.offKey(viewedKey, delta))

    /**
     * allEntries is fully loaded at startup and kept in sync locally by
     * `update()`, so every date's data is already in memory — no DB read needed.
     */
    fun jumpTo(key: String) {
        if (DateUtils.isFuture(key)) return
        viewedKey = key
        entry = allEntries[key] ?: DayEntry(date = key)
        stepIndex = defStep(entry)
    }

    // ── Step navigation ───────────────────────────────────────────────────────

    fun next() { if (stepIndex < Step.entries.lastIndex) stepIndex++ }
    fun back() { if (stepIndex > 0) stepIndex-- }
    fun skip() = next()
    fun editAt(index: Int) { stepIndex = index.coerceIn(0, Step.entries.lastIndex) }

    // ── Field updates ─────────────────────────────────────────────────────────

    /**
     * Mutates the active entry, updates the in-memory cache immediately (so the
     * UI and `allEntries` never disagree), then persists asynchronously. We
     * update `allEntries` here — directly, from the value we already have —
     * rather than re-reading the whole table after every save.
     */
    private fun update(transform: (DayEntry) -> DayEntry) {
        val updated = transform(entry)
        entry = updated
        allEntries = allEntries + (updated.date to updated)
        viewModelScope.launch {
            runCatching { repo.save(updated) }
                .onFailure { reportError("Couldn't save your entry", it) }
        }
    }

    private fun reportError(message: String, cause: Throwable) {
        _errors.trySend("$message — ${cause.message ?: "unknown error"}")
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
        if (h == Heart.GOOD) {
            viewModelScope.launch { delay(380); next() }
        }
    }

    fun toggleNotInKeto() = update { it.copy(notInKeto = !it.notInKeto) }
    fun toggleTested() = update { it.copy(tested = !it.tested) }

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

    // ── Smart step logic ──────────────────────────────────────────────────────

    private fun smartStep(): Step {
        val h = LocalTime.now().hour
        return when {
            h < 10 -> Step.BREAKFAST
            h < 14 -> Step.LUNCH
            h < 20 -> Step.DINNER
            else -> Step.RATINGS
        }
    }

    internal fun isIncomplete(s: Step, e: DayEntry): Boolean = when (s) {
        Step.BREAKFAST -> e.breakfast.isEmpty()
        Step.LUNCH -> e.lunch.isEmpty()
        Step.DINNER -> e.dinner.isEmpty()
        Step.RATINGS -> e.energy == null || e.happiness == null || e.portion == null
        Step.HEART -> e.heart == null
        else -> false
    }

    internal fun defStep(e: DayEntry): Int {
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
