# NamazTime Local Quran Audio Backend

Minimal local stub backend for `Audio Quran`.

## Run

```bash
cd "/Users/rasulalekberov/APP/Muslim Time"
node backend/server.mjs
```

Default address:

- `http://127.0.0.1:8787`

## Endpoints

- `GET /v1/health`
- `GET /v1/quran/audio/suras`
- `GET /v1/quran/audio/suras/1/ayahs`

## Notes

- This is a stub backend.
- It currently exposes only Surah 1.
- All ayah playback uses the local sample file:
  - `android_app/app/src/main/res/raw/azan_short_1.mp3`

## Emulator

Android emulator can use:

- `http://10.0.2.2:8787/v1`

## Physical phone over USB

Use ADB reverse:

```bash
adb reverse tcp:8787 tcp:8787
```

Then the app can use:

- `http://127.0.0.1:8787/v1`
