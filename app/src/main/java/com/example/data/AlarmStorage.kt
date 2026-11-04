package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.AlarmItem
import org.json.JSONArray
import org.json.JSONObject

class AlarmStorage(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("alarms_prefs", Context.MODE_PRIVATE)

    fun getAlarms(): List<AlarmItem> {
        val jsonString = prefs.getString(KEY_ALARMS, null) ?: return defaultAlarms()
        return try {
            val array = JSONArray(jsonString)
            val list = mutableListOf<AlarmItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val daysArray = obj.optJSONArray("daysOfWeek")
                val daysSet = mutableSetOf<Int>()
                if (daysArray != null) {
                    for (d in 0 until daysArray.length()) {
                        daysSet.add(daysArray.getInt(d))
                    }
                }
                list.add(
                    AlarmItem(
                        id = obj.optLong("id", System.currentTimeMillis()),
                        hour = obj.getInt("hour"),
                        minute = obj.getInt("minute"),
                        label = obj.optString("label", "منبه"),
                        isEnabled = obj.optBoolean("isEnabled", true),
                        daysOfWeek = daysSet,
                        vibrate = obj.optBoolean("vibrate", true)
                    )
                )
            }
            list.sortedWith(compareBy({ it.hour }, { it.minute }))
        } catch (e: Exception) {
            defaultAlarms()
        }
    }

    fun saveAlarms(alarms: List<AlarmItem>) {
        val array = JSONArray()
        for (alarm in alarms) {
            val obj = JSONObject().apply {
                put("id", alarm.id)
                put("hour", alarm.hour)
                put("minute", alarm.minute)
                put("label", alarm.label)
                put("isEnabled", alarm.isEnabled)
                put("vibrate", alarm.vibrate)
                val daysArr = JSONArray()
                alarm.daysOfWeek.forEach { daysArr.put(it) }
                put("daysOfWeek", daysArr)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_ALARMS, array.toString()).apply()
    }

    private fun defaultAlarms(): List<AlarmItem> {
        val list = listOf(
            AlarmItem(
                id = 1L,
                hour = 6,
                minute = 30,
                label = "استيقاظ الصباح",
                isEnabled = true,
                daysOfWeek = emptySet(),
                vibrate = true
            ),
            AlarmItem(
                id = 2L,
                hour = 8,
                minute = 0,
                label = "موعد العمل",
                isEnabled = false,
                daysOfWeek = emptySet(),
                vibrate = true
            )
        )
        saveAlarms(list)
        return list
    }

    companion object {
        private const val KEY_ALARMS = "saved_alarms_list"
    }
}
