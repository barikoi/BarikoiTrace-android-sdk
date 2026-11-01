package com.barikoi.barikoitracesdkapp

import com.barikoi.barikoitrace.TraceMode
import java.net.URI

internal object DemoInput {
    fun restUrl(value: String): String {
        val text = value.trim()
        val uri = runCatching { URI(text) }.getOrNull()
        require(uri != null && uri.scheme in setOf("http", "https") && !uri.host.isNullOrBlank() &&
            uri.userInfo == null && uri.query == null && uri.fragment == null &&
            (uri.port == -1 || uri.port in 1..65535)) {
            "Use an HTTP or HTTPS base URL without credentials, query, or fragment."
        }
        return text.trimEnd('/') + "/"
    }

    fun mqttUrl(value: String): String {
        val text = value.trim()
        val uri = runCatching { URI(text) }.getOrNull()
        require(uri != null && uri.scheme in setOf("tcp", "ssl", "ws", "wss") &&
            !uri.host.isNullOrBlank() && uri.userInfo == null && uri.query == null &&
            uri.fragment == null && (uri.port == -1 || uri.port in 1..65535) &&
            (uri.scheme in setOf("ws", "wss") || uri.path.isNullOrEmpty())) {
            "Use a TCP, SSL, WS, or WSS broker URL with a valid host and port."
        }
        return text
    }

    fun mode(preset: String, interval: String, distance: String, accuracy: String, offline: Boolean): TraceMode {
        val mode = when (preset) {
            "ACTIVE" -> TraceMode.ACTIVE
            "PASSIVE" -> TraceMode.PASSIVE
            "REACTIVE" -> TraceMode.REACTIVE
            else -> {
                val seconds = interval.trim().toIntOrNull()
                val meters = distance.trim().toIntOrNull()
                val precision = accuracy.trim().toIntOrNull()
                require(seconds != null && (seconds == 0 || seconds >= 5)) { "Interval must be 0 or at least 5 seconds." }
                require(meters != null && (meters == 0 || meters >= 10)) { "Distance must be 0 or at least 10 meters." }
                require(precision != null && precision >= 20) { "Accuracy must be at least 20 meters." }
                require(seconds == 0 || meters == 0) { "Choose an interval or a distance filter. Set the other to 0." }
                TraceMode.Builder().setUpdateInterval(seconds).setDistanceFilter(meters)
                    .setAccuracyFilter(precision).build()
            }
        }
        return mode.copy(offline = offline)
    }
}
