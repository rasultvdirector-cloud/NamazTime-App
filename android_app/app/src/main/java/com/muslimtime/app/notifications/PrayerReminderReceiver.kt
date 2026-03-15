package com.muslimtime.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.muslimtime.app.R
import com.muslimtime.app.data.AzerbaijaniDuaRepository
import com.muslimtime.app.data.PrayerCompletionStore
import com.muslimtime.app.data.PrayerPreferences
import com.muslimtime.app.data.dailyDuaContent
import com.muslimtime.app.ui.MainActivity
import com.muslimtime.app.ui.PrayerReminderActivity

class PrayerReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prayerName = intent.getStringExtra(EXTRA_PRAYER) ?: context.getString(R.string.nav_prayer)
        val prayerId = intent.getIntExtra(EXTRA_ID, 0)
        val requestCode = intent.getIntExtra(EXTRA_REQUEST_CODE, prayerId)
        val kind = intent.getStringExtra(EXTRA_KIND) ?: KIND_MAIN
        val leadMinutes = intent.getIntExtra(EXTRA_LEAD_MINUTES, 0)
        val repeatCount = intent.getIntExtra(EXTRA_REPEAT_COUNT, 0)
        val notificationOnly = intent.getBooleanExtra(EXTRA_NOTIFICATION_ONLY, false)

        when (kind) {
            KIND_PRE -> showPreReminder(context, prayerName, prayerId, requestCode, leadMinutes)
            KIND_REPEAT -> showMainReminder(
                context,
                prayerName,
                prayerId,
                requestCode,
                isRepeat = true,
                repeatCount = repeatCount,
                notificationOnly = notificationOnly,
            )
            else -> showMainReminder(
                context,
                prayerName,
                prayerId,
                requestCode,
                isRepeat = false,
                repeatCount = repeatCount,
                notificationOnly = notificationOnly,
            )
        }
    }

    private fun showPreReminder(
        context: Context,
        prayerName: String,
        prayerId: Int,
        requestCode: Int,
        leadMinutes: Int,
    ) {
        val reminderMessage = buildPreReminderMessage(context, prayerName, prayerId, leadMinutes)
        val manager = ensureChannel(
            context = context,
            channelId = PRE_REMINDER_CHANNEL_ID,
            name = context.getString(R.string.reminder_channel_name),
            silent = true,
        )

        val launchPending = PendingIntent.getActivity(
            context,
            requestCode + 5000,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, PRE_REMINDER_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(reminderMessage.title)
            .setContentText(reminderMessage.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(reminderMessage.body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(launchPending)
            .build()

        notifyIfAllowed(context, manager, requestCode, notification)
    }

    private fun showMainReminder(
        context: Context,
        prayerName: String,
        prayerId: Int,
        requestCode: Int,
        isRepeat: Boolean,
        repeatCount: Int,
        notificationOnly: Boolean,
    ) {
        val reminderMessage = buildReminderMessage(context, prayerName, prayerId, isRepeat)

        if (prayerId != TEST_NOTIFICATION_ID) {
            PrayerCompletionStore.markPending(context, prayerId)
        }

        if (!isRepeat && prayerId != TEST_NOTIFICATION_ID) {
            PrayerReminderScheduler.scheduleNextForPrayerId(context, prayerId, forceTomorrow = true)
        }

        if (prayerId != TEST_NOTIFICATION_ID && PrayerPreferences.getRepeatReminderMinutes(context) > 0) {
            PrayerReminderScheduler.scheduleRepeatReminder(context, prayerName, prayerId, repeatCount + 1)
        }

        val reminderType = if (notificationOnly) {
            PrayerPreferences.REMINDER_TYPE_NOTIFICATION
        } else {
            PrayerPreferences.getReminderType(context)
        }
        if (!notificationOnly && reminderType != PrayerPreferences.REMINDER_TYPE_NOTIFICATION) {
            val serviceIntent = Intent(context, AzanPlaybackService::class.java).apply {
                action = AzanPlaybackService.ACTION_START
            }
            ContextCompat.startForegroundService(context, serviceIntent)
        }

        val manager = ensureChannel(
            context = context,
            channelId = MAIN_REMINDER_CHANNEL_ID,
            name = context.getString(R.string.reminder_channel_name),
            silent = true,
        )

        val fullScreenIntent = Intent(context, PrayerReminderActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(PrayerReminderActivity.EXTRA_PRAYER, prayerName)
            putExtra(PrayerReminderActivity.EXTRA_ID, prayerId)
            putExtra(PrayerReminderActivity.EXTRA_TITLE, reminderMessage.title)
            putExtra(PrayerReminderActivity.EXTRA_BODY, reminderMessage.body)
            putExtra(
                PrayerReminderActivity.EXTRA_SIMPLE_MODE,
                reminderType == PrayerPreferences.REMINDER_TYPE_FULLSCREEN_SIMPLE || PrayerPreferences.isElderModeEnabled(context),
            )
        }
        val launchIntent = if (reminderType == PrayerPreferences.REMINDER_TYPE_FULLSCREEN_SIMPLE) {
            fullScreenIntent
        } else {
            Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        }
        val launchPending = PendingIntent.getActivity(
            context,
            requestCode + 1000,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val stopIntent = Intent(context, PrayerActionReceiver::class.java).apply {
            action = PrayerActionReceiver.ACTION_STOP_REMINDER
            putExtra(PrayerActionReceiver.EXTRA_ID, prayerId)
        }
        val stopPending = PendingIntent.getBroadcast(
            context,
            requestCode + 3000,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(context, MAIN_REMINDER_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(reminderMessage.title)
            .setContentText(reminderMessage.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(reminderMessage.body))
            .setContentIntent(launchPending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSilent(true)
            .addAction(0, context.getString(R.string.notification_action_stop), stopPending)

        val notification = builder.build()
        val notificationsGranted = hasNotificationPermission(context)
        if (notificationsGranted) {
            manager.notify(prayerId, notification)
        }
    }

    private fun ensureChannel(
        context: Context,
        channelId: String,
        name: String,
        silent: Boolean,
    ): NotificationManager {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, name, NotificationManager.IMPORTANCE_HIGH)
            if (silent) channel.setSound(null, null)
            manager.createNotificationChannel(channel)
        }
        return manager
    }

    private fun hasNotificationPermission(context: Context): Boolean {
        return android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun notifyIfAllowed(
        context: Context,
        manager: NotificationManager,
        notificationId: Int,
        notification: android.app.Notification,
    ) {
        if (hasNotificationPermission(context)) {
            manager.notify(notificationId, notification)
        }
    }

    private fun buildReminderMessage(
        context: Context,
        prayerName: String,
        id: Int,
        isRepeat: Boolean,
    ): ReminderMessage {
        val isIftar = prayerName.contains(context.getString(R.string.prayer_name_maghrib_iftar), ignoreCase = true)
        val prayerKey = prayerKey(context, prayerName)
        val title =
            when {
                isRepeat -> context.getString(R.string.reminder_repeat_title, prayerName)
                isIftar -> {
                    val city = PrayerPreferences.loadSelectedLocation(context)?.city?.ifBlank {
                        context.getString(R.string.city_baku)
                    } ?: context.getString(R.string.city_baku)
                    context.getString(R.string.reminder_notification_iftar_title, city)
                }
                else -> context.getString(R.string.reminder_notification_title_dynamic, prayerName)
            }
        val body =
            when {
                isRepeat -> buildRepeatBody(context, prayerName, prayerKey, id)
                isIftar -> {
                    val bodyOptions = AzerbaijaniDuaRepository.iftarMessages(context).ifEmpty {
                        listOf(
                            context.getString(R.string.reminder_body_iftar_1),
                            context.getString(R.string.reminder_body_iftar_2),
                            context.getString(R.string.reminder_body_iftar_3),
                        )
                    }
                    context.getString(
                        R.string.reminder_body_iftar_with_dua,
                        AzerbaijaniDuaRepository.nextMessage(context, "iftar_messages", bodyOptions),
                        dailyDuaContent(context).iftarBody ?: context.getString(R.string.daily_iftar_dua_body),
                    )
                }
                else -> {
                    val baseOptions = (
                        AzerbaijaniDuaRepository.prayerMessages(context, prayerKey) +
                            AzerbaijaniDuaRepository.prayerMessages(context) +
                            AzerbaijaniDuaRepository.dailyMessages(context)
                        ).ifEmpty {
                            listOf(
                                context.getString(R.string.reminder_body_prayer_1),
                                context.getString(R.string.reminder_body_prayer_2),
                                context.getString(R.string.reminder_body_prayer_3),
                                context.getString(R.string.reminder_body_prayer_4),
                            )
                        }
                    val closingPool = AzerbaijaniDuaRepository.prayerClosings(context, prayerKey) +
                        AzerbaijaniDuaRepository.prayerClosings(context)
                    val closing = AzerbaijaniDuaRepository.nextMessage(
                        context,
                        "prayer_closing_$prayerKey",
                        closingPool,
                    ).ifBlank {
                        context.getString(R.string.reminder_body_prayer_4)
                    }
                    val base = AzerbaijaniDuaRepository.nextMessage(
                        context,
                        "prayer_message_${if (prayerKey.isBlank()) "general" else prayerKey}",
                        baseOptions,
                    )
                    "$base ${closing.trim()}".trim()
                }
            }
        return ReminderMessage(title = title, body = personalize(context, body))
    }

    private fun buildPreReminderMessage(
        context: Context,
        prayerName: String,
        id: Int,
        leadMinutes: Int,
    ): ReminderMessage {
        val prayerKey = prayerKey(context, prayerName)
        val title = context.getString(
            R.string.reminder_prereminder_title,
            prayerName.removeSuffix(" namazı"),
            leadMinutes,
        )
        val base = AzerbaijaniDuaRepository.nextMessage(
            context,
            "pre_message_${if (prayerKey.isBlank()) "general" else prayerKey}",
            AzerbaijaniDuaRepository.prayerMessages(context, prayerKey) +
                AzerbaijaniDuaRepository.prayerMessages(context) +
                AzerbaijaniDuaRepository.dailyMessages(context),
        ).ifBlank {
            context.getString(R.string.reminder_prereminder_body)
        }
        val closing = AzerbaijaniDuaRepository.nextMessage(
            context,
            "pre_closing_${if (prayerKey.isBlank()) "general" else prayerKey}",
            AzerbaijaniDuaRepository.prayerClosings(context, prayerKey) +
                AzerbaijaniDuaRepository.prayerClosings(context),
        ).ifBlank {
            context.getString(R.string.reminder_body_prayer_4)
        }
        return ReminderMessage(
            title = title,
            body = personalize(context, "$base ${closing.trim()}".trim()),
        )
    }

    private fun buildRepeatBody(
        context: Context,
        prayerName: String,
        prayerKey: String,
        id: Int,
    ): String {
        val base = AzerbaijaniDuaRepository.nextMessage(
            context,
            "repeat_message_${if (prayerKey.isBlank()) "general" else prayerKey}",
            AzerbaijaniDuaRepository.prayerMessages(context, prayerKey) +
                AzerbaijaniDuaRepository.prayerMessages(context) +
                AzerbaijaniDuaRepository.dailyMessages(context),
        ).ifBlank {
            context.getString(R.string.reminder_repeat_body, prayerName)
        }
        val closing = AzerbaijaniDuaRepository.nextMessage(
            context,
            "repeat_closing_${if (prayerKey.isBlank()) "general" else prayerKey}",
            AzerbaijaniDuaRepository.prayerClosings(context, prayerKey) +
                AzerbaijaniDuaRepository.prayerClosings(context),
        ).ifBlank {
            context.getString(R.string.reminder_body_prayer_4)
        }
        return "$base ${closing.trim()}".trim()
    }

    private fun personalize(context: Context, message: String): String {
        val address = PrayerPreferences.personalizedAddress(context)
        return if (address.isBlank()) message else "$address, $message"
    }

    private fun prayerKey(context: Context, prayerName: String): String {
        return when {
            prayerName.contains(context.getString(R.string.prayer_name_fajr), ignoreCase = true) -> "fajr"
            prayerName.contains(context.getString(R.string.prayer_name_dhuhr), ignoreCase = true) -> "dhuhr"
            prayerName.contains(context.getString(R.string.prayer_name_asr), ignoreCase = true) -> "asr"
            prayerName.contains(context.getString(R.string.prayer_name_maghrib_iftar), ignoreCase = true) -> "maghrib"
            prayerName.contains(context.getString(R.string.prayer_name_isha), ignoreCase = true) -> "isha"
            else -> "general"
        }
    }

    private data class ReminderMessage(
        val title: String,
        val body: String,
    )

    companion object {
        const val EXTRA_PRAYER = "extra_prayer"
        const val EXTRA_ID = "extra_id"
        const val EXTRA_REQUEST_CODE = "extra_request_code"
        const val EXTRA_KIND = "extra_kind"
        const val EXTRA_LEAD_MINUTES = "extra_lead_minutes"
        const val EXTRA_REPEAT_COUNT = "extra_repeat_count"
        const val EXTRA_NOTIFICATION_ONLY = "extra_notification_only"
        const val KIND_PRE = "pre"
        const val KIND_MAIN = "main"
        const val KIND_REPEAT = "repeat"
        const val TEST_NOTIFICATION_ID = 9001
        private const val PRE_REMINDER_CHANNEL_ID = "prayer_pre_reminder_channel"
        private const val MAIN_REMINDER_CHANNEL_ID = "prayer_reminder_channel_silent"
    }
}
