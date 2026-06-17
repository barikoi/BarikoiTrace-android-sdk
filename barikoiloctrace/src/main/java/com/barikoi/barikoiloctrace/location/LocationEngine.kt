package com.barikoi.barikoiloctrace.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.barikoi.barikoiloctrace.TraceMode
import com.barikoi.barikoiloctrace.model.TraceError
import com.barikoi.barikoiloctrace.util.SystemSettingsManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class LocationEngine(private val context: Context) {

    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private var locationCallback: LocationCallback? = null

    @SuppressLint("MissingPermission")
    fun startLocationUpdates(
        traceMode: TraceMode,
        listener: LocationUpdateListener
    ) {
        if (!SystemSettingsManager.checkPermissions(context)) {
            listener.onFailure(TraceError.locationPermissionError())
            return
        }

        stopLocationUpdates()

        val priority = when (traceMode.desiredAccuracy) {
            TraceMode.DesiredAccuracy.HIGH -> Priority.PRIORITY_HIGH_ACCURACY
            TraceMode.DesiredAccuracy.MEDIUM -> Priority.PRIORITY_BALANCED_POWER_ACCURACY
            TraceMode.DesiredAccuracy.LOW -> Priority.PRIORITY_LOW_POWER
        }

        val requestBuilder = LocationRequest.Builder(
            if (traceMode.updateInterval > 0) traceMode.updateInterval * 1000L else 5000L
        ).apply {
            setPriority(priority)
            setWaitForAccurateLocation(true)

            if (traceMode.updateInterval > 0) {
                val interval = traceMode.updateInterval * 1000L
                setIntervalMillis(interval)
                setMinUpdateIntervalMillis(interval)
                if (traceMode.pingSyncInterval > 0) {
                    setMaxUpdateDelayMillis(traceMode.pingSyncInterval * 1000L)
                }
            } else if (traceMode.distanceFilter > 0) {
                setMinUpdateDistanceMeters(traceMode.distanceFilter.toFloat())
            }
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                for (location in result.locations) {
                    listener.onLocationReceived(location)
                }
            }
        }

        fusedClient.requestLocationUpdates(
            requestBuilder.build(),
            locationCallback!!,
            Looper.getMainLooper()
        )
    }

    fun stopLocationUpdates() {
        locationCallback?.let {
            fusedClient.removeLocationUpdates(it)
            locationCallback = null
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location = suspendCancellableCoroutine { continuation ->
        if (!SystemSettingsManager.checkPermissions(context)) {
            continuation.resumeWithException(SecurityException("Location permission not granted"))
            return@suspendCancellableCoroutine
        }

        val cts = CancellationTokenSource()
        fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
            .addOnSuccessListener { location ->
                if (location != null) {
                    continuation.resume(location)
                } else {
                    continuation.resumeWithException(Exception("Could not determine location"))
                }
            }
            .addOnFailureListener { e ->
                continuation.resumeWithException(e)
            }
    }
}
