package com.muslimtime.app.data

import android.content.Context
import android.icu.util.IslamicCalendar
import com.muslimtime.app.R
import java.util.Calendar

data class PrayerTime(val name: String, val time: String)

data class CityPrayerTimes(
    val city: String,
    val country: String,
    val times: List<PrayerTime>,
    val imsakTime: String? = null,
)

data class AppLocation(
    val city: String,
    val country: String,
)

data class LocationCoordinates(
    val latitude: Double,
    val longitude: Double,
)

data class AudioTrack(val title: String, val narrator: String)
data class AudioSuraItem(
    val suraNumber: Int,
    val nameArabic: String,
    val nameLatin: String,
    val ayahCount: Int,
)
data class AudioAyahItem(
    val ayahNumber: Int,
    val ayahKey: String,
    val arabicText: String,
    val translation: String,
    val audioUrl: String,
    val durationMs: Long? = null,
)
data class QuranSura(val number: Int, val name: String)
data class QuranVerse(val title: String, val arabic: String, val translation: String)
data class QuranAudioAyah(val label: String, val reciter: String)
data class AllahName(
    val number: Int,
    val arabic: String,
    val transliteration: String,
    val english: String,
    val meaning: String,
)

data class AppLanguage(val code: String, val label: String)
data class AppAzanSound(val rawName: String, val label: String)
data class DailyDuaContent(
    val dayLabel: String,
    val dailyTitle: String,
    val dailyBody: String,
    val ramadanTitle: String?,
    val ramadanBody: String?,
    val iftarTitle: String?,
    val iftarBody: String?,
    val extraDuas: List<String>,
)

fun samplePrayerTimes(context: Context): List<CityPrayerTimes> {
    return listOf(
        CityPrayerTimes(
            context.getString(R.string.city_baku),
            context.getString(R.string.country_azerbaijan),
            listOf(
                PrayerTime(context.getString(R.string.prayer_name_fajr), "05:22"),
                PrayerTime(context.getString(R.string.prayer_name_sunrise), "06:51"),
                PrayerTime(context.getString(R.string.prayer_name_dhuhr), "13:18"),
                PrayerTime(context.getString(R.string.prayer_name_asr), "16:53"),
                PrayerTime(context.getString(R.string.prayer_name_maghrib_iftar), "19:10"),
                PrayerTime(context.getString(R.string.prayer_name_isha), "20:41"),
            ),
            imsakTime = "05:17",
        ),
        CityPrayerTimes(
            context.getString(R.string.city_london),
            context.getString(R.string.country_united_kingdom),
            listOf(
                PrayerTime(context.getString(R.string.prayer_name_fajr), "06:02"),
                PrayerTime(context.getString(R.string.prayer_name_sunrise), "07:43"),
                PrayerTime(context.getString(R.string.prayer_name_dhuhr), "13:03"),
                PrayerTime(context.getString(R.string.prayer_name_asr), "16:29"),
                PrayerTime(context.getString(R.string.prayer_name_maghrib_iftar), "18:54"),
                PrayerTime(context.getString(R.string.prayer_name_isha), "20:14"),
            ),
            imsakTime = "06:02",
        ),
    )
}

fun sampleDuas(context: Context): List<String> =
    AzerbaijaniDuaRepository.dailyMessages(context).ifEmpty {
        listOf(
            context.getString(R.string.sample_dua_1),
            context.getString(R.string.sample_dua_2),
            context.getString(R.string.sample_dua_3),
        )
    }

fun sampleAyat(context: Context): List<QuranVerse> = listOf(
    QuranVerse(
        title = context.getString(R.string.sample_verse_title_1),
        arabic = context.getString(R.string.sample_verse_arabic_1),
        translation = context.getString(R.string.sample_ayat_1),
    ),
    QuranVerse(
        title = context.getString(R.string.sample_verse_title_2),
        arabic = context.getString(R.string.sample_verse_arabic_2),
        translation = context.getString(R.string.sample_ayat_2),
    ),
    QuranVerse(
        title = context.getString(R.string.sample_verse_title_3),
        arabic = context.getString(R.string.sample_verse_arabic_3),
        translation = context.getString(R.string.sample_ayat_3),
    ),
)

fun quranSuras(): List<QuranSura> = listOf(
    QuranSura(1, "Əl-Fatihə"),
    QuranSura(2, "Əl-Bəqərə"),
    QuranSura(3, "Ali-İmran"),
    QuranSura(4, "Ən-Nisa"),
    QuranSura(5, "Əl-Maidə"),
    QuranSura(6, "Əl-Ənam"),
    QuranSura(7, "Əl-Əraf"),
    QuranSura(8, "Əl-Ənfal"),
    QuranSura(9, "Ət-Tövbə"),
    QuranSura(10, "Yunus"),
    QuranSura(11, "Hud"),
    QuranSura(12, "Yusuf"),
    QuranSura(13, "Ər-Rəd"),
    QuranSura(14, "İbrahim"),
    QuranSura(15, "Əl-Hicr"),
    QuranSura(16, "Ən-Nəhl"),
    QuranSura(17, "Əl-İsra"),
    QuranSura(18, "Əl-Kəhf"),
    QuranSura(19, "Məryəm"),
    QuranSura(20, "Taha"),
    QuranSura(21, "Əl-Ənbiya"),
    QuranSura(22, "Əl-Həcc"),
    QuranSura(23, "Əl-Muminun"),
    QuranSura(24, "Ən-Nur"),
    QuranSura(25, "Əl-Furqan"),
    QuranSura(26, "Əş-Şuəra"),
    QuranSura(27, "Ən-Nəml"),
    QuranSura(28, "Əl-Qəsəs"),
    QuranSura(29, "Əl-Ənkəbut"),
    QuranSura(30, "Ər-Rum"),
    QuranSura(31, "Loğman"),
    QuranSura(32, "Əs-Səcdə"),
    QuranSura(33, "Əl-Əhzab"),
    QuranSura(34, "Səba"),
    QuranSura(35, "Fatir"),
    QuranSura(36, "Yasin"),
    QuranSura(37, "Əs-Saffat"),
    QuranSura(38, "Sad"),
    QuranSura(39, "Əz-Zumər"),
    QuranSura(40, "Ğafir"),
    QuranSura(41, "Fussilət"),
    QuranSura(42, "Əş-Şura"),
    QuranSura(43, "Əz-Zuxruf"),
    QuranSura(44, "Əd-Duxan"),
    QuranSura(45, "Əl-Casiyə"),
    QuranSura(46, "Əl-Əhqaf"),
    QuranSura(47, "Muhəmməd"),
    QuranSura(48, "Əl-Fəth"),
    QuranSura(49, "Əl-Hucurat"),
    QuranSura(50, "Qaf"),
    QuranSura(51, "Əz-Zariyat"),
    QuranSura(52, "Ət-Tur"),
    QuranSura(53, "Ən-Nəcm"),
    QuranSura(54, "Əl-Qəmər"),
    QuranSura(55, "Ər-Rəhman"),
    QuranSura(56, "Əl-Vaqiə"),
    QuranSura(57, "Əl-Hədid"),
    QuranSura(58, "Əl-Mücadilə"),
    QuranSura(59, "Əl-Həşr"),
    QuranSura(60, "Əl-Mumtəhinə"),
    QuranSura(61, "Əs-Saff"),
    QuranSura(62, "Əl-Cümə"),
    QuranSura(63, "Əl-Münafiqun"),
    QuranSura(64, "Ət-Təğabun"),
    QuranSura(65, "Ət-Talaq"),
    QuranSura(66, "Ət-Təhrim"),
    QuranSura(67, "Əl-Mülk"),
    QuranSura(68, "Əl-Qələm"),
    QuranSura(69, "Əl-Haqqə"),
    QuranSura(70, "Əl-Məaric"),
    QuranSura(71, "Nuh"),
    QuranSura(72, "Əl-Cinn"),
    QuranSura(73, "Əl-Muzzəmmil"),
    QuranSura(74, "Əl-Muddəssir"),
    QuranSura(75, "Əl-Qiyamə"),
    QuranSura(76, "Əl-İnsan"),
    QuranSura(77, "Əl-Mursəlat"),
    QuranSura(78, "Ən-Nəbə"),
    QuranSura(79, "Ən-Naziat"),
    QuranSura(80, "Əbəsə"),
    QuranSura(81, "Ət-Təkvir"),
    QuranSura(82, "Əl-İnfitar"),
    QuranSura(83, "Əl-Mutəffifin"),
    QuranSura(84, "Əl-İnşiqaq"),
    QuranSura(85, "Əl-Buruc"),
    QuranSura(86, "Ət-Tariq"),
    QuranSura(87, "Əl-Əla"),
    QuranSura(88, "Əl-Ğaşiyə"),
    QuranSura(89, "Əl-Fəcr"),
    QuranSura(90, "Əl-Bələd"),
    QuranSura(91, "Əş-Şəms"),
    QuranSura(92, "Əl-Leyl"),
    QuranSura(93, "Əd-Duha"),
    QuranSura(94, "Əş-Şərh"),
    QuranSura(95, "Ət-Tin"),
    QuranSura(96, "Əl-Ələq"),
    QuranSura(97, "Əl-Qədr"),
    QuranSura(98, "Əl-Bəyyinə"),
    QuranSura(99, "Əz-Zəlzələ"),
    QuranSura(100, "Əl-Adiyat"),
    QuranSura(101, "Əl-Qariə"),
    QuranSura(102, "Ət-Təkasur"),
    QuranSura(103, "Əl-Əsr"),
    QuranSura(104, "Əl-Huməzə"),
    QuranSura(105, "Əl-Fil"),
    QuranSura(106, "Qureyş"),
    QuranSura(107, "Əl-Maun"),
    QuranSura(108, "Əl-Kövsər"),
    QuranSura(109, "Əl-Kafirun"),
    QuranSura(110, "Ən-Nəsr"),
    QuranSura(111, "Əl-Məsəd"),
    QuranSura(112, "Əl-İxlas"),
    QuranSura(113, "Əl-Fələq"),
    QuranSura(114, "Ən-Nas"),
)

fun allahNameAzerbaijaniTitle(number: Int): String {
    val titles = listOf(
        "Mərhəmətli", "Rəhmli", "Hökmdar", "Müqəddəs", "Salamətlik verən", "Təmin edən",
        "Qoruyan", "Qüdrətli", "İradə sahibi", "Uca", "Yaradan", "Yoxdan var edən",
        "Surət verən", "Çox bağışlayan", "Qəhredən", "Bəxş edən", "Ruzi verən",
        "Açan", "Hər şeyi bilən", "Daraldan", "Genişləndirən", "Alçaldan", "Ucaldan",
        "İzzət verən", "Zəlil edən", "Eşidən", "Görən", "Hökm verən", "Ədalətli",
        "Lütf edən", "Hər şeydən xəbərdar", "Həlim", "Əzəmətli", "Bağışlayan",
        "Qədrbilən", "Uca", "Böyük", "Qoruyan", "Qüvvət verən", "Hesab çəkən",
        "Cəlal sahibi", "Kərəm sahibi", "Nəzarət edən", "Dua qəbul edən",
        "Hər şeyi əhatə edən", "Hikmət sahibi", "Sevən", "Şan-şöhrət sahibi",
        "Dirildən", "Şahid", "Haqq", "Vəkil", "Qüvvətli", "Mətin", "Dost və yardımçı",
        "Tərifə layiq", "Sayıb bilən", "Başladan", "Qaytaran", "Həyat verən",
        "Öldürən", "Diri", "Qaim", "Tapıb verən", "Şərəf sahibi", "Tək", "Yeganə",
        "Heç nəyə möhtac olmayan", "Qadir", "Tam qüdrət sahibi", "Önə keçirən",
        "Geri salan", "Əvvəl", "Axır", "Aşkar", "Gizli", "İdarə edən", "Ən uca",
        "Yaxşılıq edən", "Tövbələri qəbul edən", "Cəza verən", "Əfv edən",
        "Şəfqətli", "Mülkün sahibi", "Cəlal və ikram sahibi", "İnsaf sahibi",
        "Toplayan", "Zəngin", "Varlı edən", "Mane olan", "Zərər verən", "Fayda verən",
        "Nur", "Hidayət edən", "Bənzərsiz yaradan", "Əbədi qalan", "Varis", "Doğru yola yönəldən",
        "Səbir sahibi",
    )
    return titles.getOrElse(number - 1) { "" }
}

fun sampleAudioAyat(context: Context): List<QuranAudioAyah> = listOf(
    QuranAudioAyah(
        label = context.getString(R.string.sample_audio_ayah_1),
        reciter = context.getString(R.string.audio_track_narrator_1),
    ),
    QuranAudioAyah(
        label = context.getString(R.string.sample_audio_ayah_2),
        reciter = context.getString(R.string.audio_track_narrator_2),
    ),
    QuranAudioAyah(
        label = context.getString(R.string.sample_audio_ayah_3),
        reciter = context.getString(R.string.audio_track_narrator_3),
    ),
)

fun sampleAudioTracks(context: Context): List<AudioTrack> {
    return listOf(
        AudioTrack(
            title = context.getString(R.string.audio_track_title_1),
            narrator = context.getString(R.string.audio_track_narrator_1),
        ),
        AudioTrack(
            title = context.getString(R.string.audio_track_title_2),
            narrator = context.getString(R.string.audio_track_narrator_2),
        ),
        AudioTrack(
            title = context.getString(R.string.audio_track_title_3),
            narrator = context.getString(R.string.audio_track_narrator_3),
        ),
    )
}

fun supportedLanguages(context: Context): List<AppLanguage> = listOf(
    AppLanguage("az", context.getString(R.string.language_az)),
    AppLanguage("en", context.getString(R.string.language_en)),
    AppLanguage("ru", context.getString(R.string.language_ru)),
    AppLanguage("tr", context.getString(R.string.language_tr)),
)

fun supportedAzanSounds(context: Context): List<AppAzanSound> = listOf(
    AppAzanSound("azan_full", context.getString(R.string.azan_sound_option_1)),
    AppAzanSound("azan_full_2", context.getString(R.string.azan_sound_option_2)),
    AppAzanSound("azan_short_1", context.getString(R.string.azan_sound_option_short)),
)

fun dailyDuaContent(context: Context, date: Calendar = Calendar.getInstance()): DailyDuaContent {
    val islamicCalendar = IslamicCalendar().apply { timeInMillis = date.timeInMillis }
    val hijriDay = islamicCalendar.get(IslamicCalendar.DAY_OF_MONTH)
    val hijriMonth = islamicCalendar.get(IslamicCalendar.MONTH)
    val isRamadan = hijriMonth == 8
    val extras = sampleDuas(context)
    val dayLabel = if (isRamadan) {
        context.getString(R.string.dua_ramadan_day_label, hijriDay)
    } else {
        context.getString(R.string.dua_today_label)
    }

    val dailyBody = when (date.get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> context.getString(R.string.daily_dua_monday)
        Calendar.TUESDAY -> context.getString(R.string.daily_dua_tuesday)
        Calendar.WEDNESDAY -> context.getString(R.string.daily_dua_wednesday)
        Calendar.THURSDAY -> context.getString(R.string.daily_dua_thursday)
        Calendar.FRIDAY -> context.getString(R.string.daily_dua_friday)
        Calendar.SATURDAY -> context.getString(R.string.daily_dua_saturday)
        else -> context.getString(R.string.daily_dua_sunday)
    }

    val ramadanBody = if (isRamadan) {
        context.getString(R.string.daily_ramadan_dua_template, hijriDay)
    } else {
        null
    }

    val customIftarDua = if (isRamadan) AzerbaijaniDuaRepository.ramadanDailyDua(context, hijriDay) else null

    return DailyDuaContent(
        dayLabel = dayLabel,
        dailyTitle = context.getString(R.string.daily_dua_title),
        dailyBody = dailyBody,
        ramadanTitle = if (isRamadan) context.getString(R.string.daily_ramadan_dua_title) else null,
        ramadanBody = ramadanBody,
        iftarTitle = if (isRamadan) context.getString(R.string.daily_iftar_dua_title) else null,
        iftarBody = if (isRamadan) {
            customIftarDua ?: AzerbaijaniDuaRepository.fallbackIftarDua(context).ifBlank {
                context.getString(R.string.daily_iftar_dua_body)
            }
        } else {
            null
        },
        extraDuas = extras,
    )
}
