package com.muslimtime.app.data

import android.content.Context
import org.json.JSONObject
import java.util.Calendar
import kotlin.random.Random
import kotlin.math.absoluteValue

object AzerbaijaniDuaRepository {
    private const val ASSET_FILE = "duas_az.json"
    private const val ROTATION_PREFS = "dua_rotation_prefs"
    private const val KEY_ORDER_PREFIX = "order_"
    private const val KEY_CURSOR_PREFIX = "cursor_"
    private const val KEY_LAST_PREFIX = "last_"

    fun dailyMessages(context: Context): List<String> =
        loadRoot(context).getJSONArray("general_daily_duas").toStringList()

    fun prayerMessages(context: Context): List<String> =
        loadRoot(context).getJSONArray("prayer_messages").toStringList()

    fun prayerMessages(context: Context, key: String): List<String> =
        loadRoot(context).optJSONArray("${key}_messages")?.toStringList().orEmpty()

    fun prayerClosings(context: Context): List<String> =
        loadRoot(context).optJSONArray("prayer_closings")?.toStringList().orEmpty()

    fun prayerClosings(context: Context, key: String): List<String> =
        loadRoot(context).optJSONArray("${key}_closings")?.toStringList().orEmpty()

    fun jumaaMessages(context: Context): List<String> =
        loadRoot(context).getJSONArray("jumaa_messages").toStringList()

    fun iftarMessages(context: Context): List<String> =
        loadRoot(context).getJSONArray("iftar_messages").toStringList()

    fun fallbackIftarDua(context: Context): String =
        loadRoot(context).optString("iftar_fallback_dua")

    fun ramadanDailyDua(context: Context, hijriDay: Int): String? {
        val map = loadRoot(context).optJSONObject("ramadan_daily_duas") ?: return null
        return map.optString(hijriDay.toString()).ifBlank { null }
    }

    fun rotatingMessage(messages: List<String>, seed: Int = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)): String {
        if (messages.isEmpty()) return ""
        return messages[seed.absoluteValue % messages.size]
    }

    fun randomMessage(messages: List<String>, salt: Int = 0): String {
        if (messages.isEmpty()) return ""
        val seed = ((System.currentTimeMillis() / 60000L).toInt() + salt).absoluteValue
        return messages[seed % messages.size]
    }

    fun nextMessage(context: Context, poolKey: String, messages: List<String>): String {
        if (messages.isEmpty()) return ""
        if (messages.size == 1) return messages.first()

        val prefs = context.getSharedPreferences(ROTATION_PREFS, Context.MODE_PRIVATE)
        val orderKey = KEY_ORDER_PREFIX + poolKey
        val cursorKey = KEY_CURSOR_PREFIX + poolKey
        val lastKey = KEY_LAST_PREFIX + poolKey

        var order = prefs.getString(orderKey, null)
            ?.split(",")
            ?.mapNotNull { it.toIntOrNull() }
            ?.filter { it in messages.indices }
            .orEmpty()
        var cursor = prefs.getInt(cursorKey, 0).coerceAtLeast(0)
        val lastIndex = prefs.getInt(lastKey, -1)

        if (order.size != messages.size || cursor >= order.size) {
            order = shuffledIndexes(messages.size, lastIndex)
            cursor = 0
        }

        val selectedIndex = order.getOrElse(cursor) { 0 }
        val nextCursor = cursor + 1
        val editor = prefs.edit()
            .putString(orderKey, order.joinToString(","))
            .putInt(lastKey, selectedIndex)

        if (nextCursor >= order.size) {
            val nextOrder = shuffledIndexes(messages.size, selectedIndex)
            editor.putString(orderKey, nextOrder.joinToString(","))
            editor.putInt(cursorKey, 0)
        } else {
            editor.putInt(cursorKey, nextCursor)
        }
        editor.apply()
        return messages.getOrElse(selectedIndex) { messages.first() }
    }

    private fun loadRoot(context: Context): JSONObject {
        val raw = context.assets.open(ASSET_FILE).bufferedReader().use { it.readText() }
        return JSONObject(raw)
    }

    private fun shuffledIndexes(size: Int, avoidFirst: Int): List<Int> {
        val list = (0 until size).toMutableList()
        list.shuffle(Random(System.currentTimeMillis()))
        if (avoidFirst in list.indices && size > 1 && list.first() == avoidFirst) {
            val swapIndex = list.indexOfFirst { it != avoidFirst }.takeIf { it > 0 } ?: 1
            val first = list.first()
            list[0] = list[swapIndex]
            list[swapIndex] = first
        }
        return list
    }

    private fun org.json.JSONArray.toStringList(): List<String> =
        List(length()) { index -> optString(index) }.filter { it.isNotBlank() }
}
