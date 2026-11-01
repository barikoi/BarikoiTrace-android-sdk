package com.barikoi.barikoitracesdkapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.View
import android.widget.AdapterView
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.barikoi.barikoitrace.BarikoiTrace
import com.barikoi.barikoitrace.TraceMode
import com.barikoi.barikoitrace.TraceModeSource
import com.barikoi.barikoitrace.model.TraceUser
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds
import androidx.core.net.toUri
import androidx.core.content.edit

class DemoActivity : AppCompatActivity() {
    private lateinit var model: DemoViewModel
    private var rendering = false
    private var formInitialized = false
    private var shownRevision = -1
    private var shownUser: TraceUser? = null
    private var shownLogs: List<String>? = null
    private var pendingStart = false
    private var externalRequest = false
    private var restoredForm: Bundle? = null
    private val permissionPreferences by lazy { getSharedPreferences("demo_permissions", MODE_PRIVATE) }

    private val locationPermission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        externalRequest = false
        if (BarikoiTrace.isLocationPermissionsGranted()) {
            model.notice("Location permission granted; approximate location is also supported.")
            if (pendingStart) continueStart() else requestNotifications()
        } else {
            pendingStart = false
            model.notice("Location access was denied. Use Permissions to try again or open app settings.")
        }
    }
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        externalRequest = false
        model.notice(if (granted) "Notifications enabled." else "Notifications denied; tracking can still run with a foreground service.")
        if (pendingStart) continueStart()
    }
    private val backgroundPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        externalRequest = false
        model.notice(if (granted) "Background location enabled." else "Background location was not granted.")
    }
    private val settingsResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        externalRequest = false
        if (pendingStart) {
            if (BarikoiTrace.isLocationPermissionsGranted() && BarikoiTrace.isLocationSettingsOn()) continueStart()
            else {
                pendingStart = false
                model.notice("Tracking was not started. Enable location access and device location first.")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_demo)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        model = ViewModelProvider(this)[DemoViewModel::class.java]
        pendingStart = savedInstanceState?.getBoolean("pending_start") ?: false
        externalRequest = savedInstanceState?.getBoolean("external_request") ?: false
        restoredForm = savedInstanceState

        button(R.id.btnSetUser).setOnClickListener {
            model.setUser(input(R.id.inputPhone).text.toString(), input(R.id.inputName).text.toString())
        }
        button(R.id.btnLogout).setOnClickListener {
            AlertDialog.Builder(this).setTitle("Log out?")
                .setMessage("This stops tracking and deletes the saved user, trip, and queued locations.")
                .setPositiveButton("Log out") { _, _ -> model.logout() }
                .setNegativeButton("Cancel", null).show()
        }
        button(R.id.btnSetUrl).setOnClickListener {
            model.configureEndpoints(input(R.id.inputBaseUrl).text.toString(), input(R.id.inputMqttUrl).text.toString())
        }
        findViewById<ImageButton>(R.id.ivApiKey).setOnClickListener { showApiKeyDialog() }
        button(R.id.btnToggleTracking).setOnClickListener {
            val state = model.state.value
            if (state.status?.serviceRunning == true || state.status?.trackingRequested == true) model.stop()
            else {
                pendingStart = true
                continueStart()
            }
        }
        button(R.id.btnApplyMode).setOnClickListener { readMode()?.let(model::applyMode) }
        button(R.id.btnRemoteSettings).setOnClickListener { model.refreshRemoteSettings() }
        button(R.id.btnPermissions).setOnClickListener {
            pendingStart = false
            if (!BarikoiTrace.isLocationPermissionsGranted()) requestLocation() else requestNotifications(explicit = true)
        }
        button(R.id.btnBackgroundPermission).setOnClickListener { requestBackgroundAccess() }
        button(R.id.btnBatterySettings).setOnClickListener {
            AlertDialog.Builder(this).setTitle("Battery optimization")
                .setMessage("An exemption may help long-running tracking. It does not replace location permission.")
                .setPositiveButton("Open settings") { _, _ ->
                    runCatching { BarikoiTrace.requestDisableBatteryOptimization(this) }
                        .onFailure { model.notice("Battery exemption settings are unavailable on this device.") }
                }.setNegativeButton("Cancel", null).show()
        }
        switch(R.id.switchLogging).setOnCheckedChangeListener { _, checked -> if (!rendering) model.setLogging(checked) }
        switch(R.id.switchBroadcast).setOnCheckedChangeListener { _, checked -> if (!rendering) model.setBroadcasting(checked) }
        button(R.id.btnClearLog).setOnClickListener { model.clearLogs() }
        val selectionListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) = updateModeControls()
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        findViewById<Spinner>(R.id.spinnerMode).onItemSelectedListener = selectionListener
        findViewById<Spinner>(R.id.spinnerSource).onItemSelectedListener = selectionListener

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { model.state.collect(::render) }
                launch { BarikoiTrace.locationUpdates.collect(model::onLocation) }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (isActive) {
                    model.refresh()
                    delay(1_000.milliseconds)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (pendingStart && !externalRequest) continueStart()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("pending_start", pendingStart)
        outState.putBoolean("external_request", externalRequest)
        listOf(R.id.inputInterval, R.id.inputDistance, R.id.inputAccuracy, R.id.inputBaseUrl, R.id.inputMqttUrl,
            R.id.inputPhone, R.id.inputName).forEach { outState.putString("draft_$it", input(it).text.toString()) }
        outState.putInt("draft_mode", findViewById<Spinner>(R.id.spinnerMode).selectedItemPosition)
        outState.putInt("draft_source", findViewById<Spinner>(R.id.spinnerSource).selectedItemPosition)
        outState.putBoolean("draft_offline", switch(R.id.switchOffline).isChecked)
        outState.putBoolean("draft_trip", switch(R.id.switchTrip).isChecked)
        outState.putInt("draft_revision", model.state.value.configurationRevision)
        outState.putString("draft_user_id", model.state.value.user?.userId)
        super.onSaveInstanceState(outState)
    }

    private fun continueStart() {
        if (externalRequest || !lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return
        if (model.state.value.working) { pendingStart = false; return }
        if (model.state.value.user == null) {
            pendingStart = false
            model.notice("Set a user online first. An already saved user can start offline.")
            return
        }
        if (!BarikoiTrace.isLocationPermissionsGranted()) { requestLocation(); return }
        if (!BarikoiTrace.isLocationSettingsOn()) {
            externalRequest = true
            AlertDialog.Builder(this).setTitle("Enable device location")
                .setMessage("Turn on device location, then return here to start tracking.")
                .setPositiveButton("Open settings") { _, _ ->
                    settingsResult.launch(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }.setNegativeButton("Cancel") { _, _ -> externalRequest = false; pendingStart = false }
                .setOnCancelListener { externalRequest = false; pendingStart = false }.show()
            return
        }
        if (needsNotificationPermission() && !permissionPreferences.getBoolean("asked_notifications", false)) {
            requestNotifications()
            return
        }
        val mode = readMode() ?: run { pendingStart = false; return }
        pendingStart = false
        val source = if (findViewById<Spinner>(R.id.spinnerSource).selectedItem.toString() == "REMOTE") {
            TraceModeSource.REMOTE
        } else TraceModeSource.LOCAL
        model.start(mode, switch(R.id.switchTrip).isChecked, source)
    }

    private fun requestLocation() {
        val asked = permissionPreferences.getBoolean("asked_location", false)
        if (asked && !shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) &&
            !shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            externalRequest = true
            AlertDialog.Builder(this).setTitle("Location permission")
                .setMessage("Location access is disabled. You can enable it in app settings.")
                .setPositiveButton("App settings") { _, _ -> openAppSettings() }
                .setNegativeButton("Cancel") { _, _ -> externalRequest = false; pendingStart = false }
                .setOnCancelListener { externalRequest = false; pendingStart = false }.show()
        } else {
            permissionPreferences.edit { putBoolean("asked_location", true) }
            externalRequest = true
            locationPermission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }

    private fun needsNotificationPermission(): Boolean = Build.VERSION.SDK_INT >= 33 &&
        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED

    private fun requestNotifications(explicit: Boolean = false) {
        if (needsNotificationPermission() && (explicit || !permissionPreferences.getBoolean("asked_notifications", false))) {
            permissionPreferences.edit { putBoolean("asked_notifications", true) }
            externalRequest = true
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else if (pendingStart) continueStart()
        else model.notice("Location permission is available. Notification permission is ${if (needsNotificationPermission()) "disabled" else "available"}.")
    }

    private fun requestBackgroundAccess() {
        if (!BarikoiTrace.isLocationPermissionsGranted()) {
            model.notice("Grant foreground location using Permissions first."); return
        }
        if (Build.VERSION.SDK_INT < 29) { model.notice("A separate background permission is not needed on this Android version."); return }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            model.notice("Background location is already enabled."); return
        }
        AlertDialog.Builder(this).setTitle("Background location")
            .setMessage("Allow background location only if you need eligible tracking starts while the app is not visible. On Android 11 or newer, choose Allow all the time in the app's location permission settings.")
            .setPositiveButton("Continue") { _, _ ->
                externalRequest = true
                if (Build.VERSION.SDK_INT >= 30) openAppSettings()
                else backgroundPermission.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }.setNegativeButton("Cancel", null).show()
    }

    private fun openAppSettings() {
        settingsResult.launch(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            "package:$packageName".toUri()))
    }

    private fun showApiKeyDialog() {
        val entry = EditText(this).apply {
            hint = "API key"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            isSaveEnabled = false
        }
        val dialog = AlertDialog.Builder(this).setTitle("API key")
            .setMessage("Changing the key logs out the saved user. The key is stored for this demo on this device.")
            .setView(entry).setPositiveButton("Save", null).setNegativeButton("Cancel", null).create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val key = entry.text.toString().trim()
                if (key.isBlank()) entry.error = "API key cannot be empty"
                else { model.configureApiKey(key); entry.text.clear(); dialog.dismiss() }
            }
        }
        dialog.show()
    }

    private fun readMode(): TraceMode? = try {
        DemoInput.mode(findViewById<Spinner>(R.id.spinnerMode).selectedItem.toString(),
            input(R.id.inputInterval).text.toString(), input(R.id.inputDistance).text.toString(),
            input(R.id.inputAccuracy).text.toString(), switch(R.id.switchOffline).isChecked)
    } catch (e: IllegalArgumentException) {
        model.notice(e.message ?: "Check the tracking settings.")
        null
    }

    private fun render(state: DemoState) {
        val status = state.status
        rendering = true
        try {
            if (status != null && !formInitialized) {
                input(R.id.inputInterval).setText(status.mode.updateInterval.toString())
                input(R.id.inputDistance).setText(status.mode.distanceFilter.toString())
                input(R.id.inputAccuracy).setText(status.mode.accuracyFilter.toString())
                switch(R.id.switchOffline).isChecked = status.offlineTracking
                findViewById<Spinner>(R.id.spinnerSource).setSelection(if (status.source == TraceModeSource.REMOTE) 1 else 0)
                formInitialized = true
            }
            if (status != null && shownRevision != state.configurationRevision) {
                input(R.id.inputBaseUrl).setText(status.baseUrl)
                input(R.id.inputMqttUrl).setText(status.mqttUrl)
                shownRevision = state.configurationRevision
            }
            if (shownUser != state.user) {
                input(R.id.inputPhone).setText(state.user?.phone.orEmpty())
                input(R.id.inputName).setText(state.user?.name.orEmpty())
                shownUser = state.user
            }
            if (status != null && restoredForm != null) {
                val saved = restoredForm!!
                // Preserve draft controls during rotation, but never restore a superseded endpoint change.
                listOf(R.id.inputInterval, R.id.inputDistance, R.id.inputAccuracy).forEach { id ->
                    saved.getString("draft_$id")?.let { input(id).setText(it) }
                }
                if (saved.getInt("draft_revision", -1) == state.configurationRevision) {
                    listOf(R.id.inputBaseUrl, R.id.inputMqttUrl).forEach { id ->
                        saved.getString("draft_$id")?.let { input(id).setText(it) }
                    }
                }
                if (saved.getString("draft_user_id") == state.user?.userId) {
                    listOf(R.id.inputPhone, R.id.inputName).forEach { id ->
                        saved.getString("draft_$id")?.let { input(id).setText(it) }
                    }
                }
                findViewById<Spinner>(R.id.spinnerMode).setSelection(saved.getInt("draft_mode", 0))
                findViewById<Spinner>(R.id.spinnerSource).setSelection(saved.getInt("draft_source", 0))
                switch(R.id.switchOffline).isChecked = saved.getBoolean("draft_offline", true)
                switch(R.id.switchTrip).isChecked = saved.getBoolean("draft_trip", false)
                restoredForm = null
            }
            text(R.id.tvStatus).text = when {
                state.transition != null -> "Tracking: ${state.transition.lowercase()}…"
                status == null -> "Tracking: checking…"
                status.serviceRunning -> "Tracking service: running"
                status.trackingRequested -> "Tracking: requested, service not running"
                else -> "Tracking: stopped"
            }
            text(R.id.tvNotice).text = if (!state.hasApiKey) "Configure an API key using the key button.\n${state.notice}" else state.notice
            text(R.id.tvConnection).text = when {
                status == null -> "MQTT: checking…"
                status.mqttConnected -> "MQTT: connected"
                !status.serviceRunning -> "MQTT: disconnected (tracking stopped)"
                else -> "MQTT: disconnected / connecting"
            }
            text(R.id.tvPendingUploads).text = "Pending uploads: ${status?.pendingLocationCount ?: "…"}"
            text(R.id.tvTripStatus).text = status?.tripId?.let { "Trip: $it" } ?: "No active trip"
            text(R.id.tvEffectiveMode).text = status?.let {
                "Effective ${it.source}: interval ${it.mode.updateInterval}s, distance ${it.mode.distanceFilter}m, accuracy ${it.mode.accuracyFilter}m, offline storage ${if (it.offlineTracking) "on" else "off"}"
            } ?: "Waiting for saved settings…"
            text(R.id.tvLastLocation).text = state.lastLocation?.let {
                String.format(Locale.US, "Last location: %.5f, %.5f (±%.0fm)", it.latitude, it.longitude, it.accuracy)
            } ?: "Last location: none received (enable Broadcast)"
            val running = status?.serviceRunning == true || status?.trackingRequested == true
            val ready = status != null && !state.working
            button(R.id.btnToggleTracking).apply {
                text = state.transition?.let { "$it…" } ?: if (running) "Stop" else "Start"
                isEnabled = ready && (running || state.user != null)
                backgroundTintList = android.content.res.ColorStateList.valueOf(
                    if (running) 0xFFE53935.toInt() else 0xFF43BC5D.toInt()
                )
            }
            button(R.id.btnSetUser).isEnabled = ready && !running && status?.tripId == null && state.hasApiKey
            button(R.id.btnLogout).isEnabled = ready && (state.user != null || (status?.pendingLocationCount ?: 0) > 0 || status?.tripId != null)
            button(R.id.btnSetUrl).isEnabled = ready && !running
            findViewById<ImageButton>(R.id.ivApiKey).isEnabled = ready && !running
            listOf(R.id.inputPhone, R.id.inputName, R.id.inputBaseUrl, R.id.inputMqttUrl).forEach {
                input(it).isEnabled = ready && !running
            }
            button(R.id.btnApplyMode).isEnabled = ready && status?.serviceRunning == true && status.source == TraceModeSource.LOCAL
            button(R.id.btnRemoteSettings).isEnabled = ready && state.user != null
            findViewById<Spinner>(R.id.spinnerSource).isEnabled = ready && !running
            switch(R.id.switchTrip).isEnabled = ready && !running
            if (running && status?.tripId != null) switch(R.id.switchTrip).isChecked = true
            status?.let {
                switch(R.id.switchLogging).isChecked = it.logging
                switch(R.id.switchBroadcast).isChecked = it.broadcasting
            }
            if (shownLogs != state.logs) {
                text(R.id.tvLog).text = state.logs.joinToString("\n")
                shownLogs = state.logs
                findViewById<ScrollView>(R.id.logScrollView).post {
                    if (!isDestroyed) findViewById<ScrollView>(R.id.logScrollView).fullScroll(ScrollView.FOCUS_DOWN)
                }
            }
        } finally { rendering = false }
        updateModeControls()
    }

    private fun updateModeControls() {
        if (!::model.isInitialized) return
        val state = model.state.value
        val remoteRunning = state.status?.serviceRunning == true && state.status.source == TraceModeSource.REMOTE
        val editable = !state.working && !remoteRunning
        val custom = findViewById<Spinner>(R.id.spinnerMode).selectedItem.toString() == "CUSTOM"
        findViewById<Spinner>(R.id.spinnerMode).isEnabled = editable
        listOf(R.id.inputInterval, R.id.inputDistance, R.id.inputAccuracy).forEach { input(it).isEnabled = editable && custom }
        switch(R.id.switchOffline).isEnabled = editable
    }

    private fun input(id: Int): TextInputEditText = findViewById(id)
    private fun text(id: Int): TextView = findViewById(id)
    private fun button(id: Int): MaterialButton = findViewById(id)
    private fun switch(id: Int): SwitchMaterial = findViewById(id)
}
