package com.muslimtime.app.ui

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.muslimtime.app.data.AppAzanSound
import com.muslimtime.app.data.PrayerPreferences
import com.muslimtime.app.notifications.AzanPlaybackService

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
