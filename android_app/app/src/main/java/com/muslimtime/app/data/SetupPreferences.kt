package com.muslimtime.app.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

internal object SetupPreferences {
    private const val PREFS_NAME = "prayer_prefs"
    private const val KEY_SETUP_COMPLETE = "setup_complete"
    private const val KEY_PERMISSION_ONBOARDING_DONE = "permission_onboarding_done"
    private const val KEY_LOCATION_SETUP_DONE = "location_setup_done"
    private const val KEY_NOTIFICATION_SETUP_DONE = "notification_setup_done"
    private const val KEY_SOUND_SETUP_DONE = "sound_setup_done"

    fun hasCompletedInitialSetup(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_SETUP_COMPLETE, false)

    fun setInitialSetupComplete(context: Context, complete: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_SETUP_COMPLETE, complete)
            .apply()
    }

    fun isLocationSetupCompleted(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_LOCATION_SETUP_DONE, false)

    fun setLocationSetupCompleted(context: Context, complete: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_LOCATION_SETUP_DONE, complete)
            .apply()
    }

    fun isNotificationSetupCompleted(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_NOTIFICATION_SETUP_DONE, false)

    fun setNotificationSetupCompleted(context: Context, complete: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_NOTIFICATION_SETUP_DONE, complete)
            .apply()
    }

    fun isSoundSetupCompleted(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_SOUND_SETUP_DONE, false)

    fun setSoundSetupCompleted(context: Context, complete: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_SOUND_SETUP_DONE, complete)
            .apply()
    }

    fun hasCompletedPermissionOnboarding(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_PERMISSION_ONBOARDING_DONE, false)

    fun setPermissionOnboardingCompleted(context: Context, completed: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_PERMISSION_ONBOARDING_DONE, completed)
            .apply()
    }

    fun isInitialSetupSatisfied(context: Context): Boolean {
        val notificationsGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        val locationGranted =
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (!notificationsGranted) return false
        if (!UserPreferences.isProfileConfigured(context) ||
            !UserPreferences.isProfileSetupCompleted(context) ||
            !isNotificationSetupCompleted(context) ||
            !isSoundSetupCompleted(context)
        ) {
            return false
        }
        val locationReady = if (PrayerSourcePreferences.isAutoLocationEnabled(context)) {
            locationGranted
        } else {
            PrayerSourcePreferences.loadSelectedLocation(context) != null
        }
        return locationReady && isLocationSetupCompleted(context)
    }
}
