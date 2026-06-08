package com.ketotracker.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.ketotracker.data.DateUtils
import com.ketotracker.data.io.StorageStats
import com.ketotracker.model.AppViewModel
import com.ketotracker.model.ImportMode
import com.ketotracker.model.PendingImport
import com.ketotracker.ui.components.KText
import com.ketotracker.ui.theme.KetoTheme

private const val APP_VERSION = "1.0-native-demo"

@Composable
fun SettingsSheet(vm: AppViewModel, onTheme: () -> Unit, onClose: () -> Unit) {
    val c = KetoTheme.colors
    val context = LocalContext.current

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) vm.exportAll(context, uri)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.importFrom(context, uri)
    }

    LaunchedEffect(Unit) { vm.loadStorageStats(context) }

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
                    InfoBanner("Data is stored locally on this device using Room. Your log survives app restarts and updates. Themes are persisted via DataStore.")
                }

                // Theme
                SettingsSection("Appearance") {
                    SettingsButton(
                        "🎨 Choose Theme",
                        subtitle = if (vm.autoThemeEnabled) "Auto — follows system dark/light mode" else "Currently: ${vm.themeId}",
                    ) {
                        onTheme()
                    }
                }

                // Data
                SettingsSection("Data") {
                    SettingsDivider("${vm.loggedKeys().size} day(s) logged")
                    SettingsButton("📋 Export Data", subtitle = "Save all entries as a .json file") {
                        exportLauncher.launch("keto-all-data-${DateUtils.todayKey()}.json")
                    }
                    SettingsButton("📥 Import Data", subtitle = "Merge entries from a .json file") {
                        importLauncher.launch(arrayOf("application/json"))
                    }
                }

                // Backups
                SettingsSection("Backups") {
                    SettingsButton(
                        "💾 Snapshots",
                        subtitle = "Coming soon",
                        enabled = false,
                    ) {}
                }

                // Storage
                SettingsSection("Storage") {
                    StorageBar(vm.storageStats)
                }

                Spacer(Modifier.height(32.dp))
            }
        }

        vm.pendingImport?.let { pending ->
            ImportConfirmDialog(
                pending = pending,
                onConfirm = { vm.confirmImport(it) },
                onCancel = { vm.cancelImport() },
            )
        }
    }
}

// ── Import confirmation ───────────────────────────────────────────────────────
// Custom-styled dialog (no Material3 AlertDialog used anywhere in this codebase)
// that collapses the web app's chained confirm() calls into one screen — see
// CLAUDE.md "Import" for the merge/overwrite/skip semantics this mirrors.

@Composable
private fun ImportConfirmDialog(
    pending: PendingImport,
    onConfirm: (ImportMode) -> Unit,
    onCancel: () -> Unit,
) {
    val c = KetoTheme.colors
    Dialog(onDismissRequest = onCancel) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(c.surf)
                .border(1.dp, c.bdI, RoundedCornerShape(18.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            KText("📥 Import data?", size = 17, color = c.gold, weight = FontWeight.Bold)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (pending.newCount > 0) {
                    KText("• ${pending.newCount} new day(s) will be added", size = 14, color = c.txt)
                }
                if (pending.dupCount > 0) {
                    KText("• ${pending.dupCount} day(s) already exist — choose how to handle them below", size = 14, color = c.txt)
                }
            }
            if (pending.dupCount > 0) {
                DialogOption("Merge — fill empty fields only", "Existing values are kept; gaps are filled from the import") { onConfirm(ImportMode.MERGE) }
                DialogOption("Overwrite", "Imported data replaces the existing duplicate days") { onConfirm(ImportMode.OVERWRITE) }
                DialogOption("Skip duplicates", "Keep existing data; only add the new days") { onConfirm(ImportMode.SKIP) }
            } else {
                DialogOption("Import", "Add ${pending.newCount} day(s) to your log") { onConfirm(ImportMode.SKIP) }
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .clickable { onCancel() }
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                KText("Cancel", size = 14, color = c.txtM, weight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun DialogOption(title: String, subtitle: String, onClick: () -> Unit) {
    val c = KetoTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(c.inp)
            .border(1.dp, c.bd, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        KText(title, size = 14, color = c.txt, weight = FontWeight.SemiBold)
        KText(subtitle, size = 12, color = c.txtM)
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

/**
 * Real on-device usage — native counterpart of the web app's storage bar
 * (CLAUDE.md "Settings Modal" / `getStorageStats()`). [stats] is `null`
 * while `loadStorageStats` is still sizing the database file and photo
 * directory on `Dispatchers.IO`.
 */
@Composable
private fun StorageBar(stats: StorageStats?) {
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
            KText(
                if (stats == null) "Calculating…" else "${formatKB(stats.totalKB)} used",
                size = 13,
                color = c.txtD,
            )
        }
        // Bar track
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(c.surf2)
        ) {
            val pct = stats?.pct ?: 0f
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
        if (stats != null) {
            KText(
                "${stats.days} day(s) logged · ${stats.photoCount} photo(s) · " +
                    "${formatKB(stats.dbKB)} log data + ${formatKB(stats.photoKB)} photos",
                size = 12,
                color = c.txtD,
            )
        }
    }
}

private fun formatKB(kb: Int): String =
    if (kb >= 1024) "%.1f MB".format(kb / 1024f) else "$kb KB"
