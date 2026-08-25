package com.ketotracker.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ketotracker.data.prefs.PrefsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Re-arms the daily reminder alarm after a reboot. Exact `AlarmManager`
 * alarms — unlike WorkManager jobs — do not survive a device restart, so
 * without this the reminder would silently stop firing until the user
 * happened to reopen Settings.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val appContext = context.applicationContext

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = PrefsStore(appContext)
                if (prefs.notificationsEnabled.first()) {
                    val hour = prefs.notificationHour.first()
                    val minute = prefs.notificationMinute.first()
                    ReminderScheduler.schedule(appContext, hour, minute)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
