package com.barikoi.barikoitracesdkapp

import android.Manifest
import android.app.Application
import android.content.Context
import android.widget.Spinner
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.barikoi.barikoitrace.BarikoiTrace
import com.barikoi.barikoitrace.TraceMode
import com.barikoi.barikoitrace.TraceModeSource
import com.barikoi.barikoitrace.model.TraceStatus
import com.barikoi.barikoitrace.model.TraceUser
import com.barikoi.barikoitrace.util.SystemSettingsManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
class DemoActivityTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var controller: ActivityController<DemoActivity>
    private var destroyed = false
    private var user: TraceUser? = null
    private val status = TraceStatus(false, false, false, 0, null, TraceMode.ACTIVE,
        TraceModeSource.LOCAL, true, false, false, "https://example.com/api/v1/", "tcp://example.com:1883")

    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        mockkStatic(BarikoiTrace::class)
        every { BarikoiTrace.setLogListener(any()) } just Runs
        every { BarikoiTrace.getUser() } answers { user }
        every { BarikoiTrace.locationUpdates } returns MutableSharedFlow()
        coEvery { BarikoiTrace.getTrackingStatus() } returns status
        coEvery { BarikoiTrace.awaitReady() } just Runs
    }
    @After fun teardown() {
        closeActivity()
        dispatcher.scheduler.runCurrent()
        Dispatchers.resetMain()
        unmockkAll()
    }
    private fun closeActivity() {
        if (::controller.isInitialized && !destroyed) {
            controller.pause().stop().destroy()
            destroyed = true
        }
    }
    private fun withDemo(block: (DemoActivity) -> Unit) {
        controller = Robolectric.buildActivity(DemoActivity::class.java).setup()
        try {
            dispatcher.scheduler.runCurrent()
            block(controller.get())
        } finally {
            closeActivity()
            dispatcher.scheduler.runCurrent()
        }
    }
    @Test fun stoppedDemoDoesNotClaimTrackingIsActiveOrStartWithoutUser() = runTest(dispatcher) {
        withDemo { activity ->
            assertThat(activity.findViewById<TextView>(R.id.tvStatus).text.toString()).isEqualTo("Tracking: stopped")
            assertThat(activity.findViewById<MaterialButton>(R.id.btnToggleTracking).isEnabled).isFalse()
            assertThat(activity.findViewById<TextView>(R.id.tvPendingUploads).text.toString()).isEqualTo("Pending uploads: 0")
            verify(exactly = 0) { BarikoiTrace.initialize(any(), any()) }
        }
    }
    @Test fun recreationPreservesDraftModeAndDoesNotReinitializeSdk() = runTest(dispatcher) {
        withDemo {
            controller.get().findViewById<TextInputEditText>(R.id.inputInterval).setText("73")
            controller.get().findViewById<Spinner>(R.id.spinnerSource).setSelection(1)
            controller.recreate()
            runCurrent()
            assertThat(controller.get().findViewById<TextInputEditText>(R.id.inputInterval).text.toString()).isEqualTo("73")
            assertThat(controller.get().findViewById<Spinner>(R.id.spinnerSource).selectedItem.toString()).isEqualTo("REMOTE")
            verify(exactly = 0) { BarikoiTrace.initialize(any(), any()) }
        }
    }
    @Test fun coarsePermissionAloneAllowsStartAndUiWaitsForService() = runTest(dispatcher) {
        val application = ApplicationProvider.getApplicationContext<Application>()
        Shadows.shadowOf(application).grantPermissions(Manifest.permission.ACCESS_COARSE_LOCATION)
        user = TraceUser(userId = "u1")
        every { BarikoiTrace.isLocationPermissionsGranted() } answers { SystemSettingsManager.checkPermissions(application) }
        every { BarikoiTrace.isLocationSettingsOn() } returns true
        every { BarikoiTrace.startTracking(any(), any(), any()) } just Runs
        withDemo { activity ->
            activity.findViewById<MaterialButton>(R.id.btnToggleTracking).performClick()
            runCurrent()
            verify(exactly = 1) { BarikoiTrace.startTracking(any(), any(), any()) }
            assertThat(activity.findViewById<TextView>(R.id.tvStatus).text.toString()).isEqualTo("Tracking: starting…")
        }
    }
}
