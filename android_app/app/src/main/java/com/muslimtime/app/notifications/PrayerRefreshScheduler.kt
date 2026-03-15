package com.muslimtime.app.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

object PrayerRefreshScheduler {
    private const val REQUEST_CODE = 7800
    private const val JUMAA_REQUEST_CODE = 7801
    private const val BIRTHDAY_REQUEST_CODE = 7802

    fun scheduleNextDailyRefresh(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, PrayerDailyRefreshReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val triggerTime = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 5)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S &&
                    !alarmManager.canScheduleExactAlarms()
                ) {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime.timeInMillis,
                        pendingIntent,
                    )
                } else {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime.timeInMillis,
                        pendingIntent,
                    )
                }
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime.timeInMillis, pendingIntent)
            }
        } catch (_: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime.timeInMillis, pendingIntent)
        }
    }

    fun scheduleNextJumaaReminder(context: Context) {
        if (!com.muslimtime.app.data.PrayerPreferences.areJumaaNotificationsEnabled(context)) {
            cancelJumaaReminder(context)
            return
        }
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, JumaaReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            JUMAA_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val triggerTime = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            val fajrTime = com.muslimtime.app.data.PrayerPreferences.loadSelectedPrayerTimes(context)
                ?.times
                ?.firstOrNull()
                ?.takeIf { it.time.contains(":") }
                ?.time
            val hour = fajrTime?.substringBefore(":")?.toIntOrNull() ?: 6
            val minute = fajrTime?.substringAfter(":", "0")?.take(2)?.toIntOrNull() ?: 0
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)

            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.WEEK_OF_YEAR, 1)
            }
        }

        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S &&
                    !alarmManager.canScheduleExactAlarms()
                ) {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime.timeInMillis,
                        pendingIntent,
                    )
                } else {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime.timeInMillis,
                        pendingIntent,
                    )
                }
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime.timeInMillis, pendingIntent)
            }
        } catch (_: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime.timeInMillis, pendingIntent)
        }
    }

    fun cancelJumaaReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, JumaaReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            JUMAA_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    fun scheduleNextBirthdayReminder(context: Context) {
        if (!com.muslimtime.app.data.PrayerPreferences.hasBirthDate(context)) {
            cancelBirthdayReminder(context)
            return
        }

        val day = com.muslimtime.app.data.PrayerPreferences.getBirthDay(context) ?: run {
            cancelBirthdayReminder(context)
            return
        }
        val month = com.muslimtime.app.data.PrayerPreferences.getBirthMonth(context) ?: run {
            cancelBirthdayReminder(context)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, BirthdayReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            BIRTHDAY_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val triggerTime = Calendar.getInstance().apply {
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.YEAR, 1)
            }
        }

        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S &&
                    !alarmManager.canScheduleExactAlarms()
                ) {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime.timeInMillis,
                        pendingIntent,
                    )
                } else {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime.timeInMillis,
                        pendingIntent,
                    )
                }
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime.timeInMillis, pendingIntent)
            }
        } catch (_: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime.timeInMillis, pendingIntent)
        }
    }

    fun cancelBirthdayReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, BirthdayReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            BIRTHDAY_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }
}
