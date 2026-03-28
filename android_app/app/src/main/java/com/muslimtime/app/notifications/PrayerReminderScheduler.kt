package com.muslimtime.app.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import com.muslimtime.app.data.CityPrayerTimes
import com.muslimtime.app.data.PrayerPreferences
import java.util.Calendar

object PrayerReminderScheduler {
    private val mainRequestCodes = listOf(7, 8, 9, 10, 11, 12)
    private val preRequestCodes = listOf(107, 108, 109, 110, 111, 112)
    private val repeatRequestCodes = listOf(207, 208, 209, 210, 211, 212)
    private val delayedNotificationRequestCodes = listOf(307, 308, 309, 310, 311, 312)
    private const val MAX_REPEAT_COUNT = 3

    fun scheduleReminderSet(context: Context, prayerTimes: CityPrayerTimes) {
        cancelAll(context)

        if (!PrayerPreferences.areRemindersGloballyEnabled(context)) return

        prayerTimes.times.forEachIndexed { index, prayerTime ->
            if (index <= 5 && PrayerPreferences.isReminderEnabled(context, index)) {
                schedulePrayer(context, prayerTime.name, index, prayerTime.time)
            }
        }
    }

    fun scheduleNextForPrayerId(context: Context, prayerId: Int, forceTomorrow: Boolean = false) {
        if (!PrayerPreferences.areRemindersGloballyEnabled(context)) return
        val prayerTimes = PrayerPreferences.loadSelectedPrayerTimes(context) ?: return
        val prayerIndex = prayerIndexFromAnyId(prayerId)
        if (prayerIndex !in prayerTimes.times.indices) return
        if (!PrayerPreferences.isReminderEnabled(context, prayerIndex)) return

        cancelReminder(context, mainRequestCodes[prayerIndex])
        cancelReminder(context, preRequestCodes[prayerIndex])
        cancelReminder(context, repeatRequestCodes[prayerIndex])
        cancelReminder(context, delayedNotificationRequestCodes[prayerIndex])

        val prayerTime = prayerTimes.times[prayerIndex]
        schedulePrayer(context, prayerTime.name, prayerIndex, prayerTime.time, forceTomorrow)
    }

    fun scheduleRepeatReminder(
        context: Context,
        prayerName: String,
        prayerId: Int,
        repeatCount: Int,
    ) {
        val interval = PrayerPreferences.getRepeatReminderMinutes(context)
        if (interval <= 0 || repeatCount >= MAX_REPEAT_COUNT) return
        val prayerIndex = prayerIndexFromMainId(prayerId)
        if (prayerIndex == -1) return
        val requestCode = repeatRequestCodes[prayerIndex]
        cancelReminder(context, requestCode)

        val triggerAt = System.currentTimeMillis() + interval * 60_000L
        val repeatIntent = buildReceiverIntent(
            context = context,
            prayerName = prayerName,
            prayerId = prayerId,
            requestCode = requestCode,
            kind = PrayerReminderReceiver.KIND_REPEAT,
            leadMinutes = 0,
            repeatCount = repeatCount,
        )
        scheduleExactAt(context, requestCode, repeatIntent, triggerAt)
    }

    fun cancelAll(context: Context) {
        (mainRequestCodes + preRequestCodes + repeatRequestCodes + delayedNotificationRequestCodes).forEach {
            cancelReminder(context, it)
        }
    }

    fun scheduleDelayedNotification(
        context: Context,
        prayerName: String,
        prayerId: Int,
        delayMillis: Long,
    ) {
        val prayerIndex = prayerIndexFromAnyId(prayerId)
        if (prayerIndex == -1) return
        val requestCode = delayedNotificationRequestCodes[prayerIndex]
        cancelReminder(context, requestCode)
        val intent = buildReceiverIntent(
            context = context,
            prayerName = prayerName,
            prayerId = prayerId,
            requestCode = requestCode,
            kind = PrayerReminderReceiver.KIND_DELAYED_NOTIFICATION,
        )
        scheduleExactAt(context, requestCode, intent, System.currentTimeMillis() + delayMillis)
    }

    private fun schedulePrayer(
        context: Context,
        label: String,
        prayerIndex: Int,
        time: String,
        forceTomorrow: Boolean = false,
    ) {
        val mainRequestCode = mainRequestCodes[prayerIndex]
        val eventMoment = buildPrayerMoment(time, forceTomorrow) ?: return

        val mainIntent = buildReceiverIntent(
            context = context,
            prayerName = label,
            prayerId = mainRequestCode,
            requestCode = mainRequestCode,
            kind = PrayerReminderReceiver.KIND_MAIN,
        )
        scheduleExactAt(context, mainRequestCode, mainIntent, eventMoment.timeInMillis)

        val leadMinutes = PrayerPreferences.getPreReminderMinutes(context)
        if (leadMinutes > 0) {
            val preMoment = Calendar.getInstance().apply { timeInMillis = eventMoment.timeInMillis }
            preMoment.add(Calendar.MINUTE, -leadMinutes)
            if (forceTomorrow || preMoment.timeInMillis > System.currentTimeMillis()) {
                val preRequestCode = preRequestCodes[prayerIndex]
                val preIntent = buildReceiverIntent(
                    context = context,
                    prayerName = label,
                    prayerId = mainRequestCode,
                    requestCode = preRequestCode,
                    kind = PrayerReminderReceiver.KIND_PRE,
                    leadMinutes = leadMinutes,
                )
                scheduleExactAt(context, preRequestCode, preIntent, preMoment.timeInMillis)
            }
        }
    }

    private fun buildPrayerMoment(time: String, forceTomorrow: Boolean): Calendar? {
        val hour = time.substringBefore(":").toIntOrNull() ?: return null
        val minute = time.substringAfter(":", "0").take(2).toIntOrNull() ?: return null
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (forceTomorrow || timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
    }

    private fun buildReceiverIntent(
        context: Context,
        prayerName: String,
        prayerId: Int,
        requestCode: Int,
        kind: String,
        leadMinutes: Int = 0,
        repeatCount: Int = 0,
    ): Intent {
        return Intent(context, PrayerReminderReceiver::class.java).apply {
            putExtra(PrayerReminderReceiver.EXTRA_PRAYER, prayerName)
            putExtra(PrayerReminderReceiver.EXTRA_ID, prayerId)
            putExtra(PrayerReminderReceiver.EXTRA_REQUEST_CODE, requestCode)
            putExtra(PrayerReminderReceiver.EXTRA_KIND, kind)
            putExtra(PrayerReminderReceiver.EXTRA_LEAD_MINUTES, leadMinutes)
            putExtra(PrayerReminderReceiver.EXTRA_REPEAT_COUNT, repeatCount)
        }
    }

    private fun scheduleExactAt(
        context: Context,
        requestCode: Int,
        intent: Intent,
        triggerAtMillis: Long,
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE,
        )

        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S &&
                    !alarmManager.canScheduleExactAlarms()
                ) {
                    val infoIntent = PendingIntent.getActivity(
                        context,
                        9000 + requestCode,
                        Intent(context, com.muslimtime.app.ui.MainActivity::class.java),
                        FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE,
                    )
                    val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerAtMillis, infoIntent)
                    alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
                } else {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent,
                    )
                }
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } catch (_: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    private fun prayerIndexFromMainId(prayerId: Int): Int = mainRequestCodes.indexOf(prayerId)

    fun prayerIndexFromAnyId(prayerId: Int): Int {
        return prayerIndexFromMainId(prayerId).takeIf { it != -1 }
            ?: preRequestCodes.indexOf(prayerId).takeIf { it != -1 }
            ?: repeatRequestCodes.indexOf(prayerId).takeIf { it != -1 }
            ?: delayedNotificationRequestCodes.indexOf(prayerId)
    }

    private fun cancelReminder(context: Context, reqCode: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, PrayerReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reqCode,
            intent,
            FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE,
        )
        alarmManager.cancel(pendingIntent)
    }
}
