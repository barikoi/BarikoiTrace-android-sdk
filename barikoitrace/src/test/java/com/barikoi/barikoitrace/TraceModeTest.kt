package com.barikoi.barikoitrace

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalTime

class TraceModeTest {

    @Test
    fun daytimeWindow_includesBoundsAndExcludesTimesOutside() {
        val mode = TraceMode.ACTIVE.copy(startTime = LocalTime.of(8, 0), endTime = LocalTime.of(18, 0))
        assertThat(mode.isWithinTrackingWindow(LocalTime.of(8, 0))).isTrue()
        assertThat(mode.isWithinTrackingWindow(LocalTime.NOON)).isTrue()
        assertThat(mode.isWithinTrackingWindow(LocalTime.of(18, 0))).isTrue()
        assertThat(mode.isWithinTrackingWindow(LocalTime.of(7, 59))).isFalse()
        assertThat(mode.isWithinTrackingWindow(LocalTime.of(18, 1))).isFalse()
    }

    @Test
    fun overnightWindow_includesBothSidesOfMidnightAndExcludesDaytime() {
        val mode = TraceMode.ACTIVE.copy(startTime = LocalTime.of(22, 0), endTime = LocalTime.of(6, 0))
        for (time in listOf(LocalTime.of(22, 0), LocalTime.of(23, 0), LocalTime.MIDNIGHT, LocalTime.of(5, 0), LocalTime.of(6, 0))) {
            assertThat(mode.isWithinTrackingWindow(time)).isTrue()
        }
        assertThat(mode.isWithinTrackingWindow(LocalTime.of(21, 59))).isFalse()
        assertThat(mode.isWithinTrackingWindow(LocalTime.of(6, 1))).isFalse()
        assertThat(mode.isWithinTrackingWindow(LocalTime.NOON)).isFalse()
    }

    @Test
    fun startOnlyWindow_rejectsTimesBeforeStartEvenWithDefaultEnd() {
        val mode = TraceMode.ACTIVE.copy(startTime = LocalTime.of(8, 0))
        assertThat(mode.isWithinTrackingWindow(LocalTime.of(7, 59))).isFalse()
        assertThat(mode.isWithinTrackingWindow(LocalTime.of(8, 0))).isTrue()
        assertThat(mode.isWithinTrackingWindow(LocalTime.MAX)).isTrue()
    }

    @Test
    fun defaultWindow_includesEntireDay() {
        assertThat(TraceMode.ACTIVE.isWithinTrackingWindow(LocalTime.MIN)).isTrue()
        assertThat(TraceMode.ACTIVE.isWithinTrackingWindow(LocalTime.NOON)).isTrue()
        assertThat(TraceMode.ACTIVE.isWithinTrackingWindow(LocalTime.MAX)).isTrue()
    }

    @Test
    fun builder_zeroUpdateIntervalPreservesDistanceBasedMode() {
        val mode = TraceMode.Builder().setUpdateInterval(0).setDistanceFilter(100).build()
        assertThat(mode.updateInterval).isEqualTo(0)
        assertThat(mode.distanceFilter).isEqualTo(100)
    }

    @Test
    fun builder_zeroDistanceFilterKeepsDistanceFilteringDisabled() {
        val mode = TraceMode.Builder().setUpdateInterval(10).setDistanceFilter(0).build()
        assertThat(mode.distanceFilter).isEqualTo(0)
        assertThat(mode.updateInterval).isEqualTo(10)
    }

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
