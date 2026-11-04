package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.AlarmStorage
import com.example.service.AlarmRingtoneService
import com.example.util.AlarmScheduler

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return

        when (action) {
            ACTION_FIRE_ALARM -> {
                handleAlarmFire(context, intent)
            }
            ACTION_DISMISS_ALARM -> {
                handleDismiss(context, intent)
            }
            ACTION_SNOOZE_ALARM -> {
                handleSnooze(context, intent)
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                handleBootCompleted(context)
            }
        }
    }

    private fun handleAlarmFire(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, 0L)
        val label = intent.getStringExtra(EXTRA_ALARM_LABEL) ?: "منبه"
        val vibrate = intent.getBooleanExtra(EXTRA_ALARM_VIBRATE, true)

        // 1. Start Audio & Vibration Service
        val serviceIntent = Intent(context, AlarmRingtoneService::class.java).apply {
            action = AlarmRingtoneService.ACTION_START
            putExtra(AlarmRingtoneService.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmRingtoneService.EXTRA_LABEL, label)
            putExtra(AlarmRingtoneService.EXTRA_VIBRATE, vibrate)
        }
        try {
            context.startService(serviceIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Show Heads-up Notification
        createNotificationChannel(context)

        val fullScreenIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_ALARM_ID, alarmId)
            putExtra(EXTRA_IS_RINGING, true)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            alarmId.toInt(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_DISMISS_ALARM
            putExtra(EXTRA_ALARM_ID, alarmId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            (alarmId + 1000).toInt(),
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_SNOOZE_ALARM
            putExtra(EXTRA_ALARM_ID, alarmId)
            putExtra(EXTRA_ALARM_LABEL, label)
            putExtra(EXTRA_ALARM_VIBRATE, vibrate)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            (alarmId + 2000).toInt(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("⏰ $label")
            .setContentText("حان وقت المنبه!")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(false)
            .setOngoing(true)
            .setContentIntent(fullScreenPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "إيقاف", dismissPendingIntent)
            .addAction(android.R.drawable.ic_popup_sync, "غفوة 5 دقائق", snoozePendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(alarmId.toInt(), notification)

        // 3. Update storage if single-run alarm
        val storage = AlarmStorage(context)
        val alarms = storage.getAlarms()
        val alarm = alarms.find { it.id == alarmId }
        if (alarm != null) {
            if (!alarm.isRepeating) {
                val updated = alarms.map {
                    if (it.id == alarmId) it.copy(isEnabled = false) else it
                }
                storage.saveAlarms(updated)
            } else {
                // Reschedule for next repeating day
                AlarmScheduler.scheduleAlarm(context, alarm)
            }
        }
    }

    private fun handleDismiss(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, 0L)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.cancel(alarmId.toInt())
        AlarmRingtoneService.stopService(context)
    }

    private fun handleSnooze(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, 0L)
        val label = intent.getStringExtra(EXTRA_ALARM_LABEL) ?: "منبه"
        val vibrate = intent.getBooleanExtra(EXTRA_ALARM_VIBRATE, true)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.cancel(alarmId.toInt())
        AlarmRingtoneService.stopService(context)

        AlarmScheduler.scheduleSnooze(context, alarmId, label, vibrate, minutes = 5)
    }

    private fun handleBootCompleted(context: Context) {
        val storage = AlarmStorage(context)
        val alarms = storage.getAlarms()
        for (alarm in alarms) {
            if (alarm.isEnabled) {
                AlarmScheduler.scheduleAlarm(context, alarm)
            }
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "تنبيهات المنبه"
            val descriptionText = "قناة إشعارات المنبه عالية الأولوية"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    companion object {
        const val ACTION_FIRE_ALARM = "com.example.ACTION_FIRE_ALARM"
        const val ACTION_DISMISS_ALARM = "com.example.ACTION_DISMISS_ALARM"
        const val ACTION_SNOOZE_ALARM = "com.example.ACTION_SNOOZE_ALARM"

        const val EXTRA_ALARM_ID = "extra_alarm_id"
        const val EXTRA_ALARM_HOUR = "extra_alarm_hour"
        const val EXTRA_ALARM_MINUTE = "extra_alarm_minute"
        const val EXTRA_ALARM_LABEL = "extra_alarm_label"
        const val EXTRA_ALARM_VIBRATE = "extra_alarm_vibrate"
        const val EXTRA_IS_RINGING = "extra_is_ringing"

        const val CHANNEL_ID = "alarm_channel_high_priority"
    }
}
