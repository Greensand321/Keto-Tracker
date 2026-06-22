# Keto Tracker — CLAUDE.md

A guide to the codebase for AI assistants and developers working on this project.

---

## Project Overview

Keto Tracker is a **native Android** app for personal keto diet logging, built in
**Kotlin + Jetpack Compose**. It runs fully offline, stores all data locally on the
device (no account, no server, no sync), and is designed around one principle:

> **Record everything with the fewest clicks possible.**

Every interaction is tuned to remove friction — time-aware starting step, auto-advancing
ratings, one-tap quick-select chips, instant auto-save, inline editing, and swipe
navigation — so a complete day can be logged in well under a minute.

**Tech stack**: Kotlin, Jetpack Compose (Material 3), Room, DataStore Preferences,
kotlinx.serialization, Coil, WorkManager.

The app module lives in `android/`. The root `README.md` is the user-facing feature
overview; `android/README.md` carries module-level build/architecture notes.

---

## File Structure

```
android/app/src/main/java/com/ketotracker/
├── MainActivity.kt              # Single activity; hosts the Compose tree, resolves auto-theme
├── data/
│   ├── DayEntry.kt              # Core data model (one day's log)
│   ├── DayEntrySurrogate.kt     # @Serializable surrogate + DayEntry ⇄ JSON mapping
│   ├── Heart.kt                 # Heart enum (GOOD/MILD/BAD) + manual KSerializer
│   ├── Meal.kt                  # Meal enum (BREAKFAST/LUNCH/DINNER)
│   ├── Steps.kt                 # Step enum (7-step wizard) + label/placeholder/supplement constants
│   ├── Snapshot.kt              # Snapshot metadata model (feature partially built)
│   ├── DateUtils.kt             # ISO date-key helpers (todayKey, offKey, fmtDate, isToday/isFuture, monthGrid)
│   ├── db/                      # Room
│   │   ├── DayEntryEntity.kt    #   @Entity: (date TEXT PK, data TEXT) — JSON column
│   │   ├── DayEntryDao.kt       #   @Dao: upsert / saveAll / get / observeAll (Flow) / deleteAll
│   │   └── KetoDatabase.kt      #   @Database singleton (KETO_DB_NAME = "keto_tracker.db")
│   ├── repository/
│   │   ├── IDayRepository.kt    #   interface: suspend load/save/saveAll/deleteAll + Flow<List<DayEntry>>
│   │   ├── DayRepository.kt     #   Room-backed impl; encodes/decodes DayEntry ⇄ JSON
│   │   └── FakeDayRepository.kt #   in-memory impl (seeded demo data) for Compose Previews
│   ├── prefs/
│   │   └── PrefsStore.kt        #   DataStore<Preferences> — theme id + auto-theme prefs
│   ├── photo/
│   │   ├── PhotoStore.kt        #   on-disk compressed JPEGs in filesDir/photos/
│   │   └── CameraCapture.kt     #   FileProvider capture target + stale-capture cleanup
│   ├── notifications/
│   │   └── NotificationHelper.kt#   reminder notification channel + builder
│   └── io/
│       ├── DataPortability.kt   #   JSON encode/decode/merge for export & import
│       ├── SnapshotStore.kt     #   snapshot persistence helper
│       ├── ZipPortability.kt    #   zip bundling helper
│       └── StorageStats.kt      #   StorageUsage.compute() — DB file + photo dir sizing
├── model/
│   └── AppViewModel.kt          # Single ViewModel for the whole app (see "Application State")
├── work/
│   ├── BackupWorker.kt          # WorkManager: periodic JSON backup to getExternalFilesDir("backups")
│   └── ReminderWorker.kt        # WorkManager: daily reminder notification
└── ui/
    ├── theme/
    │   ├── KetoTheme.kt         # KetoColors palette, KETO_THEMES (14 themes), THEME_LIST, KetoTracker() root
    │   └── Type.kt              # KetoTypography
    └── components/ + screens/   # Compose UI (see "UI Layer")
```

Other notable paths:
- `android/app/src/main/AndroidManifest.xml` — permissions (`POST_NOTIFICATIONS`), camera
  `<queries>`, FileProvider, launcher activity, `Theme.KetoTracker` splash style.
- `android/app/src/main/res/` — `strings.xml` (`app_name`), `themes.xml`, `colors.xml`
  (`keto_bg`/`keto_gold`), launcher icons, `xml/file_paths.xml`, backup rules.
- `android/app/src/test/java/com/ketotracker/` — JVM unit tests for the data layer
  (`DateUtilsTest`, `DayRepositoryTest`).
- `android/app/build.gradle.kts`, `android/build.gradle.kts`, `android/settings.gradle.kts`
  — Gradle config.

---

## Architecture

The app follows a conventional **MVVM + Repository** structure on top of Compose. Data
flows one direction — persistence → repository → ViewModel (Compose `State`) → UI — and
user actions flow back: UI → ViewModel methods → repository (async) → persistence.

```
Room / DataStore / PhotoStore  →  IDayRepository / PrefsStore  →  AppViewModel (State)  →  Compose UI
                                                              ←  ViewModel methods  ←  user actions
```

- **Single activity, no Navigation library.** `MainActivity` hosts the whole Compose
  tree. `WizardScreen` holds an `Overlay` enum
  (`NONE/THEME/OVERVIEW/CALENDAR/SUPPLEMENTS/QUICK_SELECT/SETTINGS`) and draws the active
  overlay as a sibling `Box` on top of the wizard.
- **Dependency injection by factory.** `AppViewModel` takes an `IDayRepository` and a
  nullable `PrefsStore`, so it runs identically in production and in Compose Previews:
  - `AppViewModel.factory(application)` — production: `KetoDatabase` → `DayRepository`
    (Room) + `PrefsStore` (DataStore).
  - `AppViewModel.preview()` — design-time: `FakeDayRepository` (in-memory, seeded) and
    no `PrefsStore`, so every `@Preview` renders real-looking data without touching disk.

---

## Data Model

### `DayEntry` (one day's log) — `data/DayEntry.kt`

```kotlin
data class DayEntry(
    val date: String,                       // "YYYY-MM-DD" (primary key)
    val breakfast: String = "",
    val lunch: String = "",
    val dinner: String = "",
    val energy: Int? = null,                // 1–5 or null
    val happiness: Int? = null,             // 1–5 or null
    val portion: Int? = null,               // 1–5 or null
    val notInKeto: Boolean = false,
    val tested: Boolean = false,
    val notes: String = "",
    val breakfastKeto: Boolean = false,     // per-meal "Keto" stamp
    val lunchKeto: Boolean = false,
    val dinnerKeto: Boolean = false,
    val breakfastTime: String? = null,      // timestamp set when meal stamped keto
    val lunchTime: String? = null,
    val dinnerTime: String? = null,
    val heart: Heart? = null,               // GOOD / MILD / BAD
    val heartNotes: String = "",
    val supplements: Map<String, Int> = emptyMap(),
)
```

Helpers: `mealText(meal)` and `mealKeto(meal)` keep wizard code declarative.
`Heart` is a plain enum with a hand-written `HeartSerializer` (the generated
`@Serializable` companion breaks the Compose preview renderer).

### Persistence — Room with a JSON column (zero-migration design)

The `day_entries` table has just two columns:

```
date TEXT PRIMARY KEY     —  "2026-06-08"
data TEXT                 —  the full DayEntry, serialized to JSON
```

`DayRepository` uses `kotlinx.serialization`
(`Json { ignoreUnknownKeys = true; encodeDefaults = true }`) to convert `DayEntry ⇄ JSON`
via `DayEntrySurrogate`. Because the table schema never changes, **adding a new field to
`DayEntry` only requires giving it a default value** — no Room `Migration`, no table
rebuild, no risk to existing data. Old rows simply deserialize with the default for the
new field.

### Preferences — DataStore — `data/prefs/PrefsStore.kt`

`PrefsStore` wraps Jetpack **DataStore Preferences** and persists:
- the active theme id,
- an `autoThemeEnabled` flag,
- separate "night" and "light" theme ids for auto mode,

each exposed as a `Flow`, collected by `AppViewModel` on init.

### Photos — on-disk JPEGs — `data/photo/`

Photos live entirely outside the `DayEntry` JSON, as files (not in the DB):

- **`PhotoStore`** owns `filesDir/photos/` and stores compressed JPEGs named
  `{date}_{meal}_{timestamp}.jpg`. The timestamp makes every filename unique forever so a
  deleted name is never recycled (Coil caches by path).
- **Capture**: `createCaptureTarget()` hands the system camera app a `content://` URI via
  `FileProvider` and `ActivityResultContracts.TakePicture()` — **no `CAMERA` permission**.
  The temp file lives in `cacheDir/captures/`; `clearStaleCaptures()` sweeps it on launch.
- **Compression**: `PhotoStore.addFromCapture()` decodes with a memory-safe
  `inSampleSize`, corrects EXIF orientation, downscales to ≤900 px long edge, and
  re-encodes JPEG at quality 75. Max 5 photos per meal.
- **State**: `AppViewModel` exposes a `photoTick` counter the UI reads to re-list a meal's
  photos after add/remove (`mealPhotos()` / `addPhoto()` / `removePhoto()`).

---

## Application State — `AppViewModel`

One `ViewModel` drives the entire app, holding Compose `State`:

| State | Description |
|---|---|
| `viewedKey: String` | Currently viewed date key (`YYYY-MM-DD`) |
| `stepIndex: Int` | Current wizard step index |
| `entry: DayEntry` | In-memory copy of the viewed day being edited |
| `allEntries: Map<String, DayEntry>` | Full log, kept in memory for Overview / calendar / history |
| `themeId: String` | Active theme id (plus auto-theme state) |
| `pendingImport: PendingImport?` | Import confirmation summary (counts only) |
| `messages` | `Channel<String>` → Compose `SnackbarHost` |

All UI-driving fields are `by mutableStateOf(...)`, so any change triggers recomposition.
The core cycle is **mutate → save (async) → recompose (automatic)**: `update { transform }`
applies the change to `entry` immediately (instant UI) and launches a `viewModelScope`
coroutine to persist via the repository. There is no manual render call.

---

## Wizard Steps — `data/Steps.kt`

The core UI is a **7-step** daily logging wizard, driven by the `Step` enum.

| Index | `Step` | Field(s) | Behaviour |
|---|---|---|---|
| 0 | `BREAKFAST` | `breakfast` (+ photo, keto stamp) | Free text; Next / Keto / Skip |
| 1 | `LUNCH` | `lunch` (+ photo, keto stamp) | Free text; Next / Keto / Skip |
| 2 | `DINNER` | `dinner` (+ photo, keto stamp) | Free text; Next / Keto / Skip |
| 3 | `RATINGS` | `energy`, `happiness`, `portion` | 1–5 each; auto-advance ~380 ms after the last |
| 4 | `HEART` | `heart`, `heartNotes` | Good/Mild/Bad; auto-advances on "Good", shows notes otherwise |
| 5 | `FLAGS` | `notInKeto`, `tested`, `supplements`, `notes` | Combined Flags & Notes page |
| 6 | `SUMMARY` | — | Read-only recap with inline per-field edit |

`Step.dotted` is every step except `SUMMARY` (the ones that show a progress dot).
Rating labels (`RATING_LABELS`, `PORTION_LABELS`), placeholders (`PLACEHOLDERS`), and
default supplement chips (`SUPPLEMENT_DEFAULTS`) also live in `Steps.kt`.

### Behaviour rules
- **Meal steps (0–2)** show Next / Keto / Skip. "Keto" stamps the meal keto flag + a
  timestamp, then advances like Next. The photo area renders *below* the action row so
  buttons stay visible with the keyboard open.
- **Ratings (3)** auto-advance shortly after selection.
- **Heart (4)** auto-advances on "Good"; "Mild"/"Bad" reveal a notes field.
- **Text fields** can always be skipped.
- **Viewing a past day** jumps straight to the summary (read-only).
- **Smart start**: on open, the wizard picks the time-of-day-relevant step and skips to
  the first incomplete field.

---

## UI Layer — Jetpack Compose

### Theming — `ui/theme/KetoTheme.kt`
- `KetoColors` — a data class of the app's palette (`bg`, `surf`, `accent`, `gold`,
  `red`, `blue`, `txt`, …).
- `KETO_THEMES` — map of all **14 themes** (8 dark + 6 light) to `KetoColors`.
- `THEME_LIST` — ordered `ThemeInfo(id, emoji, label, dark)` for the picker grid.
- `LocalKetoColors` — a `CompositionLocal` making `KetoTheme.colors.xyz` available anywhere.
- `KetoTracker(themeId) { … }` — the root wrapper that provides colors and builds a
  matching Material 3 color scheme. `MainActivity` resolves auto-theme (via
  `isSystemInDarkTheme()`) before passing the effective id in.

### Components — `ui/components/`
- `Common.kt` — `KetoCard`, `StepHeading`, `Dots`, `KText`, `ketoBorder`
- `Header.kt` — `HeaderBar` (date nav + overview / theme / settings buttons)
- `Buttons.kt` — `PrimaryButton`, `BackButton`, `SkipButton`, `KetoButton` (PillButton base)
- `StepBodies.kt` — `MealBody`, `RatingsBody`, `HeartBody`, `FlagsBody`, `KetoTextArea`
- `Summary.kt` — `SummaryCard` (full read-only recap + inline edit + `PhotoIndicator`)
- `CalendarPanel.kt` — color-coded month grid + `CalMonthYearPicker` wheels
- `PhotoComponents.kt` — `MealPhotoArea`, `PhotoViewer`, `PhotoIndicator`
- `ThemePanel.kt` — bottom-sheet theme picker (scrim + swatch grid + auto toggle)

### Screens — `ui/screens/`
- `WizardScreen.kt` — top-level: wizard + header + overlay routing + swipe gestures + `BackHandler`
- `Sheets.kt` — `OverviewSheet`, `SupplementsSheet`, `QuickSelectSheet` overlays
- `SettingsSheet.kt` — about, theme shortcut, export/import, storage stats, snapshots placeholder
- `Previews.kt` — `@Preview` composables across steps / overlays / themes

### Interaction details
- **Recomposition isolation**: `key(vm.stepIndex, vm.viewedKey)` wraps `StepContent` so
  switching steps/days never bleeds stale `TextField` state.
- **Gestures**: horizontal drags accumulate as `totalDrag`, evaluated once on `onDragEnd`
  (>50 dp triggers step/day navigation).
- **System back**: `BackHandler` walks back through UI state (close photo viewer → close
  overlay → return to today → back one wizard step) and only falls through to the system
  default when at today's first step with nothing open.

---

## Calendar — `ui/components/CalendarPanel.kt`

A bottom-anchored overlay (`Overlay.CALENDAR`) opened from the header date chip.

- **Grid math**: `DateUtils.monthGrid(year, month)` returns the 42-cell (6×7,
  Sunday-first) layout — leading/current/trailing days — so the grid never collapses.
- **Colour priority** (highest wins), evaluated against in-memory `vm.allEntries`:
  - **Blue** — `tested == true` AND `notInKeto == false`
  - **Green** — ≥2 of `breakfastKeto`/`lunchKeto`/`dinnerKeto`
  - **Gold** — any log entry exists
  - No colour — nothing logged
  - Gold ring = today; white ring = currently viewed day (viewed wins over today).
- **Navigation**: tapping a day in-month jumps via `vm.jumpTo()` and closes; tapping an
  adjacent-month day re-centres the grid. Future days are dimmed and inert.
- **Month/year picker**: `CalMonthYearPicker` — two snap-scrolled `WheelColumn`s for fast
  multi-year travel; "Go →" jumps to the centred month/year, "Go to Today" jumps to now.

Because `vm.jumpTo()` falls back to a blank `DayEntry` for unlogged dates, the calendar
doubles as a "jump to any date" picker.

---

## Export / Import — `data/io/DataPortability.kt`

All pure JSON encode/decode/merge logic lives in `DataPortability`; `SettingsSheet` owns
the Storage Access Framework pickers and one confirmation dialog.

- **Pickers**: export via `ActivityResultContracts.CreateDocument("application/json")`,
  import via `OpenDocument()` (filtered to JSON); `DataPortability.write/read` stream
  through `ContentResolver` on `Dispatchers.IO`.
- **Format**: a flat, pretty-printed `{ "YYYY-MM-DD": { ...DayEntry... } }` map. `decode`
  also accepts legacy `kt_d_` / `kt_` key prefixes and forces each entry's `date` to match
  its normalised outer key.
- **Pending-import flow**: `AppViewModel.importFrom` parses off the main thread and stores
  only a `PendingImport(newCount, dupCount)` summary; `SettingsSheet` shows
  `ImportConfirmDialog` (a custom `Dialog` styled like `ThemePanel`/`KetoCard` — the
  codebase deliberately uses no Material3 `AlertDialog`).
- **Resolution**: `confirmImport(mode)` applies `DataPortability.merge` (fills only empty
  fields — `""`/`null`/`false` count as empty), full overwrite, or skip for duplicates;
  **new days are always written**. Persisted in one pass via `IDayRepository.saveAll()`.

---

## Background Work & Notifications

- **`work/BackupWorker.kt`** — WorkManager periodic job; writes a JSON backup to
  `getExternalFilesDir("backups")` (internal storage fallback), keeping the last 7 files.
- **`work/ReminderWorker.kt`** — WorkManager daily job; posts a reminder notification.
- **`data/notifications/NotificationHelper.kt`** — builds the notification channel + the
  reminder notification (gold icon tint via `R.color.keto_gold`).
- **Permissions**: only `POST_NOTIFICATIONS` (Android 13+), requested at runtime from the
  Notifications settings row. No camera or storage permission is needed.

---

## Storage Stats — `data/io/StorageStats.kt`

`StorageUsage.compute()` sizes the Room DB file directly
(`context.getDatabasePath(KETO_DB_NAME)`) and sums every JPEG in `PhotoStore`'s directory,
reporting usage against a fixed 512 MB display ceiling (native app-private storage has no
real ~5 MB quota). `AppViewModel.loadStorageStats()` runs it lazily on `Dispatchers.IO`
when Settings opens; `StorageBar` renders the total/percentage plus a day/photo breakdown.

---

## Versioning

The app version is tracked in two places that **must both be updated** on meaningful changes:

1. **`APP_VERSION`** constant in `ui/screens/SettingsSheet.kt` — shown in Settings.
2. **`versionName` / `versionCode`** in `android/app/build.gradle.kts` — the installable
   build identity (bump `versionCode` for any release the Play Store / device must treat
   as an update).

Keep `APP_VERSION` and `versionName` in sync — a native install updates by version code,
so there is no cache to invalidate.

---

## Common Patterns & Conventions

### Adding a new field to the data model
1. Add the field to `DayEntry` **with a default value** (`""`, `null`, `false`, etc.).
2. Add it to `DayEntrySurrogate` so it serializes. No Room migration is needed — old rows
   deserialize with the default.
3. Surface it in the relevant `StepBodies` composable, `SummaryCard`, and
   `DataPortability.merge` if it should participate in import merging.

### Adding a new wizard step
1. Add a `Step` enum entry (id, icon, label, title, sub) in `Steps.kt`.
2. Handle it in `StepContent()` (`WizardScreen`) with the right body composable.
3. Update smart-start / "first incomplete" logic if completeness depends on the new field.
4. Add it to `SummaryCard`.

### Adding a new theme
1. Add a `KetoColors` entry to `KETO_THEMES` in `KetoTheme.kt`.
2. Add a `ThemeInfo(id, emoji, label, dark)` to `THEME_LIST` — the picker reads it
   automatically.

---

## Known Constraints

- **Local-only**: all data is on the device. No server, no cross-device sync — use
  Export/Import (and the periodic backup) to move or back up data.
- **Photos are device-local files**: stored in app-private storage, sized into the
  storage stats, but **not** included in JSON exports.
- **No `AlertDialog`**: dialogs are custom `Dialog` composables styled like
  `KetoCard`/`ThemePanel` — match that pattern for new dialogs.
- **Snapshots** (named in-app backups) are **partially built** — a disabled placeholder in
  Settings; the data/IO scaffolding (`Snapshot.kt`, `SnapshotStore.kt`) exists.
- **Tests**: JVM unit tests cover the data layer (`src/test`); UI is verified via Compose
  Previews and manual testing.
- **Min SDK 26**, target/compile SDK 35; Kotlin 2.0, Compose BOM, KSP for Room.
</content>
