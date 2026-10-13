package com.barikoi.barikoitrace

import android.app.Application
import android.content.Context
import android.location.LocationManager
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.barikoi.barikoitrace.model.TraceError
import com.barikoi.barikoitrace.model.TraceUser
import com.barikoi.barikoitrace.storage.TraceDataStore
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

@RunWith(RobolectricTestRunner::class)
class BarikoiTraceFacadeTest {

    private lateinit var dataStore: TraceDataStore

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        Shadows.shadowOf(context as Application).grantPermissions(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        Shadows.shadowOf(locationManager).setProviderEnabled(LocationManager.GPS_PROVIDER, true)
        Shadows.shadowOf(locationManager).setProviderEnabled(LocationManager.NETWORK_PROVIDER, true)

        BarikoiTrace.initialize(context, "test-api-key", "mqtt-user", "mqtt-pass")
        Thread.sleep(200)
        dataStore = TraceDataStore(context)
    }

    @After
    fun tearDown() = runBlocking {
        BarikoiTrace.stopTracking()
        dataStore.clearUser()
        dataStore.clearLocalTrip()
        dataStore.clearTraceModeWithTiming()
        dataStore.resetUrls()
        dataStore.setNotificationConfig(null, null)
    }

    @Test
    fun setNotificationConfig_updatesStoredTitleAndContent() = runBlocking {
        BarikoiTrace.setNotificationConfig("New Title", "New Message")
        for (i in 1..50) {
            if (dataStore.getNotificationTitle() == "New Title" &&
                dataStore.getNotificationContent() == "New Message"
            ) break
            Thread.sleep(10)
        }
        assertThat(dataStore.getNotificationTitle()).isEqualTo("New Title")
        assertThat(dataStore.getNotificationContent()).isEqualTo("New Message")

        BarikoiTrace.setNotificationConfig(null, null)
        for (i in 1..50) {
            if (dataStore.getNotificationTitle() == null &&
                dataStore.getNotificationContent() == null
            ) break
            Thread.sleep(10)
        }
        assertThat(dataStore.getNotificationTitle()).isNull()
        assertThat(dataStore.getNotificationContent()).isNull()
    }

    @Test
    fun updateUserName_suspend_updatesAndReturnsUser() = runBlocking {
        dataStore.setUser(TraceUser(userId = "u1", name = "Old", phone = "01700000000"))
        val updated = BarikoiTrace.updateUserName("Alice")
        assertThat(updated.name).isEqualTo("Alice")
        assertThat(BarikoiTrace.getUser()?.name).isEqualTo("Alice")
    }

    @Test
    fun updateUserName_callback_success() {
        runBlocking {
            dataStore.setUser(TraceUser(userId = "u1", name = "Old", phone = "01700000000"))
        }

        var resultUser: TraceUser? = null
        var resultError: TraceError? = null

        BarikoiTrace.updateUserName("Bob", object : BarikoiTrace.TraceUserCallback {
            override fun onSuccess(user: TraceUser) {
                resultUser = user
            }

            override fun onFailure(error: TraceError) {
                resultError = error
            }
        })

        Shadows.shadowOf(Looper.getMainLooper()).idle()

        assertThat(resultError).isNull()
        assertThat(resultUser).isNotNull()
        assertThat(resultUser?.name).isEqualTo("Bob")
    }

    @Test
    fun updateUserName_callback_failureOnBlank() {
        runBlocking {
            dataStore.setUser(TraceUser(userId = "u1", name = "Old", phone = "01700000000"))
        }

        var resultUser: TraceUser? = null
        var resultError: TraceError? = null

        BarikoiTrace.updateUserName("   ", object : BarikoiTrace.TraceUserCallback {
            override fun onSuccess(user: TraceUser) {
                resultUser = user
            }

            override fun onFailure(error: TraceError) {
                resultError = error
            }
        })

        Shadows.shadowOf(Looper.getMainLooper()).idle()

        assertThat(resultUser).isNull()
        assertThat(resultError).isNotNull()
        assertThat(resultError?.code).isEqualTo(TraceError.noDataError().code)
    }

    @Test
    fun logout_clearsUserDataAndTrip() = runBlocking {
        dataStore.setUser(TraceUser(userId = "u1", name = "Alice", phone = "01700000000"))
        dataStore.setLocalTripId("trip-101")
        dataStore.setSdkTracking(true)

        BarikoiTrace.logout()

        assertThat(BarikoiTrace.getUser()).isNull()
        assertThat(BarikoiTrace.getUserId()).isNull()
        assertThat(BarikoiTrace.isOnTrip()).isFalse()
        assertThat(BarikoiTrace.getTripId()).isNull()
    }

    @Test
    fun startTracking_setsModeAndSource() = runBlocking {
        dataStore.setUser(TraceUser(userId = "u1", phone = "01700000000"))
        val mode = TraceMode.Builder().setUpdateInterval(15).build()

        BarikoiTrace.startTracking(mode, withTrip = false, source = TraceModeSource.REMOTE)
        assertThat(dataStore.getTraceModeSource()).isEqualTo(TraceModeSource.REMOTE)
        assertThat(dataStore.getUpdateInterval()).isEqualTo(15)

        BarikoiTrace.stopTracking()
        for (i in 1..50) {
            if (dataStore.getTraceModeSource() == TraceModeSource.LOCAL) break
            Thread.sleep(10)
        }
        assertThat(dataStore.getTraceModeSource()).isEqualTo(TraceModeSource.LOCAL)
    }

    @Test
    fun resetUrls_restoresDefaults() = runBlocking {
        dataStore.setBaseUrl("https://custom.api.com/")
        dataStore.setMqttUrl("tcp://custom.mqtt.com:1883")

        BarikoiTrace.resetUrls()

        for (i in 1..50) {
            if (dataStore.getBaseUrl() == null && dataStore.getMqttUrl() == null) break
            Thread.sleep(10)
        }

        assertThat(dataStore.getBaseUrl()).isNull()
        assertThat(dataStore.getMqttUrl()).isNull()
    }
}
