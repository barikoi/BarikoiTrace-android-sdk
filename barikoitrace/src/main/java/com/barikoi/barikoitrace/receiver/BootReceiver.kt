package com.barikoi.barikoitrace.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.barikoi.barikoitrace.util.TraceLogger
import com.barikoi.barikoitrace.LocTraceManager

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val logger = TraceLogger(context, "BootReceiver")
            logger.d("Boot completed, checking tracking state")
            try {
                val manager = LocTraceManager.getInstance(context)
                val dataStore = com.barikoi.barikoitrace.storage.TraceDataStore(context)
                if (dataStore.isSdkTracking()) {
                    val traceMode = dataStore.getTraceMode()
                    if (traceMode != null) {
                        val hasActiveTrip = dataStore.getLocalTripId() != null
                        manager.startTracking(traceMode, hasActiveTrip, dataStore.getTraceModeSource())
                    }
                }
            } catch (e: Exception) {
                logger.e("Error restarting tracking", e)
            }
        }
    }
}
