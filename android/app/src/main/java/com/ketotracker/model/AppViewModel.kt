package com.ketotracker.model

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Job
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
import com.ketotracker.data.io.DataPortability
import com.ketotracker.data.io.StorageStats
import com.ketotracker.data.io.StorageUsage
import com.ketotracker.data.photo.MAX_MEAL_PHOTOS
import com.ketotracker.data.photo.MealPhoto
import com.ketotracker.data.photo.PhotoSaveResult
import com.ketotracker.data.photo.PhotoStore
import com.ketotracker.data.prefs.PrefsStore
import com.ketotracker.data.repository.DayRepository
import com.ketotracker.data.repository.FakeDayRepository
import com.ketotracker.data.repository.IDayRepository
import java.io.File
import java.time.LocalTime

class AppViewModel(
    private val repo: IDayRepository,
    private val prefs: PrefsStore?,
    private val photoStore: PhotoStore? = null,
) : ViewModel() {

    var themeId by mutableStateOf("midnight")
        private set

    // Auto-theme — native counterpart to the web app's kt_theme_auto/
    // kt_theme_dark_auto/kt_theme_light_auto (CLAUDE.md "Theme System"). When
    // [autoThemeEnabled], the UI resolves the active theme from these two IDs
    // based on the system's dark/light setting (see `resolveAutoTheme`)
    // instead of using [themeId] directly.
    var autoThemeEnabled by mutableStateOf(false)
        private set
    var darkAutoThemeId by mutableStateOf("midnight")
        private set
    var lightAutoThemeId by mutableStateOf("pearl")
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
     * One-shot, user-facing notifications — failures ("couldn't save your
     * entry") as well as plain confirmations ("Photo saved ✓"), mirroring the
     * web app's toast(message, isError). A Channel — not a state — so each
     * message is delivered exactly once and doesn't replay on
     * recomposition/rotation. The UI collects this as a Snackbar.
     */
    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages: Flow<String> = _messages.receiveAsFlow()

    // Bumped whenever photos for the active day change, so `mealPhotos()` —
    // a plain disk read with no Compose State of its own — re-triggers reads
    // on recomposition instead of going stale after add/remove.
    private var photoTick by mutableStateOf(0)

    // Set by `importFrom` once a file is parsed, so the UI can show a
    // merge/overwrite/skip choice (native counterpart of the web app's
    // chained confirm() dialogs — see CLAUDE.md "Import"). The raw decoded
    // entries stay private; the UI only ever sees the summary counts.
    var pendingImport by mutableStateOf<PendingImport?>(null)
        private set
    private var pendingNewEntries: Map<String, DayEntry> = emptyMap()
    private var pendingDupEntries: Map<String, DayEntry> = emptyMap()

    // Populated on demand by `loadStorageStats` — native counterpart of the
    // web app's `getStorageStats()` (see CLAUDE.md "Data Access"). Sizing the
    // database file and walking the photo directory does real disk I/O, so we
    // compute it lazily when Settings opens rather than keeping it live.
    var storageStats by mutableStateOf<StorageStats?>(null)
        private set

    init {
        // Observe the persisted theme preferences.
        if (prefs != null) {
            viewModelScope.launch { prefs.theme.collect { id -> themeId = id } }
            viewModelScope.launch { prefs.autoThemeEnabled.collect { on -> autoThemeEnabled = on } }
            viewModelScope.launch { prefs.darkAutoTheme.collect { id -> darkAutoThemeId = id } }
            viewModelScope.launch { prefs.lightAutoTheme.collect { id -> lightAutoThemeId = id } }
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
                val photoStore = PhotoStore(app)
                AppViewModel(repo, prefs, photoStore)
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

    /** Toggles auto-theme mode — mirrors the web app's `toggleAutoTheme()`. */
    fun toggleAutoTheme() {
        val enabled = !autoThemeEnabled
        autoThemeEnabled = enabled
        if (prefs != null) {
            viewModelScope.launch {
                runCatching { prefs.setAutoThemeEnabled(enabled) }
                    .onFailure { reportError("Couldn't save theme choice", it) }
            }
        }
    }

    /** Sets the night/day theme auto-theme switches between (CLAUDE.md "Theme System"). */
    fun setAutoThemeChoice(forDark: Boolean, id: String) {
        if (forDark) darkAutoThemeId = id else lightAutoThemeId = id
        if (prefs != null) {
            viewModelScope.launch {
                runCatching { if (forDark) prefs.setDarkAutoTheme(id) else prefs.setLightAutoTheme(id) }
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

    private fun notify(message: String) {
        _messages.trySend(message)
    }

    private fun reportError(message: String, cause: Throwable) {
        notify("$message — ${cause.message ?: "unknown error"}")
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

    // Cancelled/replaced whenever the user picks a different heart before the
    // delayed advance fires — otherwise tapping "Good" then quickly switching
    // to "Mild"/"Bad" still auto-advances the wizard past the now-selected
    // (non-Good) choice, which should require an explicit "Next" tap.
    private var heartAdvanceJob: Job? = null

    fun selectHeart(h: Heart) {
        heartAdvanceJob?.cancel()
        heartAdvanceJob = null
        update { if (h == Heart.GOOD) it.copy(heart = h, heartNotes = "") else it.copy(heart = h) }
        if (h == Heart.GOOD) {
            heartAdvanceJob = viewModelScope.launch { delay(380); next() }
        }
    }

    fun toggleNotInKeto() = update { it.copy(notInKeto = !it.notInKeto) }
    fun toggleTested() = update { it.copy(tested = !it.tested) }

    /**
     * Marks [meal] as keto and, the *first* time it's marked on today's entry,
     * stamps it with the current time. Editing a past day (reachable via the
     * summary's "Edit" buttons) never stamps "now" — that would record
     * tonight's clock time as when a historical breakfast happened — and
     * re-confirming an already-timestamped meal keeps its original time
     * instead of overwriting it.
     */
    fun markKeto(meal: Meal) {
        val now = if (isToday) LocalTime.now().let { "%02d:%02d".format(it.hour, it.minute) } else null
        update {
            when (meal) {
                Meal.BREAKFAST -> it.copy(breakfastKeto = true, breakfastTime = it.breakfastTime ?: now)
                Meal.LUNCH -> it.copy(lunchKeto = true, lunchTime = it.lunchTime ?: now)
                Meal.DINNER -> it.copy(dinnerKeto = true, dinnerTime = it.dinnerTime ?: now)
            }
        }
        next()
    }

    fun setSupplement(name: String, count: Int) = update {
        val m = it.supplements.toMutableMap()
        if (count <= 0) m.remove(name) else m[name] = count
        it.copy(supplements = m)
    }

    // ── Photos ────────────────────────────────────────────────────────────────
    // Native counterpart to the web app's getMealPhotos/addMealPhoto/
    // removeMealPhotoAt (see CLAUDE.md "IndexedDB (photo store)"). Files live
    // outside the JSON-column day entry entirely, so reads/writes go straight
    // to PhotoStore — `photoTick` is the recomposition signal that keeps the
    // UI in sync after an add/remove.

    /** Photos for [meal] on the day currently being viewed, oldest first. */
    fun mealPhotos(meal: Meal): List<MealPhoto> {
        photoTick
        return photoStore?.listPhotos(viewedKey, meal.field) ?: emptyList()
    }

    /**
     * Compresses and stores the freshly-captured photo [file] for [meal] on
     * the day being viewed *right now* — captured before the async hop so a
     * same-second day change can't misfile the photo under the wrong date.
     * [PhotoStore.addFromCapture] always deletes [file] when it's done with it.
     */
    fun addPhoto(meal: Meal, file: File) {
        val store = photoStore ?: return
        val date = viewedKey
        viewModelScope.launch {
            when (store.addFromCapture(date, meal.field, file)) {
                PhotoSaveResult.SAVED -> { photoTick++; notify("Photo saved ✓") }
                PhotoSaveResult.LIMIT_REACHED -> notify("Max $MAX_MEAL_PHOTOS photos per meal")
                PhotoSaveResult.FAILED -> notify("Could not save photo")
            }
        }
    }

    fun removePhoto(photo: MealPhoto) {
        val store = photoStore ?: return
        viewModelScope.launch {
            store.delete(photo)
            photoTick++
            notify("Photo removed")
        }
    }

    // ── Export / Import ───────────────────────────────────────────────────────
    // Native counterpart to the web app's exportAll/handleImport (CLAUDE.md
    // "Export / Import"). The UI hands us a SAF Uri obtained via
    // CreateDocument/OpenDocument; we own the JSON encode/decode and the
    // merge/overwrite/skip resolution, surfacing only a summary for the UI to
    // confirm via `pendingImport`.

    fun exportAll(context: Context, uri: Uri) {
        val snapshot = allEntries
        viewModelScope.launch {
            val text = DataPortability.encode(snapshot)
            val ok = DataPortability.write(context, uri, text)
            if (ok) notify("Exported ${snapshot.size} day${if (snapshot.size != 1) "s" else ""} ✓")
            else notify("Export failed")
        }
    }

    fun importFrom(context: Context, uri: Uri) {
        viewModelScope.launch {
            val text = DataPortability.read(context, uri)
            if (text == null) { notify("Could not read file"); return@launch }
            val decoded = DataPortability.decode(text)
            if (decoded.isEmpty()) { notify("No valid entries found in file"); return@launch }
            val newEntries = decoded.filterKeys { it !in allEntries }
            val dupEntries = decoded.filterKeys { it in allEntries }
            pendingNewEntries = newEntries
            pendingDupEntries = dupEntries
            pendingImport = PendingImport(newCount = newEntries.size, dupCount = dupEntries.size)
        }
    }

    /**
     * Resolves the pending import with the chosen [mode] for duplicate days
     * (new days are always written, mirroring the web app), bulk-persists the
     * result, and refreshes in-memory state — including the active `entry` if
     * the day being viewed was among those just written.
     */
    fun confirmImport(mode: ImportMode) {
        val newEntries = pendingNewEntries
        val dupEntries = pendingDupEntries
        pendingImport = null
        pendingNewEntries = emptyMap()
        pendingDupEntries = emptyMap()

        viewModelScope.launch {
            val toWrite = LinkedHashMap<String, DayEntry>(newEntries)
            when (mode) {
                ImportMode.MERGE -> dupEntries.forEach { (key, imported) ->
                    toWrite[key] = DataPortability.merge(allEntries[key] ?: DayEntry(date = key), imported)
                }
                ImportMode.OVERWRITE -> toWrite += dupEntries
                ImportMode.SKIP -> Unit
            }
            if (toWrite.isEmpty()) { notify("No days imported"); return@launch }

            runCatching { repo.saveAll(toWrite.values.toList()) }
                .onSuccess {
                    allEntries = allEntries + toWrite
                    toWrite[viewedKey]?.let { entry = it; stepIndex = defStep(it) }
                    val n = toWrite.size
                    val note = when {
                        dupEntries.isEmpty() -> ""
                        mode == ImportMode.MERGE -> " (merged)"
                        mode == ImportMode.OVERWRITE -> " (overwritten)"
                        else -> " · ${dupEntries.size} duplicate${if (dupEntries.size != 1) "s" else ""} skipped"
                    }
                    notify("Imported $n day${if (n != 1) "s" else ""}$note ✓")
                }
                .onFailure { reportError("Import failed", it) }
        }
    }

    fun cancelImport() {
        pendingImport = null
        pendingNewEntries = emptyMap()
        pendingDupEntries = emptyMap()
    }

    /**
     * Sizes the Room database file and the on-disk photo directory — native
     * counterpart of the web app's `getStorageStats()`. Triggered when the
     * Settings sheet opens; not kept continuously live since it touches disk.
     */
    fun loadStorageStats(context: Context) {
        val store = photoStore ?: return
        viewModelScope.launch {
            storageStats = StorageUsage.compute(context, store, allEntries.size)
        }
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

/** Summary counts shown by the import-confirmation dialog (CLAUDE.md "Import"). */
data class PendingImport(val newCount: Int, val dupCount: Int)

/** How to resolve duplicate days during import — mirrors the web app's three confirm() choices. */
enum class ImportMode { MERGE, OVERWRITE, SKIP }
