# Qafqazislam Data Workflow

Məqsəd:
- `android_app/app/src/main/assets/azerbaijan_prayer_times.json` faylını əl ilə redaktə etməmək
- cari ay və növbəti ay datasını rəsmi Qafqazislam şəkil cədvəlindən almaq
- bütün aylarda rəsmi sütun ardıcıllığını qorumaq
- Bakı və digər şəhərlər üçün eyni workflow istifadə etmək

## 1. Rəsmi sütun ardıcıllığı

Qafqazislam şəkillərində sütunlar bu ardıcıllıqla gəlir və parser bunu sabit qayda kimi qəbul edir:

```text
ayın günləri
hicri ayın günləri
həftənin günləri
imsak vaxtı
sübh azanı
gün çıxır
zöhr azanı
əsr azanı
gün batır
məğrib azanı
işa azanı
gecə yarısı
```

App bu sıradan aşağıdakı map-i istifadə edir:
- `imsak vaxtı` -> `imsak`
- `sübh azanı` -> `fajr`
- `gün çıxır` -> `sunrise`
- `zöhr azanı` -> `dhuhr`
- `əsr azanı` -> `asr`
- `məğrib azanı` -> `maghrib`
- `işa azanı` -> `isha`

`gün batır` və `gecə yarısı` da docs JSON-da saxlanılır ki, rəsmi mənbənin tam ardıcıllığı itməsin.

## 2. Import et

Bir şəhər üçün cari ay + növbəti ay:

```bash
cd '/Users/rasulalekberov/APP/Muslim Time'
python3 tools/qafqaz_sync.py \
  --city-id 1 \
  --city-name Bakı \
  --year 2026 \
  --month 4 \
  --include-next-month
```

Bütün şəhərlər üçün cari ay + növbəti ay:

```bash
cd '/Users/rasulalekberov/APP/Muslim Time'
python3 tools/qafqaz_sync.py \
  --all-cities \
  --current-window \
  --include-next-month
```

Bu script:
- Qafqazislam print səhifəsindən rəsmi şəkil URL-ni tapır
- CMYK JPG-ni PNG-yə çevirir
- `Vision OCR` ilə cədvəli oxuyur
- rəsmi sütun ardıcıllığını sabit saxlayaraq parse edir
- `docs/namaz-time-data/...` altında tam JSON yaradır
- Android asset içində app üçün lazım olan saatları yeniləyir
- asset `version` dəyərini yalnız dəyişiklik olanda artırır

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

- Azərbaycan üçün ən azı `cari ay + növbəti ay` saxla
- həftəlik avtomatik run ilə yeni ay pəncərəsini qabaqcadan yenilə
- digər ölkələr üçün API axını qalır

## 6. Şəhər adları

Script və repository şəhər adlarını normallaşdırır:
- `Bakı` -> `Baku`
- `Gəncə` -> `Ganja`
- `Sumqayıt` -> `Sumgait`
- `Naxçıvan` -> `Nakhchivan`
- `Şəki` -> `Sheki`
- `Lənkəran` -> `Lankaran`

Yeni şəhər əlavə ediləcəksə:
- saytın city option siyahısında varsa `--all-cities` rejimi ilə avtomatik əlavə olunur
- lazım olsa `QafqazIslamRepository` alias siyahısı genişləndirilir
