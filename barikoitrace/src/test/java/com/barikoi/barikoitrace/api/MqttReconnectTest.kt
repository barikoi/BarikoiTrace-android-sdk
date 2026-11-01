package com.barikoi.barikoitrace.api

import android.content.Context
import android.content.ContextWrapper
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import info.mqtt.android.service.MqttAndroidClient
import io.mockk.*
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowNetwork
import java.io.IOException
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.PAUSED)
class MqttReconnectTest {
    private val connectivity = mockk<ConnectivityManager>(relaxed = true)
    private val observer = slot<ConnectivityManager.NetworkCallback>()
    private val client = mockk<MqttAndroidClient>(relaxed = true)
    private val status = mockk<MqttManager.MqttStatusCallback>(relaxed = true)
    private val listeners = mutableListOf<IMqttActionListener>()
    private var behavior: (IMqttActionListener) -> Unit = {}
    private var clientConnected = false
    private var attempts = 0
    private lateinit var manager: MqttManager

    @Before fun setup() {
        val application = ApplicationProvider.getApplicationContext<Context>()
        val context = object : ContextWrapper(application) {
            override fun getSystemService(name: String): Any? =
                if (name == Context.CONNECTIVITY_SERVICE) connectivity else super.getSystemService(name)
        }
        every { connectivity.registerDefaultNetworkCallback(capture(observer)) } just Runs
        every { client.isConnected } answers { clientConnected }
        every { client.connect(any<MqttConnectOptions>(), any(), any()) } answers {
            attempts++
            val listener = arg<IMqttActionListener>(2)
            listeners += listener
            behavior(listener)
            mockk()
        }
        manager = MqttManager(context, "tcp://test:1883", "u1", "c1", "g1", "uuid", status)
        manager.clientFactory = { client }
    }
    @After fun teardown() {
        manager.destroy()
        shadowOf(Looper.getMainLooper()).idle()
    }
    private fun start() {
        manager.connect()
        shadowOf(Looper.getMainLooper()).idle()
    }
    private fun networkAvailable() {
        observer.captured.onCapabilitiesChanged(ShadowNetwork.newInstance(1), capabilities())
        shadowOf(Looper.getMainLooper()).idle()
    }
    private fun capabilities(validated: Boolean = true) = mockk<NetworkCapabilities> {
        every { hasCapability(any()) } answers {
            firstArg<Int>() == NetworkCapabilities.NET_CAPABILITY_INTERNET ||
                (validated && firstArg<Int>() == NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        }
    }
    @Test fun retriesContinueForThirtyMinutesAndEventuallyConnect() {
        behavior = { it.onFailure(null, IOException("offline")) }
        start()
        shadowOf(Looper.getMainLooper()).idleFor(30, TimeUnit.MINUTES)
        assertThat(attempts).isGreaterThan(10)
        behavior = { clientConnected = true; it.onSuccess(null) }
        shadowOf(Looper.getMainLooper()).idleFor(60, TimeUnit.SECONDS)
        assertThat(manager.isConnected()).isTrue()
        verify(exactly = 1) { status.onConnectionStatusChanged(true, any()) }
        val connectedAttempts = attempts
        shadowOf(Looper.getMainLooper()).idleFor(5, TimeUnit.MINUTES)
        assertThat(attempts).isEqualTo(connectedAttempts)
    }
    @Test fun validatedNetworkReturnCancelsBackoffAndRetriesImmediately() {
        behavior = { it.onFailure(null, IOException("offline")) }
        start()
        assertThat(attempts).isEqualTo(1)
        behavior = {}
        networkAvailable()
        assertThat(attempts).isEqualTo(2)
        networkAvailable()
        assertThat(attempts).isEqualTo(2)
    }
    @Test fun networkReturnDuringConnectDoesNotStartCompetingAttempt() {
        start()
        networkAvailable()
        assertThat(attempts).isEqualTo(1)
        listeners.last().onFailure(null, IOException("old network failed"))
        shadowOf(Looper.getMainLooper()).idle()
        assertThat(attempts).isEqualTo(2)
    }
    @Test fun synchronousConnectExceptionDoesNotLeaveManagerConnectingForever() {
        every { client.connect(any<MqttConnectOptions>(), any(), any()) } throws IOException("failed immediately")
        start()
        shadowOf(Looper.getMainLooper()).idleFor(5, TimeUnit.SECONDS)
        verify(exactly = 2) { client.connect(any<MqttConnectOptions>(), any(), any()) }
    }
    @Test fun missingConnectCallbackTimesOutAndAllowsAnotherAttempt() {
        start()
        shadowOf(Looper.getMainLooper()).idleFor(35, TimeUnit.SECONDS)
        assertThat(attempts).isEqualTo(2)
    }
    @Test fun destroyedClientDoesNotReconnectOnTimersNetworksOrLateCallbacks() {
        behavior = { it.onFailure(null, IOException("offline")) }
        start()
        manager.destroy()
        networkAvailable()
        listeners.first().onSuccess(null)
        shadowOf(Looper.getMainLooper()).idleFor(30, TimeUnit.MINUTES)
        assertThat(attempts).isEqualTo(1)
        assertThat(manager.isConnected()).isFalse()
        verify { connectivity.unregisterNetworkCallback(observer.captured) }
        verify(exactly = 0) { status.onConnectionStatusChanged(true, any()) }
    }
    @Test fun closedClientIsReleasedAndRecreatedWithStaleCallbacksIgnored() {
        val replacement = mockk<MqttAndroidClient>(relaxed = true)
        every { replacement.isConnected } returns false
        var factories = 0
        manager.clientFactory = { if (factories++ == 0) client else replacement }
        behavior = { it.onFailure(null, MqttException(MqttException.REASON_CODE_CLIENT_CLOSED.toInt())) }
        start()
        listeners.first().onSuccess(null)
        shadowOf(Looper.getMainLooper()).idleFor(5, TimeUnit.SECONDS)
        verify { client.close() }
        verify { client.disconnect(any<Long>(), any(), any()) }
        verify(exactly = 1) { replacement.connect(any<MqttConnectOptions>(), any(), any()) }
        verify(exactly = 0) { status.onConnectionStatusChanged(true, any()) }
    }
    @Test fun connectionOptionsUseOneRetryOwnerAndBoundSocketConnectTime() {
        start()
        verify { client.connect(match<MqttConnectOptions> { !it.isAutomaticReconnect && it.connectionTimeout == 15 }, any(), any()) }
    }
    @Test fun validatedInternetReturningOnSameNetworkAlsoTriggersRecovery() {
        behavior = { it.onFailure(null, IOException("offline")) }
        start()
        networkAvailable()
        val attemptsBefore = attempts
        observer.captured.onCapabilitiesChanged(ShadowNetwork.newInstance(1), capabilities(validated = false))
        shadowOf(Looper.getMainLooper()).idle()
        behavior = {}
        networkAvailable()
        assertThat(attempts).isEqualTo(attemptsBefore + 1)
    }
}
