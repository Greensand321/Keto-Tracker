@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.ketotracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ketotracker.data.DateUtils
import com.ketotracker.data.Meal
import com.ketotracker.data.SUPPLEMENT_DEFAULTS
import com.ketotracker.model.AppViewModel
import com.ketotracker.ui.components.KText
import com.ketotracker.ui.theme.KetoTheme

/** Full-screen modal scaffold matching the web `.fs-modal`. */
@Composable
private fun FullScreenSheet(title: String, onClose: () -> Unit, body: @Composable () -> Unit) {
    val c = KetoTheme.colors
    Box(Modifier.fillMaxSize().background(c.bg)) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .border(1.dp, c.bd, RoundedCornerShape(0.dp))
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                KText(title, size = 16, color = c.gold, weight = FontWeight.Bold)
                Box(Modifier.clickable { onClose() }.padding(4.dp)) {
                    KText("✕", size = 18, color = c.txtM)
                }
            }
            Box(Modifier.fillMaxSize()) { body() }
        }
    }
}

// ── Overview: list of all logged days ───────────────────────────────────────
@Composable
fun OverviewSheet(vm: AppViewModel, onJump: (String) -> Unit, onClose: () -> Unit) {
    val c = KetoTheme.colors
    FullScreenSheet("📋 All Days", onClose) {
        val keys = vm.loggedKeys()
        if (keys.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                KText("No days logged yet", size = 15, color = c.txtM)
            }
            return@FullScreenSheet
        }
        LazyColumn(
            Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(keys) { key ->
                val e = vm.entryFor(key)
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (e.notInKeto) c.red.copy(alpha = 0.08f) else c.surf)
                        .border(1.dp, c.bd, RoundedCornerShape(16.dp))
                        .clickable { onJump(key) }
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    KText(DateUtils.fmtDate(key), size = 15, color = c.gold, weight = FontWeight.Bold)
                    listOf("🍳" to e.breakfast, "🥗" to e.lunch, "🍽️" to e.dinner).forEach { (ic, txt) ->
                        if (txt.isNotEmpty()) KText("$ic $txt", size = 13, color = c.txtM, maxLines = 1)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        e.energy?.let { Stat("⚡", "$it/5") }
                        e.happiness?.let { Stat("😊", "$it/5") }
                        if (e.tested) Stat("🧪", "Tested")
                        if (e.notInKeto) Stat("⚠️", "Off")
                    }
                }
            }
        }
    }
}

@Composable
private fun Stat(icon: String, value: String) {
    val c = KetoTheme.colors
    Row(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(c.inp)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        KText(icon, size = 12)
        KText(value, size = 12, color = c.txt, weight = FontWeight.Bold)
    }
}

// ── Supplements: chips with tap-to-increment counts ─────────────────────────
@Composable
fun SupplementsSheet(vm: AppViewModel, onClose: () -> Unit) {
    val c = KetoTheme.colors
    FullScreenSheet("💊 Supplements", onClose) {
        Column(
            Modifier.fillMaxSize().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            KText("Tap to add one. Tap again to add more; long-press a count to clear.", size = 13, color = c.txtM)
            FlowChips {
                SUPPLEMENT_DEFAULTS.forEach { name ->
                    val count = vm.entry.supplements[name] ?: 0
                    SupplementChip(
                        name = name,
                        count = count,
                        onTap = { vm.setSupplement(name, count + 1) },
                        onClear = { vm.setSupplement(name, 0) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SupplementChip(name: String, count: Int, onTap: () -> Unit, onClear: () -> Unit) {
    val c = KetoTheme.colors
    val active = count > 0
    Box {
        Box(
            Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(if (active) c.accent.copy(alpha = 0.15f) else c.surf)
                .border(1.5.dp, if (active) c.accent else c.bdI, RoundedCornerShape(20.dp))
                .clickable { onTap() }
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            KText(name, size = 15, color = if (active) c.accent else c.txt, weight = FontWeight.SemiBold)
        }
        if (active) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .size(20.dp)
                    .clip(RoundedCornerShape(50))
                    .background(c.accent)
                    .clickable { onClear() },
                contentAlignment = Alignment.Center,
            ) {
                KText("$count", size = 11, color = Color.White, weight = FontWeight.ExtraBold)
            }
        }
    }
}

// ── Quick-select: tap food chips to append to the meal text ──────────────────
@Composable
fun QuickSelectSheet(vm: AppViewModel, meal: Meal, onClose: () -> Unit) {
    val c = KetoTheme.colors
    val title = "⚡ " + meal.field.replaceFirstChar { it.uppercase() }
    FullScreenSheet(title, onClose) {
        Column(
            Modifier.fillMaxSize().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            KText("Tap items to add them to this meal.", size = 13, color = c.txtM)
            FlowChips {
                QUICK_FOODS.forEach { food ->
                    val current = vm.entry.mealText(meal)
                    val selected = current.split(",").map { it.trim() }.contains(food)
                    FoodChip(food, selected) {
                        val parts = current.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
                        if (selected) parts.remove(food) else parts.add(food)
                        vm.setMealText(meal, parts.joinToString(", "))
                    }
                }
            }
        }
    }
}

@Composable
private fun FoodChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val c = KetoTheme.colors
    Box(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) c.accent.copy(alpha = 0.15f) else c.surf)
            .border(1.5.dp, if (selected) c.accent else c.bdI, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        KText(label, size = 15, color = if (selected) c.accent else c.txt, weight = FontWeight.SemiBold)
    }
}

private val QUICK_FOODS = listOf(
    "Eggs", "Bacon", "Chicken", "Steak", "Salmon", "Avocado", "Cheddar", "HM Mayo",
    "Sourdough", "Broccoli", "Cauliflower", "Almonds", "Coffee", "Butter", "Cream", "Olive Oil",
)

/** Simple wrapping row of chips using FlowRow. */
@Composable
private fun FlowChips(content: @Composable androidx.compose.foundation.layout.FlowRowScope.() -> Unit) {
    androidx.compose.foundation.layout.FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}
