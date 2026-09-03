package com.barikoi.barikoitrace.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.barikoi.barikoitrace.TraceMode
import com.barikoi.barikoitrace.api.MqttManager
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

    /**
     * Credentials and user identity live here, not in [store]. Mirrors the
     * iOS SDK, which puts exactly this set in the Keychain and leaves runtime
     * config in UserDefaults — the same sensitivity split, so the two SDKs can
     * be described identically to a wrapper.
     */
    private val secure = SecureStore(context.applicationContext)

    private object SecureKeys {
        const val API_KEY = "api_key"
        const val MQTT_USERNAME = "mqtt_username"
        const val MQTT_PASSWORD = "mqtt_password"
        const val USER_ID = "user_id"
        const val USER_NAME = "user_name"
        const val USER_EMAIL = "user_email"
        const val USER_PHONE = "user_phone"
        const val USER_COMPANY = "user_company"
        const val USER_GROUP = "user_group"
        const val USER_UPDATED_AT = "user_updated_at"
    }

    private object Keys {
        // The API key, broker credentials and user identity are NOT here —
        // see [SecureKeys]/[SecureStore]. This object is runtime config only,
        // matching the iOS SDK's UserDefaults/Keychain split.
        val BASE_URL = stringPreferencesKey("base_url")
        val MQTT_URL = stringPreferencesKey("mqtt_url")
        val MQTT_CLIENT_ID_PREFIX = stringPreferencesKey("mqtt_client_id_prefix")
        val DEVICE_TOKEN = stringPreferencesKey("device_token")
        val SDK_TRACKING = booleanPreferencesKey("sdk_tracking")
        /**
         * Never written by the SDK — `isOnTrip()` is derived from
         * [LOCAL_TRIP_ID] everywhere that matters. Kept only because
         * `setOnTrip`/`isOnTrip` are public.
         */
        val ON_TRIP = booleanPreferencesKey("on_trip")
        val LOCAL_TRIP_ID = stringPreferencesKey("local_trip_id")
        /** The host app's explicit `setOfflineTracking()` override. */
        val OFFLINE_TRACKING = booleanPreferencesKey("offlineTracking")
        /**
         * `TraceMode.offline`, on its own key. These used to share
         * [OFFLINE_TRACKING], which is why a mode's `offline` value was both
         * unwritable and able to be clobbered by the host app's toggle.
         */
        val MODE_OFFLINE_SYNC = booleanPreferencesKey("mode_offline_sync")
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
                // The API key, broker credentials and user identity are no
                // longer here — they live in `secure` (SecureStore), whose
                // reads are synchronous and need no pre-warming.
                prefs[Keys.BASE_URL]?.let { cache[Keys.BASE_URL.name] = it }
                prefs[Keys.MQTT_URL]?.let { cache[Keys.MQTT_URL.name] = it }
                prefs[Keys.MQTT_CLIENT_ID_PREFIX]?.let { cache[Keys.MQTT_CLIENT_ID_PREFIX.name] = it }
                prefs[Keys.DEVICE_TOKEN]?.let { cache[Keys.DEVICE_TOKEN.name] = it }
                cache[Keys.SDK_TRACKING.name] = prefs[Keys.SDK_TRACKING] ?: false
                prefs[Keys.LOCAL_TRIP_ID]?.let { cache[Keys.LOCAL_TRIP_ID.name] = it }
                // Left absent when unset, rather than defaulted: absence is
                // what `isOfflineTracking()` distinguishes to decide between
                // the host override, the mode's value, and its own default.
                prefs[Keys.OFFLINE_TRACKING]?.let { cache[Keys.OFFLINE_TRACKING.name] = it }
                prefs[Keys.MODE_OFFLINE_SYNC]?.let { cache[Keys.MODE_OFFLINE_SYNC.name] = it }
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
    suspend fun setApiKey(key: String) = secure.putString(SecureKeys.API_KEY, key)
    fun getApiKey(): String? = secure.getString(SecureKeys.API_KEY)

    // --- URLs ---
    suspend fun setBaseUrl(url: String) = putAndCache(Keys.BASE_URL, url)
    fun getBaseUrl(): String? = getString(Keys.BASE_URL)
    suspend fun setMqttUrl(url: String) = putAndCache(Keys.MQTT_URL, url)
    fun getMqttUrl(): String? = getString(Keys.MQTT_URL)

    /** See [MqttManager.DEFAULT_CLIENT_ID_PREFIX]. */
    suspend fun setMqttClientIdPrefix(prefix: String) = putAndCache(Keys.MQTT_CLIENT_ID_PREFIX, prefix)
    fun getMqttClientIdPrefix(): String =
        getString(Keys.MQTT_CLIENT_ID_PREFIX) ?: MqttManager.DEFAULT_CLIENT_ID_PREFIX

    // Keystore-backed now, together with API_KEY and the user fields — the
    // revisit the previous comment here asked for. Matches the iOS SDK, which
    // has always kept this set in the Keychain.
    suspend fun setMqttUsername(username: String) = secure.putString(SecureKeys.MQTT_USERNAME, username)
    fun getMqttUsername(): String? = secure.getString(SecureKeys.MQTT_USERNAME)
    suspend fun setMqttPassword(password: String) = secure.putString(SecureKeys.MQTT_PASSWORD, password)
    fun getMqttPassword(): String? = secure.getString(SecureKeys.MQTT_PASSWORD)

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
    // Identity is PII and lives in [secure], matching the iOS SDK's Keychain
    // split. `EncryptedSharedPreferences` reads are synchronous and cheap, so
    // these need no cache layer of their own.
    suspend fun setUser(user: TraceUser) {
        secure.putString(SecureKeys.USER_ID, user.userId)
        user.name?.let { secure.putString(SecureKeys.USER_NAME, it) }
        user.email?.let { secure.putString(SecureKeys.USER_EMAIL, it) }
        user.phone?.let { secure.putString(SecureKeys.USER_PHONE, it) }
        user.companyId?.let { secure.putString(SecureKeys.USER_COMPANY, it) }
        user.group?.let { secure.putString(SecureKeys.USER_GROUP, it) }
        secure.putLong(SecureKeys.USER_UPDATED_AT, user.updatedAt)
    }

    fun getUser(): TraceUser? {
        val userId = secure.getString(SecureKeys.USER_ID) ?: return null
        return TraceUser(
            userId = userId,
            name = secure.getString(SecureKeys.USER_NAME),
            email = secure.getString(SecureKeys.USER_EMAIL),
            phone = secure.getString(SecureKeys.USER_PHONE),
            companyId = secure.getString(SecureKeys.USER_COMPANY),
            group = secure.getString(SecureKeys.USER_GROUP),
            updatedAt = secure.getLong(SecureKeys.USER_UPDATED_AT)
        )
    }

    fun getUserId(): String? = secure.getString(SecureKeys.USER_ID)

    suspend fun clearUser() = secure.remove(
        SecureKeys.USER_ID, SecureKeys.USER_NAME, SecureKeys.USER_EMAIL,
        SecureKeys.USER_PHONE, SecureKeys.USER_COMPANY, SecureKeys.USER_GROUP,
        SecureKeys.USER_UPDATED_AT
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

    /**
     * Defaults to **true** when neither the host app nor a [TraceMode] has set
     * it. The host app's explicit [setOfflineTracking] wins; failing that the
     * mode's `offline` value; failing that the durable queue is on.
     *
     * Previously this defaulted to false, and the mode's `offline` was read
     * from this same key while [setTraceMode] never wrote it — so the two
     * public setters fought over one slot and `TraceMode.Builder().setOfflineSync(false)`
     * was silently ignored. Same fix as the iOS SDK's.
     */
    fun isOfflineTracking(): Boolean {
        (cache[Keys.OFFLINE_TRACKING.name] as? Boolean)?.let { return it }
        (cache[Keys.MODE_OFFLINE_SYNC.name] as? Boolean)?.let { return it }
        return true
    }

    /**
     * In-memory only, deliberately — this guards a single flush run, it is not
     * durable state. Persisted, a process kill mid-flush left it `true` and
     * blocked every later flush for the lifetime of the install. Static so all
     * [TraceDataStore] instances share one flag; a per-instance one would not
     * actually guard anything.
     */
    @Suppress("RedundantSuspendModifier") // signature kept for source compatibility
    suspend fun setDataSyncing(syncing: Boolean) {
        synchronized(syncLock) { dataSyncing = syncing }
    }

    fun isDataSyncing(): Boolean = synchronized(syncLock) { dataSyncing }

    /**
     * Compare-and-set entry point for the flush guard. `isDataSyncing()` then
     * `setDataSyncing(true)` is check-then-act across two calls, and there are
     * several concurrent flush callers, so two runs could both pass and the
     * first to finish would clear the flag for both. Returns true only to the
     * caller that actually claimed the run.
     */
    fun beginDataSyncIfIdle(): Boolean = synchronized(syncLock) {
        if (dataSyncing) return@synchronized false
        dataSyncing = true
        true
    }

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
        // `getTraceMode()` reads `offline` back out, but this method never
        // wrote it — a mode built with `.setOfflineSync(false)` was silently
        // ignored. Written and read on the same key now, and that key is the
        // mode's own rather than the host app's override.
        cache[Keys.MODE_OFFLINE_SYNC.name] = mode.offline
        store.edit {
            it[Keys.DESIRED_ACCURACY] = mode.desiredAccuracy.name
            it[Keys.UPDATE_INTERVAL] = mode.updateInterval
            it[Keys.DISTANCE_FILTER] = mode.distanceFilter
            it[Keys.STOP_DURATION] = mode.stopDuration
            it[Keys.ACCURACY_FILTER] = mode.accuracyFilter
            it[Keys.PING_SYNC_INTERVAL] = mode.pingSyncInterval
            it[Keys.TRACKING_TYPE] = mode.trackingMode.option
            it[Keys.DEBUG] = mode.debug
            it[Keys.MODE_OFFLINE_SYNC] = mode.offline
        }

        // A caller-supplied daily window is persisted here too. Only
        // `setTraceModeWithTiming` used to write these keys, and nothing but
        // the remote-settings path calls it — so a mode built with
        // `.setStartTime`/`.setEndTime` and handed to `startTracking` had its
        // window silently dropped, while the service's window check read the
        // store and found none.
        //
        // A full-day mode writes nothing, deliberately: a mode built without
        // times must not erase a window that `/sdk/company/settings`
        // configured. Clearing one is `clearTraceModeWithTiming()`'s job.
        if (mode.startTime != LocalTime.MIN || mode.endTime != LocalTime.MAX) {
            cache[Keys.START_TIME.name] = mode.startTime.toString()
            cache[Keys.END_TIME.name] = mode.endTime.toString()
            store.edit {
                it[Keys.START_TIME] = mode.startTime.toString()
                it[Keys.END_TIME] = mode.endTime.toString()
            }
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
                .setOfflineSync(getBoolean(Keys.MODE_OFFLINE_SYNC, true))
                .setPingSyncInterval(getInt(Keys.PING_SYNC_INTERVAL))
                .setDesiredAccuracy(TraceMode.DesiredAccuracy.fromString(getString(Keys.DESIRED_ACCURACY)))
                .apply {
                    // `updateInterval` and `distanceFilter` are alternatives,
                    // and zero means "not this axis". The builder floors them
                    // (5s / 10m), so calling both setters unconditionally
                    // turned a stored 0 into a live value: a PASSIVE mode
                    // (interval 0, distance 100) round-tripped as interval 5,
                    // and ACTIVE (interval 5, distance 0) came back
                    // distance-gated at 10m. LocationEngine reads the two as
                    // an if/else-if, so the wrong one silently won.
                    if (updateInterval != 0) setUpdateInterval(updateInterval)
                    if (distanceFilter != 0) setDistanceFilter(distanceFilter)
                }
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

    // MODE_OFFLINE_SYNC belongs to the mode, so it clears with it — otherwise
    // a mode with `offline == false` would keep the durable queue disabled
    // after the mode itself was cleared.
    suspend fun clearTraceMode() = removeAndCache(
        Keys.DESIRED_ACCURACY, Keys.UPDATE_INTERVAL, Keys.DISTANCE_FILTER,
        Keys.STOP_DURATION, Keys.ACCURACY_FILTER, Keys.PING_SYNC_INTERVAL,
        Keys.TRACKING_TYPE, Keys.DEBUG, Keys.MODE_OFFLINE_SYNC
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

        /** See [setDataSyncing] — process-wide, never persisted. */
        private val syncLock = Any()
        private var dataSyncing = false

        @Volatile
        private var INSTANCE: TraceDataStore? = null

        operator fun invoke(context: Context): TraceDataStore {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TraceDataStore(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
