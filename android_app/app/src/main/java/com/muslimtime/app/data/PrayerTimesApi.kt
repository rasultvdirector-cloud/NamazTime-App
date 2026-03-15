package com.muslimtime.app.data

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import java.util.Calendar

data class RemotePrayerTimesResult(
    val city: String,
    val country: String,
    val times: List<PrayerTime>,
    val imsakTime: String? = null,
)

object PrayerTimesApi {
    private const val BASE_URL = "https://api.aladhan.com/v1/timingsByCity"
    private const val CALENDAR_URL = "https://api.aladhan.com/v1/calendarByCity"
    private const val DEFAULT_METHOD = "13"

    fun fetchByCity(city: String, country: String): Result<RemotePrayerTimesResult> {
        return runCatching {
            val url = buildUrl(city, country)
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15000
                readTimeout = 15000
            }

            connection.inputStream.bufferedReader().use { reader ->
                val body = reader.readText()
                parseResponse(body)
            }
        }
    }

    fun fetchByCityAndDate(city: String, country: String, date: Calendar): Result<RemotePrayerTimesResult> {
        return runCatching {
            val url = buildCalendarUrl(city, country, date)
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15000
                readTimeout = 15000
            }

            connection.inputStream.bufferedReader().use { reader ->
                val body = reader.readText()
                parseCalendarResponse(body, date)
            }
        }
    }

    private fun buildUrl(city: String, country: String): String {
        val encodedCity = URLEncoder.encode(city, "UTF-8")
        val encodedCountry = URLEncoder.encode(country, "UTF-8")
        return "$BASE_URL?city=$encodedCity&country=$encodedCountry&method=$DEFAULT_METHOD"
    }

    private fun buildCalendarUrl(city: String, country: String, date: Calendar): String {
        val encodedCity = URLEncoder.encode(city, "UTF-8")
        val encodedCountry = URLEncoder.encode(country, "UTF-8")
        val month = date.get(Calendar.MONTH) + 1
        val year = date.get(Calendar.YEAR)
        return "$CALENDAR_URL?city=$encodedCity&country=$encodedCountry&method=$DEFAULT_METHOD&month=$month&year=$year"
    }

    private fun parseResponse(body: String): RemotePrayerTimesResult {
        val root = JSONObject(body)
        val data = root.getJSONObject("data")
        val meta = data.getJSONObject("meta")
        val timings = data.getJSONObject("timings")

        return RemotePrayerTimesResult(
            city = meta.optString("city"),
            country = meta.optString("country"),
            times = listOf(
                PrayerTime("Fajr", cleanTime(timings.getString("Fajr"))),
                PrayerTime("Sunrise", cleanTime(timings.getString("Sunrise"))),
                PrayerTime("Dhuhr", cleanTime(timings.getString("Dhuhr"))),
                PrayerTime("Asr", cleanTime(timings.getString("Asr"))),
                PrayerTime("Maghrib", cleanTime(timings.getString("Maghrib"))),
                PrayerTime("Isha", cleanTime(timings.getString("Isha"))),
            ),
            imsakTime = cleanTime(timings.optString("Imsak", timings.getString("Fajr"))),
        )
    }

    private fun parseCalendarResponse(body: String, date: Calendar): RemotePrayerTimesResult {
        val root = JSONObject(body)
        val data = root.getJSONArray("data")
        val dayIndex = date.get(Calendar.DAY_OF_MONTH) - 1
        val dayData = data.getJSONObject(dayIndex)
        val meta = dayData.getJSONObject("meta")
        val timings = dayData.getJSONObject("timings")

        return RemotePrayerTimesResult(
            city = meta.optString("city"),
            country = meta.optString("country"),
            times = listOf(
                PrayerTime("Fajr", cleanTime(timings.getString("Fajr"))),
                PrayerTime("Sunrise", cleanTime(timings.getString("Sunrise"))),
                PrayerTime("Dhuhr", cleanTime(timings.getString("Dhuhr"))),
                PrayerTime("Asr", cleanTime(timings.getString("Asr"))),
                PrayerTime("Maghrib", cleanTime(timings.getString("Maghrib"))),
                PrayerTime("Isha", cleanTime(timings.getString("Isha"))),
            ),
            imsakTime = cleanTime(timings.optString("Imsak", timings.getString("Fajr"))),
        )
    }

    private fun cleanTime(value: String): String = value.substringBefore(" ")
}
