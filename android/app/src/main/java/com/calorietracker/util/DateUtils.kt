package com.calorietracker.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {
    private val dayOfWeekFmt = SimpleDateFormat("EEE", Locale.ENGLISH)
    private val monthDayFmt = SimpleDateFormat("MMM d", Locale.ENGLISH)
    private val fullDateFmt = SimpleDateFormat("EEE, MMM d", Locale.ENGLISH)
    private val planKeyFmt = SimpleDateFormat("EEE MMM dd yyyy", Locale.ENGLISH)
    private val shortDateFmt = SimpleDateFormat("d MMM", Locale.ENGLISH)

    fun formatPlanKey(millis: Long): String = planKeyFmt.format(Date(millis))

    fun formatDisplayDate(millis: Long): String {
        val cal = Calendar.getInstance()
        val today = cal.clone() as Calendar
        cal.timeInMillis = millis
        return if (isSameDay(cal, today)) "Today"
        else fullDateFmt.format(Date(millis))
    }

    fun formatShortDate(millis: Long): String = shortDateFmt.format(Date(millis))

    fun formatDayOfWeek(millis: Long): String = dayOfWeekFmt.format(Date(millis))

    fun formatMonthDay(millis: Long): String = monthDayFmt.format(Date(millis))

    fun startOfDay(millis: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = millis
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun addDays(millis: Long, days: Int): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = millis
        cal.add(Calendar.DAY_OF_MONTH, days)
        return cal.timeInMillis
    }

    fun isSameDay(millis1: Long, millis2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = millis1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = millis2 }
        return isSameDay(cal1, cal2)
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean =
        cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)

    fun calculateStreak(logDates: List<Long>): Int {
        if (logDates.isEmpty()) return 0
        val today = startOfDay(System.currentTimeMillis())
        val uniqueDays = logDates.map { startOfDay(it) }.toSortedSet().reversed()
        var streak = 0
        var expected = today
        for (day in uniqueDays) {
            if (day == expected) {
                streak++
                expected = addDays(expected, -1)
            } else if (day < expected) {
                break
            }
        }
        return streak
    }

    fun inferMealType(timestamp: Long): String {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        return when (cal.get(Calendar.HOUR_OF_DAY)) {
            in 5..10 -> "Breakfast"
            in 11..15 -> "Lunch"
            in 16..21 -> "Dinner"
            else -> "Snacks"
        }
    }
}
