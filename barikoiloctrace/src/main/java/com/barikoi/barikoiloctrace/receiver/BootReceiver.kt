package com.barikoi.barikoiloctrace.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.barikoi.barikoiloctrace.LocTraceManager

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Boot completed, checking tracking state")
            try {
                val manager = LocTraceManager.getInstance(context)
                val dataStore = com.barikoi.barikoiloctrace.storage.TraceDataStore(context)
                if (dataStore.isSdkTracking()) {
                    val traceMode = dataStore.getTraceMode()
                    if (traceMode != null) {
                        val hasActiveTrip = dataStore.getLocalTripId() != null
                        manager.startTracking(traceMode, hasActiveTrip)
                    }
                }
            } catch (e: Exception) {
                Log.e("BootReceiver", "Error restarting tracking", e)
            }
        }
    }
}
