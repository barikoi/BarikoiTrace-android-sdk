package com.barikoi.barikoitrace

import android.Manifest
import android.app.Application
import android.content.Context
import android.location.LocationManager
import android.location.Location
import androidx.test.core.app.ApplicationProvider
import com.barikoi.barikoitrace.api.TraceApiClient
import com.barikoi.barikoitrace.api.TraceApiService
import com.barikoi.barikoitrace.location.LocationEngine
import com.barikoi.barikoitrace.model.TraceUser
import com.barikoi.barikoitrace.service.LocTraceForegroundService
import com.barikoi.barikoitrace.storage.OfflineLocationDb
import com.barikoi.barikoitrace.storage.OfflineLocationEntity
import com.barikoi.barikoitrace.storage.TraceDataStore
import com.barikoi.barikoitrace.util.NetworkChecker
import com.google.common.truth.Truth.assertThat
import com.google.gson.JsonParser
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.every
import io.mockk.mockkObject
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.async
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import retrofit2.Response
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
    fun authenticateImmediatelyAfterInitialize_waitsForApiClientReadiness() {
        assertAuthenticationWaitsForInitialization(previousKey = "", keys = listOf("new-key"))
    }

    @Test
    fun authenticateDuringReinitialization_usesNewKeyInsteadOfPersistedKey() {
        assertAuthenticationWaitsForInitialization(previousKey = "old-key", keys = listOf("new-key"))
    }

    @Test
    fun queuedInitializations_applyInOrderAndAuthenticateWithLatestKey() {
        assertAuthenticationWaitsForInitialization(previousKey = "old-key", keys = listOf("first-key", "latest-key"))
    }

    @Test
    fun failedInitialization_isReportedAndCanBeRetried() = runBlocking {
        dataStore.clearUser()
        dataStore.stopSdkTracking()
        val apiClient = TraceApiClient.getInstance(ApplicationProvider.getApplicationContext<Context>())
        manager.authenticator = { _, _, phone -> TraceUser(userId = "u1", phone = phone) }
        mockkObject(NetworkChecker) {
            every { NetworkChecker.isNetworkAvailable(any()) } returns true
            mockkObject(apiClient) {
                every { apiClient.setApiKey("bad-key") } throws IOException("Initialization failed")
                manager.initialize("bad-key")
                try {
                    manager.setOrCreateUser("Test", null, "01700000000")
                    throw AssertionError("Expected initialization failure")
                } catch (e: IOException) {
                    assertThat(e.message).isEqualTo("Initialization failed")
                }
                manager.initialize("good-key")
                assertThat(manager.setOrCreateUser("Test", null, "01700000000").userId).isEqualTo("u1")
                assertThat(dataStore.getApiKey()).isEqualTo("good-key")
            }
        }
    }

    @Test
    fun initialize_preservesExplicitLoggingOptOut() = runBlocking {
        dataStore.stopSdkTracking()
        dataStore.clearUser()
        dataStore.setUser(TraceUser(userId = "u1", phone = "01700000000"))
        dataStore.setLogging(true)
        manager.setLoggingEnabled(false)
        mockkObject(NetworkChecker) {
            every { NetworkChecker.isNetworkAvailable(any()) } returns true
            manager.initialize("test-key")
            manager.setOrCreateUser(null, null, "01700000000")
        }
        assertThat(dataStore.isLogging()).isFalse()
    }

    private fun assertAuthenticationWaitsForInitialization(previousKey: String, keys: List<String>) = runBlocking {
        dataStore.clearUser()
        dataStore.stopSdkTracking()
        dataStore.setApiKey(previousKey)
        val apiClient = TraceApiClient.getInstance(ApplicationProvider.getApplicationContext<Context>())
        apiClient.setApiKey(previousKey)
        val serviceField = TraceApiClient::class.java.getDeclaredField("apiService").apply { isAccessible = true }
        val originalService = serviceField.get(apiClient)
        val apiService = mockk<TraceApiService>()
        serviceField.set(apiClient, apiService)
        var sentKey: String? = null
        coEvery { apiService.authenticate(any(), any()) } coAnswers {
            sentKey = secondArg<com.google.gson.JsonObject>().get("api_key").asString
            Response.success(JsonParser.parseString("""{
                "user":{"_id":"u1","companies":[{"company_id":"c1","group_id":"g1"}]}
            }""").asJsonObject)
        }
        manager.authenticator = { name, email, phone -> apiClient.authenticate(name, email, phone) }
        val initializationStarted = CountDownLatch(1)
        val allowInitialization = CountDownLatch(1)
        try {
            mockkObject(NetworkChecker) {
                every { NetworkChecker.isNetworkAvailable(any()) } returns true
                mockkObject(apiClient) {
                    every { apiClient.setApiKey(keys.first()) } answers {
                        initializationStarted.countDown()
                        check(allowInitialization.await(5, TimeUnit.SECONDS)) { "Initialization gate timed out" }
                        callOriginal()
                    }
                    manager.initialize(keys.first())
                    check(initializationStarted.await(5, TimeUnit.SECONDS)) { "Initialization did not start" }
                    keys.drop(1).forEach { manager.initialize(it) }
                    try {
                        val authentication = async(start = CoroutineStart.UNDISPATCHED) {
                            manager.setOrCreateUser("Test", null, "01700000000")
                        }
                        assertThat(authentication.isCompleted).isFalse()
                        assertThat(sentKey).isNull()
                        allowInitialization.countDown()
                        assertThat(authentication.await().userId).isEqualTo("u1")
                        assertThat(sentKey).isEqualTo(keys.last())
                        assertThat(dataStore.getApiKey()).isEqualTo(keys.last())
                        assertThat(dataStore.getDeviceToken()).isNotNull()
                    } finally {
                        allowInitialization.countDown()
                    }
                }
            }
        } finally {
            allowInitialization.countDown()
            serviceField.set(apiClient, originalService)
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
    fun setTraceModeWhileRunning_persistsModeAndSendsUpdateWithoutStoppingTrip() {
        assertRuntimeModeUpdate(isRunning = true)
    }

    @Test
    fun setTraceModeWhileStopped_savesModeWithoutStartingService() {
        assertRuntimeModeUpdate(isRunning = false)
    }

    private fun assertRuntimeModeUpdate(isRunning: Boolean) = runBlocking {
        dataStore.setTraceMode(TraceMode.ACTIVE)
        dataStore.setSdkTracking(isRunning)
        dataStore.setLocalTripId("ongoing-trip")
        manager.serviceRunningChecker = { isRunning }
        val newMode = TraceMode.PASSIVE.copy(distanceFilter = 250, accuracyFilter = 150)
        manager.setTraceMode(newMode)
        val scope = LocTraceManager::class.java.getDeclaredField("scope")
            .apply { isAccessible = true }.get(manager) as CoroutineScope
        scope.coroutineContext[Job]!!.children.toList().joinAll()
        val saved = dataStore.getTraceMode()
        assertThat(saved.updateInterval).isEqualTo(0)
        assertThat(saved.distanceFilter).isEqualTo(250)
        assertThat(saved.accuracyFilter).isEqualTo(150)
        assertThat(dataStore.getLocalTripId()).isEqualTo("ongoing-trip")
        assertThat(dataStore.isSdkTracking()).isEqualTo(isRunning)
        val shadowApp = Shadows.shadowOf(ApplicationProvider.getApplicationContext<Context>() as Application)
        val intent = shadowApp.nextStartedService
        if (isRunning) {
            assertThat(intent?.action).isEqualTo(LocTraceForegroundService.ACTION_TRACE_MODE_CHANGED)
            assertThat(intent?.component?.className).isEqualTo(LocTraceForegroundService::class.java.name)
        } else {
            assertThat(intent).isNull()
        }
        assertThat(shadowApp.nextStoppedService).isNull()
        assertThat(shadowApp.peekNextStartedService()).isNull()
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

    @Test
    fun configureEndpoints_validatesBothBeforeChangingUserOrConfiguration() = runBlocking {
        manager.serviceRunningChecker = { false }
        dataStore.setSdkTracking(false)
        dataStore.setBaseUrl("https://original.example/api/v1/")
        dataStore.setUser(TraceUser(userId = "original", phone = "01700000000"))
        try {
            manager.configureEndpoints("https://new.example/api/v1/", "tcp://user:secret@broker.example:1883")
            throw AssertionError("Expected invalid broker URL")
        } catch (_: IllegalArgumentException) {}
        assertThat(dataStore.getBaseUrl()).isEqualTo("https://original.example/api/v1/")
        assertThat(dataStore.getUserId()).isEqualTo("original")
    }

    @Test
    fun configureEndpoints_waitsForSavedConfigurationAndClearsUserForChangedBase() = runBlocking {
        manager.serviceRunningChecker = { false }
        dataStore.setSdkTracking(false)
        dataStore.setBaseUrl("https://original.example/api/v1/")
        dataStore.setUser(TraceUser(userId = "original", phone = "01700000000"))
        seedOfflineRows()
        manager.configureEndpoints(" https://new.example/api/v1 ", "tcp://broker.example:1883")
        val status = manager.getTrackingStatus()
        assertThat(status.baseUrl).isEqualTo("https://new.example/api/v1/")
        assertThat(status.mqttUrl).isEqualTo("tcp://broker.example:1883")
        assertThat(dataStore.getUser()).isNull()
    }

    @Test
    fun configureEndpoints_rejectsChangesWhileTracking() = runBlocking {
        manager.serviceRunningChecker = { true }
        val original = dataStore.getBaseUrl()
        try {
            manager.configureEndpoints("https://new.example/api/v1/", "tcp://broker.example:1883")
            throw AssertionError("Expected active tracking rejection")
        } catch (_: IllegalStateException) {}
        assertThat(dataStore.getBaseUrl()).isEqualTo(original)
    }

    @Test
    fun configureEndpoints_preservesQueuedDataAndRejectsMovingItToAnotherServer() = runBlocking {
        manager.serviceRunningChecker = { false }
        dataStore.setSdkTracking(false)
        dataStore.setBaseUrl("https://original.example/api/v1/")
        dataStore.setUser(TraceUser(userId = "original", phone = "01700000000"))
        seedOfflineRows("""{"user_id":"original"}""")
        try {
            manager.configureEndpoints("https://new.example/api/v1/", "tcp://broker.example:1883")
            throw AssertionError("Expected queued-data rejection")
        } catch (_: IllegalStateException) {}
        assertThat(dataStore.getBaseUrl()).isEqualTo("https://original.example/api/v1/")
        assertThat(dataStore.getUserId()).isEqualTo("original")
        assertThat(offlineRowCount()).isEqualTo(1)
    }

    @Test
    fun trackingStatus_reportsQueueEvenWhileServiceIsStopped() = runBlocking {
        manager.serviceRunningChecker = { false }
        dataStore.setSdkTracking(false)
        seedOfflineRows("{}", "{}")
        val status = manager.getTrackingStatus()
        assertThat(status.pendingLocationCount).isEqualTo(2)
        assertThat(status.serviceRunning).isFalse()
        assertThat(status.mqttConnected).isFalse()
    }

    @Test
    fun updateCurrentLocation_returnsLocationButOnlyQueuesWhenOfflineEnabled() = runBlocking {
        seedOfflineRows()
        val location = Location("test").apply { time = System.currentTimeMillis() }
        val engine = LocTraceManager::class.java.getDeclaredField("locationEngine")
            .apply { isAccessible = true }.get(manager) as LocationEngine
        mockkObject(engine) {
            coEvery { engine.getCurrentLocation() } returns location
            dataStore.setOfflineTracking(false)
            assertThat(manager.updateCurrentLocation()).isSameInstanceAs(location)
            assertThat(offlineRowCount()).isEqualTo(0)
            dataStore.setOfflineTracking(true)
            assertThat(manager.updateCurrentLocation()).isSameInstanceAs(location)
            assertThat(offlineRowCount()).isEqualTo(1)
        }
    }

    @Test
    fun failedUserSwitchWhileTracking_preservesOutgoingSessionAndOfflineData() {
        assertUnsuccessfulSwitchPreservesSession(wasTracking = true)
    }

    @Test
    fun failedUserSwitchWhileStopped_preservesOutgoingSessionAndOfflineData() {
        assertUnsuccessfulSwitchPreservesSession(wasTracking = false)
    }

    @Test
    fun cancelledUserSwitch_preservesOutgoingSessionAndOfflineData() {
        assertUnsuccessfulSwitchPreservesSession(wasTracking = true, cancel = true)
    }

    private fun assertUnsuccessfulSwitchPreservesSession(wasTracking: Boolean, cancel: Boolean = false) = runBlocking {
        seedOfflineRows("""{"user_id":"u1","latitude":23.0}""", """{"user_id":"u1","latitude":23.1}""")
        val dao = OfflineLocationDb.getInstance(ApplicationProvider.getApplicationContext<Context>()).locationDao()
        val originalRows = dao.getBatch()
        val outgoingUser = TraceUser(userId = "u1", name = "A", phone = "01700000000")
        dataStore.setApiKey("test-key")
        dataStore.clearUser()
        dataStore.setUser(outgoingUser)
        dataStore.setLocalTripId("outgoing-trip")
        dataStore.setSdkTracking(wasTracking)
        dataStore.setTraceMode(TraceMode.ACTIVE)
        dataStore.setTraceModeSource(TraceModeSource.REMOTE)
        manager.serviceRunningChecker = { wasTracking }
        val authStarted = CompletableDeferred<Unit>()
        val authResult = CompletableDeferred<TraceUser>()
        var fetchedSettings = false
        manager.settingsFetcher = {
            fetchedSettings = true
            TraceMode.ACTIVE
        }
        manager.authenticator = { _, _, _ ->
            authStarted.complete(Unit)
            authResult.await()
        }
        mockkObject(NetworkChecker) {
            every { NetworkChecker.isNetworkAvailable(any()) } returns true
            val switching = async {
                try {
                    manager.setOrCreateUser("B", null, "01800000000")
                    null
                } catch (e: IOException) {
                    e
                }
            }
            authStarted.await()
            assertThat(dao.getBatch()).isEqualTo(originalRows)
            assertThat(dataStore.getUser()).isEqualTo(outgoingUser)
            // The outgoing service can still collect locations during authentication.
            dao.insert(OfflineLocationEntity(json = """{"user_id":"u1","latitude":23.2}"""))
            val rowsBeforeFailure = dao.getBatch()
            if (cancel) {
                switching.cancelAndJoin()
            } else {
                authResult.completeExceptionally(IOException("Authentication failed"))
                assertThat(switching.await()?.message).isEqualTo("Authentication failed")
            }
            assertThat(dao.getBatch()).isEqualTo(rowsBeforeFailure)
            assertThat(dataStore.getUser()).isEqualTo(outgoingUser)
            assertThat(dataStore.getLocalTripId()).isEqualTo("outgoing-trip")
            assertThat(dataStore.isSdkTracking()).isEqualTo(wasTracking)
            assertThat(dataStore.getTraceModeSource()).isEqualTo(TraceModeSource.REMOTE)
            assertThat(fetchedSettings).isFalse()
            val shadowApp = Shadows.shadowOf(ApplicationProvider.getApplicationContext<Context>() as Application)
            assertThat(shadowApp.getNextStoppedService()).isNull()
            assertThat(shadowApp.peekNextStartedService()).isNull()
        }
    }

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
            assertThat(offlineRowCount()).isEqualTo(2)
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
