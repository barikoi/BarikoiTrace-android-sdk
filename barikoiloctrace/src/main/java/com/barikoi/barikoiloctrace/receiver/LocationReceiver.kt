package com.barikoi.barikoiloctrace.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build

class LocationReceiver : BroadcastReceiver() {

    interface EventCallback {
        fun onError(error: String)
        fun onLocationUpdated(location: Location)
    }

    var callback: EventCallback? = null

    fun setEventCallback(callback: EventCallback) {
        this.callback = callback
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.barikoi.trace.android.RECEIVED") {
            try {
                val location = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra("location", Location::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra("location")
                }

                val event = intent.getStringExtra("event")
                val error = intent.getStringExtra("error")

                if (location != null && event == "LOCATION_RECEIVED") {
                    callback?.onLocationUpdated(location)
                }

                if (error != null) {
                    callback?.onError(error)
                }
            } catch (e: Exception) {
                callback?.onError(e.message ?: "Unknown error")
            }
        }
    }
}
