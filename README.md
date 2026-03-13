# Keto-Tracker

A highly specialized keto diet tracker for personal use, designed to be fast, no-nonsense, and aimed at minimizing input time while maximizing data. The result is a self-contained, single-file web app that requires no installation, no account, and no internet connection after the initial load.

## How to Use

Open `keto_tracker.html` in any modern browser. That's it — no build step, no dependencies.

## Features

### Daily Logging Wizard
A 9-step guided flow walks through each day's entry in order:
1. **Breakfast** — free-text meal description
2. **Lunch** — free-text meal description
3. **Dinner** — free-text meal description
4. **Energy** — 1–5 rating (Low → Great)
5. **Happiness** — 1–5 rating (Low → Great)
6. **Portions** — 1–5 rating (Tiny → Huge)
7. **Flags** — toggles for "Not in Keto" (red) and "Tested" ketone levels (green)
8. **Notes** — optional free-text field for anything else
9. **Summary** — full day review with per-field inline edit buttons

The wizard is **time-aware**: it automatically opens at the most relevant step based on the current hour (e.g. Breakfast before 10am, Lunch before 2pm, etc.). After rating steps are selected, the app advances automatically.

Text steps (meals, notes) can be skipped. All other steps can be navigated back and forth freely.

### Day Navigation
- **Prev/Next buttons** in the header to step through days
- **History chip strip** shows the last 20 logged days (plus today) for quick jumping
- Viewing a past day shows a read-only summary; only today's entries can be edited

### Overview Panel
A full-screen modal (📋) lists every logged day in reverse chronological order. Each card shows meals, ratings, and flags at a glance. Tap any card to jump to that day.

### Themes
14 themes selectable from a floating panel (🎨), split into Dark and Light groups. Hovering a swatch previews the theme live before committing.

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

Theme preference is saved to localStorage and restored on next open.

### Data & Storage
- **Auto-save** — every change is written to `localStorage` instantly, no save button needed
- **Storage usage** — a visual bar in Settings shows how much of the browser's ~5 MB localStorage quota is in use
- **Snapshots** — save up to 25 named snapshots of all data; each can be restored, exported, or deleted individually
- **Export / Import** — full data can be exported as a `.json` file and re-imported on any device

### Misc
- **Toast notifications** — brief feedback for saves, imports, errors, etc.
- **No dependencies** — zero external libraries, frameworks, or network requests
- **Mobile-first** — full-height layout (`100dvh`), hidden scrollbars, touch-optimized tap targets, pinch-zoom disabled
- **Responsive** — wider layout kicks in at 560px for desktop use
