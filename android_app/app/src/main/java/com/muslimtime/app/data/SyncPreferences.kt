package com.muslimtime.app.data

import android.content.Context

object SyncPreferences {
    fun getLastPrayerSyncDate(context: Context): String? =
        prefs(context).getString("last_sync_date", null)

    fun setLastPrayerSyncDate(context: Context, value: String) {
        prefs(context).edit().putString("last_sync_date", value).apply()
    }

    fun getLastAzerbaijanSyncMonth(context: Context): String? =
        prefs(context).getString("last_azerbaijan_sync_month", null)

    fun setLastAzerbaijanSyncMonth(context: Context, value: String) {
        prefs(context).edit().putString("last_azerbaijan_sync_month", value).apply()
    }

    fun getLastAzerbaijanSyncWindow(context: Context): String? =
        prefs(context).getString("last_azerbaijan_sync_window", null)

    fun setLastAzerbaijanSyncWindow(context: Context, value: String) {
        prefs(context).edit().putString("last_azerbaijan_sync_window", value).apply()
    }

    fun getLastAzerbaijanAssetVersion(context: Context): Int =
        prefs(context).getInt("last_azerbaijan_asset_version", 0)

    fun setLastAzerbaijanAssetVersion(context: Context, value: Int) {
        prefs(context).edit().putInt("last_azerbaijan_asset_version", value).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences("prayer_prefs", Context.MODE_PRIVATE)
}
