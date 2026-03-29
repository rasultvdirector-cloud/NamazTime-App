package com.muslimtime.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.muslimtime.app.R
import com.muslimtime.app.data.NotificationCenterStore
import com.muslimtime.app.data.PrayerPreferences
import com.muslimtime.app.ui.MainActivity
import java.util.Calendar

class BirthdayReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        PrayerRefreshScheduler.scheduleNextBirthdayReminder(context)

        val notificationsGranted =
            android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        if (!notificationsGranted) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.birthday_title),
                NotificationManager.IMPORTANCE_HIGH,
            )
            channel.setSound(
                Settings.System.DEFAULT_NOTIFICATION_URI,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            manager.createNotificationChannel(channel)
        }

        val address = PrayerPreferences.personalizedAddress(context)
        val age = PrayerPreferences.getBirthYear(context)?.let { birthYear ->
            (Calendar.getInstance().get(Calendar.YEAR) - birthYear).takeIf { it in 1..130 }
        }
        val body = if (address.isBlank()) {
            context.getString(R.string.birthday_body_fallback)
        } else {
            val messages = mutableListOf(
                context.getString(R.string.birthday_body_1, address),
                context.getString(R.string.birthday_body_2, address),
                context.getString(R.string.birthday_body_3, address),
                context.getString(R.string.birthday_body_4, address),
                context.getString(R.string.birthday_body_5, address),
                context.getString(R.string.birthday_body_6, address),
                context.getString(R.string.birthday_body_7, address),
                context.getString(R.string.birthday_body_8, address),
            )
            age?.let {
                messages += context.getString(R.string.birthday_body_age_1, address, it)
                messages += context.getString(R.string.birthday_body_age_2, address, it)
            }
            messages.random()
        }

        val openIntent = PendingIntent.getActivity(
            context,
            9400,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(context.getString(R.string.birthday_title))
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_SOUND)
            .build()

        manager.notify(NOTIFICATION_ID, notification)
        NotificationCenterStore.addGeneral(
            context,
            uniqueKey = "birthday_${System.currentTimeMillis() / 60000L}",
            title = context.getString(R.string.birthday_title),
            body = body,
        )
    }

    companion object {
        private const val CHANNEL_ID = "birthday_reminder_channel"
        private const val NOTIFICATION_ID = 9401
    }
}
