package com.ketotracker.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ketotracker.data.DayEntry
import com.ketotracker.data.Heart
import com.ketotracker.data.Meal
import com.ketotracker.data.PLACEHOLDERS
import com.ketotracker.data.PORTION_LABELS
import com.ketotracker.data.RATING_LABELS
import com.ketotracker.model.RatingField
import com.ketotracker.ui.theme.KetoTheme

/** Styled multi-line input mirroring the CSS `textarea`. */
@Composable
fun KetoTextArea(
    value: String,
    placeholder: String,
    minLines: Int,
    onValueChange: (String) -> Unit,
) {
    val c = KetoTheme.colors
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(c.inp)
            .border(1.5.dp, c.bd, RoundedCornerShape(13.dp))
            .padding(14.dp)
    ) {
        if (value.isEmpty()) {
            KText(placeholder, size = 17, color = c.txtD)
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = (minLines * 24).dp),
            textStyle = LocalTextStyle.current.copy(
                color = c.txt, fontSize = 17.sp, lineHeight = 25.sp,
            ),
            cursorBrush = SolidColor(c.accent),
        )
    }
}

/** "⚡ Quick Select" / "💊 Supplements" style full-width pill button. */
@Composable
fun QuickButton(text: String, onClick: () -> Unit) {
    val c = KetoTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
        label = "quickBtnScale",
    )
    Box(
        Modifier
            .fillMaxWidth()
            .scale(pressScale)
            .clip(RoundedCornerShape(10.dp))
            .background(c.surf2)
            .border(1.dp, c.bd, RoundedCornerShape(10.dp))
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .padding(vertical = 9.dp, horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        KText(text, size = 14, color = c.txtM, weight = FontWeight.SemiBold)
    }
}

// ── Meal step (breakfast/lunch/dinner) ─────────────────────────────────────
@Composable
fun MealBody(meal: Meal, entry: DayEntry, onText: (String) -> Unit, onQuickSelect: () -> Unit) {
    KetoTextArea(
        value = entry.mealText(meal),
        placeholder = PLACEHOLDERS[meal.field] ?: "",
        minLines = 4,
        onValueChange = onText,
    )
    QuickButton("⚡ Quick Select", onQuickSelect)
}

// ── Ratings step (energy / happiness / portions) ────────────────────────────
@Composable
fun RatingsBody(entry: DayEntry, onPick: (RatingField, Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        RatingRow("⚡ Energy", entry.energy, RATING_LABELS) { onPick(RatingField.ENERGY, it) }
        RatingRow("😊 Happiness", entry.happiness, RATING_LABELS) { onPick(RatingField.HAPPINESS, it) }
        RatingRow("🍽 Portions", entry.portion, PORTION_LABELS) { onPick(RatingField.PORTION, it) }
    }
}

@Composable
private fun RatingRow(label: String, selected: Int?, labels: Map<Int, String>, onPick: (Int) -> Unit) {
    val c = KetoTheme.colors
    Column {
        KText(label.uppercase(), size = 12, color = c.txtM, weight = FontWeight.SemiBold, letterSpacing = 0.7f, modifier = Modifier.padding(bottom = 5.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            (1..5).forEach { n ->
                val sel = selected == n

                // Bounce the button when it becomes selected.
                val bounceScale = remember { Animatable(1f) }
                LaunchedEffect(sel) {
                    if (sel) {
                        bounceScale.animateTo(1.14f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessHigh))
                        bounceScale.animateTo(1f, spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow))
                    }
                }

                // Smooth color transitions instead of instant snaps.
                val animBg by animateColorAsState(
                    if (sel) c.accent.copy(alpha = 0.12f) else c.inp, tween(160), "ratingBg$n"
                )
                val animBorder by animateColorAsState(
                    if (sel) c.accent else c.bd, tween(160), "ratingBorder$n"
                )
                val animNum by animateColorAsState(
                    if (sel) c.accent else c.txt, tween(160), "ratingNum$n"
                )
                val animLbl by animateColorAsState(
                    if (sel) c.accent else c.txtM, tween(160), "ratingLbl$n"
                )

                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()
                val pressScale by animateFloatAsState(
                    targetValue = if (isPressed) 0.93f else 1f,
                    animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessHigh),
                    label = "ratingPress$n",
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .scale(bounceScale.value * pressScale)
                        .heightIn(min = 52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(animBg)
                        .border(2.dp, animBorder, RoundedCornerShape(12.dp))
                        .clickable(interactionSource = interactionSource, indication = null) { onPick(n) }
                        .padding(vertical = 7.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    KText("$n", size = 17, color = animNum, weight = FontWeight.ExtraBold)
                    KText(
                        (labels[n] ?: "").uppercase(),
                        size = 8,
                        color = animLbl,
                        letterSpacing = 0.7f,
                    )
                }
            }
        }
    }
}

// ── Heart step ──────────────────────────────────────────────────────────────
@Composable
fun HeartBody(entry: DayEntry, onSelect: (Heart) -> Unit, onNotes: (String) -> Unit) {
    val c = KetoTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        HeartChoice("😊", "Good", entry.heart == Heart.GOOD, c.accent) { onSelect(Heart.GOOD) }
        HeartChoice("😐", "Mild", entry.heart == Heart.MILD, c.gold) { onSelect(Heart.MILD) }
        HeartChoice("😟", "Bad", entry.heart == Heart.BAD, c.red) { onSelect(Heart.BAD) }
    }
    // Notes field expands smoothly rather than popping in.
    AnimatedVisibility(
        visible = entry.heart != null && entry.heart != Heart.GOOD,
        enter = expandVertically(spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)) + fadeIn(tween(220)),
        exit = shrinkVertically(tween(200)) + fadeOut(tween(160)),
    ) {
        KetoTextArea(
            value = entry.heartNotes,
            placeholder = "Describe how your heart felt…",
            minLines = 3,
            onValueChange = onNotes,
        )
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.HeartChoice(
    emoji: String, label: String, selected: Boolean, selColor: Color, onClick: () -> Unit,
) {
    val c = KetoTheme.colors

    // Smooth color/border transitions.
    val animBg by animateColorAsState(if (selected) selColor.copy(alpha = 0.12f) else c.inp, tween(220), "heartBg")
    val animBorder by animateColorAsState(if (selected) selColor else c.bd, tween(220), "heartBorder")
    val animText by animateColorAsState(if (selected) selColor else c.txtM, tween(220), "heartText")

    // Pop bigger on select, then settle.
    val bounceScale = remember { Animatable(1f) }
    LaunchedEffect(selected) {
        if (selected) {
            bounceScale.animateTo(1.12f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessHigh))
            bounceScale.animateTo(1f, spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow))
        }
    }

    // Shrink on press.
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessHigh),
        label = "heartPress",
    )

    Column(
        modifier = Modifier
            .weight(1f)
            .scale(bounceScale.value * pressScale)
            .clip(RoundedCornerShape(14.dp))
            .background(animBg)
            .border(2.dp, animBorder, RoundedCornerShape(14.dp))
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .padding(vertical = 18.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        KText(emoji, size = 22)
        KText(label, size = 13, color = animText, weight = FontWeight.SemiBold)
    }
}

// ── Flags + Notes step (combined) ────────────────────────────────────────
@Composable
fun FlagsBody(
    entry: DayEntry,
    onNotes: (String) -> Unit,
    onToggleNotInKeto: () -> Unit,
    onToggleTested: () -> Unit,
    onOpenSupplements: () -> Unit,
) {
    val c = KetoTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        KetoTextArea(
            value = entry.notes,
            placeholder = PLACEHOLDERS["notes"] ?: "",
            minLines = 2,
            onValueChange = onNotes,
        )
        ToggleRow(
            title = "⚠️ Not in Keto",
            desc = "Did you eat outside keto today?",
            on = entry.notInKeto,
            onColor = c.red,
            onClick = onToggleNotInKeto,
        )
        ToggleRow(
            title = "🧪 Tested",
            desc = "Did you test your ketone levels today?",
            on = entry.tested,
            onColor = c.accent,
            onClick = onToggleTested,
        )
        val total = entry.supplements.values.sum()
        QuickButton("💊 Supplements" + if (total > 0) " · $total logged" else "", onOpenSupplements)
    }
}

@Composable
private fun ToggleRow(title: String, desc: String, on: Boolean, onColor: Color, onClick: () -> Unit) {
    val c = KetoTheme.colors

    val animBg by animateColorAsState(
        targetValue = if (on) onColor.copy(alpha = 0.09f) else c.inp,
        animationSpec = tween(250),
        label = "toggleBg",
    )
    val animBorder by animateColorAsState(
        targetValue = if (on) onColor else c.bd,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "toggleBorder",
    )

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessHigh),
        label = "toggleScale",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(pressScale)
            .clip(RoundedCornerShape(14.dp))
            .background(animBg)
            .border(2.dp, animBorder, RoundedCornerShape(14.dp))
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            KText(title, size = 17, weight = FontWeight.SemiBold)
            KText(desc, size = 13, color = c.txtM, modifier = Modifier.padding(top = 2.dp))
        }
        Switch(on = on, onColor = onColor)
    }
}

@Composable
private fun Switch(on: Boolean, onColor: Color) {
    val c = KetoTheme.colors
    val trackColor by animateColorAsState(
        targetValue = if (on) onColor else c.bdI,
        animationSpec = tween(200),
        label = "switch_track",
    )
    // Knob slides from left (off) to right (on) with a spring bounce.
    // 50dp track, 3dp padding each side, 21dp knob → travel = 50 - 6 - 21 = 23dp.
    val knobX by animateDpAsState(
        targetValue = if (on) 23.dp else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "switch_knob",
    )
    Box(
        Modifier
            .width(50.dp)
            .height(27.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(trackColor)
            .padding(3.dp),
    ) {
        Box(
            Modifier
                .offset(x = knobX)
                .size(21.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White)
        )
    }
}
