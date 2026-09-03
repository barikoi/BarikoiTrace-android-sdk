package com.barikoi.barikoitrace.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.barikoi.barikoitrace.R
import com.barikoi.barikoitrace.location.LocationEngine
import com.barikoi.barikoitrace.storage.OfflineLocationDb
import com.barikoi.barikoitrace.storage.OfflineLocationEntity
import com.barikoi.barikoitrace.storage.TraceDataStore
import com.barikoi.barikoitrace.util.DateTimeUtils
import com.google.gson.JsonObject

class LocTraceDataService(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "LocTraceDataService"
        private const val CHANNEL_ID = "barikoi_channel_sync_location"
        private const val NOTIFICATION_ID = 36999
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "doWork: Started to work")

        // Promotion to a foreground worker is best-effort. It is *not* worth
        // taking the host app down for: an exception thrown out of
        // `setForeground` reaches WorkManager's SystemForegroundService on the
        // main thread and becomes a fatal RuntimeException in the host process
        // — a library killing an app over a periodic sync it could simply run
        // in the background instead.
        //
        // Real cases that throw here: the app lacks location permission at
        // this instant (SecurityException, since the FGS type is `location`),
        // the OS refuses a background FGS start
        // (ForegroundServiceStartNotAllowedException on API 31+), or the
        // process is otherwise restricted. In all of them the work below still
        // runs, just without the notification and its wake guarantees.
        try {
            setForeground(createForegroundInfo())
        } catch (e: Exception) {
            Log.w(TAG, "Continuing as a background worker — foreground promotion refused", e)
        }

        return try {
            val location = LocationEngine(applicationContext).getCurrentLocation()
            val db = OfflineLocationDb.getInstance(applicationContext)
            val dataStore = TraceDataStore(applicationContext)
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
            db.locationDao().insert(OfflineLocationEntity(json = json.toString()))
            Log.d(TAG, "Location saved offline for MQTT sync")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Could not get location", e)
            Result.failure()
        }
    }

    private fun createForegroundInfo(): ForegroundInfo {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel()
        }

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Syncing Location")
            .setTicker("Syncing Location")
            .setSmallIcon(R.drawable.barikoi_logo)
            .setOngoing(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .build()

        // The type has to be supplied *here*, not only in the manifest.
        // WorkManager passes `ForegroundInfo.foregroundServiceType` straight to
        // `Service.startForeground(...)`, and the two-argument constructor
        // leaves it at `FOREGROUND_SERVICE_TYPE_NONE` (0). On a targetSdk-34+
        // app that is fatal:
        //
        //   android.app.InvalidForegroundServiceTypeException:
        //   Starting FGS with type none ... has been prohibited
        //
        // The `<service android:name="androidx.work.impl.foreground.SystemForegroundService"
        // android:foregroundServiceType="location" tools:node="merge"/>` entry
        // in this SDK's manifest declares the *permitted* type; this declares
        // the one actually being used. Both are required.
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createChannel() {
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val channel = NotificationChannel(
            CHANNEL_ID, "Location Syncing",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()
            setSound(soundUri, audioAttributes)
        }
        (applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }
}
