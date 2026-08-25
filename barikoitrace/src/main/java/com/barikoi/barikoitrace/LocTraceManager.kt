package com.barikoi.barikoitrace

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.location.Location
import android.util.Log
import com.barikoi.barikoitrace.api.ApiRoutes
import com.barikoi.barikoitrace.api.TraceApiClient
import com.barikoi.barikoitrace.location.LocationEngine
import com.barikoi.barikoitrace.model.TraceError
import com.barikoi.barikoitrace.model.TraceUser
import com.barikoi.barikoitrace.service.LocTraceForegroundService
import com.barikoi.barikoitrace.storage.OfflineLocationDb
import com.barikoi.barikoitrace.storage.OfflineLocationEntity
import com.barikoi.barikoitrace.storage.TraceDataStore
import com.barikoi.barikoitrace.util.DateTimeUtils
import com.barikoi.barikoitrace.util.NetworkChecker
import com.barikoi.barikoitrace.util.SystemSettingsManager
import com.google.gson.JsonObject
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.TimeUnit

class LocTraceManager private constructor(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val dataStore = TraceDataStore(context)
    private val apiClient = TraceApiClient.getInstance(context)
    private val locationEngine = LocationEngine(context)
    private val offlineDb = OfflineLocationDb.getInstance(context)

    private val _locationBroadcast = MutableSharedFlow<Location>(replay = 0)
    val locationBroadcast: SharedFlow<Location> = _locationBroadcast

    companion object {
        private const val OFFLINE_SYNC_WORK_NAME = "com.barikoi.barikoitrace.offline_sync"

        @Volatile
        private var INSTANCE: LocTraceManager? = null

        fun getInstance(context: Context): LocTraceManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LocTraceManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    // --- Init ---

    fun initialize(apiKey: String, mqttUsername: String, mqttPassword: String) {
        scope.launch {
            dataStore.setApiKey(apiKey)
            apiClient.setApiKey(apiKey)
            dataStore.setMqttUsername(mqttUsername)
            dataStore.setMqttPassword(mqttPassword)
            dataStore.setLogging(true)

            if (dataStore.getDeviceToken() == null) {
                dataStore.setDeviceToken(UUID.randomUUID().toString())
            }

            if (!NetworkChecker.isNetworkAvailable(context)) {
                Log.w("LocTrace", TraceError.networkError().message)
            }

            // Restart tracking if it was active
            if (dataStore.isSdkTracking()) {
                val traceMode = dataStore.getTraceMode()
                if (traceMode != null && !isServiceRunning()) {
                    val hasActiveTrip = dataStore.getLocalTripId() != null
                    startTracking(traceMode, hasActiveTrip)
                }
            }
        }
    }

    // --- User ---

    suspend fun setOrCreateUser(name: String?, email: String?, phone: String): TraceUser {
        if (phone.isBlank()) throw Exception(TraceError.noDataError().message)
        if (dataStore.getApiKey().isNullOrBlank()) throw Exception(TraceError.noKeyError().message)
        if (!NetworkChecker.isNetworkAvailable(context)) throw Exception(TraceError.networkError().message)

        // Check cached user
        val cached = dataStore.getUser()
        val user = if (cached != null && phone == cached.phone &&
            (System.currentTimeMillis() - cached.updatedAt) < 24 * 60 * 60 * 1000
        ) {
            cached
        } else {
            val freshUser = apiClient.authenticate(name, email, phone)
            apiClient.setUserId(freshUser.userId)
            freshUser
        }

        // Best-effort remote settings refresh
        try {
            if (!user.phone.isNullOrBlank()) {
                val mode = apiClient.getCompanySettings(user.phone)
                dataStore.setTraceModeWithTiming(mode)
            }
        } catch (e: Exception) {
            Log.w("LocTrace", "Failed to fetch remote settings: ${e.message}")
        }

        return user
    }

    fun getUser(): TraceUser? = dataStore.getUser()
    fun getUserId(): String? = dataStore.getUserId()

    // --- URLs ---

    fun setBaseUrl(url: String) {
        val normalizedUrl = url.trimEnd('/') + "/"
        scope.launch {
            val current = dataStore.getBaseUrl()
            if (current == null || normalizedUrl != current) {
                dataStore.setBaseUrl(normalizedUrl)
                dataStore.clearUser()
                dataStore.clearTraceModeWithTiming()
                apiClient.setBaseUrl(normalizedUrl)
                stopTracking()
            }
        }
    }

    fun setMqttUrl(url: String) {
        scope.launch {
            val current = dataStore.getMqttUrl()
            if (current == null || url != current) {
                dataStore.setMqttUrl(url)
            }
        }
    }

    fun resetUrls() {
        scope.launch {
            dataStore.resetUrls()
            apiClient.setBaseUrl(ApiRoutes.BASE_URL)
            dataStore.clearUser()
            dataStore.clearTraceModeWithTiming()
            stopTracking()
        }
    }

    // --- Tracking ---

    fun startTracking(traceMode: TraceMode, withTrip: Boolean = false) {
        val userId = dataStore.getUserId()
        if (userId.isNullOrBlank()) {
            Log.w("LocTrace", TraceError.noUserError().message)
            return
        }
        if (!SystemSettingsManager.checkPermissions(context) ||
            !SystemSettingsManager.checkLocationSettings(context)
        ) {
            Log.w("LocTrace", TraceError.locationPermissionError().message)
            return
        }

        scope.launch {
            dataStore.setSdkTracking(true)
            dataStore.setTraceMode(traceMode)
            if (withTrip) {
                if (dataStore.getLocalTripId() == null) {
                    dataStore.setLocalTripId(UUID.randomUUID().toString())
                }
            } else {
                dataStore.clearLocalTrip()
            }
        }

        startLocationService()
        scheduleOfflineSyncWork()
    }

    fun stopTracking() {
        scope.launch {
            dataStore.stopSdkTracking()
        }
        stopLocationService()
        cancelOfflineSyncWork()
    }

    fun isLocationTracking(): Boolean = isServiceRunning()

    fun setTraceMode(mode: TraceMode) {
        scope.launch { dataStore.setTraceMode(mode) }
    }

    fun setOfflineTracking(enabled: Boolean) {
        scope.launch { dataStore.setOfflineTracking(enabled) }
    }

    fun setLoggingEnabled(enabled: Boolean) {
        scope.launch { dataStore.setLogging(enabled) }
    }

    fun setBroadcastingEnabled(enabled: Boolean) {
        scope.launch { dataStore.setBroadcasting(enabled) }
    }

    // --- Trips ---

    fun isOnTrip(): Boolean = dataStore.getLocalTripId() != null

    fun getTripId(): String? = dataStore.getLocalTripId()

    fun getCurrentTrip(): String? = dataStore.getLocalTripId()

    // --- Location ---

    suspend fun updateCurrentLocation(): Location {
        val location = locationEngine.getCurrentLocation()
        val tripId = dataStore.getLocalTripId()
        val json = JsonObject().apply {
            addProperty("latitude", location.latitude)
            addProperty("longitude", location.longitude)
            addProperty("bearing", location.bearing)
            addProperty("altitude", location.altitude)
            addProperty("gpx_time", DateTimeUtils.getDateTimeLocal(location.time))
            addProperty("speed", location.speed)
            addProperty("accuracy", location.accuracy)
            tripId?.let {
                addProperty("trip_id", it)
                addProperty("trip_status", "active")
            }
        }
        offlineDb.locationDao().insert(OfflineLocationEntity(json = json.toString()))
        return location
    }

    fun uploadOfflineData() {
        // Offline sync is handled automatically by the foreground service via MQTT.
        // Restarting the service triggers MQTT reconnect and flush.
        Log.i("LocTrace", "Offline sync is automatic via MQTT. Restarting service if tracking is active.")
        refreshTracking()
    }

    suspend fun getSettingsFromRemote(): TraceMode {
        val user = dataStore.getUser()
        if (user == null || user.phone.isNullOrBlank()) throw Exception(TraceError.noUserError().message)

        val mode = apiClient.getCompanySettings(user.phone)
        dataStore.setTraceModeWithTiming(mode)
        return mode
    }

    // --- Broadcast ---

    fun broadcastLocation(location: Location) {
        scope.launch {
            _locationBroadcast.emit(location)
        }
    }

    // --- Service Management ---

    private fun startLocationService() {
        try {
            val intent = Intent(context, LocTraceForegroundService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (e: IllegalStateException) {
            Log.e("LocTrace", "Cannot start foreground service from background", e)
        } catch (e: Exception) {
            Log.e("LocTrace", "Error starting location service", e)
        }
    }

    private fun stopLocationService() {
        context.stopService(Intent(context, LocTraceForegroundService::class.java))
    }

    // --- Offline sync fallback (WorkManager) ---
    //
    // LocTraceDataService was fully implemented (one-shot location fetch,
    // insert into the offline queue) but never scheduled anywhere in this
    // codebase — dead code, flagged in the iOS work plan's defect
    // carry-forward checklist. Wired up here as a periodic safety-net sync,
    // independent of the foreground service's own location-driven publish/
    // offline-write path: if the foreground service is killed or the OS
    // withholds location callbacks for a stretch, this still gets a fix
    // into the offline queue periodically for the next successful MQTT
    // flush to pick up.
    //
    // Deliberately NOT wired to TraceMode.pingSyncInterval (30-120s in the
    // presets): WorkManager enforces a hard 15-minute floor on periodic
    // work (PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS) regardless of
    // what's requested, so honoring pingSyncInterval literally isn't
    // possible here — same kind of OS-imposed coarseness as iOS's
    // BGProcessingTask, not a bug to work around.
    private fun scheduleOfflineSyncWork() {
        val request = PeriodicWorkRequestBuilder<com.barikoi.barikoitrace.service.LocTraceDataService>(
            15, TimeUnit.MINUTES
        ).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            OFFLINE_SYNC_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request
        )
    }

    private fun cancelOfflineSyncWork() {
        WorkManager.getInstance(context).cancelUniqueWork(OFFLINE_SYNC_WORK_NAME)
    }

    private fun isServiceRunning(): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (LocTraceForegroundService::class.java.name == service.service.className && service.started) {
                return true
            }
        }
        return false
    }

    // --- Permissions ---

    fun checkLocationSettings(): Boolean = SystemSettingsManager.checkLocationSettings(context)
    fun checkPermissions(): Boolean = SystemSettingsManager.checkPermissions(context)
    fun checkIgnoringBatteryOptimization(): Boolean = SystemSettingsManager.isIgnoringBatteryOptimization(context)

    fun requestBatteryOptimization(context: Context) {
        SystemSettingsManager.requestBatteryOptimizationSetting(context)
    }

    fun refreshTracking() {
        val isTracking = dataStore.isSdkTracking()
        val isRunning = isServiceRunning()
        if (isTracking || isRunning) {
            val traceMode = dataStore.getTraceMode()
            if (traceMode != null) {
                stopLocationService()
                startLocationService()
            }
        }
    }
}
