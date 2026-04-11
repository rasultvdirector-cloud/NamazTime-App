package com.muslimtime.app.data

import android.content.Context
import java.util.Calendar
import java.util.concurrent.ConcurrentHashMap

object PrayerTimesRepository {
    private data class CachedPrayerResult(
        val result: RemotePrayerTimesResult,
        val savedAtMs: Long,
    )

    private val responseCache = ConcurrentHashMap<String, CachedPrayerResult>()
    private const val TODAY_CACHE_MS = 3 * 60 * 1000L
    private const val OTHER_DAY_CACHE_MS = 12 * 60 * 60 * 1000L

    fun fetchByCity(context: Context, city: String, country: String): Result<RemotePrayerTimesResult> {
        return fetchByCityAndDate(context, city, country, Calendar.getInstance())
    }

    fun fetchByCityAndDate(context: Context, city: String, country: String, date: Calendar): Result<RemotePrayerTimesResult> {
        val cacheKey = buildCacheKey(context, city, country, date)
        responseCache[cacheKey]?.takeIf { isCacheFresh(it, date) }?.let { cached ->
            return Result.success(cached.result)
        }

        val chain = sourceChain(context, city, country, date)
        var lastError: Throwable? = null
        for (source in chain) {
            val result = when (source) {
                PrayerPreferences.PRAYER_SOURCE_QAFQAZ -> QafqazIslamRepository.getPrayerTimes(context, city, country, date)
                PrayerPreferences.PRAYER_SOURCE_UMMAH -> fetchFromUmmah(context, city, country)
                else -> fetchFromAlAdhan(city, country, date)
            }
            if (result.isSuccess) {
                result.getOrNull()?.let { resolved ->
                    responseCache[cacheKey] = CachedPrayerResult(
                        result = resolved,
                        savedAtMs = System.currentTimeMillis(),
                    )
                }
                return result
            }
            lastError = result.exceptionOrNull()
        }
        return Result.failure(lastError ?: IllegalStateException("No prayer time source available"))
    }

    fun resolvedSource(
        context: Context,
        city: String,
        country: String,
        date: Calendar = Calendar.getInstance(),
    ): String = sourceChain(context, city, country, date).first()

    private fun sourceChain(
        context: Context,
        city: String,
        country: String,
        date: Calendar,
    ): List<String> {
        val supportsQafqaz = QafqazIslamRepository.supports(country) &&
            QafqazIslamRepository.hasCoverage(context, city, country, date)
        return when (PrayerPreferences.getSelectedPrayerSource(context)) {
            PrayerPreferences.PRAYER_SOURCE_QAFQAZ ->
                if (supportsQafqaz) listOf(PrayerPreferences.PRAYER_SOURCE_QAFQAZ, PrayerPreferences.PRAYER_SOURCE_ALADHAN, PrayerPreferences.PRAYER_SOURCE_UMMAH)
                else listOf(PrayerPreferences.PRAYER_SOURCE_ALADHAN, PrayerPreferences.PRAYER_SOURCE_UMMAH)
            PrayerPreferences.PRAYER_SOURCE_ALADHAN ->
                listOf(PrayerPreferences.PRAYER_SOURCE_ALADHAN, PrayerPreferences.PRAYER_SOURCE_UMMAH)
            PrayerPreferences.PRAYER_SOURCE_UMMAH ->
                listOf(PrayerPreferences.PRAYER_SOURCE_UMMAH, PrayerPreferences.PRAYER_SOURCE_ALADHAN)
            else ->
                if (supportsQafqaz) {
                    listOf(PrayerPreferences.PRAYER_SOURCE_QAFQAZ, PrayerPreferences.PRAYER_SOURCE_ALADHAN, PrayerPreferences.PRAYER_SOURCE_UMMAH)
                } else {
                    listOf(PrayerPreferences.PRAYER_SOURCE_ALADHAN, PrayerPreferences.PRAYER_SOURCE_UMMAH)
                }
        }
    }

    private fun fetchFromAlAdhan(city: String, country: String, date: Calendar): Result<RemotePrayerTimesResult> {
        return if (isToday(date)) {
            PrayerTimesApi.fetchByCity(city, country)
        } else {
            PrayerTimesApi.fetchByCityAndDate(city, country, date)
        }
    }

    private fun fetchFromUmmah(context: Context, city: String, country: String): Result<RemotePrayerTimesResult> {
        return LocationCoordinatesResolver.resolve(context, city, country).fold(
            onSuccess = { coordinates ->
                UmmahApiPrayerRepository.fetchByCoordinates(city, country, coordinates)
            },
            onFailure = { Result.failure(it) },
        )
    }

    private fun isToday(date: Calendar): Boolean {
        val today = Calendar.getInstance()
        return today.get(Calendar.YEAR) == date.get(Calendar.YEAR) &&
            today.get(Calendar.DAY_OF_YEAR) == date.get(Calendar.DAY_OF_YEAR)
    }

    private fun buildCacheKey(context: Context, city: String, country: String, date: Calendar): String {
        return listOf(
            resolvedSource(context, city, country, date),
            city.trim().lowercase(),
            country.trim().lowercase(),
            date.get(Calendar.YEAR).toString(),
            date.get(Calendar.MONTH).toString(),
            date.get(Calendar.DAY_OF_MONTH).toString(),
        ).joinToString("|")
    }

    private fun isCacheFresh(cached: CachedPrayerResult, date: Calendar): Boolean {
        val maxAge = if (isToday(date)) TODAY_CACHE_MS else OTHER_DAY_CACHE_MS
        return System.currentTimeMillis() - cached.savedAtMs <= maxAge
    }
}
