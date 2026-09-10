package com.barikoi.barikoitrace

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalTime

class TraceModeTest {

    @Test
    fun builder_clampsUpdateIntervalToMinimum5() {
        val mode = TraceMode.Builder().setUpdateInterval(3).build()
        assertThat(mode.updateInterval).isEqualTo(5)
    }

    @Test
    fun builder_updateIntervalAt5IsKept() {
        val mode = TraceMode.Builder().setUpdateInterval(5).build()
        assertThat(mode.updateInterval).isEqualTo(5)
    }

    @Test
    fun builder_clampsDistanceFilterToMinimum10() {
        val mode = TraceMode.Builder().setDistanceFilter(5).build()
        assertThat(mode.distanceFilter).isEqualTo(10)
    }

    @Test
    fun builder_clampsAccuracyFilterToMinimum20() {
        val mode = TraceMode.Builder().setAccuracyFilter(15).build()
        assertThat(mode.accuracyFilter).isEqualTo(20)
    }

    @Test
    fun desiredAccuracyFromString_validInput() {
        assertThat(TraceMode.DesiredAccuracy.fromString("HIGH")).isEqualTo(TraceMode.DesiredAccuracy.HIGH)
        assertThat(TraceMode.DesiredAccuracy.fromString("MEDIUM")).isEqualTo(TraceMode.DesiredAccuracy.MEDIUM)
        assertThat(TraceMode.DesiredAccuracy.fromString("LOW")).isEqualTo(TraceMode.DesiredAccuracy.LOW)
    }

    @Test
    fun desiredAccuracyFromString_nullReturnsHigh() {
        assertThat(TraceMode.DesiredAccuracy.fromString(null)).isEqualTo(TraceMode.DesiredAccuracy.HIGH)
    }

    @Test
    fun desiredAccuracyFromString_blankReturnsHigh() {
        assertThat(TraceMode.DesiredAccuracy.fromString("")).isEqualTo(TraceMode.DesiredAccuracy.HIGH)
    }

    @Test
    fun desiredAccuracyFromString_invalidReturnsHigh() {
        assertThat(TraceMode.DesiredAccuracy.fromString("INVALID")).isEqualTo(TraceMode.DesiredAccuracy.HIGH)
    }

    @Test
    fun activePreset_hasExpectedValues() {
        val active = TraceMode.ACTIVE
        assertThat(active.desiredAccuracy).isEqualTo(TraceMode.DesiredAccuracy.HIGH)
        assertThat(active.updateInterval).isEqualTo(5)
        assertThat(active.accuracyFilter).isEqualTo(50)
        assertThat(active.trackingMode).isEqualTo(TraceMode.TrackingMode.ACTIVE)
    }

    @Test
    fun passivePreset_hasExpectedValues() {
        val passive = TraceMode.PASSIVE
        assertThat(passive.desiredAccuracy).isEqualTo(TraceMode.DesiredAccuracy.MEDIUM)
        assertThat(passive.distanceFilter).isEqualTo(100)
        assertThat(passive.accuracyFilter).isEqualTo(300)
        assertThat(passive.trackingMode).isEqualTo(TraceMode.TrackingMode.PASSIVE)
    }

    @Test
    fun reactivePreset_hasExpectedValues() {
        val reactive = TraceMode.REACTIVE
        assertThat(reactive.desiredAccuracy).isEqualTo(TraceMode.DesiredAccuracy.HIGH)
        assertThat(reactive.distanceFilter).isEqualTo(100)
        assertThat(reactive.accuracyFilter).isEqualTo(100)
        assertThat(reactive.trackingMode).isEqualTo(TraceMode.TrackingMode.REACTIVE)
    }

    @Test
    fun builder_setsStartTimeAndEndTime() {
        val start = LocalTime.of(8, 0)
        val end = LocalTime.of(18, 0)
        val mode = TraceMode.Builder()
            .setStartTime(start)
            .setEndTime(end)
            .build()
        assertThat(mode.startTime).isEqualTo(start)
        assertThat(mode.endTime).isEqualTo(end)
    }

    @Test
    fun builder_defaultsOfflineToTrue() {
        val mode = TraceMode.Builder().build()
        assertThat(mode.offline).isTrue()
    }
}
