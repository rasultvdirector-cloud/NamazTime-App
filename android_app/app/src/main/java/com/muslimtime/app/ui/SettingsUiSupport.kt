package com.muslimtime.app.ui

import android.app.AlarmManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.core.content.ContextCompat
import com.muslimtime.app.R
import com.muslimtime.app.data.AppAzanSound
import com.muslimtime.app.data.PrayerHistoryStore
import com.muslimtime.app.data.PrayerPreferences
import com.muslimtime.app.data.PrayerTimesRepository
import com.muslimtime.app.data.ReminderDiagnosticsStore
import java.util.Calendar

internal fun prayerSourceOptions(context: Context): List<Pair<String, String>> = listOf(
    PrayerPreferences.PRAYER_SOURCE_AUTO to context.getString(R.string.settings_prayer_source_auto),
    PrayerPreferences.PRAYER_SOURCE_ALADHAN to context.getString(R.string.settings_prayer_source_aladhan),
    PrayerPreferences.PRAYER_SOURCE_QAFQAZ to context.getString(R.string.settings_prayer_source_qafqaz),
    PrayerPreferences.PRAYER_SOURCE_UMMAH to context.getString(R.string.settings_prayer_source_ummah),
)

internal fun prayerSourceHelpRes(source: String): Int =
    when (source) {
        PrayerPreferences.PRAYER_SOURCE_QAFQAZ -> R.string.settings_prayer_source_help_qafqaz
        PrayerPreferences.PRAYER_SOURCE_ALADHAN -> R.string.settings_prayer_source_help_aladhan
        PrayerPreferences.PRAYER_SOURCE_UMMAH -> R.string.settings_prayer_source_help_ummah
        else -> R.string.settings_prayer_source_help_auto
    }

internal fun prayerSourceSummaryText(context: Context, source: String): String {
    val label = prayerSourceOptions(context).firstOrNull { it.first == source }?.second
        ?: context.getString(R.string.settings_prayer_source_auto)
    return buildString {
        append(label)
        append("\n")
        append(context.getString(prayerSourceHelpRes(source)))
    }
}

internal fun resolvedPrayerSourceLabel(context: Context, country: String): String {
    val resolvedSource = PrayerTimesRepository.resolvedSource(context, country)
    return context.getString(
        when (resolvedSource) {
            PrayerPreferences.PRAYER_SOURCE_QAFQAZ -> R.string.settings_prayer_source_qafqaz
            PrayerPreferences.PRAYER_SOURCE_UMMAH -> R.string.settings_prayer_source_ummah
            else -> R.string.settings_prayer_source_aladhan
        },
    )
}

internal fun reminderSourceStatusRes(resolvedSource: String): Int =
    when (resolvedSource) {
        PrayerPreferences.PRAYER_SOURCE_QAFQAZ -> R.string.reminder_status_source_qafqaz
        PrayerPreferences.PRAYER_SOURCE_ALADHAN -> R.string.reminder_status_source_aladhan
        PrayerPreferences.PRAYER_SOURCE_UMMAH -> R.string.reminder_status_source_ummah_only
        else -> R.string.reminder_status_source_api
    }

internal fun reminderMethodStatusRes(resolvedSource: String): Int =
    when (resolvedSource) {
        PrayerPreferences.PRAYER_SOURCE_QAFQAZ -> R.string.reminder_status_method_qafqaz
        PrayerPreferences.PRAYER_SOURCE_UMMAH -> R.string.reminder_status_method_ummah
        else -> R.string.reminder_status_method_aladhan
    }

internal fun reminderSyncStatusRes(resolvedSource: String): Int =
    if (resolvedSource == PrayerPreferences.PRAYER_SOURCE_QAFQAZ) {
        R.string.reminder_status_sync_monthly
    } else {
        R.string.reminder_status_sync_daily
    }

internal fun soundSettingsSummaryText(
    context: Context,
    selectedRawName: String,
    azanSounds: List<AppAzanSound>,
): String {
    val selectedLabel = azanSounds.firstOrNull { it.rawName == selectedRawName }?.label
        ?: azanSounds.firstOrNull()?.label
        ?: ""
    return buildString {
        append(context.getString(R.string.settings_sound_selected, selectedLabel))
        append("\n")
        append(context.getString(R.string.azan_sound_note))
    }
}

internal fun profileSettingsSummaryText(context: Context): String {
    val address = PrayerPreferences.personalizedAddress(context)
    val age = PrayerPreferences.getBirthYear(context)?.let { birthYear ->
        (Calendar.getInstance().get(Calendar.YEAR) - birthYear).takeIf { it in 1..130 }
    }
    val birthDate = listOfNotNull(
        PrayerPreferences.getBirthDay(context)?.toString(),
        PrayerPreferences.getBirthMonth(context)?.toString(),
        PrayerPreferences.getBirthYear(context)?.toString(),
    ).takeIf { it.size == 3 }?.joinToString(".")
    if (address.isBlank()) return context.getString(R.string.settings_profile_summary_empty)
    return buildString {
        append(context.getString(R.string.settings_profile_summary_named_full, address, ""))
        age?.let { append(" • $it") }
        birthDate?.let { append(" • $it") }
    }
}

internal fun appearanceSettingsSummaryText(context: Context): String {
    val fontLabel = context.getString(
        when (PrayerPreferences.getAppFontSize(context)) {
            PrayerPreferences.FONT_SIZE_LARGE -> R.string.settings_font_size_large
            PrayerPreferences.FONT_SIZE_EXTRA -> R.string.settings_font_size_extra
            else -> R.string.settings_font_size_normal
        },
    )
    val themeLabel = context.getString(
        when (PrayerPreferences.getThemeMode(context)) {
            com.muslimtime.app.data.AppearancePreferences.THEME_LIGHT -> R.string.settings_theme_light
            com.muslimtime.app.data.AppearancePreferences.THEME_DARK -> R.string.settings_theme_dark
            else -> R.string.settings_theme_system
        },
    )
    val elder = if (PrayerPreferences.isElderModeEnabled(context)) {
        " • ${context.getString(R.string.settings_elder_mode_label)}"
    } else {
        ""
    }
    return context.getString(R.string.settings_appearance_current, fontLabel, themeLabel) + elder
}

internal fun reminderSummaryText(
    context: Context,
    masterEnabled: Boolean,
    enabledStates: List<Boolean>,
): String {
    val enabledNames = PrayerPreferences.localizedPrayerNames(context).filterIndexed { index, _ ->
        enabledStates.getOrElse(index) { false }
    }
    return when {
        !masterEnabled -> context.getString(R.string.reminder_master_disabled)
        enabledNames.isEmpty() -> context.getString(R.string.reminder_selection_empty)
        else -> {
            val before = PrayerPreferences.getPreReminderMinutes(context)
            val repeat = PrayerPreferences.getRepeatReminderMinutes(context)
            buildString {
                append(enabledNames.joinToString(", "))
                if (before > 0) append(" • $before dəq əvvəl")
                if (repeat > 0) append(" • $repeat dəq təkrar")
            }
        }
    }
}

internal fun reminderStatusLines(
    context: Context,
    masterEnabled: Boolean,
    enabledStates: List<Boolean>,
    nextReminderStatus: String,
): List<String> {
    val notificationAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
    val notificationStatus = context.getString(
        if (notificationAllowed) R.string.reminder_status_notification_ok else R.string.reminder_status_notification_missing,
    )

    val exactAlarmAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()
    val exactAlarmStatus = context.getString(
        if (exactAlarmAllowed) R.string.reminder_status_exact_alarm_ok else R.string.reminder_status_exact_alarm_missing,
    )

    val powerManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        context.getSystemService(Context.POWER_SERVICE) as PowerManager
    } else {
        null
    }
    val batteryOptimized = powerManager?.let { !it.isIgnoringBatteryOptimizations(context.packageName) } ?: false
    val batteryStatus = context.getString(
        if (batteryOptimized) R.string.reminder_status_battery_missing else R.string.reminder_status_battery_ok,
    )

    val bootStatus = context.getString(R.string.reminder_status_boot_ready)
    val location = PrayerPreferences.loadSelectedLocation(context)
    val locationStatus = location?.let {
        context.getString(R.string.reminder_status_location, it.city, it.country)
    } ?: context.getString(R.string.reminder_status_location_empty)
    val autoLocationStatus = context.getString(
        if (PrayerPreferences.isAutoLocationEnabled(context)) {
            R.string.reminder_status_auto_location_on
        } else {
            R.string.reminder_status_auto_location_off
        },
    )
    val resolvedSource = PrayerTimesRepository.resolvedSource(context, location?.country.orEmpty())
    val sourceStatus = context.getString(reminderSourceStatusRes(resolvedSource))
    val methodStatus = context.getString(reminderMethodStatusRes(resolvedSource))
    val syncStrategyStatus = context.getString(reminderSyncStatusRes(resolvedSource))
    val lastSyncStatus = if (resolvedSource == PrayerPreferences.PRAYER_SOURCE_QAFQAZ) {
        PrayerPreferences.getLastAzerbaijanSyncWindow(context)?.let {
            context.getString(R.string.reminder_status_last_sync_window, it)
        } ?: context.getString(R.string.reminder_status_last_sync_empty)
    } else {
        PrayerPreferences.getLastPrayerSyncDate(context)?.let {
            context.getString(R.string.reminder_status_last_sync_date, it)
        } ?: context.getString(R.string.reminder_status_last_sync_empty)
    }
    return listOf(
        notificationStatus,
        exactAlarmStatus,
        batteryStatus,
        bootStatus,
        locationStatus,
        autoLocationStatus,
        sourceStatus,
        methodStatus,
        syncStrategyStatus,
        lastSyncStatus,
        nextReminderStatus,
    )
}

internal fun reminderHistoryBlock(context: Context): String {
    val historyEntries = PrayerHistoryStore.loadEntries(context)
    return if (historyEntries.isEmpty()) {
        context.getString(R.string.prayer_history_title) + "\n\n" + context.getString(R.string.prayer_history_empty)
    } else {
        context.getString(R.string.prayer_history_title) + "\n\n" +
            historyEntries.joinToString("\n\n") { PrayerHistoryStore.formatEntry(it) }
    }
}

internal fun reminderDiagnosticsBlock(context: Context): String = ReminderDiagnosticsStore.diagnosticsBlock(context)

internal fun preReminderOptions(context: Context): List<Pair<Int, String>> = listOf(
    0 to context.getString(R.string.settings_reminder_before_off),
    5 to context.getString(R.string.settings_reminder_before_5),
    10 to context.getString(R.string.settings_reminder_before_10),
    15 to context.getString(R.string.settings_reminder_before_15),
)

internal fun reminderTypeOptions(context: Context): List<Pair<String, String>> = listOf(
    PrayerPreferences.REMINDER_TYPE_NOTIFICATION to context.getString(R.string.settings_reminder_type_notification),
    PrayerPreferences.REMINDER_TYPE_NOTIFICATION_AZAN to context.getString(R.string.settings_reminder_type_notification_azan),
    PrayerPreferences.REMINDER_TYPE_FULLSCREEN_SIMPLE to context.getString(R.string.settings_reminder_type_fullscreen),
)

internal fun repeatReminderOptions(context: Context): List<Pair<Int, String>> = listOf(
    0 to context.getString(R.string.settings_reminder_repeat_off),
    10 to context.getString(R.string.settings_reminder_repeat_10),
    20 to context.getString(R.string.settings_reminder_repeat_20),
)

internal fun genderOptions(context: Context): List<Pair<String, String>> = listOf(
    PrayerPreferences.USER_GENDER_UNSPECIFIED to context.getString(R.string.settings_user_gender_unspecified),
    PrayerPreferences.USER_GENDER_FEMALE to context.getString(R.string.settings_user_gender_female),
    PrayerPreferences.USER_GENDER_MALE to context.getString(R.string.settings_user_gender_male),
)

internal fun birthDayOptions(context: Context): List<String> =
    listOf(context.getString(R.string.settings_birth_day_hint)) + (1..31).map { it.toString() }

internal fun birthMonthOptions(context: Context): List<String> =
    listOf(context.getString(R.string.settings_birth_month_hint)) +
        context.resources.getStringArray(R.array.gregorian_months_az).toList()

internal fun birthYearValues(): List<Int> {
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    return (currentYear downTo 1900).toList()
}

internal fun birthYearOptions(context: Context, yearValues: List<Int>): List<String> =
    listOf(context.getString(R.string.settings_birth_year_hint)) + yearValues.map { it.toString() }

internal fun fontSizeOptions(context: Context): List<Pair<String, String>> = listOf(
    PrayerPreferences.FONT_SIZE_NORMAL to context.getString(R.string.settings_font_size_normal),
    PrayerPreferences.FONT_SIZE_LARGE to context.getString(R.string.settings_font_size_large),
    PrayerPreferences.FONT_SIZE_EXTRA to context.getString(R.string.settings_font_size_extra),
)

internal fun bindLabelSpinner(
    context: Context,
    spinner: Spinner,
    labels: List<String>,
    selectedIndex: Int,
) {
    spinner.adapter = ArrayAdapter(
        context,
        android.R.layout.simple_spinner_item,
        labels,
    ).apply {
        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
    }
    spinner.setSelection(selectedIndex.coerceAtLeast(0), false)
}

internal fun <T> pairOptionIndex(options: List<Pair<T, String>>, selectedValue: T): Int =
    options.indexOfFirst { it.first == selectedValue }.coerceAtLeast(0)

internal fun selectedPairValueOr(
    spinner: Spinner,
    options: List<Pair<Int, String>>,
    fallback: Int,
): Int = options.getOrNull(spinner.selectedItemPosition)?.first ?: fallback

internal fun selectedPairValueOr(
    spinner: Spinner,
    options: List<Pair<String, String>>,
    fallback: String,
): String = options.getOrNull(spinner.selectedItemPosition)?.first ?: fallback
