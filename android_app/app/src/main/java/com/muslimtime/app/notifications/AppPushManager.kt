package com.muslimtime.app.notifications

import android.content.Context
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.muslimtime.app.BuildConfig
import com.muslimtime.app.data.ReminderDiagnosticsStore

object AppPushManager {
    private const val TAG = "AppPushManager"
    private const val TOPIC_ALL_USERS = "all_users"
    private const val TOPIC_ANDROID_USERS = "android_users"

    fun initialize(context: Context) {
        if (!isConfigured(context)) {
            ReminderDiagnosticsStore.record(
                context,
                "fcm_init_skipped",
                context.getString(com.muslimtime.app.R.string.push_init_missing_config),
            )
            return
        }

        FirebaseMessaging.getInstance().isAutoInitEnabled = true
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                ReminderDiagnosticsStore.record(
                    context,
                    "fcm_token_ready",
                    "Token alındı (${token.take(12)}...)",
                )
            }
            .addOnFailureListener { throwable ->
                ReminderDiagnosticsStore.record(
                    context,
                    "fcm_token_failed",
                    throwable.message ?: "Token alma xətası",
                )
                Log.w(TAG, "FCM token almaq olmadı", throwable)
            }

        subscribe(context, TOPIC_ALL_USERS)
        subscribe(context, TOPIC_ANDROID_USERS)
    }

    fun isConfigured(context: Context): Boolean {
        if (!BuildConfig.FIREBASE_PUSH_ENABLED) return false
        val googleAppId = runCatching {
            val resId = context.resources.getIdentifier("google_app_id", "string", context.packageName)
            if (resId == 0) "" else context.getString(resId)
        }.getOrDefault("")
        return googleAppId.isNotBlank()
    }

    private fun subscribe(context: Context, topic: String) {
        FirebaseMessaging.getInstance().subscribeToTopic(topic)
            .addOnSuccessListener {
                ReminderDiagnosticsStore.record(context, "fcm_topic_subscribed", topic)
            }
            .addOnFailureListener { throwable ->
                ReminderDiagnosticsStore.record(
                    context,
                    "fcm_topic_failed",
                    "$topic | ${throwable.message ?: "xəta"}",
                )
                Log.w(TAG, "FCM topic subscribe olmadı: $topic", throwable)
            }
    }
}
