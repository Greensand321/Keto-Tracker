# Keto Tracker — Native Android (Interface Demo)

A native **Jetpack Compose** rebuild of the Keto Tracker interface. This is the
first milestone of the "Option 3" full-native rewrite: a faithful, fully
interactive port of the **daily logging wizard**, the **14-theme system**, the
**day summary**, and the supporting overlays (overview, supplements, quick
select, theme picker).

It is wired to an **in-memory repository** for now — every screen works and
edits persist for the session, but nothing is written to disk yet. Swapping in
a Room-backed store later is a drop-in change behind `DemoRepository`.

---

## What's implemented

| Area | Status | Notes |
|------|--------|-------|
| 8-step wizard | ✅ | breakfast → lunch → dinner → ratings → heart → flags → notes → summary |
| Smart start step | ✅ | `defStep()` / `smartStep()` ported 1:1 (time-of-day aware) |
| Combined ratings | ✅ | Energy / Happiness / Portions, 1–5 with labels |
| Heart health | ✅ | Good / Mild / Bad, with conditional notes box |
| Flags + supplements | ✅ | "Not in Keto" / "Tested" toggles + supplement counter |
| Keto button + timestamps | ✅ | Marks a meal keto and stamps the time, like `markKeto()` |
| Day summary | ✅ | Read-only recap with per-row edit buttons |
| 14 themes | ✅ | Dark + light, identical colour values to the web app |
| Theme picker | ✅ | Bottom panel with swatches |
| Day navigation | ✅ | Prev/next day, future days blocked |
| Swipe gestures | ✅ | Left/right = step nav (or day nav on summary) |
| Overview (all days) | ✅ | Tap a day to jump to it |
| Quick Select | ✅ | Tap food chips to build a meal string |
| Photos | ⬜ | Not in this milestone (needs CameraX + storage) |
| Calendar month grid | ⬜ | Overview list stands in for now |
| Persistence (Room) | ⬜ | In-memory only this milestone |
| Snapshots / export-import | ⬜ | Future milestone |

---

## How to build & run

You need **Android Studio** (Ladybug or newer) or the Android command-line SDK.

### Android Studio (recommended)
1. `File → Open` and select this `android/` folder.
2. Let Gradle sync (it downloads the AGP/Compose dependencies).
3. Pick an emulator or device and hit **Run ▶**.

### Design-time preview (no device needed)
Open `ui/screens/Previews.kt` and switch to the **Split/Design** view. Three
previews render the interface in Midnight, Pearl, and Forest themes.

### Command line
```bash
cd android
# Point Gradle at your SDK (or set ANDROID_HOME):
echo "sdk.dir=/path/to/Android/sdk" > local.properties
./gradlew assembleDebug          # build APK -> app/build/outputs/apk/debug/
./gradlew installDebug           # build + install on a connected device
```

> The Gradle wrapper (`./gradlew`) is committed. A full build requires the
> Android SDK with platform 35; the sandbox this was authored in had the JDK +
> Gradle but no SDK, so the build has not been run here — expect to resolve the
> usual first-sync SDK/version nudges in Android Studio.

---

## Project layout

```
android/
├── build.gradle.kts            # Plugin versions (AGP 8.7.2, Kotlin 2.0.21)
├── settings.gradle.kts
├── app/
│   ├── build.gradle.kts        # compileSdk 35, minSdk 26, Compose BOM
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── res/                # launcher icon (adaptive), themes, strings
│       └── java/com/ketotracker/
│           ├── MainActivity.kt
│           ├── data/           # DayEntry, Step/META, DateUtils, DemoRepository
│           ├── model/          # AppViewModel (vk / si / ent state)
│           └── ui/
│               ├── theme/      # KetoTheme.kt = the 14-theme system
│               ├── components/ # Card, Dots, buttons, step bodies, summary, header, theme panel
│               └── screens/    # WizardScreen, Sheets, Previews
```

### How it maps to the web app

| Web (`index.html`) | Native |
|---|---|
| Global `vk` / `si` / `ent` | `AppViewModel` Compose state |
| `STEPS` / `META` constants | `Step` enum (`data/Steps.kt`) |
| CSS `--bg`, `--accent`, … vars | `KetoColors` + `LocalKetoColors` |
| `[data-theme="…"]` blocks | `KETO_THEMES` map |
| `renderStep()` switch | `StepContent()` in `WizardScreen` |
| `renderSum()` | `SummaryCard` |
| `load()` / `save()` | `DemoRepository` (Room later) |
| Service worker / offline | Not needed — native app is offline by default |

---

## Next milestones (not in this demo)

1. **Room persistence** — replace `DemoRepository` with a Room database + DataStore
   for prefs (theme, auto-theme).
2. **Photos** — CameraX capture + on-device JPEG compression, stored as files.
3. **Calendar** — month grid with the 3-tier colour priority and the scroll-wheel picker.
4. **Snapshots + export/import** — kotlinx.serialization + Storage Access Framework.
5. **Data migration** — import the web app's exported JSON so existing users keep their history.
