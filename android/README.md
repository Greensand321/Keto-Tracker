# Keto Tracker — Native Android (v2.0)

**v1.0** is the existing single-file PWA at the repo root (`index.html`) — a
zero-dependency vanilla JS/HTML/CSS app that works well as a TWA but is
constrained by browser storage limits, lack of native camera APIs, and
PWA install friction.

**v2.0** (this `android/` project) is a ground-up **native rewrite in Kotlin +
Jetpack Compose**. The goal is feature parity with the PWA first, then to
exceed it using native capabilities the browser can't offer (CameraX,
unrestricted local storage via Room, background work, richer notifications,
etc). Every screen, theme, and interaction is being ported field-for-field
from `index.html` so the day-to-day experience stays familiar while the
underlying platform is fully native.

---

## Code architecture

The app follows a conventional **MVVM + Repository** structure on top of
Compose. Data flows in one direction: persistence layer → repository →
ViewModel (Compose `State`) → UI. User actions flow back the other way:
UI → ViewModel methods → repository (async) → persistence.

```
android/app/src/main/java/com/ketotracker/
├── MainActivity.kt              # Single activity; hosts the Compose tree
├── data/
│   ├── DayEntry.kt              # Core @Serializable data model (one day's log)
│   ├── DateUtils.kt             # ISO date-key helpers (todayKey, offKey, fmtDate, isToday/isFuture)
│   ├── Steps.kt                 # Step enum (8-step wizard) + label/placeholder constants
│   ├── db/                      # Room
│   │   ├── DayEntryEntity.kt    #   @Entity: (date TEXT PK, data TEXT) — JSON column
│   │   ├── DayEntryDao.kt       #   @Dao: upsert / get / observeAll (Flow) / deleteAll
│   │   └── KetoDatabase.kt      #   @Database singleton (Room.databaseBuilder)
│   ├── repository/
│   │   ├── IDayRepository.kt    #   interface: suspend load/save/deleteAll + Flow<List<DayEntry>>
│   │   ├── DayRepository.kt     #   Room-backed impl; encodes/decodes DayEntry ⇄ JSON
│   │   └── FakeDayRepository.kt #   in-memory impl (seeded demo data) for Compose Previews
│   └── prefs/
│       └── PrefsStore.kt        #   DataStore<Preferences> wrapper — persists theme choice
├── model/
│   └── AppViewModel.kt          # Single ViewModel for the whole app (see "State" below)
└── ui/
    ├── theme/
    │   ├── KetoTheme.kt         # KetoColors palette, KETO_THEMES map (14 themes), KetoTracker()
    │   └── Type.kt              # KetoTypography (maps web font stack → Compose Typography)
    ├── components/              # Reusable building blocks
    │   ├── Common.kt            #   KetoCard, StepHeading, Dots, KText, ketoBorder
    │   ├── Header.kt            #   HeaderBar (date nav + overview/theme/settings buttons)
    │   ├── Buttons.kt           #   PrimaryButton, BackButton, SkipButton, KetoButton (PillButton base)
    │   ├── StepBodies.kt        #   MealBody, RatingsBody, HeartBody, FlagsBody, KetoTextArea
    │   ├── Summary.kt           #   SummaryCard — full read-only day recap with inline edit
    │   └── ThemePanel.kt        #   Bottom-sheet theme picker (scrim + swatch grid)
    └── screens/
        ├── WizardScreen.kt      # Top-level screen: wizard + header + overlay routing + swipe gestures
        ├── Sheets.kt            # OverviewSheet, SupplementsSheet, QuickSelectSheet (full-screen overlays)
        ├── SettingsSheet.kt     # Settings overlay (about, theme shortcut, data, storage)
        └── Previews.kt          # 20 @Preview composables across steps/overlays/themes
```

### State management — `AppViewModel`

One `ViewModel` drives the entire app, holding Compose `State` that mirrors
the web app's three globals:

| Web global | Android equivalent |
|---|---|
| `vk` (viewed date key) | `viewedKey: String` |
| `si` (step index) | `stepIndex: Int` |
| `e` (in-memory day entry) | `entry: DayEntry` |
| *(new)* | `allEntries: Map<String, DayEntry>` — full log, kept in memory for Overview/history/calendar screens |
| `kt_theme` | `themeId: String` |

All are `by mutableStateOf(...)` so any change triggers recomposition. The
**mutate → save → render** cycle from `index.html` becomes **mutate → save
(async) → recompose (automatic)**: `update { transform }` applies the change
to `entry` immediately (UI feels instant) and fires a `viewModelScope.launch`
to persist it via the repository — Compose then recomposes on its own when
state changes, no manual `render()` call needed.

`AppViewModel` is constructed via dependency injection (`IDayRepository` +
nullable `PrefsStore`) so it works identically in production and in Compose
Previews:

- `AppViewModel.factory(application)` — production `ViewModelProvider.Factory`; wires `KetoDatabase` → `DayRepository` → Room, plus `PrefsStore` → DataStore.
- `AppViewModel.preview()` — design-time factory using `FakeDayRepository` (in-memory, pre-seeded with two demo days) and no `PrefsStore`; lets every `@Preview` render real-looking data without touching disk.

### Persistence — Room with a JSON column (zero-migration design)

Rather than a normalized multi-column table (which requires a SQL migration
every time a field is added — risky for an app in active daily use), the
`day_entries` table has just two columns:

```
date TEXT PRIMARY KEY     —  "2026-06-08"
data TEXT                 —  the full DayEntry, serialized to JSON
```

`DayRepository` uses `kotlinx.serialization` (`Json { ignoreUnknownKeys = true; encodeDefaults = true }`)
to convert `DayEntry ⇄ JSON`. Because the schema never changes, **adding a
new field to `DayEntry` only requires updating the Kotlin data class with a
default value** — exactly mirroring how the web app's `load()` fills in
missing fields on read. No `Migration` objects, no destructive table
rebuilds, no risk to existing user data.

`PrefsStore` wraps Jetpack **DataStore Preferences** (the modern
`SharedPreferences` replacement) to persist the active theme id as a
`Flow<String>`, collected by `AppViewModel` on init.

### UI layer — Jetpack Compose

- **Theming**: `KetoColors` is a data class mirroring the web app's CSS
  custom properties (`--bg`, `--accent`, `--gold`, …). `KETO_THEMES` holds
  all 14 themes with hex values copied 1:1 from the CSS. `LocalKetoColors`
  (a `CompositionLocal`) makes `KetoTheme.colors.xyz` available anywhere in
  the tree; `KetoTracker(themeId) { … }` is the root wrapper that provides it
  and builds a matching Material3 color scheme.
- **Navigation model**: there's no Navigation library — `WizardScreen` holds
  an `Overlay` enum (`NONE/THEME/OVERVIEW/SUPPLEMENTS/QUICK_SELECT/SETTINGS`)
  and draws the active overlay as a `Box` sibling on top of the wizard,
  matching the web app's modal/panel pattern.
- **Recomposition isolation**: `key(vm.stepIndex, vm.viewedKey)` wraps
  `StepContent` so switching steps or days always produces a clean composable
  instance — no stale `TextField` state bleeding between screens.
- **Gestures**: horizontal drags are accumulated as `totalDrag` and evaluated
  once on `onDragEnd` (>50dp triggers step/day navigation), avoiding the
  per-frame-delta bug where small thresholds never fire on a normal swipe.

---

## Feature status (parity with the v1.0 web app)

| Feature | Status | Notes |
|---|---|---|
| 8-step wizard (breakfast → … → summary) | ✅ Done | `Step` enum + `StepContent` switch |
| Smart starting step (`smartStep`/`defStep`) | ✅ Done | Time-of-day aware, ported 1:1 |
| Meal entry + Quick Select chips | ✅ Done | `MealBody`, `QuickSelectSheet` |
| Keto button + per-meal timestamps | ✅ Done | `markKeto()` |
| Ratings (energy/happiness/portion) | ✅ Done | `RatingsBody`, auto-advance after 380ms |
| Heart health + conditional notes | ✅ Done | `HeartBody`, auto-advance on "Good" |
| Flags (Not in Keto / Tested) + supplements | ✅ Done | `FlagsBody`, `SupplementsSheet` |
| Day summary with inline edit | ✅ Done | `SummaryCard` |
| 14 themes (8 dark + 6 light) | ✅ Done | Exact hex values from CSS |
| Theme picker panel | ✅ Done | `ThemePanel` |
| Day navigation (prev/next, swipe) | ✅ Done | Future days blocked |
| Overview list of logged days | ✅ Done | `OverviewSheet` (flat list, jump on tap) |
| **Local persistence (Room + DataStore)** | ✅ Done | JSON-column table; theme persisted via DataStore |
| **Photos** (camera capture, compression, storage) | ⬜ Missing | Needs CameraX + blob/file storage |
| **Calendar / month grid view** | ⬜ Missing | Overview is a flat list; no color-coded month grid or arbitrary-date jump |
| **Snapshots** (named backups, restore/export) | ⬜ Missing | Disabled placeholder in Settings |
| **Export / Import** (JSON, merge/overwrite/skip) | ⬜ Missing | Disabled placeholders in Settings |
| **History chip strip** (recent-days row) | ⬜ Missing | Shown below wizard in web app |
| **Toast / snack-bar feedback** | ⬜ Missing | No equivalent to `toast()` yet |
| **Auto-theme** (sync with system dark/light) | 🟡 Partial | `resolveAutoTheme()` exists but isn't wired into `PrefsStore`/`ThemePanel` |
| **Storage usage stats** | 🟡 Partial | `StorageBar` renders but always shows 0% — no real `getStorageStats()` equivalent |
| Jump to an arbitrary (unlogged) date | 🟡 Partial | Can only jump to days with existing entries, no date picker |

---

## How to build & run

You need **Android Studio** (Ladybug or newer) or the Android command-line SDK.

### Android Studio (recommended)
1. `File → Open` and select this `android/` folder.
2. Let Gradle sync — first sync downloads AGP/Compose/Room/KSP/DataStore/serialization deps and may take a while.
3. Pick an emulator or device and hit **Run ▶**.

### Design-time previews (no device needed)
Open `ui/screens/Previews.kt` and switch to **Split/Design** view — 20
previews cover every wizard step, overlay, and a sample of themes, all backed
by `AppViewModel.preview()` with seeded demo data.

### Command line
```bash
cd android
echo "sdk.dir=/path/to/Android/sdk" > local.properties
./gradlew assembleDebug          # build APK -> app/build/outputs/apk/debug/
./gradlew installDebug           # build + install on a connected device
```

---

## How v2.0 maps to the v1.0 web app

| Web (`index.html`) | Native (v2.0) |
|---|---|
| Global `vk` / `si` / `e` | `AppViewModel` Compose state (`viewedKey`/`stepIndex`/`entry`) |
| `STEPS` / `META` constants | `Step` enum (`data/Steps.kt`) |
| CSS `--bg`, `--accent`, … vars | `KetoColors` + `LocalKetoColors` |
| `[data-theme="…"]` blocks | `KETO_THEMES` map |
| `renderStep()` switch | `StepContent()` in `WizardScreen` |
| `renderSum()` | `SummaryCard` |
| `load()` / `save()` (localStorage) | `IDayRepository` → `DayRepository` (Room, JSON column) |
| `kt_theme` / `kt_theme_auto` (localStorage) | `PrefsStore` (DataStore Preferences) |
| IndexedDB photo blobs | *(not yet built — planned: CameraX + Room/file storage)* |
| `.cal-panel` month grid | *(not yet built — planned calendar screen)* |
| `kt__snapshots` (localStorage array) | *(not yet built — planned Room table or DataStore blob)* |
| `exportAll()` / `handleImport()` | *(not yet built — planned via Storage Access Framework)* |
| Service worker / offline cache | Not needed — native app is offline by default |
| `toast()` | *(not yet built — planned: Compose `SnackbarHost`)* |

---

## Suggested next steps (in priority order)

1. **Calendar / month grid + jump-to-any-date** — biggest navigation gap; pure UI + `DateUtils` math.
2. **Toast/snackbar feedback** — small effort, unblocks meaningful UX for export/import/snapshots.
3. **Export / Import** — serialize `allEntries` ⇄ JSON via Storage Access Framework + ported `mergeEntries()` logic.
4. **Snapshots** — same shape as export/import; up to 25 named backups (Room table or DataStore blob).
5. **Auto-theme wiring** — extend `PrefsStore` with dark/light preferences + auto toggle; `resolveAutoTheme()` already exists.
6. **History chip strip** — small UI addition below the wizard, data already in `allEntries`.
7. **Real storage stats** — query Room row count/size estimate for the Settings storage bar.
8. **Photos** — CameraX capture + on-device compression + storage (largest single effort; consider tackling last or in its own milestone).
