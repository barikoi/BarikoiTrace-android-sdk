# iOS Native Library — Work Plan

Mirrors `barikoitrace` (Android, `dev-v3`, Kotlin) feature-for-feature in Swift, with true background location tracing as the primary design constraint. No Flutter/Dart scope in this plan — this is a standalone native iOS library, same relationship to a future Flutter bridge as `barikoitrace` already has today (consumable on its own, bridgeable later).

Reference: `docs/SDK_DOCUMENTATION.md` in this repo (dev-v3 feature/API audit) — every phase below maps to specific Kotlin files from that doc.

---

## 1. Target module structure (mirrors the Kotlin package layout)

```
BarikoiTrace.xcframework (or SPM package)
  Sources/BarikoiTrace/
    BarikoiTrace.swift              — public facade, mirrors BarikoiTrace.kt (object → enum/static API)
    TraceManager.swift               — orchestrator singleton, mirrors LocTraceManager.kt
    TraceMode.swift                  — mirrors TraceMode.kt (struct + Builder + presets)
    Model/
      TraceUser.swift                — mirrors TraceUser.kt
      TraceError.swift               — mirrors TraceError.kt
    API/
      TraceApiRoutes.swift           — mirrors ApiRoutes.kt
      TraceApiClient.swift           — mirrors TraceApiClient.kt (URLSession, async/await)
    Location/
      TraceLocationEngine.swift      — mirrors LocationEngine.kt (CLLocationManager wrapper)
      LocationUpdateListener.swift   — mirrors LocationUpdateListener.kt (protocol)
    Mqtt/
      TraceMqttClient.swift          — mirrors MqttManager.kt
    Storage/
      TraceDataStore.swift           — mirrors TraceDataStore.kt (Keychain + UserDefaults)
      OfflineLocationStore.swift     — mirrors OfflineLocationDb.kt / OfflineLocationDao.kt (SQLite/GRDB)
    Background/
      TraceBackgroundCoordinator.swift — mirrors LocTraceForegroundService.kt + LocTraceDataService.kt, no direct 1:1 (see §3)
    Util/
      SystemSettingsManager.swift    — mirrors SystemSettingsManager.kt
      NetworkChecker.swift           — mirrors NetworkChecker.kt
      DateTimeUtils.swift            — mirrors DateTimeUtils.kt
  Tests/BarikoiTraceTests/           — XCTest, mirrors the 6 existing Kotlin test files
```

---

## 2. Phase 0 — Contract & parity spec (before any Swift code)

Lock these in writing, reviewed against the Kotlin source directly (not from memory/docs), before Phase 1 starts:

- **Models**: `TraceMode` fields, defaults, and builder floors — exact match to `TraceMode.kt` (`updateInterval` floors at 5s, `distanceFilter` floors at 10m, `accuracyFilter` floors at 20m; `ACTIVE`/`PASSIVE`/`REACTIVE` presets numerically identical). `TraceUser` fields (`userId, name, email, phone, companyId, group, lastLat, lastLon, updatedAt`). `TraceError` string codes (`NO_USER, NO_KEY, NO_DATA, NETWORK, PERMISSION, LOCATION, SERVER, TRIP, MOCK, JSON`).
- **REST**: `/sdk/authenticate` and `/sdk/company/settings` — same request/response JSON shapes as `TraceApiClient.kt`. Same `companies[0]` requirement (throw if a user has zero companies).
- **MQTT**: same topic pattern `company/{companyId}/{groupId}/{userId}/location`, same LWT (`device/{userId}/status = offline`, retained), same QoS (1), same payload fields. **Fix the Kotlin branch's own bug here rather than porting it**: `gpx_time` must use one consistent format (recommend ISO-8601 UTC string) on both the live-publish and offline-flush paths — the Kotlin code currently uses epoch-ms on one path and a formatted string on the other; don't replicate that on iOS, and consider back-porting the fix to Kotlin once iOS establishes the correct shape.
- **MQTT auth**: the Kotlin library currently has hardcoded broker username/password baked into `MqttManager.kt` as constants. Do not carry that pattern into Swift. Decide the real auth story now — token issued by `/sdk/authenticate`, or per-app config injected at build time at minimum — and treat "remove the Kotlin hardcoded constant too" as a linked follow-up, not something iOS quietly fixes while Android stays exposed.
- **Offline queue**: same batch size (100), same "insert raw JSON row, batch-delete oldest 100 on flush" shape as `OfflineLocationDao.kt`.
- **Trip lifecycle**: confirmed local-UUID-only, no server trip authority (matches Kotlin's current design — this was a deliberate architecture change from the older Java branch, not an oversight). Decide explicitly what happens to the `trip_status: "completed"` event when the process is killed before it can publish — Kotlin's `LocTraceForegroundService.onDestroy()` has the same gap (only fires on a clean stop, not a force-kill). For iOS this is more likely to happen (iOS terminates backgrounded apps more aggressively than Android), so pick one: accept the gap and document it, or add a "reconcile dangling trip on next launch" step. Write the decision down.

**Exit criteria:** a written spec doc (or the Swift model files themselves, stubbed with doc comments, opened as a draft PR) reviewed by whoever owns the Kotlin SDK.

**Estimate:** 3–4 days.

---

## 3. True background tracking on iOS — design before Phase 5

This is the part with no clean 1:1 Android mapping and the part most likely to under-deliver if under-designed. Read this section before writing `TraceBackgroundCoordinator`.

**What Android has that iOS fundamentally does not:** a persistent foreground `Service` the app controls directly, running indefinitely with a guaranteed-visible notification, restartable on boot via a manifest-registered receiver. iOS has no equivalent primitive — there is no "just keep my process running." Background execution on iOS is always the OS waking your app for a bounded window in response to a specific trigger. The design goal is to make those windows frequent and reliable enough that the *user-visible outcome* matches Android, even though the *mechanism* is completely different.

**The trigger stack, layered from primary to fallback:**

1. **`CLLocationManager` background location delivery** (primary mechanism). Requires `Always` authorization + `UIBackgroundModes: location` in `Info.plist` + `allowsBackgroundLocationUpdates = true` + `pausesLocationUpdatesAutomatically = false`. While these are set and the app has `Always` permission, iOS wakes the app for each qualifying location update even when backgrounded/suspended, for as long as the OS judges the app to have a legitimate ongoing reason (this is why the purpose string in Phase 7 matters — it's not just an App Review formality, weak justification correlates with the OS deprioritizing the app over time too).
2. **Significant-location-change monitoring** (`startMonitoringSignificantLocationChanges`) as a low-power fallback *and*, critically, as the mechanism that **relaunches a terminated app**. If iOS or the user has killed the process, standard location updates do not resume it — significant-location-change is the one API that does, delivering `UIApplicationLaunchOptionsLocationKey` in `application(_:didFinishLaunchingWithOptions:)`. This is the iOS analog to `BootReceiver.kt`, except it triggers on movement past a threshold (~500m/cell change), not on device boot — a stationary killed app will not silently resume the way an Android device reboot resumes tracking via `BOOT_COMPLETED`. Document this behavioral difference explicitly for product; it is a real capability gap, not a bug to "just fix."
3. **`BGAppRefreshTask` / `BGProcessingTask`** (via `BGTaskScheduler`) as the periodic offline-queue-flush mechanism — the iOS analog to `LocTraceDataService.kt`. Register task identifiers in `Info.plist`, submit chained requests (each run reschedules the next) inside `TraceBackgroundCoordinator`. **Do not repeat the Kotlin branch's actual bug here**: `LocTraceDataService` is fully implemented on Android but is never scheduled anywhere in the codebase — it's dead code today. On iOS, scheduling is not optional infrastructure to add later; without it there is no periodic flush path at all when location-triggered wakes are infrequent (e.g. device stationary for hours).

**What "connect to MQTT" means inside a background window:** never attempt to hold a persistent socket open across the background lifetime — iOS does not support this reliably and it will not survive App Review scrutiny or the OS's own resource reclamation. Each wake window (location delivery or `BGTask` execution) does a fast connect → publish/flush → disconnect. This means the MQTT client (`TraceMqttClient`, Phase 4 in the previous plan) needs a connect path that's fast and idempotent, not a long-lived-connection assumption ported from the Kotlin implementation, which *does* hold a persistent MQTT connection because Android's foreground service allows it.

**State restoration on relaunch:** `TraceManager` must be able to fully reconstruct tracking state (`TraceMode`, user identity, active trip id) from `TraceDataStore` (Keychain/UserDefaults) at cold-start-via-location-launch, with no assumption that any in-memory state survived — because it didn't. This is a stronger requirement than the Kotlin side's `initialize()`, which only needs to resume after a normal process restart, not reconstruct from a location-triggered relaunch with a different call sequence.

**Constraints to surface to the app, not hide:** Low Power Mode throttles background location delivery; a user can disable "Background App Refresh" for the app entirely (silently breaks the `BGTask` flush path, not the `CLLocationManager` path); a user can downgrade `Always` to `While Using` at any time in Settings without the app being notified synchronously. `TraceManager` should expose a queryable status (`isBackgroundTrackingDegraded` or similar) reflecting these so host apps can surface it to end users, rather than the SDK silently under-delivering with no signal — this is a genuine feature Android's SDK doesn't need and iOS's does.

**Realistic expectation to set with product now:** with all of the above correctly implemented, an actively-moving device gets background updates close to the configured interval; a stationary device's update cadence degrades to whatever `BGTaskScheduler` and the OS grant, typically much coarser than the configured `intervalMs`. This is the ceiling of what's achievable within Apple's public APIs — it will not be identical to Android's persistent-service cadence in the stationary case, and that should be stated as a known platform difference, not discovered during QA.

---

## 4. Phases

### Phase 1 — Project scaffold + models + permissions (~1 week)
- SPM package or xcframework scaffold, CI-buildable from day one.
- `TraceMode`, `TraceUser`, `TraceError` per Phase 0's spec.
- `TracePermissionManager` (or a `SystemSettingsManager.swift` extension): `CLLocationManager` authorization mapping, `Always`-then-`WhenInUse` request flow, notification permission (`UNUserNotificationCenter`). No Android battery-optimization equivalent — omit cleanly, document the platform difference rather than a silent no-op.
- Maps to: `TraceMode.kt`, `TraceUser.kt`, `TraceError.kt`, `SystemSettingsManager.kt`.

### Phase 2 — Location engine (~1 week)
- `TraceLocationEngine` wrapping `CLLocationManager`: accuracy mapping (HIGH→`kCLLocationAccuracyBest`, MEDIUM→`kCLLocationAccuracyHundredMeters`, LOW→`kCLLocationAccuracyKilometer`), `distanceFilter`, background-delivery flags per §3, one-shot `getCurrentLocation()` via `async/await` continuation (mirrors `LocationEngine.getCurrentLocation()`).
- Mock-location parity: iOS has no `Location.isMock` equivalent — explicitly decide to omit this check (likely correct answer) rather than attempt a weak heuristic, and document it as a platform difference.
- Maps to: `LocationEngine.kt`.

### Phase 3 — Offline queue (~3–4 days)
- SQLite (GRDB.swift recommended, or raw SQLite3) single table matching `OfflineLocationEntity`/`OfflineLocationDao`: `insert`, `getCount`, `getBatch(100)`, `deleteBatch(100)`.
- Maps to: `OfflineLocationDb.kt`, `OfflineLocationDao.kt`, `OfflineLocationEntity.kt`.

### Phase 4 — MQTT client (~1 week)
- Wraps CocoaMQTT (or equivalent MQTT 3.1.1 client): same topic/LWT/QoS as Phase 0, **same exponential backoff policy as `MqttManager.kt`** (5s base, doubling, capped 60s, bounded attempts), fast connect→publish→disconnect cycle per §3 rather than a held-open connection.
- Connection state exposed as a stream/delegate (`disconnected/connecting/connected/reconnecting`).
- Maps to: `MqttManager.kt`.

### Phase 5 — Background orchestration (~2 weeks — expanded from the earlier estimate given §3's scope)
- `TraceManager`: `initialize`, `startTracking(mode, withTrip:)`, `stopTracking`, trip UUID lifecycle, config/URL overrides — mirrors `LocTraceManager.kt`'s public surface.
- `TraceBackgroundCoordinator`: implements the full trigger stack from §3 — `CLLocationManager` background delivery, significant-location-change monitoring + relaunch handling in `AppDelegate`/`SceneDelegate` integration points, `BGTaskScheduler` registration **and actual scheduling** (unlike the Kotlin branch's unscheduled `LocTraceDataService`), state restoration on location-triggered cold launch.
- Degraded-state reporting (Low Power Mode, Background App Refresh disabled, downgraded authorization) as a queryable property.
- Maps to: `LocTraceManager.kt`, `LocTraceForegroundService.kt`, `LocTraceDataService.kt`, `BootReceiver.kt` (partial/different-trigger analog).

### Phase 6 — Tests (~4–5 days, overlaps 1–5)
- XCTest suite mirroring `TraceModeTest`, `TraceErrorTest`, `TraceDataStoreTest`, `DateTimeUtilsTest`, plus an offline-queue test mirroring `OfflineLocationDaoTest`.
- Contract test: assert the MQTT JSON payload produced for a given `TraceLocation` input matches the Kotlin payload shape field-for-field (this is what actually prevents the `gpx_time`-style drift from recurring — a doc saying "match the schema" isn't enough on its own).
- Background-execution test plan (necessarily partly manual/on-device — `BGTaskScheduler` and background location delivery aren't fully simulator-testable): documented device-test matrix covering active-movement, stationary, Low Power Mode, Background App Refresh disabled, and force-kill-then-relaunch-via-significant-location-change scenarios.

**Phase 1–6 subtotal: ~6–7 engineer-weeks.**

---

## 5. Phase 7 — App Store readiness (~2–3 days concentrated, plus review wait time)

- `NSLocationAlwaysAndWhenInUseUsageDescription` copy specifically justifying continuous background tracking as a core, visible app feature — current App Review guidance for background-location apps is stricter than a generic purpose string; verify against Apple's live documentation before submission rather than reusing older boilerplate.
- Background Modes capability: `location` + `processing` (for `BGProcessingTask`) declared and justified in App Review notes, with a demo account/flow reviewers can actually exercise to see the feature working.
- Budget 1–2 rejection/resubmission cycles into the release schedule as a scheduling risk, communicated to whoever owns the ship date now.

---

## 6. Phase 8 — CI/CD (~2 days)

- `xcodebuild test` (or `swift test` for an SPM package) gated on PR, matching whatever gate level the Kotlin repo has (currently none — worth fixing there too, but out of scope for this plan beyond noting it).
- Version the framework independently, tag releases, changelog discipline matching the Kotlin repo's `CHANGELOG.md` pattern for consistency.

---

## 7. Defect carry-forward checklist (don't port these as-is)

| Defect | Source (Kotlin) | Action for iOS |
|---|---|---|
| Hardcoded MQTT broker username/password | `MqttManager.kt` | Phase 0 — real auth strategy, and flag Kotlin for the same fix |
| `LocTraceDataService` implemented but never scheduled | `dev-v3` Android | Phase 5 — schedule `BGTaskScheduler` for real; this is not optional on iOS the way it accidentally became optional on Android |
| `gpx_time` format differs between live and offline-flush MQTT payloads | `dev-v3` Android | Phase 0 — pick one format, use it everywhere, consider back-porting to Kotlin |
| Trip `"completed"` event lost on force-kill (no reconciliation) | `LocTraceForegroundService.onDestroy()` | Phase 0 — explicit decision; more likely to matter on iOS given more aggressive OS termination |
| No visibility into degraded background capability (Low Power Mode, permission downgrade, Background App Refresh off) | N/A — Android doesn't need this | Phase 5 — new capability, not a port; iOS-specific requirement |

---

## 8. Effort rollup

| Phase | Estimate |
|---|---|
| 0 — Contract & parity spec | 3–4 days |
| 1 — Scaffold, models, permissions | 1 week |
| 2 — Location engine | 1 week |
| 3 — Offline queue | 3–4 days |
| 4 — MQTT client | 1 week |
| 5 — Background orchestration | 2 weeks |
| 6 — Tests | 4–5 days |
| 7 — App Store readiness | 2–3 days + review wait (not engineer time) |
| 8 — CI/CD | 2 days |

**Total: roughly 7–8 engineer-weeks** for one iOS engineer end to end. Phase 5 is the long pole and the one least compressible by adding people (it's one coherent state machine); Phases 1–4 could plausibly parallelize across two engineers if available, saving perhaps a week and a half of wall-clock time.

---

## 9. Definition of done

- Feature-complete against `docs/SDK_DOCUMENTATION.md`'s dev-v3 feature list, with documented, deliberate exceptions only for the platform gaps identified in this plan (mock-location detection, boot-vs-relaunch semantics).
- Offline queue persists across a forced process kill and flushes correctly on next wake.
- MQTT payload byte-for-byte identical in shape to the Kotlin implementation's (fixed) payload, verified by the Phase 6 contract test.
- No hardcoded broker credentials in the shipped framework.
- Background tracking verified **on physical devices**, not simulator-only, across: active movement, stationary/idle for several hours, Low Power Mode enabled, Background App Refresh disabled, and force-kill → relaunch-via-significant-location-change.
- `isBackgroundTrackingDegraded`-style status exposed and correctly reflects at least Low Power Mode and Background App Refresh state.
- CI green, gating merges.
