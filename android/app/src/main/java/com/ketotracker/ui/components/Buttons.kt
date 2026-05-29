package com.ketotracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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

/** Gold-outlined "🥑 Keto" button shown on meal steps. */
@Composable
fun KetoButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = KetoTheme.colors
    PillButton(
        text = "🥑 Keto",
        modifier = modifier,
        bg = c.gold.copy(alpha = 0.15f),
        textColor = c.gold,
        border = c.gold,
        weight = FontWeight.SemiBold,
        onClick = onClick,
    )
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
        KText(text, size = 17, color = textColor, weight = weight)
    }
}
