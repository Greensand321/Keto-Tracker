package com.ketotracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
            // Reading vm.themeId here inside setContent means Compose registers
            // this block as an observer of that state. Any call to vm.setTheme()
            // re-runs this entire lambda, wrapping everything in the new theme.
            val themeId = vm.themeId

            KetoTracker(themeId = themeId) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(KetoTheme.colors.bg)
                ) {
                    WizardScreen(vm)
                }
            }
        }
    }
}
