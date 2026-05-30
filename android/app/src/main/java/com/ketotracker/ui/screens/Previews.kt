package com.ketotracker.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.ketotracker.data.Heart
import com.ketotracker.data.Step
import com.ketotracker.model.AppViewModel
import com.ketotracker.model.RatingField
import com.ketotracker.ui.theme.KetoTracker

// ── Helpers ───────────────────────────────────────────────────────────────────

/** Build a preview VM pinned to a specific wizard step. */
private fun vmAt(step: Step, themeId: String = "midnight"): AppViewModel =
    AppViewModel.preview().apply {
        setTheme(themeId)
        editAt(step.ordinal)
    }

// ── All 8 Wizard Steps (Midnight dark theme) ──────────────────────────────────

@Preview(name = "Step 1 — Breakfast", showBackground = true, heightDp = 780, widthDp = 390)
@Composable
private fun PreviewBreakfast() {
    KetoTracker("midnight") { WizardScreen(vmAt(Step.BREAKFAST)) }
}

@Preview(name = "Step 2 — Lunch", showBackground = true, heightDp = 780, widthDp = 390)
@Composable
private fun PreviewLunch() {
    KetoTracker("midnight") { WizardScreen(vmAt(Step.LUNCH)) }
}

@Preview(name = "Step 3 — Dinner", showBackground = true, heightDp = 780, widthDp = 390)
@Composable
private fun PreviewDinner() {
    KetoTracker("midnight") { WizardScreen(vmAt(Step.DINNER)) }
}

@Preview(name = "Step 4 — Ratings", showBackground = true, heightDp = 780, widthDp = 390)
@Composable
private fun PreviewRatings() {
    KetoTracker("midnight") { WizardScreen(vmAt(Step.RATINGS)) }
}

@Preview(name = "Step 5 — Heart Health", showBackground = true, heightDp = 780, widthDp = 390)
@Composable
private fun PreviewHeart() {
    KetoTracker("midnight") { WizardScreen(vmAt(Step.HEART)) }
}

@Preview(name = "Step 6 — Flags", showBackground = true, heightDp = 780, widthDp = 390)
@Composable
private fun PreviewFlags() {
    KetoTracker("midnight") { WizardScreen(vmAt(Step.FLAGS)) }
}

@Preview(name = "Step 7 — Notes", showBackground = true, heightDp = 780, widthDp = 390)
@Composable
private fun PreviewNotes() {
    KetoTracker("midnight") { WizardScreen(vmAt(Step.NOTES)) }
}

@Preview(name = "Step 8 — Summary (today)", showBackground = true, heightDp = 780, widthDp = 390)
@Composable
private fun PreviewSummary() {
    KetoTracker("midnight") { WizardScreen(vmAt(Step.SUMMARY)) }
}

// ── Summary with data filled in ───────────────────────────────────────────────

@Preview(name = "Step 8 — Summary (with data)", showBackground = true, heightDp = 900, widthDp = 390)
@Composable
private fun PreviewSummaryFilled() {
    val vm = AppViewModel.preview().apply {
        setTheme("midnight")
        setMealText(com.ketotracker.data.Meal.BREAKFAST, "3 eggs, bacon, avocado")
        setMealText(com.ketotracker.data.Meal.LUNCH, "Chicken Caesar salad")
        setMealText(com.ketotracker.data.Meal.DINNER, "Ribeye + buttered asparagus")
        pickRating(RatingField.ENERGY, 4)
        pickRating(RatingField.HAPPINESS, 5)
        pickRating(RatingField.PORTION, 3)
        selectHeart(Heart.GOOD)
        toggleTested()
        setSupplement("Magnesium", 1)
        setSupplement("Omega-3", 2)
        editAt(Step.SUMMARY.ordinal)
    }
    KetoTracker("midnight") { WizardScreen(vm) }
}

// ── Overlays ──────────────────────────────────────────────────────────────────

@Preview(name = "Overlay — Theme Picker", showBackground = true, heightDp = 780, widthDp = 390)
@Composable
private fun PreviewThemePicker() {
    KetoTracker("midnight") {
        com.ketotracker.ui.components.ThemePanel(
            currentId = "midnight",
            onPick = {},
            onClose = {},
        )
    }
}

@Preview(name = "Overlay — Overview", showBackground = true, heightDp = 780, widthDp = 390)
@Composable
private fun PreviewOverview() {
    KetoTracker("midnight") {
        OverviewSheet(vm = AppViewModel.preview(), onJump = {}, onClose = {})
    }
}

@Preview(name = "Overlay — Supplements", showBackground = true, heightDp = 780, widthDp = 390)
@Composable
private fun PreviewSupplements() {
    KetoTracker("midnight") {
        SupplementsSheet(vm = AppViewModel.preview(), onClose = {})
    }
}

@Preview(name = "Overlay — Quick Select", showBackground = true, heightDp = 780, widthDp = 390)
@Composable
private fun PreviewQuickSelect() {
    KetoTracker("midnight") {
        QuickSelectSheet(
            vm = AppViewModel.preview(),
            meal = com.ketotracker.data.Meal.BREAKFAST,
            onClose = {},
        )
    }
}

@Preview(name = "Overlay — Settings", showBackground = true, heightDp = 780, widthDp = 390)
@Composable
private fun PreviewSettings() {
    KetoTracker("midnight") {
        SettingsSheet(vm = AppViewModel.preview(), onTheme = {}, onClose = {})
    }
}

// ── Light themes ──────────────────────────────────────────────────────────────

@Preview(name = "Theme — Pearl (light)", showBackground = true, heightDp = 780, widthDp = 390)
@Composable
private fun PreviewPearl() {
    KetoTracker("pearl") { WizardScreen(vmAt(Step.RATINGS, "pearl")) }
}

@Preview(name = "Theme — Blossom (light)", showBackground = true, heightDp = 780, widthDp = 390)
@Composable
private fun PreviewBlossom() {
    KetoTracker("blossom") { WizardScreen(vmAt(Step.FLAGS, "blossom")) }
}

// ── Dark themes ───────────────────────────────────────────────────────────────

@Preview(name = "Theme — Forest (dark)", showBackground = true, heightDp = 780, widthDp = 390)
@Composable
private fun PreviewForest() {
    KetoTracker("forest") { WizardScreen(vmAt(Step.HEART, "forest")) }
}

@Preview(name = "Theme — Ember (dark)", showBackground = true, heightDp = 780, widthDp = 390)
@Composable
private fun PreviewEmber() {
    KetoTracker("ember") { WizardScreen(vmAt(Step.NOTES, "ember")) }
}
