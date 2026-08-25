package com.ketotracker.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ketotracker.data.DateUtils
import com.ketotracker.data.db.KetoDatabase
import com.ketotracker.data.notifications.NotificationHelper
import com.ketotracker.data.notifications.ReminderMessages
import com.ketotracker.data.prefs.PrefsStore
import com.ketotracker.data.repository.DayRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Fires when [ReminderScheduler]'s exact alarm goes off. Posts the reminder
 * (unless the log is already complete, or notifications were disabled after
 * the alarm was armed) and always re-arms the next day's alarm before
 * finishing, since `AlarmManager` alarms are one-shot.
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val hour = intent.getIntExtra(ReminderScheduler.EXTRA_HOUR, 20)
        val minute = intent.getIntExtra(ReminderScheduler.EXTRA_MINUTE, 0)
        val appContext = context.applicationContext

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = PrefsStore(appContext)
                if (prefs.notificationsEnabled.first()) {
                    maybeShowReminder(appContext)
                    // Re-read the current hour/minute in case they changed since this
                    // alarm was armed, then re-arm for the next occurrence.
                    val nextHour = prefs.notificationHour.first()
                    val nextMinute = prefs.notificationMinute.first()
                    ReminderScheduler.schedule(appContext, nextHour, nextMinute)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun maybeShowReminder(context: Context) {
        val repo = DayRepository(KetoDatabase.get(context).dayEntryDao())
        val entry = repo.load(DateUtils.todayKey())

        val breakfastDone = entry.breakfast.isNotEmpty() || entry.breakfastKeto
        val lunchDone = entry.lunch.isNotEmpty() || entry.lunchKeto
        val dinnerDone = entry.dinner.isNotEmpty() || entry.dinnerKeto

        // All three meals logged — user is already on top of it. Skip the notification
        // so we never congratulate-nag someone who is already done for the day.
        if (breakfastDone && lunchDone && dinnerDone) return

        val body = when {
            !breakfastDone && !lunchDone && !dinnerDone -> ReminderMessages.forNothingLogged()
            !dinnerDone -> ReminderMessages.forDinnerMissing()
            !lunchDone -> ReminderMessages.forLunchMissing()
            else -> ReminderMessages.forBreakfastMissing()
        }

        NotificationHelper.showReminder(context, body)
    }
}
