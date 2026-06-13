package com.skintracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.skintracker.data.DateUtils
import com.skintracker.data.DaySeverity
import com.skintracker.data.DayEntry
import com.skintracker.data.Flare
import com.skintracker.data.Meal
import com.skintracker.data.SymptomSnapshot
import com.skintracker.data.severity
import com.skintracker.data.photo.MealPhoto
import com.skintracker.ui.theme.KetoTheme

@Composable
fun SummaryCard(
    entry: DayEntry,
    viewedKey: String,
    isToday: Boolean,
    canEdit: Boolean,
    onEdit: (Int) -> Unit,
    mealPhotos: (Meal) -> List<MealPhoto> = { emptyList() },
    onViewPhoto: (MealPhoto) -> Unit = {},
) {
    val c = KetoTheme.colors
    val e = entry

    KetoCard {
        Column {
            KText(DateUtils.fmtDate(viewedKey), size = 11, color = c.txtM, letterSpacing = 1.5f)
            KText("✅ ${if (isToday) "Today's Log" else "Day Summary"}", size = 30, color = c.gold, weight = FontWeight.ExtraBold)
        }

        SeverityBanner(e.severity())

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Meal.entries.forEach { meal ->
                MealSummaryRow(
                    meal = meal,
                    entry = e,
                    canEdit = canEdit,
                    onEdit = onEdit,
                    photos = mealPhotos(meal),
                    onViewPhoto = onViewPhoto,
                )
            }
            if (e.flares.isNotEmpty()) {
                FlaresSummaryRow(e.flares)
            }
        }

        if (canEdit) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(11.dp))
                    .background(c.surf2)
                    .border(1.dp, c.bd, RoundedCornerShape(11.dp))
                    .clickable { onEdit(0) }
                    .padding(13.dp),
                contentAlignment = Alignment.Center,
            ) {
                KText("✏️ Edit all entries", size = 14, color = c.txtM, weight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun SeverityBanner(severity: DaySeverity) {
    val c = KetoTheme.colors
    val (color, label, emoji) = when (severity) {
        DaySeverity.SEVERE -> Triple(c.red, "Severe flare-up day", "🔴")
        DaySeverity.MILD -> Triple(c.gold, "Mild symptoms", "🟡")
        DaySeverity.CLEAR -> Triple(c.accent, "Clear — no symptoms", "🟢")
        DaySeverity.NONE -> Triple(c.txtM, "Nothing logged yet", "⚪")
    }
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(vertical = 10.dp, horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        KText("$emoji  $label", size = 14, color = color, weight = FontWeight.SemiBold)
    }
}

@Composable
private fun MealSummaryRow(
    meal: Meal,
    entry: DayEntry,
    canEdit: Boolean,
    onEdit: (Int) -> Unit,
    photos: List<MealPhoto>,
    onViewPhoto: (MealPhoto) -> Unit,
) {
    val c = KetoTheme.colors
    val icon = when (meal) { Meal.BREAKFAST -> "🍳"; Meal.LUNCH -> "🥗"; Meal.DINNER -> "🍽️" }
    val food = entry.mealText(meal).ifEmpty { null }
    val time = entry.mealTime(meal)
    val s = entry.mealSymptoms(meal)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(c.inp)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        KText(icon, size = 19)
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                KText(meal.field.uppercase(), size = 11, color = c.txtM, letterSpacing = 1.5f)
                if (photos.isNotEmpty()) {
                    PhotoIndicator(count = photos.size) { onViewPhoto(photos.first()) }
                }
                if (time != null) {
                    KText("  @ $time", size = 11, color = c.txtD)
                }
            }
            KText(
                text = food ?: "Not logged",
                size = 15,
                color = if (food == null) c.txtM else c.txt,
                modifier = Modifier.padding(top = 3.dp),
            )
            SymptomLine(s)
        }
        if (canEdit) EditButton { onEdit(meal.ordinal) }
    }
}

/** Compact line of symptom chips + swelling + touch for one reading. */
@Composable
private fun SymptomLine(s: SymptomSnapshot) {
    val c = KetoTheme.colors
    if (s.isEmpty) return
    Column(Modifier.padding(top = 5.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        val parts = buildList {
            s.itch?.let { add("🌡 $it") }
            s.redness?.let { add("🔴 $it") }
            s.bumps?.let { add("🟤 $it") }
        }
        if (parts.isNotEmpty()) {
            KText(parts.joinToString("   "), size = 13, color = c.txt, weight = FontWeight.SemiBold)
        }
        if (s.swelling.isNotEmpty()) {
            KText("🧍 Swelling: " + s.swelling.entries.joinToString(", ") { "${it.key} (${it.value})" }, size = 12, color = c.txtM)
        }
        if (s.touch.isNotBlank()) {
            KText("✋ ${s.touch}", size = 12, color = c.txtM)
        }
    }
}

@Composable
private fun FlaresSummaryRow(flares: List<Flare>) {
    val c = KetoTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(c.inp)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        KText("⚡", size = 19)
        Column(Modifier.weight(1f)) {
            KText("FLARE-UPS", size = 11, color = c.txtM, letterSpacing = 1.5f)
            flares.sortedBy { it.time }.forEach { flare ->
                Column(Modifier.padding(top = 6.dp)) {
                    KText("@ ${flare.time}", size = 12, color = c.txtD)
                    SymptomLine(flare.symptoms)
                }
            }
        }
    }
}

@Composable
private fun EditButton(onClick: () -> Unit) {
    val c = KetoTheme.colors
    Box(
        Modifier
            .clip(RoundedCornerShape(7.dp))
            .background(c.surf2)
            .border(1.dp, c.bd, RoundedCornerShape(7.dp))
            .clickable { onClick() }
            .padding(horizontal = 9.dp, vertical = 5.dp),
    ) {
        KText("✏️", size = 12)
    }
}
