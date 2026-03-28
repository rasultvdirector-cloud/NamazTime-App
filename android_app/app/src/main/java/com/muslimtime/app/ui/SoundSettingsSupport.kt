package com.muslimtime.app.ui

import android.content.Context
import android.content.Intent
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import androidx.core.content.ContextCompat
import com.muslimtime.app.R
import com.muslimtime.app.data.AppAzanSound
import com.muslimtime.app.data.PrayerPreferences
import com.muslimtime.app.data.supportedAzanSounds
import com.muslimtime.app.notifications.AzanPlaybackService

private var activeToneRingtone: Ringtone? = null

internal fun azanSoundOptions(context: Context): List<AppAzanSound> {
    val options = supportedAzanSounds(context).toMutableList()
    val customLabel = PrayerPreferences.getCustomAzanLabel(context)
    val customUri = PrayerPreferences.getCustomAzanUri(context)
    if (!customLabel.isNullOrBlank() && !customUri.isNullOrBlank()) {
        options.add(
            0,
            AppAzanSound(
                PrayerPreferences.CUSTOM_AZAN_RAW_NAME,
                context.getString(R.string.azan_sound_option_custom_template, customLabel),
            ),
        )
    }
    return options
}

internal fun selectedAzanIndex(
    selectedRawName: String,
    azanSounds: List<AppAzanSound>,
): Int = azanSounds.indexOfFirst { it.rawName == selectedRawName }.takeIf { it >= 0 } ?: 0

internal fun persistSelectedAzan(
    context: Context,
    rawName: String,
) {
    PrayerPreferences.setSelectedAzanRawName(context, rawName)
    PrayerPreferences.setSoundSetupCompleted(context, true)
}

internal fun startAzanTest(context: Context) {
    ContextCompat.startForegroundService(
        context,
        Intent(context, AzanPlaybackService::class.java).apply {
            action = AzanPlaybackService.ACTION_START
        },
    )
}

internal fun stopAzanTest(context: Context) {
    context.stopService(
        Intent(context, AzanPlaybackService::class.java).apply {
            action = AzanPlaybackService.ACTION_STOP
        },
    )
}

internal fun startToneTest(context: Context) {
    stopToneTest()
    val toneUri = resolveSelectedToneUri(context)
    val ringtone = toneUri?.let { RingtoneManager.getRingtone(context, it) } ?: return
    activeToneRingtone = ringtone
    ringtone.play()
}

internal fun stopToneTest() {
    activeToneRingtone?.stop()
    activeToneRingtone = null
}

internal fun resolveSelectedToneUri(context: Context): Uri? {
    val custom = PrayerPreferences.getSelectedSignalToneUri(context)?.takeIf { it.isNotBlank() }?.let(Uri::parse)
    return custom
        ?: RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_NOTIFICATION)
        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
}

internal fun resolveSelectedToneLabel(context: Context): String {
    PrayerPreferences.getSelectedSignalToneUri(context)?.takeIf { it.isNotBlank() }?.let { value ->
        val uri = Uri.parse(value)
        val label = PrayerPreferences.getSelectedSignalToneLabel(context)
        if (!label.isNullOrBlank()) return label
        val title = RingtoneManager.getRingtone(context, uri)?.getTitle(context)
        if (!title.isNullOrBlank()) return title
    }
    return context.getString(R.string.signal_tone_default)
}

internal fun persistSelectedTone(
    context: Context,
    uri: Uri?,
    label: String?,
) {
    PrayerPreferences.setSelectedSignalTone(context, uri?.toString(), label)
    PrayerPreferences.setSoundSetupCompleted(context, true)
}
