package com.barikoi.barikoitrace

import android.app.Activity
import android.content.Context
import android.location.Location
import com.barikoi.barikoitrace.model.TraceError
import com.barikoi.barikoitrace.model.TraceUser
import com.barikoi.barikoitrace.receiver.LocationReceiver
import com.barikoi.barikoitrace.util.SystemSettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

object BarikoiTrace {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var manager: LocTraceManager? = null

    @Volatile
    private var logListener: TraceLogListener? = null

    @JvmStatic
    fun setLogListener(listener: TraceLogListener?) {
        logListener = listener
    }

    internal fun notifyLog(level: String, tag: String, message: String) {
        logListener?.onLog(level, tag, message)
    }

    private fun getInstance(): LocTraceManager {
        return manager ?: throw IllegalStateException(
            "BarikoiTrace not initialized. Call initialize() first."
        )
    }

    // --- Init & Config ---

    /**
     * BREAKING CHANGE: `mqttUsername`/`mqttPassword` are now required.
     * Previously this SDK connected to the MQTT broker with a single
     * hardcoded username/password baked into `MqttManager.kt`, shared by
     * every app that depended on this library. Get real per-app/
     * per-environment broker credentials from your backend and pass them
     * here — do not hardcode them in the calling app either; that just
     * moves the same problem up one layer.
     */
    @JvmStatic
    fun initialize(context: Context, apiKey: String, mqttUsername: String, mqttPassword: String) {
        manager = LocTraceManager.getInstance(context)
        getInstance().initialize(apiKey, mqttUsername, mqttPassword)
    }

    @JvmStatic
    fun setBaseUrl(url: String) = getInstance().setBaseUrl(url)

    @JvmStatic
    fun setMqttUrl(url: String) = getInstance().setMqttUrl(url)

    @JvmStatic
    fun resetUrls() = getInstance().resetUrls()

    // --- User ---

    @JvmStatic
    suspend fun setOrCreateUser(name: String?, email: String?, phone: String): TraceUser =
        getInstance().setOrCreateUser(name, email, phone)

    @JvmStatic
    fun getUser(): TraceUser? = getInstance().getUser()

    @JvmStatic
    fun getUserId(): String? = getInstance().getUserId()

    // --- Permissions & Settings ---

    @JvmStatic
    fun isLocationPermissionsGranted(): Boolean = getInstance().checkPermissions()

    @JvmStatic
    fun isLocationSettingsOn(): Boolean = getInstance().checkLocationSettings()

    @JvmStatic
    fun requestLocationPermissions(activity: Activity) =
        SystemSettingsManager.requestLocationPermissions(activity)

    @JvmStatic
    fun requestBackgroundLocationPermission(activity: Activity) =
        SystemSettingsManager.requestAndroidBackgroundLocationPermission(activity)

    @JvmStatic
    fun requestNotificationPermission(activity: Activity) =
        SystemSettingsManager.requestNotificationPermission(activity)

    @JvmStatic
    fun requestLocationServices(activity: Activity) =
        SystemSettingsManager.requestLocationSettings(activity)

    @JvmStatic
    fun requestDisableBatteryOptimization(context: Context) =
        SystemSettingsManager.requestBatteryOptimizationSetting(context)

    @JvmStatic
    fun isBatteryOptimizationEnabled(): Boolean = getInstance().checkIgnoringBatteryOptimization()

    @JvmStatic
    fun checkAppServicePermission(context: Context) =
        SystemSettingsManager.checkAppServicePermission(context)

    @JvmStatic
    fun openAutostartSettings(context: Context) =
        SystemSettingsManager.openAutostartSettings(context)

    // --- Tracking ---

    @JvmStatic
    fun setTraceMode(mode: TraceMode) = getInstance().setTraceMode(mode)

    @JvmStatic
    fun startTracking(traceMode: TraceMode) = getInstance().startTracking(traceMode)

    @JvmStatic
    fun startTracking(traceMode: TraceMode, withTrip: Boolean) =
        getInstance().startTracking(traceMode, withTrip)

    @JvmStatic
    fun stopTracking() = getInstance().stopTracking()

    @JvmStatic
    fun isLocationTracking(): Boolean = getInstance().isLocationTracking()

    @JvmStatic
    fun setOfflineTracking(enabled: Boolean) = getInstance().setOfflineTracking(enabled)

    @JvmStatic
    fun setLoggingEnabled(enabled: Boolean) = getInstance().setLoggingEnabled(enabled)

    @JvmStatic
    fun setBroadcastingEnabled(enabled: Boolean) = getInstance().setBroadcastingEnabled(enabled)

    // --- Trips ---

    @JvmStatic
    fun isOnTrip(): Boolean = getInstance().isOnTrip()

    @JvmStatic
    fun getTripId(): String? = getInstance().getTripId()

    @JvmStatic
    fun getCurrentTrip(): String? = getInstance().getCurrentTrip()

    // --- Location ---

    @JvmStatic
    suspend fun updateCurrentLocation(): Location = getInstance().updateCurrentLocation()

    @JvmStatic
    fun uploadOfflineData() = getInstance().uploadOfflineData()

    @JvmStatic
    suspend fun getSettingsFromRemote(): TraceMode = getInstance().getSettingsFromRemote()

    // --- Broadcast ---

    /**
     * Flow of location updates broadcast by the SDK when broadcasting is enabled.
     * Collect this flow to receive location updates in real time.
     */
    @JvmStatic
    val locationUpdates: SharedFlow<Location>
        get() = getInstance().locationBroadcast

    @JvmStatic
    fun registerLocationUpdate(receiver: LocationReceiver) {
        // Consumers should collect [locationUpdates] flow instead.
        // This stub is kept for backward compatibility but does nothing.
    }

    @JvmStatic
    fun unregisterLocationUpdate(receiver: LocationReceiver) {
        // Consumers should collect [locationUpdates] flow instead.
        // This stub is kept for backward compatibility but does nothing.
    }

    // --- Convenience callback wrappers ---

    @JvmStatic
    fun setOrCreateUser(
        name: String?, email: String?, phone: String,
        callback: TraceUserCallback
    ) {
        scope.launch {
            try {
                val user = setOrCreateUser(name, email, phone)
                callback.onSuccess(user)
            } catch (e: Exception) {
                callback.onFailure(TraceError("USER_ERROR", e.message ?: "Unknown error"))
            }
        }
    }

    @JvmStatic
    fun updateCurrentLocation(callback: TraceLocationUpdateCallback) {
        scope.launch {
            try {
                val location = updateCurrentLocation()
                callback.onLocationUpdate(location)
            } catch (e: Exception) {
                callback.onFailure(TraceError("LOCATION_ERROR", e.message ?: "Unknown error"))
            }
        }
    }

    @JvmStatic
    fun getSettingsFromRemote(callback: TraceSettingsCallback) {
        scope.launch {
            try {
                val mode = getSettingsFromRemote()
                callback.onSuccess(mode)
            } catch (e: Exception) {
                callback.onFailure(TraceError("SETTINGS_ERROR", e.message ?: "Unknown error"))
            }
        }
    }

    // --- Callback Interfaces ---

    interface TraceUserCallback {
        fun onSuccess(user: TraceUser)
        fun onFailure(error: TraceError)
    }

    interface TraceLocationUpdateCallback {
        fun onLocationUpdate(location: Location)
        fun onFailure(error: TraceError)
    }

    interface TraceSettingsCallback {
        fun onSuccess(mode: TraceMode)
        fun onFailure(error: TraceError)
    }

    interface TraceLogListener {
        fun onLog(level: String, tag: String, message: String)
    }
}
