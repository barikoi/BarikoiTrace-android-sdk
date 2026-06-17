package com.barikoi.barikoiloctrace.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.barikoi.barikoiloctrace.R
import com.barikoi.barikoiloctrace.location.LocationEngine
import com.barikoi.barikoiloctrace.storage.OfflineLocationDb
import com.barikoi.barikoiloctrace.storage.OfflineLocationEntity
import com.barikoi.barikoiloctrace.storage.TraceDataStore
import com.barikoi.barikoiloctrace.util.DateTimeUtils
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
        setForeground(createForegroundInfo())

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

        return ForegroundInfo(NOTIFICATION_ID, notification)
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
