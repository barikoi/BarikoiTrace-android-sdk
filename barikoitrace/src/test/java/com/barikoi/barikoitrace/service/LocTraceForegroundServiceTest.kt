package com.barikoi.barikoitrace.service

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.barikoi.barikoitrace.BarikoiTrace
import com.barikoi.barikoitrace.TraceMode
import com.barikoi.barikoitrace.api.MqttManager
import com.barikoi.barikoitrace.storage.OfflineLocationDb
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
import io.mockk.justRun
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadows.ShadowService

@RunWith(RobolectricTestRunner::class)
class LocTraceForegroundServiceTest {

    @Test
    fun disabledOfflineTracking_dropsDisconnectedLocations() {
        assertOfflineStorage(enabled = false)
    }

    @Test
    fun enabledOfflineTracking_savesDisconnectedLocations() {
        assertOfflineStorage(enabled = true)
    }

    @Test
    fun remoteOfflineSetting_preventsDisconnectedStorage() {
        assertOfflineStorage(enabled = false, remote = true)
    }

    @Test
    fun disabledOfflineTracking_stillPublishesConnectedLocations() {
        assertOfflineStorage(enabled = false, connected = true)
    }

    private fun assertOfflineStorage(enabled: Boolean, remote: Boolean = false, connected: Boolean = false) = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dataStore = TraceDataStore(context)
        dataStore.clearLocalTrip()
        dataStore.setBroadcasting(false)
        dataStore.setTraceMode(TraceMode.ACTIVE)
        if (remote) dataStore.setTraceModeFromRemote(TraceMode.ACTIVE.copy(offline = enabled))
        else dataStore.setOfflineTracking(enabled)
        val db = OfflineLocationDb.getInstance(context)
        db.locationDao().deleteAll()
        val controller = Robolectric.buildService(LocTraceForegroundService::class.java).create()
        val service = controller.get()
        fun setField(name: String, value: Any) {
            LocTraceForegroundService::class.java.getDeclaredField(name).apply { isAccessible = true }.set(service, value)
        }
        setField("dataStore", dataStore)
        setField("offlineDb", db)
        val mqtt = mockk<MqttManager>(relaxed = true)
        every { mqtt.isConnected() } returns connected
        justRun { mqtt.publishLocation(any(), any(), any()) }
        setField("mqttManager", mqtt)
        val scope = LocTraceForegroundService::class.java.getDeclaredField("serviceScope")
            .apply { isAccessible = true }.get(service) as CoroutineScope
        try {
            val location = Location("test").apply {
                latitude = 23.79
                longitude = 90.40
                accuracy = 5f
                time = System.currentTimeMillis()
            }
            service.onLocationReceived(location)
            scope.coroutineContext[Job]!!.children.toList().joinAll()
            assertThat(db.locationDao().getCount()).isEqualTo(if (enabled && !connected) 1 else 0)
            verify(exactly = if (connected) 1 else 0) { mqtt.publishLocation(location, any(), any()) }
        } finally {
            controller.destroy()
        }
    }

    @Test
    fun repeatedStarts_replaceSubscriptionAndDestroyRemovesLastCallback() {
        assertSingleLocationSubscription(initialAction = null)
    }

    @Test
    fun normalStartAfterModeChangedAction_reusesEngineAndRemovesPreviousCallback() {
        assertSingleLocationSubscription(initialAction = LocTraceForegroundService.ACTION_TRACE_MODE_CHANGED)
    }

    @Test
    fun modeChangedAction_replacesActiveRequestWithDistanceBasedModeAndKeepsTrip() {
        assertSingleLocationSubscription(initialAction = null, replacementMode = TraceMode.PASSIVE)
    }

    private fun assertSingleLocationSubscription(initialAction: String?, replacementMode: TraceMode? = null) = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dataStore = TraceDataStore(context)
        dataStore.clearUser()
        dataStore.clearLocalTrip()
        dataStore.setTraceMode(TraceMode.ACTIVE)
        val fusedClient = mockk<FusedLocationProviderClient>()
        val activeCallbacks = mutableListOf<LocationCallback>()
        val requests = mutableListOf<LocationRequest>()
        var registrations = 0
        every {
            fusedClient.requestLocationUpdates(any<LocationRequest>(), any<LocationCallback>(), any<Looper>())
        } answers {
            // Every replacement must unregister its predecessor first.
            assertThat(activeCallbacks).isEmpty()
            activeCallbacks.add(secondArg())
            requests.add(firstArg())
            registrations++
            Tasks.forResult<Void>(null)
        }
        every { fusedClient.removeLocationUpdates(any<LocationCallback>()) } answers {
            assertThat(activeCallbacks.remove(firstArg<LocationCallback>())).isTrue()
            Tasks.forResult<Void>(null)
        }
        mockkStatic(LocationServices::class) {
            every { LocationServices.getFusedLocationProviderClient(any<Context>()) } returns fusedClient
            mockkObject(SystemSettingsManager) {
                every { SystemSettingsManager.checkPermissions(any()) } returns true
                val controller = Robolectric.buildService(LocTraceForegroundService::class.java).create()
                val service = controller.get()
                try {
                    service.onStartCommand(Intent(service, LocTraceForegroundService::class.java).apply {
                        action = initialAction
                    }, 0, 1)
                    val nextIntent = Intent(service, LocTraceForegroundService::class.java)
                    if (replacementMode != null) {
                        dataStore.setLocalTripId("ongoing-trip")
                        dataStore.setTraceMode(replacementMode)
                        nextIntent.action = LocTraceForegroundService.ACTION_TRACE_MODE_CHANGED
                    }
                    service.onStartCommand(nextIntent, 0, 2)
                    assertThat(registrations).isEqualTo(2)
                    assertThat(activeCallbacks).hasSize(1)
                    if (replacementMode != null) {
                        assertThat(requests.first().minUpdateDistanceMeters).isEqualTo(0f)
                        assertThat(requests.last().minUpdateDistanceMeters).isEqualTo(100f)
                        assertThat(requests.last().priority).isEqualTo(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
                        assertThat(dataStore.getLocalTripId()).isEqualTo("ongoing-trip")
                    }
                } finally {
                    controller.destroy()
                }
                assertThat(activeCallbacks).isEmpty()
            }
        }
    }

    @Test
    fun onStartCommand_withModeChangedAction_returnsStickyWithoutCrashing() {
        val controller = Robolectric.buildService(LocTraceForegroundService::class.java).create()
        val service = controller.get()
        val intent = Intent(service, LocTraceForegroundService::class.java).apply {
            action = LocTraceForegroundService.ACTION_TRACE_MODE_CHANGED
        }

        val result = service.onStartCommand(intent, 0, 0)

        assertThat(result).isEqualTo(Service.START_STICKY)
        controller.destroy()
    }

    @Test
    fun onDestroy_withoutStartCommand_doesNotThrow() {
        val controller = Robolectric.buildService(LocTraceForegroundService::class.java).create()

        controller.destroy()
    }

    @Test
    fun onDestroy_afterUpdateUserNameEarlyReturn_doesNotThrow() {
        val controller = Robolectric.buildService(LocTraceForegroundService::class.java).create()
        val service = controller.get()
        val intent = Intent(service, LocTraceForegroundService::class.java).apply {
            action = LocTraceForegroundService.ACTION_UPDATE_USER_NAME
        }

        service.onStartCommand(intent, 0, 0)

        controller.destroy()
    }

    @Test
    @Config(sdk = [34], shadows = [DenyingForegroundShadowService::class])
    fun onCreate_whenForegroundStartDenied_stopsSelfWithoutCrashing() {
        val dataStore = TraceDataStore(ApplicationProvider.getApplicationContext<Context>())
        val wasLogging = dataStore.isLogging()
        runBlocking { dataStore.setLogging(true) }
        val levels = mutableListOf<String>()
        BarikoiTrace.setLogListener(object : BarikoiTrace.TraceLogListener {
            override fun onLog(level: String, tag: String, message: String) {
                levels.add(level)
            }
        })
        try {
            val controller = Robolectric.buildService(LocTraceForegroundService::class.java).create()

            assertThat(Shadows.shadowOf(controller.get()).isStoppedBySelf()).isTrue()
            assertThat(levels).contains("ERROR")
        } finally {
            BarikoiTrace.setLogListener(null)
            runBlocking { dataStore.setLogging(wasLogging) }
        }
    }

    @Test
    @Config(sdk = [34], shadows = [DenyingForegroundShadowService::class])
    fun onStartCommand_whenForegroundStartDenied_returnsNotStickyAndStopsSelf() {
        val controller = Robolectric.buildService(LocTraceForegroundService::class.java).create()
        val service = controller.get()

        val result = service.onStartCommand(Intent(service, LocTraceForegroundService::class.java), 0, 0)

        assertThat(result).isEqualTo(Service.START_NOT_STICKY)
        assertThat(Shadows.shadowOf(service).isStoppedBySelf()).isTrue()
    }

    @Test
    @Config(sdk = [34], shadows = [DenyingForegroundShadowService::class])
    fun onStartCommand_whenPermissionRevokedAfterCreation_stopsBeforeHandlingAction() {
        DenyingForegroundShadowService.failure = null
        try {
            val controller = Robolectric.buildService(LocTraceForegroundService::class.java).create()
            val service = controller.get()
            DenyingForegroundShadowService.failure = SecurityException("Location permission revoked")
            val intent = Intent(service, LocTraceForegroundService::class.java).apply {
                action = LocTraceForegroundService.ACTION_TRACE_MODE_CHANGED
            }

            assertThat(service.onStartCommand(intent, 0, 0)).isEqualTo(Service.START_NOT_STICKY)
            assertThat(Shadows.shadowOf(service).isStoppedBySelf()).isTrue()
            controller.destroy()
        } finally {
            DenyingForegroundShadowService.failure = IllegalStateException("Foreground start denied")
        }
    }
}

/**
 * Simulates the Android 12+ foreground-service background-start restriction:
 * both startForeground overloads throw, like ForegroundServiceStartNotAllowedException
 * (an IllegalStateException subclass) does on a denied promotion.
 */
@Implements(android.app.Service::class)
class DenyingForegroundShadowService : ShadowService() {

    companion object {
        var failure: RuntimeException? = IllegalStateException("Foreground start denied")
    }

    @Implementation
    override fun startForeground(id: Int, notification: Notification?) {
        failure?.let { throw it }
    }

    @Implementation(minSdk = Build.VERSION_CODES.Q)
    override fun startForeground(id: Int, notification: Notification?, foregroundServiceType: Int) {
        failure?.let { throw it }
    }
}
