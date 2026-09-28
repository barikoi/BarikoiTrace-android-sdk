package com.barikoi.barikoitrace

import android.Manifest
import android.app.Application
import android.content.Context
import android.location.LocationManager
import androidx.test.core.app.ApplicationProvider
import com.barikoi.barikoitrace.model.TraceUser
import com.barikoi.barikoitrace.service.LocTraceForegroundService
import com.barikoi.barikoitrace.storage.OfflineLocationDb
import com.barikoi.barikoitrace.storage.OfflineLocationEntity
import com.barikoi.barikoitrace.storage.TraceDataStore
import com.barikoi.barikoitrace.util.NetworkChecker
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockkObject
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class LocTraceManagerTest {

    private lateinit var manager: LocTraceManager
    private lateinit var dataStore: com.barikoi.barikoitrace.storage.TraceDataStore

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        manager = LocTraceManager.getInstance(context)
        dataStore = TraceDataStore(context)

        Shadows.shadowOf(context as android.app.Application).grantPermissions(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        Shadows.shadowOf(locationManager).setProviderEnabled(LocationManager.GPS_PROVIDER, true)
        Shadows.shadowOf(locationManager).setProviderEnabled(LocationManager.NETWORK_PROVIDER, true)

        // Default test fetcher: fail loudly if an unexpected remote fetch happens
        manager.settingsFetcher = { throw IOException("unexpected settings fetch") }
        // Default test authenticator: fail loudly if an unexpected auth call happens
        manager.authenticator = { _, _, _ -> throw IOException("unexpected authenticate") }
    }

    private fun waitFor(timeoutMs: Long = 5000, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (condition()) return
            Thread.sleep(20)
        }
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

    // --- TraceModeSource ---

    @Test
    fun startTracking_default_writesLocalSourceAndMode() = runBlocking {
        dataStore.setUser(TraceUser(userId = "u1", phone = "01700000000"))
        manager.startTracking(TraceMode.Builder().setUpdateInterval(7).build())
        assertThat(dataStore.getTraceModeSource()).isEqualTo(TraceModeSource.LOCAL)
        assertThat(dataStore.getUpdateInterval()).isEqualTo(7)
        dataStore.stopSdkTracking()
    }

    @Test
    fun startTracking_remote_withoutStoredRemote_startsOnPassedFallbackThenAppliesRemote() {
        runBlocking {
            dataStore.setUser(TraceUser(userId = "u1", phone = "01700000000"))
            dataStore.setTraceMode(TraceMode.Builder().setUpdateInterval(20).build())
        }
        val fetchStarted = CountDownLatch(1)
        val finishFetch = CountDownLatch(1)
        manager.settingsFetcher = {
            fetchStarted.countDown()
            finishFetch.await()
            TraceMode.Builder().setUpdateInterval(99).build()
        }
        try {
            manager.startTracking(
                TraceMode.Builder().setUpdateInterval(7).build(),
                false,
                TraceModeSource.REMOTE
            )
            assertThat(dataStore.getTraceModeSource()).isEqualTo(TraceModeSource.REMOTE)
            assertThat(dataStore.getUpdateInterval()).isEqualTo(7)
            assertThat(fetchStarted.await(5, TimeUnit.SECONDS)).isTrue()

            finishFetch.countDown()
            waitFor { dataStore.getUpdateInterval() == 99 && dataStore.isTraceModeFromRemote() }
            assertThat(dataStore.getUpdateInterval()).isEqualTo(99)
            assertThat(dataStore.isTraceModeFromRemote()).isTrue()
        } finally {
            finishFetch.countDown()
            runBlocking { dataStore.stopSdkTracking() }
        }
    }

    @Test
    fun startTracking_remote_withStoredRemote_keepsStoredMode() = runBlocking {
        dataStore.setUser(TraceUser(userId = "u1", phone = "01700000000"))
        dataStore.setTraceModeFromRemote(TraceMode.Builder().setUpdateInterval(30).build())
        manager.startTracking(
            TraceMode.Builder().setUpdateInterval(7).build(),
            false,
            TraceModeSource.REMOTE
        )
        assertThat(dataStore.getUpdateInterval()).isEqualTo(30)
        assertThat(dataStore.isTraceModeFromRemote()).isTrue()
        dataStore.stopSdkTracking()
    }

    @Test
    fun startTracking_remote_fetchFailure_keepsFallback() = runBlocking {
        dataStore.setUser(TraceUser(userId = "u1", phone = "01700000000"))
        dataStore.setTraceMode(TraceMode.Builder().setUpdateInterval(20).build())
        val attempted = CountDownLatch(1)
        manager.settingsFetcher = {
            attempted.countDown()
            throw IOException("offline")
        }
        manager.startTracking(
            TraceMode.Builder().setUpdateInterval(7).build(),
            false,
            TraceModeSource.REMOTE
        )
        assertThat(attempted.await(5, TimeUnit.SECONDS)).isTrue()
        assertThat(dataStore.getUpdateInterval()).isEqualTo(7)
        assertThat(dataStore.isTraceModeFromRemote()).isFalse()
        dataStore.stopSdkTracking()
    }

    @Test
    fun fetchAndApplyRemoteSettings_success_storesRemoteMode() = runBlocking {
        dataStore.setUser(TraceUser(userId = "u1", phone = "01700000000"))
        dataStore.setTraceMode(TraceMode.Builder().setUpdateInterval(7).build())
        manager.settingsFetcher = { TraceMode.Builder().setUpdateInterval(42).build() }
        manager.fetchAndApplyRemoteSettings()
        assertThat(dataStore.getUpdateInterval()).isEqualTo(42)
        assertThat(dataStore.isTraceModeFromRemote()).isTrue()
    }

    @Test
    fun fetchAndApplyRemoteSettings_failure_keepsExistingMode() = runBlocking {
        dataStore.setUser(TraceUser(userId = "u1", phone = "01700000000"))
        dataStore.setTraceMode(TraceMode.Builder().setUpdateInterval(7).build())
        manager.settingsFetcher = { throw IOException("offline") }
        manager.fetchAndApplyRemoteSettings()
        assertThat(dataStore.getUpdateInterval()).isEqualTo(7)
        assertThat(dataStore.isTraceModeFromRemote()).isFalse()
    }

    @Test
    fun fetchAndApplyRemoteSettings_noUser_returnsSilently() = runBlocking {
        dataStore.clearUser()
        var invoked = false
        manager.settingsFetcher = {
            invoked = true
            TraceMode.ACTIVE
        }
        manager.fetchAndApplyRemoteSettings()
        assertThat(invoked).isFalse()
    }

    @Test
    fun setOrCreateUser_localSource_skipsSettingsFetch() = runBlocking {
        dataStore.setApiKey("test-key")
        dataStore.setUser(TraceUser(userId = "u1", name = "Test", phone = "01700000000"))
        dataStore.setTraceMode(TraceMode.Builder().setUpdateInterval(7).build())
        dataStore.setTraceModeSource(TraceModeSource.LOCAL)
        var invoked = false
        manager.settingsFetcher = {
            invoked = true
            TraceMode.Builder().setUpdateInterval(42).build()
        }

        var user: TraceUser? = null
        mockkObject(NetworkChecker) {
            every { NetworkChecker.isNetworkAvailable(any()) } returns true
            user = manager.setOrCreateUser("Test", null, "01700000000")
        }

        assertThat(user!!.userId).isEqualTo("u1")
        assertThat(invoked).isFalse()
        assertThat(dataStore.getUpdateInterval()).isEqualTo(7)
        assertThat(dataStore.isTraceModeFromRemote()).isFalse()
    }

    @Test
    fun setOrCreateUser_remoteSource_fetchesAndStoresRemote() = runBlocking {
        dataStore.setApiKey("test-key")
        dataStore.setUser(TraceUser(userId = "u1", name = "Test", phone = "01700000000"))
        dataStore.setTraceModeSource(TraceModeSource.REMOTE)
        manager.settingsFetcher = { TraceMode.Builder().setUpdateInterval(42).build() }

        mockkObject(NetworkChecker) {
            every { NetworkChecker.isNetworkAvailable(any()) } returns true
            manager.setOrCreateUser("Test", null, "01700000000")
        }

        assertThat(dataStore.getUpdateInterval()).isEqualTo(42)
        assertThat(dataStore.isTraceModeFromRemote()).isTrue()
    }

    @Test
    fun getSettingsFromRemote_localSource_returnsWithoutStoring() = runBlocking {
        dataStore.setUser(TraceUser(userId = "u1", phone = "01700000000"))
        dataStore.setTraceMode(TraceMode.Builder().setUpdateInterval(7).build())
        dataStore.setTraceModeSource(TraceModeSource.LOCAL)
        manager.settingsFetcher = { TraceMode.Builder().setUpdateInterval(42).build() }

        val mode = manager.getSettingsFromRemote()

        assertThat(mode.updateInterval).isEqualTo(42)
        assertThat(dataStore.getUpdateInterval()).isEqualTo(7)
        assertThat(dataStore.isTraceModeFromRemote()).isFalse()
    }

    @Test
    fun getSettingsFromRemote_remoteSource_storesAndReturns() = runBlocking {
        dataStore.setUser(TraceUser(userId = "u1", phone = "01700000000"))
        dataStore.setTraceModeSource(TraceModeSource.REMOTE)
        manager.settingsFetcher = { TraceMode.Builder().setUpdateInterval(42).build() }

        val mode = manager.getSettingsFromRemote()

        assertThat(mode.updateInterval).isEqualTo(42)
        assertThat(dataStore.getUpdateInterval()).isEqualTo(42)
        assertThat(dataStore.isTraceModeFromRemote()).isTrue()
    }

    // --- User switch & logout ---

    private fun seedOfflineRows(vararg rows: String) = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dao = OfflineLocationDb.getInstance(context).locationDao()
        dao.deleteAll()
        rows.forEach { OfflineLocationDb.getInstance(context).locationDao().insert(OfflineLocationEntity(json = it)) }
    }

    private fun offlineRowCount(): Int = runBlocking {
        OfflineLocationDb.getInstance(ApplicationProvider.getApplicationContext<Context>())
            .locationDao().getCount()
    }

    @Test
    fun setOrCreateUser_switchWhileRunning_wipesOfflineDataAndRestartsSession() = runBlocking {
        seedOfflineRows("{\"latitude\":23.0}", "{\"latitude\":23.1}")

        dataStore.setApiKey("test-key")
        dataStore.setUser(TraceUser(userId = "u1", name = "A", phone = "01700000000"))
        dataStore.setTraceMode(TraceMode.Builder().setUpdateInterval(10).build())
        dataStore.setTraceModeSource(TraceModeSource.LOCAL)
        dataStore.setSdkTracking(true)
        manager.authenticator = { _, _, phone ->
            val user = TraceUser(userId = "u2", name = "B", phone = phone)
            dataStore.setUser(user)
            user
        }
        // Simulate the foreground service running
        manager.serviceRunningChecker = { true }

        mockkObject(NetworkChecker) {
            every { NetworkChecker.isNetworkAvailable(any()) } returns true
            manager.setOrCreateUser("B", null, "01800000000")
        }

        assertThat(dataStore.getUserId()).isEqualTo("u2")
        assertThat(offlineRowCount()).isEqualTo(0)

        // The tracking session was restarted: the running service got a stop
        // (refreshTracking stops then starts it for the new user)
        val shadowApp = Shadows.shadowOf(
            ApplicationProvider.getApplicationContext<Context>() as Application
        )
        assertThat(shadowApp.getNextStoppedService()?.component?.className)
            .isEqualTo(LocTraceForegroundService::class.java.name)
    }

    @Test
    fun setOrCreateUser_switchWhileNotRunning_wipesOfflineDataWithoutRestart() = runBlocking {
        seedOfflineRows("{\"latitude\":23.0}")

        dataStore.setApiKey("test-key")
        dataStore.setUser(TraceUser(userId = "u1", name = "A", phone = "01700000000"))
        dataStore.setTraceMode(TraceMode.Builder().setUpdateInterval(10).build())
        dataStore.setTraceModeSource(TraceModeSource.LOCAL)
        manager.authenticator = { _, _, phone ->
            val user = TraceUser(userId = "u2", name = "B", phone = phone)
            dataStore.setUser(user)
            user
        }

        mockkObject(NetworkChecker) {
            every { NetworkChecker.isNetworkAvailable(any()) } returns true
            manager.setOrCreateUser("B", null, "01800000000")
        }

        assertThat(dataStore.getUserId()).isEqualTo("u2")
        assertThat(offlineRowCount()).isEqualTo(0)

        // Nothing was running, so no service restart was issued
        val shadowApp = Shadows.shadowOf(
            ApplicationProvider.getApplicationContext<Context>() as Application
        )
        assertThat(shadowApp.peekNextStartedService()).isNull()
        assertThat(shadowApp.getNextStoppedService()).isNull()
    }

    @Test
    fun setOrCreateUser_samePhoneCacheHit_keepsOfflineData() = runBlocking {
        seedOfflineRows("{\"latitude\":23.0}")

        dataStore.setApiKey("test-key")
        dataStore.setUser(TraceUser(userId = "u1", name = "A", phone = "01700000000"))
        dataStore.setTraceModeSource(TraceModeSource.LOCAL)

        mockkObject(NetworkChecker) {
            every { NetworkChecker.isNetworkAvailable(any()) } returns true
            val user = manager.setOrCreateUser("A", null, "01700000000")
            assertThat(user.userId).isEqualTo("u1")
        }

        assertThat(offlineRowCount()).isEqualTo(1)
    }

    @Test
    fun logout_clearsUserTripOfflineDataAndTrackingFlag() = runBlocking {
        seedOfflineRows("{}", "{}")

        dataStore.setApiKey("test-key")
        dataStore.setUser(TraceUser(userId = "u1", name = "A", phone = "01700000000"))
        dataStore.setLocalTripId("trip-1")
        dataStore.setSdkTracking(true)
        dataStore.setTraceMode(TraceMode.Builder().setUpdateInterval(10).build())

        manager.logout()

        assertThat(dataStore.getUser()).isNull()
        assertThat(dataStore.getLocalTripId()).isNull()
        assertThat(dataStore.isSdkTracking()).isFalse()
        assertThat(offlineRowCount()).isEqualTo(0)
        // App-level config survives logout
        assertThat(dataStore.getApiKey()).isEqualTo("test-key")
    }
}
