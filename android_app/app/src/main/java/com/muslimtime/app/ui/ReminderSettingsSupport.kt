package com.muslimtime.app.ui

import android.content.Context
import com.muslimtime.app.data.CityPrayerTimes
import com.muslimtime.app.data.PrayerPreferences
import com.muslimtime.app.notifications.PrayerRefreshScheduler

internal data class ReminderSettingsSelection(
    val masterEnabled: Boolean,
    val jumaaEnabled: Boolean,
    val preReminderMinutes: Int,
    val repeatMinutes: Int,
    val showImsakIftarAllMonths: Boolean,
    val prayerModes: List<String>,
    val notificationStates: List<Boolean>,
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
    PrayerPreferences.setRepeatReminderMinutes(context, selection.repeatMinutes)
    PrayerPreferences.setShowImsakIftarOutsideRamadan(context, selection.showImsakIftarAllMonths)
    selection.prayerModes.forEachIndexed { index, mode ->
        PrayerPreferences.setPrayerReminderMode(context, index, mode)
    }
    selection.notificationStates.forEachIndexed { index, enabled ->
        PrayerPreferences.setPrayerNotificationEnabled(context, index, enabled)
    }
    selection.prayerModes.indices.forEach { index ->
        val enabled = selection.prayerModes[index] != PrayerPreferences.REMINDER_MODE_OFF || selection.notificationStates.getOrElse(index) { false }
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
