package com.ketotracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.ketotracker.data.DateUtils
import com.ketotracker.data.Meal
import com.ketotracker.data.Step
import com.ketotracker.model.AppViewModel
import com.ketotracker.ui.components.BackButton
import com.ketotracker.ui.components.Dots
import com.ketotracker.ui.components.FlagsBody
import com.ketotracker.ui.components.HeaderBar
import com.ketotracker.ui.components.HeartBody
import com.ketotracker.ui.components.KetoButton
import com.ketotracker.ui.components.KetoCard
import com.ketotracker.ui.components.MealBody
import com.ketotracker.ui.components.PrimaryButton
import com.ketotracker.ui.components.RatingsBody
import com.ketotracker.ui.components.SkipButton
import com.ketotracker.ui.components.StepHeading
import com.ketotracker.ui.components.SummaryCard
import com.ketotracker.ui.components.ThemePanel
import com.ketotracker.ui.theme.KetoTheme

private enum class Overlay { NONE, THEME, OVERVIEW, SUPPLEMENTS, QUICK_SELECT }

@Composable
fun WizardScreen(vm: AppViewModel) {
    val c = KetoTheme.colors
    var overlay by remember { mutableStateOf(Overlay.NONE) }
    var quickMeal by remember { mutableStateOf<Meal?>(null) }

    Box(
        Modifier
            .fillMaxSize()
            .background(c.bg)
            .systemBarsPadding()
    ) {
        Column(Modifier.fillMaxSize()) {
            // Main content (top, scrollable, grows to fill).
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .imePadding()
                    .pointerInput(vm.stepIndex, vm.viewedKey) {
                        detectHorizontalDragGestures { _, drag ->
                            if (drag > 18) onSwipeRight(vm)
                            else if (drag < -18) onSwipeLeft(vm)
                        }
                    }
            ) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(22.dp),
                ) {
                    if (vm.step != Step.SUMMARY) {
                        Dots(currentIndex = vm.stepIndex)
                    }
                    StepContent(
                        vm = vm,
                        onQuickSelect = { meal -> quickMeal = meal; overlay = Overlay.QUICK_SELECT },
                        onSupplements = { overlay = Overlay.SUPPLEMENTS },
                    )
                }
            }

            // Header bar (bottom).
            HeaderBar(
                dateText = if (vm.isToday) "Today" else DateUtils.fmtDate(vm.viewedKey),
                nextEnabled = !vm.isToday,
                onPrev = { vm.changeDay(-1) },
                onNext = { vm.changeDay(1) },
                onDateClick = { overlay = Overlay.OVERVIEW },
                onOverview = { overlay = Overlay.OVERVIEW },
                onTheme = { overlay = Overlay.THEME },
                onSettings = { overlay = Overlay.OVERVIEW },
            )
        }

        // ── Overlays ────────────────────────────────────────────────────────
        when (overlay) {
            Overlay.THEME -> ThemePanel(
                currentId = vm.themeId,
                onPick = { vm.setTheme(it) },
                onClose = { overlay = Overlay.NONE },
            )
            Overlay.OVERVIEW -> OverviewSheet(
                vm = vm,
                onJump = { vm.jumpTo(it); overlay = Overlay.NONE },
                onClose = { overlay = Overlay.NONE },
            )
            Overlay.SUPPLEMENTS -> SupplementsSheet(
                vm = vm,
                onClose = { overlay = Overlay.NONE },
            )
            Overlay.QUICK_SELECT -> QuickSelectSheet(
                vm = vm,
                meal = quickMeal ?: Meal.BREAKFAST,
                onClose = { overlay = Overlay.NONE },
            )
            Overlay.NONE -> Unit
        }
    }
}

@Composable
private fun StepContent(
    vm: AppViewModel,
    onQuickSelect: (Meal) -> Unit,
    onSupplements: () -> Unit,
) {
    val step = vm.step
    if (step == Step.SUMMARY) {
        SummaryCard(
            entry = vm.entry,
            viewedKey = vm.viewedKey,
            isToday = vm.isToday,
            canEdit = !vm.isFuture,
            onEdit = { vm.editAt(it) },
        )
        return
    }

    KetoCard {
        StepHeading(step, showLabelAndSub = !step.isMeal)

        when {
            step.isMeal -> MealBody(
                meal = step.meal!!,
                entry = vm.entry,
                onText = { vm.setMealText(step.meal!!, it) },
                onQuickSelect = { onQuickSelect(step.meal!!) },
            )
            step == Step.RATINGS -> RatingsBody(vm.entry) { field, value -> vm.pickRating(field, value) }
            step == Step.HEART -> HeartBody(
                entry = vm.entry,
                onSelect = { vm.selectHeart(it) },
                onNotes = { vm.setHeartNotes(it) },
            )
            step == Step.FLAGS -> FlagsBody(
                entry = vm.entry,
                onToggleNotInKeto = { vm.toggleNotInKeto() },
                onToggleTested = { vm.toggleTested() },
                onOpenSupplements = onSupplements,
            )
            step == Step.NOTES -> com.ketotracker.ui.components.KetoTextArea(
                value = vm.entry.notes,
                placeholder = com.ketotracker.data.PLACEHOLDERS["notes"] ?: "",
                minLines = 4,
                onValueChange = { vm.setNotes(it) },
            )
        }

        ActionRow(vm)
    }
}

@Composable
private fun ActionRow(vm: AppViewModel) {
    val step = vm.step
    val isLastBeforeSummary = vm.stepIndex == Step.entries.size - 2
    Row(
        Modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (vm.stepIndex > 0) BackButton { vm.back() }
        PrimaryButton(
            text = if (isLastBeforeSummary) "Finish ✓" else "Next →",
            modifier = Modifier.weight(1f),
        ) { vm.next() }
        if (step.isMeal) KetoButton(Modifier.weight(1f)) { vm.markKeto(step.meal!!) }
        if (step.isText) SkipButton(Modifier.weight(1f)) { vm.skip() }
    }
}

// Swipe: left → next step (or next day on summary); right → previous.
private fun onSwipeLeft(vm: AppViewModel) {
    if (vm.step == Step.SUMMARY) vm.changeDay(1) else vm.next()
}

private fun onSwipeRight(vm: AppViewModel) {
    if (vm.step == Step.SUMMARY) vm.changeDay(-1) else vm.back()
}
