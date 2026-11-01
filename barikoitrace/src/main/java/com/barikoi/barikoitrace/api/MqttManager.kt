package com.barikoi.barikoitrace.api

import android.content.Context
import android.location.Location
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import com.barikoi.barikoitrace.util.TraceLogger
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
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
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence
import kotlin.time.Duration.Companion.milliseconds

class MqttManager(
    private val context: Context,
    private val serverUri: String,
    private val userId: String,
    private val companyId: String,
    private val groupId: String,
    private val uuid: String,
    private val callback: MqttStatusCallback? = null,
    userName: String? = null
) {
    private companion object {
        const val MQTT_USERNAME = "rilus"
        const val MQTT_PASSWORD = "r1lu5"
    }

    interface MqttStatusCallback {
        fun onConnectionStatusChanged(connected: Boolean, message: String)
        fun onMessageDelivered(topic: String)
        fun onMessageReceived(topic: String, message: String)
    }

    private val logger = TraceLogger(context, "MqttManager")

    private val clientId = "AndroidClient-$userId-$uuid"
    private val deviceId = userId
    private val channelTopic = "company/$companyId/$groupId/$userId/location"

    private var mqttClient: MqttAndroidClient? = null
    @Volatile private var isConnected = false
    private var isConnecting = false
    @Volatile private var destroyed = false
    private var reconnectAttempts = 0
    private val reconnectIntervalMs = 5000L
    private val handler = Handler(Looper.getMainLooper())
    private var reconnectScheduled = false
    private var retryAfterNetworkChange = false
    private var clientGeneration = 0
    private var networkCallbackRegistered = false
    private var validatedNetwork: Network? = null
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    internal var clientFactory: () -> MqttAndroidClient = {
        MqttAndroidClient(context, serverUri, clientId, Ack.AUTO_ACK,
            MqttDefaultFilePersistence(context.filesDir.absolutePath), false, 100)
    }
    private val reconnectTask = Runnable {
        reconnectScheduled = false
        connectInternal()
    }
    private val connectionTimeoutTask = Runnable {
        if (!destroyed && isConnecting) {
            isConnecting = false
            logger.w("MQTT connection attempt timed out")
            callback?.onConnectionStatusChanged(false, "Connection attempt timed out")
            scheduleReconnect()
        }
    }
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            val available = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            onMain {
                if (!available && validatedNetwork == network) validatedNetwork = null
                if (available) {
                    if (!destroyed && validatedNetwork != network) {
                        validatedNetwork = network
                        retryAfterNetworkChange = true
                        reconnectAttempts = 0
                        logger.i("Network available; checking MQTT connection")
                        if (!isConnecting && !isConnected()) connectInternal()
                    }
                }
            }
        }

        override fun onLost(network: Network) {
            onMain {
                if (!destroyed && validatedNetwork == network) {
                    validatedNetwork = null
                    isConnected = false
                    callback?.onConnectionStatusChanged(false, "Network unavailable")
                    // Retire a connected socket bound to the old network. An in-flight
                    // connect is allowed to finish/fail, avoiding competing connects.
                    if (!isConnecting) releaseClient(0)
                    scheduleReconnect()
                }
            }
        }
    }

    private fun onMain(block: () -> Unit) {
        if (Looper.myLooper() == handler.looper) block() else handler.post { block() }
    }

    @Volatile
    private var userName: String? = userName

    private var mqttCallback = createCallback(clientGeneration)

    private fun createCallback(generation: Int) = object : MqttCallbackExtended {
        override fun connectComplete(reconnect: Boolean, serverURI: String?) {
            onMain {
                if (!destroyed && generation == clientGeneration) connected(if (reconnect) "Reconnected" else "Connected")
            }
        }

        override fun connectionLost(cause: Throwable?) {
            onMain {
                if (!destroyed && generation == clientGeneration) connectionFailed(cause, "Connection lost")
            }
        }

        override fun messageArrived(topic: String?, message: MqttMessage?) {
            if (destroyed || generation != clientGeneration) return
            val payload = message?.payload ?: return
            logger.d("MQTT message received")
            callback?.onMessageReceived(topic ?: "", String(payload, Charsets.UTF_8))
        }

        override fun deliveryComplete(token: IMqttDeliveryToken?) {
            if (destroyed || generation != clientGeneration) return
            try {
                token?.topics?.firstOrNull()?.let {
                    logger.d("MQTT message delivered")
                    callback?.onMessageDelivered(it)
                }
            } catch (e: Exception) {
                logger.e("Error in delivery complete", e)
            }
        }
    }

    fun connect() {
        onMain {
            if (destroyed) return@onMain
            if (!networkCallbackRegistered) {
                try {
                    connectivityManager?.registerDefaultNetworkCallback(networkCallback)
                    networkCallbackRegistered = connectivityManager != null
                } catch (e: Exception) {
                    logger.w("Network monitoring unavailable; MQTT retry timer remains active", e)
                }
            }
            connectInternal()
        }
    }

    private fun connectInternal() {
        if (destroyed || isConnecting) return
        handler.removeCallbacks(reconnectTask)
        reconnectScheduled = false
        var generation = clientGeneration
        try {
            val client = mqttClient ?: clientFactory().also {
                mqttClient = it
                clientGeneration++
                generation = clientGeneration
                mqttCallback = createCallback(generation)
                it.setCallback(mqttCallback)
            }
            if (client.isConnected) {
                connected("Already connected")
                return
            }
            isConnecting = true
            retryAfterNetworkChange = false
            handler.removeCallbacks(connectionTimeoutTask)
            handler.postDelayed(connectionTimeoutTask, 30_000L)
            logger.d("Attempting to connect to MQTT broker")
            callback?.onConnectionStatusChanged(false, "Connecting")
            client.connect(createConnectionOptions(), null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    onMain {
                        if (!destroyed && generation == clientGeneration) connected("Connected")
                    }
                }
                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    onMain {
                        if (!destroyed && generation == clientGeneration) connectionFailed(exception)
                    }
                }
            })
        } catch (e: Exception) {
            connectionFailed(e)
        }
    }

    private fun connected(message: String) {
        val wasConnected = isConnected
        isConnected = true
        isConnecting = false
        reconnectAttempts = 0
        retryAfterNetworkChange = false
        reconnectScheduled = false
        handler.removeCallbacks(reconnectTask)
        handler.removeCallbacks(connectionTimeoutTask)
        if (!wasConnected) {
            logger.i("Connected to MQTT broker")
            callback?.onConnectionStatusChanged(true, message)
        }
    }

    private fun connectionFailed(error: Throwable?, message: String = "Connection failed") {
        // A late failure from a competing dependency callback must not undo a successful connection.
        if (runCatching { mqttClient?.isConnected == true }.getOrDefault(false)) {
            connected("Connected")
            return
        }
        isConnected = false
        isConnecting = false
        handler.removeCallbacks(connectionTimeoutTask)
        val reason = (error as? MqttException)?.reasonCode
        val cause = error?.cause?.javaClass?.simpleName ?: error?.javaClass?.simpleName ?: "Unknown"
        val detail = "$message (reason=${reason ?: "unknown"}, cause=$cause)"
        logger.w(detail)
        callback?.onConnectionStatusChanged(false, detail)
        if (reason == MqttException.REASON_CODE_CLIENT_CLOSED.toInt() || error is IllegalArgumentException) {
            releaseClient(0)
        }
        val immediate = retryAfterNetworkChange
        retryAfterNetworkChange = false
        scheduleReconnect(immediate)
    }

    private fun createConnectionOptions(): MqttConnectOptions {
        return MqttConnectOptions().apply {
            // One SDK-owned retry loop handles initial failures and network recovery.
            isAutomaticReconnect = false
            connectionTimeout = 15
            isCleanSession = false
            keepAliveInterval = 60
            serverURIs = arrayOf(this@MqttManager.serverUri)
            try {
                setWill("device/$deviceId/status", "offline".toByteArray(), 1, true)
            } catch (e: Exception) {
                logger.e("Error setting LWT", e)
            }
            userName = MQTT_USERNAME
            password = MQTT_PASSWORD.toCharArray()
        }
    }

    private fun scheduleReconnect(immediate: Boolean = false) {
        if (destroyed || isConnecting || reconnectScheduled) return
        val delay = if (immediate) 0L else
            (reconnectIntervalMs * (1L shl reconnectAttempts.coerceAtMost(4))).coerceAtMost(60_000L)
        reconnectAttempts = (reconnectAttempts + 1).coerceAtMost(4)
        reconnectScheduled = true
        logger.d("Scheduling MQTT reconnect in ${delay}ms")
        handler.postDelayed(reconnectTask, delay)
    }

    fun publishLocation(location: Location, tripId: String? = null, tripStatus: String = "active") {
        try {
            val locationData = buildLocationJson(location, tripId, tripStatus)
            logger.d("Publishing location")
            publishMessage(channelTopic, locationData.toString(), 1, false)
        } catch (e: Exception) {
            logger.e("Error creating location JSON", e)
        }
    }

    internal fun buildLocationJson(
        location: Location,
        tripId: String? = null,
        tripStatus: String = "active"
    ): JsonObject {
        return JsonObject().apply {
            addProperty("latitude", location.latitude)
            addProperty("longitude", location.longitude)
            addProperty("gpx_time", location.time)
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
    }

    fun updateUserName(name: String?) {
        userName = name
    }

    fun publishLocationJson(json: JsonObject) {
        logger.d("Publishing offline location")
        publishMessage(channelTopic, json.toString(), 1, false)
    }

    // For durable offline rows, returning from publish() is not delivery confirmation.
    internal suspend fun publishLocationJsonAndAwaitDelivery(json: JsonObject) {
        withTimeout(30_000L.milliseconds) {
            suspendCancellableCoroutine<Unit> { continuation ->
                val completed = AtomicBoolean(false)
                val listener = object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        if (completed.compareAndSet(false, true)) continuation.resume(Unit)
                    }

                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        if (completed.compareAndSet(false, true)) {
                            continuation.resumeWithException(exception ?: IllegalStateException("MQTT publish failed"))
                        }
                    }
                }
                try {
                    publishMessage(channelTopic, json.toString(), 1, false, listener)
                } catch (e: Exception) {
                    listener.onFailure(null, e)
                }
            }
        }
    }

    fun publishOfflineBatch(jsonArray: JsonArray) {
        logger.d("Publishing offline batch")
        publishMessage(channelTopic, jsonArray.toString(), 1, false)
    }

    private fun publishMessage(
        topic: String, payload: String, qos: Int, retained: Boolean,
        listener: IMqttActionListener? = null
    ) {
        val client = mqttClient
        if (client != null && isConnected) {
            val message = MqttMessage(payload.toByteArray()).apply {
                this.qos = qos
                isRetained = retained
            }
            client.publish(topic, message, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    logger.d("MQTT publish completed")
                    listener?.onSuccess(asyncActionToken)
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    logger.e("MQTT publish failed", exception)
                    listener?.onFailure(asyncActionToken, exception)
                }
            })
        } else {
            logger.w("Cannot publish - not connected")
            listener?.onFailure(null, IllegalStateException("MQTT not connected"))
        }
    }

    fun isConnected(): Boolean = !destroyed && isConnected &&
        runCatching { mqttClient?.isConnected == true }.getOrDefault(false)

    fun destroy() {
        onMain {
            if (destroyed) return@onMain
            destroyed = true
            handler.removeCallbacksAndMessages(null)
            reconnectScheduled = false
            isConnecting = false
            if (networkCallbackRegistered) {
                runCatching { connectivityManager?.unregisterNetworkCallback(networkCallback) }
                networkCallbackRegistered = false
            }
            releaseClient(1000)
        }
    }

    private fun releaseClient(quiesceMs: Long) {
        val client = mqttClient
        mqttClient = null
        isConnected = false
        clientGeneration++
        if (client == null) return
        runCatching { client.removeCallback(mqttCallback) }
        // close() alone leaves a closed handle in the Android MQTT service map.
        // Disconnect also removes that handle so a replacement can connect again.
        if (!runCatching { client.isConnected }.getOrDefault(false)) runCatching { client.close() }
        try {
            client.disconnect(quiesceMs, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) = Unit
                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) = Unit
            })
        } catch (e: Exception) {
            logger.w("MQTT connection cleanup could not complete", e)
        } finally {
            runCatching { client.unregisterResources() }
        }
    }
}
