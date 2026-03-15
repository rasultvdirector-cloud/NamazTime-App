package com.muslimtime.app.ui

import android.content.Context
import com.muslimtime.app.data.PrayerPreferences
import com.muslimtime.app.notifications.PrayerRefreshScheduler

internal data class ProfileSettingsSelection(
    val userName: String,
    val gender: String,
    val birthDay: Int?,
    val birthMonth: Int?,
    val birthYear: Int?,
)

internal fun persistProfileSettings(
    context: Context,
    selection: ProfileSettingsSelection,
) {
    PrayerPreferences.setUserName(context, selection.userName)
    PrayerPreferences.setUserGender(context, selection.gender)
    PrayerPreferences.setBirthDate(
        context,
        selection.birthDay,
        selection.birthMonth,
        selection.birthYear,
    )
    PrayerPreferences.setProfileSetupCompleted(context, PrayerPreferences.isProfileConfigured(context))
    PrayerRefreshScheduler.scheduleNextBirthdayReminder(context)
}
