# BarikoiTrace (Android)

Native Android SDK for background location tracing. Authenticates a user
against the Barikoi Trace backend, streams their location to an MQTT broker
while the app runs — foreground *or* background — and queues fixes to disk when
the network is gone, flushing them when it returns.

Distributed through JitPack. The iOS SDK
([`BarikoiTrace-ios-sdk`](https://github.com/barikoi/BarikoiTrace-ios-sdk))
mirrors this one's feature set and public API shape, so call sites read the same
on both platforms.

- **Requirements:** minSdk 24, compileSdk 35, Kotlin 2.x, Java 8 desugaring
- **Key dependencies:** Paho MQTT (Android), Play Services Location, Room, DataStore, WorkManager
- **License:** MIT

---

## Table of contents

- [Installation](#installation)
- [How it works](#how-it-works)
- [Required app setup](#required-app-setup)
- [Configuration — base URL and MQTT broker](#configuration--base-url-and-mqtt-broker)
- [Where to put your API key](#where-to-put-your-api-key)
- [Quick start](#quick-start)
- [API reference](#api-reference)
- [Tracking modes](#tracking-modes)
- [Offline behavior](#offline-behavior)
- [MQTT contract](#mqtt-contract)
- [Error handling](#error-handling)
- [Background execution — read this before shipping](#background-execution--read-this-before-shipping)
- [Platform differences from the iOS SDK](#platform-differences-from-the-ios-sdk)
- [Example app](#example-app)
- [Building and testing](#building-and-testing)
- [Releasing](#releasing)

---

## Installation

Root `settings.gradle` (or `build.gradle`):

```groovy
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

App module:

```groovy
dependencies {
    implementation 'com.github.barikoi:barikoitrace:2.0.0'
}
```

`2.0.0` is a **breaking** release: `initialize()` now requires MQTT
credentials. The hardcoded broker constants that shipped in every earlier
version are gone — see [Where to put your API key](#where-to-put-your-api-key).

---

## How it works

```
FusedLocationProvider ──▶ LocationEngine ──▶ LocTraceForegroundService ──┬──▶ MqttManager ──▶ broker
                                                                        │
                                                                        └──▶ OfflineLocationDb (Room)
                                                                                   │  network back
                                                                                   └──▶ flush, batch of 100
```

| Component | Responsibility |
|---|---|
| `TraceApiClient` | `POST /sdk/authenticate`, `POST /sdk/company/settings`. Retrofit + coroutines. |
| `LocationEngine` | `FusedLocationProviderClient` wrapper — continuous updates and one-shot fetch. |
| `MqttManager` | Paho wrapper. Topic resolution, LWT, QoS 1, exponential-backoff reconnect, permanent-refusal detection. |
| `OfflineLocationDb` | Room-backed durable queue. Survives process death — not an in-memory buffer. |
| `LocTraceForegroundService` | The foreground service that owns the tracking session: validation, publish-or-queue, offline flush. |
| `LocTraceDataService` | `WorkManager` job — periodic fix + queue flush when the service is starved. |
| `LocTraceManager` | Orchestrator. Auth state, mode, trip state, service lifecycle. |
| `BarikoiTrace` | The public facade. The only type you call against. |

Credentials and user identity live in `EncryptedSharedPreferences`
(`SecureStore`); non-secret runtime config in DataStore Preferences
(`TraceDataStore`). Same split as the iOS SDK's Keychain/UserDefaults.

---

## Required app setup

A library cannot grant its own permissions. These steps are the host app's job,
and tracking will silently underperform without them.

### 1. `AndroidManifest.xml`

The SDK's own manifest already declares its permissions, service and receivers,
and they merge into yours. You only need to add a foreground-service type if
your `targetSdk` is 34+ and you override the service declaration.

Permissions the SDK requests on your behalf:

```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
```

### 2. Runtime permissions, in order

Android will not grant background location in the same prompt as foreground
location, and asking in the wrong order gets you a silent denial:

```kotlin
BarikoiTrace.requestLocationPermissions(activity)        // fine/coarse first
BarikoiTrace.requestBackgroundLocationPermission(activity) // then "Allow all the time"
BarikoiTrace.requestNotificationPermission(activity)      // Android 13+, for the service notification
```

### 3. OEM process-kill workarounds

Chinese OEM builds (MIUI, EMUI, ColorOS, …) kill background services
aggressively regardless of what Android says. Offer these to the user:

```kotlin
BarikoiTrace.requestDisableBatteryOptimization(context)
BarikoiTrace.openAutostartSettings(context)
```

### 4. Credentials

`initialize` needs an API key **and** broker credentials. See
[Configuration](#configuration--base-url-and-mqtt-broker) for the endpoints and
[Where to put your API key](#where-to-put-your-api-key) for the secrets.

---

## Configuration — base URL and MQTT broker

The SDK reads no config file, no manifest `meta-data` and no environment
variable. You hand it a `TraceConfig`, and that is the entire contract:

```kotlin
BarikoiTrace.initialize(
    context,
    TraceConfig(
        apiKey = "…",        // Barikoi dashboard
        mqttUsername = "…",  // issued separately, per company
        mqttPassword = "…"
    )
)
```

Endpoints default to production and are overridable for staging or a
self-hosted deployment:

```kotlin
val config = TraceConfig(
    apiKey = BuildConfig.API_KEY,
    mqttUsername = BuildConfig.MQTT_USERNAME,
    mqttPassword = BuildConfig.MQTT_PASSWORD,
    baseUrl = "https://api.staging.example.com/api/v1/",
    mqttUrl = "ssl://broker.staging.example.com:8883",
    mqttClientIdPrefix = "fleet-android-"  // only if the broker ACL matches on client id
)

check(config.warnings.isEmpty()) { config.warnings.toString() }  // plaintext broker, non-HTTPS API, empty key…
BarikoiTrace.initialize(context, config)
```

Configure through `TraceConfig` rather than calling `setBaseUrl`/`setMqttUrl`
after `initialize`. `initialize` resumes tracking if the previous process was
tracking, and a resumed session starts the foreground service — and with it the
MQTT client — immediately, so endpoints set afterwards arrive too late for that
first connection.

| Field | Default | |
|---|---|---|
| `apiKey` | — | required |
| `mqttUsername` / `mqttPassword` | — | required |
| `baseUrl` | `https://api.trace.bmapsbd.com/api/v1/` | trailing slash normalized |
| `mqttUrl` | `tcp://broker.trace.bmapsbd.com:1883` | **plaintext** — see below |
| `mqttClientIdPrefix` | `AndroidClient-` | iOS uses `iOSClient-` |

`mqttUrl` accepts `tcp`/`mqtt`/`ws` (plaintext) and `ssl`/`mqtts`/`tls`/`wss`
(TLS); Paho reads the scheme and the port from the URL.
`config.isMqttTransportEncrypted` tells you which you got — the SDK default is
plaintext, meaning both broker credentials and every location fix travel
unencrypted. Point it at a TLS listener for anything carrying real user
locations.

### Changing endpoints mid-session

`setBaseUrl`, `setMqttUrl` and `setMqttClientIdPrefix` exist for switching a
*running* app between environments. `setBaseUrl` is destructive on purpose —
a different backend means a different user namespace, so it clears the cached
user and stops tracking. `resetUrls()` returns to the SDK defaults and does the
same.

```kotlin
BarikoiTrace.setBaseUrl("https://api.staging.example.com/api/v1/")
BarikoiTrace.setMqttUrl("ssl://broker.staging.example.com:8883")
BarikoiTrace.setMqttClientIdPrefix("fleet-android-")   // before startTracking
```

---

## Where to put your API key

The API key and the MQTT username/password are **three separate secrets**. None
is derivable from the others, and the broker pair is not a public identifier —
a leaked pair lets anyone publish fixes to your company's topics.

Where the credential *values* come from is your app's decision. Three options,
in increasing order of safety.

### Option A — `local.properties` → `BuildConfig` (local development)

`local.properties` is git-ignored by default in every Android project:

```properties
BARIKOI_API_KEY=your_key
BARIKOI_MQTT_USERNAME=your_username
BARIKOI_MQTT_PASSWORD=your_password
```

`app/build.gradle`:

```groovy
def localProps = new Properties()
def f = rootProject.file('local.properties')
if (f.exists()) f.withInputStream { localProps.load(it) }

android.defaultConfig {
    buildConfigField "String", "API_KEY", "\"${localProps.getProperty('BARIKOI_API_KEY', '')}\""
    buildConfigField "String", "MQTT_USERNAME", "\"${localProps.getProperty('BARIKOI_MQTT_USERNAME', '')}\""
    buildConfigField "String", "MQTT_PASSWORD", "\"${localProps.getProperty('BARIKOI_MQTT_PASSWORD', '')}\""
}
```

### Option B — build-type or flavor values (per-environment builds)

Put staging and production values in their own `buildTypes`/`productFlavors`
blocks so a debug build cannot reach production credentials.

### Option C — issued by your own backend (production)

Your server hands the broker credentials to the app after its own login, and
the app calls `initialize` once they arrive. This is the only option where a
decompiled APK yields nothing.

### Rules regardless of option

- `BuildConfig` fields are **not secret** — they are string constants in the
  APK, readable with `apktool` in seconds. Option A/B keep secrets out of *git*,
  not out of the *binary*.
- Never commit real values to `build.gradle`, source, or a checked-in
  properties file.
- Rotate anything that has ever been committed. Removing it from `HEAD` does
  not remove it from history, and it does not un-leak a published artifact.

---

## Quick start

```kotlin
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // One call, endpoints included.
        BarikoiTrace.initialize(
            this,
            TraceConfig(
                apiKey = BuildConfig.API_KEY,
                mqttUsername = BuildConfig.MQTT_USERNAME,
                mqttPassword = BuildConfig.MQTT_PASSWORD
            )
        )

        BarikoiTrace.setLogListener(object : BarikoiTrace.TraceLogListener {
            override fun onLog(level: String, tag: String, message: String) {
                Log.println(Log.INFO, tag, "$level $message")
            }
        })
    }

    private fun signInAndTrack() = lifecycleScope.launch {
        try {
            BarikoiTrace.setOrCreateUser(name = "Jane", email = null, phone = "01700000000")
            BarikoiTrace.startTracking(TraceMode.ACTIVE, withTrip = true)
        } catch (e: TraceException) {
            Log.e("Trace", "${e.code}: ${e.message}")
        }
    }

    private fun observeLocations() = lifecycleScope.launch {
        BarikoiTrace.setBroadcastingEnabled(true)
        BarikoiTrace.locationUpdates.collect { location ->
            // live fixes, only while broadcasting is enabled
        }
    }
}
```

Java callers use the callback overloads (`setOrCreateUser(..., TraceUserCallback)`,
`updateCurrentLocation(TraceLocationUpdateCallback)`,
`getSettingsFromRemote(TraceSettingsCallback)`) instead of the `suspend`
functions.

---

## API reference

Everything public is a static member of the `BarikoiTrace` object.

### Lifecycle

| Method | Notes |
|---|---|
| `initialize(context, config: TraceConfig)` | Call once, first, before anything else. |
| `initialize(context, apiKey, mqttUsername, mqttPassword)` | Convenience — forwards to the above with default endpoints. |
| `setLogListener(listener)` | Implement `TraceLogListener` to pipe SDK logs into your own debug console. |

Tracking resumes on its own after a reboot (`BootReceiver`) and after a process
restart, provided it was active when the app went away.

### Endpoints

Set these through `TraceConfig` at `initialize`. The setters below exist for
changing endpoints mid-session — switching a running app between staging and
production, say — and re-point the MQTT client on the next fix.

| Method | Notes |
|---|---|
| `setBaseUrl(url)` | Clears the cached user and stops tracking when the value changes. |
| `setMqttUrl(url)` | |
| `setMqttClientIdPrefix(prefix)` | Call before `startTracking`. |
| `resetUrls()` | Back to the SDK defaults. |

### User

| Method | Notes |
|---|---|
| `setOrCreateUser(name, email, phone): TraceUser` | `suspend`. Authenticates or creates. Throws `TraceError`. Also refreshes remote settings — that refresh swallows its own failure by design (a secondary step should not fail the primary auth call). |
| `getUser(): TraceUser?` | Cached user, no network. |
| `getUserId(): String?` | |

`TraceUser` carries `userId`, `name`, `email`, `phone`, `companyId`, `group`,
`updatedAt` (epoch ms). `companyId` and `group` are required — MQTT topic
resolution needs them.

### Permissions

| Method | Notes |
|---|---|
| `isLocationPermissionsGranted(): Boolean` | |
| `isLocationSettingsOn(): Boolean` | Device-level Location Services. |
| `requestLocationPermissions(activity)` | Fine/coarse. |
| `requestBackgroundLocationPermission(activity)` | "Allow all the time". Foreground must already be granted. |
| `requestNotificationPermission(activity)` | Android 13+. |
| `requestLocationServices(activity)` | Opens the system Location Services toggle. |
| `requestDisableBatteryOptimization(context)` | |
| `isBatteryOptimizationEnabled(): Boolean` | |
| `checkAppServicePermission(context)` | Conditional battery-optimization prompt. |
| `openAutostartSettings(context)` | OEM autostart screens. |

### Tracking

| Method | Notes |
|---|---|
| `startTracking(mode)` / `startTracking(mode, withTrip)` | `withTrip` attaches a locally generated trip UUID. |
| `stopTracking()` | If a trip was open, publishes the completed-trip payload. |
| `setTraceMode(mode)` | |
| `isLocationTracking(): Boolean` | Reads the real service state, not a stored flag. |
| `setOfflineTracking(enabled)` | Toggles the Room queue. Defaults to on. |
| `setBroadcastingEnabled(enabled)` | Gates `locationUpdates`. |
| `setLoggingEnabled(enabled)` | |

### Trips

`isOnTrip(): Boolean`, `getTripId(): String?`, `getCurrentTrip(): String?`.
Trip IDs are generated on device, not issued by the server.

### Location and sync

| Method | Notes |
|---|---|
| `updateCurrentLocation(): Location` | `suspend`. One-shot fix. |
| `uploadOfflineData()` | Forces a flush of the queue. |
| `getSettingsFromRemote(): TraceMode` | `suspend`. Explicit fetch — unlike the implicit refresh inside `setOrCreateUser`, this **throws** on failure. |
| `locationUpdates: SharedFlow<Location>` | Live stream. Requires `setBroadcastingEnabled(true)`. |

---

## Tracking modes

`TraceMode` has three presets:

| Preset | Accuracy | Interval | Distance filter | Accuracy filter | Ping sync |
|---|---|---|---|---|---|
| `ACTIVE` | high | 5s | — | 50m | — |
| `REACTIVE` | high | — | 100m | 100m | 30s |
| `PASSIVE` | medium | — | 100m | 300m | 120s |

`updateInterval` and `distanceFilter` are mutually exclusive: whichever is
non-zero decides whether tracking is time-based or movement-based.
`accuracyFilter` rejects any fix with worse horizontal accuracy than the given
metres.

Custom modes go through the builder, which enforces floors of interval ≥ 5s,
distance ≥ 10m, accuracy ≥ 20m:

```kotlin
val mode = TraceMode.Builder()
    .setDesiredAccuracy(TraceMode.DesiredAccuracy.HIGH)
    .setDistanceFilter(50)      // metres
    .setAccuracyFilter(30)
    .setPingSyncInterval(60)
    .setOfflineSync(true)
    .setStartTime(LocalTime.of(8, 0))   // daily tracking window
    .setEndTime(LocalTime.of(20, 0))
    .build()

BarikoiTrace.startTracking(mode)
```

`startTime`/`endTime` define a daily window; outside it, the service stops
itself. A window that wraps past midnight (22:00–06:00) is the union of both
sides. Defaults to the full day.

---

## Offline behavior

When the network is unavailable — or the MQTT connection is down — fixes go to
a Room table rather than a memory buffer, so they survive the app being killed
or the device rebooting. On reconnect they flush in batches of 100, oldest
first, and are deleted only after the connection is confirmed still up.

Rows queued before authentication get `user_id`, `company_id` and `user_name`
backfilled at flush time, so nothing is published unattributed.

Disable with `setOfflineTracking(false)` or
`TraceMode.Builder().setOfflineSync(false)`. Force a flush with
`uploadOfflineData()`. `LocTraceDataService` (WorkManager, ~15 min) also takes a
fix and flushes when the foreground service is starved.

---

## MQTT contract

**Location topic:** `company/{companyId}/{groupId}/{userId}/location`
**LWT topic:** `device/{userId}/status`, retained, payload `offline`
**Client ID:** `{prefix}{userId}-{deviceUuid}` — QoS 1 throughout.

Payload:

```json
{
  "latitude": 23.8103,
  "longitude": 90.4125,
  "altitude": 4.0,
  "speed": 1.4,
  "bearing": 275.0,
  "accuracy": 12.0,
  "gpx_time": "2026-09-02 11:04:38",
  "user_id": "…",
  "company_id": "…",
  "user_name": "Jane",
  "trip_id": "…",
  "trip_status": "active"
}
```

`trip_id`/`trip_status` appear only while on a trip; stopping publishes a final
full payload with `trip_status: "completed"`. `gpx_time` uses one UTC string
format on every path — live publish, offline insert and offline flush alike.

A CONNACK of `notAuthorized`, `badUsernameOrPassword` or `identifierRejected`
is treated as permanent: the SDK stops the retry ladder and reports through
`MqttStatusCallback.onConnectionRejected`, because the same CONNECT will be
refused every time. Check the credentials, then the broker's client-id ACL —
Android connects as `AndroidClient-…` and iOS as `iOSClient-…`, so an ACL
written for one platform refuses the other.

---

## Error handling

`suspend` methods throw `TraceException`, which wraps a `TraceError` carrying a
stable string `code` and a human-readable `message`. It extends `Exception`, so
existing `catch (e: Exception)` blocks keep working.

```kotlin
try {
    val user = BarikoiTrace.setOrCreateUser("Jane", null, phone)
} catch (e: TraceException) {
    when (e.code) {
        "NO_KEY" -> {}      // initialize() was never called
        "NO_COMPANY" -> {}  // user has no company — cannot resolve an MQTT topic
        "NETWORK" -> {}     // no connectivity
        "PERMISSION" -> {}  // location permission not granted
        "SERVER" -> {}      // backend 5xx
    }
}
```

Java callers get the same `TraceError` through the callback overloads'
`onFailure`.

Codes: `NO_USER`, `NO_KEY`, `NO_DATA`, `NETWORK`, `PERMISSION`, `LOCATION`,
`SERVER`, `TRIP`, `MOCK`, `JSON`, `NO_COMPANY`. Identical to the iOS SDK's.

---

## Background execution — read this before shipping

Tracking runs in a foreground service with an ongoing notification, which is
what keeps it alive. What still stops it:

- **Battery optimization** — the single most common cause of a silently dead
  session. Offer `requestDisableBatteryOptimization`.
- **OEM autostart managers** — MIUI, EMUI and friends kill services regardless
  of Android's rules. Offer `openAutostartSettings`.
- **Background location revoked** — Android 11+ lets the user downgrade
  "Allow all the time" to "Only while using" at any time.
- **Doze** — location delivery slows for a stationary device even with
  everything granted.

The service restarts itself (`START_STICKY`) and resumes after reboot
(`BootReceiver`) when tracking was active.

---

## Platform differences from the iOS SDK

By design, not oversight.

| Behavior | Android | iOS |
|---|---|---|
| Background execution | Persistent foreground `Service` | Bounded wake windows |
| Resume after force-kill | `BOOT_COMPLETED` receiver, any reboot | Significant-location-change only (~500m); a stationary killed app stays dead |
| Mock-location detection | `Location.isMock` | `CLLocation.sourceInformation.isSimulatedBySoftware` |
| Battery-optimization exemption | Requestable | No equivalent — omitted |
| Autostart / OEM process-kill workarounds | Six vendor-specific methods | Not applicable |
| Degraded-capability signal | Not needed | `isBackgroundTrackingDegraded` — iOS-only |
| Async style | `suspend` + a callback API for Java interop | `async`/`await` only |
| Error type | `TraceException` wrapping `TraceError` | `TraceError` (a `struct: Error`) |
| Live updates | `SharedFlow<Location>` | `AsyncStream<CLLocation>` |
| Secret storage | `EncryptedSharedPreferences` | Keychain |
| Default client-id prefix | `AndroidClient-` | `iOSClient-` |

Everything else — `TraceConfig`, the tracking modes and their floors, the topic
and payload contract, the error codes, the offline-queue semantics — is the
same on both platforms on purpose, so one wrapper can drive either.

---

## Example app

[`app/`](app) — a demo covering the full flow: initialization, permission
requests in the correct order, sign-in, start/stop with a trip, live updates
and a debug log console (`DemoActivity`). Credentials come from
`local.properties` as described above.

---

## Building and testing

```bash
./gradlew :barikoitrace:assembleRelease
./gradlew :barikoitrace:testDebugUnitTest          # Robolectric + MockK
./gradlew :barikoitrace:connectedDebugAndroidTest  # Room DAO tests, needs a device
```

**Unit tests cannot validate background execution.** Before shipping, exercise
the real matrix on real hardware: sustained movement, stationary for hours,
battery optimization on, background location revoked mid-session, force-kill
then reboot.

---

## Releasing

JitPack builds from git tags:

```bash
git tag -a 2.0.1 -m "2.0.1" && git push origin 2.0.1
```

Keep `version` in `barikoitrace/build.gradle.kts` in step with the tag. Tags are
immutable to consumers — never move one, ship a patch instead.

---

## Further reading

- [`docs/SDK_DOCUMENTATION.md`](docs/SDK_DOCUMENTATION.md) — architecture and API detail
- [`docs/IOS_NATIVE_LIBRARY_WORK_PLAN.md`](docs/IOS_NATIVE_LIBRARY_WORK_PLAN.md) — the plan the iOS port was built from
