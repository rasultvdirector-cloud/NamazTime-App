package com.muslimtime.app.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.muslimtime.app.R
import com.muslimtime.app.data.NotificationCenterStore
import com.muslimtime.app.data.ReminderDiagnosticsStore
import com.muslimtime.app.ui.MainActivity

class NamazFirebaseMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        ReminderDiagnosticsStore.record(
            applicationContext,
            "fcm_token_refreshed",
            "Yeni token alındı (${token.take(12)}...)",
        )
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.notification?.title
            ?: message.data["title"]
            ?: getString(R.string.push_default_title)
        val body = message.notification?.body
            ?: message.data["body"]
            ?: return

        ReminderDiagnosticsStore.record(
            applicationContext,
            "fcm_message_received",
            "${title.take(60)} | ${body.take(80)}",
        )
        NotificationCenterStore.addAnnouncement(
            applicationContext,
            uniqueKey = message.messageId ?: "${System.currentTimeMillis() / 60000L}",
            title = title,
            body = body,
        )
        showNotification(title, body)
    }

    private fun showNotification(title: String, body: String) {
        createChannelIfNeeded()
        if (!hasNotificationPermission()) return

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            7001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(this, GENERAL_PUSH_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        NotificationManagerCompat.from(this).notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification)
    }

    private fun createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            GENERAL_PUSH_CHANNEL_ID,
            getString(R.string.push_channel_general_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = getString(R.string.push_channel_general_description)
        }
        manager.createNotificationChannel(channel)
    }

    private fun hasNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    companion object {
        const val GENERAL_PUSH_CHANNEL_ID = "general_push_announcements"
    }
}
