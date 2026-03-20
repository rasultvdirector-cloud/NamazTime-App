package com.muslimtime.app.data

import android.content.Context
import android.os.Build
import com.muslimtime.app.BuildConfig
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.Executors

object ReminderDiagnosticsStore {
    private const val PREFS_NAME = "prayer_prefs"
    private const val KEY_RECENT_EVENTS = "diagnostics_recent_events"
    private const val MAX_EVENTS = 40
    private val uploadExecutor = Executors.newSingleThreadExecutor()

    fun record(context: Context, event: String, detail: String) {
        val timestamp = timestampNow()
        val line = "$timestamp | $event | $detail"
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existing = prefs.getString(KEY_RECENT_EVENTS, "").orEmpty()
            .split('\n')
            .filter { it.isNotBlank() }
        val updated = (existing + line).takeLast(MAX_EVENTS)
        prefs.edit().putString(KEY_RECENT_EVENTS, updated.joinToString("\n")).apply()
        uploadAsync(event = event, detail = detail, timestamp = timestamp)
    }

    fun diagnosticsBlock(context: Context): String {
        val events = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_RECENT_EVENTS, "")
            .orEmpty()
            .split('\n')
            .filter { it.isNotBlank() }
            .takeLast(MAX_EVENTS)
            .reversed()
        return if (events.isEmpty()) {
            "Azan diagnostics\n\nHələ diaqnostik hadisə yazılmayıb."
        } else {
            "Azan diagnostics\n\n" + events.joinToString("\n\n")
        }
    }

    private fun timestampNow(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getDefault()
        }.format(Date())
    }

    private fun uploadAsync(event: String, detail: String, timestamp: String) {
        val baseUrl = BuildConfig.TELEMETRY_BASE_URL.trim().trimEnd('/')
        if (baseUrl.isBlank()) return
        uploadExecutor.execute {
            runCatching {
                val connection = (URL("$baseUrl/telemetry/logs").openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 5_000
                    readTimeout = 5_000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                }
                val payload = """
                    {
                      "appId":"${json(BuildConfig.APPLICATION_ID)}",
                      "versionName":"${json(BuildConfig.VERSION_NAME)}",
                      "versionCode":"${BuildConfig.VERSION_CODE}",
                      "device":"${json(Build.MANUFACTURER + " " + Build.MODEL)}",
                      "android":"${json(Build.VERSION.RELEASE ?: "")}",
                      "event":"${json(event)}",
                      "detail":"${json(detail)}",
                      "timestamp":"${json(timestamp)}"
                    }
                """.trimIndent()
                connection.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
                connection.inputStream.use { it.readBytes() }
                connection.disconnect()
            }
        }
    }

    private fun json(value: String): String = buildString {
        value.forEach { ch ->
            when (ch) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(ch)
            }
        }
    }
}
