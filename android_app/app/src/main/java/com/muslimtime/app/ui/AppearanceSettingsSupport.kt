package com.muslimtime.app.ui

import android.content.Context
import com.muslimtime.app.data.PrayerPreferences

internal data class AppearanceSettingsSelection(
    val fontSize: String,
    val elderModeEnabled: Boolean,
    val themeMode: String,
)

internal fun appearanceDialogSummaryText(
    context: Context,
    selection: AppearanceSettingsSelection,
    fontOptions: List<Pair<String, String>>,
    themeOptions: List<Pair<String, String>>,
): String {
    val fontLabel = fontOptions.firstOrNull { it.first == selection.fontSize }?.second
        ?: context.getString(com.muslimtime.app.R.string.settings_font_size_normal)
    val themeLabel = themeOptions.firstOrNull { it.first == selection.themeMode }?.second
        ?: context.getString(com.muslimtime.app.R.string.settings_theme_system)
    val elder = if (selection.elderModeEnabled) {
        " • ${context.getString(com.muslimtime.app.R.string.settings_elder_mode_label)}"
    } else {
        ""
    }
    return context.getString(com.muslimtime.app.R.string.settings_appearance_current, fontLabel, themeLabel) + elder
}

internal fun persistAppearanceSettings(
    context: Context,
    selection: AppearanceSettingsSelection,
): Boolean {
    val changed = selection.fontSize != PrayerPreferences.getAppFontSize(context) ||
        selection.elderModeEnabled != PrayerPreferences.isElderModeEnabled(context) ||
        selection.themeMode != PrayerPreferences.getThemeMode(context)
    PrayerPreferences.setAppFontSize(context, selection.fontSize)
    PrayerPreferences.setElderModeEnabled(context, selection.elderModeEnabled)
    PrayerPreferences.setThemeMode(context, selection.themeMode)
    return changed
}
