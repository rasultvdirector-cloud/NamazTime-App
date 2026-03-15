import Foundation

struct PrayerTime: Identifiable, Hashable {
    let id = UUID()
    let name: String
    let time: String
}

struct CityPrayerTimes: Identifiable {
    let id = UUID()
    let city: String
    let country: String
    let times: [PrayerTime]
}

struct Dua: Identifiable {
    let id = UUID()
    let title: String
    let text: String
}

struct Verse: Identifiable {
    let id = UUID()
    let text: String
}

struct ReciterTrack: Identifiable {
    let id = UUID()
    let title: String
    let reciter: String
}

let samplePrayerTimes: [CityPrayerTimes] = [
    CityPrayerTimes(
        city: "Bakı",
        country: "Azərbaycan",
        times: [
            PrayerTime(name: "İmsak", time: "05:22"),
            PrayerTime(name: "Gün doğumu", time: "06:51"),
            PrayerTime(name: "Günorta", time: "13:18"),
            PrayerTime(name: "Əsr", time: "16:53"),
            PrayerTime(name: "Məğrib (İftar)", time: "19:10"),
            PrayerTime(name: "İşa", time: "20:41"),
        ]
    ),
    CityPrayerTimes(
        city: "London",
        country: "Birləşmiş Krallıq",
        times: [
            PrayerTime(name: "İmsak", time: "06:02"),
            PrayerTime(name: "Gün doğumu", time: "07:43"),
            PrayerTime(name: "Günorta", time: "13:03"),
            PrayerTime(name: "Əsr", time: "16:29"),
            PrayerTime(name: "Məğrib (İftar)", time: "18:54"),
            PrayerTime(name: "İşa", time: "20:14"),
        ]
    )
]

let sampleDuas: [Dua] = [
    Dua(title: "Səhər duası", text: "Əllər qaldırıb dua: Rəbbimiz, bu günü bizə xeyir və bərəkət et."),
    Dua(title: "Yol duası", text: "Allahummə inni es'elükə sənnəl hidayət və taqiyyət."),
    Dua(title: "Gecə duası", text: "Yatmağdan əvvəl: Alləhmə, bu gecəni xeyir və mağfirətlə keçirt."),
]

let sampleVerses: [Verse] = [
    Verse(text: "Bismillahir-Rahmanir-Rahim — Hər başlanğıcda Allahın adı ilə."),
    Verse(text: "وَفِي السَّمَاءِ رِزْقُكُمْ وَمَا تُوعَدُونَ — Rızqınız göydədir."),
    Verse(text: "İnna ənşənəhlə… — " + "Dua və səbrlə yola davam."),
]

let sampleTracks: [ReciterTrack] = [
    ReciterTrack(title: "Al-Fatiha", reciter: "Məscid Qari"),
    ReciterTrack(title: "Al-Ikhlas", reciter: "Quran Voice"),
    ReciterTrack(title: "Yasin", reciter: "Məshriq Studio"),
]
