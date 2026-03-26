package com.muslimtime.app.ui

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.muslimtime.app.R
import com.muslimtime.app.data.AppAzanSound
import com.muslimtime.app.data.PrayerPreferences
import com.muslimtime.app.data.supportedAzanSounds
import com.muslimtime.app.notifications.AzanPlaybackService

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
