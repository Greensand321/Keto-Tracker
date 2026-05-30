package com.ketotracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ketotracker.model.AppViewModel
import com.ketotracker.ui.components.KText
import com.ketotracker.ui.theme.KetoTheme

private const val APP_VERSION = "1.0-native-demo"

@Composable
fun SettingsSheet(vm: AppViewModel, onTheme: () -> Unit, onClose: () -> Unit) {
    val c = KetoTheme.colors

    Box(Modifier.fillMaxSize().background(c.bg)) {
        Column(Modifier.fillMaxSize()) {

            // ── Header ───────────────────────────────────────────────────
            Row(
                Modifier
                    .fillMaxWidth()
                    .border(1.dp, c.bd, RoundedCornerShape(0.dp))
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                KText("⚙️ Settings", size = 16, color = c.gold, weight = FontWeight.Bold)
                Box(Modifier.clickable { onClose() }.padding(4.dp)) {
                    KText("✕", size = 18, color = c.txtM)
                }
            }

            // ── Scrollable body ──────────────────────────────────────────
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {

                // App version
                SettingsSection("About") {
                    SettingsRow(label = "App", value = "Keto Tracker")
                    SettingsRow(label = "Version", value = APP_VERSION)
                    SettingsRow(label = "Platform", value = "Native Android (Compose)")
                    InfoBanner("This is the interface demo build. Data entered is stored in-memory only and resets when the app is closed. Full persistence (Room database) comes in the next milestone.")
                }

                // Theme
                SettingsSection("Appearance") {
                    SettingsButton("🎨 Choose Theme", subtitle = "Currently: ${vm.themeId}") {
                        onTheme()
                    }
                }

                // Data
                SettingsSection("Data") {
                    SettingsDivider("${vm.loggedKeys().size} day(s) logged this session")
                    SettingsButton(
                        "📋 Export Data",
                        subtitle = "Coming in persistence milestone",
                        enabled = false,
                    ) {}
                    SettingsButton(
                        "📥 Import Data",
                        subtitle = "Coming in persistence milestone",
                        enabled = false,
                    ) {}
                }

                // Backups
                SettingsSection("Backups") {
                    SettingsButton(
                        "💾 Snapshots",
                        subtitle = "Coming in persistence milestone",
                        enabled = false,
                    ) {}
                }

                // Storage
                SettingsSection("Storage") {
                    StorageBar(
                        usedLabel = "Session only (in-memory)",
                        pct = 0f,
                    )
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

// ── Sub-components ────────────────────────────────────────────────────────────

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    val c = KetoTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        KText(
            title.uppercase(),
            size = 11,
            color = c.txtM,
            weight = FontWeight.Bold,
            letterSpacing = 1.8f,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
        content()
    }
}

@Composable
private fun SettingsRow(label: String, value: String) {
    val c = KetoTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(c.surf)
            .border(1.dp, c.bd, RoundedCornerShape(13.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        KText(label, size = 15, color = c.txtM)
        KText(value, size = 15, color = c.txt, weight = FontWeight.SemiBold)
    }
}

@Composable
private fun SettingsButton(
    title: String,
    subtitle: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val c = KetoTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(c.surf)
            .border(1.dp, c.bd, RoundedCornerShape(13.dp))
            .then(if (enabled) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        KText(title, size = 15, color = if (enabled) c.txt else c.txtD, weight = FontWeight.SemiBold)
        if (subtitle != null) {
            KText(subtitle, size = 12, color = c.txtD)
        }
    }
}

@Composable
private fun SettingsDivider(note: String) {
    val c = KetoTheme.colors
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(c.surf2)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        KText(note, size = 13, color = c.txtM)
    }
}

@Composable
private fun InfoBanner(text: String) {
    val c = KetoTheme.colors
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(c.gold.copy(alpha = 0.08f))
            .border(1.dp, c.gold.copy(alpha = 0.3f), RoundedCornerShape(13.dp))
            .padding(14.dp),
    ) {
        KText(text, size = 13, color = c.gold)
    }
}

@Composable
private fun StorageBar(usedLabel: String, pct: Float) {
    val c = KetoTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(c.surf)
            .border(1.dp, c.bd, RoundedCornerShape(13.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            KText("Storage", size = 14, color = c.txtM)
            KText(usedLabel, size = 13, color = c.txtD)
        }
        // Bar track
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(c.surf2)
        ) {
            if (pct > 0f) {
                Box(
                    Modifier
                        .fillMaxWidth(pct)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(c.accent)
                )
            }
        }
    }
}
