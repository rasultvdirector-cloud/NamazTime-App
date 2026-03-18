package com.muslimtime.app.data

import android.content.Context

internal object ReminderPreferences {
    private const val PREFS_NAME = "prayer_prefs"
    private const val KEY_REMINDERS_ENABLED = "reminders_enabled"
    private const val KEY_JUMAA_NOTIFICATIONS = "jumaa_notifications"
    private const val KEY_PRE_REMINDER_MINUTES = "pre_reminder_minutes"
    private const val KEY_REMINDER_TYPE = "reminder_type"
    private const val KEY_REPEAT_REMINDER_MINUTES = "repeat_reminder_minutes"
    private const val KEY_SHOW_IMSAK_IFTAR_OUTSIDE_RAMADAN = "show_imsak_iftar_outside_ramadan"
    private const val KEY_REMINDER_FAJR = "reminder_fajr"
    private const val KEY_REMINDER_SUNRISE = "reminder_sunrise"
    private const val KEY_REMINDER_DHUHR = "reminder_dhuhr"
    private const val KEY_REMINDER_ASR = "reminder_asr"
    private const val KEY_REMINDER_MAGHRIB = "reminder_maghrib"
    private const val KEY_REMINDER_ISHA = "reminder_isha"

    fun reminderEnabledKeys(): List<String> = listOf(
        KEY_REMINDER_FAJR,
        KEY_REMINDER_SUNRISE,
        KEY_REMINDER_DHUHR,
        KEY_REMINDER_ASR,
        KEY_REMINDER_MAGHRIB,
        KEY_REMINDER_ISHA,
    )

    fun isReminderEnabled(context: Context, index: Int): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(reminderEnabledKeys()[index], index != 1)
    }

    fun setReminderEnabled(context: Context, index: Int, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(reminderEnabledKeys()[index], enabled).apply()
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

    fun getPreReminderMinutes(context: Context): Int =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_PRE_REMINDER_MINUTES, 5)
            .coerceIn(0, 15)

    fun setPreReminderMinutes(context: Context, minutes: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_PRE_REMINDER_MINUTES, minutes.coerceIn(0, 15))
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

    fun getReminderType(context: Context): String =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_REMINDER_TYPE, PrayerPreferences.REMINDER_TYPE_NOTIFICATION_AZAN)
            ?.takeIf {
                it == PrayerPreferences.REMINDER_TYPE_NOTIFICATION ||
                    it == PrayerPreferences.REMINDER_TYPE_NOTIFICATION_AZAN ||
                    it == PrayerPreferences.REMINDER_TYPE_FULLSCREEN_SIMPLE
            }
            ?: PrayerPreferences.REMINDER_TYPE_NOTIFICATION_AZAN

    fun setReminderType(context: Context, type: String) {
        val safeType = if (
            type == PrayerPreferences.REMINDER_TYPE_NOTIFICATION ||
            type == PrayerPreferences.REMINDER_TYPE_NOTIFICATION_AZAN ||
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
