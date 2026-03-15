package com.muslimtime.app.data

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object QuranEncRepository {
    private const val BASE_URL = "https://quranenc.com/api/v1"
    private const val CACHE_DIR = "quranenc_cache"
    const val AZERBAIJANI_TRANSLATION_KEY = "azeri_musayev"

    fun fetchTranslations(context: Context, languageIso: String = "az", localization: String = "az"): Result<List<QuranTranslationInfo>> {
        return runCatching {
            val cacheFile = cacheFile(context, "translations_${languageIso}_$localization.json")
            val body = fetchWithCache(cacheFile) {
                val url = "$BASE_URL/translations/list/$languageIso?localization=$localization"
                val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 15000
                    readTimeout = 15000
                }
                connection.inputStream.bufferedReader().use { it.readText() }
            }
            parseTranslations(body)
        }
    }

    fun fetchSura(context: Context, translationKey: String, suraNumber: Int): Result<List<QuranVerse>> {
        return runCatching {
            val cacheFile = cacheFile(context, "sura_${translationKey}_$suraNumber.json")
            val body = fetchWithCache(cacheFile) {
                val url = "$BASE_URL/translation/sura/$translationKey/$suraNumber"
                val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 15000
                    readTimeout = 15000
                }
                connection.inputStream.bufferedReader().use { it.readText() }
            }
            parseSura(body)
        }
    }

    private fun cacheFile(context: Context, name: String): File {
        val dir = File(context.cacheDir, CACHE_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return File(dir, name)
    }

    private fun fetchWithCache(cacheFile: File, networkFetch: () -> String): String {
        return try {
            val body = networkFetch()
            cacheFile.writeText(body)
            body
        } catch (error: Exception) {
            if (cacheFile.exists()) {
                cacheFile.readText()
            } else {
                throw error
            }
        }
    }

    private fun parseTranslations(body: String): List<QuranTranslationInfo> {
        val root = JSONObject(body)
        val translations = root.getJSONArray("translations")
        return buildList(translations.length()) {
            for (index in 0 until translations.length()) {
                val item = translations.getJSONObject(index)
                add(
                    QuranTranslationInfo(
                        key = item.getString("key"),
                        title = item.getString("title"),
                    ),
                )
            }
        }
    }

    private fun parseSura(body: String): List<QuranVerse> {
        val root = JSONObject(body)
        val result = root.getJSONArray("result")
        return buildList(result.length()) {
            for (index in 0 until result.length()) {
                val item = result.getJSONObject(index)
                add(
                    QuranVerse(
                        title = "${item.getString("sura")}:${item.getString("aya")}",
                        arabic = item.getString("arabic_text"),
                        translation = item.getString("translation"),
                    ),
                )
            }
        }
    }
}

data class QuranTranslationInfo(
    val key: String,
    val title: String,
)
