# Muslim Time (Flutter MVP)

Android-first Flutter skeleton (iOS-ready architecture) for:
- Namaz vaxtlari (cari konum + seher/rayon/olke axtarisi)
- Iftar / Imsak schedule
- Azan reminder + "Namazi qildin?" confirmation dialog
- Quran (multi-language API)
- Sesli Quran ayah playback
- Duas section (local JSON)

## Current Technical Stack
- Flutter (Dart)
- Aladhan API (prayer timings)
- Nominatim API (city/district/country search)
- AlQuran Cloud API (ayah + translation)
- everyayah.com MP3 links (ayah audio)
- flutter_local_notifications (reminders)

## Run
1. Install Flutter SDK (stable)
2. Generate native folders first (this repo started from custom skeleton):
   - `flutter create .`
3. Install packages:
   - `flutter pub get`
4. Run app:
   - `flutter run`

## Important
- Android homescreen widget implementation requires native `android/` files.
- Widget implementation plan: `docs/android_widget_plan.md`
- Play Console release checklist: `docs/play_console_release_checklist.md`

## Next
- Add persistent prayer completion tracking (qildim/sonra)
- Add proper onboarding and accessibility mode for older users
- Add offline Quran cache
- After Android release, prepare iOS release tracks
