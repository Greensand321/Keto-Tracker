package com.ketotracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ketotracker.data.CalendarDay
import com.ketotracker.data.DateUtils
import com.ketotracker.data.DayEntry
import com.ketotracker.ui.theme.KetoTheme
import java.time.LocalDate

private val MONTH_NAMES = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December",
)

private enum class CalTier { TESTED_KETO, KETO_MEALS, HAS_DATA, NONE }

/**
 * Bottom-anchored month-grid calendar — native counterpart of the web app's
 * `.cal-panel` (CLAUDE.md "Calendar Panel"). Tapping a day in the displayed
 * month jumps straight to it (`calSelect`); tapping an adjacent-month day
 * navigates the grid there instead of selecting it (`calNavMonth`). Future
 * months/days are dimmed and inert, same as the web version.
 *
 * Day colour uses the same 3-tier priority as the web app (highest wins):
 * blue = tested & on-keto, green = ≥2 keto meals logged, gold = any data —
 * see CLAUDE.md "Calendar Panel" for the exact rules.
 */
@Composable
fun CalendarPanel(
    viewedKey: String,
    entries: Map<String, DayEntry>,
    onSelect: (String) -> Unit,
    onClose: () -> Unit,
) {
    val c = KetoTheme.colors
    val today = remember { DateUtils.todayKey() }
    val todayDate = remember { LocalDate.parse(today) }
    val viewedDate = remember(viewedKey) { LocalDate.parse(viewedKey) }

    var year by remember { mutableStateOf(viewedDate.year) }
    var month by remember { mutableStateOf(viewedDate.monthValue) }

    fun go(y: Int, m: Int) {
        when {
            m < 1 -> { year = y - 1; month = 12 }
            m > 12 -> { year = y + 1; month = 1 }
            else -> { year = y; month = m }
        }
    }

    val canGoNext = year < todayDate.year || (year == todayDate.year && month < todayDate.monthValue)

    Box(Modifier.fillMaxSize()) {

        // ── Scrim ─────────────────────────────────────────────────────────
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable { onClose() }
        )

        // ── Panel (bottom-aligned, eats all its own touch events) ────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(12.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(c.bg)
                .border(1.dp, c.bdI, RoundedCornerShape(20.dp))
                .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } }
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Header: ‹ Month Year ›
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                CalNavButton("‹") { go(year, month - 1) }
                KText("${MONTH_NAMES[month - 1]} $year", size = 16, color = c.gold, weight = FontWeight.Bold)
                CalNavButton("›", enabled = canGoNext) { go(year, month + 1) }
            }

            // Day-of-week row (Sunday-first, matches the web grid)
            Row(Modifier.fillMaxWidth()) {
                listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa").forEach { dow ->
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        KText(dow, size = 10, color = c.txtD, weight = FontWeight.Bold)
                    }
                }
            }

            // Month grid — 42 cells (6 weeks × 7 days), never scrolls on its own
            val days = remember(year, month) { DateUtils.monthGrid(year, month) }
            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(258.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
                userScrollEnabled = false,
            ) {
                items(days) { day ->
                    CalendarCell(
                        day = day,
                        isToday = day.key == today,
                        isViewing = day.key == viewedKey,
                        isFuture = day.key > today,
                        entry = entries[day.key],
                        onClick = {
                            if (day.inCurrentMonth) {
                                onSelect(day.key)
                            } else {
                                val d = LocalDate.parse(day.key)
                                go(d.year, d.monthValue)
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun CalNavButton(symbol: String, enabled: Boolean = true, onClick: () -> Unit) {
    val c = KetoTheme.colors
    Box(
        Modifier
            .size(32.dp)
            .alpha(if (enabled) 1f else 0.3f)
            .clip(RoundedCornerShape(9.dp))
            .background(c.surf2)
            .border(1.dp, c.bd, RoundedCornerShape(9.dp))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        KText(symbol, size = 16, color = c.txt)
    }
}

@Composable
private fun CalendarCell(
    day: CalendarDay,
    isToday: Boolean,
    isViewing: Boolean,
    isFuture: Boolean,
    entry: DayEntry?,
    onClick: () -> Unit,
) {
    val c = KetoTheme.colors

    val tier = when {
        entry == null -> CalTier.NONE
        entry.tested && !entry.notInKeto -> CalTier.TESTED_KETO
        listOf(entry.breakfastKeto, entry.lunchKeto, entry.dinnerKeto).count { it } >= 2 -> CalTier.KETO_MEALS
        else -> CalTier.HAS_DATA
    }

    val (bg, fg) = when (tier) {
        CalTier.TESTED_KETO -> c.blue to Color.White
        CalTier.KETO_MEALS -> c.accent to Color.White
        CalTier.HAS_DATA -> c.gold to Color.Black
        CalTier.NONE -> Color.Transparent to if (isToday) c.gold else c.txtM
    }

    // CSS cascade in the web app applies `.is-viewing`'s white ring over
    // `.is-today`'s gold ring when a cell is both — replicated by priority here.
    val ring = when {
        isViewing -> Color.White
        isToday -> c.gold
        else -> null
    }

    val faded = isFuture || !day.inCurrentMonth

    Box(
        Modifier
            .aspectRatio(1f)
            .alpha(if (!faded) 1f else if (isFuture) 0.2f else 0.28f)
            .clip(CircleShape)
            .background(bg)
            .let { m -> if (ring != null) m.border(2.dp, ring, CircleShape) else m }
            .clickable(enabled = !isFuture, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        KText(
            "${day.dayOfMonth}",
            size = 13,
            color = fg,
            weight = if (tier != CalTier.NONE || isToday) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}
