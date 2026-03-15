package com.muslimtime.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.LayoutInflater
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.muslimtime.app.R

object ReminderStatusDialogSupport {
    fun show(
        context: Context,
        inflater: LayoutInflater,
        contentText: String,
        onFailure: () -> Unit,
    ) {
        runCatching {
            val notificationIntent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
            val batteryIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            } else null
            val alarmIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
            } else null
            val dialogView = inflater.inflate(R.layout.dialog_reminder_status, null)
            dialogView.findViewById<TextView>(R.id.status_dialog_content)?.text = contentText
            dialogView.findViewById<Button>(R.id.status_open_notifications)?.setOnClickListener {
                runCatching { context.startActivity(notificationIntent) }
            }
            val secondaryButton = dialogView.findViewById<Button>(R.id.status_open_secondary)
            secondaryButton?.text = context.getString(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    R.string.reminder_status_open_alarms
                } else {
                    R.string.reminder_status_open_battery
                },
            )
            secondaryButton?.setOnClickListener {
                val target = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) alarmIntent else batteryIntent
                target?.let { intent -> runCatching { context.startActivity(intent) } }
            }
            AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.reminder_status_title))
                .setView(dialogView)
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }.onFailure {
            onFailure()
        }
    }

    fun showSafeFallback(context: Context) {
        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.reminder_status_title))
            .setMessage(context.getString(R.string.settings_safe_mode_message))
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }
}
