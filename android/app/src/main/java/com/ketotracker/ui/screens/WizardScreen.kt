package com.ketotracker.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.draw.alpha
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

// A quick fade in/out keeps every overlay — bottom-anchored panels and
// full-screen sheets alike — feeling consistent without needing to know
// each one's internal layout (scrim, card alignment, etc.).
private val OVERLAY_ENTER = fadeIn(tween(180))
private val OVERLAY_EXIT = fadeOut(tween(140))

@Composable
fun WizardScreen(vm: AppViewModel) {
    val c = KetoTheme.colors
    var overlay by remember { mutableStateOf(Overlay.NONE) }
    var quickMeal by remember { mutableStateOf<Meal?>(null) }
    var viewingPhoto by remember { mutableStateOf<MealPhoto?>(null) }

    // Retained through the close animation so the photo viewer's exit fade
    // has something to render — by the time it plays, viewingPhoto is
    // already null (AnimatedVisibility keeps re-invoking this content while
    // animating out).
    var lastViewedPhoto by remember { mutableStateOf<MealPhoto?>(null) }
    LaunchedEffect(viewingPhoto) { viewingPhoto?.let { lastViewedPhoto = it } }

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
                    // Slides + fades the new step/day in from the direction of
                    // travel; key() inside still forces a full recompose when
                    // the step changes so stale TextFields or button states
                    // never bleed between steps.
                    StepTransition(stepIndex = vm.stepIndex, dayKey = vm.viewedKey) {
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
        //
        // Each overlay gets its own always-present AnimatedVisibility keyed
        // on a live `overlay == Overlay.X` comparison, rather than living
        // inside the `when` below — a `when` branch stops being composed the
        // instant `overlay` changes, so it'd skip the exit fade entirely and
        // just cut away like before.
        AnimatedVisibility(overlay == Overlay.THEME, enter = OVERLAY_ENTER, exit = OVERLAY_EXIT) {
            ThemePanel(
                currentId = vm.themeId,
                autoEnabled = vm.autoThemeEnabled,
                darkAutoId = vm.darkAutoThemeId,
                lightAutoId = vm.lightAutoThemeId,
                onPick = { vm.setTheme(it); overlay = Overlay.NONE },
                onPickAuto = { forDark, id -> vm.setAutoThemeChoice(forDark, id) },
                onToggleAuto = { vm.toggleAutoTheme() },
                onClose = { overlay = Overlay.NONE },
            )
        }
        AnimatedVisibility(overlay == Overlay.OVERVIEW, enter = OVERLAY_ENTER, exit = OVERLAY_EXIT) {
            OverviewSheet(
                vm = vm,
                onJump = { vm.jumpTo(it); overlay = Overlay.NONE },
                onClose = { overlay = Overlay.NONE },
            )
        }
        AnimatedVisibility(overlay == Overlay.CALENDAR, enter = OVERLAY_ENTER, exit = OVERLAY_EXIT) {
            CalendarPanel(
                viewedKey = vm.viewedKey,
                entries = vm.allEntries,
                onSelect = { vm.jumpTo(it); overlay = Overlay.NONE },
                onClose = { overlay = Overlay.NONE },
            )
        }
        AnimatedVisibility(overlay == Overlay.SUPPLEMENTS, enter = OVERLAY_ENTER, exit = OVERLAY_EXIT) {
            SupplementsSheet(vm = vm, onClose = { overlay = Overlay.NONE })
        }
        AnimatedVisibility(overlay == Overlay.QUICK_SELECT, enter = OVERLAY_ENTER, exit = OVERLAY_EXIT) {
            QuickSelectSheet(
                vm = vm,
                meal = quickMeal ?: Meal.BREAKFAST,
                onClose = { overlay = Overlay.NONE },
            )
        }
        AnimatedVisibility(overlay == Overlay.SETTINGS, enter = OVERLAY_ENTER, exit = OVERLAY_EXIT) {
            SettingsSheet(
                vm = vm,
                onTheme = { overlay = Overlay.THEME },
                onClose = { overlay = Overlay.NONE },
            )
        }

        // ── Photo viewer (full-screen, drawn above overlays too) ─────────
        //
        // Renders lastViewedPhoto (not viewingPhoto) so the exit fade has a
        // photo to animate — viewingPhoto is already null by the time the
        // close animation plays.
        AnimatedVisibility(viewingPhoto != null, enter = OVERLAY_ENTER, exit = OVERLAY_EXIT) {
            lastViewedPhoto?.let { photo ->
                PhotoViewer(photo = photo, onClose = { viewingPhoto = null })
            }
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

private const val STEP_SLIDE_DP = 28f
private const val STEP_TRANSITION_MS = 260

/**
 * Slides + fades the active step (or summary day) in from the direction of
 * travel — Next/swipe-left enters from the right, Back/swipe-right from the
 * left, and day-to-day summary navigation follows calendar order the same
 * way (`dayKey` is a `YYYY-MM-DD` string, so `>` already sorts it
 * chronologically).
 *
 * Only the *entrance* is animated. [content] always reflects live `vm`
 * state, so animating an outgoing step's disappearance would just show the
 * new step's data through the old composable for a few frames — letting the
 * old content vanish the instant [stepIndex]/[dayKey] changes, while the new
 * content slides smoothly into place on top, sidesteps that entirely.
 */
@Composable
private fun StepTransition(stepIndex: Int, dayKey: String, content: @Composable () -> Unit) {
    val target = stepIndex to dayKey

    // Plain (non-State) holders — they only need to persist the previous
    // target/first-run flag across recompositions, and must NOT themselves
    // trigger one (unlike mutableStateOf, which caused an extra frame here).
    val previousTarget = remember { arrayOf(target) }
    val isFirst = remember { booleanArrayOf(true) }

    val forward = remember(target) {
        val (prevStep, prevDay) = previousTarget[0]
        val dir = if (dayKey != prevDay) dayKey > prevDay else stepIndex > prevStep
        previousTarget[0] = target
        dir
    }

    // A fresh Animatable per target starts pre-offset/transparent on the very
    // first frame the new content is composed (except on initial app launch,
    // which renders in place with no animation). Previously a single
    // Animatable(1f) was reset via a post-composition snapTo(0f) inside
    // LaunchedEffect, leaving one frame where the new step flashed fully
    // visible before snapping away and animating back in.
    val progress = remember(target) { Animatable(if (isFirst[0]) 1f else 0f) }
    LaunchedEffect(target) {
        if (isFirst[0]) {
            isFirst[0] = false
        } else {
            progress.animateTo(1f, tween(STEP_TRANSITION_MS, easing = FastOutSlowInEasing))
        }
    }

    val offsetDp = (1f - progress.value) * STEP_SLIDE_DP * if (forward) 1f else -1f
    Box(Modifier.offset(x = offsetDp.dp).alpha(progress.value)) { content() }
}

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
