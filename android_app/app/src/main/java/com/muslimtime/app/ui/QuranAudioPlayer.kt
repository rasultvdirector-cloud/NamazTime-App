package com.muslimtime.app.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri

class QuranAudioPlayer {
    private var mediaPlayer: MediaPlayer? = null
    private var currentUrl: String? = null
    private var prepared = false

    fun play(
        context: Context,
        url: String,
        onReady: () -> Unit,
        onCompletion: () -> Unit,
        onError: () -> Unit,
    ) {
        stop()
        currentUrl = url
        prepared = false

        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build(),
            )
            if (url.startsWith("http://") || url.startsWith("https://")) {
                setDataSource(url)
            } else {
                setDataSource(context, Uri.parse(url))
            }
            setOnPreparedListener {
                prepared = true
                it.start()
                onReady()
            }
            setOnCompletionListener {
                onCompletion()
            }
            setOnErrorListener { _, _, _ ->
                onError()
                stop()
                true
            }
            prepareAsync()
        }
    }

    fun pause() {
        if (prepared) {
            mediaPlayer?.pause()
        }
    }

    fun resume() {
        if (prepared) {
            mediaPlayer?.start()
        }
    }

    fun isPlaying(): Boolean = mediaPlayer?.isPlaying == true

    fun isCurrent(url: String): Boolean = currentUrl == url

    fun stop() {
        mediaPlayer?.runCatching {
            stop()
        }
        mediaPlayer?.release()
        mediaPlayer = null
        currentUrl = null
        prepared = false
    }
}
