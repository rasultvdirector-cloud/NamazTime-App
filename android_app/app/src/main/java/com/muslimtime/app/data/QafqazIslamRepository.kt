package com.muslimtime.app.data

import android.content.Context
import org.json.JSONObject
import java.util.Calendar

object QafqazIslamRepository {
    private const val ASSET_FILE = "azerbaijan_prayer_times.json"
    private val cityAliases = mapOf(
        "baki" to "baku",
        "baku" to "baku",
        "bakucity" to "baku",
        "bakucity" to "baku",
        "bakuşehəri" to "baku",
        "bakuseheri" to "baku",
        "bakisaheri" to "baku",
        "bakisheheri" to "baku",
        "sebail" to "baku",
        "sabail" to "baku",
        "sahil" to "baku",
        "yasamal" to "baku",
        "nasimi" to "baku",
        "nesimi" to "baku",
        "narimanov" to "baku",
        "nerimanov" to "baku",
        "nizami" to "baku",
        "xetai" to "baku",
        "khatai" to "baku",
        "suraxani" to "baku",
        "surakhani" to "baku",
        "binəqədi" to "baku",
        "bineqedi" to "baku",
        "binagadi" to "baku",
        "qaradag" to "baku",
        "qaradağ" to "baku",
        "garadagh" to "baku",
        "xezar" to "baku",
        "khazar" to "baku",
        "pirallahi" to "baku",
        "gence" to "ganja",
        "ganja" to "ganja",
        "sumqayit" to "sumgait",
        "sumgayit" to "sumgait",
        "sumgait" to "sumgait",
        "naxcivan" to "nakhchivan",
        "nakhchivan" to "nakhchivan",
        "seki" to "sheki",
        "sheki" to "sheki",
        "lenkeran" to "lankaran",
        "lankaran" to "lankaran",
        "mingecevir" to "mingachevir",
        "mingachevir" to "mingachevir",
        "quba" to "quba",
        "qusar" to "qusar",
        "zaqatala" to "zagatala",
        "sirvan" to "shirvan",
        "shirvan" to "shirvan",
        "yevlax" to "yevlakh",
        "yevlakh" to "yevlakh",
    )
    private val countryAliases = setOf(
        "azerbaijan",
        "azərbaycan",
        "azerbaycan",
        "azerbaycanrespublikasi",
        "republicofazerbaijan",
        "азербайджан",
    )

    fun supports(country: String): Boolean {
        val normalized = normalize(country)
        return normalized in countryAliases
    }

    fun monthWindowKey(date: Calendar = Calendar.getInstance()): String {
        val currentYear = date.get(Calendar.YEAR)
        val currentMonth = date.get(Calendar.MONTH) + 1
        val nextDate = date.clone() as Calendar
        nextDate.add(Calendar.MONTH, 1)
        val nextYear = nextDate.get(Calendar.YEAR)
        val nextMonth = nextDate.get(Calendar.MONTH) + 1
        return "%04d-%02d|%04d-%02d".format(currentYear, currentMonth, nextYear, nextMonth)
    }

    fun hasCurrentAndNextMonthCoverage(
        context: Context,
        city: String,
        country: String,
        referenceDate: Calendar = Calendar.getInstance(),
    ): Boolean {
        if (!supports(country)) return false
        val json = loadJson(context)
        val cityObject = findCity(json, city, country) ?: return false
        val currentMonthExists = hasMonthEntry(cityObject, referenceDate)
        val nextDate = (referenceDate.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
        val nextMonthExists = hasMonthEntry(cityObject, nextDate)
        return currentMonthExists && nextMonthExists
    }

    fun getPrayerTimes(context: Context, city: String, country: String, date: Calendar): Result<RemotePrayerTimesResult> {
        return runCatching {
            val json = loadJson(context)
            val resolvedCity = findCity(json, city, country) ?: error("No Azerbaijan schedule found for $city")
            val months = resolvedCity.getJSONArray("months")
            val year = date.get(Calendar.YEAR)
            val month = date.get(Calendar.MONTH) + 1
            var monthObject: JSONObject? = null
            for (index in 0 until months.length()) {
                val item = months.getJSONObject(index)
                if (item.getInt("year") == year && item.getInt("month") == month) {
                    monthObject = item
                    break
                }
            }

            val resolvedMonth = monthObject ?: error("No Azerbaijan schedule found for $city $month/$year")
            val days = resolvedMonth.getJSONArray("days")
            val targetDay = date.get(Calendar.DAY_OF_MONTH)
            var dayObject: JSONObject? = null
            for (index in 0 until days.length()) {
                val item = days.getJSONObject(index)
                if (item.getInt("day") == targetDay) {
                    dayObject = item
                    break
                }
            }

            val day = dayObject ?: error("No day entry found for $targetDay")

            RemotePrayerTimesResult(
                city = resolvedCity.getString("city"),
                country = json.getString("country"),
                times = listOf(
                    PrayerTime("Fajr", day.getString("fajr")),
                    PrayerTime("Sunrise", day.getString("sunrise")),
                    PrayerTime("Dhuhr", day.getString("dhuhr")),
                    PrayerTime("Asr", day.getString("asr")),
                    PrayerTime("Maghrib", day.getString("maghrib")),
                    PrayerTime("Isha", day.getString("isha")),
                ),
                imsakTime = day.optString("imsak").ifBlank { day.getString("fajr") },
            )
        }
    }

    fun assetVersion(context: Context): Int = loadJson(context).optInt("version", 1)

    private fun loadJson(context: Context): JSONObject {
        val root = context.assets.open(ASSET_FILE).bufferedReader().use { it.readText() }
        return JSONObject(root)
    }

    private fun findCity(json: JSONObject, city: String, country: String): JSONObject? {
        val cities = json.getJSONArray("cities")
        val normalizedCity = canonicalCityKey(city)
        val normalizedCountry = canonicalCountryKey(country)
        for (index in 0 until cities.length()) {
            val item = cities.getJSONObject(index)
            if (canonicalCityKey(item.getString("city")) == normalizedCity &&
                canonicalCountryKey(json.getString("country")) == normalizedCountry
            ) {
                return item
            }
        }
        return null
    }

    private fun hasMonthEntry(cityObject: JSONObject, date: Calendar): Boolean {
        val year = date.get(Calendar.YEAR)
        val month = date.get(Calendar.MONTH) + 1
        val months = cityObject.getJSONArray("months")
        for (index in 0 until months.length()) {
            val item = months.getJSONObject(index)
            if (item.getInt("year") == year && item.getInt("month") == month) {
                return true
            }
        }
        return false
    }

    private fun canonicalCityKey(raw: String): String {
        val normalized = normalize(raw)
            .removeSuffix("rayonu")
            .removeSuffix("rayon")
            .removeSuffix("district")
            .removeSuffix("city")
        return cityAliases[normalized] ?: normalized
    }

    private fun canonicalCountryKey(raw: String): String {
        val normalized = normalize(raw)
        return when {
            normalized in countryAliases -> "azerbaijan"
            else -> normalized
        }
    }

    private fun normalize(raw: String): String {
        return raw.trim()
            .lowercase()
            .replace("ə", "e")
            .replace("ı", "i")
            .replace("ö", "o")
            .replace("ü", "u")
            .replace("ğ", "g")
            .replace("ç", "c")
            .replace("ş", "s")
            .replace(Regex("[^a-z0-9]+"), "")
    }
}
