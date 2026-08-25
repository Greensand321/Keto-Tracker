package com.ketotracker.work

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import java.time.Duration
import java.time.LocalDateTime

/**
 * Schedules the daily reminder via [AlarmManager] instead of a periodic
 * WorkManager job. WorkManager's `PeriodicWorkRequest` is deliberately
 * inexact — the OS batches and defers it under Doze/battery optimization, so
 * the fire time can drift by anywhere from minutes to hours from the chosen
 * hour, and that drift was the root cause of reminders feeling "random."
 * `setExactAndAllowWhileIdle` fires at (near-)the exact requested time even
 * in Doze, at the cost of needing to be re-armed after each fire (handled by
 * [ReminderReceiver] rescheduling itself for the next day) and after a
 * reboot (handled by [BootReceiver], since exact alarms don't survive one).
 */
object ReminderScheduler {

    const val EXTRA_HOUR = "hour"
    const val EXTRA_MINUTE = "minute"
    private const val REQUEST_CODE = 2001

    /** Schedules (or reschedules) the daily reminder for the next occurrence of [hour]:[minute] local time. */
    fun schedule(context: Context, hour: Int, minute: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val now = LocalDateTime.now()
        var target = now.toLocalDate().atTime(hour, minute)
        if (!target.isAfter(now)) target = target.plusDays(1)
        val triggerAtMillis = System.currentTimeMillis() + Duration.between(now, target).toMillis()

        val pendingIntent = reminderPendingIntent(context, hour, minute)
        if (canScheduleExactAlarms(context)) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            // No exact-alarm permission (or not required pre-S) — still Doze-aware,
            // just without the precision guarantee. Far better than a 1-day-period
            // WorkManager job, which can drift by hours.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        // Hour/minute don't matter for cancellation — the PendingIntent is matched by
        // request code + action, not by extras.
        alarmManager.cancel(reminderPendingIntent(context, 0, 0))
    }

    /** Whether this app can currently schedule exact alarms. Always true below API 31. */
    fun canScheduleExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return alarmManager.canScheduleExactAlarms()
    }

    /** Opens the system "Alarms & reminders" page so the user can grant exact-alarm access. */
    fun requestExactAlarmPermission(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        context.startActivity(intent)
    }

    private fun reminderPendingIntent(context: Context, hour: Int, minute: Int): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(EXTRA_HOUR, hour)
            putExtra(EXTRA_MINUTE, minute)
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
