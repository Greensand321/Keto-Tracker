# Keto Tracker — CLAUDE.md

A comprehensive guide to the codebase for AI assistants and developers working on this project.

---

## Project Overview

Keto Tracker is a **zero-dependency, single-file Progressive Web App (PWA)** for personal keto diet logging. It runs entirely in the browser with no build step, no server, and no external libraries. All data is stored locally on the device.

**Goal**: Minimise friction when logging daily meals, energy levels, mood, and ketone test results.

**Tech stack**: Vanilla HTML/CSS/JS, localStorage, IndexedDB, Service Worker.

---

## File Structure

```
Keto-Tracker/
├── index.html        # The entire app — HTML, CSS, and JS in one file (~1,031 lines)
├── sw.js             # Service Worker for offline caching (43 lines)
├── manifest.json     # PWA manifest (icons, display settings, theme colour)
├── icons/            # PWA icons in 11 sizes (72px → 512px)
│   ├── icon-72.png
│   ├── icon-96.png
│   ├── icon-128.png
│   ├── icon-144.png
│   ├── icon-152.png
│   ├── icon-167.png   # iPad Pro home screen
│   ├── icon-180.png   # iPhone home screen
│   ├── icon-192.png   # Android standard (also cached by SW)
│   ├── icon-384.png
│   ├── icon-512.png   # Splash screens (SW cached)
│   └── icon-512-maskable.png  # Adaptive Android icon (SW cached)
├── README.md         # User-facing feature overview
└── CLAUDE.md         # This file
```

> **Important**: There is no build system, no `package.json`, no node_modules. Open `index.html` directly in a browser — that's the entire app.

---

## Architecture

### Single-File Design

Everything lives in `index.html`:
1. `<head>` — PWA meta tags, iOS/Android icon links
2. `<style>` — All CSS (CSS variables for theming, mobile-first layout)
3. `<body>` — Minimal HTML skeleton; all UI is rendered dynamically by JS
4. `<script>` — All application logic inline (no `<script src>`, no modules, no bundling)

### No External Dependencies

- Zero npm packages
- Zero CDN links
- Zero network requests after initial load
- The only "external" file loaded is `manifest.json` and the icon PNGs

---

## Data Model

### localStorage (primary data store)

All keys are prefixed to avoid collisions.

| Key | Type | Description |
|-----|------|-------------|
| `kt_d_YYYY-MM-DD` | JSON string | Daily log entry (one per day) |
| `kt__snapshots` | JSON string | Array of up to 25 snapshots |
| `kt_theme` | string | Active theme ID (e.g. `"midnight"`) |
| `kt_theme_auto` | string | `"on"` or `"off"` for auto-theme mode |
| `kt_theme_dark_auto` | string | Preferred dark theme when auto mode is on |
| `kt_theme_light_auto` | string | Preferred light theme when auto mode is on |

#### Daily Entry Schema (`kt_d_YYYY-MM-DD`)

```json
{
  "date": "2025-01-15",
  "breakfast": "2 eggs, bacon, avocado",
  "lunch": "Grilled chicken salad",
  "dinner": "Steak with broccoli",
  "energy": 4,
  "happiness": 5,
  "portion": 3,
  "notInKeto": false,
  "tested": true,
  "notes": "Had cravings at 3pm",
  "breakfastKeto": true,
  "lunchKeto": false,
  "dinnerKeto": true
}
```

- Meal fields (`breakfast`, `lunch`, `dinner`, `notes`) are strings (empty string = not filled)
- Rating fields (`energy`, `happiness`, `portion`) are integers 1–5, or `null` if not set
- Flag fields (`notInKeto`, `tested`) are booleans
- Keto meal flags (`breakfastKeto`, `lunchKeto`, `dinnerKeto`) are booleans (default `false`) — set to `true` when the user marks a meal as keto via the Keto button

#### Snapshot Schema (`kt__snapshots`)

```json
[
  {
    "id": 1705286400000,
    "name": "Pre-vacation backup",
    "ts": 1705286400000,
    "days": 45,
    "data": {
      "2024-11-01": { "breakfast": "...", "energy": 4, ... },
      "2024-11-02": { ... }
    }
  }
]
```

### IndexedDB (photo store)

Meal photos are stored separately from localStorage to avoid hitting the ~5 MB quota.

- **Database**: `keto-photos`
- **Object store**: `photos`
- **Key format**: `YYYY-MM-DD_breakfast`, `YYYY-MM-DD_lunch`, `YYYY-MM-DD_dinner`
- **Value**: Compressed JPEG `Blob` (max 900px, 0.75 quality — compressed on device via canvas)

---

## Application State

The app uses plain global variables (no framework, no reactive state):

| Variable | Description |
|----------|-------------|
| `vk` | Currently viewed date key (`YYYY-MM-DD`) |
| `si` | Current wizard step index (0–8) |
| `e` | Current day's entry object (the in-memory copy being edited) |

State changes always follow this pattern:
1. Mutate `e` (or `si`, `vk`)
2. Call `save(vk, e)` to persist
3. Call `render()` to re-render the UI

---

## Wizard Steps

The core UI is a 9-step daily logging wizard. Step index `si` controls which step is shown.

| `si` | Field | Type | Label | Description |
|------|-------|------|-------|-------------|
| 0 | `breakfast` | text | Meal 1 of 3 | Free-text breakfast description |
| 1 | `lunch` | text | Meal 2 of 3 | Free-text lunch description |
| 2 | `dinner` | text | Meal 3 of 3 | Free-text dinner description |
| 3 | `energy` | rating (1–5) | Daily Check-in | Energy level today |
| 4 | `happiness` | rating (1–5) | Daily Check-in | Happiness level today |
| 5 | `portion` | rating (1–5) | Daily Check-in | Portion size today |
| 6 | flags | toggles | Daily Flags | "Not in Keto" and "Tested" ketone levels |
| 7 | `notes` | text | Optional | Free-text notes |
| 8 | summary | display | Done! | Read-only recap of the full day |

### Wizard Behaviour Rules

- **Text steps (0, 1, 2, 7)**: Can be skipped; no auto-advance
- **Meal steps (0, 1, 2)**: Show three action buttons — Next, Keto, Skip. Keto marks the meal's keto flag `true` then advances exactly like Next
- **Rating steps (3, 4, 5)**: Auto-advance to next step 380ms after selection
- **Viewing a past day**: Always jumps directly to step 8 (summary, read-only)
- **Summary for today**: Has inline "Edit" buttons that call `editAt(idx)` to jump back
- **Photo area**: Rendered below the action buttons on meal steps so buttons remain visible when the keyboard is open

### Smart Step Logic

`smartStep()` uses the current hour to suggest the most relevant step when opening the app:
- Before 10:00 → `breakfast` (step 0)
- Before 14:00 → `lunch` (step 1)
- Before 20:00 → `dinner` (step 2)
- 20:00+ → `energy` (step 3)

`defStep(entry)` then finds the first *incomplete* step at or after the smart step.

---

## Key Functions

### Initialisation & Rendering

| Function | Description |
|----------|-------------|
| `init()` | App entry point. Loads today's data, determines starting step, renders UI, registers service worker |
| `render()` | Updates header date display and re-renders the current wizard step |
| `renderStep()` | Generates HTML for the current step based on `si` and `e`, inserts into DOM |
| `renderSum(isToday, canEdit)` | Renders the summary card with all day fields and optional edit buttons |

### Data Access

| Function | Description |
|----------|-------------|
| `load(dateKey)` | Returns entry for a date from localStorage, or a blank default entry |
| `save(dateKey, entry)` | Stringifies and writes entry to localStorage |
| `lsGetDay(k)` | Raw localStorage get for a day key |
| `lsSetDay(k, v)` | Raw localStorage set for a day key |
| `lsAllDayKeys()` | Returns all stored date keys (sorted) |
| `allKeys()` | Alias; all logged date keys |
| `getStorageStats()` | Returns `{ usedKB, pct, days, snaps }` |

### Wizard Interaction

| Function | Description |
|----------|-------------|
| `upd(field, value)` | Update a field on the current entry and save |
| `selRate(field, value)` | Select a 1–5 rating, save, auto-advance after 380ms |
| `togFlag(field)` | Toggle a boolean flag (`notInKeto` or `tested`) |
| `next()` | Advance to `si + 1` |
| `back()` | Go to `si - 1` |
| `skip()` | Skip current step (calls `next()`) |
| `editAt(idx)` | Jump to a specific step index for editing |
| `markKeto(meal)` | Sets `ent[meal+'Keto'] = true`, saves, then calls `next()` |

### Day Navigation

| Function | Description |
|----------|-------------|
| `goToday()` | Switch view to today, reload entry, determine smart step |
| `chgDay(n)` | Move n days from current `vk` (negative = past, positive = future) |
| `jumpTo(dateKey)` | Jump directly to a specific date |
| `todayKey()` | Returns today as `YYYY-MM-DD` |
| `offKey(dateKey, n)` | Returns date key offset by n days |
| `fmtDate(dateKey)` | Formats `YYYY-MM-DD` → `"Mon, Jan 15"` |

### Snapshots

| Function | Description |
|----------|-------------|
| `saveSnapshot(name)` | Captures all current data into a named snapshot (max 25) |
| `restoreSnapshot(id)` | Replaces all current data with snapshot (requires confirm) |
| `deleteSnapshot(id)` | Removes snapshot by ID (requires confirm) |
| `exportSnapshot(id)` | Downloads snapshot data as `.json` file |
| `lsGetSnaps()` | Returns parsed snapshots array from localStorage |
| `lsSetSnaps(arr)` | Writes snapshots array to localStorage |

### Export / Import

| Function | Description |
|----------|-------------|
| `exportAll()` | Downloads all day entries as `keto-all-data-YYYY-MM-DD.json` |
| `handleImport(event)` | Reads uploaded `.json` file, validates, clears existing, imports entries |
| `dlJSON(obj, filename)` | Utility: creates Blob, triggers browser download |

### Theme System

| Function | Description |
|----------|-------------|
| `applyTheme(id, persist=true)` | Sets `data-theme` attribute on `<html>`, updates theme-color meta, optionally saves to localStorage |
| `applyAutoTheme()` | Applies the appropriate dark/light theme based on system `prefers-color-scheme` |
| `toggleAutoTheme()` | Toggles auto-theme mode on/off |
| `renderThemeGrid()` | Builds the theme picker grid HTML |
| `toggleThemePanel()` | Shows/hides the floating theme picker panel |
| `closeThemePanel()` | Closes panel and syncs theme state |

### Photos

| Function | Description |
|----------|-------------|
| `openPhDB()` | Opens the `keto-photos` IndexedDB (creates if new) |
| `savePhotoBlob(date, meal, blob)` | Stores compressed JPEG blob |
| `getPhotoBlob(date, meal)` | Retrieves blob (returns `null` if not found) |
| `deletePhotoBlob(date, meal)` | Removes photo from IndexedDB |
| `compressImage(file, maxPx, quality)` | Draws image on canvas, returns compressed JPEG Blob |
| `openCamera(meal)` | Triggers file input with `capture="environment"` |
| `handleCamera(event)` | Compresses selected image and saves it |
| `loadMealPhoto(meal)` | Renders photo UI area for a meal step (add/view/delete) |
| `openPhotoModal(meal)` | Shows fullscreen photo viewer |

### Notifications

| Function | Description |
|----------|-------------|
| `toast(message, isError=false)` | Shows a brief bottom notification (green default, red if error) |

---

## UI Sections

### Header (`.hdr`)
- `‹` / `›` — Previous/Next day buttons (next disabled if on today)
- Date button — shows current date, opens calendar picker
- `📋` — Opens overview modal (all logged days)
- `🎨` — Opens theme picker panel
- `⚙️` — Opens settings modal

### Wizard (`.main`)
- **Dots** (`.dots`) — Progress indicator; filled = complete, ring = current, empty = future
- **Card** (`.card`) — Active step content (changes each step)
- **Action row** (`.actions`) — Back / Next / Skip buttons
- **History chip strip** — A horizontal scrollable row of the last 20 logged days (plus today) shown below the wizard for quick date jumping; chips are colour-coded (red = off-keto day)

### Overview Modal (`#ovModal`)
- Full-screen list of all logged days in reverse-chronological order
- Each card shows: date, meals, ratings, flags
- **Long-press** a card to enter multi-select mode
- Multi-select mode shows checkboxes and a bulk-delete button

### Calendar Panel (`.cal-panel`)
- Month grid with day cells
- Day colour is determined by a 3-tier priority system (highest wins):
  - **Blue** (`.keto-tested`) — `tested === true` AND `notInKeto === false`
  - **Green** (`.keto-meals`) — 2 or more of `breakfastKeto`, `lunchKeto`, `dinnerKeto` are `true`
  - **Yellow** (`.has-data`) — any log entry exists but neither above condition is met
  - No colour — no log entry for that day
- Gold outline = today; white outline = currently viewed day
- Previous/next month navigation buttons only (Go to Today button has been removed)

### Settings Modal (`#setModal`)
- App version display (reads from `APP_VERSION` constant)
- Storage usage bar (KB used / ~5 MB quota)
- Snapshot creation, list, restore, export, delete
- Export all / Import JSON buttons

### Theme Panel (`#theme-panel`)
- Dark themes: Midnight, Obsidian, Graphite, Navy, Twilight, Aurora, Forest, Ember
- Light themes: Pearl, Azure, Blossom, Meadow, Lavender, Sunset
- Auto-theme toggle (respects `prefers-color-scheme`)
- Hover over a swatch to preview before applying

---

## Theme System (CSS Variables)

Themes work entirely through CSS custom properties on the `<html>` element.

```
data-theme=""           → Midnight (default, dark)
data-theme="obsidian"   → Pure black
data-theme="graphite"   → Dark grey
data-theme="navy"       → Deep navy
data-theme="twilight"   → Purple dusk
data-theme="aurora"     → Teal-green dark
data-theme="forest"     → Dark green
data-theme="ember"      → Deep red-brown
data-theme="pearl"      → Off-white (light)
data-theme="azure"      → Light blue
data-theme="blossom"    → Light pink
data-theme="meadow"     → Light green
data-theme="lavender"   → Light purple
data-theme="sunset"     → Light orange
```

#### Core CSS Variables

| Variable | Usage |
|----------|-------|
| `--bg` | Page background |
| `--surf` | Card/surface background |
| `--surf2` | Elevated surface (hover states) |
| `--inp` | Input field background |
| `--bd` | Default border colour |
| `--bd-i` | Interactive border colour |
| `--txt` | Primary text |
| `--txt-m` | Muted/secondary text |
| `--txt-d` | Disabled/placeholder text |
| `--accent` | Green (keto/positive colour) |
| `--gold` | Gold (branding, highlights) |
| `--red` | Red (off-keto, errors) |
| `--blue` | Blue (info, links) |

Light themes override all variables including text colours (dark text on light background). Dark themes only override background and surface opacity values.

---

## PWA & Offline Support

### Service Worker (`sw.js`)

Strategy: **Cache-first with background update**

1. On install: caches `index.html`, `manifest.json`, and the three critical icons
2. On activate: deletes any old cache versions
3. On fetch: serves from cache immediately; fetches from network in background and updates cache

Cache name: `keto-v3` (increment to force cache bust on next deploy — this is the only way to guarantee all devices pick up new app files)

### iOS-Specific PWA

The app supports "Add to Home Screen" on iOS via:
- `apple-mobile-web-app-capable` meta tag
- `apple-mobile-web-app-status-bar-style: black-translucent`
- Four apple-touch-icon sizes: 120, 152, 167, 180px

---

## Mobile-First Layout

- `height: 100dvh` — uses dynamic viewport height (avoids iOS Safari URL bar issues)
- `user-scalable=no` — pinch zoom disabled for consistent app-like feel
- Touch targets minimum ~44px
- Swipe gestures on the main content area:
  - Swipe **left** → next step (or next day on summary)
  - Swipe **right** → previous step (or previous day on summary)
- Hidden scrollbars globally (`scrollbar-width: none`)
- Responsive breakpoint at **560px** — wider layout for desktop use

---

## Import / Export Format

### Export All (`exportAll()`)

Downloads a flat JSON file with date keys:

```json
{
  "2024-11-01": { "breakfast": "...", "energy": 4, ... },
  "2024-11-02": { ... }
}
```

Filename: `keto-all-data-YYYY-MM-DD.json`

### Import (`handleImport()`)

Accepts JSON files where keys are dates in any of these formats:
- `YYYY-MM-DD` (bare)
- `kt_d_YYYY-MM-DD` (full storage key)
- `kt_YYYY-MM-DD` (legacy prefix)

The importer strips any prefix, validates the date format, and merges after confirmation. **Import is destructive** — it clears all existing data before restoring.

---

## Common Patterns & Conventions

### Adding a New Step

1. Add a new entry to the `STEPS` array in `renderStep()` with `{field, icon, label, type}`
2. Handle the new `type` in the `renderStep()` switch/if block
3. Ensure `defStep()` logic accounts for the new field when checking completeness
4. Update `renderSum()` to display the new field in the summary

### Adding a New Theme

1. Add a CSS block in `<style>`: `[data-theme="yourtheme"]{ --bg: ...; ... }`
2. Add an entry to the `THEMES` array in JS: `{id, emoji, label, dark: true/false}`
3. `renderThemeGrid()` will pick it up automatically

### Modifying the Data Schema

1. Update the `load()` function's default blank entry to include the new field with a default value
2. This ensures existing entries without the field get a sensible default on read
3. Update `renderStep()`, `renderSum()`, and `handleImport()` as needed

### Versioning

The app uses a simple numeric version tracked in two places that **must both be updated** when making meaningful changes:

1. **`APP_VERSION` constant** in `index.html` — displayed in the Settings modal so the user can confirm which version is running
2. **`CACHE` name** in `sw.js` — controls the service worker cache; incrementing it (e.g. `keto-v3` → `keto-v4`) forces all devices to discard the old cache and download fresh files

**When to increment:**
- Any user-facing feature addition or change → bump `APP_VERSION` (e.g. `1.2` → `1.3`)
- Any deploy where you need devices to reliably update → also bump the cache name in `sw.js`
- Bug fixes or minor tweaks → use judgement; bump if the fix is important enough to force an update

**Format:** `APP_VERSION` uses `major.minor` (e.g. `1.0`, `1.1`, `1.2`). The cache name uses `keto-vN` where N is a simple integer.

> **IMPORTANT for AI assistants:** Always check the current `APP_VERSION` value in `index.html` and the cache name in `sw.js` before finishing a session. If meaningful changes were made, increment both before committing.

---

## Known Constraints

- **No server**: All data is local. No sync across devices (use Export/Import manually)
- **localStorage limit**: ~5 MB per origin; storage bar in Settings shows usage
- **IndexedDB for photos**: Photos do not count toward the 5 MB localStorage quota but are device-local and not included in JSON exports
- **Single file**: All CSS and JS must stay in `index.html` — there is no bundler
- **No modules**: JS is plain global scope — be careful with variable naming
- **No tests**: There are no unit or integration tests; test manually in browser
- **Browser support**: Targets modern browsers (ES6+, CSS Grid, Service Workers, IndexedDB)
