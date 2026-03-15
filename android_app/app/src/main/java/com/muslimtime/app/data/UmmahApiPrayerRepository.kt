package com.muslimtime.app.data

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object UmmahApiPrayerRepository {
    private const val BASE_URL = "https://www.ummahapi.com/api/prayer-times"
    private const val DEFAULT_MADHAB = "Hanafi"
    private const val DEFAULT_METHOD = "MuslimWorldLeague"

    fun fetchByCoordinates(
        city: String,
        country: String,
        coordinates: LocationCoordinates,
    ): Result<RemotePrayerTimesResult> {
        return runCatching {
            val url = buildUrl(coordinates)
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15000
                readTimeout = 15000
            }

            connection.inputStream.bufferedReader().use { reader ->
                parseResponse(city, country, reader.readText())
            }
        }
    }

    private fun buildUrl(coordinates: LocationCoordinates): String {
        return "$BASE_URL?lat=${coordinates.latitude}&lng=${coordinates.longitude}&madhab=$DEFAULT_MADHAB&method=$DEFAULT_METHOD"
    }

    private fun parseResponse(city: String, country: String, body: String): RemotePrayerTimesResult {
        val root = JSONObject(body)
        require(root.optBoolean("success")) { "UmmahAPI prayer-times request failed" }
        val data = root.getJSONObject("data")
        val prayerTimes = data.getJSONObject("prayer_times")

        return RemotePrayerTimesResult(
            city = city,
            country = country,
            times = listOf(
                PrayerTime("Fajr", prayerTimes.getString("fajr")),
                PrayerTime("Sunrise", prayerTimes.getString("sunrise")),
                PrayerTime("Dhuhr", prayerTimes.getString("dhuhr")),
                PrayerTime("Asr", prayerTimes.getString("asr")),
                PrayerTime("Maghrib", prayerTimes.getString("maghrib")),
                PrayerTime("Isha", prayerTimes.getString("isha")),
            ),
            imsakTime = prayerTimes.getString("fajr"),
        )
    }
}
