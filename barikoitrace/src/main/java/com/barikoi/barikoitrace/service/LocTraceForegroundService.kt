package com.barikoi.barikoitrace.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.barikoi.barikoitrace.BarikoiTrace
import com.barikoi.barikoitrace.R
import com.barikoi.barikoitrace.TraceMode
import com.barikoi.barikoitrace.api.ApiRoutes
import com.barikoi.barikoitrace.api.MqttManager
import com.barikoi.barikoitrace.location.LocationEngine
import com.barikoi.barikoitrace.location.LocationUpdateListener
import com.barikoi.barikoitrace.model.TraceError
import com.barikoi.barikoitrace.storage.OfflineLocationEntity
import com.barikoi.barikoitrace.storage.TraceDataStore
import com.barikoi.barikoitrace.util.DateTimeUtils
import com.barikoi.barikoitrace.util.SystemSettingsManager
import com.google.gson.JsonParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalTime

class LocTraceForegroundService : Service(), LocationUpdateListener {

    companion object {
        private const val TAG = "LocTraceService"
        private const val CHANNEL_ID = "BarikoiTrace"
        private const val NOTIFICATION_ID = 1
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var dataStore: TraceDataStore
    private lateinit var locationEngine: LocationEngine
    private lateinit var offlineDb: com.barikoi.barikoitrace.storage.OfflineLocationDb
    private var mqttManager: MqttManager? = null
    private var lastLocation: Location? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ensureForegroundStarted()

        try {
            dataStore = TraceDataStore(this)
            locationEngine = LocationEngine(this)
            offlineDb = com.barikoi.barikoitrace.storage.OfflineLocationDb.getInstance(this)

            val user = dataStore.getUser()
            val uuid = dataStore.getDeviceToken()
            val traceMode = dataStore.getTraceMode()

            if (user != null && uuid != null) {
                val mqttUrl = dataStore.getMqttUrl() ?: ApiRoutes.MQTT_URL
                val mqttUsername = dataStore.getMqttUsername()
                val mqttPassword = dataStore.getMqttPassword()
                if (mqttUsername.isNullOrBlank() || mqttPassword.isNullOrBlank()) {
                    Log.e(TAG, "No MQTT credentials set — call BarikoiTrace.initialize(context, apiKey, mqttUsername, mqttPassword) before starting tracking")
                } else {
                    initializeMqtt(
                        mqttUrl, user.userId, uuid, user.companyId ?: "", user.group ?: "", user.name,
                        mqttUsername, mqttPassword
                    )
                }
            }

            if (traceMode != null) {
                locationEngine.startLocationUpdates(traceMode, this)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting service", e)
        }

        return START_STICKY
    }

    override fun onDestroy() {
        // Send final location with trip_status "completed" before tearing down MQTT
        val tripId = dataStore.getLocalTripId()
        if (tripId != null) {
            val location = lastLocation
            if (location != null && mqttManager?.isConnected() == true) {
                mqttManager?.publishLocation(location, tripId, "completed")
            }
            serviceScope.launch { dataStore.clearLocalTrip() }
        }
        mqttManager?.destroy()
        mqttManager = null
        locationEngine.stopLocationUpdates()
        super.onDestroy()
    }

    // --- LocationUpdateListener ---

    override fun onLocationReceived(location: Location) {
        val traceMode = dataStore.getTraceMode() ?: return

        // Check time window
        if (traceMode.endTime != LocalTime.MAX && !isWithinTrackingWindow(traceMode)) {
            serviceScope.launch { dataStore.setSdkTracking(false) }
            stopSelf()
            return
        }

        // Check location staleness (reject locations older than 10 seconds)
        if (System.currentTimeMillis() - location.time > 10000) {
            return
        }

        // Mock detection
        if (SystemSettingsManager.checkIfMockProvider(this, location)) {
            Log.w(TAG, "Mock location detected")
            return
        }

        // Validate accuracy
        val traceModeAccuracy = traceMode.accuracyFilter
        if (location.accuracy > traceModeAccuracy.toFloat()) {
            return
        }

        if (location.accuracy < 0) {
            return
        }

        Log.d(TAG, "Location: accuracy=${location.accuracy}, time=${DateTimeUtils.getDateTimeLocal(location.time)}")

        lastLocation = location

        // Broadcast location to in-app subscribers
        if (dataStore.isBroadcasting()) {
            com.barikoi.barikoitrace.LocTraceManager.getInstance(this).broadcastLocation(location)
        }

        // Publish via MQTT, or save offline if not connected
        if (mqttManager != null && mqttManager!!.isConnected()) {
            val tripId = dataStore.getLocalTripId()
            mqttManager?.publishLocation(location, tripId)
            flushOfflineData()
        } else {
            serviceScope.launch {
                val tripId = dataStore.getLocalTripId()
                val user = dataStore.getUser()
                val json = com.google.gson.JsonObject().apply {
                    addProperty("latitude", location.latitude)
                    addProperty("longitude", location.longitude)
                    addProperty("bearing", location.bearing)
                    addProperty("altitude", location.altitude)
                    addProperty("gpx_time", DateTimeUtils.getDateTimeLocal(location.time))
                    addProperty("speed", location.speed)
                    addProperty("accuracy", location.accuracy)
                    // company_id/user_name used to be missing from this
                    // payload entirely (only user_id got backfilled at
                    // flush time, in flushOfflineData() below) — the queued
                    // shape now carries the same fields the live-publish
                    // path does, whenever they're known at write time.
                    user?.companyId?.takeIf { it.isNotBlank() }?.let { addProperty("company_id", it) }
                    user?.name?.takeIf { it.isNotBlank() }?.let { addProperty("user_name", it) }
                    tripId?.let {
                        addProperty("trip_id", it)
                        addProperty("trip_status", "active")
                    }
                }
                // Gated on the same flag iOS gates on. Android used to queue
                // unconditionally, so `setOfflineTracking(false)` did nothing
                // here while it took effect on iOS — the one config knob whose
                // meaning differed between the platforms. The flag now defaults
                // to true (see TraceDataStore.isOfflineTracking), so the
                // queue-everything behavior is unchanged unless a host app
                // explicitly turns it off.
                if (dataStore.isOfflineTracking()) {
                    offlineDb.locationDao().insert(
                        OfflineLocationEntity(json = json.toString())
                    )
                } else {
                    BarikoiTrace.notifyLog(
                        "WARN", TAG,
                        "MQTT not connected and offline tracking is off — location discarded"
                    )
                }
            }
        }
    }

    /**
     * A window that wraps past midnight (start > end, e.g. 22:00–06:00) is the
     * union of both sides. The previous `now.isAfter(end) || now.isBefore(start)`
     * pair rejected *every* instant in that case — a night-shift window meant
     * tracking stopped on the first fix. Matches the iOS SDK's
     * `TraceManager.isWithinTrackingWindow`.
     */
    private fun isWithinTrackingWindow(traceMode: TraceMode): Boolean {
        val now = LocalTime.now()
        val start = traceMode.startTime
        val end = traceMode.endTime
        return if (start > end) {
            !now.isBefore(start) || !now.isAfter(end)
        } else {
            !now.isBefore(start) && !now.isAfter(end)
        }
    }

    override fun onFailure(error: TraceError) {
        Log.e(TAG, "Location error: ${error.message}")
    }

    override fun onProviderAvailabilityChanged(available: Boolean) {
        if (!available) {
            showLocationDisabledNotification()
        } else {
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(2)
        }
    }

    // --- MQTT ---

    private fun initializeMqtt(
        serverUri: String, userId: String, uuid: String,
        companyId: String, groupId: String, userName: String?,
        mqttUsername: String, mqttPassword: String
    ) {
        mqttManager = MqttManager(
            this, serverUri, userId, companyId, groupId, uuid,
            callback = object : MqttManager.MqttStatusCallback {
                override fun onConnectionStatusChanged(connected: Boolean, message: String) {
                    BarikoiTrace.notifyLog("INFO", TAG, "MQTT $message")
                    if (connected) {
                        flushOfflineData()
                    }
                }
                override fun onMessageDelivered(topic: String) {
                    BarikoiTrace.notifyLog("DEBUG", TAG, "Published to topic: $topic")
                }
                override fun onMessageReceived(topic: String, message: String) {
                    if (topic.endsWith("/command")) {
                        Log.d(TAG, "Received command: $message")
                    }
                }
                override fun onConnectionRejected(message: String) {
                    // Surfaced at ERROR and not retried — the broker refused
                    // this exact CONNECT, so the host app has to change
                    // something before another attempt can succeed.
                    Log.e(TAG, message)
                    BarikoiTrace.notifyLog("ERROR", TAG, message)
                }
            },
            userName = userName,
            mqttUsername = mqttUsername,
            mqttPassword = mqttPassword,
            clientIdPrefix = dataStore.getMqttClientIdPrefix()
        )
        mqttManager?.connect()
    }

    // --- Offline Data Sync ---

    private fun flushOfflineData() {
        // Single compare-and-set. The old `if (isDataSyncing()) return` +
        // `setDataSyncing(true)` was check-then-act across two calls, so two
        // callers could both pass and whichever finished first cleared the
        // flag for both. Same fix as the iOS SDK's `beginDataSyncIfIdle()`.
        if (!dataStore.beginDataSyncIfIdle()) return

        serviceScope.launch {
            try {
                val dao = offlineDb.locationDao()
                val mqtt = mqttManager
                if (mqtt == null || !mqtt.isConnected()) return@launch

                // Android used to refuse to flush at all without a user id;
                // the rows now simply stay queued rather than being skipped
                // silently, matching iOS.
                val userId = dataStore.getUserId() ?: run {
                    BarikoiTrace.notifyLog(
                        "WARN", TAG,
                        "Offline flush deferred — no authenticated user to attribute rows to"
                    )
                    return@launch
                }
                val user = dataStore.getUser()

                // Bounded loop instead of recursion. The old tail call
                // re-entered through `flushOfflineData()`, which re-launched a
                // coroutine per batch and could not terminate at all if
                // `deleteBatch()` failed — the same rows were republished
                // forever while `getCount()` never dropped.
                var remaining = dao.getCount()
                while (remaining > 0) {
                    val batch = dao.getBatch()
                    if (batch.isEmpty()) break

                    for (entity in batch) {
                        try {
                            val locJson = JsonParser.parseString(entity.json).asJsonObject
                            locJson.addProperty("user_id", userId)
                            // Backfill company_id/user_name for rows queued
                            // before this fix that don't already carry them
                            // (fresh writes already include both — see
                            // onLocationReceived's offline branch above).
                            if (!locJson.has("company_id")) {
                                user?.companyId?.takeIf { it.isNotBlank() }?.let { locJson.addProperty("company_id", it) }
                            }
                            if (!locJson.has("user_name")) {
                                user?.name?.takeIf { it.isNotBlank() }?.let { locJson.addProperty("user_name", it) }
                            }
                            mqtt.publishLocationJson(locJson)
                        } catch (_: Exception) {}
                    }

                    // Checked before the delete: a link that dropped mid-batch
                    // means those publishes went nowhere, and deleting first
                    // would discard them.
                    if (!mqtt.isConnected()) break

                    dao.deleteBatch()

                    val afterDelete = dao.getCount()
                    // Stall guard: a delete that frees nothing would otherwise
                    // spin this loop.
                    if (afterDelete >= remaining) break
                    remaining = afterDelete
                }
            } catch (e: Exception) {
                Log.e(TAG, "Auto-sync failed", e)
            } finally {
                // `finally`, so no early return or thrown exception can leave
                // the claim held.
                dataStore.setDataSyncing(false)
            }
        }
    }

    // --- Notifications ---

    private fun ensureForegroundStarted() {
        try {
            startForegroundNotification()
        } catch (e: Exception) {
            Log.e(TAG, "Error ensuring foreground started", e)
        }
    }

    private fun startForegroundNotification() {
        val channelId = CHANNEL_ID
        val channelName = "Trace is running as Background service"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT
            )
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }

        val notification = buildNotification(channelName)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID, notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(text: String): Notification {
        return androidx.core.app.NotificationCompat.Builder(this, CHANNEL_ID)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_trace_logo)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setOngoing(true)
            .setForegroundServiceBehavior(androidx.core.app.NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun showLocationDisabledNotification() {
        val pm = packageManager
        val intent = pm.getLaunchIntentForPackage(packageName)
        val contentIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Need to turn on location service",
                NotificationManager.IMPORTANCE_HIGH
            )
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }

        val notification = androidx.core.app.NotificationCompat.Builder(this, CHANNEL_ID)
            .setCategory(Notification.CATEGORY_ERROR)
            .setContentText("Need to turn on location service")
            .setSmallIcon(R.drawable.ic_trace_logo)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_MAX)
            .setContentIntent(contentIntent)
            .build()

        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .notify(2, notification)
    }
}
