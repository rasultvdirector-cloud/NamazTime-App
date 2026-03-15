package com.muslimtime.app.data

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PrayerHistoryEntry(
    val prayerName: String,
    val timestamp: Long,
)

object PrayerHistoryStore {
    private const val PREFS_NAME = "prayer_history"
    private const val KEY_ENTRIES = "entries"
    private const val ENTRY_SEPARATOR = "\n"
    private const val VALUE_SEPARATOR = "|"
    private const val MAX_ENTRIES = 30

    fun addEntry(context: Context, prayerName: String, timestamp: Long = System.currentTimeMillis()) {
        val updated = loadEntries(context).toMutableList().apply {
            add(0, PrayerHistoryEntry(prayerName, timestamp))
            if (size > MAX_ENTRIES) {
                subList(MAX_ENTRIES, size).clear()
            }
        }
        persist(context, updated)
    }

    fun loadEntries(context: Context): List<PrayerHistoryEntry> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_ENTRIES, null).orEmpty()
        if (raw.isBlank()) return emptyList()

        return raw.split(ENTRY_SEPARATOR).mapNotNull { line ->
            val parts = line.split(VALUE_SEPARATOR)
            if (parts.size != 2) return@mapNotNull null
            val timestamp = parts[1].toLongOrNull() ?: return@mapNotNull null
            PrayerHistoryEntry(parts[0], timestamp)
        }
    }

    fun formatEntry(entry: PrayerHistoryEntry): String {
        val formatter = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        return "${entry.prayerName} - ${formatter.format(Date(entry.timestamp))}"
    }

    private fun persist(context: Context, entries: List<PrayerHistoryEntry>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val serialized = entries.joinToString(ENTRY_SEPARATOR) { "${it.prayerName}$VALUE_SEPARATOR${it.timestamp}" }
        prefs.edit().putString(KEY_ENTRIES, serialized).apply()
    }
}
