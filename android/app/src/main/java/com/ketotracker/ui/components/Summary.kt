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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ketotracker.data.DateUtils
import com.ketotracker.data.DayEntry
import com.ketotracker.data.Heart
import com.ketotracker.data.Meal
import com.ketotracker.data.PORTION_LABELS
import com.ketotracker.data.RATING_LABELS
import com.ketotracker.data.photo.MealPhoto
import com.ketotracker.ui.theme.KetoTheme

private data class SumRow(
    val icon: String,
    val key: String,
    val value: String?,
    val editStep: Int,
    val keto: Boolean = false,
    val time: String? = null,
    val valueColor: Color? = null,
    val subNote: String? = null,
    val meal: Meal? = null,
)

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

    val heartValue = e.heart?.let {
        when (it) {
            Heart.GOOD -> "Good"; Heart.MILD -> "Mild"; Heart.BAD -> "Bad"
        }
    }
    val heartColor = when (e.heart) {
        Heart.GOOD -> c.accent; Heart.MILD -> c.gold; Heart.BAD -> c.red; null -> null
    }

    val rows = listOf(
        SumRow("🍳", "Breakfast", e.breakfast.ifEmpty { null }, 0, e.breakfastKeto, e.breakfastTime, meal = Meal.BREAKFAST),
        SumRow("🥗", "Lunch", e.lunch.ifEmpty { null }, 1, e.lunchKeto, e.lunchTime, meal = Meal.LUNCH),
        SumRow("🍽️", "Dinner", e.dinner.ifEmpty { null }, 2, e.dinnerKeto, e.dinnerTime, meal = Meal.DINNER),
        SumRow("⚡", "Energy", e.energy?.let { "$it/5 — ${RATING_LABELS[it]}" }, 3),
        SumRow("😊", "Happiness", e.happiness?.let { "$it/5 — ${RATING_LABELS[it]}" }, 3),
        SumRow("🍽", "Portions", e.portion?.let { "$it/5 — ${PORTION_LABELS[it]}" }, 3),
        SumRow("💗", "Heart", heartValue, 4, valueColor = heartColor, subNote = e.heartNotes.ifEmpty { null }),
        SumRow(
            "💊", "Supplements",
            e.supplements.entries.filter { it.value > 0 }
                .joinToString(", ") { "${it.key} × ${it.value}" }.ifEmpty { null },
            5,
        ),
        SumRow("📝", "Notes", e.notes.ifEmpty { null }, 6),
    )

    KetoCard {
        Column {
            KText(DateUtils.fmtDate(viewedKey), size = 11, color = c.txtM, letterSpacing = 1.5f)
            KText("✅ ${if (isToday) "Today's Log" else "Day Summary"}", size = 30, color = c.gold, weight = FontWeight.ExtraBold)
        }
        if (isToday) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(c.accent.copy(alpha = 0.1f))
                    .padding(vertical = 10.dp, horizontal = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                KText("🎉 Great job logging today!", size = 14, color = c.accent, weight = FontWeight.SemiBold)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            rows.forEach { SummaryRow(it, canEdit, onEdit, mealPhotos, onViewPhoto) }

            // Flags row (only when there is something to show).
            val badges = buildList {
                if (e.notInKeto) add("⚠️ Off Keto" to c.red)
                if (e.tested) add("🧪 Tested" to c.accent)
            }
            if (badges.isNotEmpty()) {
                FlagsSummaryRow(badges, canEdit) { onEdit(5) }
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
private fun SummaryRow(
    row: SumRow,
    canEdit: Boolean,
    onEdit: (Int) -> Unit,
    mealPhotos: (Meal) -> List<MealPhoto>,
    onViewPhoto: (MealPhoto) -> Unit,
) {
    val c = KetoTheme.colors
    val photos = row.meal?.let(mealPhotos) ?: emptyList()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(c.inp)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        KText(row.icon, size = 19)
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                KText(row.key.uppercase(), size = 11, color = c.txtM, letterSpacing = 1.5f)
                if (photos.isNotEmpty()) {
                    PhotoIndicator(count = photos.size) { onViewPhoto(photos.first()) }
                }
                if (row.time != null) {
                    KText("  @ ${row.time}", size = 11, color = c.txtD)
                }
            }
            KText(
                text = row.value ?: "Not logged",
                size = 15,
                color = row.valueColor ?: if (row.value == null) c.txtM else c.txt,
                modifier = Modifier.padding(top = 3.dp),
            )
            if (row.subNote != null) {
                KText(row.subNote, size = 13, color = c.txtM, modifier = Modifier.padding(top = 2.dp))
            }
            if (row.keto) {
                Box(
                    Modifier
                        .padding(top = 4.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(c.accent.copy(alpha = 0.18f))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    KText("🥑 Keto", size = 11, color = c.accent, weight = FontWeight.SemiBold)
                }
            }
        }
        if (canEdit) EditButton { onEdit(row.editStep) }
    }
}

@Composable
private fun FlagsSummaryRow(badges: List<Pair<String, Color>>, canEdit: Boolean, onEdit: () -> Unit) {
    val c = KetoTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(c.inp)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        KText("🚩", size = 19)
        Column(Modifier.weight(1f)) {
            KText("FLAGS", size = 11, color = c.txtM, letterSpacing = 1.5f)
            Row(
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                modifier = Modifier.padding(top = 5.dp),
            ) {
                badges.forEach { (label, color) ->
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(color.copy(alpha = 0.18f))
                            .padding(horizontal = 11.dp, vertical = 4.dp),
                    ) {
                        KText(label, size = 13, color = color, weight = FontWeight.SemiBold)
                    }
                }
            }
        }
        if (canEdit) EditButton(onEdit)
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
