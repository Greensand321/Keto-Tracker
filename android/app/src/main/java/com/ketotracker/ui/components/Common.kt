package com.ketotracker.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ketotracker.data.Step
import com.ketotracker.ui.theme.KetoTheme

/** The `.card` surface: rounded, bordered, translucent. */
@Composable
fun KetoCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val c = KetoTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(c.surf)
            .border(1.dp, c.bd, RoundedCornerShape(20.dp))
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) { content() }
}

/** Step label / title / subtitle header used at the top of most step cards. */
@Composable
fun StepHeading(step: Step, showLabelAndSub: Boolean = true) {
    val c = KetoTheme.colors
    Column {
        if (showLabelAndSub) {
            KText(
                step.label.uppercase(),
                size = 11,
                weight = FontWeight.Normal,
                color = c.txtM,
                letterSpacing = 2f,
            )
        }
        KText(
            "${step.icon} ${step.title}",
            size = 30,
            weight = FontWeight.ExtraBold,
            color = c.gold,
        )
        if (showLabelAndSub && step.sub.isNotEmpty()) {
            KText(step.sub, size = 14, color = c.txtM, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

/** Progress dots — filled (done), elongated (active), faint (future). */
@Composable
fun Dots(currentIndex: Int) {
    val c = KetoTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
    ) {
        Step.dotted.forEach { s ->
            val idx = s.ordinal
            when {
                idx < currentIndex -> Dot(width = 7.dp, color = c.gold)
                idx == currentIndex -> Dot(width = 22.dp, color = c.accent, rounded = 4.dp)
                else -> Dot(width = 7.dp, color = c.bdI)
            }
        }
    }
}

@Composable
private fun Dot(width: androidx.compose.ui.unit.Dp, color: Color, rounded: androidx.compose.ui.unit.Dp? = null) {
    val shape = if (rounded != null) RoundedCornerShape(rounded) else CircleShape
    Box(
        Modifier
            .size(width = width, height = 7.dp)
            .clip(shape)
            .background(color)
    )
}

/** Thin wrapper around Text with the sizing conventions the app uses. */
@Composable
fun KText(
    text: String,
    size: Int,
    color: Color = KetoTheme.colors.txt,
    weight: FontWeight = FontWeight.Normal,
    letterSpacing: Float = 0f,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
) {
    androidx.compose.material3.Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = size.sp,
        fontWeight = weight,
        letterSpacing = letterSpacing.sp,
        lineHeight = (size * 1.25).sp,
        maxLines = maxLines,
    )
}

/** Outlined chip-style mini button (e.g. "Quick Select", "Supplements"). */
@Composable
fun fullWidthOutlineShape() = RoundedCornerShape(10.dp)

/** Reusable bordered container with the standard interactive border. */
fun Modifier.ketoBorder(color: Color, width: Float = 1f, radius: Int = 13) =
    this.border(BorderStroke(width.dp, color), RoundedCornerShape(radius.dp))
