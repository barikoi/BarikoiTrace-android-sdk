package com.barikoi.barikoitracesdkapp

import android.app.Application
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import com.barikoi.barikoitrace.BarikoiTrace
import com.barikoi.barikoitrace.TraceMode
import com.barikoi.barikoitrace.TraceModeSource
import com.barikoi.barikoitrace.model.TraceStatus
import com.barikoi.barikoitrace.model.TraceUser
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
class DemoViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var model: DemoViewModel
    private val store = ViewModelStore()
    private var user: TraceUser? = null
    private var status = TraceStatus(false, false, false, 0, null, TraceMode.ACTIVE,
        TraceModeSource.LOCAL, true, false, false, "https://example.com/api/v1/", "tcp://example.com:1883")

    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        mockkStatic(BarikoiTrace::class)
        every { BarikoiTrace.setLogListener(any()) } just Runs
        coEvery { BarikoiTrace.getTrackingStatus() } answers { status }
        every { BarikoiTrace.getUser() } answers { user }
        coEvery { BarikoiTrace.awaitReady() } just Runs
        val application = ApplicationProvider.getApplicationContext<Application>()
        DemoPreferences(application).apiKey = "test-key"
        model = DemoViewModel(application)
        store.put("demo", model)
    }
    @After fun teardown() {
        store.clear()
        Dispatchers.resetMain()
        unmockkAll()
    }
    @Test fun pendingAuthentication_blocksDuplicateRequestAndLogoutAndUsesEnteredName() = runTest(dispatcher) {
        val result = CompletableDeferred<TraceUser>()
        coEvery { BarikoiTrace.setOrCreateUser(any(), any(), any()) } coAnswers { result.await().also { user = it } }
        coEvery { BarikoiTrace.updateUserName(any()) } coAnswers { user!!.copy(name = firstArg()).also { user = it } }
        model.refresh()
        model.setUser("01700000000", "Driver Name")
        runCurrent()
        model.setUser("01800000000", "Other")
        model.logout()
        coVerify(exactly = 1) { BarikoiTrace.setOrCreateUser(any(), any(), any()) }
        verify(exactly = 0) { BarikoiTrace.logout() }
        result.complete(TraceUser(userId = "u1", phone = "01700000000"))
        advanceUntilIdle()
        coVerify { BarikoiTrace.updateUserName("Driver Name") }
        assertThat(model.state.value.user?.name).isEqualTo("Driver Name")
        assertThat(model.state.value.working).isFalse()
    }
    @Test fun clearingViewModel_cancelsPendingAuthenticationWithoutUpdatingName() = runTest(dispatcher) {
        val result = CompletableDeferred<TraceUser>()
        coEvery { BarikoiTrace.setOrCreateUser(any(), any(), any()) } coAnswers { result.await() }
        model.setUser("01700000000", "Driver")
        runCurrent()
        store.clear()
        result.complete(TraceUser(userId = "u1"))
        advanceUntilIdle()
        coVerify(exactly = 0) { BarikoiTrace.updateUserName(any()) }
    }
    @Test fun authenticationFailure_isHandledWithoutLoggingRawServerText() = runTest(dispatcher) {
        coEvery { BarikoiTrace.setOrCreateUser(any(), any(), any()) } throws IOException("private-api-key")
        model.setUser("01700000000", "Driver")
        advanceUntilIdle()
        assertThat(model.state.value.busy).isNull()
        assertThat(model.state.value.notice).contains("IOException")
        assertThat(model.state.value.notice).doesNotContain("private-api-key")
        assertThat(model.state.value.logs.joinToString()).doesNotContain("01700000000")
        assertThat(model.state.value.logs.joinToString()).doesNotContain("private-api-key")
    }
    @Test fun startWaitsForServiceConfirmationAndKeepsBrokerStateSeparate() = runTest(dispatcher) {
        user = TraceUser(userId = "u1")
        every { BarikoiTrace.startTracking(any(), any(), any()) } just Runs
        model.refresh()
        model.start(TraceMode.ACTIVE, true, TraceModeSource.LOCAL)
        advanceUntilIdle()
        assertThat(model.state.value.transition).isEqualTo("Starting")
        assertThat(model.state.value.status!!.serviceRunning).isFalse()
        status = status.copy(serviceRunning = true, trackingRequested = true, pendingLocationCount = 25)
        model.refresh()
        assertThat(model.state.value.transition).isNull()
        assertThat(model.state.value.status!!.mqttConnected).isFalse()
        assertThat(model.state.value.status!!.pendingLocationCount).isEqualTo(25)
    }
    @Test fun invalidEndpoint_isRejectedBeforeCallingSdk() = runTest(dispatcher) {
        model.configureEndpoints("not-a-url", "tcp://example.com:1883")
        advanceUntilIdle()
        coVerify(exactly = 0) { BarikoiTrace.configureEndpoints(any(), any()) }
        assertThat(model.state.value.notice).contains("HTTP")
    }
    @Test fun endpointRevisionIsPublishedTogetherWithFreshConfiguration() = runTest(dispatcher) {
        model.refresh()
        val saved = CompletableDeferred<TraceStatus>()
        coEvery { BarikoiTrace.configureEndpoints(any(), any()) } just Runs
        coEvery { BarikoiTrace.getTrackingStatus() } coAnswers { saved.await() }
        model.configureEndpoints("https://new.example/api/v1", "tcp://new.example:1883")
        runCurrent()
        assertThat(model.state.value.configurationRevision).isEqualTo(0)
        assertThat(model.state.value.busy).isEqualTo("Saving endpoints")
        saved.complete(status.copy(baseUrl = "https://new.example/api/v1/", mqttUrl = "tcp://new.example:1883"))
        advanceUntilIdle()
        assertThat(model.state.value.configurationRevision).isEqualTo(1)
        assertThat(model.state.value.status!!.baseUrl).isEqualTo("https://new.example/api/v1/")
        assertThat(model.state.value.busy).isNull()
    }
    @Test fun oldStatusPollCannotOverwriteNewlySavedEndpoints() = runTest(dispatcher) {
        model.refresh()
        val oldPoll = CompletableDeferred<TraceStatus>()
        val newStatus = status.copy(baseUrl = "https://new.example/api/v1/", mqttUrl = "tcp://new.example:1883")
        var reads = 0
        coEvery { BarikoiTrace.getTrackingStatus() } coAnswers {
            if (reads++ == 0) oldPoll.await() else newStatus
        }
        coEvery { BarikoiTrace.configureEndpoints(any(), any()) } just Runs
        val polling = launch { model.refresh() }
        runCurrent()
        model.configureEndpoints(newStatus.baseUrl, newStatus.mqttUrl)
        runCurrent()
        oldPoll.complete(status)
        polling.join()
        advanceUntilIdle()
        assertThat(model.state.value.configurationRevision).isEqualTo(1)
        assertThat(model.state.value.status!!.baseUrl).isEqualTo(newStatus.baseUrl)
    }
    @Test fun logsAreBounded() = runTest(dispatcher) {
        repeat(350) { model.log("entry $it") }
        advanceUntilIdle()
        assertThat(model.state.value.logs).hasSize(300)
        assertThat(model.state.value.logs.first()).isEqualTo("entry 50")
    }
}
