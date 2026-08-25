package com.ketotracker.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ketotracker.ui.theme.KetoTheme

/** Solid green primary action ("Next →" / "Finish ✓"). */
@Composable
fun PrimaryButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = KetoTheme.colors
    PillButton(
        text = text,
        modifier = modifier,
        bg = c.accent,
        textColor = Color.White,
        weight = FontWeight.Bold,
        onClick = onClick,
    )
}

/** Subtle back ("‹") button. */
@Composable
fun BackButton(onClick: () -> Unit) {
    val c = KetoTheme.colors
    PillButton(
        text = "‹",
        bg = c.surf2,
        textColor = c.txtM,
        border = c.bd,
        weight = FontWeight.Bold,
        padding = PaddingValues(horizontal = 18.dp, vertical = 17.dp),
        onClick = onClick,
    )
}

/** Transparent "Skip" button. */
@Composable
fun SkipButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = KetoTheme.colors
    PillButton(
        text = "Skip",
        modifier = modifier,
        bg = Color.Transparent,
        textColor = c.txtM,
        border = c.bd,
        weight = FontWeight.Normal,
        onClick = onClick,
    )
}

/**
 * Gold-outlined "🥑 Keto" button. Press → shrinks (from PillButton), release → bounces
 * bigger as positive feedback. The two scales are independent Animatables and compound
 * naturally: press = 0.95×, release bounce = 1.16× → net ~1.10×.
 */
@Composable
fun KetoButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = KetoTheme.colors
    var tapCount by remember { mutableIntStateOf(0) }
    val bounceScale = remember { Animatable(1f) }
    LaunchedEffect(tapCount) {
        if (tapCount > 0) {
            bounceScale.animateTo(1.16f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh))
            bounceScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow))
        }
    }
    Box(modifier.scale(bounceScale.value)) {
        PillButton(
            text = "🥑 Keto",
            modifier = Modifier.fillMaxWidth(),
            bg = c.gold.copy(alpha = 0.15f),
            textColor = c.gold,
            border = c.gold,
            weight = FontWeight.SemiBold,
            onClick = { tapCount++; onClick() },
        )
    }
}

@Composable
private fun PillButton(
    text: String,
    bg: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
    border: Color? = null,
    weight: FontWeight = FontWeight.Bold,
    padding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 17.dp),
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
        label = "pillBtnScale",
    )

    var m = modifier
        .scale(pressScale)
        .clip(RoundedCornerShape(13.dp))
        .background(bg)
    if (border != null) m = m.border(1.dp, border, RoundedCornerShape(13.dp))
    Box(
        modifier = m
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .padding(padding),
        contentAlignment = Alignment.Center,
    ) {
        // AnimatedContent makes the label morph smoothly when text changes
        // (e.g. "Next →" → "Finish ✓" on the last step before Summary).
        AnimatedContent(
            targetState = text,
            transitionSpec = {
                (fadeIn(tween(100)) + slideInVertically(tween(100)) { it / 2 }) togetherWith
                (fadeOut(tween(80)) + slideOutVertically(tween(80)) { -it / 2 })
            },
            label = "pillBtnLabel",
        ) { t ->
            KText(
                t,
                size = 17,
                color = textColor,
                weight = weight,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Visible,
            )
        }
    }
}
