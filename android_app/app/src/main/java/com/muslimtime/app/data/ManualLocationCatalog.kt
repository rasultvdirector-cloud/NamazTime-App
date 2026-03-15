package com.muslimtime.app.data

import java.util.Locale

data class ManualCityOption(
    val label: String,
    val apiValue: String,
)

data class ManualCountryOption(
    val label: String,
    val apiValue: String,
    val cities: List<ManualCityOption>,
)

object ManualLocationCatalog {
    private val countries: List<ManualCountryOption> = listOf(
        ManualCountryOption(
            label = "Almaniya",
            apiValue = "Germany",
            cities = cities("Berlin", "Frankfurt", "Hamburg", "Cologne", "Munich"),
        ),
        ManualCountryOption(
            label = "Amerika Birləşmiş Ştatları",
            apiValue = "United States",
            cities = cities("Chicago", "Houston", "Los Angeles", "New York", "Washington"),
        ),
        ManualCountryOption(
            label = "Azərbaycan",
            apiValue = "Azerbaijan",
            cities = cities(
                "Baku" to "Bakı",
                "Ganja" to "Gəncə",
                "Sumgait" to "Sumqayıt",
                "Mingachevir" to "Mingəçevir",
                "Shaki" to "Şəki",
                "Lankaran" to "Lənkəran",
                "Shirvan" to "Şirvan",
                "Nakhchivan" to "Naxçıvan",
                "Quba" to "Quba",
                "Khachmaz" to "Xaçmaz",
            ),
        ),
        ManualCountryOption(
            label = "Birləşmiş Ərəb Əmirlikləri",
            apiValue = "United Arab Emirates",
            cities = cities("Abu Dhabi", "Ajman", "Dubai", "Sharjah"),
        ),
        ManualCountryOption(
            label = "Birləşmiş Krallıq",
            apiValue = "United Kingdom",
            cities = cities("Birmingham", "Leeds", "Liverpool", "London", "Manchester"),
        ),
        ManualCountryOption(
            label = "Fransa",
            apiValue = "France",
            cities = cities("Lille", "Lyon", "Marseille", "Nice", "Paris"),
        ),
        ManualCountryOption(
            label = "Gürcüstan",
            apiValue = "Georgia",
            cities = cities("Batumi", "Kutaisi", "Rustavi", "Tbilisi"),
        ),
        ManualCountryOption(
            label = "Hindistan",
            apiValue = "India",
            cities = cities("Delhi", "Hyderabad", "Kolkata", "Mumbai", "Bangalore"),
        ),
        ManualCountryOption(
            label = "İndoneziya",
            apiValue = "Indonesia",
            cities = cities("Bandung", "Jakarta", "Medan", "Surabaya", "Yogyakarta"),
        ),
        ManualCountryOption(
            label = "İspaniya",
            apiValue = "Spain",
            cities = cities("Barcelona", "Cordoba", "Madrid", "Malaga", "Seville"),
        ),
        ManualCountryOption(
            label = "İtaliya",
            apiValue = "Italy",
            cities = cities("Bologna", "Milan", "Naples", "Rome", "Turin"),
        ),
        ManualCountryOption(
            label = "Kanada",
            apiValue = "Canada",
            cities = cities("Calgary", "Montreal", "Ottawa", "Toronto", "Vancouver"),
        ),
        ManualCountryOption(
            label = "Misir",
            apiValue = "Egypt",
            cities = cities("Alexandria", "Aswan", "Cairo", "Giza", "Luxor"),
        ),
        ManualCountryOption(
            label = "Pakistan",
            apiValue = "Pakistan",
            cities = cities("Islamabad", "Karachi", "Lahore", "Multan", "Peshawar"),
        ),
        ManualCountryOption(
            label = "Qazaxıstan",
            apiValue = "Kazakhstan",
            cities = cities("Almaty", "Astana", "Atyrau", "Shymkent", "Turkistan"),
        ),
        ManualCountryOption(
            label = "Qırğızıstan",
            apiValue = "Kyrgyzstan",
            cities = cities("Bishkek", "Jalal-Abad", "Karakol", "Osh"),
        ),
        ManualCountryOption(
            label = "Rusiya",
            apiValue = "Russia",
            cities = cities("Kazan", "Makhachkala", "Moscow", "Saint Petersburg", "Ufa"),
        ),
        ManualCountryOption(
            label = "Səudiyyə Ərəbistanı",
            apiValue = "Saudi Arabia",
            cities = cities("Jeddah", "Madinah", "Makkah", "Riyadh", "Taif"),
        ),
        ManualCountryOption(
            label = "Tacikistan",
            apiValue = "Tajikistan",
            cities = cities("Dushanbe", "Khujand", "Kulob"),
        ),
        ManualCountryOption(
            label = "Türkiyə",
            apiValue = "Turkey",
            cities = cities("Ankara", "Antalya", "Bursa", "Istanbul", "Izmir", "Konya"),
        ),
        ManualCountryOption(
            label = "Özbəkistan",
            apiValue = "Uzbekistan",
            cities = cities("Bukhara", "Namangan", "Samarkand", "Tashkent"),
        ),
    ).sortedBy { it.label.lowercase(Locale("az")) }

    fun countries(): List<ManualCountryOption> = countries

    fun findCountry(savedCountry: String): ManualCountryOption? {
        return countries.firstOrNull {
            it.apiValue.equals(savedCountry, ignoreCase = true) ||
                it.label.equals(savedCountry, ignoreCase = true)
        }
    }

    fun findCity(country: ManualCountryOption, savedCity: String): ManualCityOption? {
        return country.cities.firstOrNull {
            it.apiValue.equals(savedCity, ignoreCase = true) ||
                it.label.equals(savedCity, ignoreCase = true)
        }
    }

    private fun cities(vararg values: String): List<ManualCityOption> =
        values.map { ManualCityOption(label = it, apiValue = it) }.sortedBy { it.label.lowercase(Locale("az")) }

    private fun cities(vararg values: Pair<String, String>): List<ManualCityOption> =
        values.map { ManualCityOption(label = it.second, apiValue = it.first) }
            .sortedBy { it.label.lowercase(Locale("az")) }
}
