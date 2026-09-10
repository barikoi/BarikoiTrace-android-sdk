package com.barikoi.barikoitrace

import com.barikoi.barikoitrace.api.ApiRoutes
import com.barikoi.barikoitrace.api.MqttManager
import java.net.URI

/**
 * Everything the SDK needs to start, in one value.
 *
 * Replaces the old `initialize(context, apiKey, mqttUsername, mqttPassword)`
 * plus a scatter of `setBaseUrl`/`setMqttUrl`/`setMqttClientIdPrefix` calls
 * that had to happen in the right order relative to it — an ordering nothing
 * enforced, and which silently produced a client pointed at the wrong broker
 * when a host app got it wrong.
 *
 * Field-for-field identical to the iOS SDK's `TraceConfig`, so the two
 * platforms can be configured through one shared wrapper.
 *
 * ```kotlin
 * BarikoiTrace.initialize(
 *     context,
 *     TraceConfig(
 *         apiKey = BuildConfig.API_KEY,
 *         mqttUsername = BuildConfig.MQTT_USERNAME,
 *         mqttPassword = BuildConfig.MQTT_PASSWORD
 *     )
 * )
 * ```
 *
 * Self-hosted or staging deployments override the endpoints:
 *
 * ```kotlin
 * val config = TraceConfig(
 *     apiKey = "…",
 *     mqttUsername = "…",
 *     mqttPassword = "…",
 *     baseUrl = "https://api.staging.example.com/api/v1/",
 *     mqttUrl = "ssl://broker.staging.example.com:8883",
 *     mqttClientIdPrefix = "fleet-android-"
 * )
 * BarikoiTrace.initialize(context, config)
 * ```
 */
data class TraceConfig(

    // --- Required ---

    /**
     * Barikoi API key, from the Barikoi dashboard. Used for
     * `POST /sdk/authenticate` and `POST /sdk/company/settings`.
     */
    val apiKey: String,

    /**
     * MQTT broker username. Issued per company, separately from [apiKey] — it
     * is not derivable from it. Must match the broker ACL, or CONNECT is
     * refused with `notAuthorized`.
     */
    val mqttUsername: String,

    /**
     * MQTT broker password. Treat as a server secret: fetch it at runtime
     * rather than compiling it in. See the README's credentials section.
     */
    val mqttPassword: String,

    // --- Endpoints (defaulted) ---

    /** REST base URL. Trailing slash is normalized for you. */
    val baseUrl: String = ApiRoutes.BASE_URL,

    /**
     * Broker URL, `scheme://host[:port]`.
     *
     * Recognized schemes: `tcp`, `mqtt`, `ws` (plaintext) and `ssl`, `mqtts`,
     * `tls`, `wss` (TLS).
     *
     * The SDK default is **plaintext** — every fix and both broker credentials
     * cross the network unencrypted. Point this at a TLS endpoint for anything
     * carrying real user locations.
     */
    val mqttUrl: String = ApiRoutes.MQTT_URL,

    /**
     * Client-id prefix. Full client id is `{prefix}{userId}-{deviceUuid}`
     * (iOS uses `iOSClient-`). Only worth changing when the broker ACL
     * authorizes by client-id pattern — the symptom is `notAuthorized` on a
     * CONNECT whose username and password are correct.
     */
    val mqttClientIdPrefix: String = MqttManager.DEFAULT_CLIENT_ID_PREFIX
) {

    /**
     * Whether [mqttUrl] names a TLS scheme. Surfaced so a host app can assert
     * on it in a release build rather than discovering plaintext transport in
     * production.
     */
    val isMqttTransportEncrypted: Boolean
        get() = try {
            URI(mqttUrl).scheme?.lowercase() in setOf("ssl", "mqtts", "tls", "wss")
        } catch (_: Exception) {
            false
        }

    /**
     * Non-fatal configuration problems, in the order they should be fixed. The
     * SDK logs these at `initialize`; check them yourself if you want to fail a
     * release build instead.
     */
    val warnings: List<String>
        get() = buildList {
            if (apiKey.isBlank()) add("apiKey is empty — /sdk/authenticate will fail with NO_KEY.")
            if (mqttUsername.isBlank() || mqttPassword.isBlank()) {
                add("MQTT credentials are empty — the broker will refuse CONNECT with notAuthorized.")
            }
            if (!isMqttTransportEncrypted) {
                add("mqttUrl '$mqttUrl' is plaintext — credentials and location data are sent unencrypted. Use ssl:// (port 8883).")
            }
            if (!baseUrl.startsWith("https://")) {
                add("baseUrl '$baseUrl' is not HTTPS.")
            }
        }
}
