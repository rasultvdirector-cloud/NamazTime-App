package com.muslimtime.app.ui

import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import com.muslimtime.app.R
import com.muslimtime.app.data.CityPrayerTimes
import com.muslimtime.app.data.PrayerPreferences
import com.muslimtime.app.notifications.PrayerReminderReceiver

internal object NotificationSettingsDialogSupport {
    internal data class Bindings(
        val jumaaSwitch: SwitchCompat,
        val preReminderSpinner: Spinner,
        val reminderTypeSpinner: Spinner,
        val prayerSectionTitle: TextView,
        val showImsakIftarAllMonthsSwitch: SwitchCompat,
        val prayerSwitches: List<SwitchCompat>,
        val testReminderButton: Button,
        val testPreReminderButton: Button,
    )

    private fun buildPrayerEnabledStates(prayerSwitches: List<SwitchCompat>): List<Boolean> = listOf(
        prayerSwitches.getOrNull(0)?.isChecked == true,
        false,
        prayerSwitches.getOrNull(1)?.isChecked == true,
        prayerSwitches.getOrNull(2)?.isChecked == true,
        prayerSwitches.getOrNull(3)?.isChecked == true,
        prayerSwitches.getOrNull(4)?.isChecked == true,
    )

    internal data class SpinnerOptions(
        val preReminderOptions: List<Pair<Int, String>>,
        val reminderTypeOptions: List<Pair<String, String>>,
    )

    internal data class BoundState(
        val bindings: Bindings,
        val options: SpinnerOptions,
    )

    internal fun collectBindings(view: View): Bindings = Bindings(
        jumaaSwitch = view.findViewById(R.id.switch_jumaa_notifications_dialog),
        preReminderSpinner = view.findViewById(R.id.spinner_pre_reminder_dialog),
        reminderTypeSpinner = view.findViewById(R.id.spinner_reminder_type_dialog),
        prayerSectionTitle = view.findViewById(R.id.notification_prayer_section_title),
        showImsakIftarAllMonthsSwitch = view.findViewById(R.id.switch_show_imsak_iftar_all_months_dialog),
        prayerSwitches = listOf(
            view.findViewById(R.id.switch_fajr_dialog),
            view.findViewById(R.id.switch_dhuhr_dialog),
            view.findViewById(R.id.switch_asr_dialog),
            view.findViewById(R.id.switch_maghrib_dialog),
            view.findViewById(R.id.switch_isha_dialog),
        ),
        testReminderButton = view.findViewById(R.id.test_reminder_button_dialog),
        testPreReminderButton = view.findViewById(R.id.test_pre_reminder_button_dialog),
    )

    internal fun bindState(
        context: Context,
        view: View,
        preReminderOptions: List<Pair<Int, String>>,
        reminderTypeOptions: List<Pair<String, String>>,
        bindLabelSpinner: (Spinner, List<String>, Int) -> Unit,
        pairOptionIndexInt: (List<Pair<Int, String>>, Int) -> Int,
        pairOptionIndexString: (List<Pair<String, String>>, String) -> Int,
    ): BoundState {
        val bindings = collectBindings(view)
        bindLabelSpinner(
            bindings.preReminderSpinner,
            preReminderOptions.map { it.second },
            pairOptionIndexInt(preReminderOptions, PrayerPreferences.getPreReminderMinutes(context)),
        )
        bindLabelSpinner(
            bindings.reminderTypeSpinner,
            reminderTypeOptions.map { it.second },
            pairOptionIndexString(reminderTypeOptions, PrayerPreferences.getReminderType(context)),
        )
        bindInitialSwitchState(context, bindings)
        return BoundState(bindings, SpinnerOptions(preReminderOptions, reminderTypeOptions))
    }

    internal fun bindInitialSwitchState(context: Context, bindings: Bindings) {
        bindings.jumaaSwitch.isChecked = PrayerPreferences.areJumaaNotificationsEnabled(context)
        bindings.showImsakIftarAllMonthsSwitch.isChecked =
            PrayerPreferences.shouldShowImsakIftarOutsideRamadan(context)
        val prayerIndexes = listOf(0, 2, 3, 4, 5)
        bindings.prayerSwitches.forEachIndexed { index, switch ->
            switch.isChecked = PrayerPreferences.isReminderEnabled(context, prayerIndexes[index])
        }
    }

    internal fun buildSelection(
        context: Context,
        bindings: Bindings,
        options: SpinnerOptions,
        selectedIntValue: (Spinner, List<Pair<Int, String>>, Int) -> Int,
        selectedStringValue: (Spinner, List<Pair<String, String>>, String) -> String,
    ): ReminderSettingsSelection {
        return ReminderSettingsSelection(
            masterEnabled = buildPrayerEnabledStates(bindings.prayerSwitches).any { it },
            jumaaEnabled = bindings.jumaaSwitch.isChecked,
            preReminderMinutes = selectedIntValue(
                bindings.preReminderSpinner,
                options.preReminderOptions,
                PrayerPreferences.getPreReminderMinutes(context),
            ),
            reminderType = selectedStringValue(
                bindings.reminderTypeSpinner,
                options.reminderTypeOptions,
                PrayerPreferences.getReminderType(context),
            ),
            repeatMinutes = 0,
            showImsakIftarAllMonths = bindings.showImsakIftarAllMonthsSwitch.isChecked,
            enabledStates = buildPrayerEnabledStates(bindings.prayerSwitches),
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
        bindings.reminderTypeSpinner.onItemSelectedListener = selectionListenerFactory(persist)
        bindings.showImsakIftarAllMonthsSwitch.setOnCheckedChangeListener { _, _ -> persist() }
        bindings.prayerSwitches.forEach { switch ->
            switch.setOnCheckedChangeListener { _, _ -> persist() }
        }
    }

    internal fun updatePrayerSectionTitle(context: Context, bindings: Bindings) {
        val reminderType = when (bindings.reminderTypeSpinner.selectedItemPosition) {
            0 -> PrayerPreferences.REMINDER_TYPE_NOTIFICATION
            1 -> PrayerPreferences.REMINDER_TYPE_NOTIFICATION_AZAN
            2 -> PrayerPreferences.REMINDER_TYPE_AZAN_ONLY
            else -> PrayerPreferences.REMINDER_TYPE_NOTIFICATION_AZAN
        }
        bindings.prayerSectionTitle.text = context.getString(
            if (reminderType == PrayerPreferences.REMINDER_TYPE_NOTIFICATION) {
                R.string.settings_reminder_prayers_label_notification
            } else {
                R.string.settings_reminder_prayers_label_azan
            },
        )
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
                putExtra(PrayerReminderReceiver.EXTRA_KIND, PrayerReminderReceiver.KIND_MAIN)
                putExtra(PrayerReminderReceiver.EXTRA_NOTIFICATION_ONLY, true)
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
