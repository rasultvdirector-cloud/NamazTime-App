# Muslim Time - Android (Kotlin)

Bu qovluq Android/Kotlin versiyadır. Flutter hissələrini sıfırladıq və yenidən Native Android app kimi qurduq.

## Run etmək üçün

### 1) Android Studio ilə (təklif olunan yol)

1. Android Studio açın.
2. Open an Existing Project → `/Users/rasulalekberov/APP/Muslim Time/android_app`
3. Studio yükləyib Sync edəcək. Əgər `Java` və `SDK` yoxdursa qurulsun.
4. `Run` düyməsinə basın və emulyator və ya real cihaz seçin.

### 2) Terminal ilə

```
cd '/Users/rasulalekberov/APP/Muslim Time/android_app'
/Applications/Android\ Studio.app/Contents/jbr/Contents/Home/bin/java -version
./gradlew tasks
./gradlew :app:assembleDebug
./gradlew :app:installDebug
```

`./gradlew` üçün əgər Java tapılmırsa, Android Studio-nun JDK yolunu `JAVA_HOME` kimi verin:

```
export JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home'
export PATH="$JAVA_HOME/bin:$PATH"
```

## Qısa xülasə

- Namaz vaxtları: avtomatik və manual məkan seçimi, mənbə seçimi, Azərbaycan üçün Qafqazislam yönümlü axın
- Quran: `Quran kitabını oxu`, `Quran dinlə`, `Allahın adları` bölmələri
- Qibləgah: kompas və xəritə görünüşü
- Xatırlatma: namazdan əvvəl xatırlatma, təkrar xatırlatma, cümə və ad günü bildirişləri
- Çoxdilli dəstək: `AZ`, `EN`, `RU`, `TR`
- Görünüş: sistem / açıq / tünd tema, böyük şrift və yaşlı istifadəçi rejimi

## Play Console üçün

1. [keystore.properties.example](/Users/rasulalekberov/APP/Muslim%20Time/android_app/keystore.properties.example) faylını `keystore.properties` kimi kopyalayın.
2. Upload keystore məlumatlarını doldurun.
3. Lazımdırsa upload keystore yaradın:

```bash
cd '/Users/rasulalekberov/APP/Muslim Time/android_app'
keytool -genkeypair -v \
  -keystore namaztime-upload-key.jks \
  -alias upload \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

4. Release bundle yığın:

```bash
cd '/Users/rasulalekberov/APP/Muslim Time/android_app'
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew :app:bundleRelease
```

Bundle çıxışı:

```text
app/build/outputs/bundle/release/app-release.aab
```

Əlavə fayllar:

- host edilə bilən privacy policy:
  - [docs/privacy-policy.html](/Users/rasulalekberov/APP/Muslim%20Time/android_app/docs/privacy-policy.html)
- release checklist:
  - [docs/play-store-release-checklist.md](/Users/rasulalekberov/APP/Muslim%20Time/android_app/docs/play-store-release-checklist.md)
