package com.barikoi.barikoitrace.location

import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.barikoi.barikoitrace.TraceMode
import com.barikoi.barikoitrace.storage.TraceDataStore
import com.barikoi.barikoitrace.util.SystemSettingsManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.Tasks
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LocationEngineTest {
    @Test
    fun restoredPassiveMode_requests100MeterDistanceFilter() {
        val request = requestForRestoredMode(TraceMode.PASSIVE)
        assertThat(request.minUpdateDistanceMeters).isEqualTo(100f)
        assertThat(request.priority).isEqualTo(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
    }

    @Test
    fun restoredReactiveMode_requests100MeterDistanceFilter() {
        val request = requestForRestoredMode(TraceMode.REACTIVE)
        assertThat(request.minUpdateDistanceMeters).isEqualTo(100f)
        assertThat(request.priority).isEqualTo(Priority.PRIORITY_HIGH_ACCURACY)
    }

    @Test
    fun restoredActiveMode_keepsFiveSecondIntervalWithoutDistanceFilter() {
        val request = requestForRestoredMode(TraceMode.ACTIVE)
        assertThat(request.intervalMillis).isEqualTo(5000L)
        assertThat(request.minUpdateIntervalMillis).isEqualTo(5000L)
        assertThat(request.minUpdateDistanceMeters).isEqualTo(0f)
    }

    private fun requestForRestoredMode(mode: TraceMode): LocationRequest = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dataStore = TraceDataStore(context)
        dataStore.setTraceMode(mode)
        val client = mockk<FusedLocationProviderClient>()
        val request = slot<LocationRequest>()
        every {
            client.requestLocationUpdates(capture(request), any<LocationCallback>(), any<Looper>())
        } returns Tasks.forResult<Void>(null)
        every { client.removeLocationUpdates(any<LocationCallback>()) } returns Tasks.forResult<Void>(null)
        mockkStatic(LocationServices::class) {
            every { LocationServices.getFusedLocationProviderClient(any<Context>()) } returns client
            mockkObject(SystemSettingsManager) {
                every { SystemSettingsManager.checkPermissions(any()) } returns true
                val engine = LocationEngine(context)
                try {
                    engine.startLocationUpdates(dataStore.getTraceMode(), mockk())
                } finally {
                    engine.stopLocationUpdates()
                }
            }
        }
        request.captured
    }
}
