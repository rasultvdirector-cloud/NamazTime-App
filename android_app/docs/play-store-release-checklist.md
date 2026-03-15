# NamazTime Play Store Release Checklist

## 1. Versiya
- `versionCode` artırılsın
- `versionName` final release formatına keçirilsin

## 2. Signing
- `keystore.properties` real məlumatlarla doldurulsun
- upload keystore yoxlanılsın
- signed `app-release.aab` yenidən yığılsın

## 3. Privacy Policy
- `docs/privacy-policy.html` host edilsin
- Play Console listing-ə public link əlavə olunsun

## 4. Data Safety
- location istifadəsi qeyd edilsin
- notification istifadəsi qeyd edilsin
- audio playback istifadəsi qeyd edilsin
- lokal ayarların cihazda saxlanıldığı qeyd edilsin

## 5. Listing məlumatları
- App name
- Short description
- Full description
- Contact email
- Category
- Privacy policy URL

## 6. Visual materiallar
- app icon final versiya
- phone screenshots
- optional feature graphic

## 7. Final test
- onboarding
- auto location
- manual location
- prayer times fetch
- reminder test
- widget
- Quran background audio
- qibləgah
- dark/light mode

## 8. Release build
```bash
cd '/Users/rasulalekberov/APP/Muslim Time/android_app'
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew :app:bundleRelease
```

Bundle output:
- `app/build/outputs/bundle/release/app-release.aab`
