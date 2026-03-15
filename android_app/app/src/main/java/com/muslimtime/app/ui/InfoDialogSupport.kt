package com.muslimtime.app.ui

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.muslimtime.app.R

object InfoDialogSupport {
    fun showPrivacyDialog(context: Context) {
        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.privacy_title))
            .setMessage(context.getString(R.string.privacy_text))
            .setPositiveButton(android.R.string.ok, null)
            .setNeutralButton(context.getString(R.string.privacy_policy_title)) { _, _ ->
                AlertDialog.Builder(context)
                    .setTitle(context.getString(R.string.privacy_policy_title))
                    .setMessage(context.getString(R.string.privacy_policy_text))
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
            }
            .show()
    }

    fun showLegacyDialog(context: Context) {
        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.legacy_title))
            .setMessage(context.getString(R.string.legacy_text))
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    fun showInfoDialog(
        context: Context,
        versionName: String,
        onAdvanced: () -> Unit,
        onPrivacy: () -> Unit,
        onLegacy: () -> Unit,
    ) {
        val message = listOf(
            context.getString(R.string.about_text),
            context.getString(R.string.about_version, versionName),
            context.getString(R.string.about_founder),
        ).joinToString("\n\n")
        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.about_title))
            .setMessage(message)
            .setPositiveButton(context.getString(R.string.advanced_title)) { _, _ -> onAdvanced() }
            .setNeutralButton(context.getString(R.string.privacy_title)) { _, _ -> onPrivacy() }
            .setNegativeButton(context.getString(R.string.legacy_title)) { _, _ -> onLegacy() }
            .show()
    }
}
