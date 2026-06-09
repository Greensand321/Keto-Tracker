@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.ketotracker.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ketotracker.data.DateUtils
import com.ketotracker.data.SnapshotMeta
import com.ketotracker.data.io.StorageStats
import com.ketotracker.model.AppViewModel
import com.ketotracker.model.ImportMode
import com.ketotracker.model.PendingImport
import com.ketotracker.ui.components.KText
import com.ketotracker.ui.theme.KetoTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val APP_VERSION = "1.0-native-demo"

private enum class SettingsPage {
    MAIN, DATA_BACKUPS, QUICK_SELECT, NOTIFICATIONS, STORAGE, APPEARANCE, ABOUT
}

@Composable
fun SettingsSheet(vm: AppViewModel, onTheme: () -> Unit, onClose: () -> Unit) {
    val c = KetoTheme.colors
    val context = LocalContext.current
    var page by remember { mutableStateOf(SettingsPage.MAIN) }

    // All SAF launchers live at the top level so they're always composed and
    // can safely receive results regardless of which sub-page is visible.
    val exportJsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> if (uri != null) vm.exportAll(context, uri) }

    val importJsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) vm.importFrom(context, uri) }

    val exportZipLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri -> if (uri != null) vm.exportFullBackup(context, uri) }

    val importZipLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) vm.importFromZip(context, uri) }

    var pendingSnapshotExportId by remember { mutableStateOf<Long?>(null) }
    val exportSnapshotLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val id = pendingSnapshotExportId
        if (uri != null && id != null) vm.exportSnapshot(context, uri, id)
        pendingSnapshotExportId = null
    }

    Box(Modifier.fillMaxSize().background(c.bg)) {
        Crossfade(
            targetState = page,
            animationSpec = tween(200),
            label = "settings_nav",
        ) { currentPage ->
            when (currentPage) {
                SettingsPage.MAIN -> SettingsMainPage(
                    vm = vm,
                    onClose = onClose,
                    onNavigate = { page = it },
                )
                SettingsPage.DATA_BACKUPS -> SettingsDataBackupsPage(
                    vm = vm,
                    onBack = { page = SettingsPage.MAIN },
                    onExportJson = { exportJsonLauncher.launch("keto-all-data-${DateUtils.todayKey()}.json") },
                    onImportJson = { importJsonLauncher.launch(arrayOf("application/json")) },
                    onExportZip = { exportZipLauncher.launch("keto-backup-${DateUtils.todayKey()}.zip") },
                    onImportZip = { importZipLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed", "*/*")) },
                    onExportSnapshot = { id ->
                        pendingSnapshotExportId = id
                        exportSnapshotLauncher.launch("keto-snapshot-${DateUtils.todayKey()}.json")
                    },
                )
                SettingsPage.QUICK_SELECT -> SettingsQuickSelectPage(
                    vm = vm,
                    onBack = { page = SettingsPage.MAIN },
                )
                SettingsPage.NOTIFICATIONS -> SettingsNotificationsPage(
                    onBack = { page = SettingsPage.MAIN },
                )
                SettingsPage.STORAGE -> SettingsStoragePage(
                    vm = vm,
                    onBack = { page = SettingsPage.MAIN },
                )
                SettingsPage.APPEARANCE -> SettingsAppearancePage(
                    vm = vm,
                    onTheme = onTheme,
                    onBack = { page = SettingsPage.MAIN },
                )
                SettingsPage.ABOUT -> SettingsAboutPage(
                    onBack = { page = SettingsPage.MAIN },
                )
            }
        }

        // Import confirmation dialog floats above all sub-pages
        vm.pendingImport?.let { pending ->
            ImportConfirmDialog(
                pending = pending,
                onConfirm = { vm.confirmImport(it) },
                onCancel = { vm.cancelImport() },
            )
        }
    }
}

// ── Main page ─────────────────────────────────────────────────────────────────

@Composable
private fun SettingsMainPage(
    vm: AppViewModel,
    onClose: () -> Unit,
    onNavigate: (SettingsPage) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        SettingsHeader(title = "⚙️ Settings", onAction = onClose, actionLabel = "✕")
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            NavRow(icon = "📁", label = "Data & Backups",
                sub = "${vm.loggedKeys().size} day(s) · ${vm.snapshots.size} snapshot(s)") {
                onNavigate(SettingsPage.DATA_BACKUPS)
            }
            NavRow(icon = "⚡", label = "Quick-Select Foods",
                sub = "${vm.quickSelectItems.size} item(s)") {
                onNavigate(SettingsPage.QUICK_SELECT)
            }
            NavRow(icon = "🔔", label = "Notifications", sub = "Coming soon") {
                onNavigate(SettingsPage.NOTIFICATIONS)
            }
            NavRow(icon = "💾", label = "Storage", sub = null) {
                onNavigate(SettingsPage.STORAGE)
            }
            NavRow(icon = "🎨", label = "Appearance",
                sub = if (vm.autoThemeEnabled) "Auto" else vm.themeId) {
                onNavigate(SettingsPage.APPEARANCE)
            }
            NavRow(icon = "ℹ️", label = "About", sub = "v$APP_VERSION") {
                onNavigate(SettingsPage.ABOUT)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── Data & Backups page ───────────────────────────────────────────────────────

@Composable
private fun SettingsDataBackupsPage(
    vm: AppViewModel,
    onBack: () -> Unit,
    onExportJson: () -> Unit,
    onImportJson: () -> Unit,
    onExportZip: () -> Unit,
    onImportZip: () -> Unit,
    onExportSnapshot: (Long) -> Unit,
) {
    val context = LocalContext.current
    var showSaveDialog by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf("") }
    var confirmDeleteSnap by remember { mutableStateOf<SnapshotMeta?>(null) }
    var confirmRestoreSnap by remember { mutableStateOf<SnapshotMeta?>(null) }

    Column(Modifier.fillMaxSize()) {
        SettingsSubHeader("📁 Data & Backups", onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // JSON export/import
            SettingsSection("JSON Export / Import") {
                SettingsDivider("${vm.loggedKeys().size} day(s) logged")
                SettingsButton("📋 Export Data", subtitle = "Save all entries as a .json file") {
                    onExportJson()
                }
                SettingsButton("📥 Import Data", subtitle = "Merge entries from a .json file") {
                    onImportJson()
                }
            }

            // Full ZIP backup
            SettingsSection("Full Backup") {
                InfoBanner("Includes all entries AND meal photos in a single .zip archive.")
                SettingsButton("📦 Export Full Backup", subtitle = "Save data + photos as a .zip") {
                    onExportZip()
                }
                SettingsButton("📥 Import from Backup", subtitle = "Restore data and photos from a .zip") {
                    onImportZip()
                }
            }

            // Snapshots
            SettingsSection("Snapshots") {
                SettingsButton("💾 Save Snapshot",
                    subtitle = "Name and save a point-in-time backup (up to 25)") {
                    nameInput = ""
                    showSaveDialog = true
                }
                if (vm.snapshots.isEmpty()) {
                    SettingsDivider("No snapshots yet")
                } else {
                    vm.snapshots.sortedByDescending { it.ts }.forEach { snap ->
                        SnapshotRow(
                            snap = snap,
                            onRestore = { confirmRestoreSnap = snap },
                            onExport = { onExportSnapshot(snap.id) },
                            onDelete = { confirmDeleteSnap = snap },
                        )
                    }
                }
            }

            // Periodic backup
            SettingsSection("Periodic Backup") {
                SettingsButton(
                    title = "🔄 Auto-Backup",
                    subtitle = if (vm.backupEnabled)
                        "On — ${vm.backupFrequency.replaceFirstChar { it.uppercase() }}"
                    else "Off — tap to enable",
                ) {
                    vm.setBackupEnabled(context, !vm.backupEnabled)
                }
                if (vm.backupEnabled) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        FrequencyChip(
                            label = "Daily",
                            selected = vm.backupFrequency == "daily",
                            modifier = Modifier.weight(1f),
                        ) { vm.setBackupFrequency(context, "daily") }
                        FrequencyChip(
                            label = "Weekly",
                            selected = vm.backupFrequency == "weekly",
                            modifier = Modifier.weight(1f),
                        ) { vm.setBackupFrequency(context, "weekly") }
                    }
                    InfoBanner("Backup files are saved to your device's external storage under Keto Tracker/backups/. The last 7 files are kept.")
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    // Save Snapshot dialog
    if (showSaveDialog) {
        val c = KetoTheme.colors
        Dialog(onDismissRequest = { showSaveDialog = false }) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(c.surf)
                    .border(1.dp, c.bdI, RoundedCornerShape(18.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                KText("💾 Save Snapshot", size = 17, color = c.gold, weight = FontWeight.Bold)
                KText("Give this snapshot a name so you can find it later.", size = 14, color = c.txtM)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(c.inp)
                        .border(1.dp, c.bd, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                ) {
                    if (nameInput.isEmpty()) {
                        KText("e.g. Pre-holiday backup", size = 15, color = c.txtD)
                    }
                    BasicTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        singleLine = true,
                        textStyle = TextStyle(color = c.txt, fontSize = 15.sp),
                        cursorBrush = SolidColor(c.accent),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                DialogOption("Save", "Snapshot ${vm.loggedKeys().size} day(s)") {
                    vm.saveSnapshot(nameInput)
                    showSaveDialog = false
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clickable { showSaveDialog = false }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    KText("Cancel", size = 14, color = c.txtM, weight = FontWeight.SemiBold)
                }
            }
        }
    }

    // Restore confirmation dialog
    confirmRestoreSnap?.let { snap ->
        val c = KetoTheme.colors
        Dialog(onDismissRequest = { confirmRestoreSnap = null }) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(c.surf)
                    .border(1.dp, c.bdI, RoundedCornerShape(18.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                KText("⚠️ Restore Snapshot?", size = 17, color = c.gold, weight = FontWeight.Bold)
                KText(
                    "\"${snap.name}\" — ${snap.days} day(s)\nThis will replace ALL current data.",
                    size = 14, color = c.txt,
                )
                DialogOption("Restore", "Replace current data with this snapshot") {
                    vm.restoreSnapshot(snap.id)
                    confirmRestoreSnap = null
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clickable { confirmRestoreSnap = null }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    KText("Cancel", size = 14, color = c.txtM, weight = FontWeight.SemiBold)
                }
            }
        }
    }

    // Delete confirmation dialog
    confirmDeleteSnap?.let { snap ->
        val c = KetoTheme.colors
        Dialog(onDismissRequest = { confirmDeleteSnap = null }) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(c.surf)
                    .border(1.dp, c.bdI, RoundedCornerShape(18.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                KText("🗑️ Delete Snapshot?", size = 17, color = c.gold, weight = FontWeight.Bold)
                KText("\"${snap.name}\" — ${snap.days} day(s)", size = 14, color = c.txt)
                DialogOption("Delete", "This cannot be undone") {
                    vm.deleteSnapshot(snap.id)
                    confirmDeleteSnap = null
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clickable { confirmDeleteSnap = null }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    KText("Cancel", size = 14, color = c.txtM, weight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun SnapshotRow(
    snap: SnapshotMeta,
    onRestore: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit,
) {
    val c = KetoTheme.colors
    val date = remember(snap.ts) {
        Instant.ofEpochMilli(snap.ts)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(c.surf)
            .border(1.dp, c.bd, RoundedCornerShape(13.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                KText(snap.name, size = 15, color = c.txt, weight = FontWeight.SemiBold)
                KText("$date · ${snap.days} day(s)", size = 12, color = c.txtD)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SmallActionButton("Restore", c.accent) { onRestore() }
            SmallActionButton("Export", c.gold) { onExport() }
            SmallActionButton("Delete", c.red) { onDelete() }
        }
    }
}

@Composable
private fun SmallActionButton(label: String, color: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    val c = KetoTheme.colors
    Box(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        KText(label, size = 13, color = color, weight = FontWeight.SemiBold)
    }
}

@Composable
private fun FrequencyChip(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = KetoTheme.colors
    Box(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) c.accent.copy(alpha = 0.15f) else c.surf)
            .border(1.5.dp, if (selected) c.accent else c.bdI, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        KText(label, size = 14, color = if (selected) c.accent else c.txtM, weight = FontWeight.SemiBold)
    }
}

// ── Quick-Select page ─────────────────────────────────────────────────────────

@Composable
private fun SettingsQuickSelectPage(vm: AppViewModel, onBack: () -> Unit) {
    val c = KetoTheme.colors
    var newItemInput by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize()) {
        SettingsSubHeader("⚡ Quick-Select Foods", onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            KText(
                "These items appear as chips in the Quick-Select panel when logging meals. Tap × to remove one.",
                size = 13, color = c.txtM,
            )

            // Current items
            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                vm.quickSelectItems.forEach { food ->
                    RemovableChip(food) { vm.removeQuickSelectItem(food) }
                }
            }

            // Add new item
            SettingsSection("Add Item") {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(c.inp)
                            .border(1.dp, c.bd, RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                    ) {
                        if (newItemInput.isEmpty()) {
                            KText("New food item…", size = 15, color = c.txtD)
                        }
                        BasicTextField(
                            value = newItemInput,
                            onValueChange = { newItemInput = it },
                            singleLine = true,
                            textStyle = TextStyle(color = c.txt, fontSize = 15.sp),
                            cursorBrush = SolidColor(c.accent),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(c.accent)
                            .clickable {
                                if (newItemInput.isNotBlank()) {
                                    vm.addQuickSelectItem(newItemInput)
                                    newItemInput = ""
                                }
                            }
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                    ) {
                        KText("Add", size = 15, color = androidx.compose.ui.graphics.Color.White, weight = FontWeight.SemiBold)
                    }
                }
            }

            SettingsButton("↩ Restore Defaults", subtitle = "Reset to the original ${AppViewModel.DEFAULT_QUICK_FOODS.size} items") {
                vm.resetQuickSelectDefaults()
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun RemovableChip(label: String, onRemove: () -> Unit) {
    val c = KetoTheme.colors
    Row(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(c.surf)
            .border(1.dp, c.bd, RoundedCornerShape(20.dp))
            .padding(start = 14.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        KText(label, size = 14, color = c.txt)
        Box(
            Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(50))
                .background(c.bdI)
                .clickable { onRemove() },
            contentAlignment = Alignment.Center,
        ) {
            KText("×", size = 14, color = c.txtM, weight = FontWeight.Bold)
        }
    }
}

// ── Notifications page ────────────────────────────────────────────────────────

@Composable
private fun SettingsNotificationsPage(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        SettingsSubHeader("🔔 Notifications", onBack)
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                KText("🔔", size = 40)
                KText("Notifications", size = 18, color = KetoTheme.colors.gold, weight = FontWeight.Bold)
                KText("Coming soon — daily logging reminders.", size = 14, color = KetoTheme.colors.txtM)
            }
        }
    }
}

// ── Storage page ──────────────────────────────────────────────────────────────

@Composable
private fun SettingsStoragePage(vm: AppViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { vm.loadStorageStats(context) }

    Column(Modifier.fillMaxSize()) {
        SettingsSubHeader("💾 Storage", onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            SettingsSection("Usage") {
                StorageBar(vm.storageStats)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── Appearance page ───────────────────────────────────────────────────────────

@Composable
private fun SettingsAppearancePage(vm: AppViewModel, onTheme: () -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        SettingsSubHeader("🎨 Appearance", onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            SettingsSection("Theme") {
                SettingsButton(
                    "🎨 Choose Theme",
                    subtitle = if (vm.autoThemeEnabled) "Auto — follows system dark/light mode"
                    else "Currently: ${vm.themeId}",
                ) {
                    onTheme()
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── About page ────────────────────────────────────────────────────────────────

@Composable
private fun SettingsAboutPage(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        SettingsSubHeader("ℹ️ About", onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            SettingsSection("About") {
                SettingsRow(label = "App", value = "Keto Tracker")
                SettingsRow(label = "Version", value = APP_VERSION)
                SettingsRow(label = "Platform", value = "Native Android (Compose)")
                InfoBanner("Data is stored locally on this device using Room. Your log survives app restarts and updates. Themes are persisted via DataStore.")
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── Import confirmation ───────────────────────────────────────────────────────

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

// ── Shared header components ──────────────────────────────────────────────────

@Composable
private fun SettingsHeader(title: String, onAction: () -> Unit, actionLabel: String) {
    val c = KetoTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .border(1.dp, c.bd, RoundedCornerShape(0.dp))
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        KText(title, size = 16, color = c.gold, weight = FontWeight.Bold)
        Box(Modifier.clickable { onAction() }.padding(4.dp)) {
            KText(actionLabel, size = 18, color = c.txtM)
        }
    }
}

@Composable
private fun SettingsSubHeader(title: String, onBack: () -> Unit) {
    val c = KetoTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .border(1.dp, c.bd, RoundedCornerShape(0.dp))
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.clickable { onBack() }.padding(end = 12.dp)) {
            KText("‹", size = 22, color = c.gold, weight = FontWeight.Bold)
        }
        KText(title, size = 16, color = c.gold, weight = FontWeight.Bold)
    }
}

// ── Sub-components ────────────────────────────────────────────────────────────

@Composable
private fun NavRow(icon: String, label: String, sub: String?, onClick: () -> Unit) {
    val c = KetoTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(c.surf)
            .border(1.dp, c.bd, RoundedCornerShape(13.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            KText(icon, size = 20)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                KText(label, size = 15, color = c.txt, weight = FontWeight.SemiBold)
                if (sub != null) KText(sub, size = 12, color = c.txtD)
            }
        }
        KText("›", size = 18, color = c.txtM)
    }
}

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
