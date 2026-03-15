package com.muslimtime.app.notifications

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.muslimtime.app.R
import com.muslimtime.app.data.PrayerCompletionStore
import com.muslimtime.app.data.PrayerHistoryStore
import com.muslimtime.app.data.PrayerPreferences

class PrayerActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val prayerName = intent.getStringExtra(EXTRA_PRAYER) ?: context.getString(R.string.nav_prayer)
        val notificationId = intent.getIntExtra(EXTRA_ID, 0)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(notificationId)
        context.stopService(Intent(context, AzanPlaybackService::class.java))

        when (action) {
            ACTION_MARK_DONE -> {
                if (notificationId != PrayerReminderReceiver.TEST_NOTIFICATION_ID &&
                    !PrayerPreferences.canMarkPrayerDoneNow(context, notificationId)
                ) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.prayer_status_not_reached),
                        Toast.LENGTH_SHORT,
                    ).show()
                    return
                }
                PrayerCompletionStore.markDone(context, notificationId)
                PrayerHistoryStore.addEntry(context, prayerName)
                PrayerReminderScheduler.scheduleNextForPrayerId(context, notificationId)
                Toast.makeText(
                    context,
                    context.getString(R.string.prayer_done_message, prayerName),
                    Toast.LENGTH_SHORT,
                ).show()
            }

            ACTION_STOP_REMINDER -> Unit
        }
    }

    companion object {
        const val ACTION_MARK_DONE = "com.muslimtime.app.ACTION_MARK_DONE"
        const val ACTION_STOP_REMINDER = "com.muslimtime.app.ACTION_STOP_REMINDER"
        const val ACTION_OPEN_APP = "com.muslimtime.app.ACTION_OPEN_APP"
        const val EXTRA_PRAYER = "extra_prayer"
        const val EXTRA_ID = "extra_id"
    }
}
