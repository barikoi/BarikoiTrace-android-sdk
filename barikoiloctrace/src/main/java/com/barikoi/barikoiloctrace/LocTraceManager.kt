package com.barikoi.barikoiloctrace

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.location.Location
import android.util.Log
import com.barikoi.barikoiloctrace.api.ApiRoutes
import com.barikoi.barikoiloctrace.api.TraceApiClient
import com.barikoi.barikoiloctrace.location.LocationEngine
import com.barikoi.barikoiloctrace.model.TraceError
import com.barikoi.barikoiloctrace.model.TraceUser
import com.barikoi.barikoiloctrace.model.Trip
import com.barikoi.barikoiloctrace.service.LocTraceForegroundService
import com.barikoi.barikoiloctrace.storage.OfflineLocationDb
import com.barikoi.barikoiloctrace.storage.OfflineLocationEntity
import com.barikoi.barikoiloctrace.storage.TraceDataStore
import com.barikoi.barikoiloctrace.util.DateTimeUtils
import com.barikoi.barikoiloctrace.util.NetworkChecker
import com.barikoi.barikoiloctrace.util.SystemSettingsManager
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import java.util.UUID

class LocTraceManager private constructor(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val dataStore = TraceDataStore(context)
    private val apiClient = TraceApiClient.getInstance(context)
    private val locationEngine = LocationEngine(context)
    private val offlineDb = OfflineLocationDb.getInstance(context)

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
        scope.launch {
            dataStore.setApiKey(apiKey)
            apiClient.setApiKey(apiKey)
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
                    startTracking(traceMode)
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
        if (cached != null && phone == cached.phone &&
            (System.currentTimeMillis() - cached.updatedAt) < 24 * 60 * 60 * 1000
        ) {
            return cached
        }

        val user = apiClient.authenticate(name, email, phone)
        apiClient.setUserId(user.userId)
        return user
    }

    fun getUser(): TraceUser? = dataStore.getUser()
    fun getUserId(): String? = dataStore.getUserId()

    // --- URLs ---

    fun setBaseUrl(url: String) {
        scope.launch {
            val current = dataStore.getBaseUrl()
            if (current == null || url != current) {
                dataStore.setBaseUrl(url)
                dataStore.clearUser()
                dataStore.clearTraceModeWithTiming()
                apiClient.setBaseUrl(url)
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

    fun startTracking(traceMode: TraceMode) {
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
        }

        startLocationService()
    }

    fun stopTracking() {
        scope.launch {
            dataStore.stopSdkTracking()
        }
        stopLocationService()
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

    suspend fun startTrip(tag: String, traceMode: TraceMode): Trip {
        val startTime = DateTimeUtils.getCurrentTimeLocal()
        dataStore.setTraceMode(traceMode)

        val trip = apiClient.startTrip(startTime, tag, traceMode)
        scope.launch {
            dataStore.setOnTrip(true)
            dataStore.setSdkTracking(true)
        }
        startLocationService()
        return trip
    }

    suspend fun endTrip(): Trip {
        if (!dataStore.isOnTrip()) throw Exception(TraceError.tripStateError().message)

        val endTime = DateTimeUtils.getCurrentTimeLocal()

        // Sync offline data first (await completion)
        if (offlineDb.locationDao().getCount() > 0 && !dataStore.isDataSyncing()) {
            try {
                uploadOfflineDataSync()
            } catch (_: Exception) {}
        }

        val trip = apiClient.endTrip(endTime)
        scope.launch {
            dataStore.setOnTrip(false)
            dataStore.stopSdkTracking()
        }
        stopLocationService()
        return trip
    }

    fun isOnTrip(): Boolean = dataStore.isOnTrip()

    suspend fun syncTripState(): Trip? {
        val userId = dataStore.getUserId()
        if (userId.isNullOrBlank()) throw Exception(TraceError.noUserError().message)
        if (!NetworkChecker.isNetworkAvailable(context)) throw Exception(TraceError.networkError().message)

        val trip = apiClient.getActiveTrip()
        if (trip != null) {
            if (!dataStore.isOnTrip()) {
                scope.launch {
                    dataStore.setOnTrip(true)
                    dataStore.setSdkTracking(true)
                }
                startLocationService()
            }
            if (!isServiceRunning()) {
                startLocationService()
            }
        } else if (dataStore.isOnTrip()) {
            scope.launch {
                dataStore.setOnTrip(false)
                dataStore.stopSdkTracking()
            }
            stopLocationService()
        }
        return trip
    }

    // --- Location ---

    suspend fun updateCurrentLocation(): Location {
        val location = locationEngine.getCurrentLocation()
        try {
            apiClient.sendLocation(
                latitude = location.latitude,
                longitude = location.longitude,
                altitude = location.altitude,
                bearing = location.bearing,
                speed = location.speed,
                accuracy = location.accuracy,
                gpxTime = DateTimeUtils.getDateTimeLocal(location.time)
            )
        } catch (e: Exception) {
            // Save offline
            val json = JsonObject().apply {
                addProperty("latitude", location.latitude)
                addProperty("longitude", location.longitude)
                addProperty("bearing", location.bearing)
                addProperty("altitude", location.altitude)
                addProperty("gpx_time", DateTimeUtils.getDateTimeLocal(location.time))
                addProperty("speed", location.speed)
                addProperty("accuracy", location.accuracy)
            }
            offlineDb.locationDao().insert(OfflineLocationEntity(json = json.toString()))
            throw e
        }
        return location
    }

    fun uploadOfflineData() {
        scope.launch { uploadOfflineDataSync() }
    }

    suspend fun uploadOfflineDataSync() {
        dataStore.setDataSyncing(true)
        try {
            val dao = offlineDb.locationDao()
            val batch = dao.getBatch()
            if (batch.isEmpty()) {
                dataStore.setDataSyncing(false)
                return
            }

            val data = JsonArray()
            for (entity in batch) {
                try {
                    val locJson = com.google.gson.JsonParser.parseString(entity.json).asJsonObject
                    locJson.addProperty("user_id", dataStore.getUserId())
                    data.add(locJson)
                } catch (_: Exception) {}
            }

            apiClient.sendBulkLocations(data)

            val deleted = dao.deleteBatch()
            if (deleted > 0) {
                // Recursively upload more
                uploadOfflineDataSync()
            } else {
                dataStore.setDataSyncing(false)
            }
        } catch (e: Exception) {
            Log.e("LocTrace", "Offline sync failed", e)
            dataStore.setDataSyncing(false)
        }
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
