package com.muslimtime.app.ui

import android.content.Context
import com.muslimtime.app.data.CityPrayerTimes
import com.muslimtime.app.data.PrayerPreferences
import com.muslimtime.app.notifications.PrayerRefreshScheduler

internal data class ReminderSettingsSelection(
    val masterEnabled: Boolean,
    val jumaaEnabled: Boolean,
    val preReminderMinutes: Int,
    val reminderType: String,
    val repeatMinutes: Int,
    val showImsakIftarAllMonths: Boolean,
    val enabledStates: List<Boolean>,
)

internal fun persistReminderSettings(
    context: Context,
    selection: ReminderSettingsSelection,
    currentPrayerTimes: CityPrayerTimes?,
    scheduleReminderSet: (CityPrayerTimes) -> Unit,
) {
    PrayerPreferences.setRemindersGloballyEnabled(context, selection.masterEnabled)
    PrayerPreferences.setJumaaNotificationsEnabled(context, selection.jumaaEnabled)
    PrayerPreferences.setPreReminderMinutes(context, selection.preReminderMinutes)
    PrayerPreferences.setReminderType(context, selection.reminderType)
    PrayerPreferences.setRepeatReminderMinutes(context, selection.repeatMinutes)
    PrayerPreferences.setShowImsakIftarOutsideRamadan(context, selection.showImsakIftarAllMonths)
    selection.enabledStates.forEachIndexed { index, enabled ->
        PrayerPreferences.setReminderEnabled(context, index, enabled)
    }
    PrayerPreferences.setNotificationSetupCompleted(context, true)

    if (selection.jumaaEnabled) {
        PrayerRefreshScheduler.scheduleNextJumaaReminder(context)
    } else {
        PrayerRefreshScheduler.cancelJumaaReminder(context)
    }

    currentPrayerTimes?.let(scheduleReminderSet)
}
