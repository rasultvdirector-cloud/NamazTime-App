package com.muslimtime.app.data

import android.content.Context
import android.content.Intent
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class NotificationCenterItem(
    val id: String,
    val type: String,
    val title: String,
    val body: String,
    val createdAt: Long,
    val expiresAt: Long,
    val isRead: Boolean,
)

object NotificationCenterStore {
    private const val PREFS_NAME = "notification_center"
    private const val KEY_ITEMS = "items"
    private const val TYPE_PRAYER = "prayer"
    private const val TYPE_ANNOUNCEMENT = "announcement"
    private const val TYPE_GENERAL = "general"
    const val ACTION_UPDATED = "com.muslimtime.app.NOTIFICATION_CENTER_UPDATED"

    fun addPrayer(context: Context, uniqueKey: String, title: String, body: String) {
        upsert(
            context = context,
            item = NotificationCenterItem(
                id = "prayer_$uniqueKey",
                type = TYPE_PRAYER,
                title = title,
                body = body,
                createdAt = System.currentTimeMillis(),
                expiresAt = tomorrowStartMillis(),
                isRead = false,
            ),
        )
    }

    fun addAnnouncement(context: Context, uniqueKey: String, title: String, body: String) {
        upsert(
            context = context,
            item = NotificationCenterItem(
                id = "announcement_$uniqueKey",
                type = TYPE_ANNOUNCEMENT,
                title = title,
                body = body,
                createdAt = System.currentTimeMillis(),
                expiresAt = System.currentTimeMillis() + 7L * 24L * 60L * 60L * 1000L,
                isRead = false,
            ),
        )
    }

    fun addGeneral(context: Context, uniqueKey: String, title: String, body: String) {
        upsert(
            context = context,
            item = NotificationCenterItem(
                id = "general_$uniqueKey",
                type = TYPE_GENERAL,
                title = title,
                body = body,
                createdAt = System.currentTimeMillis(),
                expiresAt = System.currentTimeMillis() + 7L * 24L * 60L * 60L * 1000L,
                isRead = false,
            ),
        )
    }

    fun list(context: Context): List<NotificationCenterItem> {
        cleanupExpired(context, notify = false)
        return loadItems(context).sortedByDescending { it.createdAt }
    }

    fun unreadCount(context: Context): Int = list(context).count { !it.isRead }

    fun markAllRead(context: Context) {
        val updated = loadItems(context).map { it.copy(isRead = true) }
        saveItems(context, updated, notify = true)
    }

    fun delete(context: Context, id: String) {
        saveItems(context, loadItems(context).filterNot { it.id == id }, notify = true)
    }

    fun clearAll(context: Context) {
        saveItems(context, emptyList(), notify = true)
    }

    fun formatTimestamp(timestamp: Long): String {
        return SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(timestamp))
    }

    private fun upsert(context: Context, item: NotificationCenterItem) {
        val existing = loadItems(context).associateBy { it.id }.toMutableMap()
        existing[item.id] = item
        saveItems(context, existing.values.sortedByDescending { it.createdAt }, notify = true)
    }

    private fun cleanupExpired(context: Context, notify: Boolean) {
        val now = System.currentTimeMillis()
        val kept = loadItems(context).filter { it.expiresAt > now }
        saveItems(context, kept, notify = notify)
    }

    private fun loadItems(context: Context): List<NotificationCenterItem> {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_ITEMS, "[]")
            .orEmpty()
        val array = runCatching { JSONArray(raw) }.getOrElse { JSONArray() }
        return buildList {
            for (index in 0 until array.length()) {
                val obj = array.optJSONObject(index) ?: continue
                add(
                    NotificationCenterItem(
                        id = obj.optString("id"),
                        type = obj.optString("type"),
                        title = obj.optString("title"),
                        body = obj.optString("body"),
                        createdAt = obj.optLong("createdAt"),
                        expiresAt = obj.optLong("expiresAt"),
                        isRead = obj.optBoolean("isRead"),
                    ),
                )
            }
        }
    }

    private fun saveItems(context: Context, items: List<NotificationCenterItem>, notify: Boolean) {
        val array = JSONArray()
        items.forEach { item ->
            array.put(
                JSONObject()
                    .put("id", item.id)
                    .put("type", item.type)
                    .put("title", item.title)
                    .put("body", item.body)
                    .put("createdAt", item.createdAt)
                    .put("expiresAt", item.expiresAt)
                    .put("isRead", item.isRead),
            )
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ITEMS, array.toString())
            .apply()
        if (notify) {
            context.sendBroadcast(Intent(ACTION_UPDATED).setPackage(context.packageName))
        }
    }

    private fun tomorrowStartMillis(): Long {
        return Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
