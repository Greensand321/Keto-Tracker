# Keto Tracker

A fast, no-nonsense **native Android** app for personal keto diet logging — built in
Kotlin and Jetpack Compose. Log meals, ketone tests, energy, mood, portions, heart
health, supplements, and meal photos, all stored locally on your device with no
account and no internet connection required.

---

## Ethos — record everything with the fewest clicks possible

Every design decision in this app serves one goal: **capture a complete daily record
while asking the least of you.** A tracker only works if you actually use it, and the
fastest way to lose a habit is friction. So the app is relentless about removing taps:

- **It opens where you already are.** The wizard starts on the step that matches the
  time of day — breakfast in the morning, dinner in the evening — and skips ahead to
  the first thing you *haven't* filled in yet. No scrolling to find your place.
- **Ratings advance themselves.** Tap a 1–5 rating and the app moves to the next step
  automatically. One tap = one logged field.
- **Common answers are one tap away.** Quick-select chips offer your recent and typical
  meals; a single tap fills the field. Supplement chips work the same way.
- **Nothing needs saving.** Every keystroke and tap is persisted instantly. There is no
  save button, no "are you sure", no way to lose data.
- **Skipping is first-class.** Any field can be left blank and skipped. A partial day is
  still a logged day — the app never blocks you to demand completeness.
- **Editing is inline.** The day summary has an edit button next to every field, so
  fixing one value is a single tap, not a walk back through the whole wizard.
- **Swipe to move.** Swipe left/right to change steps (or days on the summary), the same
  gesture you already use everywhere else.

The result: a full day — three meals, three ratings, heart check, flags, notes,
supplements, even photos — can be logged in well under a minute, and a quick "still on
keto today" check takes only a few taps.

---

## Features

### Daily logging wizard
A 7-step guided flow captures each day in order, then hands you a summary:

1. **Breakfast** — free-text meal description (+ optional photo, + "Keto" stamp)
2. **Lunch** — free-text meal description (+ optional photo, + "Keto" stamp)
3. **Dinner** — free-text meal description (+ optional photo, + "Keto" stamp)
4. **Daily Ratings** — energy, mood, and portion size, each 1–5 (auto-advancing)
5. **Heart Health** — Good / Mild / Bad, with conditional notes when it isn't "Good"
6. **Flags & Notes** — "Not in Keto" and "Tested" toggles, supplements, and free-text notes
7. **Day Summary** — a full read-only recap with an inline edit button on every field

The wizard is **time-aware**: it opens at the most relevant step for the current hour and
jumps to the first incomplete field. Rating steps auto-advance after a brief delay. Text
steps can be skipped freely, and you can move back and forth at any time.

### Meal extras
- **Per-meal "Keto" stamp** — one tap marks a meal as keto and timestamps it.
- **Quick Select** — chips of recent/typical meals fill a field in one tap.
- **Photos** — capture a meal photo with your system camera; it's auto-rotated (EXIF),
  downscaled to ≤900 px, and compressed to JPEG on-device. Up to 5 per meal. Photos show
  as thumbnails on the meal step, a `📷 N` badge on the summary, and full-screen on tap.

### Day navigation
- **Prev/Next** day buttons in the header (future days are blocked).
- **Swipe** left/right to move between days from the summary.
- **Calendar** — a colour-coded month grid (opened from the header date chip) lets you
  jump to *any* date, logged or not, with a month/year wheel picker for fast travel:
  - **Blue** — tested & on keto
  - **Green** — 2+ keto meals logged
  - **Gold** — any data logged
  - A gold ring marks today; a white ring marks the day you're viewing.
- **Overview** — a full-screen list of every logged day in reverse-chronological order;
  tap any card to jump to it.

### Themes
14 themes, split into Dark and Light groups, chosen from a bottom-sheet picker:

| Dark | Light |
|------|-------|
| Midnight (default) | Pearl |
| Obsidian | Azure |
| Graphite | Blossom |
| Navy | Meadow |
| Twilight | Lavender |
| Aurora | Sunset |
| Forest | |
| Ember | |

An **auto-theme** mode follows the system dark/light setting, switching between a chosen
"night" theme and a chosen "day" theme automatically.

### Data & storage
- **Local-first** — all data lives on the device in a Room database; photos are stored as
  files in app-private storage. No server, no account, no sync.
- **Instant auto-save** — every change is persisted immediately.
- **Export / Import** — back up or move your data as a single `.json` file via the system
  file picker. Import is non-destructive: duplicate days can be merged (fill gaps only),
  overwritten, or skipped, while new days are always added.
- **Storage stats** — Settings shows the real database + photo footprint with a per-day
  and per-photo breakdown.
- **Offline by default** — the app makes no network requests.

### Feedback
- **Snackbar notifications** for saves, imports, errors, and other actions.

---

## Build & run

You need **Android Studio** (Ladybug or newer) or the Android command-line SDK. The app
module lives in [`android/`](android/).

### Android Studio (recommended)
1. `File → Open` and select the `android/` folder.
2. Let Gradle sync (the first sync downloads AGP / Compose / Room / KSP / DataStore /
   serialization and may take a while).
3. Pick an emulator or device and hit **Run ▶**.

### Command line
```bash
cd android
echo "sdk.dir=/path/to/Android/sdk" > local.properties
./gradlew assembleDebug     # build debug APK -> app/build/outputs/apk/debug/
./gradlew installDebug      # build + install on a connected device
./gradlew testDebugUnitTest # run the JVM unit tests (data layer)
```

### Design-time previews (no device needed)
Open `android/app/src/main/java/com/ketotracker/ui/screens/Previews.kt` in Split/Design
view — previews cover every wizard step, overlay, and a sample of themes, all backed by
in-memory demo data.

---

## Tech stack

- **Kotlin + Jetpack Compose** (Material 3) — single-activity, no Navigation library
- **Room** with a JSON-column schema (zero-migration: add a field, no SQL migration)
- **DataStore Preferences** — theme and auto-theme settings
- **kotlinx.serialization** — `DayEntry ⇄ JSON`
- **Coil** — async photo thumbnails / full-screen loading
- **WorkManager** — periodic local JSON backup; reminder notifications
- **System camera intent + FileProvider** — photo capture (no `CAMERA` permission)

See [`CLAUDE.md`](CLAUDE.md) for a full architecture and codebase guide, and
[`android/README.md`](android/README.md) for module-level detail.
</content>
