package com.muslimtime.app.notifications

import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.RingtoneManager
import android.net.Uri
import com.muslimtime.app.data.PrayerPreferences

internal object ReminderAudioSupport {
    const val PLAYBACK_MODE_AZAN = "azan"
    const val PLAYBACK_MODE_SIGNAL = "signal"
    private const val DEFAULT_AZAN_DURATION_MS = 30_000L
    private const val DEFAULT_SIGNAL_DURATION_MS = 4_000L

    fun resolvePlaybackUri(context: Context, playbackMode: String): Uri? {
        return when (playbackMode) {
            PLAYBACK_MODE_SIGNAL -> {
                PrayerPreferences.getSelectedSignalToneUri(context)?.takeIf { it.isNotBlank() }?.let(Uri::parse)
                    ?: RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_NOTIFICATION)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }

            else -> {
                val selectedRawName = PrayerPreferences.getSelectedAzanRawName(context)
                if (selectedRawName == PrayerPreferences.CUSTOM_AZAN_RAW_NAME) {
                    PrayerPreferences.getCustomAzanUri(context)?.let(Uri::parse)
                } else {
                    val rawId = context.resources.getIdentifier(selectedRawName, "raw", context.packageName)
                    if (rawId == 0) null else Uri.parse("android.resource://${context.packageName}/$rawId")
                }
            }
        }
    }

    fun estimatePlaybackDurationMs(context: Context, playbackMode: String): Long {
        val fallback = if (playbackMode == PLAYBACK_MODE_SIGNAL) DEFAULT_SIGNAL_DURATION_MS else DEFAULT_AZAN_DURATION_MS
        val uri = resolvePlaybackUri(context, playbackMode) ?: return fallback
        return runCatching {
            MediaMetadataRetriever().use { retriever ->
                retriever.setDataSource(context, uri)
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            }
        }.getOrNull()?.coerceAtLeast(1_000L) ?: fallback
    }
}
