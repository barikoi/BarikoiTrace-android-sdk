package com.barikoi.barikoiloctrace.storage

import androidx.test.core.app.ApplicationProvider
import com.barikoi.barikoiloctrace.TraceMode
import com.barikoi.barikoiloctrace.model.TraceUser
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalTime

@RunWith(RobolectricTestRunner::class)
class TraceDataStoreTest {

    private lateinit var dataStore: TraceDataStore

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        dataStore = TraceDataStore(context)
    }

    @Test
    fun setApiKey_andGetApiKey_roundTrip() = runBlocking {
        dataStore.setApiKey("test-api-key")
        assertThat(dataStore.getApiKey()).isEqualTo("test-api-key")
    }

    @Test
    fun setUser_andGetUser_roundTrip() = runBlocking {
        val user = TraceUser(
            userId = "u123",
            name = "Test User",
            email = "test@test.com",
            phone = "01700000000",
            companyId = "c1",
            group = "g1"
        )
        dataStore.setUser(user)
        val retrieved = dataStore.getUser()
        assertThat(retrieved).isNotNull()
        assertThat(retrieved!!.userId).isEqualTo("u123")
        assertThat(retrieved.name).isEqualTo("Test User")
        assertThat(retrieved.email).isEqualTo("test@test.com")
        assertThat(retrieved.phone).isEqualTo("01700000000")
        assertThat(retrieved.companyId).isEqualTo("c1")
        assertThat(retrieved.group).isEqualTo("g1")
    }

    @Test
    fun clearUser_removesUserData() = runBlocking {
        dataStore.setUser(TraceUser(userId = "u123", phone = "01700000000"))
        dataStore.clearUser()
        assertThat(dataStore.getUser()).isNull()
        assertThat(dataStore.getUserId()).isNull()
    }

    @Test
    fun setLocalTripId_andGetLocalTripId_roundTrip() = runBlocking {
        dataStore.setLocalTripId("trip-abc")
        assertThat(dataStore.getLocalTripId()).isEqualTo("trip-abc")
    }

    @Test
    fun clearLocalTrip_removesTripId() = runBlocking {
        dataStore.setLocalTripId("trip-abc")
        dataStore.clearLocalTrip()
        assertThat(dataStore.getLocalTripId()).isNull()
    }

    @Test
    fun setLocalTripId_null_removesTripId() = runBlocking {
        dataStore.setLocalTripId("trip-abc")
        dataStore.setLocalTripId(null)
        assertThat(dataStore.getLocalTripId()).isNull()
    }

    @Test
    fun setSdkTracking_andIsSdkTracking_roundTrip() = runBlocking {
        dataStore.setSdkTracking(true)
        assertThat(dataStore.isSdkTracking()).isTrue()
        dataStore.setSdkTracking(false)
        assertThat(dataStore.isSdkTracking()).isFalse()
    }

    @Test
    fun setTraceMode_andGetTraceMode_preservesAllFields() = runBlocking {
        val mode = TraceMode.Builder()
            .setUpdateInterval(10)
            .setDistanceFilter(50)
            .setAccuracyFilter(100)
            .setOfflineSync(true)
            .setDesiredAccuracy(TraceMode.DesiredAccuracy.HIGH)
            .setStartTime(LocalTime.of(8, 0))
            .setEndTime(LocalTime.of(18, 0))
            .build()
        dataStore.setTraceModeWithTiming(mode)
        val retrieved = dataStore.getTraceMode()
        assertThat(retrieved).isNotNull()
        assertThat(retrieved!!.updateInterval).isEqualTo(10)
        assertThat(retrieved.distanceFilter).isEqualTo(50)
        assertThat(retrieved.accuracyFilter).isEqualTo(100)
        assertThat(retrieved.desiredAccuracy).isEqualTo(TraceMode.DesiredAccuracy.HIGH)
        assertThat(retrieved.startTime).isEqualTo(LocalTime.of(8, 0))
        assertThat(retrieved.endTime).isEqualTo(LocalTime.of(18, 0))
    }

    @Test
    fun setBaseUrl_andGetBaseUrl_roundTrip() = runBlocking {
        dataStore.setBaseUrl("https://api.example.com/")
        assertThat(dataStore.getBaseUrl()).isEqualTo("https://api.example.com/")
    }

    @Test
    fun resetUrls_clearsBaseUrlAndMqttUrl() = runBlocking {
        dataStore.setBaseUrl("https://api.example.com/")
        dataStore.setMqttUrl("tcp://mqtt.example.com:1883")
        dataStore.resetUrls()
        assertThat(dataStore.getBaseUrl()).isNull()
        assertThat(dataStore.getMqttUrl()).isNull()
    }

    @Test
    fun setDeviceToken_andGetDeviceToken_roundTrip() = runBlocking {
        dataStore.setDeviceToken("device-uuid-123")
        assertThat(dataStore.getDeviceToken()).isEqualTo("device-uuid-123")
    }

    @Test
    fun setLogging_andIsLogging_roundTrip() = runBlocking {
        dataStore.setLogging(true)
        assertThat(dataStore.isLogging()).isTrue()
        dataStore.setLogging(false)
        assertThat(dataStore.isLogging()).isFalse()
    }
}
