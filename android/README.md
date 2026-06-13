# Keto Tracker — Android app module

This folder is the **native Android** Keto Tracker app, written in **Kotlin + Jetpack
Compose**. It runs fully offline and stores all data locally on the device (Room +
DataStore + on-disk photos), with no account and no network requests.

For the user-facing feature overview and the app's ethos see the root
[`README.md`](../README.md); for the full architecture and codebase guide see
[`CLAUDE.md`](../CLAUDE.md). This file covers module-level structure, the feature
checklist, and how to build.

---

## Architecture at a glance

A conventional **MVVM + Repository** structure on top of Compose. Data flows
persistence → repository → ViewModel (Compose `State`) → UI; user actions flow back the
other way.

```
app/src/main/java/com/ketotracker/
├── MainActivity.kt              # Single activity; hosts the Compose tree, resolves auto-theme
├── data/
│   ├── DayEntry.kt              # Core data model (one day's log)
│   ├── DayEntrySurrogate.kt     # @Serializable surrogate (DayEntry ⇄ JSON)
│   ├── Heart.kt / Meal.kt       # Small domain enums
│   ├── Steps.kt                 # 7-step wizard enum + label/placeholder/supplement constants
│   ├── DateUtils.kt             # ISO date-key helpers + month-grid math
│   ├── db/                      # Room: DayEntryEntity (date PK + JSON data), Dao, KetoDatabase
│   ├── repository/              # IDayRepository + DayRepository (Room) + FakeDayRepository (previews)
│   ├── prefs/PrefsStore.kt      # DataStore: theme + auto-theme prefs
│   ├── photo/                   # PhotoStore (on-disk JPEGs) + camera capture
│   ├── notifications/           # Reminder notification channel + builder
│   └── io/                      # Export/import, snapshot store, storage stats
├── model/AppViewModel.kt        # Single ViewModel driving the whole app
├── work/                        # WorkManager: periodic backup + daily reminder
└── ui/
    ├── theme/                   # KetoColors, KETO_THEMES (14 themes), KetoTracker() root
    ├── components/              # Cards, header, buttons, step bodies, summary, calendar, photos, theme panel
    └── screens/                 # WizardScreen, overlay sheets, settings, @Preview gallery
```

Key design choices:

- **Single ViewModel, no Navigation library.** `AppViewModel` holds all Compose state
  (`viewedKey` / `stepIndex` / `entry` / `allEntries` / `themeId`). `WizardScreen` routes
  overlays via an `Overlay` enum drawn on top of the wizard.
- **Room with a JSON column (zero-migration).** The `day_entries` table is just
  `date TEXT PK` + `data TEXT` (the serialized `DayEntry`). Adding a field to `DayEntry`
  only needs a default value — no SQL migration, no risk to existing data.
- **Photos as files.** Compressed JPEGs live in `filesDir/photos/`, captured via the
  system camera app through `FileProvider` (no `CAMERA` permission), loaded with Coil.
- **DI by factory.** `AppViewModel.factory(application)` wires the real Room/DataStore
  stack; `AppViewModel.preview()` uses an in-memory seeded fake so every `@Preview`
  renders real-looking data without touching disk.

---

## Feature checklist

| Feature | Status | Notes |
|---|---|---|
| 7-step wizard (breakfast → … → summary) | ✅ | `Step` enum + `StepContent` switch |
| Smart starting step (time-of-day aware) | ✅ | Opens at the relevant step, skips to first incomplete |
| Meal entry + Quick Select chips | ✅ | `MealBody`, `QuickSelectSheet` |
| Per-meal "Keto" stamp + timestamps | ✅ | `markKeto()` |
| Ratings (energy / happiness / portion) | ✅ | `RatingsBody`, auto-advance after ~380 ms |
| Heart health + conditional notes | ✅ | `HeartBody`, auto-advance on "Good" |
| Flags (Not in Keto / Tested) + supplements + notes | ✅ | `FlagsBody` (combined Flags & Notes step) |
| Day summary with inline edit | ✅ | `SummaryCard` |
| 14 themes (8 dark + 6 light) | ✅ | `KETO_THEMES` / `ThemePanel` |
| Auto-theme (follows system dark/light) | ✅ | `resolveAutoTheme()` + `PrefsStore` |
| Day navigation (prev/next, swipe) | ✅ | Future days blocked |
| Calendar / month-grid view | ✅ | `CalendarPanel` + month/year wheel picker |
| Overview list of logged days | ✅ | `OverviewSheet` (jump on tap) |
| Local persistence (Room + DataStore) | ✅ | JSON-column table; prefs via DataStore |
| Photos (capture, compress, store, view) | ✅ | `PhotoStore`, `MealPhotoArea`, `PhotoViewer` |
| Export / Import (JSON; merge/overwrite/skip) | ✅ | `DataPortability` (SAF) + `ImportConfirmDialog` |
| Periodic backup + reminder notification | ✅ | `BackupWorker`, `ReminderWorker` |
| Storage usage stats | ✅ | `StorageUsage.compute()` + `StorageBar` |
| Snackbar feedback | ✅ | `AppViewModel.messages` → `SnackbarHost` |
| Snapshots (named in-app backups) | ⬜ | Disabled placeholder in Settings; IO scaffolding present |
| History chip strip (recent-days row) | ⬜ | Not yet built; data already in `allEntries` |

---

## Build & run

Requires **Android Studio** (Ladybug or newer) or the Android command-line SDK.

### Android Studio (recommended)
1. `File → Open` and select this `android/` folder.
2. Let Gradle sync — the first sync downloads AGP / Compose / Room / KSP / DataStore /
   serialization and may take a while.
3. Pick an emulator or device and hit **Run ▶**.

### Command line
```bash
echo "sdk.dir=/path/to/Android/sdk" > local.properties
./gradlew assembleDebug      # build debug APK -> app/build/outputs/apk/debug/
./gradlew installDebug       # build + install on a connected device
./gradlew testDebugUnitTest  # run the JVM unit tests (data layer)
```

### Design-time previews (no device needed)
Open `ui/screens/Previews.kt` in Split/Design view — previews cover every wizard step,
overlay, and a sample of themes, all backed by `AppViewModel.preview()` with seeded demo
data.
</content>
