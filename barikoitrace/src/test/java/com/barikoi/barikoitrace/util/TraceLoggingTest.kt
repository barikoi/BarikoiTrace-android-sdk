package com.barikoi.barikoitrace.util

import android.content.Context
import android.location.Location
import androidx.test.core.app.ApplicationProvider
import com.barikoi.barikoitrace.BarikoiTrace
import com.barikoi.barikoitrace.LocTraceManager
import com.barikoi.barikoitrace.api.MqttManager
import com.barikoi.barikoitrace.api.TraceApiClient
import com.barikoi.barikoitrace.api.TraceApiService
import com.barikoi.barikoitrace.storage.TraceDataStore
import com.google.common.truth.Truth.assertThat
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLog
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class TraceLoggingTest {
    private lateinit var context: Context
    private lateinit var dataStore: TraceDataStore
    private val listenerMessages = mutableListOf<String>()
    private val secret = "sensitive-payload-91753"

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        dataStore = TraceDataStore(context)
        ShadowLog.clear()
        BarikoiTrace.setLogListener(object : BarikoiTrace.TraceLogListener {
            override fun onLog(level: String, tag: String, message: String) {
                listenerMessages.add(message)
            }
        })
    }

    @After
    fun teardown() {
        BarikoiTrace.setLogListener(null)
        runBlocking { dataStore.setLogging(false) }
    }

    @Test
    fun disabledLogging_suppressesHttpMqttErrorsAndLogListener() = runBlocking {
        dataStore.setLogging(false)
        exerciseHttpAndMqtt()
        TraceLogger(context, "LocTrace").e("Request failed", IOException(secret))
        BarikoiTrace.notifyLog("DEBUG", "LocTrace", secret)
        assertThat(sdkMessages()).isEmpty()
        assertThat(listenerMessages).isEmpty()
    }

    @Test
    fun enabledLogging_omitsSecretsIdentifiersCoordinatesAndExceptionText() = runBlocking {
        dataStore.setLogging(true)
        exerciseHttpAndMqtt()
        TraceLogger(context, "LocTrace").e("Request failed", IOException(secret))
        val messages = sdkMessages() + listenerMessages
        assertThat(sdkMessages()).isNotEmpty()
        assertThat(listenerMessages).isNotEmpty()
        for (message in messages) {
            assertThat(message).doesNotContain(secret)
            assertThat(message).doesNotContain("01799999999")
            assertThat(message).doesNotContain("23.7925")
            assertThat(message).doesNotContain("90.4078")
            assertThat(message).doesNotContain("user-private")
            assertThat(message).doesNotContain("company-private")
            assertThat(message).doesNotContain("Bearer")
            assertThat(message).doesNotContain("https://")
        }
        assertThat(messages).contains("Request failed (IOException)")
    }

    @Test
    fun publicLoggingToggle_takesEffectImmediatelyForExistingLoggersAndListeners() = runBlocking {
        dataStore.setLogging(true)
        val manager = LocTraceManager.getInstance(context)
        val logger = TraceLogger(context, "LocTrace")
        logger.i("Before disable")
        assertThat(listenerMessages).contains("Before disable")
        manager.setLoggingEnabled(false)
        ShadowLog.clear()
        listenerMessages.clear()
        logger.e("After disable", IOException(secret))
        BarikoiTrace.notifyLog("ERROR", "LocTrace", secret)
        assertThat(sdkMessages()).isEmpty()
        assertThat(listenerMessages).isEmpty()
        manager.setLoggingEnabled(true)
        logger.i("After enable")
        assertThat(sdkMessages()).containsExactly("After enable")
        assertThat(listenerMessages).containsExactly("After enable")
        Unit
    }

    private fun sdkMessages(): List<String> = ShadowLog.getLogs()
        .filter { it.tag in setOf("TraceApi", "MqttManager", "LocTrace", "LocTraceService", "LocTraceDataService", "BootReceiver") }
        .map { it.msg }

    private suspend fun exerciseHttpAndMqtt() {
        val apiClient = TraceApiClient.getInstance(context)
        val request = Request.Builder()
            .url("https://example.com/$secret?api_key=$secret&phone=01799999999")
            .header("Authorization", "Bearer $secret")
            .post("""{"api_key":"$secret","phone":"01799999999"}""".toRequestBody())
            .build()
        val response = Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(200)
            .message(secret).body(secret.toResponseBody()).build()
        val chain = mockk<Interceptor.Chain>()
        every { chain.request() } returns request
        every { chain.proceed(any()) } returns response
        val httpClient = TraceApiClient::class.java.getDeclaredField("okHttpClient")
            .apply { isAccessible = true }.get(apiClient) as OkHttpClient
        httpClient.interceptors.forEach { it.intercept(chain).close() }

        val field = TraceApiClient::class.java.getDeclaredField("apiService").apply { isAccessible = true }
        val originalService = field.get(apiClient)
        val service = mockk<TraceApiService>()
        coEvery { service.authenticate(any(), any()) } returns retrofit2.Response.success(
            JsonParser.parseString("""{"user":{"_id":"user-private","name":"$secret",
                "companies":[{"company_id":"company-private","group_id":"$secret"}]}}""").asJsonObject
        )
        field.set(apiClient, service)
        try {
            apiClient.setApiKey(secret)
            apiClient.authenticate(secret, "$secret@example.com", "01799999999")
        } finally {
            field.set(apiClient, originalService)
        }

        val mqtt = MqttManager(context, "tcp://$secret:1883", "user-private", "company-private", secret, secret, userName = secret)
        try {
            val location = Location("test").apply { latitude = 23.7925; longitude = 90.4078 }
            mqtt.publishLocation(location)
            val json = JsonObject().apply { addProperty("api_key", secret); addProperty("latitude", 23.7925) }
            mqtt.publishLocationJson(json)
            mqtt.publishOfflineBatch(JsonArray().apply { add(json) })
            val callback = MqttManager::class.java.getDeclaredField("mqttCallback")
                .apply { isAccessible = true }.get(mqtt) as MqttCallbackExtended
            callback.messageArrived("company/company-private/user-private/$secret", MqttMessage(secret.toByteArray()))
            callback.connectionLost(IOException(secret))
        } finally {
            mqtt.destroy()
        }
    }
}
