package com.muslimtime.app.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.muslimtime.app.R
import com.muslimtime.app.data.PrayerPreferences

class AzanPlaybackService : Service() {
    private var mediaPlayer: MediaPlayer? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopPlayback()
            else -> startPlayback()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopPlayback()
        super.onDestroy()
    }

    private fun startPlayback() {
        val selectedRawName = PrayerPreferences.getSelectedAzanRawName(this)
        val rawId = resources.getIdentifier(selectedRawName, "raw", packageName)
        if (rawId == 0) {
            stopSelf()
            return
        }

        createChannelIfNeeded()
        startForeground(NOTIFICATION_ID, buildForegroundNotification())

        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build(),
                )
                setDataSource(this@AzanPlaybackService, android.net.Uri.parse("android.resource://$packageName/$rawId"))
                isLooping = false
                prepare()
                setOnCompletionListener {
                    stopPlayback()
                }
            }
        }

        mediaPlayer?.let { player ->
            if (!player.isPlaying) {
                player.start()
            }
        } ?: stopSelf()
    }

    private fun stopPlayback() {
        mediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildForegroundNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(getString(R.string.azan_playback_title))
            .setContentText(getString(R.string.azan_playback_body))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.azan_playback_channel),
            NotificationManager.IMPORTANCE_LOW,
        )
        manager.createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "azan_playback_channel"
        private const val NOTIFICATION_ID = 4401
        const val ACTION_START = "com.muslimtime.app.ACTION_START_AZAN"
        const val ACTION_STOP = "com.muslimtime.app.ACTION_STOP_AZAN"
    }
}
