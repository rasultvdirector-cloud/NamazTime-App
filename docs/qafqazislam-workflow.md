# Qafqazislam Data Workflow

Məqsəd:
- `android_app/app/src/main/assets/azerbaijan_prayer_times.json` faylını əl ilə redaktə etməmək
- cari ay və növbəti ay datasını sürətli yeniləmək
- Bakı və digər şəhərlər üçün eyni workflow istifadə etmək

## 1. Mənbə cədvəli hazırla

Qafqazislam cədvəlini bu sütunlarla `csv` və ya `tsv` kimi saxla:

```text
day,imsak,fajr,sunrise,dhuhr,asr,maghrib,isha
1,05:49,05:54,07:14,12:53,16:49,18:47,19:47
2,05:47,05:52,07:12,12:53,16:50,18:48,19:48
```

App-də istifadə etmədiyimiz sütunları daxil etmə:
- `gün batır`
- `gecə yarısı`

## 2. Import et

Nümunə:

```bash
cd '/Users/rasulalekberov/APP/Muslim Time'
python3 tools/qafqaz_importer.py \
  --city Baku \
  --year 2026 \
  --month 3 \
  --input /absolute/path/to/baku_march_2026.csv
```

Bu script:
- uyğun şəhəri tapır və ya yaradır
- uyğun ayı tapır və ya yaradır
- günləri tam əvəz edir
- asset `version` dəyərini artırır

## 3. Build yoxla

```bash
cd '/Users/rasulalekberov/APP/Muslim Time/android_app'
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew :app:assembleDebug
```

## 4. Test et

1. App-i aç
2. `Bakı, Azərbaycan` seç
3. həmin günün saatlarını Qafqazislam ilə tutuşdur
4. `Reminder statusu` panelində mənbənin `Qafqazislam lokal dataset` olduğunu yoxla

## 5. Tövsiyə olunan iş rejimi

- Azərbaycan üçün yalnız `cari ay + növbəti ay` saxla
- ay dəyişəndə yeni ayı bu script ilə import et
- digər ölkələr üçün API axını qalır

## 6. Şəhər adları

Script və repository bu şəhər adlarını normallaşdırır:
- `Bakı` -> `Baku`
- `Gəncə` -> `Ganja`
- `Sumqayıt` -> `Sumgait`
- `Naxçıvan` -> `Nakhchivan`
- `Şəki` -> `Sheki`
- `Lənkəran` -> `Lankaran`

Yeni şəhər əlavə ediləcəksə:
- asset-ə həmin city block əlavə edilir
- lazım olsa `QafqazIslamRepository` alias siyahısı genişləndirilir
