package com.muslimtime.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.muslimtime.app.data.PrayerDataSyncManager
import com.muslimtime.app.data.PrayerPreferences
import com.muslimtime.app.data.ReminderDiagnosticsStore
import com.muslimtime.app.data.PrayerTimesRefreshWorker

class PrayerBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        ReminderDiagnosticsStore.record(context, "reschedule_trigger", "action=$action")

        if (PrayerDataSyncManager.needsSync(context)) {
            PrayerTimesRefreshWorker.enqueueImmediate(context)
        }
        if (PrayerDataSyncManager.shouldUseDailyRefreshAlarm(context)) {
            PrayerRefreshScheduler.scheduleNextDailyRefresh(context)
        }
        PrayerRefreshScheduler.scheduleNextJumaaReminder(context)
        PrayerRefreshScheduler.scheduleNextBirthdayReminder(context)
        PrayerPreferences.loadSelectedPrayerTimes(context)?.let {
            PrayerReminderScheduler.scheduleReminderSet(context, it)
        }
    }
}
