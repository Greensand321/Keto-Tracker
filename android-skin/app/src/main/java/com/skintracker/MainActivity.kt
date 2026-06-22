package com.skintracker

import android.content.Intent
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
import com.skintracker.data.notifications.NotificationHelper
import com.skintracker.data.photo.clearStaleCaptures
import com.skintracker.model.AppViewModel
import com.skintracker.ui.screens.WizardScreen
import com.skintracker.ui.theme.KetoTheme
import com.skintracker.ui.theme.KetoTracker
import com.skintracker.ui.theme.resolveAutoTheme
import com.skintracker.widget.FlareWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val vm: AppViewModel by viewModels { AppViewModel.factory(application) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Register the notification channel early — safe to call on every start since
        // Android ignores it once the channel already exists.
        NotificationHelper.createChannel(applicationContext)

        // Best-effort sweep of any temp camera-capture files a previous
        // session left behind (e.g. process death between the camera writing
        // the file and PhotoStore deleting it — see CameraCapture.kt).
        lifecycleScope.launch(Dispatchers.IO) { clearStaleCaptures(applicationContext) }

        // Handle a cold start launched by the home-screen flare widget.
        handleIntent(intent)

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

    // The widget launches with SINGLE_TOP, so when the app is already running a
    // tap arrives here rather than through onCreate.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    /** Opens the flare-entry sheet when launched from the home-screen widget. */
    private fun handleIntent(intent: Intent?) {
        if (intent?.action == FlareWidgetProvider.ACTION_LOG_FLARE) {
            vm.requestFlareEntry()
        }
    }
}
