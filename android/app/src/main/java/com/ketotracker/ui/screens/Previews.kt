package com.ketotracker.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.ketotracker.model.AppViewModel
import com.ketotracker.ui.theme.KetoTracker

/**
 * Android Studio design-time previews. Open this file and use the split/design
 * view to see the interface render without deploying to a device.
 *
 * Each preview just points the shared screen at a different theme id.
 */
@Preview(name = "Midnight (dark)", showBackground = true, heightDp = 780)
@Composable
private fun PreviewMidnight() {
    KetoTracker(themeId = "midnight") { WizardScreen(AppViewModel()) }
}

@Preview(name = "Pearl (light)", showBackground = true, heightDp = 780)
@Composable
private fun PreviewPearl() {
    val vm = AppViewModel().apply { setTheme("pearl") }
    KetoTracker(themeId = "pearl") { WizardScreen(vm) }
}

@Preview(name = "Forest (dark)", showBackground = true, heightDp = 780)
@Composable
private fun PreviewForest() {
    val vm = AppViewModel().apply { setTheme("forest") }
    KetoTracker(themeId = "forest") { WizardScreen(vm) }
}
