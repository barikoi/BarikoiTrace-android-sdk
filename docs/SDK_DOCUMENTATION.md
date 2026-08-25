# BarikoiTrace Android SDK — Technical Reference

Branch: `dev-v3` (as of commit `6884a39`). Module: `barikoitrace`, published `com.github.barikoi:barikoitrace:1.0.0` (JitPack). Min SDK 24 / Target+Compile SDK 35 / Kotlin, Java 17.

> **This branch is a ground-up rewrite**, not an iteration on the previous Java implementation (which this doc previously described). Everything below replaces the earlier version of this document: language (Java→Kotlin), networking (Volley→Retrofit/OkHttp+coroutines), storage (SharedPreferences→DataStore, raw SQLite→Room), sync transport (HTTP polling→MQTT), and the trip model (server-authoritative→fully local) all changed. Geofencing was removed entirely. Real unit/instrumented tests now exist (554 lines across 6 files, previously zero).

---

## 1. What it does

Drop-in background location tracking SDK for the Barikoi location platform. Handles: permission/consent flow, adaptive foreground-service GPS tracking, offline queuing via Room + automatic resync over MQTT, lightweight local trip tagging, and simple user identity (phone/email) tied to a trace account. REST backend: `https://api.trace.bmapsbd.com/api/v1/` (auth + settings only). Live telemetry transport: MQTT broker at `tcp://broker.trace.bmapsbd.com:1883`.

---

## 2. Architecture

```
App
 └─ BarikoiTrace (Kotlin object facade — suspend fns + callback wrappers)
     └─ LocTraceManager (singleton orchestrator)
         ├─ TraceDataStore (Jetpack DataStore Preferences, backed by an in-memory ConcurrentHashMap cache)
         ├─ TraceApiClient (Retrofit/OkHttp → api.trace.bmapsbd.com, 2 endpoints only)
         ├─ LocationEngine (FusedLocationProviderClient only — no legacy android.location fallback)
         └─ OfflineLocationDb (Room: single `offline_location` table, JSON blob rows)
LocTraceForegroundService — owns the live GPS stream, opens/manages MqttManager, flushes the offline queue when MQTT (re)connects
LocTraceDataService — WorkManager CoroutineWorker, periodic one-shot location fetch → offline queue (no direct network call)
MqttManager — Paho MQTT client wrapping connect/reconnect/backoff and publish to `company/{companyId}/{groupId}/{userId}/location`
BootReceiver — resumes tracking on BOOT_COMPLETED if it was active before reboot
LocationReceiver — app-facing BroadcastReceiver stub; real live updates now come via BarikoiTrace.locationUpdates (a Kotlin SharedFlow)
```

Still a global-singleton architecture (`getInstance(context)` everywhere) — same testability caveat as before, though now partially mitigated by MockK/Robolectric unit tests around `LocTraceManager`, `TraceMode`, `TraceError`, and `TraceDataStore`.

---

## 3. Features

**Adaptive tracking modes** — `TraceMode.ACTIVE / REACTIVE / PASSIVE` presets, plus a `Builder` for custom update interval, distance filter, accuracy filter, offline-sync toggle, debug mode, ping-sync interval, and now a daily **tracking time window** (`setStartTime`/`setEndTime`, `java.time.LocalTime`) — the foreground service self-stops if the current time falls outside the configured window.

**Coroutine-first API** — `setOrCreateUser`, `updateCurrentLocation`, `getSettingsFromRemote` are `suspend` functions on the `BarikoiTrace` object, each with a matching Java-friendly callback overload (`TraceUserCallback`, `TraceLocationUpdateCallback`, `TraceSettingsCallback`) that just wraps the suspend call in an internal `CoroutineScope`.

**Local trip tagging (no server trip API)** — `startTracking(mode, withTrip = true)` generates a local `UUID` trip id (`getTripId()`/`getCurrentTrip()`/`isOnTrip()`), stamped onto every location payload (`trip_id`, `trip_status: "active"`/`"completed"`) published over MQTT. There is **no backend trip-lifecycle endpoint anymore** — the entire `/trip/*` REST surface from the previous version is gone; trip state is a local label with no server-side validation, closure guarantee, or cross-device visibility.

**MQTT live telemetry** — every accepted location fix is published to a per-user topic `company/{companyId}/{groupId}/{userId}/location` (QoS 1, LWT `device/{userId}/status = offline`), with auto-reconnect (exponential backoff, capped at 60s, 10 attempts) via `MqttManager`. This replaces the old per-point HTTP POST.

**Offline-first sync (Room + MQTT)** — if MQTT isn't connected when a fix arrives, it's inserted into a Room `offline_location` table; on every reconnect (or right after a live publish) the service drains it in batches of 100 (`flushOfflineData`, recursive until empty). `LocTraceDataService` (WorkManager) does a one-shot location fetch and also writes straight to the offline table — it does not talk to MQTT or REST itself, relying on the foreground service's next connect to flush it.

**User identity** — `setOrCreateUser(name, email, phone)` calls `/sdk/authenticate`, expects the user to belong to at least one company (`companies[0]`, throws `"Company not found"` if the array is empty), and caches the result with the same 24h freshness short-circuit as before. On successful login it also best-effort-fetches company settings and applies them as the active `TraceMode`.

**Runtime URL overrides** — `setBaseUrl`, `setMqttUrl`, `resetUrls` let a host app repoint the SDK at a different environment at runtime (e.g. staging); changing the base URL clears the cached user, cached trace mode, and stops tracking as a side effect.

**Live event bridge** — `BarikoiTrace.locationUpdates` is a `SharedFlow<Location>` (replay = 0) that the foreground service emits into when `setBroadcastingEnabled(true)`; `registerLocationUpdate`/`unregisterLocationUpdate` and `LocationReceiver` still exist for API compatibility but are now **no-op stubs** — real consumers must collect the Flow.

**SDK-internal log listener** — `BarikoiTrace.setLogListener(TraceLogListener)` surfaces internal log lines (currently just MQTT connection-status and publish-ack events) to the host app, used by `DemoActivity` to render a live debug console.

**Mock-location detection** — still checks `Location.isMock`/`isFromMockProvider`; a detected mock fix is now silently dropped (no Toast, no callback, no error surfaced) rather than shown to the user.

**Permission & OS-quirk helpers** — same battery-optimization / autostart-settings helpers as before, trimmed to Xiaomi/LeEco/Huawei only (Oppo/Vivo/Samsung/HTC/Asus deep-links from the old `checkAppServicePermission` list were dropped in this rewrite).

**Removed**: geofencing (`GeofenceManager`, `createGeofence`, `startGeofence`) is gone entirely — no circular-region or transition-event support in this branch. `insertLogFile`/log-file-upload endpoint is also gone.

---

## 4. Public API — `BarikoiTrace` (Kotlin `object`, `@JvmStatic` for Java interop)

| Member | Purpose |
|---|---|
| `initialize(Context, String apiKey)` | Must be called first. Persists API key, generates/persists a device UUID token, resumes tracking if it was active before process death. |
| `setLogListener(TraceLogListener?)` | Wires an internal debug-log sink (MQTT connect/publish events today). |
| `setBaseUrl(String)` / `setMqttUrl(String)` / `resetUrls()` | Runtime endpoint overrides (see §3). |
| `suspend setOrCreateUser(name, email, phone): TraceUser` + callback overload `setOrCreateUser(name, email, phone, TraceUserCallback)` | Login-or-register against `/sdk/authenticate`. |
| `getUser(): TraceUser?` / `getUserId(): String?` | Cached local user. |
| `isLocationPermissionsGranted(): Boolean` | Fine **or** coarse location granted (looser than the old fine-only check). |
| `isLocationSettingsOn(): Boolean` | GPS or network provider enabled. |
| `requestLocationPermissions(Activity)` | Requests both `ACCESS_FINE_LOCATION` and `ACCESS_COARSE_LOCATION`. |
| `requestBackgroundLocationPermission(Activity)` | API 29+. |
| `requestNotificationPermission(Activity)` | API 33+. |
| `requestLocationServices(Activity)` | Opens system location-source settings. |
| `requestDisableBatteryOptimization(Context)` | Prompts to allow-list the app. |
| `isBatteryOptimizationEnabled(): Boolean` | Whether battery optimization is already being ignored. |
| `checkAppServicePermission(Context)` / `openAutostartSettings(Context)` | OEM autostart deep-links (Xiaomi/LeEco/Huawei only now). |
| `setTraceMode(TraceMode)` | Persists a tracking profile without starting tracking. |
| `startTracking(TraceMode)` / `startTracking(TraceMode, withTrip: Boolean)` | Starts the foreground service; `withTrip = true` also opens a local trip UUID. No-op (logged, not thrown) if no user id or permissions/location-settings are off. |
| `stopTracking()` | Stops the foreground service and clears the SDK-tracking flag. |
| `isLocationTracking(): Boolean` | Whether the foreground service is currently running. |
| `setOfflineTracking(Boolean)` | Enables/disables the Room offline queue path. |
| `setLoggingEnabled(Boolean)` / `setBroadcastingEnabled(Boolean)` | Toggle internal debug logging / the `locationUpdates` Flow. |
| `isOnTrip(): Boolean` / `getTripId(): String?` / `getCurrentTrip(): String?` | Local trip state — **no server round-trip**, `getTripId()` and `getCurrentTrip()` are identical (redundant API surface). |
| `suspend updateCurrentLocation(): Location` + callback overload | One-shot fetch; always written to the offline queue (not sent live), tagged with the current trip id if any. |
| `uploadOfflineData()` | No longer flushes anything itself — just logs and calls `refreshTracking()` to bounce the foreground service so MQTT reconnects and the service's own flush logic runs. |
| `suspend getSettingsFromRemote(): TraceMode` + callback overload | Pulls and applies company-level `TraceMode` from `/sdk/company/settings`. |
| `locationUpdates: SharedFlow<Location>` | Kotlin-first live location stream (see §3). |
| `registerLocationUpdate(LocationReceiver)` / `unregisterLocationUpdate(...)` | **No-op stubs**, kept only for source compatibility with the old API. |

Calling almost any of these before `initialize()` throws `IllegalStateException("BarikoiTrace not initialized...")` synchronously (uncaught, will crash the host app) — same footgun as the previous version's `ContextException`.

### `TraceMode`

Now a Kotlin `data class` with a `startTime`/`endTime` (`LocalTime`) tracking window in addition to the previous fields. Builder floors are stricter than before: `setUpdateInterval` floors at 5s (was unbounded), `setDistanceFilter` floors at 10m (was unbounded), `setAccuracyFilter` floors at 20m (was 10–150 clamp defaulting to 100). Presets `ACTIVE`/`PASSIVE`/`REACTIVE` unchanged numerically. **Speed-adaptive distance filtering from the old `LocationUtils` is gone** — `ACTIVE` mode no longer tightens/loosens its distance filter based on current speed; it's a fixed profile now.

### Callback interfaces (nested in `BarikoiTrace`)

| Interface | Methods |
|---|---|
| `TraceUserCallback` | `onSuccess(TraceUser)`, `onFailure(TraceError)` |
| `TraceLocationUpdateCallback` | `onLocationUpdate(Location)`, `onFailure(TraceError)` |
| `TraceSettingsCallback` | `onSuccess(TraceMode)`, `onFailure(TraceError)` |
| `TraceLogListener` | `onLog(level: String, tag: String, message: String)` |

`LocationReceiver.EventCallback` (`onError(String)`, `onLocationUpdated(Location)`) still exists in `.receiver` but is now dead — nothing calls it, since `LocationReceiver` no longer receives real broadcasts (superseded by `locationUpdates` Flow).

### Models

- `TraceUser` (plain data class, no builder now): `userId, name, email, phone, companyId, group, lastLat, lastLon, updatedAt`. **New required field vs. the old model**: `companyId` — `authenticate()` throws if the server returns a user with zero companies.
- `TraceError`: `code` + `message`, factories: `noUserError, noKeyError, noDataError, networkError, locationPermissionError, locationNotFoundError, serverError, tripStateError, mockAppError, jsonError(detail)`. Simpler/flatter than before (no `BK4xx`/`BK5xx` numeric codes — plain string codes like `NO_USER`, `NETWORK`).
- `OfflineLocationEntity` (Room): `id (autogen), json (String)` — same "just store the raw JSON blob" pattern as the old SQLite table, now via Room instead of hand-written `SQLiteOpenHelper`.

`Trip`, `Coordinates`, `BarikoiTraceLocationInfo` and all the old trip-callback interfaces (`TraceGetTripCallback`, `TraceTripStateCallback`, `TraceTripApiCallback`) **do not exist in this branch** — trips are now just a `String?` UUID, nothing more.

---

## 5. Backend surfaces

### REST — `https://api.trace.bmapsbd.com/api/v1/` (Retrofit, Gson, no auth header — `api_key` still travels in the JSON body)

| Endpoint | Method | Body | Success shape | Called by |
|---|---|---|---|---|
| `/sdk/authenticate` | POST | `{api_key, name?, email?, phone}` | `{user: {_id, name, email, companies: [{company_id, group_id}, ...]}}` | `setOrCreateUser` |
| `/sdk/company/settings` | POST | `{api_key, phone}` | `{settings: {update_time_interval, distance_interval, accuracy_filter, offline_sync, tracking_start_time, tracking_end_time}}` | `getSettingsFromRemote` |

That's the entire REST surface now — down from 9 endpoints in the previous branch (all `/sdk/add-gpx`, `/sdk/bulk-gpx`, `/sdk/user`, and every `/trip/*` route are gone; live telemetry moved to MQTT). No retry policy configured on either call (single attempt via OkHttp defaults); any non-2xx or missing body throws `Exception("Server error: <code>")`.

### MQTT — `tcp://broker.trace.bmapsbd.com:1883` (Eclipse Paho via `hannesa2/paho.mqtt.android`)

- **Topic**: `company/{companyId}/{groupId}/{userId}/location`, QoS 1, not retained.
- **Client id**: `AndroidClient-{userId}-{deviceUuid}`.
- **Auth**: username/password — **both hardcoded as constants in `MqttManager`** (see §6, finding #1).
- **LWT**: `device/{userId}/status` = `"offline"`, QoS 1, retained.
- **Payload** (live): `{latitude, longitude, gpx_time (epoch ms), user_id, company_id, speed, bearing, altitude, accuracy, user_name?, trip_id?, trip_status?}`.
- **Payload** (offline-flush): the same shape but built at capture time with `gpx_time` as a formatted UTC string (`DateTimeUtils.getDateTimeLocal`) rather than epoch millis — **inconsistent `gpx_time` format between the live-publish path and the offline-flush path**, a real parsing hazard for anything consuming the broker downstream.
- **Inbound**: the client also subscribes to nothing explicitly, but `messageArrived` special-cases any topic ending in `/command` (logged only, no handler implemented).

---

## 6. Senior review notes — issues worth triaging before shipping this branch

1. **Hardcoded MQTT broker credentials in `MqttManager`**: `MQTT_USERNAME = "rilus"`, `MQTT_PASSWORD = "r1lu5"` — a single shared username/password, baked into the SDK source, used to authenticate *every* installed app instance to the broker. This is a materially worse version of the previous branch's hardcoded Slack webhook: it's live broker credentials (not just a notification sink), it's shared across the entire install base (no per-device/per-tenant auth), and the values read like a personal login rather than a service account. Rotate off hardcoded creds before any external release — at minimum per-app-key broker auth, ideally per-device tokens issued by `/sdk/authenticate`.
2. **Trip lifecycle lost its server authority.** The old `/trip/create` → `/trip/end` → `/trip/check-active-trip` flow (with a server-side `Trip` record, state machine, and cross-device sync) is gone; a "trip" today is a client-generated UUID stamped onto MQTT payloads with no backend validation, no guaranteed close (if the app is killed before `onDestroy` fires a `"completed"` publish, the trip is left dangling with no `"completed"` event ever sent), and no way for a dashboard to know what trips exist except by parsing telemetry after the fact. Confirm this is an intentional architecture trade (simpler client, telemetry-only backend) and not a dropped requirement.
3. **`gpx_time` format is inconsistent between live and offline-replay paths** (epoch-ms integer vs. formatted UTC string — see §5) — anything parsing the MQTT stream downstream needs to handle both, or one code path needs to be fixed to match the other.
4. **`TraceDataStore` does a blocking disk read via `runBlocking` inside its constructor's `init {}` block**, to pre-warm an in-memory cache. Since the class is constructed on first use from wherever `LocTraceManager`/services first touch it (commonly `BarikoiTrace.initialize()` from the app's main thread), this risks a main-thread disk-I/O stall/StrictMode violation on first init. The in-memory `ConcurrentHashMap` cache is a reasonable pattern to avoid `runBlocking` reads later, but the initial warm-up should move off the constructor / off the calling thread.
5. **Global `IllegalStateException` on any facade call before `initialize()`** — every `BarikoiTrace` method except `initialize`/`setLogListener` throws synchronously and uncaught if the SDK hasn't been initialized yet. Same risk class as the old `ContextException`; still worth a softer failure mode (e.g. return null/false + log) for methods like `isLocationTracking()` that a host app might reasonably poll defensively.
6. **`getTripId()` and `getCurrentTrip()` are identical** (`dataStore.getLocalTripId()` twice) — redundant public API surface; pick one and deprecate the other before this ships as a stable 1.0 API surface (it's already versioned `1.0.0` in Gradle).
7. **Mock-location detection now fails silently** — a detected mock fix is dropped with only a `Log.w`, no error surfaced to the host app or any server-side signal that spoofing was attempted, a regression from even the old Toast-only behavior (which was at least user-visible).
8. **`LocTraceDataService` is never scheduled anywhere in this codebase.** It's a fully-implemented `CoroutineWorker` (declared in the manifest, does a one-shot location fetch + Room insert), but there is no `PeriodicWorkRequest`/`WorkManager.enqueue*` call referencing it anywhere in `barikoitrace` or `app` — the periodic-sync mechanism the old Java branch had (`LocationWork`, enqueued every 15 minutes from `LocationTracker.startLocationService()`) was not carried over when this class was ported. Right now it's dead code: either wire up the periodic enqueue (and make sure it's cancelled alongside `stopTracking()`), or remove the class and its manifest entry.
9. **Fire-and-forget config writes**: `setBaseUrl`/`setMqttUrl`/`resetUrls`/`setTraceMode` all launch a coroutine and return immediately with no completion signal — a caller that calls `setBaseUrl()` immediately followed by `startTracking()` can race the DataStore write (mitigated somewhat by the synchronous in-memory cache write happening first inside `putAndCache`, but the persisted-to-disk write and any downstream `apiClient.setBaseUrl()` call for `setBaseUrl` specifically happens without any await).
10. **Real test coverage now exists** (positive callout, not a defect): `LocTraceManagerTest`, `TraceModeTest`, `TraceErrorTest`, `TraceDataStoreTest`, `DateTimeUtilsTest` (unit, MockK/Robolectric/Truth) plus `OfflineLocationDaoTest` (instrumented, Room). This is a genuine improvement over the previous branch's zero coverage — worth keeping momentum on (network client and the MQTT reconnect/backoff logic still have no tests).
11. **Fewer OEM autostart deep-links** than before — Oppo/Vivo/Samsung/HTC/Asus/iQOO entries present in the old `checkAppServicePermission` list are gone from this branch's `SystemSettingsManager`; likely fine if intentional, but worth confirming against current device-support requirements (background-tracking reliability on those OEMs depends on this).
12. **API key now sourced from `local.properties`** (gitignored, injected via `BuildConfig.API_KEY` in the flavor config) instead of hardcoded in `MainActivity` — a genuine fix carried over from the old branch's plaintext-in-source pattern, though the sample `MainActivity.kt` still has a literal `"API_KEY"` placeholder string (harmless, just unused).

### Update — findings #1, #3, #8, #12 fixed

Applied while building the iOS SDK's Phase 0 contract (see
`BarikoiTrace-ios-sdk/docs/WORK_PLAN.md`, which flagged these as
"back-port to Kotlin too" rather than something iOS should quietly work
around alone):

- **#1 (hardcoded MQTT credentials)** — removed. `MqttManager` now requires
  `mqttUsername`/`mqttPassword` constructor params; `BarikoiTrace.initialize()`
  is now `initialize(context, apiKey, mqttUsername, mqttPassword)` (**breaking
  change** — update all call sites). Credentials are persisted via
  `TraceDataStore.setMqttUsername/setMqttPassword` (same storage posture as
  `API_KEY` — plain DataStore Preferences, not Keystore-backed; a follow-up
  worth doing for all three together, not introduced as a new gap here).
  Sample app reads them from `BuildConfig.MQTT_USERNAME`/`MQTT_PASSWORD`,
  sourced from `local.properties` the same way `API_KEY` already was.
- **#3 (`gpx_time` format inconsistency)** — fixed. `MqttManager.publishLocation`
  (the live-publish path) now uses `DateTimeUtils.getDateTimeLocal(...)`,
  the same formatted-UTC-string format the offline-write/flush path already
  used. Every MQTT payload path now agrees on one shape. Also closed a
  second, related gap while in this code: the offline-write/flush path was
  missing `company_id` and `user_name` entirely (only `user_id` got
  backfilled at flush time) — both are now included wherever known, on
  every path, matching the live-publish shape's field set.
- **#8 (`LocTraceDataService` never scheduled)** — fixed. `LocTraceManager`
  now enqueues it as a unique periodic `WorkManager` job (15-minute floor —
  `PeriodicWorkRequest`'s hard OS-enforced minimum, not a chosen cadence) in
  `startTracking()`, and cancels it in `stopTracking()`, as a fallback sync
  independent of the foreground service's own location-driven publish path.
- **#12 (sample app literal `"API_KEY"` placeholder)** — fixed as part of
  the `initialize()` signature change above; `MainActivity.kt` now reads
  `BuildConfig.API_KEY`/`MQTT_USERNAME`/`MQTT_PASSWORD` like `DemoActivity.kt`
  already did.

Not touched by this pass: #2 (trip lifecycle server authority), #4
(`runBlocking` in `TraceDataStore.init`), #5 (uninitialized-facade exception
style), #6 (`getTripId`/`getCurrentTrip` redundancy), #7 (silent mock-location
drop), #9 (fire-and-forget config writes), #10, #11 — still open.

---

## 7. Manifest / permissions / components

Permissions: `INTERNET, ACCESS_NETWORK_STATE, ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION, ACCESS_BACKGROUND_LOCATION, FOREGROUND_SERVICE, FOREGROUND_SERVICE_LOCATION, FOREGROUND_SERVICE_DATA_SYNC, POST_NOTIFICATIONS` — note `ACCESS_BACKGROUND_LOCATION` is now correctly declared in the library's own manifest (fixes finding #9 from the previous review).

Components: `LocTraceForegroundService` (`location|dataSync`, not exported), `LocTraceDataService` (WorkManager-hosted `CoroutineWorker`, not exported — now actually scheduled as a unique periodic job by `LocTraceManager.startTracking()`/`stopTracking()`, see §6 finding #8's update), `BootReceiver` (`BOOT_COMPLETED`, priority 999, exported).

---

## 8. Sample usage (`app` module — two activities now)

`MainActivity.kt` — minimal flow, mirrors the old sample:
```kotlin
BarikoiTrace.initialize(this, BuildConfig.API_KEY, BuildConfig.MQTT_USERNAME, BuildConfig.MQTT_PASSWORD)
BarikoiTrace.requestNotificationPermission(this)
if (!BarikoiTrace.isLocationPermissionsGranted()) BarikoiTrace.requestLocationPermissions(this)
if (!BarikoiTrace.isLocationSettingsOn()) BarikoiTrace.requestLocationServices(this)

BarikoiTrace.setOrCreateUser(name, null, phone, object : BarikoiTrace.TraceUserCallback { ... })
BarikoiTrace.setOfflineTracking(true)
BarikoiTrace.startTracking(TraceMode.Builder().setUpdateInterval(10).build())
BarikoiTrace.updateCurrentLocation(object : BarikoiTrace.TraceLocationUpdateCallback { ... })
BarikoiTrace.stopTracking()
```

`DemoActivity.kt` — new, richer reference implementation: reads the API key from `BuildConfig.API_KEY` (flavor-injected), lets the user override it at runtime via a dialog + re-`initialize()`, exposes `setBaseUrl`/`setMqttUrl` fields, wires `BarikoiTrace.setLogListener(...)` to a live on-screen log console, and exercises `startTracking(mode, withTrip)` with a trip toggle switch plus `getTripId()` status display.

---

## 9. Dependencies (new/changed vs. the previous branch)

Retrofit 2.11.0 + OkHttp 4.12.0 + `converter-gson` (replaces Volley), `com.github.hannesa2:paho.mqtt.android:4.4` (new, MQTT), `androidx.datastore:datastore-preferences:1.1.4` (replaces raw SharedPreferences), `androidx.room:room-runtime/room-ktx:2.6.1` + KSP compiler (replaces hand-written `SQLiteOpenHelper`), `androidx.work:work-runtime-ktx:2.10.0`, `kotlinx-coroutines-android:1.9.0`, `com.google.code.gson:2.11.0` (now actually used, unlike the previous branch's dead Gson dependency), `com.android.tools:desugar_jdk_libs:2.1.4` (Java 8+ desugaring, new). Test-only: MockK, kotlinx-coroutines-test, Google Truth, Robolectric, Room testing artifact.
