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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.ketotracker.data.DateUtils
import com.ketotracker.data.Meal
import com.ketotracker.data.PLACEHOLDERS
import com.ketotracker.data.Step
import com.ketotracker.data.photo.MealPhoto
import com.ketotracker.model.AppViewModel
import com.ketotracker.ui.components.BackButton
import com.ketotracker.ui.components.CalendarPanel
import com.ketotracker.ui.components.Dots
import com.ketotracker.ui.components.FlagsBody
import com.ketotracker.ui.components.HeaderBar
import com.ketotracker.ui.components.HeartBody
import com.ketotracker.ui.components.KetoButton
import com.ketotracker.ui.components.KetoCard
import com.ketotracker.ui.components.KetoTextArea
import com.ketotracker.ui.components.MealBody
import com.ketotracker.ui.components.MealPhotoArea
import com.ketotracker.ui.components.PhotoViewer
import com.ketotracker.ui.components.PrimaryButton
import com.ketotracker.ui.components.RatingsBody
import com.ketotracker.ui.components.SkipButton
import com.ketotracker.ui.components.StepHeading
import com.ketotracker.ui.components.SummaryCard
import com.ketotracker.ui.components.ThemePanel
import com.ketotracker.ui.theme.KetoTheme

private enum class Overlay { NONE, THEME, OVERVIEW, CALENDAR, SUPPLEMENTS, QUICK_SELECT, SETTINGS }

@Composable
fun WizardScreen(vm: AppViewModel) {
    val c = KetoTheme.colors
    var overlay by remember { mutableStateOf(Overlay.NONE) }
    var quickMeal by remember { mutableStateOf<Meal?>(null) }
    var viewingPhoto by remember { mutableStateOf<MealPhoto?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(vm) {
        vm.messages.collect { message -> snackbarHostState.showSnackbar(message) }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(c.bg)
            .systemBarsPadding()
    ) {
        Column(Modifier.fillMaxSize()) {

            // ── Main scrollable content ──────────────────────────────────
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .imePadding()
                    .pointerInput(vm.stepIndex, vm.viewedKey) {
                        var totalDrag = 0f
                        detectHorizontalDragGestures(
                            onDragStart  = { totalDrag = 0f },
                            onDragCancel = { totalDrag = 0f },
                            onDragEnd = {
                                if (totalDrag >  50) onSwipeRight(vm)
                                else if (totalDrag < -50) onSwipeLeft(vm)
                            },
                            onHorizontalDrag = { _, amount -> totalDrag += amount },
                        )
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
                    // key() forces a full recompose when the step changes so
                    // stale TextFields or button states never bleed between steps.
                    key(vm.stepIndex, vm.viewedKey) {
                        StepContent(
                            vm = vm,
                            onQuickSelect = { meal ->
                                quickMeal = meal
                                overlay = Overlay.QUICK_SELECT
                            },
                            onSupplements = { overlay = Overlay.SUPPLEMENTS },
                            onViewPhoto = { viewingPhoto = it },
                        )
                    }
                }
            }

            // ── Bottom header bar ────────────────────────────────────────
            HeaderBar(
                dateText = if (vm.isToday) "Today" else DateUtils.fmtDate(vm.viewedKey),
                nextEnabled = !vm.isToday,
                onPrev = { vm.changeDay(-1) },
                onNext = { vm.changeDay(1) },
                onDateClick = { overlay = Overlay.CALENDAR },
                onOverview = { overlay = Overlay.OVERVIEW },
                onTheme = { overlay = Overlay.THEME },
                onSettings = { overlay = Overlay.SETTINGS },
            )
        }

        // ── Overlays (drawn on top of everything) ────────────────────────
        when (overlay) {
            Overlay.THEME -> ThemePanel(
                currentId = vm.themeId,
                autoEnabled = vm.autoThemeEnabled,
                darkAutoId = vm.darkAutoThemeId,
                lightAutoId = vm.lightAutoThemeId,
                onPick = { vm.setTheme(it); overlay = Overlay.NONE },
                onPickAuto = { forDark, id -> vm.setAutoThemeChoice(forDark, id) },
                onToggleAuto = { vm.toggleAutoTheme() },
                onClose = { overlay = Overlay.NONE },
            )
            Overlay.OVERVIEW -> OverviewSheet(
                vm = vm,
                onJump = { vm.jumpTo(it); overlay = Overlay.NONE },
                onClose = { overlay = Overlay.NONE },
            )
            Overlay.CALENDAR -> CalendarPanel(
                viewedKey = vm.viewedKey,
                entries = vm.allEntries,
                onSelect = { vm.jumpTo(it); overlay = Overlay.NONE },
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
            Overlay.SETTINGS -> SettingsSheet(
                vm = vm,
                onTheme = { overlay = Overlay.THEME },
                onClose = { overlay = Overlay.NONE },
            )
            Overlay.NONE -> Unit
        }

        // ── Photo viewer (full-screen, drawn above overlays too) ─────────
        viewingPhoto?.let { photo ->
            PhotoViewer(photo = photo, onClose = { viewingPhoto = null })
        }

        // ── Error feedback (always on top, never blocks input) ───────────
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp),
        )
    }
}

// ── Step content ─────────────────────────────────────────────────────────────

@Composable
private fun StepContent(
    vm: AppViewModel,
    onQuickSelect: (Meal) -> Unit,
    onSupplements: () -> Unit,
    onViewPhoto: (MealPhoto) -> Unit,
) {
    val step = vm.step

    if (step == Step.SUMMARY) {
        SummaryCard(
            entry = vm.entry,
            viewedKey = vm.viewedKey,
            isToday = vm.isToday,
            canEdit = !vm.isFuture,
            onEdit = { vm.editAt(it) },
            mealPhotos = { vm.mealPhotos(it) },
            onViewPhoto = onViewPhoto,
        )
        return
    }

    KetoCard(compact = step.isMeal) {
        // Label + title — meal steps skip the label row (matching web app)
        StepHeading(step, showLabelAndSub = !step.isMeal)

        // Step body
        when {
            step.isMeal -> MealBody(
                meal = step.meal!!,
                entry = vm.entry,
                onText = { vm.setMealText(step.meal!!, it) },
                onQuickSelect = { onQuickSelect(step.meal!!) },
            )
            step == Step.RATINGS -> RatingsBody(
                entry = vm.entry,
                onPick = { field, value -> vm.pickRating(field, value) },
            )
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
            step == Step.NOTES -> KetoTextArea(
                value = vm.entry.notes,
                placeholder = PLACEHOLDERS["notes"] ?: "",
                minLines = 4,
                onValueChange = { vm.setNotes(it) },
            )
        }

        ActionRow(vm)

        // Photo area sits below the action row so the buttons stay visible
        // when the keyboard is open (matches CLAUDE.md "Photo area" rule).
        if (step.isMeal) {
            MealPhotoArea(
                meal = step.meal!!,
                photos = vm.mealPhotos(step.meal!!),
                onCaptured = { file -> vm.addPhoto(step.meal!!, file) },
                onView = onViewPhoto,
                onRemove = { vm.removePhoto(it) },
            )
        }
    }
}

@Composable
private fun ActionRow(vm: AppViewModel) {
    val step = vm.step
    val isLastBeforeSummary = vm.stepIndex == Step.entries.size - 2
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
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

// ── Swipe helpers ─────────────────────────────────────────────────────────────

private fun onSwipeLeft(vm: AppViewModel) {
    if (vm.step == Step.SUMMARY) vm.changeDay(1) else vm.next()
}

private fun onSwipeRight(vm: AppViewModel) {
    if (vm.step == Step.SUMMARY) vm.changeDay(-1) else vm.back()
}
