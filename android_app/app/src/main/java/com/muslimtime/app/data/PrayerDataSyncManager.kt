package com.muslimtime.app.data

import android.content.Context
import com.muslimtime.app.notifications.PrayerReminderScheduler
import com.muslimtime.app.notifications.PrayerRefreshScheduler
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object PrayerDataSyncManager {
    fun syncToday(context: Context): Result<CityPrayerTimes> {
        val location = PrayerPreferences.loadSelectedLocation(context)
            ?: return Result.failure(IllegalStateException("Location not selected"))
        val resolvedSource = PrayerTimesRepository.resolvedSource(context, location.country)
        return PrayerTimesRepository.fetchByCity(context, location.city, location.country).map { remote ->
            val localizedNames = PrayerPreferences.localizedPrayerNames(context)
            val localizedTimes = remote.times.mapIndexed { index, item ->
                item.copy(name = localizedNames.getOrElse(index) { item.name })
            }
            CityPrayerTimes(
                city = if (remote.city.isBlank()) location.city else remote.city,
                country = if (remote.country.isBlank()) location.country else remote.country,
                times = localizedTimes,
                imsakTime = remote.imsakTime,
            ).also { updated ->
                PrayerPreferences.saveSelectedPrayerTimes(context, updated)
                if (resolvedSource == PrayerPreferences.PRAYER_SOURCE_QAFQAZ) {
                    PrayerPreferences.setLastAzerbaijanSyncMonth(context, monthKey())
                    PrayerPreferences.setLastAzerbaijanSyncWindow(context, QafqazIslamRepository.monthWindowKey())
                    PrayerPreferences.setLastAzerbaijanAssetVersion(context, QafqazIslamRepository.assetVersion(context))
                } else {
                    PrayerPreferences.setLastPrayerSyncDate(context, todayKey())
                }
                PrayerReminderScheduler.scheduleReminderSet(context, updated)
                if (resolvedSource == PrayerPreferences.PRAYER_SOURCE_ALADHAN) {
                    PrayerRefreshScheduler.scheduleNextDailyRefresh(context)
                }
            }
        }
    }

    fun shouldUseDailyRefreshAlarm(context: Context): Boolean =
        PrayerPreferences.hasCompletedInitialSetup(context) &&
            PrayerPreferences.loadSelectedLocation(context)?.let {
                PrayerTimesRepository.resolvedSource(context, it.country) == PrayerPreferences.PRAYER_SOURCE_ALADHAN
            } == true

    fun shouldUsePeriodicWorker(context: Context): Boolean =
        PrayerPreferences.hasCompletedInitialSetup(context) &&
            PrayerPreferences.loadSelectedLocation(context) != null

    fun isTodaySynced(context: Context): Boolean =
        PrayerPreferences.loadSelectedLocation(context)?.let {
            when (PrayerTimesRepository.resolvedSource(context, it.country)) {
                PrayerPreferences.PRAYER_SOURCE_QAFQAZ ->
                    PrayerPreferences.getLastAzerbaijanSyncWindow(context) == QafqazIslamRepository.monthWindowKey() &&
                        PrayerPreferences.getLastAzerbaijanAssetVersion(context) == QafqazIslamRepository.assetVersion(context) &&
                        PrayerPreferences.loadSelectedPrayerTimes(context) != null &&
                        QafqazIslamRepository.hasCurrentAndNextMonthCoverage(context, it.city, it.country)
                else ->
                    PrayerPreferences.getLastPrayerSyncDate(context) == todayKey() &&
                        PrayerPreferences.loadSelectedPrayerTimes(context) != null
            }
        } == true

    fun needsSync(context: Context): Boolean = !isTodaySynced(context)

    private fun todayKey(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time)
    private fun monthKey(): String = SimpleDateFormat("yyyy-MM", Locale.US).format(Calendar.getInstance().time)
}
