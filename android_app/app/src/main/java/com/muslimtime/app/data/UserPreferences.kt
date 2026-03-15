package com.muslimtime.app.data

import android.content.Context

internal object UserPreferences {
    private const val PREFS_NAME = "prayer_prefs"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_USER_GENDER = "user_gender"
    private const val KEY_USER_AGE = "user_age"
    private const val KEY_BIRTH_DAY = "birth_day"
    private const val KEY_BIRTH_MONTH = "birth_month"
    private const val KEY_BIRTH_YEAR = "birth_year"
    private const val KEY_PROFILE_SETUP_DONE = "profile_setup_done"

    fun getUserName(context: Context): String =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_USER_NAME, "")?.trim().orEmpty()

    fun setUserName(context: Context, value: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_USER_NAME, value.trim())
            .apply()
    }

    fun getUserGender(context: Context): String =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_USER_GENDER, PrayerPreferences.USER_GENDER_UNSPECIFIED)
            ?.takeIf {
                it == PrayerPreferences.USER_GENDER_FEMALE ||
                    it == PrayerPreferences.USER_GENDER_MALE ||
                    it == PrayerPreferences.USER_GENDER_UNSPECIFIED
            }
            ?: PrayerPreferences.USER_GENDER_UNSPECIFIED

    fun setUserGender(context: Context, value: String) {
        val safeValue = when (value) {
            PrayerPreferences.USER_GENDER_FEMALE, PrayerPreferences.USER_GENDER_MALE -> value
            else -> PrayerPreferences.USER_GENDER_UNSPECIFIED
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_USER_GENDER, safeValue)
            .apply()
    }

    fun getUserAge(context: Context): Int? {
        val value = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_USER_AGE, -1)
        return value.takeIf { it in 1..120 }
    }

    fun setUserAge(context: Context, value: Int?) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            if (value == null || value !in 1..120) {
                remove(KEY_USER_AGE)
            } else {
                putInt(KEY_USER_AGE, value)
            }
        }.apply()
    }

    fun getBirthDay(context: Context): Int? {
        val value = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getInt(KEY_BIRTH_DAY, -1)
        return value.takeIf { it in 1..31 }
    }

    fun getBirthMonth(context: Context): Int? {
        val value = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getInt(KEY_BIRTH_MONTH, -1)
        return value.takeIf { it in 1..12 }
    }

    fun getBirthYear(context: Context): Int? {
        val value = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getInt(KEY_BIRTH_YEAR, -1)
        return value.takeIf { it in 1900..2100 }
    }

    fun setBirthDate(
        context: Context,
        day: Int?,
        month: Int?,
        year: Int?,
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            if (day == null || day !in 1..31) remove(KEY_BIRTH_DAY) else putInt(KEY_BIRTH_DAY, day)
            if (month == null || month !in 1..12) remove(KEY_BIRTH_MONTH) else putInt(KEY_BIRTH_MONTH, month)
            if (year == null || year !in 1900..2100) remove(KEY_BIRTH_YEAR) else putInt(KEY_BIRTH_YEAR, year)
        }.apply()
    }

    fun hasBirthDate(context: Context): Boolean =
        getBirthDay(context) != null && getBirthMonth(context) != null && getBirthYear(context) != null

    fun isProfileConfigured(context: Context): Boolean =
        getUserName(context).isNotBlank() &&
            getUserGender(context) != PrayerPreferences.USER_GENDER_UNSPECIFIED &&
            hasBirthDate(context)

    fun personalizedAddress(context: Context): String {
        val name = getUserName(context)
        if (name.isBlank()) return ""
        val suffix = when (getUserGender(context)) {
            PrayerPreferences.USER_GENDER_FEMALE -> "xanım"
            PrayerPreferences.USER_GENDER_MALE -> "bəy"
            else -> ""
        }
        return listOf(name, suffix).filter { it.isNotBlank() }.joinToString(" ").trim()
    }

    fun isProfileSetupCompleted(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_PROFILE_SETUP_DONE, false)

    fun setProfileSetupCompleted(context: Context, complete: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_PROFILE_SETUP_DONE, complete)
            .apply()
    }
}
