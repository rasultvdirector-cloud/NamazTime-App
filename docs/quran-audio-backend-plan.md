# Quran Audio Backend Plan

## Goal

Expose a small backend/proxy for Quran audio so the Android app does not store `clientId` / `clientSecret` locally.

The Android app should only call our backend. The backend talks to the upstream Quran audio provider.

## Scope

First version:

- support audio by sura
- support audio by ayah
- support one reciter
- support simple play / pause / next / previous in Android
- keep payloads small

Not in first version:

- downloads / offline mode
- multiple reciters
- waveform / progress sync from server
- bookmarks

## Backend API Contract

Base URL example:

```text
https://api.namaztime.app
```

### 1. Health

`GET /v1/health`

Response:

```json
{
  "ok": true
}
```

### 2. Audio Suras

`GET /v1/quran/audio/suras`

Purpose:

- return the list of suras available for audio navigation

Response:

```json
{
  "reciter": {
    "id": "default",
    "name": "Default Reciter"
  },
  "items": [
    {
      "suraNumber": 1,
      "nameArabic": "الفاتحة",
      "nameLatin": "Al-Fatihah",
      "ayahCount": 7
    }
  ]
}
```

### 3. Audio Ayahs For One Sura

`GET /v1/quran/audio/suras/{suraNumber}/ayahs`

Purpose:

- return ayah-by-ayah audio entries for one sura
- include enough metadata for list rendering and playback

Response:

```json
{
  "suraNumber": 1,
  "nameArabic": "الفاتحة",
  "nameLatin": "Al-Fatihah",
  "items": [
    {
      "ayahNumber": 1,
      "ayahKey": "1:1",
      "arabicText": "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
      "translation": "Mərhəmətli və Rəhmli Allahın adı ilə!",
      "audioUrl": "https://cdn.example.com/quran/1/1.mp3",
      "durationMs": 5120
    }
  ]
}
```

### 4. Optional Stream Proxy

`GET /v1/quran/audio/stream?sura=1&ayah=1`

Purpose:

- optional proxy if upstream URLs should not be exposed directly
- first version can skip this if direct `audioUrl` is acceptable

## Backend Internal Flow

### Provider adapter

The backend should have one adapter layer:

- `QuranAudioProvider`

Responsibilities:

- authenticate with upstream provider
- fetch sura metadata
- fetch ayah audio metadata
- normalize output into our backend format

### Cache

Use short-lived cache:

- sura list: 24 hours
- ayah list for one sura: 24 hours

Reason:

- static data
- avoid rate-limit pressure
- faster Android UI

### Error policy

Backend should always return stable JSON:

```json
{
  "error": {
    "code": "UPSTREAM_FAILED",
    "message": "Audio provider request failed"
  }
}
```

HTTP codes:

- `200` success
- `400` bad request
- `404` unsupported sura / ayah
- `502` upstream failure

## Android Integration

### Repository

Create:

- `QuranAudioBackendRepository`

Functions:

- `fetchAudioSuras()`
- `fetchAudioAyahs(suraNumber: Int)`

### Android models

```kotlin
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
    val durationMs: Long?,
)
```

### UI flow

`Audio Quran` tab:

1. show sura list
2. user taps sura
3. load ayah list
4. user taps one ayah
5. player starts from that ayah
6. active ayah row is highlighted

### Playback engine

Use:

- `ExoPlayer`

Reason:

- better for streaming
- better buffering behavior
- easier state handling than plain `MediaPlayer`

### Playback state

Minimum state model:

```kotlin
data class QuranPlaybackState(
    val currentSura: Int?,
    val currentAyah: Int?,
    val isPlaying: Boolean,
    val isLoading: Boolean,
    val currentUrl: String?,
)
```

### Ayah-by-ayah playback flow

1. Android loads `/v1/quran/audio/suras/{sura}/ayahs`
2. user taps ayah `N`
3. app sends `audioUrl` to ExoPlayer
4. player state becomes `loading`
5. once ready, row `N` becomes active
6. if playback ends:
   - move to ayah `N+1`
   - if available, play next automatically
   - otherwise stop at end of sura

### UI controls

Minimum controls:

- play
- pause
- next ayah
- previous ayah

Optional second phase:

- seek bar
- repeat ayah
- repeat sura

## First Implementation Slice

Phase 1:

- backend
  - `/v1/health`
  - `/v1/quran/audio/suras`
  - `/v1/quran/audio/suras/1/ayahs`
- Android
  - audio sura list
  - open sura 1
  - ayah list
  - play one ayah

Phase 2:

- all suras
- next/previous ayah auto-advance

Phase 3:

- cache
- multiple reciters

## Suggested Backend Stack

Recommended:

- `Node.js`
- `Express`

Reason:

- fast to ship
- easy proxy layer
- easy JSON cache

Suggested structure:

```text
backend/
  src/
    server.ts
    routes/
      health.ts
      quranAudio.ts
    providers/
      quranAudioProvider.ts
    services/
      quranAudioService.ts
    cache/
      memoryCache.ts
```

## Security Rules

- never expose upstream `clientSecret` to Android
- keep provider auth only on backend
- if direct provider URL contains sensitive tokens, proxy the stream

## Next Step

Build:

1. backend stub with static JSON
2. Android repository against that stub
3. ExoPlayer integration
4. then replace stub with real provider adapter
