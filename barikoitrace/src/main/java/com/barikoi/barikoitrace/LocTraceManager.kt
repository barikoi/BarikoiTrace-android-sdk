package com.barikoi.barikoitrace

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.location.Location
import com.barikoi.barikoitrace.util.TraceLogger
import com.barikoi.barikoitrace.api.ApiRoutes
import com.barikoi.barikoitrace.api.TraceApiClient
import com.barikoi.barikoitrace.location.LocationEngine
import com.barikoi.barikoitrace.model.TraceError
import com.barikoi.barikoitrace.model.TraceUser
import com.barikoi.barikoitrace.model.TraceStatus
import com.barikoi.barikoitrace.service.TraceConnectionState
import com.barikoi.barikoitrace.service.LocTraceForegroundService
import com.barikoi.barikoitrace.storage.OfflineLocationDb
import com.barikoi.barikoitrace.storage.OfflineLocationEntity
import com.barikoi.barikoitrace.storage.TraceDataStore
import com.barikoi.barikoitrace.util.DateTimeUtils
import com.barikoi.barikoitrace.util.NetworkChecker
import com.barikoi.barikoitrace.util.SystemSettingsManager
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.net.URI
import java.util.UUID

class LocTraceManager private constructor(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val dataStore = TraceDataStore(context)
    private val logger = TraceLogger(context, "LocTrace")
    private val apiClient = TraceApiClient.getInstance(context)
    private val locationEngine = LocationEngine(context)
    private val offlineDb = OfflineLocationDb.getInstance(context)
    private val initializationLock = Any()
    private val endpointMutex = Mutex()
    @Volatile
    private var initialization: Deferred<Unit>? = null

    // Test seam for the company-settings fetch
    internal var settingsFetcher: suspend (String) -> TraceMode = { apiClient.getCompanySettings(it) }

    // Test seam for user authentication
    internal var authenticator: suspend (String?, String?, String) -> TraceUser =
        { name, email, phone -> apiClient.authenticate(name, email, phone) }

    // Test seam for the foreground-service running check
    internal var serviceRunningChecker: () -> Boolean = { isServiceRunning() }

    private val _locationBroadcast = MutableSharedFlow<Location>(replay = 0)
    val locationBroadcast: SharedFlow<Location> = _locationBroadcast

    companion object {
        @Volatile
        private var INSTANCE: LocTraceManager? = null

        fun getInstance(context: Context): LocTraceManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LocTraceManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    // --- Init ---

    fun initialize(apiKey: String) {
        synchronized(initializationLock) {
            val previous = initialization
            val pending = scope.async(start = CoroutineStart.LAZY) {
                // Serialize repeated initialization; a new attempt can recover from a failed one.
                previous?.join()
                initializeInternal(apiKey)
            }
            initialization = pending
            pending.start()
        }
    }

    internal suspend fun awaitInitialization() {
        initialization?.await()
    }

    suspend fun getTrackingStatus(): TraceStatus = withContext(Dispatchers.IO) {
        val running = serviceRunningChecker()
        TraceStatus(
            serviceRunning = running,
            trackingRequested = dataStore.isSdkTracking(),
            mqttConnected = running && TraceConnectionState.connected,
            pendingLocationCount = offlineDb.locationDao().getCount(),
            tripId = dataStore.getLocalTripId(),
            mode = dataStore.getTraceMode(), source = dataStore.getTraceModeSource(),
            offlineTracking = dataStore.isOfflineTracking(),
            broadcasting = dataStore.isBroadcasting(), logging = dataStore.isLogging(),
            baseUrl = dataStore.getBaseUrl() ?: ApiRoutes.BASE_URL,
            mqttUrl = dataStore.getMqttUrl() ?: ApiRoutes.MQTT_URL
        )
    }

    /** Validates both endpoints before mutation and returns after configuration is saved. */
    suspend fun configureEndpoints(baseUrl: String, mqttUrl: String) {
        val base = baseUrl.trim().toHttpUrlOrNull()
        require(base != null && base.username.isEmpty() && base.password.isEmpty() &&
            base.query == null && base.fragment == null) { "Invalid REST base URL" }
        val mqtt = runCatching { URI(mqttUrl.trim()) }.getOrNull()
        require(mqtt != null && mqtt.scheme in setOf("tcp", "ssl", "ws", "wss") &&
            !mqtt.host.isNullOrBlank() && mqtt.userInfo == null && mqtt.query == null &&
            mqtt.fragment == null && (mqtt.port == -1 || mqtt.port in 1..65535) &&
            (mqtt.scheme in setOf("ws", "wss") || mqtt.path.isNullOrEmpty())) { "Invalid MQTT URL" }
        awaitInitialization()
        withContext(Dispatchers.IO) {
            endpointMutex.withLock {
                check(!serviceRunningChecker() && !dataStore.isSdkTracking()) { "Stop tracking before changing endpoints" }
                val normalizedBase = base.toString().trimEnd('/') + "/"
                val baseChanged = normalizedBase != (dataStore.getBaseUrl() ?: ApiRoutes.BASE_URL)
                if (baseChanged) {
                    check(offlineDb.locationDao().getCount() == 0) { "Upload or clear pending locations before changing the REST endpoint" }
                    apiClient.setBaseUrl(normalizedBase)
                    dataStore.setBaseUrl(normalizedBase)
                    dataStore.clearUser()
                    dataStore.stopSdkTracking()
                    dataStore.clearLocalTrip()
                }
                dataStore.setMqttUrl(mqtt.toString())
            }
        }
    }

    private suspend fun initializeInternal(apiKey: String) {
        dataStore.setApiKey(apiKey)
        apiClient.setApiKey(apiKey)

        if (dataStore.getDeviceToken() == null) {
            dataStore.setDeviceToken(UUID.randomUUID().toString())
        }

        if (!NetworkChecker.isNetworkAvailable(context)) {
            logger.w(TraceError.networkError().message)
        }

        // Restart tracking if it was active
        if (dataStore.isSdkTracking()) {
            val traceMode = dataStore.getTraceMode()
            if (traceMode != null && !isServiceRunning()) {
                val hasActiveTrip = dataStore.getLocalTripId() != null
                startTracking(traceMode, hasActiveTrip, dataStore.getTraceModeSource())
            }
        }
    }

    // --- User ---

    suspend fun setOrCreateUser(name: String?, email: String?, phone: String): TraceUser {
        if (phone.isBlank()) throw Exception(TraceError.noDataError().message)
        awaitInitialization()
        if (dataStore.getApiKey().isNullOrBlank()) throw Exception(TraceError.noKeyError().message)
        if (!NetworkChecker.isNetworkAvailable(context)) throw Exception(TraceError.networkError().message)

        // Check cached user
        val cached = dataStore.getUser()
        val switchingUser = cached != null && phone != cached.phone
        val wasTracking = serviceRunningChecker()
        val user = if (cached != null && phone == cached.phone &&
            (System.currentTimeMillis() - cached.updatedAt) < 24 * 60 * 60 * 1000
        ) {
            cached
        } else {
            val freshUser = authenticator(name, email, phone)
            apiClient.setUserId(freshUser.userId)
            freshUser
        }

        if (switchingUser) {
            // Keep the outgoing user's data until replacement authentication succeeds.
            // Then discard it before restarting the service for the new session.
            offlineDb.locationDao().deleteAll()
        }

        if (switchingUser && wasTracking) {
            // Session handoff: stop+start the service so onDestroy completes the
            // outgoing user's trip and tears down their MQTT, and onStartCommand
            // rebuilds everything from the newly stored user
            refreshTracking()
        }

        // Remote settings only apply when the app opted into the REMOTE mode source
        if (dataStore.getTraceModeSource() == TraceModeSource.REMOTE) {
            fetchAndApplyRemoteSettings()
        }

        return user
    }

    fun logout() {
        // Wipe user-scoped state before the service tears down so no trailing
        // publishes or leftover data survive (same pattern as startTracking)
        runBlocking {
            offlineDb.locationDao().deleteAll()
            dataStore.stopSdkTracking()
            dataStore.clearLocalTrip()
            dataStore.clearUser()
        }
        stopLocationService()
    }

    suspend fun updateUserName(name: String): TraceUser {
        if (dataStore.getUser() == null) throw Exception(TraceError.noUserError().message)
        if (name.isBlank()) throw Exception(TraceError.noDataError().message)

        dataStore.updateUserName(name.trim())
        if (isServiceRunning()) sendUpdateUserNameIntent()
        return dataStore.getUser()!!
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

    fun startTracking(
        traceMode: TraceMode,
        withTrip: Boolean = false,
        source: TraceModeSource = TraceModeSource.LOCAL
    ) {
        val userId = dataStore.getUserId()
        if (userId.isNullOrBlank()) {
            logger.w(TraceError.noUserError().message)
            return
        }
        if (!SystemSettingsManager.checkPermissions(context) ||
            !SystemSettingsManager.checkLocationSettings(context)
        ) {
            logger.w(TraceError.locationPermissionError().message)
            return
        }

        val keepStoredRemote = source == TraceModeSource.REMOTE && dataStore.isTraceModeFromRemote()

        // Persist synchronously so the service reads the freshly decided mode, not a stale one
        runBlocking {
            dataStore.setSdkTracking(true)
            dataStore.setTraceModeSource(source)
            if (!keepStoredRemote) {
                dataStore.setTraceMode(traceMode)
            }
            if (withTrip) {
                if (dataStore.getLocalTripId() == null) {
                    dataStore.setLocalTripId(UUID.randomUUID().toString())
                }
            } else {
                dataStore.clearLocalTrip()
            }
        }

        startLocationService()

        if (source == TraceModeSource.REMOTE) {
            scope.launch { fetchAndApplyRemoteSettings() }
        }
    }

    fun stopTracking() {
        scope.launch {
            dataStore.stopSdkTracking()
        }
        stopLocationService()
    }

    fun isLocationTracking(): Boolean = isServiceRunning()

    fun setTraceMode(mode: TraceMode) {
        scope.launch {
            dataStore.setTraceMode(mode)
            if (serviceRunningChecker()) sendTraceModeChangedIntent()
        }
    }

    fun setOfflineTracking(enabled: Boolean) {
        scope.launch { dataStore.setOfflineTracking(enabled) }
    }

    fun setLoggingEnabled(enabled: Boolean) {
        dataStore.updateLoggingCache(enabled)
        scope.launch { dataStore.persistLogging() }
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
        if (!dataStore.isOfflineTracking()) return location
        val tripId = dataStore.getLocalTripId()
        val json = JsonObject().apply {
            addProperty("latitude", location.latitude)
            addProperty("longitude", location.longitude)
            addProperty("bearing", location.bearing)
            addProperty("altitude", location.altitude)
            addProperty("gpx_time", DateTimeUtils.getDateTimeLocal(location.time))
            addProperty("speed", location.speed)
            addProperty("accuracy", location.accuracy)
            dataStore.getUserId()?.let { addProperty("user_id", it) }
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
        logger.i("Offline sync is automatic via MQTT. Restarting service if tracking is active.")
        refreshTracking()
    }

    suspend fun getSettingsFromRemote(): TraceMode {
        awaitInitialization()
        val user = dataStore.getUser()
        if (user == null || user.phone.isNullOrBlank()) throw Exception(TraceError.noUserError().message)

        val mode = settingsFetcher(user.phone)
        if (dataStore.getTraceModeSource() == TraceModeSource.REMOTE) {
            dataStore.setTraceModeFromRemote(mode)
            if (isServiceRunning()) sendTraceModeChangedIntent()
        }
        return mode
    }

    internal suspend fun fetchAndApplyRemoteSettings() {
        try {
            awaitInitialization()
            val user = dataStore.getUser() ?: return
            if (user.phone.isNullOrBlank()) return
            val mode = settingsFetcher(user.phone)
            dataStore.setTraceModeFromRemote(mode)
            if (isServiceRunning()) sendTraceModeChangedIntent()
            logger.i(
                "Remote trace settings applied: interval=${mode.updateInterval}s distance=${mode.distanceFilter}m accuracyFilter=${mode.accuracyFilter}m"
            )
        } catch (e: Exception) {
            logger.w("Remote settings fetch failed, keeping current mode", e)
        }
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
            logger.e("Cannot start foreground service from background", e)
        } catch (e: Exception) {
            logger.e("Error starting location service", e)
        }
    }

    private fun sendUpdateUserNameIntent() {
        try {
            val intent = Intent(context, LocTraceForegroundService::class.java).apply {
                action = LocTraceForegroundService.ACTION_UPDATE_USER_NAME
            }
            context.startService(intent)
        } catch (e: Exception) {
            logger.e("Failed to deliver user name update to service", e)
        }
    }

    private fun sendTraceModeChangedIntent() {
        try {
            val intent = Intent(context, LocTraceForegroundService::class.java).apply {
                action = LocTraceForegroundService.ACTION_TRACE_MODE_CHANGED
            }
            context.startService(intent)
        } catch (e: Exception) {
            logger.e("Failed to deliver trace mode change to service", e)
        }
    }

    private fun stopLocationService() {
        context.stopService(Intent(context, LocTraceForegroundService::class.java))
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
