package com.muslimtime.app.ui

import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.muslimtime.app.R
import com.muslimtime.app.data.CityPrayerTimes
import com.muslimtime.app.data.PrayerPreferences
import com.muslimtime.app.notifications.PrayerReminderReceiver

internal object NotificationSettingsDialogSupport {
    internal data class PrayerRowBindings(
        val azanToggle: TextView,
        val signalToggle: TextView,
        val notificationToggle: TextView,
    )

    internal data class Bindings(
        val jumaaSwitch: androidx.appcompat.widget.SwitchCompat,
        val preReminderSpinner: Spinner,
        val showImsakIftarAllMonthsSwitch: androidx.appcompat.widget.SwitchCompat,
        val prayerRows: List<PrayerRowBindings>,
        val testReminderButton: Button,
        val testPreReminderButton: Button,
    )

    internal data class SpinnerOptions(
        val preReminderOptions: List<Pair<Int, String>>,
    )

    internal data class BoundState(
        val bindings: Bindings,
        val options: SpinnerOptions,
    )

    internal fun collectBindings(view: View): Bindings = Bindings(
        jumaaSwitch = view.findViewById(R.id.switch_jumaa_notifications_dialog),
        preReminderSpinner = view.findViewById(R.id.spinner_pre_reminder_dialog),
        showImsakIftarAllMonthsSwitch = view.findViewById(R.id.switch_show_imsak_iftar_all_months_dialog),
        prayerRows = listOf(
            PrayerRowBindings(
                azanToggle = view.findViewById(R.id.switch_fajr_azan_dialog),
                signalToggle = view.findViewById(R.id.switch_fajr_signal_dialog),
                notificationToggle = view.findViewById(R.id.switch_fajr_notification_dialog),
            ),
            PrayerRowBindings(
                azanToggle = view.findViewById(R.id.switch_dhuhr_azan_dialog),
                signalToggle = view.findViewById(R.id.switch_dhuhr_signal_dialog),
                notificationToggle = view.findViewById(R.id.switch_dhuhr_notification_dialog),
            ),
            PrayerRowBindings(
                azanToggle = view.findViewById(R.id.switch_asr_azan_dialog),
                signalToggle = view.findViewById(R.id.switch_asr_signal_dialog),
                notificationToggle = view.findViewById(R.id.switch_asr_notification_dialog),
            ),
            PrayerRowBindings(
                azanToggle = view.findViewById(R.id.switch_maghrib_azan_dialog),
                signalToggle = view.findViewById(R.id.switch_maghrib_signal_dialog),
                notificationToggle = view.findViewById(R.id.switch_maghrib_notification_dialog),
            ),
            PrayerRowBindings(
                azanToggle = view.findViewById(R.id.switch_isha_azan_dialog),
                signalToggle = view.findViewById(R.id.switch_isha_signal_dialog),
                notificationToggle = view.findViewById(R.id.switch_isha_notification_dialog),
            ),
        ),
        testReminderButton = view.findViewById(R.id.test_reminder_button_dialog),
        testPreReminderButton = view.findViewById(R.id.test_pre_reminder_button_dialog),
    )

    internal fun bindState(
        context: Context,
        view: View,
        preReminderOptions: List<Pair<Int, String>>,
        bindLabelSpinner: (Spinner, List<String>, Int) -> Unit,
        pairOptionIndexInt: (List<Pair<Int, String>>, Int) -> Int,
    ): BoundState {
        val bindings = collectBindings(view)
        bindLabelSpinner(
            bindings.preReminderSpinner,
            preReminderOptions.map { it.second },
            pairOptionIndexInt(preReminderOptions, PrayerPreferences.getPreReminderMinutes(context)),
        )
        bindInitialSwitchState(context, bindings)
        return BoundState(bindings, SpinnerOptions(preReminderOptions))
    }

    internal fun bindInitialSwitchState(context: Context, bindings: Bindings) {
        bindings.jumaaSwitch.isChecked = PrayerPreferences.areJumaaNotificationsEnabled(context)
        bindings.showImsakIftarAllMonthsSwitch.isChecked =
            PrayerPreferences.shouldShowImsakIftarOutsideRamadan(context)
        val prayerIndexes = listOf(0, 2, 3, 4, 5)
        bindings.prayerRows.forEachIndexed { index, row ->
            val prayerIndex = prayerIndexes[index]
            val mode = PrayerPreferences.getPrayerReminderMode(context, prayerIndex)
            bindModeToggleAppearance(context, row.azanToggle, row.signalToggle, mode)
            bindNotificationToggleAppearance(
                context,
                row.notificationToggle,
                PrayerPreferences.isPrayerNotificationEnabled(context, prayerIndex),
            )
        }
    }

    private fun buildPrayerModes(rows: List<PrayerRowBindings>): List<String> = listOf(
        rowMode(rows.getOrNull(0)),
        PrayerPreferences.REMINDER_MODE_OFF,
        rowMode(rows.getOrNull(1)),
        rowMode(rows.getOrNull(2)),
        rowMode(rows.getOrNull(3)),
        rowMode(rows.getOrNull(4)),
    )

    private fun buildPrayerNotifications(rows: List<PrayerRowBindings>): List<Boolean> = listOf(
        rows.getOrNull(0)?.notificationToggle?.tag == true,
        false,
        rows.getOrNull(1)?.notificationToggle?.tag == true,
        rows.getOrNull(2)?.notificationToggle?.tag == true,
        rows.getOrNull(3)?.notificationToggle?.tag == true,
        rows.getOrNull(4)?.notificationToggle?.tag == true,
    )

    private fun rowMode(row: PrayerRowBindings?): String {
        return when {
            row?.azanToggle?.tag == true -> PrayerPreferences.REMINDER_MODE_AZAN
            row?.signalToggle?.tag == true -> PrayerPreferences.REMINDER_MODE_SIGNAL
            else -> PrayerPreferences.REMINDER_MODE_OFF
        }
    }

    private fun bindModeToggleAppearance(
        context: Context,
        azanView: TextView,
        signalView: TextView,
        mode: String,
    ) {
        val isSignal = mode == PrayerPreferences.REMINDER_MODE_SIGNAL
        azanView.tag = !isSignal
        signalView.tag = isSignal
        applyPillState(context, azanView, !isSignal)
        applyPillState(context, signalView, isSignal)
    }

    private fun applyPillState(context: Context, textView: TextView, active: Boolean) {
        textView.background = ContextCompat.getDrawable(
            context,
            if (active) R.drawable.bg_reminder_mode_active else R.drawable.bg_reminder_mode_inactive,
        )
        textView.setTextColor(
            ContextCompat.getColor(context, if (active) R.color.white else R.color.text_primary),
        )
    }

    private fun bindNotificationToggleAppearance(context: Context, textView: TextView, enabled: Boolean) {
        textView.tag = enabled
        textView.text = if (enabled) context.getString(R.string.settings_notification_checkmark) else ""
        textView.background = ContextCompat.getDrawable(
            context,
            if (enabled) R.drawable.bg_reminder_square_active else R.drawable.bg_reminder_square_inactive,
        )
        textView.setTextColor(
            ContextCompat.getColor(context, if (enabled) R.color.white else R.color.text_primary),
        )
    }

    internal fun buildSelection(
        context: Context,
        bindings: Bindings,
        options: SpinnerOptions,
        selectedIntValue: (Spinner, List<Pair<Int, String>>, Int) -> Int,
    ): ReminderSettingsSelection {
        val prayerModes = buildPrayerModes(bindings.prayerRows)
        val notificationStates = buildPrayerNotifications(bindings.prayerRows)
        val anyEnabled = prayerModes.indices.any { index ->
            prayerModes[index] != PrayerPreferences.REMINDER_MODE_OFF || notificationStates.getOrElse(index) { false }
        }
        return ReminderSettingsSelection(
            masterEnabled = anyEnabled,
            jumaaEnabled = bindings.jumaaSwitch.isChecked,
            preReminderMinutes = selectedIntValue(
                bindings.preReminderSpinner,
                options.preReminderOptions,
                PrayerPreferences.getPreReminderMinutes(context),
            ),
            repeatMinutes = 0,
            showImsakIftarAllMonths = bindings.showImsakIftarAllMonthsSwitch.isChecked,
            prayerModes = prayerModes,
            notificationStates = notificationStates,
        )
    }

    internal fun persistSelection(
        context: Context,
        selection: ReminderSettingsSelection,
        currentPrayerTimes: CityPrayerTimes?,
        scheduleReminderSet: (CityPrayerTimes) -> Unit,
    ) {
        persistReminderSettings(context, selection, currentPrayerTimes, scheduleReminderSet)
    }

    internal fun attachAutoSaveListeners(bindings: Bindings, persist: () -> Unit, selectionListenerFactory: (() -> Unit) -> android.widget.AdapterView.OnItemSelectedListener) {
        bindings.jumaaSwitch.setOnCheckedChangeListener { _, _ -> persist() }
        bindings.preReminderSpinner.onItemSelectedListener = selectionListenerFactory(persist)
        bindings.showImsakIftarAllMonthsSwitch.setOnCheckedChangeListener { _, _ -> persist() }
        bindings.prayerRows.forEach { row ->
            row.azanToggle.setOnClickListener {
                bindModeToggleAppearance(it.context, row.azanToggle, row.signalToggle, PrayerPreferences.REMINDER_MODE_AZAN)
                persist()
            }
            row.signalToggle.setOnClickListener {
                bindModeToggleAppearance(it.context, row.azanToggle, row.signalToggle, PrayerPreferences.REMINDER_MODE_SIGNAL)
                persist()
            }
            row.notificationToggle.setOnClickListener {
                val next = row.notificationToggle.tag != true
                bindNotificationToggleAppearance(it.context, row.notificationToggle, next)
                persist()
            }
        }
    }

    internal fun attachTestActions(context: Context, bindings: Bindings, options: SpinnerOptions) {
        bindings.testReminderButton.setOnClickListener {
            sendNotificationOnlyReminderTest(context, context.getString(R.string.reminder_test_prayer_name))
        }
        bindings.testPreReminderButton.setOnClickListener {
            val lead = options.preReminderOptions[bindings.preReminderSpinner.selectedItemPosition].first.takeIf { it > 0 } ?: 5
            sendPreReminderTest(context, context.getString(R.string.prayer_name_dhuhr), lead)
        }
    }

    internal fun sendNotificationOnlyReminderTest(context: Context, prayerName: String) {
        context.sendBroadcast(
            Intent(context, PrayerReminderReceiver::class.java).apply {
                putExtra(PrayerReminderReceiver.EXTRA_PRAYER, prayerName)
                putExtra(PrayerReminderReceiver.EXTRA_ID, PrayerReminderReceiver.TEST_NOTIFICATION_ID)
                putExtra(PrayerReminderReceiver.EXTRA_REQUEST_CODE, PrayerReminderReceiver.TEST_NOTIFICATION_ID)
                putExtra(PrayerReminderReceiver.EXTRA_KIND, PrayerReminderReceiver.KIND_DELAYED_NOTIFICATION)
            },
        )
    }

    internal fun sendPreReminderTest(context: Context, prayerName: String, leadMinutes: Int) {
        context.sendBroadcast(
            Intent(context, PrayerReminderReceiver::class.java).apply {
                putExtra(PrayerReminderReceiver.EXTRA_PRAYER, prayerName)
                putExtra(PrayerReminderReceiver.EXTRA_ID, PrayerReminderReceiver.TEST_NOTIFICATION_ID + 1)
                putExtra(PrayerReminderReceiver.EXTRA_REQUEST_CODE, PrayerReminderReceiver.TEST_NOTIFICATION_ID + 1)
                putExtra(PrayerReminderReceiver.EXTRA_KIND, PrayerReminderReceiver.KIND_PRE)
                putExtra(PrayerReminderReceiver.EXTRA_LEAD_MINUTES, leadMinutes)
            },
        )
    }
}
