package com.muslimtime.app.data

import android.content.Context

internal object ReminderPreferences {
    private const val PREFS_NAME = "prayer_prefs"
    private const val KEY_REMINDERS_ENABLED = "reminders_enabled"
    private const val KEY_JUMAA_NOTIFICATIONS = "jumaa_notifications"
    private const val KEY_REMINDER_TYPE = "reminder_type"
    private const val KEY_REPEAT_REMINDER_MINUTES = "repeat_reminder_minutes"
    private const val KEY_SHOW_IMSAK_IFTAR_OUTSIDE_RAMADAN = "show_imsak_iftar_outside_ramadan"
    private const val KEY_REMINDER_FAJR = "reminder_fajr"
    private const val KEY_REMINDER_SUNRISE = "reminder_sunrise"
    private const val KEY_REMINDER_DHUHR = "reminder_dhuhr"
    private const val KEY_REMINDER_ASR = "reminder_asr"
    private const val KEY_REMINDER_MAGHRIB = "reminder_maghrib"
    private const val KEY_REMINDER_ISHA = "reminder_isha"
    private const val KEY_MODE_FAJR = "mode_fajr"
    private const val KEY_MODE_SUNRISE = "mode_sunrise"
    private const val KEY_MODE_DHUHR = "mode_dhuhr"
    private const val KEY_MODE_ASR = "mode_asr"
    private const val KEY_MODE_MAGHRIB = "mode_maghrib"
    private const val KEY_MODE_ISHA = "mode_isha"
    private const val KEY_NOTIFY_FAJR = "notify_fajr"
    private const val KEY_NOTIFY_SUNRISE = "notify_sunrise"
    private const val KEY_NOTIFY_DHUHR = "notify_dhuhr"
    private const val KEY_NOTIFY_ASR = "notify_asr"
    private const val KEY_NOTIFY_MAGHRIB = "notify_maghrib"
    private const val KEY_NOTIFY_ISHA = "notify_isha"

    fun reminderEnabledKeys(): List<String> = listOf(
        KEY_REMINDER_FAJR,
        KEY_REMINDER_SUNRISE,
        KEY_REMINDER_DHUHR,
        KEY_REMINDER_ASR,
        KEY_REMINDER_MAGHRIB,
        KEY_REMINDER_ISHA,
    )

    private fun reminderModeKeys(): List<String> = listOf(
        KEY_MODE_FAJR,
        KEY_MODE_SUNRISE,
        KEY_MODE_DHUHR,
        KEY_MODE_ASR,
        KEY_MODE_MAGHRIB,
        KEY_MODE_ISHA,
    )

    private fun notificationEnabledKeys(): List<String> = listOf(
        KEY_NOTIFY_FAJR,
        KEY_NOTIFY_SUNRISE,
        KEY_NOTIFY_DHUHR,
        KEY_NOTIFY_ASR,
        KEY_NOTIFY_MAGHRIB,
        KEY_NOTIFY_ISHA,
    )

    private fun defaultPrayerMode(context: Context, index: Int): String {
        if (index == 1) return PrayerPreferences.REMINDER_MODE_OFF
        return when (getReminderType(context)) {
            PrayerPreferences.REMINDER_TYPE_NOTIFICATION -> PrayerPreferences.REMINDER_MODE_OFF
            PrayerPreferences.REMINDER_TYPE_AZAN_ONLY,
            PrayerPreferences.REMINDER_TYPE_FULLSCREEN_SIMPLE,
            PrayerPreferences.REMINDER_TYPE_NOTIFICATION_AZAN,
            -> PrayerPreferences.REMINDER_MODE_AZAN
            else -> PrayerPreferences.REMINDER_MODE_AZAN
        }
    }

    private fun defaultPrayerNotification(context: Context, index: Int): Boolean {
        if (index == 1) return false
        return getReminderType(context) != PrayerPreferences.REMINDER_TYPE_AZAN_ONLY &&
            getReminderType(context) != PrayerPreferences.REMINDER_TYPE_FULLSCREEN_SIMPLE
    }

    fun isReminderEnabled(context: Context, index: Int): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val legacyEnabled = prefs.getBoolean(reminderEnabledKeys()[index], index != 1)
        val mode = getPrayerReminderMode(context, index)
        val notificationEnabled = isPrayerNotificationEnabled(context, index)
        return legacyEnabled && (mode != PrayerPreferences.REMINDER_MODE_OFF || notificationEnabled)
    }

    fun setReminderEnabled(context: Context, index: Int, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(reminderEnabledKeys()[index], enabled).apply()
        if (!enabled) {
            setPrayerReminderMode(context, index, PrayerPreferences.REMINDER_MODE_OFF)
            setPrayerNotificationEnabled(context, index, false)
        } else {
            if (getPrayerReminderMode(context, index) == PrayerPreferences.REMINDER_MODE_OFF &&
                !isPrayerNotificationEnabled(context, index)
            ) {
                setPrayerReminderMode(context, index, defaultPrayerMode(context, index))
                setPrayerNotificationEnabled(context, index, defaultPrayerNotification(context, index))
            }
        }
    }

    fun areRemindersGloballyEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_REMINDERS_ENABLED, true)

    fun setRemindersGloballyEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_REMINDERS_ENABLED, enabled)
            .apply()
    }

    fun areJumaaNotificationsEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_JUMAA_NOTIFICATIONS, true)

    fun setJumaaNotificationsEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_JUMAA_NOTIFICATIONS, enabled)
            .apply()
    }

    fun getRepeatReminderMinutes(context: Context): Int {
        val value = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_REPEAT_REMINDER_MINUTES, 0)
        return if (value == 10 || value == 20) value else 0
    }

    fun setRepeatReminderMinutes(context: Context, minutes: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_REPEAT_REMINDER_MINUTES, if (minutes == 10 || minutes == 20) minutes else 0)
            .apply()
    }

    fun shouldShowImsakIftarOutsideRamadan(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_SHOW_IMSAK_IFTAR_OUTSIDE_RAMADAN, false)

    fun setShowImsakIftarOutsideRamadan(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_SHOW_IMSAK_IFTAR_OUTSIDE_RAMADAN, enabled)
            .apply()
    }

    fun getPrayerReminderMode(context: Context, index: Int): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = reminderModeKeys()[index]
        val stored = prefs.getString(key, null)
        if (stored == PrayerPreferences.REMINDER_MODE_AZAN || stored == PrayerPreferences.REMINDER_MODE_SIGNAL || stored == PrayerPreferences.REMINDER_MODE_OFF) {
            return stored
        }
        if (!prefs.getBoolean(reminderEnabledKeys()[index], index != 1)) {
            return PrayerPreferences.REMINDER_MODE_OFF
        }
        return defaultPrayerMode(context, index)
    }

    fun setPrayerReminderMode(context: Context, index: Int, mode: String) {
        val safeMode = when (mode) {
            PrayerPreferences.REMINDER_MODE_AZAN,
            PrayerPreferences.REMINDER_MODE_SIGNAL,
            PrayerPreferences.REMINDER_MODE_OFF,
            -> mode
            else -> PrayerPreferences.REMINDER_MODE_OFF
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(reminderModeKeys()[index], safeMode)
            .putBoolean(reminderEnabledKeys()[index], safeMode != PrayerPreferences.REMINDER_MODE_OFF || isPrayerNotificationEnabled(context, index))
            .apply()
    }

    fun isPrayerNotificationEnabled(context: Context, index: Int): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = notificationEnabledKeys()[index]
        if (prefs.contains(key)) {
            return prefs.getBoolean(key, false)
        }
        if (!prefs.getBoolean(reminderEnabledKeys()[index], index != 1)) return false
        return defaultPrayerNotification(context, index)
    }

    fun setPrayerNotificationEnabled(context: Context, index: Int, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(notificationEnabledKeys()[index], enabled)
            .putBoolean(
                reminderEnabledKeys()[index],
                enabled || getPrayerReminderMode(context, index) != PrayerPreferences.REMINDER_MODE_OFF,
            )
            .apply()
    }

    fun getReminderType(context: Context): String =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_REMINDER_TYPE, PrayerPreferences.REMINDER_TYPE_NOTIFICATION_AZAN)
            ?.takeIf {
                it == PrayerPreferences.REMINDER_TYPE_NOTIFICATION ||
                    it == PrayerPreferences.REMINDER_TYPE_NOTIFICATION_AZAN ||
                    it == PrayerPreferences.REMINDER_TYPE_AZAN_ONLY ||
                    it == PrayerPreferences.REMINDER_TYPE_FULLSCREEN_SIMPLE
            }
            ?: PrayerPreferences.REMINDER_TYPE_NOTIFICATION_AZAN

    fun setReminderType(context: Context, type: String) {
        val safeType = if (
            type == PrayerPreferences.REMINDER_TYPE_NOTIFICATION ||
            type == PrayerPreferences.REMINDER_TYPE_NOTIFICATION_AZAN ||
            type == PrayerPreferences.REMINDER_TYPE_AZAN_ONLY ||
            type == PrayerPreferences.REMINDER_TYPE_FULLSCREEN_SIMPLE
        ) {
            type
        } else {
            PrayerPreferences.REMINDER_TYPE_NOTIFICATION_AZAN
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_REMINDER_TYPE, safeType)
            .apply()
    }
}
