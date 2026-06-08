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

### Photos — system camera intent + on-disk JPEGs

The web app stores compressed photo blobs in IndexedDB (`Storage.photos`,
keyed `YYYY-MM-DD_meal[_idx]`, max 5 per meal — see CLAUDE.md "IndexedDB
(photo store)"). The native port keeps the same user-facing limits and
behaviour but trades IndexedDB for plain files, and CameraX for the system
camera app:

- **`PhotoStore`** (`data/photo/PhotoStore.kt`) — owns a `filesDir/photos/`
  directory of compressed JPEGs named `{date}_{meal}_{timestamp}.jpg`. The
  timestamp (rather than a reusable index slot like the web version's
  `addMealPhoto` migration logic) makes every filename unique forever, so a
  deleted photo's name is never recycled — which matters because Coil caches
  images by file path, and a recycled name could otherwise serve stale bytes.
- **Capture**: `createCaptureTarget()` hands the system camera app a
  `content://` URI via `FileProvider` (`ActivityResultContracts.TakePicture()`),
  returning a `CaptureTarget(file, uri)` pair so the UI can hold onto the
  underlying temp `File` too. No `CameraX` dependency and no `CAMERA`
  permission — capture is fully delegated to whatever camera app the user
  already has, exactly like the web app delegates to the OS via
  `<input type="file" capture="environment">`. The temp file lives in
  `cacheDir/captures/`; `PhotoStore.addFromCapture` always deletes it when
  it's done (success, failure, or a cancelled capture deletes it directly in
  `MealPhotoArea`), and `clearStaleCaptures()` sweeps the directory on every
  app launch as a safety net for process death mid-capture.
- **Compression**: `PhotoStore.addFromCapture()` reads the temp file directly
  (no `ContentResolver`/`Uri` needed), decodes with a memory-safe
  `inSampleSize`, corrects orientation from EXIF (`androidx.exifinterface`),
  downscales to ≤900px on the long edge, and re-encodes as JPEG at quality 75
  — matching the dimensions/quality documented in CLAUDE.md for the web app's
  `compressImage()`.
- **UI**: `MealPhotoArea` (thumbnails + "Add Photo"/"Add Another", rendered
  below the action row so it never hides behind the keyboard — see CLAUDE.md
  "Photo area"), `PhotoViewer` (full-screen, tap-to-dismiss — mirrors
  `#photoModal`), and `PhotoIndicator` (the `📷 N` badge in `SummaryCard`,
  mirroring `loadSummaryPhotoIcons`/`#ph-ic-{meal}`). Thumbnails and the
  viewer load via **Coil** (`coil-compose`), which handles async decoding,
  memory/disk caching, and lifecycle-aware cancellation — the native
  equivalent of the web app's `URL.createObjectURL`/`revokeObjectURL` dance.
- **State**: photo files live entirely outside the `DayEntry` JSON column, so
  `AppViewModel` exposes them via a small `photoTick` counter that the UI
  reads to know when to re-list a meal's photos after an add/remove —
  `mealPhotos()`/`addPhoto()`/`removePhoto()`.

### Calendar — color-coded month grid

The native counterpart of the web app's `.cal-panel` (CLAUDE.md "Calendar
Panel"). Opened from the header's date chip (`HeaderBar`'s `onDateClick`),
it's a bottom-anchored overlay — `Overlay.CALENDAR` in `WizardScreen` —
styled the same way as `ThemePanel` (scrim + rounded bottom card that
swallows its own touch events).

- **Grid math**: `DateUtils.monthGrid(year, month)` returns the 42-cell
  (6×7, Sunday-first) layout — leading days from the previous month, the
  full current month, and trailing days from the next — exactly mirroring
  `buildCal()`'s leading/current/trailing loops so the grid never collapses
  to fewer than six rows.
- **Colour priority** (`CalendarCell` in `CalendarPanel.kt`): the same
  3-tier system as the web app, evaluated directly against `vm.allEntries`
  (already resident in memory — no extra DB queries needed): **blue** =
  tested & on-keto, **green** = ≥2 keto meals logged, **gold** = any logged
  data, no colour = nothing logged. A gold ring marks today, a white ring
  marks the currently-viewed day — `is-viewing` wins over `is-today` when
  both apply, replicating the CSS cascade order from `.cal-day.is-today` /
  `.cal-day.is-viewing`.
- **Navigation**: tapping a day *in* the displayed month jumps straight to
  it via `vm.jumpTo()` and closes the panel (`calSelect`); tapping an
  adjacent-month day instead re-centres the grid on that month
  (`calNavMonth`) without selecting a date. Future months/days are dimmed
  and inert (`pointer-events:none` → `clickable(enabled = !isFuture)`),
  and the `›` button disables once the grid reaches the current month.
- Because `vm.jumpTo()` already falls back to a blank `DayEntry` for
  unlogged dates, the calendar doubles as the "jump to an arbitrary date"
  picker the Overview list couldn't provide — closing that parity gap too.

### Export / Import — Storage Access Framework + JSON

The native counterpart of the web app's `exportAll()`/`handleImport()`
(CLAUDE.md "Export / Import"). All pure JSON encode/decode/merge logic lives
in `data/io/DataPortability.kt`; the Settings screen owns the SAF pickers and
a single confirmation dialog.

- **File pickers replace Blob-download/`<input type="file">`**: `SettingsSheet`
  launches `ActivityResultContracts.CreateDocument("application/json")` for
  export and `ActivityResultContracts.OpenDocument()` (filtered to
  `application/json`) for import — the user picks the destination/source via
  the system file UI; `DataPortability.write/read` then stream through
  `ContentResolver` on `Dispatchers.IO`.
- **Format parity**: `DataPortability.encode/decode` keep the exact flat
  `{ "YYYY-MM-DD": {...} }` shape (pretty-printed, like the web's
  `JSON.stringify(obj, null, 2)`), and `decode` still accepts the legacy
  `kt_d_`/`kt_` key prefixes — forcing each entry's `date` field to match its
  (normalised) outer key so a malformed export can't produce an inconsistent
  record.
- **Pending-import flow**: `AppViewModel.importFrom` parses the file off the
  main thread and stores only a count summary in `pendingImport`
  (`PendingImport(newCount, dupCount)`) — the raw decoded entries stay
  private. `SettingsSheet` observes that state and shows
  `ImportConfirmDialog`, a custom `Dialog`-based overlay (the codebase has no
  Material3 `AlertDialog` usage anywhere, so this matches `ThemePanel`/
  `KetoCard` styling instead) that collapses the web app's chained `confirm()`
  calls into one screen with merge/overwrite/skip options.
- **Resolution**: `confirmImport(mode)` applies `DataPortability.merge` (fills
  empty fields only — `""`/`null`/`false` count as empty, mirroring
  `mergeEntries`), full overwrite, or skip for duplicates — new days are
  always written — then bulk-persists via `IDayRepository.saveAll()` (a new
  `@Upsert` DAO method shared with the future Snapshot-restore feature) and
  refreshes `allEntries`/`entry` in one pass.

### Auto-theme & storage stats

Two small features that round out Settings parity with the web app's
"Theme System" and "Settings Modal" (CLAUDE.md).

- **Auto-theme**: mirrors `kt_theme_auto`/`kt_theme_dark_auto`/
  `kt_theme_light_auto`. `PrefsStore` persists an `autoThemeEnabled` flag plus
  separate "night" and "light" theme IDs; `AppViewModel` exposes them and
  `toggleAutoTheme()`/`setAutoThemeChoice()`. The actual resolution —
  `resolveAutoTheme(darkId, lightId)`, which reads `isSystemInDarkTheme()` —
  has to happen in Compose (ViewModels can't observe system theme), so
  `MainActivity` picks between `vm.themeId` and the resolved auto id before
  handing it to `KetoTracker`. `ThemePanel` relabels its two sections "🌙
  Night Theme"/"☀️ Day Theme" and routes taps to `onPickAuto` (which updates
  the slot in place, mirroring `renderThemeGrid()`'s auto-mode behaviour)
  instead of applying-and-closing; an `AutoThemeToggle` row replaces the
  web's `.auto-tog` swatch tile.
- **Storage stats**: mirrors `getStorageStats()`. Browser storage
  (localStorage/IndexedDB) is quota-limited around 5 MB, but native
  app-private storage (Room + `filesDir/photos/`) isn't — so, like the web
  app's own Capacitor/native code path, `StorageUsage.compute()` sizes the
  Room database file directly (`context.getDatabasePath(KETO_DB_NAME)`) and
  sums every JPEG in `PhotoStore`'s photo directory, then reports usage
  against a fixed 512 MB display ceiling rather than a real OS limit.
  `AppViewModel.loadStorageStats()` runs this lazily on `Dispatchers.IO` when
  Settings opens (it touches disk, so it isn't kept continuously live), and
  `StorageBar` renders the real total/percentage plus a day/photo breakdown.

### UI layer — Jetpack Compose

- **Theming**: `KetoColors` is a data class mirroring the web app's CSS
  custom properties (`--bg`, `--accent`, `--gold`, …). `KETO_THEMES` holds
  all 14 themes with hex values copied 1:1 from the CSS. `LocalKetoColors`
  (a `CompositionLocal`) makes `KetoTheme.colors.xyz` available anywhere in
  the tree; `KetoTracker(themeId) { … }` is the root wrapper that provides it
  and builds a matching Material3 color scheme.
- **Navigation model**: there's no Navigation library — `WizardScreen` holds
  an `Overlay` enum (`NONE/THEME/OVERVIEW/CALENDAR/SUPPLEMENTS/QUICK_SELECT/SETTINGS`)
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
| **Photos** (camera capture, compression, storage) | ✅ Done | System camera intent + on-disk JPEGs (`PhotoStore`, `MealPhotoArea`, `PhotoViewer`) |
| **Calendar / month grid view** | ✅ Done | `CalendarPanel` — color-coded month grid, opened from the header date chip |
| **Snapshots** (named backups, restore/export) | ⬜ Missing | Disabled placeholder in Settings |
| **Export / Import** (JSON, merge/overwrite/skip) | ✅ Done | `DataPortability` (SAF + `kotlinx.serialization`), `ImportConfirmDialog` in Settings |
| **History chip strip** (recent-days row) | ⬜ Missing | Shown below wizard in web app |
| **Toast / snack-bar feedback** | ✅ Done | `AppViewModel.messages` → Compose `SnackbarHost` |
| **Auto-theme** (sync with system dark/light) | ✅ Done | `resolveAutoTheme()` + `PrefsStore` auto-theme prefs; `ThemePanel` Night/Day sections + Auto toggle |
| **Storage usage stats** | ✅ Done | `StorageUsage.compute()` sizes the Room DB file + photo directory; real `StorageBar` |
| Jump to an arbitrary (unlogged) date | ✅ Done | `CalendarPanel` lets you tap any past/present day, logged or not |

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
| `kt_theme` / `kt_theme_auto` / `kt_theme_dark_auto` / `kt_theme_light_auto` (localStorage) | `PrefsStore` (DataStore Preferences) |
| `applyAutoTheme()` / `toggleAutoTheme()` | `resolveAutoTheme()` (resolves by `isSystemInDarkTheme()`) + `AppViewModel.toggleAutoTheme/setAutoThemeChoice`, applied in `MainActivity` |
| IndexedDB photo blobs (`Storage.photos`) | `PhotoStore` — compressed JPEGs in app-private `filesDir/photos/` |
| `compressImage()` / `openCamera()` / `handleCamera()` | `PhotoStore.addFromCapture()` (EXIF-correct, downscale, JPEG-encode) + `createCaptureTarget()` + `ActivityResultContracts.TakePicture()` |
| `loadMealPhoto()` / `#photo-area` | `MealPhotoArea` (rendered below the action row on meal steps) |
| `openPhotoModal()` / `#photoModal` | `PhotoViewer` (full-screen, tap-to-dismiss) |
| `loadSummaryPhotoIcons()` / `#ph-ic-{meal}` | `PhotoIndicator` badge in `SummaryCard` |
| `.cal-panel` month grid | `CalendarPanel` (`DateUtils.monthGrid()` for the 6×7 grid math) |
| `kt__snapshots` (localStorage array) | *(not yet built — planned Room table or DataStore blob)* |
| `exportAll()` / `handleImport()` | `DataPortability.encode/decode/merge` + `AppViewModel.exportAll/importFrom/confirmImport`, via `CreateDocument`/`OpenDocument` (SAF) |
| `getStorageStats()` | `StorageUsage.compute()` — sizes the Room DB file + walks `PhotoStore`'s photo directory |
| Service worker / offline cache | Not needed — native app is offline by default |
| `toast()` | `AppViewModel.messages` (`Channel<String>`) → Compose `SnackbarHost` |

---

## Suggested next steps (in priority order)

1. **Snapshots** — same shape as export/import; up to 25 named backups (Room table or DataStore blob). Can reuse `IDayRepository.saveAll()` for restore.
2. **History chip strip** — small UI addition below the wizard, data already in `allEntries`.
