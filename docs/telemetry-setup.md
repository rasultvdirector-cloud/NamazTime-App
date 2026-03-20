# NamazTime Telemetry Setup

This document explains how reminder / azan diagnostics can be uploaded automatically.

## What the app now records

- reminder scheduled / receiver fired
- azan service start attempted
- azan service start failed
- playback started / completed / failed
- notification shown / blocked
- reschedule after boot / app update

These diagnostics are always stored locally inside the app and shown in `Advanced`.

## How to enable remote upload

The Android app reads the telemetry base URL from:

- `local.properties`
- or environment variable `NAMAZTIME_TELEMETRY_BASE_URL`

Example for local testing:

```properties
namaztime.telemetryBaseUrl=http://10.0.2.2:8787/v1
```

Example for a deployed backend:

```properties
namaztime.telemetryBaseUrl=https://your-domain.example/v1
```

## Local backend

Run:

```bash
cd "/Users/rasulalekberov/APP/Muslim Time"
node backend/server.mjs
```

The backend now accepts:

- `POST /v1/telemetry/logs`

Logs are appended to:

- `backend/data/telemetry_logs.ndjson`

## Production note

For real tester devices from Play Console, the telemetry backend must be deployed to a public HTTPS URL and added to the release build configuration before generating the final AAB.
