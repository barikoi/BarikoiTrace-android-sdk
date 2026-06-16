package com.barikoi.barikoiloctrace.service

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
import com.barikoi.barikoiloctrace.R
import com.barikoi.barikoiloctrace.BarikoiLocTrace
import com.barikoi.barikoiloctrace.api.ApiRoutes
import com.barikoi.barikoiloctrace.api.MqttManager
import com.barikoi.barikoiloctrace.api.TraceApiClient
import com.barikoi.barikoiloctrace.location.LocationEngine
import com.barikoi.barikoiloctrace.location.LocationUpdateListener
import com.barikoi.barikoiloctrace.model.TraceError
import com.barikoi.barikoiloctrace.storage.TraceDataStore
import com.barikoi.barikoiloctrace.util.DateTimeUtils
import com.barikoi.barikoiloctrace.util.SystemSettingsManager
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalTime

class LocTraceForegroundService : Service(), LocationUpdateListener {

    companion object {
        private const val TAG = "LocTraceService"
        private const val CHANNEL_ID = "BarikoiLocTrace"
        private const val NOTIFICATION_ID = 1
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var dataStore: TraceDataStore
    private lateinit var locationEngine: LocationEngine
    private lateinit var offlineDb: com.barikoi.barikoiloctrace.storage.OfflineLocationDb
    private var mqttManager: MqttManager? = null

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
            offlineDb = com.barikoi.barikoiloctrace.storage.OfflineLocationDb.getInstance(this)

            val user = dataStore.getUser()
            val uuid = dataStore.getDeviceToken()
            val traceMode = dataStore.getTraceMode()

            if (user != null && uuid != null) {
                val mqttUrl = dataStore.getMqttUrl() ?: ApiRoutes.MQTT_URL
                initializeMqtt(mqttUrl, user.userId, uuid, user.companyId ?: "", user.group ?: "")
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
        mqttManager?.destroy()
        mqttManager = null
        locationEngine.stopLocationUpdates()
        super.onDestroy()
    }

    // --- LocationUpdateListener ---

    override fun onLocationReceived(location: Location) {
        val traceMode = dataStore.getTraceMode() ?: return

        // Check time window
        if (traceMode.endTime != LocalTime.MAX) {
            val now = LocalTime.now()
            if (now.isAfter(traceMode.endTime) || now.isBefore(traceMode.startTime)) {
                serviceScope.launch { dataStore.setSdkTracking(false) }
                stopSelf()
                return
            }
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

        // Broadcast location to in-app subscribers
        if (dataStore.isBroadcasting()) {
            com.barikoi.barikoiloctrace.LocTraceManager.getInstance(this).broadcastLocation(location)
        }

        // Publish via MQTT, or save offline if not connected
        if (mqttManager != null && mqttManager!!.isConnected()) {
            mqttManager?.publishLocation(location)
            flushOfflineData()
        } else {
            serviceScope.launch {
                val json = com.google.gson.JsonObject().apply {
                    addProperty("latitude", location.latitude)
                    addProperty("longitude", location.longitude)
                    addProperty("bearing", location.bearing)
                    addProperty("altitude", location.altitude)
                    addProperty("gpx_time", DateTimeUtils.getDateTimeLocal(location.time))
                    addProperty("speed", location.speed)
                    addProperty("accuracy", location.accuracy)
                }
                offlineDb.locationDao().insert(
                    com.barikoi.barikoiloctrace.storage.OfflineLocationEntity(json = json.toString())
                )
            }
        }
    }

    override fun onFailure(error: TraceError) {
        Log.e(TAG, "Location error: ${error.message}")
    }

    override fun onProviderAvailabilityChanged(available: Boolean) {
        if (!available) {
            showLocationDisabledNotification()
        } else {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(2)
        }
    }

    // --- MQTT ---

    private fun initializeMqtt(
        serverUri: String, userId: String, uuid: String,
        companyId: String, groupId: String
    ) {
        mqttManager = MqttManager(
            this, serverUri, userId, companyId, groupId, uuid,
            object : MqttManager.MqttStatusCallback {
                override fun onConnectionStatusChanged(connected: Boolean, message: String) {
                    BarikoiLocTrace.notifyLog("INFO", TAG, "MQTT $message")
                    if (connected) {
                        flushOfflineData()
                    }
                }
                override fun onMessageDelivered(topic: String) {
                    BarikoiLocTrace.notifyLog("DEBUG", TAG, "Published to topic: $topic")
                }
                override fun onMessageReceived(topic: String, message: String) {
                    if (topic.endsWith("/command")) {
                        Log.d(TAG, "Received command: $message")
                    }
                }
            }
        )
        mqttManager?.connect()
    }

    // --- Offline Data Sync ---

    private fun flushOfflineData() {
        if (dataStore.isDataSyncing()) return
        serviceScope.launch {
            if (dataStore.isDataSyncing()) return@launch
            val dao = offlineDb.locationDao()
            if (dao.getCount() == 0) return@launch
            dataStore.setDataSyncing(true)
            try {
                val batch = dao.getBatch()
                if (batch.isEmpty()) {
                    dataStore.setDataSyncing(false)
                    return@launch
                }

                val userId = dataStore.getUserId()
                if (userId == null) {
                    dataStore.setDataSyncing(false)
                    return@launch
                }

                val data = JsonArray()
                for (entity in batch) {
                    try {
                        val locJson = JsonParser.parseString(entity.json).asJsonObject
                        locJson.addProperty("user_id", userId)
                        data.add(locJson)
                    } catch (_: Exception) {}
                }

                val apiClient = TraceApiClient.getInstance(this@LocTraceForegroundService)
                apiClient.sendBulkLocations(data)
                dao.deleteBatch()
                dataStore.setDataSyncing(false)

                // Recursively flush if more data remains
                flushOfflineData()
            } catch (e: Exception) {
                Log.e(TAG, "Auto-sync failed", e)
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
