package com.barikoi.barikoiloctrace

import java.time.LocalTime

data class TraceMode(
    val desiredAccuracy: DesiredAccuracy = DesiredAccuracy.HIGH,
    val updateInterval: Int = 0,
    val distanceFilter: Int = 0,
    val stopDuration: Int = 0,
    val accuracyFilter: Int = 100,
    val trackingMode: TrackingMode = TrackingMode.CUSTOM,
    val offline: Boolean = true,
    val debug: Boolean = false,
    val pingSyncInterval: Int = 0,
    val startTime: LocalTime = LocalTime.MIN,
    val endTime: LocalTime = LocalTime.MAX
) {
    enum class DesiredAccuracy {
        HIGH, MEDIUM, LOW;

        companion object {
            fun fromString(str: String?): DesiredAccuracy {
                if (str.isNullOrBlank()) return HIGH
                return try { valueOf(str) } catch (_: Exception) { HIGH }
            }
        }
    }

    enum class AppState {
        FOREGROUND, BACKGROUND, ALWAYS_ON
    }

    enum class TrackingMode(val option: Int) {
        PASSIVE(0), REACTIVE(1), ACTIVE(2), CUSTOM(3)
    }

    class Builder {
        private var desiredAccuracy = DesiredAccuracy.HIGH
        private var updateInterval = 0
        private var distanceFilter = 0
        private var stopDuration = 0
        private var accuracyFilter = 100
        private var offline = true
        private var debug = false
        private var pingSyncInterval = 0
        private var startTime = LocalTime.MIN
        private var endTime = LocalTime.MAX

        fun setDesiredAccuracy(accuracy: DesiredAccuracy) = apply { this.desiredAccuracy = accuracy }
        fun setUpdateInterval(seconds: Int) = apply { this.updateInterval = if (seconds < 5) 5 else seconds }
        fun setDistanceFilter(meters: Int) = apply { this.distanceFilter = if (meters < 10) 10 else meters }
        fun setStopDuration(seconds: Int) = apply { this.stopDuration = seconds }
        fun setAccuracyFilter(meters: Int) = apply { this.accuracyFilter = if (meters < 20) 20 else meters }
        fun setOfflineSync(enabled: Boolean) = apply { this.offline = enabled }
        fun setDebugModeOn() = apply { this.debug = true }
        fun setPingSyncInterval(seconds: Int) = apply { this.pingSyncInterval = seconds }
        fun setStartTime(time: LocalTime) = apply { this.startTime = time }
        fun setEndTime(time: LocalTime) = apply { this.endTime = time }

        fun build() = TraceMode(
            desiredAccuracy, updateInterval, distanceFilter, stopDuration,
            accuracyFilter, TrackingMode.CUSTOM, offline, debug,
            pingSyncInterval, startTime, endTime
        )
    }

    companion object {
        @JvmField
        val ACTIVE = TraceMode(
            DesiredAccuracy.HIGH, 5, 0, 0, 50,
            TrackingMode.ACTIVE, offline = true, debug = false, pingSyncInterval = 0
        )

        @JvmField
        val PASSIVE = TraceMode(
            DesiredAccuracy.MEDIUM, 0, 100, 0, 300,
            TrackingMode.PASSIVE, offline = true, debug = false, pingSyncInterval = 120
        )

        @JvmField
        val REACTIVE = TraceMode(
            DesiredAccuracy.HIGH, 0, 100, 0, 100,
            TrackingMode.REACTIVE, offline = true, debug = false, pingSyncInterval = 30
        )
    }
}
