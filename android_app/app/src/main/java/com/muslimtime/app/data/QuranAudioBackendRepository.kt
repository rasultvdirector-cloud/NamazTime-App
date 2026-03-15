package com.muslimtime.app.data

import android.content.Context
import com.muslimtime.app.R
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object QuranAudioBackendRepository {
    private const val AUDIO_SURAH_URL = "https://api.alquran.cloud/v1/surah/%d/ar.alafasy"
    private const val CACHE_DIR = "quran_audio_cache"
    private const val AYAH_CACHE_MAX_AGE_MS = 7L * 24L * 60L * 60L * 1000L

    fun fetchSuras(context: Context): Result<List<AudioSuraItem>> {
        return runCatching {
            quranSuras().map { sura ->
                AudioSuraItem(
                    suraNumber = sura.number,
                    nameArabic = "",
                    nameLatin = sura.name,
                    ayahCount = 0,
                )
            }
        }
    }

    fun fetchAyahs(context: Context, suraNumber: Int): Result<List<AudioAyahItem>> {
        return runCatching {
            loadCachedAyahs(context, suraNumber)?.let { return@runCatching it }

            val audioRoot = requestJson(AUDIO_SURAH_URL.format(suraNumber))
            val audioData = audioRoot.getJSONObject("data")
            val audioAyahs = audioData.getJSONArray("ayahs")

            val translationMap = QuranEncRepository
                .fetchSura(context, QuranEncRepository.AZERBAIJANI_TRANSLATION_KEY, suraNumber)
                .getOrElse { emptyList() }
                .associateBy { verse ->
                    verse.title.substringAfter(" ").toIntOrNull()
                }

            buildList(audioAyahs.length()) {
                for (index in 0 until audioAyahs.length()) {
                    val item = audioAyahs.getJSONObject(index)
                    val ayahNumber = item.getInt("numberInSurah")
                    val translation = translationMap[ayahNumber]?.translation.orEmpty()
                    add(
                        AudioAyahItem(
                            ayahNumber = ayahNumber,
                            ayahKey = "$suraNumber:$ayahNumber",
                            arabicText = item.getString("text").trim(),
                            translation = translation,
                            audioUrl = item.getString("audio"),
                            durationMs = null,
                        ),
                    )
                }
            }.ifEmpty {
                fallbackAyahs(context)
            }.also { ayahs ->
                saveAyahsToCache(context, suraNumber, ayahs)
            }
        }.recoverCatching {
            loadCachedAyahs(context, suraNumber) ?: fallbackAyahs(context)
        }
    }

    private fun requestJson(url: String): JSONObject {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15000
            readTimeout = 15000
        }
        val bodyStream = if (connection.responseCode in 200..299) {
            connection.inputStream
        } else {
            connection.errorStream ?: error("Audio source HTTP ${connection.responseCode}")
        }
        val body = bodyStream.bufferedReader().use { it.readText() }
        return JSONObject(body)
    }

    private fun loadCachedAyahs(context: Context, suraNumber: Int): List<AudioAyahItem>? {
        val cacheFile = cacheFile(context, suraNumber)
        if (!cacheFile.exists()) return null
        if (System.currentTimeMillis() - cacheFile.lastModified() > AYAH_CACHE_MAX_AGE_MS) return null

        return runCatching {
            val root = JSONObject(cacheFile.readText())
            val items = root.getJSONArray("ayahs")
            buildList(items.length()) {
                for (index in 0 until items.length()) {
                    val item = items.getJSONObject(index)
                    add(
                        AudioAyahItem(
                            ayahNumber = item.getInt("ayahNumber"),
                            ayahKey = item.getString("ayahKey"),
                            arabicText = item.getString("arabicText"),
                            translation = item.getString("translation"),
                            audioUrl = item.getString("audioUrl"),
                            durationMs = item.optLong("durationMs").takeIf { it > 0 },
                        ),
                    )
                }
            }
        }.getOrNull()
    }

    private fun saveAyahsToCache(context: Context, suraNumber: Int, ayahs: List<AudioAyahItem>) {
        runCatching {
            val root = JSONObject().apply {
                put("suraNumber", suraNumber)
                put(
                    "ayahs",
                    JSONArray().apply {
                        ayahs.forEach { ayah ->
                            put(
                                JSONObject().apply {
                                    put("ayahNumber", ayah.ayahNumber)
                                    put("ayahKey", ayah.ayahKey)
                                    put("arabicText", ayah.arabicText)
                                    put("translation", ayah.translation)
                                    put("audioUrl", ayah.audioUrl)
                                    put("durationMs", ayah.durationMs ?: JSONObject.NULL)
                                },
                            )
                        }
                    },
                )
            }
            val dir = File(context.cacheDir, CACHE_DIR).apply { mkdirs() }
            File(dir, "sura_${suraNumber}.json").writeText(root.toString())
        }
    }

    private fun cacheFile(context: Context, suraNumber: Int): File {
        val dir = File(context.cacheDir, CACHE_DIR).apply { mkdirs() }
        return File(dir, "sura_${suraNumber}.json")
    }

    private fun fallbackAyahs(context: Context): List<AudioAyahItem> {
        val audioUrl = "android.resource://${context.packageName}/${R.raw.azan_short_1}"
        return listOf(
            AudioAyahItem(1, "1:1", "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ", "Mərhəmətli və Rəhmli Allahın adı ilə.", audioUrl, 4500),
            AudioAyahItem(2, "1:2", "الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ", "Həmd aləmlərin Rəbbi Allaha məxsusdur.", audioUrl, 4500),
            AudioAyahItem(3, "1:3", "الرَّحْمَٰنِ الرَّحِيمِ", "Mərhəmətli, Rəhmli olana.", audioUrl, 4500),
            AudioAyahItem(4, "1:4", "مَالِكِ يَوْمِ الدِّينِ", "Din gününün sahibinə.", audioUrl, 4500),
            AudioAyahItem(5, "1:5", "إِيَّاكَ نَعْبُدُ وَإِيَّاكَ نَسْتَعِينُ", "Yalnız Sənə ibadət edir və yalnız Səndən kömək diləyirik.", audioUrl, 4500),
            AudioAyahItem(6, "1:6", "اهْدِنَا الصِّرَاطَ الْمُسْتَقِيمَ", "Bizi doğru yola yönəlt.", audioUrl, 4500),
            AudioAyahItem(7, "1:7", "صِرَاطَ الَّذِينَ أَنْعَمْتَ عَلَيْهِمْ غَيْرِ الْمَغْضُوبِ عَلَيْهِمْ وَلَا الضَّالِّينَ", "Nemət verdiklərinin yoluna; qəzəbə uğrayanların və azanların yoluna yox.", audioUrl, 6000),
        )
    }
}
