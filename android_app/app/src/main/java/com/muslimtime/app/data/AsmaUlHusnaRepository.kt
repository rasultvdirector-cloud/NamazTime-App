package com.muslimtime.app.data

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object AsmaUlHusnaRepository {
    private const val URL_STRING = "https://www.ummahapi.com/api/asma-ul-husna"

    fun fetchAll(): Result<List<AllahName>> {
        return runCatching {
            val connection = (URL(URL_STRING).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15000
                readTimeout = 15000
            }

            connection.inputStream.bufferedReader().use { reader ->
                parseResponse(reader.readText())
            }
        }
    }

    private fun parseResponse(body: String): List<AllahName> {
        val root = JSONObject(body)
        require(root.optBoolean("success")) { "UmmahAPI asma-ul-husna request failed" }
        val names = root.getJSONObject("data").getJSONArray("names")

        return buildList(names.length()) {
            for (index in 0 until names.length()) {
                val item = names.getJSONObject(index)
                add(
                    AllahName(
                        number = item.getInt("number"),
                        arabic = item.getString("arabic"),
                        transliteration = item.getString("transliteration"),
                        english = item.getString("english"),
                        meaning = item.getString("meaning"),
                    ),
                )
            }
        }
    }
}
