package com.ketotracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ketotracker.ui.theme.KETO_THEMES
import com.ketotracker.ui.theme.KetoTheme
import com.ketotracker.ui.theme.THEME_LIST
import com.ketotracker.ui.theme.ThemeInfo

/** Bottom-anchored theme picker, shown as an overlay above everything. */
@Composable
fun ThemePanel(
    currentId: String,
    onPick: (String) -> Unit,
    onClose: () -> Unit,
) {
    val c = KetoTheme.colors
    // Scrim
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable { onClose() },
        contentAlignment = Alignment.BottomCenter,
    ) {
        // Panel — consume clicks so taps inside don't dismiss.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(c.bg)
                .border(1.dp, c.bdI, RoundedCornerShape(20.dp))
                .clickable(enabled = false) {}
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                KText("🎨 Themes", size = 16, color = c.gold, weight = FontWeight.Bold)
                Box(Modifier.align(Alignment.CenterEnd).clickable { onClose() }) {
                    KText("✕", size = 16, color = c.txtM)
                }
            }

            ThemeSection("Dark", THEME_LIST.filter { it.dark }, currentId, onPick)
            ThemeSection("Light", THEME_LIST.filter { !it.dark }, currentId, onPick)
        }
    }
}

@Composable
private fun ThemeSection(title: String, themes: List<ThemeInfo>, currentId: String, onPick: (String) -> Unit) {
    val c = KetoTheme.colors
    KText(title.uppercase(), size = 11, color = c.txtM, weight = FontWeight.Bold, letterSpacing = 1.8f)
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = Modifier.fillMaxWidth().heightForRows(themes.size),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        userScrollEnabled = false,
    ) {
        items(themes) { t -> ThemeSwatch(t, selected = t.id == currentId, onPick = onPick) }
    }
}

@Composable
private fun ThemeSwatch(info: ThemeInfo, selected: Boolean, onPick: (String) -> Unit) {
    val c = KetoTheme.colors
    val swatchBg = KETO_THEMES[info.id]?.bg ?: c.surf
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(c.surf2)
            .border(2.dp, if (selected) c.accent else c.bd, RoundedCornerShape(12.dp))
            .clickable { onPick(info.id) }
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(50))
                .background(swatchBg)
                .border(1.dp, c.bdI, RoundedCornerShape(50)),
            contentAlignment = Alignment.Center,
        ) { KText(info.emoji, size = 14) }
        KText(info.label, size = 10, color = if (selected) c.accent else c.txtM, weight = FontWeight.SemiBold)
    }
}

// Fixed-height helper so the non-scrolling grid lays out fully inside a Column.
private fun Modifier.heightForRows(count: Int): Modifier {
    val rows = (count + 3) / 4
    return this.height((rows * 72).dp)
}
