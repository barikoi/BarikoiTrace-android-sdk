package com.barikoi.barikoitrace.model

import com.barikoi.barikoitrace.TraceMode
import com.barikoi.barikoitrace.TraceModeSource

/** A point-in-time snapshot; a running service does not itself confirm MQTT delivery. */
data class TraceStatus(
    val serviceRunning: Boolean,
    val trackingRequested: Boolean,
    val mqttConnected: Boolean,
    val pendingLocationCount: Int,
    val tripId: String?,
    val mode: TraceMode,
    val source: TraceModeSource,
    val offlineTracking: Boolean,
    val broadcasting: Boolean,
    val logging: Boolean,
    val baseUrl: String,
    val mqttUrl: String
)
