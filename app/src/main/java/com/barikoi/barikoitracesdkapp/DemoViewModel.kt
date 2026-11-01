package com.barikoi.barikoitracesdkapp

import android.app.Application
import android.location.Location
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.barikoi.barikoitrace.BarikoiTrace
import com.barikoi.barikoitrace.TraceMode
import com.barikoi.barikoitrace.TraceModeSource
import com.barikoi.barikoitrace.model.TraceStatus
import com.barikoi.barikoitrace.model.TraceUser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class DemoState(
    val status: TraceStatus? = null,
    val user: TraceUser? = null,
    val busy: String? = null,
    val transition: String? = null,
    val notice: String = "Checking tracking state…",
    val logs: List<String> = emptyList(),
    val lastLocation: Location? = null,
    val configurationRevision: Int = 0,
    val hasApiKey: Boolean = false
) {
    val working: Boolean get() = busy != null || transition != null
}

class DemoViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = DemoPreferences(application)
    private val mutableState = MutableStateFlow(DemoState(hasApiKey = preferences.apiKey.isNotBlank()))
    internal val state = mutableState.asStateFlow()
    private var operation: Job? = null
    private var transitionStarted = 0L
    private val listener = object : BarikoiTrace.TraceLogListener {
        override fun onLog(level: String, tag: String, message: String) {
            log("[$level][$tag] $message")
        }
    }

    init { BarikoiTrace.setLogListener(listener) }

    internal suspend fun refresh() {
        try {
            val revision = state.value.configurationRevision
            val status = BarikoiTrace.getTrackingStatus()
            if (revision != state.value.configurationRevision) return
            var transition = state.value.transition
            var notice = state.value.notice
            if (notice == "Checking tracking state…") {
                notice = if (BarikoiTrace.getUser() == null) "Ready. Set a user online to begin." else "Ready. A saved user is available for online or offline tracking."
            }
            if (transition == "Starting" && status.serviceRunning) {
                transition = null
                notice = "Tracking service is running. Broker status is shown separately."
            } else if (transition == "Stopping" && !status.serviceRunning && !status.trackingRequested) {
                transition = null
                notice = "Tracking stopped. Queued locations remain saved."
            } else if (transition != null && SystemClock.elapsedRealtime() - transitionStarted > 10_000) {
                transition = null
                notice = "Tracking change was not confirmed. Check permissions and try again."
            }
            mutableState.update { it.copy(status = status, user = BarikoiTrace.getUser(), transition = transition, notice = notice) }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            mutableState.update { it.copy(notice = "Could not read SDK status.") }
        }
    }

    internal fun setUser(phone: String, name: String) {
        if (phone.isBlank()) { notice("Enter a phone number."); return }
        if (!state.value.hasApiKey) { notice("Configure an API key first."); return }
        if (state.value.status?.serviceRunning == true || state.value.status?.tripId != null) {
            notice("Stop tracking and finish the trip before changing users."); return
        }
        perform("Setting user") {
            BarikoiTrace.setOrCreateUser(name.trim().ifBlank { null }, null, phone.trim())
            if (name.isNotBlank()) BarikoiTrace.updateUserName(name.trim())
            notice("User is ready. You can start tracking, including while offline.")
        }
    }

    internal fun configureEndpoints(base: String, mqtt: String) {
        val normalizedBase: String
        val normalizedMqtt: String
        try {
            normalizedBase = DemoInput.restUrl(base)
            normalizedMqtt = DemoInput.mqttUrl(mqtt)
        } catch (e: IllegalArgumentException) { notice(e.message ?: "Check the URLs."); return }
        val status = state.value.status
        if (status != null && normalizedBase != status.baseUrl && status.pendingLocationCount > 0) {
            notice("Upload queued locations first, or log out to discard them before changing the REST server.")
            return
        }
        perform("Saving endpoints") {
            BarikoiTrace.configureEndpoints(normalizedBase, normalizedMqtt)
            val saved = BarikoiTrace.getTrackingStatus()
            mutableState.update { it.copy(status = saved, user = BarikoiTrace.getUser(), configurationRevision = it.configurationRevision + 1) }
            notice("Endpoints saved. A changed REST endpoint requires authentication again.")
        }
    }

    internal fun configureApiKey(key: String) {
        if (key.isBlank()) { notice("API key cannot be empty."); return }
        if (state.value.status?.serviceRunning == true || state.value.status?.trackingRequested == true) {
            notice("Stop tracking before changing the API key."); return
        }
        perform("Saving API key") {
            // Existing users are scoped to the old API key; avoid reusing their cached identity.
            BarikoiTrace.logout()
            BarikoiTrace.initialize(getApplication(), key.trim())
            BarikoiTrace.awaitReady()
            preferences.apiKey = key.trim()
            mutableState.update { it.copy(hasApiKey = true) }
            notice("API key saved. Set the user again.")
        }
    }

    internal fun start(mode: TraceMode, withTrip: Boolean, source: TraceModeSource) {
        if (state.value.user == null) { notice("Set a user online before starting. A saved user can start offline."); return }
        perform("Starting tracking") {
            BarikoiTrace.awaitReady()
            BarikoiTrace.startTracking(mode, withTrip, source)
            transitionStarted = SystemClock.elapsedRealtime()
            mutableState.update { it.copy(transition = "Starting", notice = "Waiting for the tracking service…") }
        }
    }

    internal fun stop() = perform("Stopping tracking") {
        BarikoiTrace.stopTracking()
        transitionStarted = SystemClock.elapsedRealtime()
        mutableState.update { it.copy(transition = "Stopping", notice = "Waiting for tracking to stop…") }
    }

    internal fun applyMode(mode: TraceMode) {
        if (state.value.status?.source == TraceModeSource.REMOTE) {
            notice("Remote settings control this session. Stop and start with LOCAL to change the source."); return
        }
        perform("Applying mode") {
            BarikoiTrace.setTraceMode(mode)
            notice("Mode update requested; active settings will refresh below.")
        }
    }

    internal fun refreshRemoteSettings() = perform("Fetching remote settings") {
        BarikoiTrace.getSettingsFromRemote()
        notice("Remote settings fetched. They apply only when the session source is REMOTE.")
    }

    internal fun logout() = perform("Logging out") {
        BarikoiTrace.logout()
        mutableState.update { it.copy(lastLocation = null) }
        notice("Logged out. User, trip, and queued locations were cleared.")
    }

    internal fun setLogging(enabled: Boolean) {
        BarikoiTrace.setLoggingEnabled(enabled)
        // A local UI action stays visible even when SDK diagnostics are disabled.
        log(if (enabled) "SDK logging enabled" else "SDK logging disabled")
    }

    internal fun setBroadcasting(enabled: Boolean) { BarikoiTrace.setBroadcastingEnabled(enabled) }
    internal fun onLocation(location: Location) { mutableState.update { it.copy(lastLocation = location) } }
    internal fun clearLogs() { mutableState.update { it.copy(logs = emptyList()) } }
    internal fun notice(message: String) { mutableState.update { it.copy(notice = message) } }

    internal fun log(message: String) {
        viewModelScope.launch {
            mutableState.update { it.copy(logs = (it.logs + message.take(1000)).takeLast(300)) }
        }
    }

    private fun perform(label: String, block: suspend () -> Unit) {
        if (state.value.working || operation?.isActive == true) return
        mutableState.update { it.copy(busy = label) }
        operation = viewModelScope.launch {
            try {
                block()
                log("$label completed")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Raw server/transport errors may contain credentials or personal information.
                notice("$label failed (${e.javaClass.simpleName}). Check configuration and connectivity.")
                log("$label failed (${e.javaClass.simpleName})")
            } finally {
                try {
                    if (currentCoroutineContext().isActive) refresh()
                } finally {
                    mutableState.update { it.copy(busy = null) }
                }
            }
        }
    }

    override fun onCleared() {
        BarikoiTrace.setLogListener(null)
        super.onCleared()
    }
}
