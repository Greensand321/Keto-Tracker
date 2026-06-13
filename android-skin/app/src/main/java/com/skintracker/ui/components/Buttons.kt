package com.skintracker.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.skintracker.ui.theme.KetoTheme

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

/** Gold-outlined "🥑 Keto" button shown on meal steps. Bounces on tap as positive feedback. */
@Composable
fun KetoButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = KetoTheme.colors
    var tapCount by remember { mutableIntStateOf(0) }
    val scale = remember { Animatable(1f) }
    LaunchedEffect(tapCount) {
        if (tapCount > 0) {
            scale.animateTo(1.16f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh))
            scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow))
        }
    }
    Box(modifier.scale(scale.value)) {
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
    var m = modifier
        .clip(RoundedCornerShape(13.dp))
        .background(bg)
    if (border != null) m = m.border(1.dp, border, RoundedCornerShape(13.dp))
    Box(
        modifier = m.clickable { onClick() }.padding(padding),
        contentAlignment = Alignment.Center,
    ) {
        // Single line, never wraps — keeps every action-row button the same
        // height regardless of label length (e.g. "Next →" vs "🥑 Keto"),
        // letting it overflow the padding slightly rather than wrapping to a
        // taller two-line layout.
        KText(
            text,
            size = 17,
            color = textColor,
            weight = weight,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Visible,
        )
    }
}
