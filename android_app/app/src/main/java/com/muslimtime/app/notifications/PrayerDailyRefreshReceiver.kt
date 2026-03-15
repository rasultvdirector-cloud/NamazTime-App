package com.muslimtime.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.muslimtime.app.data.PrayerPreferences
import com.muslimtime.app.data.PrayerTimesRefreshWorker

class PrayerDailyRefreshReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        PrayerTimesRefreshWorker.enqueueImmediate(context)
        val location = PrayerPreferences.loadSelectedLocation(context)
        if (location != null) {
            PrayerRefreshScheduler.scheduleNextDailyRefresh(context)
        }
    }
}
