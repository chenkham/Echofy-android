package com.Chenkham.Echofy.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.Chenkham.Echofy.playback.MusicService
import timber.log.Timber
import java.util.Calendar

class SmartAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        Timber.d("SmartAlarmReceiver triggered - waking up with gentle music ramp")
        val serviceIntent =
            Intent(context, MusicService::class.java).apply {
                action = "com.Chenkham.Echofy.action.ALARM_WAKE"
            }
        context.startForegroundService(serviceIntent)
    }
}

object SmartAlarmManager {
    fun setAlarm(
        context: Context,
        hour: Int,
        minute: Int,
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val calendar =
            Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                if (before(Calendar.getInstance())) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

        val intent = Intent(context, SmartAlarmReceiver::class.java)
        val pendingIntent =
            PendingIntent.getBroadcast(
                context,
                1001,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
            Timber.w("SmartAlarm: exact alarm permission not granted, fallback to inexact")
            return
        }

        val alarmClockInfo = AlarmManager.AlarmClockInfo(calendar.timeInMillis, pendingIntent)
        alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
        Timber.d("SmartAlarm set for ${calendar.time}")
    }

    fun cancelAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, SmartAlarmReceiver::class.java)
        val pendingIntent =
            PendingIntent.getBroadcast(
                context,
                1001,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        alarmManager.cancel(pendingIntent)
        Timber.d("SmartAlarm cancelled")
    }
}
