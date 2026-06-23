package com.ketotracker.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ketotracker.data.DateUtils
import com.ketotracker.data.Meal
import com.ketotracker.data.Step
import com.ketotracker.data.photo.MealPhoto
import com.ketotracker.model.AppViewModel
import com.ketotracker.ui.components.BackButton
import com.ketotracker.ui.components.CalendarPanel
import com.ketotracker.ui.components.Dots
import com.ketotracker.ui.components.FlagsBody
import com.ketotracker.ui.components.HeaderBar
import com.ketotracker.ui.components.HeartBody
import com.ketotracker.ui.components.KText
import com.ketotracker.ui.components.KetoButton
import com.ketotracker.ui.components.KetoCard
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
import kotlinx.coroutines.delay

private enum class Overlay { NONE, THEME, OVERVIEW, CALENDAR, SUPPLEMENTS, QUICK_SELECT, SETTINGS }

// Bottom-sheet overlays slide up from below + fade in; reverse on close.
// Using the same spec for every overlay keeps motion consistent.
private val OVERLAY_ENTER = slideInVertically(tween(320, easing = FastOutSlowInEasing)) { it } + fadeIn(tween(260))
private val OVERLAY_EXIT  = slideOutVertically(tween(260, easing = FastOutSlowInEasing)) { it } + fadeOut(tween(200))

@Composable
fun WizardScreen(vm: AppViewModel) {
    val c = KetoTheme.colors
    var overlay by remember { mutableStateOf(Overlay.NONE) }
    var quickMeal by remember { mutableStateOf<Meal?>(null) }
    var viewingPhoto by remember { mutableStateOf<MealPhoto?>(null) }

    // Retained through the close animation so the photo viewer's exit fade
    // has something to render.
    var lastViewedPhoto by remember { mutableStateOf<MealPhoto?>(null) }
    LaunchedEffect(viewingPhoto) { viewingPhoto?.let { lastViewedPhoto = it } }

    // Tier 4: "Day logged!" celebration — shown once when entering Summary today.
    var showFinish by remember { mutableStateOf(false) }
    LaunchedEffect(vm.stepIndex) {
        if (vm.step == Step.SUMMARY && vm.isToday) {
            showFinish = true
            delay(1800)
            showFinish = false
        }
    }

    // Tier 4: "🥑 Keto!" floating stamp — ketoStampTick increments each tap,
    // triggering a fresh stamp animation via LaunchedEffect.
    var ketoStampTick by remember { mutableIntStateOf(0) }
    var showKetoStamp by remember { mutableStateOf(false) }
    LaunchedEffect(ketoStampTick) {
        if (ketoStampTick > 0) {
            showKetoStamp = true
            delay(950)
            showKetoStamp = false
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(vm) {
        vm.messages.collect { message -> snackbarHostState.showSnackbar(message) }
    }

    val canGoBack = viewingPhoto != null || overlay != Overlay.NONE || !vm.isToday || vm.stepIndex > 0
    BackHandler(enabled = canGoBack) {
        when {
            viewingPhoto != null -> viewingPhoto = null
            overlay != Overlay.NONE -> overlay = Overlay.NONE
            !vm.isToday -> vm.goToday()
            vm.stepIndex > 0 -> vm.back()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(c.bg)
            .systemBarsPadding()
    ) {
        Column(Modifier.fillMaxSize()) {

            // ── Main content area with directional page transitions ───────
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
                // Full directional cross-transition: new step slides in from the direction
                // of travel while the old step slides off to the opposite side and fades.
                // AnimatedContent's composition isolation means each slot gets its own
                // scroll state and TextField state — no key() needed.
                AnimatedContent(
                    targetState = vm.stepIndex to vm.viewedKey,
                    transitionSpec = {
                        val fwd = if (initialState.second != targetState.second)
                            targetState.second > initialState.second
                        else
                            targetState.first > initialState.first

                        // Entrance: full-width slide from direction of travel + fade in.
                        // Exit: short parallax slide + quick fade (25% travel keeps it subtle).
                        (slideInHorizontally(
                            spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow)
                        ) { if (fwd) it else -it } + fadeIn(tween(280, easing = FastOutSlowInEasing))) togetherWith
                        (slideOutHorizontally(
                            tween(220, easing = FastOutSlowInEasing)
                        ) { if (fwd) -it / 4 else it / 4 } + fadeOut(tween(200)))
                    },
                    modifier = Modifier.fillMaxSize(),
                    label = "stepTransition",
                ) { (stepIdx, _) ->
                    val frozenStep = Step.entries.getOrElse(stepIdx) { Step.SUMMARY }
                    Column(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 18.dp, vertical = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(22.dp),
                    ) {
                        if (frozenStep != Step.SUMMARY) {
                            Dots(currentIndex = stepIdx)
                        }
                        StepContent(
                            vm = vm,
                            frozenStep = frozenStep,
                            onQuickSelect = { meal ->
                                quickMeal = meal
                                overlay = Overlay.QUICK_SELECT
                            },
                            onSupplements = { overlay = Overlay.SUPPLEMENTS },
                            onViewPhoto = { viewingPhoto = it },
                            onKeto = { ketoStampTick++ },
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

        // ── Overlays ─────────────────────────────────────────────────────
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

        // ── Photo viewer ─────────────────────────────────────────────────
        AnimatedVisibility(viewingPhoto != null, enter = OVERLAY_ENTER, exit = OVERLAY_EXIT) {
            lastViewedPhoto?.let { photo ->
                PhotoViewer(photo = photo, onClose = { viewingPhoto = null })
            }
        }

        // ── Tier 4: "🥑 Keto!" stamp ─────────────────────────────────────
        AnimatedVisibility(
            visible = showKetoStamp,
            enter = scaleIn(spring(Spring.DampingRatioLowBouncy, Spring.StiffnessHigh)) + fadeIn(tween(120)),
            exit = slideOutVertically(tween(280)) { -it / 2 } + fadeOut(tween(240)),
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .background(KetoTheme.colors.gold.copy(alpha = 0.93f))
                        .padding(horizontal = 32.dp, vertical = 16.dp),
                ) {
                    KText("🥑  Keto!", size = 24, color = Color.White, weight = FontWeight.ExtraBold)
                }
            }
        }

        // ── Tier 4: "Day logged!" finish celebration ──────────────────────
        AnimatedVisibility(
            visible = showFinish,
            enter = fadeIn(tween(80)),
            exit = fadeOut(tween(500)),
        ) {
            FinishCelebration()
        }

        // ── Error feedback ────────────────────────────────────────────────
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp),
        )
    }
}

// ── Tier 4: Finish celebration overlay ───────────────────────────────────────

@Composable
private fun FinishCelebration() {
    val scale = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        scale.animateTo(1.18f, spring(Spring.DampingRatioLowBouncy, Spring.StiffnessHigh))
        scale.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium))
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.38f))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .scale(scale.value)
                .clip(RoundedCornerShape(28.dp))
                .background(KetoTheme.colors.surf)
                .padding(horizontal = 44.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            KText("✅", size = 56)
            KText("Day logged!", size = 24, weight = FontWeight.ExtraBold, color = KetoTheme.colors.accent)
            KText("Nice work 🥑", size = 15, color = KetoTheme.colors.txtM)
        }
    }
}

// ── Step content ─────────────────────────────────────────────────────────────

@Composable
private fun StepContent(
    vm: AppViewModel,
    frozenStep: Step,
    onQuickSelect: (Meal) -> Unit,
    onSupplements: () -> Unit,
    onViewPhoto: (MealPhoto) -> Unit,
    onKeto: () -> Unit,
) {
    if (frozenStep == Step.SUMMARY) {
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

    KetoCard(compact = frozenStep.isMeal || frozenStep == Step.FLAGS) {
        StepHeading(frozenStep, showLabelAndSub = !frozenStep.isMeal)

        when {
            frozenStep.isMeal -> MealBody(
                meal = frozenStep.meal!!,
                entry = vm.entry,
                onText = { vm.setMealText(frozenStep.meal!!, it) },
                onQuickSelect = { onQuickSelect(frozenStep.meal!!) },
            )
            frozenStep == Step.RATINGS -> RatingsBody(
                entry = vm.entry,
                onPick = { field, value -> vm.pickRating(field, value) },
            )
            frozenStep == Step.HEART -> HeartBody(
                entry = vm.entry,
                onSelect = { vm.selectHeart(it) },
                onNotes = { vm.setHeartNotes(it) },
            )
            frozenStep == Step.FLAGS -> FlagsBody(
                entry = vm.entry,
                onNotes = { vm.setNotes(it) },
                onToggleNotInKeto = { vm.toggleNotInKeto() },
                onToggleTested = { vm.toggleTested() },
                onOpenSupplements = onSupplements,
            )
        }

        ActionRow(vm = vm, frozenStep = frozenStep, onKeto = onKeto)

        if (frozenStep.isMeal) {
            MealPhotoArea(
                meal = frozenStep.meal!!,
                photos = vm.mealPhotos(frozenStep.meal!!),
                onCaptured = { file -> vm.addPhoto(frozenStep.meal!!, file) },
                onView = onViewPhoto,
                onRemove = { vm.removePhoto(it) },
            )
        }
    }
}

@Composable
private fun ActionRow(vm: AppViewModel, frozenStep: Step, onKeto: () -> Unit) {
    val isLastBeforeSummary = vm.stepIndex == Step.entries.size - 2
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (vm.stepIndex > 0) BackButton { vm.back() }
        PrimaryButton(
            text = if (isLastBeforeSummary) "Finish ✓" else "Next →",
            modifier = Modifier.weight(1f),
        ) { vm.next() }
        if (frozenStep.isMeal) KetoButton(Modifier.weight(1f)) { vm.markKeto(frozenStep.meal!!); onKeto() }
        if (frozenStep.isMeal) SkipButton(Modifier.weight(1f)) { vm.skip() }
    }
}

// ── Swipe helpers ─────────────────────────────────────────────────────────────

private fun onSwipeLeft(vm: AppViewModel) {
    if (vm.step == Step.SUMMARY) vm.changeDay(1) else vm.next()
}

private fun onSwipeRight(vm: AppViewModel) {
    if (vm.step == Step.SUMMARY) vm.changeDay(-1) else vm.back()
}
