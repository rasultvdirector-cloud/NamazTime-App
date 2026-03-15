package com.muslimtime.app.data

import android.content.Context

internal object PrayerSourcePreferences {
    private const val PREFS_NAME = "prayer_prefs"
    private const val KEY_CITY = "selected_city"
    private const val KEY_COUNTRY = "selected_country"
    private const val KEY_PRAYER_SOURCE = "prayer_source"
    private const val KEY_LOCATION_MODE = "location_mode"

    private const val LOCATION_MODE_AUTO = "auto"
    private const val LOCATION_MODE_MANUAL = "manual"

    fun saveLocation(context: Context, location: AppLocation, isAutoDetected: Boolean = false) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CITY, location.city)
            .putString(KEY_COUNTRY, location.country)
            .putString(KEY_LOCATION_MODE, if (isAutoDetected) LOCATION_MODE_AUTO else LOCATION_MODE_MANUAL)
            .apply()
    }

    fun loadSelectedLocation(context: Context): AppLocation? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val city = prefs.getString(KEY_CITY, null) ?: return null
        val country = prefs.getString(KEY_COUNTRY, null) ?: return null
        return AppLocation(city = city, country = country)
    }

    fun suggestedLocation(context: Context): AppLocation {
        val locale = context.resources.configuration.locales[0]
        return when (locale.country.uppercase()) {
            "AZ" -> AppLocation("Baku", "Azerbaijan")
            "TR" -> AppLocation("Istanbul", "Turkey")
            "RU" -> AppLocation("Moscow", "Russia")
            "US" -> AppLocation("New York", "United States")
            "GB" -> AppLocation("London", "United Kingdom")
            else -> AppLocation("Baku", "Azerbaijan")
        }
    }

    fun isAutoLocationEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LOCATION_MODE, LOCATION_MODE_AUTO) == LOCATION_MODE_AUTO

    fun setAutoLocationEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LOCATION_MODE, if (enabled) LOCATION_MODE_AUTO else LOCATION_MODE_MANUAL)
            .apply()
    }

    fun getSelectedPrayerSource(context: Context): String =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_PRAYER_SOURCE, PrayerPreferences.PRAYER_SOURCE_AUTO)
            ?: PrayerPreferences.PRAYER_SOURCE_AUTO

    fun setSelectedPrayerSource(context: Context, source: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PRAYER_SOURCE, source)
            .apply()
    }
}
