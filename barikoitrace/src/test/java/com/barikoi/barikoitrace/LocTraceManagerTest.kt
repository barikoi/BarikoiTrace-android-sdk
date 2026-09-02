package com.barikoi.barikoitrace

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LocTraceManagerTest {

    private lateinit var manager: LocTraceManager
    private lateinit var dataStore: com.barikoi.barikoitrace.storage.TraceDataStore

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        manager = LocTraceManager.getInstance(context)
        dataStore = com.barikoi.barikoitrace.storage.TraceDataStore(context)
    }

    @Test
    fun isOnTrip_returnsFalseWhenNoTripId() = runBlocking {
        dataStore.clearLocalTrip()
        assertThat(manager.isOnTrip()).isFalse()
    }

    @Test
    fun isOnTrip_returnsTrueWhenTripIdSet() = runBlocking {
        dataStore.setLocalTripId("trip-123")
        assertThat(manager.isOnTrip()).isTrue()
        dataStore.clearLocalTrip()
    }

    @Test
    fun getTripId_returnsStoredValue() = runBlocking {
        dataStore.setLocalTripId("trip-456")
        assertThat(manager.getTripId()).isEqualTo("trip-456")
        dataStore.clearLocalTrip()
    }

    @Test
    fun getCurrentTrip_returnsStoredValue() = runBlocking {
        dataStore.setLocalTripId("trip-789")
        assertThat(manager.getCurrentTrip()).isEqualTo("trip-789")
        dataStore.clearLocalTrip()
    }

    @Test
    fun getCurrentTrip_returnsNullWhenNoTrip() = runBlocking {
        dataStore.clearLocalTrip()
        assertThat(manager.getCurrentTrip()).isNull()
    }

    @Test
    fun getUser_returnsNullWhenNoUserSet() {
        runBlocking { dataStore.clearUser() }
        assertThat(manager.getUser()).isNull()
    }

    @Test
    fun getUser_returnsStoredUser() = runBlocking {
        val user = com.barikoi.barikoitrace.model.TraceUser(
            userId = "u123",
            name = "Test",
            phone = "01700000000"
        )
        dataStore.setUser(user)
        val retrieved = manager.getUser()
        assertThat(retrieved).isNotNull()
        assertThat(retrieved!!.userId).isEqualTo("u123")
        assertThat(retrieved.name).isEqualTo("Test")
    }

    @Test
    fun getUserId_returnsStoredUserId() = runBlocking {
        dataStore.setUser(
            com.barikoi.barikoitrace.model.TraceUser(
                userId = "user-abc",
                phone = "01700000000"
            )
        )
        assertThat(manager.getUserId()).isEqualTo("user-abc")
    }

    @Test
    fun setTraceMode_storesAndRetrievesMode() = runBlocking {
        val mode = TraceMode.Builder().setUpdateInterval(10).build()
        dataStore.setTraceMode(mode)
        val retrieved = dataStore.getTraceMode()
        assertThat(retrieved).isNotNull()
        assertThat(retrieved!!.updateInterval).isEqualTo(10)
    }

    @Test
    fun stopTracking_clearsSdkTrackingFlag() = runBlocking {
        dataStore.setSdkTracking(true)
        assertThat(dataStore.isSdkTracking()).isTrue()
        dataStore.stopSdkTracking()
        assertThat(dataStore.isSdkTracking()).isFalse()
    }

    @Test
    fun updateUserName_noUserSet_throwsException() {
        runBlocking { dataStore.clearUser() }
        var thrown: Exception? = null
        try {
            runBlocking { manager.updateUserName("New") }
        } catch (e: Exception) {
            thrown = e
        }
        assertThat(thrown).isNotNull()
        assertThat(thrown!!.message)
            .isEqualTo(com.barikoi.barikoitrace.model.TraceError.noUserError().message)
    }

    @Test
    fun updateUserName_blankName_throwsException() = runBlocking {
        dataStore.setUser(
            com.barikoi.barikoitrace.model.TraceUser(
                userId = "u123",
                name = "Old",
                phone = "01700000000"
            )
        )
        var thrown: Exception? = null
        try {
            manager.updateUserName("   ")
        } catch (e: Exception) {
            thrown = e
        }
        assertThat(thrown).isNotNull()
        assertThat(thrown!!.message)
            .isEqualTo(com.barikoi.barikoitrace.model.TraceError.noDataError().message)
    }

    @Test
    fun updateUserName_returnsUpdatedUser() = runBlocking {
        dataStore.setUser(
            com.barikoi.barikoitrace.model.TraceUser(
                userId = "u123",
                name = "Old",
                phone = "01700000000"
            )
        )
        val updated = manager.updateUserName("New")
        assertThat(updated.name).isEqualTo("New")
        assertThat(manager.getUser()!!.name).isEqualTo("New")
    }
}
