package com.barikoi.barikoitrace.api

import android.content.Context
import android.location.Location
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.google.gson.JsonObject
import info.mqtt.android.service.MqttAndroidClient
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.TimeoutCancellationException
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import java.io.IOException
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class MqttManagerTest {

    private fun createManager(userName: String? = null): MqttManager {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return MqttManager(
            context, "tcp://test:1883", "u1", "c1", "g1", "uuid-1",
            callback = null, userName = userName
        )
    }

    private fun testLocation(): Location =
        Location("test").apply {
            latitude = 23.7925
            longitude = 90.4078
            time = 1700000000000L
        }

    @Test
    fun updateUserName_updatesPayloadUserName() {
        val manager = createManager(userName = "Old")
        manager.updateUserName("New")
        val json = manager.buildLocationJson(testLocation())
        assertThat(json.get("user_name").asString).isEqualTo("New")
    }

    @Test
    fun buildLocationJson_initialUserNameIncluded() {
        val manager = createManager(userName = "Initial")
        val json = manager.buildLocationJson(testLocation())
        assertThat(json.get("user_name").asString).isEqualTo("Initial")
    }

    @Test
    fun buildLocationJson_blankUserName_omitted() {
        val manager = createManager(userName = "Old")
        manager.updateUserName("   ")
        val json = manager.buildLocationJson(testLocation())
        assertThat(json.has("user_name")).isFalse()
    }

    @Test
    fun buildLocationJson_containsCoreFields() {
        val manager = createManager(userName = "Tester")
        val json = manager.buildLocationJson(testLocation(), tripId = "trip-1")
        assertThat(json.get("user_id").asString).isEqualTo("u1")
        assertThat(json.get("company_id").asString).isEqualTo("c1")
        assertThat(json.get("latitude").asString).isEqualTo("23.7925")
        assertThat(json.get("trip_id").asString).isEqualTo("trip-1")
        assertThat(json.get("trip_status").asString).isEqualTo("active")
    }

    private fun connectedManager(client: MqttAndroidClient): MqttManager = createManager().also { manager ->
        MqttManager::class.java.getDeclaredField("mqttClient").apply { isAccessible = true }.set(manager, client)
        MqttManager::class.java.getDeclaredField("isConnected").apply { isAccessible = true }.setBoolean(manager, true)
    }

    @Test
    fun offlinePublish_waitsForConfirmationAndToleratesDuplicateCallbacks() = runTest {
        val client = mockk<MqttAndroidClient>()
        lateinit var listener: IMqttActionListener
        every { client.publish(any<String>(), any<org.eclipse.paho.client.mqttv3.MqttMessage>(), any(), any()) } answers {
            assertThat(secondArg<org.eclipse.paho.client.mqttv3.MqttMessage>().qos).isEqualTo(1)
            listener = arg(3)
            mockk()
        }
        val manager = connectedManager(client)
        val publish = async { manager.publishLocationJsonAndAwaitDelivery(JsonObject()) }
        runCurrent()
        assertThat(publish.isCompleted).isFalse()
        listener.onSuccess(null)
        listener.onSuccess(null)
        publish.await()
    }

    @Test
    fun offlinePublish_failureIsPropagated() = runTest {
        val client = mockk<MqttAndroidClient>()
        val failure = IOException("Connection lost")
        every { client.publish(any<String>(), any<org.eclipse.paho.client.mqttv3.MqttMessage>(), any(), any()) } answers {
            arg<IMqttActionListener>(3).onFailure(null, failure)
            mockk()
        }
        try {
            connectedManager(client).publishLocationJsonAndAwaitDelivery(JsonObject())
            throw AssertionError("Expected publish failure")
        } catch (e: IOException) {
            assertThat(e.message).isEqualTo(failure.message)
        }
    }

    @Test
    fun offlinePublish_disconnectedFailsImmediately() = runTest {
        try {
            createManager().publishLocationJsonAndAwaitDelivery(JsonObject())
            throw AssertionError("Expected disconnected failure")
        } catch (e: IllegalStateException) {
            assertThat(e.message).isEqualTo("MQTT not connected")
        }
    }

    @Test
    fun offlinePublish_missingConfirmationTimesOutAndLateCallbackIsHarmless() = runTest {
        val client = mockk<MqttAndroidClient>()
        lateinit var listener: IMqttActionListener
        every { client.publish(any<String>(), any<org.eclipse.paho.client.mqttv3.MqttMessage>(), any(), any()) } answers {
            listener = arg(3)
            mockk()
        }
        supervisorScope {
            val publish = async { connectedManager(client).publishLocationJsonAndAwaitDelivery(JsonObject()) }
            runCurrent()
            advanceTimeBy(30_000L)
            runCurrent()
            try {
                publish.await()
                throw AssertionError("Expected delivery timeout")
            } catch (_: TimeoutCancellationException) {}
            listener.onSuccess(null)
        }
    }
}
