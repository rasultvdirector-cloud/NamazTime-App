package com.muslimtime.app.ui

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import com.muslimtime.app.data.AppLocation
import com.muslimtime.app.data.CityPrayerTimes
import com.muslimtime.app.data.PrayerPreferences
import com.muslimtime.app.data.PrayerTimesRepository
import com.muslimtime.app.notifications.PrayerRefreshScheduler
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

internal data class AutoLocationFlowState(
    var requestInFlight: Boolean = false,
    var lastRequestAtMs: Long = 0L,
    var lastResolvedLocation: AppLocation? = null,
    var lastResolvedLocationAtMs: Long = 0L,
)

internal fun shouldStartAutoLocationRequest(
    state: AutoLocationFlowState,
    nowMs: Long,
    debounceMs: Long,
): Boolean {
    if (state.requestInFlight) return false
    if (nowMs - state.lastRequestAtMs < debounceMs) return false
    state.lastRequestAtMs = nowMs
    return true
}

internal fun resolveLastKnownLocationForSettings(context: Context): Location? {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
    val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
    return providers.mapNotNull { provider ->
        try {
            locationManager.getLastKnownLocation(provider)
        } catch (_: SecurityException) {
            null
        }
    }.maxByOrNull { it.time }
}

internal fun detectCurrentAppLocation(
    context: Context,
    state: AutoLocationFlowState,
    cacheMs: Long,
    fallbackLocation: () -> AppLocation,
    postToUi: (() -> Unit) -> Unit,
    onResolved: (AppLocation) -> Unit,
    onFinished: () -> Unit = {},
) {
    val cached = state.lastResolvedLocation
    if (cached != null && System.currentTimeMillis() - state.lastResolvedLocationAtMs < cacheMs) {
        onResolved(cached)
        onFinished()
        return
    }

    state.requestInFlight = true
    thread {
        val detected = try {
            val currentLocation = resolveLastKnownLocationForSettings(context)
            if (currentLocation != null) {
                reverseGeocodeToAppLocation(context, currentLocation)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }

        val resolved = detected ?: fallbackLocation()
        postToUi {
            state.requestInFlight = false
            state.lastResolvedLocation = resolved
            state.lastResolvedLocationAtMs = System.currentTimeMillis()
            onResolved(resolved)
            onFinished()
        }
    }
}

internal fun fetchAndPersistPrayerLocation(
    context: Context,
    location: AppLocation,
    isAutoDetected: Boolean,
    postToUi: (() -> Unit) -> Unit,
    scheduleReminderSet: (CityPrayerTimes) -> Unit,
    onSuccess: (CityPrayerTimes) -> Unit,
    onFailure: () -> Unit,
) {
    PrayerPreferences.saveLocation(context, location, isAutoDetected = isAutoDetected)
    thread {
        val result = PrayerTimesRepository.fetchByCity(context, location.city, location.country)
        postToUi {
            result.onSuccess { remote ->
                val localizedNames = PrayerPreferences.localizedPrayerNames(context)
                val localizedTimes = remote.times.mapIndexed { index, item ->
                    item.copy(name = localizedNames.getOrElse(index) { item.name })
                }
                val updated = CityPrayerTimes(
                    city = if (remote.city.isBlank()) location.city else remote.city,
                    country = if (remote.country.isBlank()) location.country else remote.country,
                    times = localizedTimes,
                    imsakTime = remote.imsakTime,
                )
                PrayerPreferences.saveSelectedPrayerTimes(context, updated)
                val resolvedSource = PrayerTimesRepository.resolvedSource(context, updated.city, updated.country)
                if (resolvedSource == PrayerPreferences.PRAYER_SOURCE_QAFQAZ) {
                    PrayerPreferences.setLastAzerbaijanSyncMonth(
                        context,
                        SimpleDateFormat("yyyy-MM", Locale.US).format(Calendar.getInstance().time),
                    )
                } else {
                    PrayerPreferences.setLastPrayerSyncDate(
                        context,
                        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time),
                    )
                }
                scheduleReminderSet(updated)
                if (resolvedSource == PrayerPreferences.PRAYER_SOURCE_ALADHAN) {
                    PrayerRefreshScheduler.scheduleNextDailyRefresh(context)
                }
                onSuccess(updated)
            }.onFailure {
                onFailure()
            }
        }
    }
}

private fun reverseGeocodeToAppLocation(
    context: Context,
    location: Location,
): AppLocation? {
    val geocoder = Geocoder(context, Locale.getDefault())
    val addresses = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val resultRef = AtomicReference<List<Address>?>(null)
        val latch = CountDownLatch(1)
        geocoder.getFromLocation(
            location.latitude,
            location.longitude,
            1,
            object : Geocoder.GeocodeListener {
                override fun onGeocode(addresses: MutableList<Address>) {
                    resultRef.set(addresses)
                    latch.countDown()
                }

                override fun onError(errorMessage: String?) {
                    latch.countDown()
                }
            },
        )
        latch.await(5, TimeUnit.SECONDS)
        resultRef.get()
    } else {
        @Suppress("DEPRECATION")
        geocoder.getFromLocation(location.latitude, location.longitude, 1)
    }
    val address = addresses?.firstOrNull() ?: return null
    val city = address.locality ?: address.subAdminArea ?: address.adminArea ?: return null
    val country = address.countryName ?: return null
    return AppLocation(city, country)
}
