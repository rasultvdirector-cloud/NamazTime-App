package com.muslimtime.app.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import androidx.media.session.MediaButtonReceiver
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.muslimtime.app.R
import com.muslimtime.app.data.PrayerPreferences
import com.muslimtime.app.data.QuranAudioBackendRepository
import com.muslimtime.app.data.QuranAudioOfflineRepository
import com.muslimtime.app.data.quranSuras
import com.muslimtime.app.ui.MainActivity
import kotlin.concurrent.thread

class QuranAudioPlaybackService : Service() {
    private var mediaPlayer: MediaPlayer? = null
    private var playlist: List<QuranTrack> = emptyList()
    private var currentIndex: Int = -1
    private var currentSuraNumber: Int = -1
    private lateinit var mediaSession: MediaSessionCompat
    private val progressHandler = Handler(Looper.getMainLooper())
    private val progressRunnable = object : Runnable {
        override fun run() {
            if (mediaPlayer != null && currentTrackUrl != null) {
                currentPositionMs = mediaPlayer?.currentPosition ?: 0
                currentDurationMs = mediaPlayer?.duration?.takeIf { it > 0 } ?: 0
                notifyStateChanged()
                updateNotification()
                if (isPlayingNow) {
                    progressHandler.postDelayed(this, 1000L)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> handlePlay(intent)
            ACTION_PAUSE -> pausePlayback()
            ACTION_RESUME -> resumePlayback()
            ACTION_NEXT -> playNext()
            ACTION_PREVIOUS -> playPrevious()
            ACTION_STOP -> stopPlayback()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        mediaSession = MediaSessionCompat(this, "QuranAudioPlayback").apply {
            isActive = true
        }
    }

    override fun onDestroy() {
        progressHandler.removeCallbacksAndMessages(null)
        releasePlayer()
        mediaSession.release()
        super.onDestroy()
    }

    private fun handlePlay(intent: Intent) {
        val urls = intent.getStringArrayListExtra(EXTRA_URLS).orEmpty()
        val ayahKeys = intent.getStringArrayListExtra(EXTRA_AYAH_KEYS).orEmpty()
        val suraTitle = intent.getStringExtra(EXTRA_SURA_TITLE).orEmpty()
        val suraNumber = intent.getIntExtra(EXTRA_SURA_NUMBER, ayahKeys.firstOrNull()?.substringBefore(":")?.toIntOrNull() ?: -1)
        val requestedIndex = intent.getIntExtra(EXTRA_INDEX, 0)
        if (urls.isEmpty() || ayahKeys.size != urls.size) return

        currentSuraNumber = suraNumber
        playlist = urls.mapIndexed { index, url ->
            QuranTrack(
                url = url,
                ayahKey = ayahKeys.getOrElse(index) { "${index + 1}" },
                suraTitle = suraTitle,
            )
        }
        playIndex(requestedIndex.coerceIn(0, playlist.lastIndex))
    }

    private fun playIndex(index: Int) {
        val track = playlist.getOrNull(index) ?: run {
            stopPlayback()
            return
        }
        currentIndex = index
        currentTrackUrl = track.url
        currentAyahKey = track.ayahKey
        currentSuraTitle = track.suraTitle
        isPlayingNow = false
        PrayerPreferences.saveLastQuranAudioPlayback(this, track.url, track.ayahKey, track.suraTitle)
        currentPositionMs = 0
        currentDurationMs = 0

        createChannelIfNeeded()
        updateMediaSession(track, PlaybackStateCompat.STATE_BUFFERING)
        startForeground(NOTIFICATION_ID, buildNotification(track))
        releasePlayer()

        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build(),
            )
            val uri = Uri.parse(track.url)
            if (track.url.startsWith("http://") || track.url.startsWith("https://")) {
                setDataSource(track.url)
            } else {
                setDataSource(this@QuranAudioPlaybackService, uri)
            }
            setOnPreparedListener {
                currentDurationMs = it.duration.takeIf { duration -> duration > 0 } ?: 0
                it.start()
                isPlayingNow = true
                updateMediaSession(track, PlaybackStateCompat.STATE_PLAYING)
                notifyStateChanged()
                updateNotification()
                progressHandler.removeCallbacks(progressRunnable)
                progressHandler.post(progressRunnable)
            }
            setOnCompletionListener {
                progressHandler.removeCallbacks(progressRunnable)
                val nextIndex = currentIndex + 1
                if (nextIndex <= playlist.lastIndex) {
                    playIndex(nextIndex)
                } else {
                    playAdjacentSura(1)
                }
            }
            setOnErrorListener { _, _, _ ->
                updateMediaSession(track, PlaybackStateCompat.STATE_ERROR)
                stopPlayback()
                true
            }
            prepareAsync()
        }

        notifyStateChanged()
    }

    private fun pausePlayback() {
        mediaPlayer?.takeIf { it.isPlaying }?.pause()
        isPlayingNow = false
        currentPositionMs = mediaPlayer?.currentPosition ?: currentPositionMs
        playlist.getOrNull(currentIndex)?.let {
            updateMediaSession(it, PlaybackStateCompat.STATE_PAUSED)
            PrayerPreferences.saveLastQuranAudioPlayback(this, it.url, it.ayahKey, it.suraTitle)
        }
        progressHandler.removeCallbacks(progressRunnable)
        updateNotification()
        notifyStateChanged()
    }

    private fun resumePlayback() {
        mediaPlayer?.start()
        if (mediaPlayer != null) {
            isPlayingNow = true
            playlist.getOrNull(currentIndex)?.let {
                updateMediaSession(it, PlaybackStateCompat.STATE_PLAYING)
                PrayerPreferences.saveLastQuranAudioPlayback(this, it.url, it.ayahKey, it.suraTitle)
            }
            progressHandler.removeCallbacks(progressRunnable)
            progressHandler.post(progressRunnable)
            updateNotification()
            notifyStateChanged()
        }
    }

    private fun playNext() {
        playAdjacentSura(1)
    }

    private fun playPrevious() {
        playAdjacentSura(-1)
    }

    private fun playAdjacentSura(offset: Int) {
        val current = currentSuraNumber
        if (current <= 0) {
            stopPlayback()
            return
        }
        val nextSura = (current + offset).coerceIn(1, quranSuras().last().number)
        if (nextSura == current) {
            stopPlayback()
            return
        }
        thread {
            val result = QuranAudioBackendRepository.fetchAyahs(this, nextSura)
            result.onSuccess { ayahs ->
                val playableAyahs = QuranAudioOfflineRepository.resolvePlaybackAyahs(this, nextSura, ayahs)
                if (ayahs.isEmpty()) {
                    stopPlayback()
                    return@onSuccess
                }
                val suraTitle = quranSuras().firstOrNull { it.number == nextSura }?.let { "${it.number}. ${it.name}" }.orEmpty()
                currentSuraNumber = nextSura
                playlist = playableAyahs.map {
                    QuranTrack(
                        url = it.audioUrl,
                        ayahKey = it.ayahKey,
                        suraTitle = suraTitle,
                    )
                }
                playIndex(0)
            }.onFailure {
                stopPlayback()
            }
        }
    }

    private fun stopPlayback() {
        releasePlayer()
        playlist = emptyList()
        currentIndex = -1
        currentTrackUrl = null
        currentAyahKey = null
        currentSuraTitle = null
        isPlayingNow = false
        currentPositionMs = 0
        currentDurationMs = 0
        progressHandler.removeCallbacks(progressRunnable)
        mediaSession.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_STOPPED, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1f)
                .build(),
        )
        notifyStateChanged()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun releasePlayer() {
        mediaPlayer?.runCatching {
            if (isPlaying) stop()
        }
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun updateNotification() {
        val track = playlist.getOrNull(currentIndex) ?: return
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(track))
    }

    private fun buildNotification(track: QuranTrack): Notification {
        val coverArt = buildCoverArt(track)
        val openIntent = PendingIntent.getActivity(
            this,
            8200,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val pauseOrPlayIntent = PendingIntent.getService(
            this,
            8201,
            Intent(this, QuranAudioPlaybackService::class.java).apply {
                action = if (isPlayingNow) ACTION_PAUSE else ACTION_RESUME
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stopIntent = PendingIntent.getService(
            this,
            8202,
            Intent(this, QuranAudioPlaybackService::class.java).apply {
                action = ACTION_STOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val previousIntent = PendingIntent.getService(
            this,
            8203,
            Intent(this, QuranAudioPlaybackService::class.java).apply {
                action = ACTION_PREVIOUS
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val nextIntent = PendingIntent.getService(
            this,
            8204,
            Intent(this, QuranAudioPlaybackService::class.java).apply {
                action = ACTION_NEXT
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(track.suraTitle.ifBlank { getString(R.string.quran_audio_tab) })
            .setContentText(track.ayahKey)
            .setLargeIcon(coverArt)
            .setSubText(
                getString(
                    R.string.quran_audio_progress_template,
                    formatTime(currentPositionMs),
                    formatTime(currentDurationMs),
                ),
            )
            .setContentIntent(openIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(isPlayingNow)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setProgress(currentDurationMs.coerceAtLeast(0), currentPositionMs.coerceAtLeast(0), currentDurationMs <= 0)
            .setStyle(
                MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
                    .setCancelButtonIntent(stopIntent)
                    .setShowCancelButton(!isPlayingNow),
            )
            .addAction(android.R.drawable.ic_media_previous, getString(R.string.quran_audio_previous_button), previousIntent)
            .addAction(
                if (isPlayingNow) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                getString(if (isPlayingNow) R.string.quran_audio_pause_button else R.string.quran_audio_play_button),
                pauseOrPlayIntent,
            )
            .addAction(android.R.drawable.ic_media_next, getString(R.string.quran_audio_next_button), nextIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.quran_audio_stop_button), stopIntent)
            .build()
    }

    private fun createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.quran_audio_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        )
        manager.createNotificationChannel(channel)
    }

    private fun notifyStateChanged() {
        sendBroadcast(
            Intent(ACTION_STATE_CHANGED).apply {
                setPackage(packageName)
                putExtra(EXTRA_URL, currentTrackUrl)
                putExtra(EXTRA_AYAH_KEY, currentAyahKey)
                putExtra(EXTRA_IS_PLAYING, isPlayingNow)
                putExtra(EXTRA_IS_ACTIVE, currentTrackUrl != null)
                putExtra(EXTRA_POSITION_MS, currentPositionMs)
                putExtra(EXTRA_DURATION_MS, currentDurationMs)
            },
        )
    }

    private fun updateMediaSession(track: QuranTrack, state: Int) {
        val availableActions =
            PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                PlaybackStateCompat.ACTION_STOP

        mediaSession.setMetadata(
            MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, track.ayahKey)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, track.suraTitle)
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, buildCoverArt(track))
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ART, buildCoverArt(track))
                .build(),
        )
        mediaSession.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(availableActions)
                .setState(state, currentPositionMs.toLong(), if (isPlayingNow) 1f else 0f)
                .build(),
        )
    }

    private fun formatTime(valueMs: Int): String {
        val totalSeconds = (valueMs / 1000).coerceAtLeast(0)
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    private fun buildCoverArt(track: QuranTrack): Bitmap {
        val size = 512
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val cardRect = RectF(0f, 0f, size.toFloat(), size.toFloat())

        val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f,
                0f,
                size.toFloat(),
                size.toFloat(),
                intArrayOf(Color.parseColor("#0A6B3A"), Color.parseColor("#02572E")),
                null,
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRoundRect(cardRect, 56f, 56f, backgroundPaint)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            color = Color.parseColor("#33FFFFFF")
        }
        canvas.drawRoundRect(
            RectF(18f, 18f, size - 18f, size - 18f),
            44f,
            44f,
            borderPaint,
        )

        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#22FFFFFF")
        }
        val badgeRect = RectF(size * 0.30f, size * 0.12f, size * 0.70f, size * 0.22f)
        canvas.drawRoundRect(badgeRect, 18f, 18f, badgePaint)

        val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F2FFF8")
            textAlign = Paint.Align.CENTER
            textSize = 22f
            isFakeBoldText = true
            letterSpacing = 0.08f
        }
        canvas.drawText(getString(R.string.quran_audio_tab), size / 2f, size * 0.185f, badgeTextPaint)

        val ornamentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = Color.parseColor("#18FFFFFF")
        }
        val ornamentDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#16FFFFFF")
        }
        val centerX = size / 2f
        val centerY = size * 0.40f
        val ringRadius = size * 0.12f
        canvas.drawCircle(centerX, centerY, ringRadius, ornamentPaint)
        canvas.drawCircle(centerX, centerY, ringRadius * 0.68f, ornamentPaint)
        canvas.drawCircle(centerX, centerY - ringRadius, 6f, ornamentDotPaint)
        canvas.drawCircle(centerX + ringRadius, centerY, 6f, ornamentDotPaint)
        canvas.drawCircle(centerX, centerY + ringRadius, 6f, ornamentDotPaint)
        canvas.drawCircle(centerX - ringRadius, centerY, 6f, ornamentDotPaint)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            textSize = 40f
            isFakeBoldText = true
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D8F3E5")
            textAlign = Paint.Align.CENTER
            textSize = 28f
        }
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#55D8F3E5")
            strokeWidth = 2f
        }

        val title = track.suraTitle.ifBlank { getString(R.string.quran_audio_tab) }
        val subtitle = track.ayahKey
        canvas.drawText(ellipsize(title, 20), size / 2f, size * 0.57f, titlePaint)
        canvas.drawLine(size * 0.24f, size * 0.66f, size * 0.76f, size * 0.66f, dividerPaint)
        canvas.drawText(subtitle, size / 2f, size * 0.76f, subtitlePaint)

        return bitmap
    }

    private fun ellipsize(value: String, maxChars: Int): String {
        return if (value.length <= maxChars) value else value.take(maxChars - 1) + "…"
    }

    data class QuranTrack(
        val url: String,
        val ayahKey: String,
        val suraTitle: String,
    )

    companion object {
        private const val CHANNEL_ID = "quran_audio_playback_channel"
        private const val NOTIFICATION_ID = 5501

        const val ACTION_PLAY = "com.muslimtime.app.ACTION_QURAN_PLAY"
        const val ACTION_PAUSE = "com.muslimtime.app.ACTION_QURAN_PAUSE"
        const val ACTION_RESUME = "com.muslimtime.app.ACTION_QURAN_RESUME"
        const val ACTION_NEXT = "com.muslimtime.app.ACTION_QURAN_NEXT"
        const val ACTION_PREVIOUS = "com.muslimtime.app.ACTION_QURAN_PREVIOUS"
        const val ACTION_STOP = "com.muslimtime.app.ACTION_QURAN_STOP"
        const val ACTION_STATE_CHANGED = "com.muslimtime.app.ACTION_QURAN_STATE_CHANGED"

        const val EXTRA_URLS = "extra_urls"
        const val EXTRA_AYAH_KEYS = "extra_ayah_keys"
        const val EXTRA_INDEX = "extra_index"
        const val EXTRA_SURA_TITLE = "extra_sura_title"
        const val EXTRA_SURA_NUMBER = "extra_sura_number"
        const val EXTRA_URL = "extra_url"
        const val EXTRA_AYAH_KEY = "extra_ayah_key"
        const val EXTRA_IS_PLAYING = "extra_is_playing"
        const val EXTRA_IS_ACTIVE = "extra_is_active"
        const val EXTRA_POSITION_MS = "extra_position_ms"
        const val EXTRA_DURATION_MS = "extra_duration_ms"

        @Volatile
        var currentTrackUrl: String? = null

        @Volatile
        var currentAyahKey: String? = null

        @Volatile
        var currentSuraTitle: String? = null

        @Volatile
        var isPlayingNow: Boolean = false

        @Volatile
        var currentPositionMs: Int = 0

        @Volatile
        var currentDurationMs: Int = 0
    }
}
