package com.example

import com.example.model.AlarmItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ExampleUnitTest {
    @Test
    fun alarmItem_formattedTime_returnsCorrect12Hour() {
        val alarmMorning = AlarmItem(hour = 7, minute = 30)
        assertEquals("07:30", alarmMorning.formattedTime())
        assertEquals("ص", alarmMorning.amPmText())

        val alarmEvening = AlarmItem(hour = 19, minute = 45)
        assertEquals("07:45", alarmEvening.formattedTime())
        assertEquals("م", alarmEvening.amPmText())

        val alarmMidnight = AlarmItem(hour = 0, minute = 0)
        assertEquals("12:00", alarmMidnight.formattedTime())
        assertEquals("ص", alarmMidnight.amPmText())

        val alarmNoon = AlarmItem(hour = 12, minute = 15)
        assertEquals("12:15", alarmNoon.formattedTime())
        assertEquals("م", alarmNoon.amPmText())
    }

    @Test
    fun alarmItem_daysSummary_arabicText() {
        val singleAlarm = AlarmItem(hour = 8, minute = 0, daysOfWeek = emptySet())
        assertEquals("مرة واحدة", singleAlarm.daysSummaryArabic())
        assertFalse(singleAlarm.isRepeating)

        val dailyAlarm = AlarmItem(
            hour = 6,
            minute = 0,
            daysOfWeek = setOf(
                Calendar.SUNDAY,
                Calendar.MONDAY,
                Calendar.TUESDAY,
                Calendar.WEDNESDAY,
                Calendar.THURSDAY,
                Calendar.FRIDAY,
                Calendar.SATURDAY
            )
        )
        assertEquals("كل يوم", dailyAlarm.daysSummaryArabic())
        assertTrue(dailyAlarm.isRepeating)
    }
}
