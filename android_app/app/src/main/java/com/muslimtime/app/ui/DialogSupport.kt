package com.muslimtime.app.ui

import android.content.Context
import android.view.View
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog

internal object DialogSupport {
    internal fun createOkDialog(
        context: Context,
        @StringRes titleRes: Int,
        view: View,
        onPositive: (() -> Unit)? = null,
        onDismiss: (() -> Unit)? = null,
    ): AlertDialog {
        return AlertDialog.Builder(context)
            .setTitle(context.getString(titleRes))
            .setView(view)
            .setPositiveButton(android.R.string.ok) { _, _ -> onPositive?.invoke() }
            .create().also { dialog ->
                dialog.setOnDismissListener { onDismiss?.invoke() }
            }
    }

    internal fun createDialog(
        context: Context,
        title: String,
        view: View,
        positiveText: String,
        onPositive: (() -> Unit)? = null,
        negativeText: String? = null,
        onNegative: (() -> Unit)? = null,
        cancelable: Boolean = true,
        onDismiss: (() -> Unit)? = null,
    ): AlertDialog {
        return AlertDialog.Builder(context)
            .setTitle(title)
            .setView(view)
            .setCancelable(cancelable)
            .setPositiveButton(positiveText) { _, _ -> onPositive?.invoke() }
            .apply {
                if (negativeText != null) {
                    setNegativeButton(negativeText) { _, _ -> onNegative?.invoke() }
                }
            }
            .create().also { dialog ->
                dialog.setOnDismissListener { onDismiss?.invoke() }
            }
    }
}
