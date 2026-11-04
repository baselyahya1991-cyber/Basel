package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AlarmStorage
import com.example.model.AlarmItem
import com.example.service.AlarmRingtoneService
import com.example.util.AlarmScheduler
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

class AlarmViewModel(application: Application) : AndroidViewModel(application) {

    private val storage = AlarmStorage(application)

    private val _alarms = MutableStateFlow<List<AlarmItem>>(emptyList())
    val alarms: StateFlow<List<AlarmItem>> = _alarms.asStateFlow()

    private val _currentTime = MutableStateFlow(Calendar.getInstance())
    val currentTime: StateFlow<Calendar> = _currentTime.asStateFlow()

    val ringingState: StateFlow<AlarmRingtoneService.Companion.RingingState> =
        AlarmRingtoneService.ringingAlarmState

    // Formatted time strings for live clock
    val formattedCurrentTime: StateFlow<String> = _currentTime.combine(_alarms) { cal, _ ->
        val hour24 = cal.get(Calendar.HOUR_OF_DAY)
        val hour12 = when {
            hour24 == 0 -> 12
            hour24 > 12 -> hour24 - 12
            else -> hour24
        }
        val min = cal.get(Calendar.MINUTE)
        val sec = cal.get(Calendar.SECOND)
        String.format(Locale.getDefault(), "%02d:%02d:%02d", hour12, min, sec)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "00:00:00")

    val currentAmPm: StateFlow<String> = _currentTime.combine(_alarms) { cal, _ ->
        if (cal.get(Calendar.HOUR_OF_DAY) >= 12) "م" else "ص"
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "ص")

    val currentDateFormatted: StateFlow<String> = _currentTime.combine(_alarms) { cal, _ ->
        val dayNames = arrayOf("الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت")
        val monthNames = arrayOf(
            "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
            "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
        )
        val dayOfWeek = dayNames[(cal.get(Calendar.DAY_OF_WEEK) - 1).coerceIn(0, 6)]
        val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
        val month = monthNames[cal.get(Calendar.MONTH).coerceIn(0, 11)]
        "$dayOfWeek، $dayOfMonth $month"
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val nextAlarmCountdown: StateFlow<String> = _currentTime.combine(_alarms) { cal, alarmList ->
        val activeAlarms = alarmList.filter { it.isEnabled }
        if (activeAlarms.isEmpty()) {
            return@combine "لا يوجد منبه مفعّل حالياً"
        }

        var minTrigger = Long.MAX_VALUE
        for (a in activeAlarms) {
            val t = a.calculateNextTriggerMillis()
            if (t in (cal.timeInMillis + 1)..<minTrigger) {
                minTrigger = t
            }
        }

        if (minTrigger == Long.MAX_VALUE) {
            return@combine "لا يوجد منبه قادم"
        }

        val diffMillis = minTrigger - cal.timeInMillis
        val diffMinutes = (diffMillis / (1000 * 60)).toInt()
        val hours = diffMinutes / 60
        val mins = diffMinutes % 60

        when {
            hours > 0 && mins > 0 -> "المنبه التالي خلال: $hours ساعة و $mins دقيقة"
            hours > 0 -> "المنبه التالي خلال: $hours ساعة"
            mins > 0 -> "المنبه التالي خلال: $mins دقيقة"
            else -> "المنبه سيرن قريباً جداً"
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    init {
        loadAlarms()
        startClockTicker()
    }

    private fun loadAlarms() {
        val loaded = storage.getAlarms()
        _alarms.value = loaded
    }

    private fun startClockTicker() {
        viewModelScope.launch {
            while (true) {
                _currentTime.value = Calendar.getInstance()
                delay(1000)
            }
        }
    }

    fun addOrUpdateAlarm(alarm: AlarmItem) {
        val currentList = _alarms.value.toMutableList()
        val existingIndex = currentList.indexOfFirst { it.id == alarm.id }
        if (existingIndex >= 0) {
            currentList[existingIndex] = alarm
        } else {
            currentList.add(alarm)
        }
        val sorted = currentList.sortedWith(compareBy({ it.hour }, { it.minute }))
        _alarms.value = sorted
        storage.saveAlarms(sorted)
        AlarmScheduler.scheduleAlarm(getApplication(), alarm)
    }

    fun deleteAlarm(alarm: AlarmItem) {
        val updated = _alarms.value.filter { it.id != alarm.id }
        _alarms.value = updated
        storage.saveAlarms(updated)
        AlarmScheduler.cancelAlarm(getApplication(), alarm)
    }

    fun toggleAlarm(alarm: AlarmItem, enabled: Boolean) {
        val updatedAlarm = alarm.copy(isEnabled = enabled)
        val updatedList = _alarms.value.map {
            if (it.id == alarm.id) updatedAlarm else it
        }
        _alarms.value = updatedList
        storage.saveAlarms(updatedList)

        if (enabled) {
            AlarmScheduler.scheduleAlarm(getApplication(), updatedAlarm)
        } else {
            AlarmScheduler.cancelAlarm(getApplication(), updatedAlarm)
        }
    }

    fun addQuickNap(minutes: Int) {
        val cal = Calendar.getInstance().apply {
            add(Calendar.MINUTE, minutes)
        }
        val quickAlarm = AlarmItem(
            id = System.currentTimeMillis(),
            hour = cal.get(Calendar.HOUR_OF_DAY),
            minute = cal.get(Calendar.MINUTE),
            label = "قيلولة ($minutes دقيقة)",
            isEnabled = true,
            daysOfWeek = emptySet(),
            vibrate = true
        )
        addOrUpdateAlarm(quickAlarm)
    }

    fun dismissRingingAlarm() {
        val ringing = _ringingAlarmState.value
        AlarmRingtoneService.stopService(getApplication())
        // If it was a non-repeating alarm, disable it
        if (ringing.alarmId != 0L) {
            val alarm = _alarms.value.find { it.id == ringing.alarmId }
            if (alarm != null && !alarm.isRepeating) {
                toggleAlarm(alarm, false)
            }
        }
    }

    fun snoozeRingingAlarm(minutes: Int = 5) {
        val ringing = _ringingAlarmState.value
        val label = if (ringing.label.isNotEmpty()) ringing.label else "منبه"
        AlarmRingtoneService.stopService(getApplication())
        AlarmScheduler.scheduleSnooze(getApplication(), ringing.alarmId, label, vibrate = true, minutes = minutes)
    }

    private val _ringingAlarmState = AlarmRingtoneService.ringingAlarmState
}
