package com.example.model

import java.util.Calendar
import java.util.Locale

data class AlarmItem(
    val id: Long = System.currentTimeMillis(),
    val hour: Int,
    val minute: Int,
    val label: String = "منبه",
    val isEnabled: Boolean = true,
    val daysOfWeek: Set<Int> = emptySet(), // Calendar.SUNDAY .. Calendar.SATURDAY
    val vibrate: Boolean = true
) {
    val isRepeating: Boolean
        get() = daysOfWeek.isNotEmpty()

    fun formattedTime(is24Hour: Boolean = false): String {
        return if (is24Hour) {
            String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
        } else {
            val displayHour = when {
                hour == 0 -> 12
                hour > 12 -> hour - 12
                else -> hour
            }
            String.format(Locale.getDefault(), "%02d:%02d", displayHour, minute)
        }
    }

    fun amPmText(): String {
        return if (hour >= 12) "م" else "ص"
    }

    fun amPmFullText(): String {
        return if (hour >= 12) "مساءً" else "صباحاً"
    }

    fun daysSummaryArabic(): String {
        if (daysOfWeek.isEmpty()) return "مرة واحدة"
        if (daysOfWeek.size == 7) return "كل يوم"
        
        val weekdays = setOf(Calendar.SUNDAY, Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY)
        val weekends = setOf(Calendar.FRIDAY, Calendar.SATURDAY)
        
        if (daysOfWeek == weekdays) return "أيام العمل (الأحد - الخميس)"
        if (daysOfWeek == weekends) return "عطلة نهاية الأسبوع (الجمعة والسبت)"

        val names = mapOf(
            Calendar.SATURDAY to "السبت",
            Calendar.SUNDAY to "الأحد",
            Calendar.MONDAY to "الإثنين",
            Calendar.TUESDAY to "الثلاثاء",
            Calendar.WEDNESDAY to "الأربعاء",
            Calendar.THURSDAY to "الخميس",
            Calendar.FRIDAY to "الجمعة"
        )
        return daysOfWeek.sorted().mapNotNull { names[it] }.joinToString("، ")
    }

    fun calculateNextTriggerMillis(): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (daysOfWeek.isEmpty()) {
            if (target.timeInMillis <= now.timeInMillis) {
                target.add(Calendar.DAY_OF_YEAR, 1)
            }
            return target.timeInMillis
        }

        // Repeating on specific days of week
        for (i in 0..7) {
            val checkCal = Calendar.getInstance().apply {
                timeInMillis = target.timeInMillis
                add(Calendar.DAY_OF_YEAR, i)
            }
            val dayOfWeek = checkCal.get(Calendar.DAY_OF_WEEK)
            if (daysOfWeek.contains(dayOfWeek)) {
                if (checkCal.timeInMillis > now.timeInMillis) {
                    return checkCal.timeInMillis
                }
            }
        }

        // Fallback next week
        target.add(Calendar.DAY_OF_YEAR, 7)
        return target.timeInMillis
    }
}
