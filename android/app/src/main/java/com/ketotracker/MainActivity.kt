package com.ketotracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.ketotracker.model.AppViewModel
import com.ketotracker.ui.screens.WizardScreen
import com.ketotracker.ui.theme.KetoTheme
import com.ketotracker.ui.theme.KetoTracker

class MainActivity : ComponentActivity() {

    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // The selected theme id lives in the ViewModel so it survives
            // recomposition and config changes.
            KetoTracker(themeId = vm.themeId) {
                androidx.compose.foundation.layout.Box(
                    Modifier.fillMaxSize().background(KetoTheme.colors.bg)
                ) {
                    WizardScreen(vm)
                }
            }
        }
    }
}
