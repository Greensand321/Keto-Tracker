package com.ketotracker.data

import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * Date helpers mirroring todayKey()/fmtDate()/offKey() from index.html, using
 * java.time. Day keys are ISO `YYYY-MM-DD` strings, same as localStorage keys.
 */
object DateUtils {
    fun todayKey(): String = LocalDate.now().toString()

    fun offKey(key: String, days: Long): String =
        LocalDate.parse(key).plusDays(days).toString()

    /** "Mon, Jan 15" — same format as the web fmtDate(). */
    fun fmtDate(key: String): String {
        val d = LocalDate.parse(key)
        val dow = d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
        val mon = d.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
        return "$dow, $mon ${d.dayOfMonth}"
    }

    fun isToday(key: String): Boolean = key == todayKey()
    fun isFuture(key: String): Boolean = key > todayKey()
}
