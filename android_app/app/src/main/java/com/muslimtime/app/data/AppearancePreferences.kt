package com.muslimtime.app.data

import android.content.Context

internal object AppearancePreferences {
    private const val PREFS_NAME = "prayer_prefs"
    private const val KEY_APP_FONT_SIZE = "app_font_size"
    private const val KEY_ELDER_MODE = "elder_mode"
    private const val KEY_THEME_MODE = "theme_mode"

    const val THEME_SYSTEM = "system"
    const val THEME_LIGHT = "light"
    const val THEME_DARK = "dark"

    fun getAppFontSize(context: Context): String {
        val value = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_APP_FONT_SIZE, PrayerPreferences.FONT_SIZE_NORMAL)
        return when (value) {
            PrayerPreferences.FONT_SIZE_LARGE,
            PrayerPreferences.FONT_SIZE_EXTRA,
            PrayerPreferences.FONT_SIZE_NORMAL,
            -> value ?: PrayerPreferences.FONT_SIZE_NORMAL
            else -> PrayerPreferences.FONT_SIZE_NORMAL
        }
    }

    fun setAppFontSize(context: Context, value: String) {
        val safeValue = when (value) {
            PrayerPreferences.FONT_SIZE_LARGE,
            PrayerPreferences.FONT_SIZE_EXTRA,
            -> value
            else -> PrayerPreferences.FONT_SIZE_NORMAL
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_APP_FONT_SIZE, safeValue)
            .apply()
    }

    fun isElderModeEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ELDER_MODE, false)

    fun setElderModeEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ELDER_MODE, enabled)
            .apply()
    }

    fun getThemeMode(context: Context): String {
        val value = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_THEME_MODE, THEME_SYSTEM)
        return when (value) {
            THEME_LIGHT, THEME_DARK, THEME_SYSTEM -> value ?: THEME_SYSTEM
            else -> THEME_SYSTEM
        }
    }

    fun setThemeMode(context: Context, value: String) {
        val safeValue = when (value) {
            THEME_LIGHT, THEME_DARK -> value
            else -> THEME_SYSTEM
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME_MODE, safeValue)
            .apply()
    }

    fun getAppFontScale(context: Context): Float {
        val base = when (getAppFontSize(context)) {
            PrayerPreferences.FONT_SIZE_LARGE -> 1.12f
            PrayerPreferences.FONT_SIZE_EXTRA -> 1.26f
            else -> 1f
        }
        return if (isElderModeEnabled(context)) base * 1.12f else base
    }
}
