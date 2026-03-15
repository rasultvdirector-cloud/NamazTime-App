package com.muslimtime.app.data

import android.content.Context

object QuranStatePreferences {
    fun getQuranReadFontSizeSp(context: Context): Float =
        prefs(context).getFloat("quran_read_font_sp", 22f).coerceIn(18f, 34f)

    fun setQuranReadFontSizeSp(context: Context, value: Float) {
        prefs(context).edit().putFloat("quran_read_font_sp", value.coerceIn(18f, 34f)).apply()
    }

    fun saveLastQuranAudioPlayback(context: Context, url: String?, ayahKey: String?, suraTitle: String?) {
        prefs(context).edit()
            .putString("quran_audio_last_url", url)
            .putString("quran_audio_last_ayah_key", ayahKey)
            .putString("quran_audio_last_sura_title", suraTitle)
            .apply()
    }

    fun loadLastQuranAudioPlayback(context: Context): Triple<String?, String?, String?> = Triple(
        prefs(context).getString("quran_audio_last_url", null),
        prefs(context).getString("quran_audio_last_ayah_key", null),
        prefs(context).getString("quran_audio_last_sura_title", null),
    )

    private fun prefs(context: Context) =
        context.getSharedPreferences("prayer_prefs", Context.MODE_PRIVATE)
}
