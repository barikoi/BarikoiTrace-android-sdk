package com.barikoi.barikoitrace.api

import android.content.Context
import android.location.Location
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.barikoi.barikoitrace.util.DateTimeUtils
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import info.mqtt.android.service.Ack
import info.mqtt.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence

class MqttManager(
    private val context: Context,
    private val serverUri: String,
    private val userId: String,
    private val companyId: String,
    private val groupId: String,
    private val uuid: String,
    private val callback: MqttStatusCallback? = null,
    private val userName: String? = null,
    // Required now — was a hardcoded companion-object constant
    // ("rilus"/"r1lu5") shared by every consumer of this library. Caller
    // (LocTraceForegroundService) reads the real per-app/per-environment
    // credentials from TraceDataStore. See BarikoiTrace.initialize()'s doc
    // comment for the migration note on this breaking change.
    private val mqttUsername: String,
    private val mqttPassword: String
) {
    interface MqttStatusCallback {
        fun onConnectionStatusChanged(connected: Boolean, message: String)
        fun onMessageDelivered(topic: String)
        fun onMessageReceived(topic: String, message: String)
    }

    private val tag = "MqttManager"

    private val clientId = "AndroidClient-$userId-$uuid"
    private val deviceId = userId
    private val channelTopic = "company/$companyId/$groupId/$userId/location"

    private var mqttClient: MqttAndroidClient? = null
    private var isConnected = false
    private var isConnecting = false
    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 10
    private val reconnectIntervalMs = 5000L
    private val handler = Handler(Looper.getMainLooper())

    private val mqttCallback = object : MqttCallbackExtended {
        override fun connectComplete(reconnect: Boolean, serverURI: String?) {
            isConnected = true
            isConnecting = false
            Log.i(tag, "Connected to MQTT broker")
            callback?.onConnectionStatusChanged(true, if (reconnect) "reconnected" else "connected")
        }

        override fun connectionLost(cause: Throwable?) {
            isConnected = false
            isConnecting = false
            Log.e(tag, "Connection lost", cause)
            callback?.onConnectionStatusChanged(false, "Connection lost: ${cause?.message ?: "Unknown"}")
            scheduleReconnect()
        }

        override fun messageArrived(topic: String?, message: MqttMessage?) {
            val payloadBytes = message?.payload ?: run {
                Log.w(tag, "Message arrived with empty payload on topic: $topic")
                return
            }
            val payload = String(payloadBytes, Charsets.UTF_8)
            Log.d(tag, "Message arrived on topic: $topic - $payload")
            callback?.onMessageReceived(topic ?: "", payload)
        }

        override fun deliveryComplete(token: IMqttDeliveryToken?) {
            try {
                val topics = token?.topics
                if (!topics.isNullOrEmpty()) {
                    Log.d(tag, "Message delivered to topic: ${topics[0]}")
                    callback?.onMessageDelivered(topics[0])
                }
            } catch (e: Exception) {
                Log.e(tag, "Error in delivery complete", e)
            }
        }
    }

    fun connect() {
        if (isConnecting) return

        val client = mqttClient ?: MqttAndroidClient(
            context, serverUri, clientId,
            Ack.AUTO_ACK, MqttDefaultFilePersistence(context.filesDir.absolutePath), false, 100
        ).also { mqttClient = it }

        if (client.isConnected) {
            isConnected = true
            callback?.onConnectionStatusChanged(true, "already connected")
            return
        }

        isConnecting = true
        client.setCallback(mqttCallback)
        val options = createConnectionOptions()

        Log.d(tag, "Attempting to connect to MQTT broker: $serverUri")
        callback?.onConnectionStatusChanged(false, "Connecting...")

        client.connect(options, null, object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                isConnected = true
                isConnecting = false
                reconnectAttempts = 0
                Log.i(tag, "Connected to MQTT broker")
                callback?.onConnectionStatusChanged(true, "Connected")
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                isConnected = false
                isConnecting = false
                Log.e(tag, "Connection failed", exception)
                callback?.onConnectionStatusChanged(false, "Connection failed: ${exception?.message}")
                scheduleReconnect()
            }
        })
    }

    private fun createConnectionOptions(): MqttConnectOptions {
        return MqttConnectOptions().apply {
            isAutomaticReconnect = true
            isCleanSession = false
            keepAliveInterval = 60
            serverURIs = arrayOf(this@MqttManager.serverUri)
            try {
                setWill("device/$deviceId/status", "offline".toByteArray(), 1, true)
            } catch (e: Exception) {
                Log.e(tag, "Error setting LWT", e)
            }
            userName = mqttUsername
            password = mqttPassword.toCharArray()
        }
    }

    private fun scheduleReconnect() {
        if (isConnecting) return
        isConnecting = true
        if (reconnectAttempts >= maxReconnectAttempts) {
            reconnectAttempts = 0
            isConnecting = false
            return
        }

        val delay = (reconnectIntervalMs * (1L shl reconnectAttempts.coerceAtMost(4))).coerceAtMost(60_000L)
        reconnectAttempts++
        Log.d(tag, "Scheduling reconnect in ${delay}ms, attempt $reconnectAttempts")

        handler.postDelayed({
            isConnecting = false
            if (!isConnected) connect()
        }, delay)
    }

    fun publishLocation(location: Location, tripId: String? = null, tripStatus: String = "active") {
        try {
            val locationData = JsonObject().apply {
                addProperty("latitude", location.latitude)
                addProperty("longitude", location.longitude)
                // Was raw epoch-ms (location.time) here vs. a formatted UTC
                // string on the offline-write/flush path — the two payload
                // shapes silently disagreed on this field's type. Now uses
                // the same DateTimeUtils formatting everywhere, matching
                // what the iOS SDK standardized on (see its work plan's
                // Phase 0 / defect carry-forward checklist).
                addProperty("gpx_time", DateTimeUtils.getDateTimeLocal(location.time))
                addProperty("user_id", deviceId)
                addProperty("company_id", companyId)
                addProperty("speed", location.speed)
                addProperty("bearing", location.bearing)
                addProperty("altitude", location.altitude)
                addProperty("accuracy", location.accuracy)
                userName?.takeIf { it.isNotBlank() }?.let {
                    addProperty("user_name", it)
                }
                tripId?.let {
                    addProperty("trip_id", it)
                    addProperty("trip_status", tripStatus)
                }
            }
            Log.d(tag, "Publishing location: $locationData")
            publishMessage(channelTopic, locationData.toString(), 1, false)
        } catch (e: Exception) {
            Log.e(tag, "Error creating location JSON", e)
        }
    }

    fun publishLocationJson(json: JsonObject) {
        Log.d(tag, "Publishing offline location: $json")
        publishMessage(channelTopic, json.toString(), 1, false)
    }

    fun publishOfflineBatch(jsonArray: JsonArray) {
        Log.d(tag, "Publishing offline batch: $jsonArray")
        publishMessage(channelTopic, jsonArray.toString(), 1, false)
    }

    private fun publishMessage(topic: String, payload: String, qos: Int, retained: Boolean) {
        val client = mqttClient
        if (client != null && isConnected) {
            val message = MqttMessage(payload.toByteArray()).apply {
                this.qos = qos
                isRetained = retained
            }
            client.publish(topic, message, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(tag, "Published to topic: $topic")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.e(tag, "Failed to publish to topic: $topic", exception)
                }
            })
        } else {
            Log.w(tag, "Cannot publish - not connected")
        }
    }

    fun isConnected(): Boolean = isConnected && mqttClient?.isConnected == true

    fun destroy() {
        handler.removeCallbacksAndMessages(null)
        val client = mqttClient ?: return
        
        isConnecting = false
        reconnectAttempts = 0
        
        client.removeCallback(mqttCallback)
        try {
            if (client.isConnected) {
                client.disconnect(0, null, object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        Log.i(tag, "Disconnected from MQTT broker")
                    }
                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        Log.e(tag, "Failed to disconnect", exception)
                    }
                })
            }
            client.unregisterResources()
            client.close()
        } catch (e: Exception) {
            Log.e(tag, "Error during destroy", e)
        } finally {
            mqttClient = null
            isConnected = false
        }
    }
}
