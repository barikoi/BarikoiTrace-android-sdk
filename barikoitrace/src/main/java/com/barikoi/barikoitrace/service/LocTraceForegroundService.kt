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
import com.barikoi.barikoitrace.util.TraceLogger
import com.barikoi.barikoitrace.R
import com.barikoi.barikoitrace.api.ApiRoutes
import com.barikoi.barikoitrace.api.MqttManager
import com.barikoi.barikoitrace.location.LocationEngine
import com.barikoi.barikoitrace.location.LocationUpdateListener
import com.barikoi.barikoitrace.model.TraceError
import com.barikoi.barikoitrace.storage.OfflineLocationEntity
import com.barikoi.barikoitrace.storage.TraceDataStore
import com.barikoi.barikoitrace.util.DateTimeUtils
import com.barikoi.barikoitrace.util.SystemSettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalTime

class LocTraceForegroundService : Service(), LocationUpdateListener {

    companion object {
        private const val TAG = "LocTraceService"
        private const val CHANNEL_ID = "BarikoiTrace"
        private const val NOTIFICATION_ID = 1
        const val ACTION_UPDATE_USER_NAME = "com.barikoi.barikoitrace.action.UPDATE_USER_NAME"
        const val ACTION_TRACE_MODE_CHANGED = "com.barikoi.barikoitrace.action.TRACE_MODE_CHANGED"
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val logger by lazy { TraceLogger(this, TAG) }
    private lateinit var dataStore: TraceDataStore
    private lateinit var locationEngine: LocationEngine
    private lateinit var offlineDb: com.barikoi.barikoitrace.storage.OfflineLocationDb
    private var mqttManager: MqttManager? = null
    private var connectionToken: Any? = null
    private var lastLocation: Location? = null
    private var foregroundStartFailed = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureForegroundStarted()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!ensureForegroundStarted()) return START_NOT_STICKY

        if (intent?.action == ACTION_UPDATE_USER_NAME) {
            handleUpdateUserName()
            return START_STICKY
        }

        if (intent?.action == ACTION_TRACE_MODE_CHANGED) {
            handleTraceModeChanged()
            return START_STICKY
        }

        try {
            dataStore = TraceDataStore(this)
            // Reuse the engine so it can remove its previous callback before restarting.
            if (!::locationEngine.isInitialized) locationEngine = LocationEngine(this)
            offlineDb = com.barikoi.barikoitrace.storage.OfflineLocationDb.getInstance(this)

            val user = dataStore.getUser()
            val uuid = dataStore.getDeviceToken()
            val traceMode = dataStore.getTraceMode()

            if (user != null && uuid != null) {
                val mqttUrl = dataStore.getMqttUrl() ?: ApiRoutes.MQTT_URL
                initializeMqtt(mqttUrl, user.userId, uuid, user.companyId ?: "", user.group ?: "", user.name)
            }

            if (traceMode != null) {
                locationEngine.startLocationUpdates(traceMode, this)
            }
        } catch (e: Exception) {
            logger.e("Error starting service", e)
        }

        return START_STICKY
    }

    override fun onDestroy() {
        TraceConnectionState.clear(connectionToken)
        // Send final location with trip_status "completed" before tearing down MQTT
        val tripId = if (::dataStore.isInitialized) dataStore.getLocalTripId() else null
        if (tripId != null) {
            val location = lastLocation
            if (location != null && mqttManager?.isConnected() == true) {
                mqttManager?.publishLocation(location, tripId, "completed")
            }
            serviceScope.launch { dataStore.clearLocalTrip() }
        }
        mqttManager?.destroy()
        mqttManager = null
        if (::locationEngine.isInitialized) locationEngine.stopLocationUpdates()
        super.onDestroy()
    }

    // --- LocationUpdateListener ---

    override fun onLocationReceived(location: Location) {
        val traceMode = dataStore.getTraceMode() ?: return

        // Check time window
        if (!traceMode.isWithinTrackingWindow(LocalTime.now())) {
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
            logger.w("Mock location detected")
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

        logger.d("Location update accepted")

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
        } else if (dataStore.isOfflineTracking()) {
            serviceScope.launch {
                if (!dataStore.isOfflineTracking()) return@launch
                val tripId = dataStore.getLocalTripId()
                val json = com.google.gson.JsonObject().apply {
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
                offlineDb.locationDao().insert(
                    OfflineLocationEntity(json = json.toString())
                )
            }
        }
    }

    override fun onFailure(error: TraceError) {
        logger.e("Location update failed")
    }

    override fun onProviderAvailabilityChanged(available: Boolean) {
        if (!available) {
            showLocationDisabledNotification()
        } else {
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(2)
        }
    }

    private fun handleUpdateUserName() {
        if (!::dataStore.isInitialized) dataStore = TraceDataStore(this)
        val name = dataStore.getUser()?.name
        val mqtt = mqttManager
        if (mqtt == null) {
            logger.w("updateUserName action ignored - MQTT not initialized")
            return
        }
        if (name.isNullOrBlank()) {
            logger.w("updateUserName action ignored - no stored user name")
            return
        }
        mqtt.updateUserName(name)
        logger.i("MQTT user name updated")
    }

    private fun handleTraceModeChanged() {
        try {
            if (!::dataStore.isInitialized) dataStore = TraceDataStore(this)
            if (!::locationEngine.isInitialized) locationEngine = LocationEngine(this)
            val traceMode = dataStore.getTraceMode()
            // startLocationUpdates stops the previous request first, so this safely replaces it
            locationEngine.startLocationUpdates(traceMode, this)
            logger.i(
                "Trace mode re-applied: interval=${traceMode.updateInterval}s distance=${traceMode.distanceFilter}m accuracyFilter=${traceMode.accuracyFilter}m"
            )
        } catch (e: Exception) {
            logger.e("Error applying new trace mode", e)
        }
    }

    // --- MQTT ---

    private fun initializeMqtt(
        serverUri: String, userId: String, uuid: String,
        companyId: String, groupId: String, userName: String?
    ) {
        // Tear down any previous client before replacing it (re-starts of the
        // service would otherwise leave the old connection alive)
        mqttManager?.destroy()
        val token = Any()
        connectionToken = token
        TraceConnectionState.bind(token)
        mqttManager = MqttManager(
            this, serverUri, userId, companyId, groupId, uuid,
            callback = object : MqttManager.MqttStatusCallback {
                override fun onConnectionStatusChanged(connected: Boolean, message: String) {
                    TraceConnectionState.update(token, connected)
                    logger.i("MQTT connection status changed: connected=$connected")
                    if (connected) {
                        flushOfflineData()
                    }
                }
                override fun onMessageDelivered(topic: String) {
                    logger.d("MQTT message delivered")
                }
                override fun onMessageReceived(topic: String, message: String) {
                    if (topic.endsWith("/command")) {
                        logger.d("Received MQTT command")
                    }
                }
            },
            userName = userName
        )
        mqttManager?.connect()
    }

    // --- Offline Data Sync ---

    private fun flushOfflineData() {
        serviceScope.launch {
            val dao = offlineDb.locationDao()
            val mqtt = mqttManager
            if (mqtt == null || !mqtt.isConnected()) return@launch
            try {
                val userId = dataStore.getUserId() ?: return@launch
                OfflineDataFlusher(dao, mqtt::publishLocationJsonAndAwaitDelivery).flush(userId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.e("Auto-sync failed", e)
            }
        }
    }

    // --- Notifications ---

    private fun ensureForegroundStarted(): Boolean {
        if (foregroundStartFailed) return false
        return try {
            startForegroundNotification()
            true
        } catch (e: Exception) {
            foregroundStartFailed = true
            stopSelf()
            logger.e("Unable to start foreground service", e)
            false
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
