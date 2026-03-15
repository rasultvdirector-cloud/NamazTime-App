package com.muslimtime.app.data

import android.content.Context
import android.location.Geocoder
import android.os.Build
import org.json.JSONObject
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference
import java.util.Locale

object LocationCoordinatesResolver {
    private const val PREFS_NAME = "location_coordinates_cache"
    private val memoryCache = ConcurrentHashMap<String, LocationCoordinates>()

    fun resolve(context: Context, city: String, country: String): Result<LocationCoordinates> {
        return runCatching {
            val query = listOf(city.trim(), country.trim())
                .filter { it.isNotBlank() }
                .joinToString(", ")
            require(query.isNotBlank()) { "Empty location query" }

            memoryCache[query]?.let { return@runCatching it }
            loadFromDisk(context, query)?.let {
                memoryCache[query] = it
                return@runCatching it
            }

            val geocoder = Geocoder(context, Locale.getDefault())
            val address = geocodeByName(geocoder, query)
                ?.firstOrNull()
                ?: error("No coordinates found for $query")

            LocationCoordinates(
                latitude = address.latitude,
                longitude = address.longitude,
            ).also {
                memoryCache[query] = it
                saveToDisk(context, query, it)
            }
        }
    }

    private fun geocodeByName(
        geocoder: Geocoder,
        query: String,
    ): List<android.location.Address>? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val resultRef = AtomicReference<List<android.location.Address>?>(null)
            val latch = CountDownLatch(1)
            geocoder.getFromLocationName(
                query,
                1,
                object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<android.location.Address>) {
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
            geocoder.getFromLocationName(query, 1)
        }
    }

    private fun loadFromDisk(context: Context, query: String): LocationCoordinates? {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(query, null) ?: return null
        return runCatching {
            val json = JSONObject(raw)
            LocationCoordinates(
                latitude = json.getDouble("lat"),
                longitude = json.getDouble("lng"),
            )
        }.getOrNull()
    }

    private fun saveToDisk(context: Context, query: String, coordinates: LocationCoordinates) {
        val raw = JSONObject()
            .put("lat", coordinates.latitude)
            .put("lng", coordinates.longitude)
            .toString()
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(query, raw)
            .apply()
    }
}
