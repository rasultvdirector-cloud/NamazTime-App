package com.muslimtime.app.ui

import android.content.Context
import com.muslimtime.app.data.AppLocation
import com.muslimtime.app.data.CityPrayerTimes
import com.muslimtime.app.data.PrayerDataSyncManager
import com.muslimtime.app.data.PrayerPreferences
import com.muslimtime.app.data.PrayerTimesRefreshWorker
import com.muslimtime.app.notifications.PrayerRefreshScheduler

internal data class LocationSettingsSelection(
    val autoEnabled: Boolean,
    val manualLocation: AppLocation?,
)

internal fun persistLocationSelection(
    context: Context,
    selection: LocationSettingsSelection,
    currentPrayerTimes: CityPrayerTimes?,
    scheduleReminderSet: (CityPrayerTimes) -> Unit,
) {
    PrayerPreferences.setAutoLocationEnabled(context, selection.autoEnabled)
    if (!selection.autoEnabled) {
        selection.manualLocation?.let { location ->
            PrayerPreferences.saveLocation(context, location, isAutoDetected = false)
            PrayerPreferences.setLocationSetupCompleted(context, true)
        }
    } else {
        PrayerPreferences.setLocationSetupCompleted(context, true)
    }

    val jumaaEnabled = PrayerPreferences.areJumaaNotificationsEnabled(context)
    if (jumaaEnabled) {
        PrayerRefreshScheduler.scheduleNextJumaaReminder(context)
    } else {
        PrayerRefreshScheduler.cancelJumaaReminder(context)
    }

    if (PrayerDataSyncManager.shouldUsePeriodicWorker(context)) {
        PrayerTimesRefreshWorker.schedule(context)
    } else {
        PrayerTimesRefreshWorker.cancel(context)
    }

    if (PrayerDataSyncManager.shouldUseDailyRefreshAlarm(context)) {
        PrayerRefreshScheduler.scheduleNextDailyRefresh(context)
    }

    currentPrayerTimes?.let(scheduleReminderSet)
}
