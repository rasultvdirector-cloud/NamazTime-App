package com.muslimtime.app.data

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.muslimtime.app.widget.PrayerTimesWidgetProvider
import java.util.Calendar

object PrayerPreferences {
    private const val PREFS_NAME = "prayer_prefs"
    private const val KEY_CITY = "selected_city"
    private const val KEY_COUNTRY = "selected_country"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_USER_GENDER = "user_gender"
    private const val KEY_USER_AGE = "user_age"
    private const val KEY_BIRTH_DAY = "birth_day"
    private const val KEY_BIRTH_MONTH = "birth_month"
    private const val KEY_BIRTH_YEAR = "birth_year"
    private const val KEY_SETUP_COMPLETE = "setup_complete"
    private const val KEY_REMINDERS_ENABLED = "reminders_enabled"
    private const val KEY_JUMAA_NOTIFICATIONS = "jumaa_notifications"
    private const val KEY_PRE_REMINDER_MINUTES = "pre_reminder_minutes"
    private const val KEY_REMINDER_TYPE = "reminder_type"
    private const val KEY_REPEAT_REMINDER_MINUTES = "repeat_reminder_minutes"
    private const val KEY_SHOW_IMSAK_IFTAR_OUTSIDE_RAMADAN = "show_imsak_iftar_outside_ramadan"
    private const val KEY_IMSAK = "time_imsak"
    private const val KEY_FAJR = "time_fajr"
    private const val KEY_SUNRISE = "time_sunrise"
    private const val KEY_DHUHR = "time_dhuhr"
    private const val KEY_ASR = "time_asr"
    private const val KEY_MAGHRIB = "time_maghrib"
    private const val KEY_ISHA = "time_isha"
    private const val KEY_REMINDER_FAJR = "reminder_fajr"
    private const val KEY_REMINDER_SUNRISE = "reminder_sunrise"
    private const val KEY_REMINDER_DHUHR = "reminder_dhuhr"
    private const val KEY_REMINDER_ASR = "reminder_asr"
    private const val KEY_REMINDER_MAGHRIB = "reminder_maghrib"
    private const val KEY_REMINDER_ISHA = "reminder_isha"
    private const val KEY_AZAN_SOUND = "azan_sound"
    private const val KEY_PRAYER_SOURCE = "prayer_source"
    private const val KEY_LOCATION_MODE = "location_mode"
    private const val KEY_LAST_SYNC_DATE = "last_sync_date"
    private const val KEY_LAST_AZERBAIJAN_SYNC_MONTH = "last_azerbaijan_sync_month"
    private const val KEY_LAST_AZERBAIJAN_SYNC_WINDOW = "last_azerbaijan_sync_window"
    private const val KEY_LAST_AZERBAIJAN_ASSET_VERSION = "last_azerbaijan_asset_version"
    private const val KEY_QURAN_READ_FONT_SP = "quran_read_font_sp"
    private const val KEY_QURAN_AUDIO_LAST_URL = "quran_audio_last_url"
    private const val KEY_QURAN_AUDIO_LAST_AYAH_KEY = "quran_audio_last_ayah_key"
    private const val KEY_QURAN_AUDIO_LAST_SURA_TITLE = "quran_audio_last_sura_title"
    private const val KEY_PERMISSION_ONBOARDING_DONE = "permission_onboarding_done"
    private const val KEY_PROFILE_SETUP_DONE = "profile_setup_done"
    private const val KEY_LOCATION_SETUP_DONE = "location_setup_done"
    private const val KEY_NOTIFICATION_SETUP_DONE = "notification_setup_done"
    private const val KEY_SOUND_SETUP_DONE = "sound_setup_done"
    private const val KEY_APP_FONT_SIZE = "app_font_size"
    private const val KEY_ELDER_MODE = "elder_mode"
    private const val LOCATION_MODE_AUTO = "auto"
    private const val LOCATION_MODE_MANUAL = "manual"
    const val FONT_SIZE_NORMAL = "normal"
    const val FONT_SIZE_LARGE = "large"
    const val FONT_SIZE_EXTRA = "extra"
    const val PRAYER_SOURCE_AUTO = "auto"
    const val PRAYER_SOURCE_ALADHAN = "aladhan"
    const val PRAYER_SOURCE_QAFQAZ = "qafqaz"
    const val PRAYER_SOURCE_UMMAH = "ummah"
    const val REMINDER_TYPE_NOTIFICATION = "notification"
    const val REMINDER_TYPE_NOTIFICATION_AZAN = "notification_azan"
    const val REMINDER_TYPE_FULLSCREEN_SIMPLE = "fullscreen_simple"
    const val USER_GENDER_UNSPECIFIED = "unspecified"
    const val USER_GENDER_FEMALE = "female"
    const val USER_GENDER_MALE = "male"

    fun saveLocation(context: Context, location: AppLocation, isAutoDetected: Boolean = false) =
        PrayerSourcePreferences.saveLocation(context, location, isAutoDetected)

    fun getUserName(context: Context): String = UserPreferences.getUserName(context)

    fun setUserName(context: Context, value: String) = UserPreferences.setUserName(context, value)

    fun getUserGender(context: Context): String = UserPreferences.getUserGender(context)

    fun setUserGender(context: Context, value: String) = UserPreferences.setUserGender(context, value)

    fun getUserAge(context: Context): Int? = UserPreferences.getUserAge(context)

    fun setUserAge(context: Context, value: Int?) = UserPreferences.setUserAge(context, value)

    fun getBirthDay(context: Context): Int? = UserPreferences.getBirthDay(context)

    fun getBirthMonth(context: Context): Int? = UserPreferences.getBirthMonth(context)

    fun getBirthYear(context: Context): Int? = UserPreferences.getBirthYear(context)

    fun setBirthDate(context: Context, day: Int?, month: Int?, year: Int?) =
        UserPreferences.setBirthDate(context, day, month, year)

    fun hasBirthDate(context: Context): Boolean = UserPreferences.hasBirthDate(context)

    fun isProfileConfigured(context: Context): Boolean = UserPreferences.isProfileConfigured(context)

    fun personalizedAddress(context: Context): String = UserPreferences.personalizedAddress(context)

    fun saveSelectedPrayerTimes(context: Context, prayerTimes: CityPrayerTimes) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_CITY, prayerTimes.city)
            .putString(KEY_COUNTRY, prayerTimes.country)
            .putString(KEY_IMSAK, prayerTimes.imsakTime ?: prayerTimes.times.getOrNull(0)?.time)
            .putString(KEY_FAJR, prayerTimes.times.getOrNull(0)?.time)
            .putString(KEY_SUNRISE, prayerTimes.times.getOrNull(1)?.time)
            .putString(KEY_DHUHR, prayerTimes.times.getOrNull(2)?.time)
            .putString(KEY_ASR, prayerTimes.times.getOrNull(3)?.time)
            .putString(KEY_MAGHRIB, prayerTimes.times.getOrNull(4)?.time)
            .putString(KEY_ISHA, prayerTimes.times.getOrNull(5)?.time)
            .apply()
        PrayerTimesWidgetProvider.updateAllWidgets(context)
    }

    fun clearSelectedPrayerTimes(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove(KEY_IMSAK)
            .remove(KEY_FAJR)
            .remove(KEY_SUNRISE)
            .remove(KEY_DHUHR)
            .remove(KEY_ASR)
            .remove(KEY_MAGHRIB)
            .remove(KEY_ISHA)
            .remove(KEY_LAST_SYNC_DATE)
            .remove(KEY_LAST_AZERBAIJAN_SYNC_MONTH)
            .remove(KEY_LAST_AZERBAIJAN_SYNC_WINDOW)
            .remove(KEY_LAST_AZERBAIJAN_ASSET_VERSION)
            .apply()
        PrayerTimesWidgetProvider.updateAllWidgets(context)
    }

    fun loadSelectedLocation(context: Context): AppLocation? = PrayerSourcePreferences.loadSelectedLocation(context)

    fun loadSelectedPrayerTimes(context: Context): CityPrayerTimes? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val city = prefs.getString(KEY_CITY, null) ?: return null
        val country = prefs.getString(KEY_COUNTRY, null) ?: return null
        val fajr = prefs.getString(KEY_FAJR, null) ?: return null
        val sunrise = prefs.getString(KEY_SUNRISE, null) ?: return null
        val dhuhr = prefs.getString(KEY_DHUHR, null) ?: return null
        val asr = prefs.getString(KEY_ASR, null) ?: return null
        val maghrib = prefs.getString(KEY_MAGHRIB, null) ?: return null
        val isha = prefs.getString(KEY_ISHA, null) ?: return null
        val names = localizedPrayerNames(context)
        return CityPrayerTimes(
            city = city,
            country = country,
            times = listOf(
                PrayerTime(names[0], fajr),
                PrayerTime(names[1], sunrise),
                PrayerTime(names[2], dhuhr),
                PrayerTime(names[3], asr),
                PrayerTime(names[4], maghrib),
                PrayerTime(names[5], isha),
            ),
            imsakTime = prefs.getString(KEY_IMSAK, fajr),
        )
    }

    fun localizedPrayerNames(context: Context): List<String> = listOf(
        context.getString(com.muslimtime.app.R.string.prayer_name_fajr),
        context.getString(com.muslimtime.app.R.string.prayer_name_sunrise),
        context.getString(com.muslimtime.app.R.string.prayer_name_dhuhr),
        context.getString(com.muslimtime.app.R.string.prayer_name_asr),
        context.getString(com.muslimtime.app.R.string.prayer_name_maghrib_iftar),
        context.getString(com.muslimtime.app.R.string.prayer_name_isha),
    )

    fun reminderEnabledKeys(): List<String> = ReminderPreferences.reminderEnabledKeys()

    fun isReminderEnabled(context: Context, index: Int): Boolean = ReminderPreferences.isReminderEnabled(context, index)

    fun setReminderEnabled(context: Context, index: Int, enabled: Boolean) =
        ReminderPreferences.setReminderEnabled(context, index, enabled)

    fun areRemindersGloballyEnabled(context: Context): Boolean =
        ReminderPreferences.areRemindersGloballyEnabled(context)

    fun setRemindersGloballyEnabled(context: Context, enabled: Boolean) =
        ReminderPreferences.setRemindersGloballyEnabled(context, enabled)

    fun areJumaaNotificationsEnabled(context: Context): Boolean =
        ReminderPreferences.areJumaaNotificationsEnabled(context)

    fun setJumaaNotificationsEnabled(context: Context, enabled: Boolean) =
        ReminderPreferences.setJumaaNotificationsEnabled(context, enabled)

    fun getPreReminderMinutes(context: Context): Int = ReminderPreferences.getPreReminderMinutes(context)

    fun setPreReminderMinutes(context: Context, minutes: Int) =
        ReminderPreferences.setPreReminderMinutes(context, minutes)

    fun getRepeatReminderMinutes(context: Context): Int =
        ReminderPreferences.getRepeatReminderMinutes(context)

    fun setRepeatReminderMinutes(context: Context, minutes: Int) =
        ReminderPreferences.setRepeatReminderMinutes(context, minutes)

    fun shouldShowImsakIftarOutsideRamadan(context: Context): Boolean =
        ReminderPreferences.shouldShowImsakIftarOutsideRamadan(context)

    fun setShowImsakIftarOutsideRamadan(context: Context, enabled: Boolean) =
        ReminderPreferences.setShowImsakIftarOutsideRamadan(context, enabled)

    fun getReminderType(context: Context): String = ReminderPreferences.getReminderType(context)

    fun setReminderType(context: Context, type: String) = ReminderPreferences.setReminderType(context, type)

    fun hasCompletedInitialSetup(context: Context): Boolean = SetupPreferences.hasCompletedInitialSetup(context)

    fun isInitialSetupSatisfied(context: Context): Boolean = SetupPreferences.isInitialSetupSatisfied(context)

    fun getSelectedAzanRawName(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_AZAN_SOUND, "azan_short_1") ?: "azan_short_1"
    }

    fun setSelectedAzanRawName(context: Context, rawName: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_AZAN_SOUND, rawName).apply()
    }

    fun getSelectedPrayerSource(context: Context): String = PrayerSourcePreferences.getSelectedPrayerSource(context)

    fun setSelectedPrayerSource(context: Context, source: String) =
        PrayerSourcePreferences.setSelectedPrayerSource(context, source)

    fun getAppFontSize(context: Context): String = AppearancePreferences.getAppFontSize(context)

    fun setAppFontSize(context: Context, value: String) = AppearancePreferences.setAppFontSize(context, value)

    fun isElderModeEnabled(context: Context): Boolean = AppearancePreferences.isElderModeEnabled(context)

    fun setElderModeEnabled(context: Context, enabled: Boolean) = AppearancePreferences.setElderModeEnabled(context, enabled)

    fun getThemeMode(context: Context): String = AppearancePreferences.getThemeMode(context)

    fun setThemeMode(context: Context, value: String) = AppearancePreferences.setThemeMode(context, value)

    fun appFontScale(context: Context): Float = AppearancePreferences.getAppFontScale(context)

    fun setInitialSetupComplete(context: Context, complete: Boolean) =
        SetupPreferences.setInitialSetupComplete(context, complete)

    fun isProfileSetupCompleted(context: Context): Boolean = UserPreferences.isProfileSetupCompleted(context)

    fun setProfileSetupCompleted(context: Context, complete: Boolean) =
        UserPreferences.setProfileSetupCompleted(context, complete)

    fun isLocationSetupCompleted(context: Context): Boolean = SetupPreferences.isLocationSetupCompleted(context)

    fun setLocationSetupCompleted(context: Context, complete: Boolean) =
        SetupPreferences.setLocationSetupCompleted(context, complete)

    fun isNotificationSetupCompleted(context: Context): Boolean =
        SetupPreferences.isNotificationSetupCompleted(context)

    fun setNotificationSetupCompleted(context: Context, complete: Boolean) =
        SetupPreferences.setNotificationSetupCompleted(context, complete)

    fun isSoundSetupCompleted(context: Context): Boolean = SetupPreferences.isSoundSetupCompleted(context)

    fun setSoundSetupCompleted(context: Context, complete: Boolean) =
        SetupPreferences.setSoundSetupCompleted(context, complete)

    fun suggestedLocation(context: Context): AppLocation = PrayerSourcePreferences.suggestedLocation(context)

    fun canMarkPrayerDoneNow(context: Context, reminderId: Int, now: Calendar = Calendar.getInstance()): Boolean {
        val prayerTimes = loadSelectedPrayerTimes(context) ?: return false
        val prayerIndex = listOf(7, 8, 9, 10, 11, 12).indexOf(reminderId)
        if (prayerIndex !in prayerTimes.times.indices) return false
        val time = prayerTimes.times[prayerIndex].time
        val hour = time.substringBefore(":").toIntOrNull() ?: return false
        val minute = time.substringAfter(":", "0").take(2).toIntOrNull() ?: return false
        val prayerMoment = Calendar.getInstance().apply {
            timeInMillis = now.timeInMillis
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return prayerMoment.timeInMillis <= now.timeInMillis
    }

    fun isAutoLocationEnabled(context: Context): Boolean = PrayerSourcePreferences.isAutoLocationEnabled(context)

    fun setAutoLocationEnabled(context: Context, enabled: Boolean) =
        PrayerSourcePreferences.setAutoLocationEnabled(context, enabled)

    fun getLastPrayerSyncDate(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LAST_SYNC_DATE, null)
    }

    fun setLastPrayerSyncDate(context: Context, value: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LAST_SYNC_DATE, value).apply()
    }

    fun getLastAzerbaijanSyncMonth(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LAST_AZERBAIJAN_SYNC_MONTH, null)
    }

    fun setLastAzerbaijanSyncMonth(context: Context, value: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LAST_AZERBAIJAN_SYNC_MONTH, value).apply()
    }

    fun getLastAzerbaijanSyncWindow(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LAST_AZERBAIJAN_SYNC_WINDOW, null)
    }

    fun setLastAzerbaijanSyncWindow(context: Context, value: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LAST_AZERBAIJAN_SYNC_WINDOW, value).apply()
    }

    fun getLastAzerbaijanAssetVersion(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_LAST_AZERBAIJAN_ASSET_VERSION, -1)
    }

    fun setLastAzerbaijanAssetVersion(context: Context, value: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_LAST_AZERBAIJAN_ASSET_VERSION, value).apply()
    }

    fun getQuranReadFontSizeSp(context: Context): Float {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getFloat(KEY_QURAN_READ_FONT_SP, 22f).coerceIn(18f, 34f)
    }

    fun setQuranReadFontSizeSp(context: Context, value: Float) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putFloat(KEY_QURAN_READ_FONT_SP, value.coerceIn(18f, 34f)).apply()
    }

    fun saveLastQuranAudioPlayback(context: Context, url: String?, ayahKey: String?, suraTitle: String?) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_QURAN_AUDIO_LAST_URL, url)
            .putString(KEY_QURAN_AUDIO_LAST_AYAH_KEY, ayahKey)
            .putString(KEY_QURAN_AUDIO_LAST_SURA_TITLE, suraTitle)
            .apply()
    }

    fun loadLastQuranAudioPlayback(context: Context): Triple<String?, String?, String?> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return Triple(
            prefs.getString(KEY_QURAN_AUDIO_LAST_URL, null),
            prefs.getString(KEY_QURAN_AUDIO_LAST_AYAH_KEY, null),
            prefs.getString(KEY_QURAN_AUDIO_LAST_SURA_TITLE, null),
        )
    }

    fun hasCompletedPermissionOnboarding(context: Context): Boolean =
        SetupPreferences.hasCompletedPermissionOnboarding(context)

    fun setPermissionOnboardingCompleted(context: Context, completed: Boolean) =
        SetupPreferences.setPermissionOnboardingCompleted(context, completed)
}
