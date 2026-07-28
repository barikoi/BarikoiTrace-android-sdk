package com.barikoi.barikoitrace.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.barikoi.barikoitrace.TraceMode
import com.barikoi.barikoitrace.model.TraceUser
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.time.LocalTime
import java.util.concurrent.ConcurrentHashMap

val Context.traceDataStore: DataStore<Preferences> by preferencesDataStore(name = "barikoi_trace_config")

class TraceDataStore private constructor(context: Context) {

    private val store = context.traceDataStore

    // In-memory cache to avoid runBlocking reads on main thread (shared across all instances)
    private val cache = sharedCache

    private object Keys {
        val API_KEY = stringPreferencesKey("api_key")
        val BASE_URL = stringPreferencesKey("base_url")
        val MQTT_URL = stringPreferencesKey("mqtt_url")
        val DEVICE_TOKEN = stringPreferencesKey("device_token")
        val USER_ID = stringPreferencesKey("user_id")
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val USER_PHONE = stringPreferencesKey("user_phone")
        val USER_COMPANY = stringPreferencesKey("user_company")
        val USER_GROUP = stringPreferencesKey("user_group")
        val USER_UPDATED_AT = longPreferencesKey("user_updated_at")
        val SDK_TRACKING = booleanPreferencesKey("sdk_tracking")
        val ON_TRIP = booleanPreferencesKey("on_trip")
        val LOCAL_TRIP_ID = stringPreferencesKey("local_trip_id")
        val OFFLINE_TRACKING = booleanPreferencesKey("offlineTracking")
        val DATA_SYNCING = booleanPreferencesKey("offline_syncing")
        val LOGGING = booleanPreferencesKey("logger")
        val BROADCASTING = booleanPreferencesKey("broadcasting")
        val DESIRED_ACCURACY = stringPreferencesKey("desiredAccuracy")
        val UPDATE_INTERVAL = intPreferencesKey("updateInterval")
        val DISTANCE_FILTER = intPreferencesKey("distanceFilter")
        val STOP_DURATION = intPreferencesKey("stopDuration")
        val ACCURACY_FILTER = intPreferencesKey("accuracyFilter")
        val PING_SYNC_INTERVAL = intPreferencesKey("pingSyncInterval")
        val TRACKING_TYPE = intPreferencesKey("type")
        val START_TIME = stringPreferencesKey("startTime")
        val END_TIME = stringPreferencesKey("endTime")
        val DEBUG = booleanPreferencesKey("debug")
    }

    init {
        // Pre-warm cache from DataStore on first instantiation
        runBlocking {
            store.data.first().let { prefs ->
                prefs[Keys.API_KEY]?.let { cache[Keys.API_KEY.name] = it }
                prefs[Keys.BASE_URL]?.let { cache[Keys.BASE_URL.name] = it }
                prefs[Keys.MQTT_URL]?.let { cache[Keys.MQTT_URL.name] = it }
                prefs[Keys.DEVICE_TOKEN]?.let { cache[Keys.DEVICE_TOKEN.name] = it }
                prefs[Keys.USER_ID]?.let { cache[Keys.USER_ID.name] = it }
                prefs[Keys.USER_NAME]?.let { cache[Keys.USER_NAME.name] = it }
                prefs[Keys.USER_EMAIL]?.let { cache[Keys.USER_EMAIL.name] = it }
                prefs[Keys.USER_PHONE]?.let { cache[Keys.USER_PHONE.name] = it }
                prefs[Keys.USER_COMPANY]?.let { cache[Keys.USER_COMPANY.name] = it }
                prefs[Keys.USER_GROUP]?.let { cache[Keys.USER_GROUP.name] = it }
                cache[Keys.USER_UPDATED_AT.name] = prefs[Keys.USER_UPDATED_AT] ?: 0L
                cache[Keys.SDK_TRACKING.name] = prefs[Keys.SDK_TRACKING] ?: false
                cache[Keys.ON_TRIP.name] = prefs[Keys.ON_TRIP] ?: false
                prefs[Keys.LOCAL_TRIP_ID]?.let { cache[Keys.LOCAL_TRIP_ID.name] = it }
                cache[Keys.OFFLINE_TRACKING.name] = prefs[Keys.OFFLINE_TRACKING] ?: false
                cache[Keys.DATA_SYNCING.name] = prefs[Keys.DATA_SYNCING] ?: false
                cache[Keys.LOGGING.name] = prefs[Keys.LOGGING] ?: false
                cache[Keys.BROADCASTING.name] = prefs[Keys.BROADCASTING] ?: false
                prefs[Keys.DESIRED_ACCURACY]?.let { cache[Keys.DESIRED_ACCURACY.name] = it }
                cache[Keys.UPDATE_INTERVAL.name] = prefs[Keys.UPDATE_INTERVAL] ?: 0
                cache[Keys.DISTANCE_FILTER.name] = prefs[Keys.DISTANCE_FILTER] ?: 0
                cache[Keys.STOP_DURATION.name] = prefs[Keys.STOP_DURATION] ?: 0
                cache[Keys.ACCURACY_FILTER.name] = prefs[Keys.ACCURACY_FILTER] ?: 0
                cache[Keys.PING_SYNC_INTERVAL.name] = prefs[Keys.PING_SYNC_INTERVAL] ?: 0
                cache[Keys.TRACKING_TYPE.name] = prefs[Keys.TRACKING_TYPE] ?: 0
                prefs[Keys.START_TIME]?.let { cache[Keys.START_TIME.name] = it }
                prefs[Keys.END_TIME]?.let { cache[Keys.END_TIME.name] = it }
                cache[Keys.DEBUG.name] = prefs[Keys.DEBUG] ?: false
            }
        }
    }

    private fun getString(key: Preferences.Key<String>): String? = cache[key.name] as String?

    private fun getInt(key: Preferences.Key<Int>, default: Int = 0): Int = (cache[key.name] as? Int) ?: default

    private fun getLong(key: Preferences.Key<Long>, default: Long = 0L): Long = (cache[key.name] as? Long) ?: default

    private fun getBoolean(key: Preferences.Key<Boolean>, default: Boolean = false): Boolean = (cache[key.name] as? Boolean) ?: default

    private suspend fun <T> putAndCache(key: Preferences.Key<T>, value: T) {
        cache[key.name] = value
        store.edit { it[key] = value }
    }

    private suspend fun removeAndCache(vararg keys: Preferences.Key<*>) {
        for (key in keys) cache.remove(key.name)
        store.edit { prefs -> keys.forEach { prefs.remove(it) } }
    }

    // --- API Key ---
    suspend fun setApiKey(key: String) = putAndCache(Keys.API_KEY, key)
    fun getApiKey(): String? = getString(Keys.API_KEY)

    // --- URLs ---
    suspend fun setBaseUrl(url: String) = putAndCache(Keys.BASE_URL, url)
    fun getBaseUrl(): String? = getString(Keys.BASE_URL)
    suspend fun setMqttUrl(url: String) = putAndCache(Keys.MQTT_URL, url)
    fun getMqttUrl(): String? = getString(Keys.MQTT_URL)
    suspend fun resetUrls() {
        cache.remove(Keys.BASE_URL.name)
        cache.remove(Keys.MQTT_URL.name)
        store.edit {
            it.remove(Keys.BASE_URL)
            it.remove(Keys.MQTT_URL)
        }
    }

    // --- Device Token ---
    suspend fun setDeviceToken(token: String) = putAndCache(Keys.DEVICE_TOKEN, token)
    fun getDeviceToken(): String? = getString(Keys.DEVICE_TOKEN)

    // --- User ---
    suspend fun setUser(user: TraceUser) {
        cache[Keys.USER_ID.name] = user.userId
        user.name?.let { cache[Keys.USER_NAME.name] = it }
        user.email?.let { cache[Keys.USER_EMAIL.name] = it }
        user.phone?.let { cache[Keys.USER_PHONE.name] = it }
        user.companyId?.let { cache[Keys.USER_COMPANY.name] = it }
        user.group?.let { cache[Keys.USER_GROUP.name] = it }
        cache[Keys.USER_UPDATED_AT.name] = user.updatedAt
        store.edit {
            it[Keys.USER_ID] = user.userId
            user.name?.let { n -> it[Keys.USER_NAME] = n }
            user.email?.let { e -> it[Keys.USER_EMAIL] = e }
            user.phone?.let { p -> it[Keys.USER_PHONE] = p }
            user.companyId?.let { c -> it[Keys.USER_COMPANY] = c }
            user.group?.let { g -> it[Keys.USER_GROUP] = g }
            it[Keys.USER_UPDATED_AT] = user.updatedAt
        }
    }

    fun getUser(): TraceUser? {
        val userId = getString(Keys.USER_ID) ?: return null
        return TraceUser(
            userId = userId,
            name = getString(Keys.USER_NAME),
            email = getString(Keys.USER_EMAIL),
            phone = getString(Keys.USER_PHONE),
            companyId = getString(Keys.USER_COMPANY),
            group = getString(Keys.USER_GROUP),
            updatedAt = getLong(Keys.USER_UPDATED_AT)
        )
    }

    fun getUserId(): String? = getString(Keys.USER_ID)

    suspend fun clearUser() = removeAndCache(
        Keys.USER_ID, Keys.USER_NAME, Keys.USER_EMAIL,
        Keys.USER_PHONE, Keys.USER_COMPANY, Keys.USER_GROUP, Keys.USER_UPDATED_AT
    )

    // --- Tracking State ---
    suspend fun setSdkTracking(on: Boolean) = putAndCache(Keys.SDK_TRACKING, on)
    fun isSdkTracking(): Boolean = getBoolean(Keys.SDK_TRACKING)

    suspend fun setOnTrip(on: Boolean) = putAndCache(Keys.ON_TRIP, on)
    fun isOnTrip(): Boolean = getBoolean(Keys.ON_TRIP)

    suspend fun setLocalTripId(tripId: String?) {
        if (tripId != null) putAndCache(Keys.LOCAL_TRIP_ID, tripId)
        else removeAndCache(Keys.LOCAL_TRIP_ID)
    }
    fun getLocalTripId(): String? = getString(Keys.LOCAL_TRIP_ID)
    suspend fun clearLocalTrip() = removeAndCache(Keys.LOCAL_TRIP_ID)

    suspend fun setOfflineTracking(enabled: Boolean) = putAndCache(Keys.OFFLINE_TRACKING, enabled)
    fun isOfflineTracking(): Boolean = getBoolean(Keys.OFFLINE_TRACKING)

    suspend fun setDataSyncing(syncing: Boolean) = putAndCache(Keys.DATA_SYNCING, syncing)
    fun isDataSyncing(): Boolean = getBoolean(Keys.DATA_SYNCING)

    suspend fun setLogging(enabled: Boolean) = putAndCache(Keys.LOGGING, enabled)
    fun isLogging(): Boolean = getBoolean(Keys.LOGGING)

    suspend fun setBroadcasting(enabled: Boolean) = putAndCache(Keys.BROADCASTING, enabled)
    fun isBroadcasting(): Boolean = getBoolean(Keys.BROADCASTING)

    // --- TraceMode ---
    suspend fun setTraceMode(mode: TraceMode) {
        cache[Keys.DESIRED_ACCURACY.name] = mode.desiredAccuracy.name
        cache[Keys.UPDATE_INTERVAL.name] = mode.updateInterval
        cache[Keys.DISTANCE_FILTER.name] = mode.distanceFilter
        cache[Keys.STOP_DURATION.name] = mode.stopDuration
        cache[Keys.ACCURACY_FILTER.name] = mode.accuracyFilter
        cache[Keys.PING_SYNC_INTERVAL.name] = mode.pingSyncInterval
        cache[Keys.TRACKING_TYPE.name] = mode.trackingMode.option
        cache[Keys.DEBUG.name] = mode.debug
        store.edit {
            it[Keys.DESIRED_ACCURACY] = mode.desiredAccuracy.name
            it[Keys.UPDATE_INTERVAL] = mode.updateInterval
            it[Keys.DISTANCE_FILTER] = mode.distanceFilter
            it[Keys.STOP_DURATION] = mode.stopDuration
            it[Keys.ACCURACY_FILTER] = mode.accuracyFilter
            it[Keys.PING_SYNC_INTERVAL] = mode.pingSyncInterval
            it[Keys.TRACKING_TYPE] = mode.trackingMode.option
            it[Keys.DEBUG] = mode.debug
        }
    }

    suspend fun setTraceModeWithTiming(mode: TraceMode) {
        setTraceMode(mode)
        cache[Keys.START_TIME.name] = mode.startTime.toString()
        cache[Keys.END_TIME.name] = mode.endTime.toString()
        store.edit {
            it[Keys.START_TIME] = mode.startTime.toString()
            it[Keys.END_TIME] = mode.endTime.toString()
        }
    }

    fun getTraceMode(): TraceMode {
        val updateInterval = getInt(Keys.UPDATE_INTERVAL)
        val distanceFilter = getInt(Keys.DISTANCE_FILTER)

        val builder = if (updateInterval != 0 || distanceFilter != 0) {
            TraceMode.Builder()
                .setAccuracyFilter(getInt(Keys.ACCURACY_FILTER, 200))
                .setDistanceFilter(distanceFilter)
                .setUpdateInterval(updateInterval)
                .setOfflineSync(getBoolean(Keys.OFFLINE_TRACKING, true))
                .setPingSyncInterval(getInt(Keys.PING_SYNC_INTERVAL))
                .setDesiredAccuracy(TraceMode.DesiredAccuracy.fromString(getString(Keys.DESIRED_ACCURACY)))
        } else {
            TraceMode.Builder()
                .setAccuracyFilter(200)
                .setDistanceFilter(0)
                .setUpdateInterval(5)
                .setOfflineSync(true)
                .setDesiredAccuracy(TraceMode.DesiredAccuracy.HIGH)
        }

        val endTimeStr = getString(Keys.END_TIME)
        if (endTimeStr != null) {
            try {
                builder.setEndTime(LocalTime.parse(endTimeStr))
                val startTimeStr = getString(Keys.START_TIME)
                if (startTimeStr != null) builder.setStartTime(LocalTime.parse(startTimeStr))
            } catch (_: Exception) {}
        }

        return builder.build()
    }

    fun getTrackingType(): Int = getInt(Keys.TRACKING_TYPE)
    fun getUpdateInterval(): Int = getInt(Keys.UPDATE_INTERVAL)

    suspend fun clearTraceMode() = removeAndCache(
        Keys.DESIRED_ACCURACY, Keys.UPDATE_INTERVAL, Keys.DISTANCE_FILTER,
        Keys.STOP_DURATION, Keys.ACCURACY_FILTER, Keys.PING_SYNC_INTERVAL,
        Keys.TRACKING_TYPE, Keys.DEBUG
    )

    suspend fun clearTraceModeWithTiming() {
        clearTraceMode()
        removeAndCache(Keys.START_TIME, Keys.END_TIME)
    }

    suspend fun stopSdkTracking() {
        putAndCache(Keys.SDK_TRACKING, false)
        clearTraceMode()
    }

    companion object {
        private val sharedCache = ConcurrentHashMap<String, Any?>()

        @Volatile
        private var INSTANCE: TraceDataStore? = null

        operator fun invoke(context: Context): TraceDataStore {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TraceDataStore(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
