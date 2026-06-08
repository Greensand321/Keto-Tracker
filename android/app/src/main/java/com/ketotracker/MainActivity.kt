package com.ketotracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.ketotracker.data.photo.clearStaleCaptures
import com.ketotracker.model.AppViewModel
import com.ketotracker.ui.screens.WizardScreen
import com.ketotracker.ui.theme.KetoTheme
import com.ketotracker.ui.theme.KetoTracker
import com.ketotracker.ui.theme.resolveAutoTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val vm: AppViewModel by viewModels { AppViewModel.factory(application) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Best-effort sweep of any temp camera-capture files a previous
        // session left behind (e.g. process death between the camera writing
        // the file and PhotoStore deleting it — see CameraCapture.kt).
        lifecycleScope.launch(Dispatchers.IO) { clearStaleCaptures(applicationContext) }

        setContent {
            val themeId = if (vm.autoThemeEnabled) {
                resolveAutoTheme(vm.darkAutoThemeId, vm.lightAutoThemeId)
            } else {
                vm.themeId
            }

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
