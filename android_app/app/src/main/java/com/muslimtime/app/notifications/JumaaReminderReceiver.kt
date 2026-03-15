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
import com.muslimtime.app.data.AzerbaijaniDuaRepository
import com.muslimtime.app.data.PrayerPreferences
import com.muslimtime.app.ui.MainActivity
import kotlin.math.absoluteValue

class JumaaReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        PrayerRefreshScheduler.scheduleNextJumaaReminder(context)

        val notificationsGranted =
            android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED

        if (!notificationsGranted) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.jumaa_channel_name),
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

        val messages = AzerbaijaniDuaRepository.jumaaMessages(context).ifEmpty {
            listOf(
                context.getString(R.string.jumaa_body_1),
                context.getString(R.string.jumaa_body_2),
                context.getString(R.string.jumaa_body_3),
            )
        }
        val rawBody = AzerbaijaniDuaRepository.nextMessage(context, "jumaa_messages", messages)
        val address = PrayerPreferences.personalizedAddress(context)
        val body = if (address.isBlank()) rawBody else "$address, $rawBody"

        val openIntent = PendingIntent.getActivity(
            context,
            9300,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(context.getString(R.string.jumaa_title))
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_SOUND)
            .build()

        manager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val CHANNEL_ID = "jumaa_reminder_channel"
        private const val NOTIFICATION_ID = 9301
    }
}
