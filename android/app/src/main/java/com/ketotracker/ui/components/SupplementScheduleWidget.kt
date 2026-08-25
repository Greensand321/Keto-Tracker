package com.ketotracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.ketotracker.data.SupplementDose
import com.ketotracker.data.SupplementSchedule
import com.ketotracker.ui.theme.KetoTheme

/**
 * "Today's Supplements" widget shown on the Flags step — the recommended doses for the
 * viewed day under the active [SupplementSchedule], each tap-to-toggle taken/not-taken
 * (backed by the same `entry.supplements` map the As-Needed chips use — see
 * `AppViewModel.toggleScheduledDose`). Tapping anywhere else on the card opens the
 * full-rotation overlay via [onOpenSchedule].
 */
@Composable
fun SupplementScheduleWidget(
    schedule: SupplementSchedule?,
    dayIndex: Int,
    doses: List<SupplementDose>,
    taken: Map<String, Int>,
    onToggle: (String) -> Unit,
    onOpenSchedule: () -> Unit,
) {
    val c = KetoTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(c.surf2)
            .border(1.dp, c.bdI, RoundedCornerShape(13.dp))
            .clickable(onClick = onOpenSchedule)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            KText(
                "💊 " + (schedule?.name ?: "Supplement Schedule"),
                size = 14, color = c.txt, weight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (schedule != null) {
                KText("Day ${dayIndex + 1} of ${schedule.cycleLengthDays}", size = 12, color = c.txtM)
            }
        }
        when {
            schedule == null -> KText(
                "No schedule set up — tap to import or create one in Settings.",
                size = 13, color = c.txtM,
            )
            doses.isEmpty() -> KText("Nothing scheduled today.", size = 13, color = c.txtM)
            else -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                doses.forEach { dose ->
                    val isTaken = (taken[dose.name] ?: 0) > 0
                    DoseRow(dose = dose, taken = isTaken, onToggle = { onToggle(dose.name) })
                }
            }
        }
    }
}

@Composable
private fun DoseRow(dose: SupplementDose, taken: Boolean, onToggle: () -> Unit) {
    val c = KetoTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (taken) c.accent.copy(alpha = 0.12f) else c.inp)
            .clickable(onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(if (taken) c.accent else Color.Transparent)
                .border(1.5.dp, if (taken) c.accent else c.bdI, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (taken) KText("✓", size = 12, color = Color.White, weight = FontWeight.Bold)
        }
        Column(Modifier.weight(1f)) {
            KText(dose.name, size = 14, color = c.txt, weight = FontWeight.Medium)
            if (dose.dosage.isNotEmpty()) KText(dose.dosage, size = 12, color = c.txtM)
        }
    }
}
