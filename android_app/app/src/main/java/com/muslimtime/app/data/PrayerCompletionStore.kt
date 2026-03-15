package com.muslimtime.app.data

import android.content.Context
import com.muslimtime.app.widget.PrayerTimesWidgetProvider
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class PrayerCompletionState {
    UPCOMING,
    PENDING,
    DONE,
}

object PrayerCompletionStore {
    private const val PREFS_NAME = "prayer_completion"
    private const val VALUE_PENDING = "pending"
    private const val VALUE_DONE = "done"

    fun markPending(context: Context, prayerId: Int, timestamp: Long = System.currentTimeMillis()) {
        saveState(context, prayerId, VALUE_PENDING, timestamp)
    }

    fun markDone(context: Context, prayerId: Int, timestamp: Long = System.currentTimeMillis()) {
        saveState(context, prayerId, VALUE_DONE, timestamp)
    }

    fun getState(context: Context, prayerId: Int, timestamp: Long = System.currentTimeMillis()): PrayerCompletionState {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return when (prefs.getString(buildKey(prayerId, timestamp), null)) {
            VALUE_DONE -> PrayerCompletionState.DONE
            VALUE_PENDING -> PrayerCompletionState.PENDING
            else -> PrayerCompletionState.UPCOMING
        }
    }

    private fun saveState(context: Context, prayerId: Int, value: String, timestamp: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(buildKey(prayerId, timestamp), value).apply()
        PrayerTimesWidgetProvider.updateAllWidgets(context)
    }

    private fun buildKey(prayerId: Int, timestamp: Long): String {
        val day = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date(timestamp))
        return "prayer_${prayerId}_$day"
    }
}
