package com.barikoi.barikoitracesdkapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.barikoi.barikoitrace.BarikoiTrace
import com.barikoi.barikoitrace.TraceMode
import com.barikoi.barikoitrace.model.TraceError
import com.barikoi.barikoitrace.model.TraceUser
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DemoActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var tvLog: TextView
    private lateinit var logScrollView: ScrollView
    private lateinit var tvTripStatus: TextView
    private lateinit var spinnerMode: Spinner
    private val logBuilder = StringBuilder()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_demo)

        tvStatus = findViewById(R.id.tvStatus)
        tvLog = findViewById(R.id.tvLog)
        val logContainer = findViewById<ScrollView>(R.id.logScrollView)
        logScrollView = logContainer
        tvTripStatus = findViewById(R.id.tvTripStatus)
        spinnerMode = findViewById(R.id.spinnerMode)

        // --- Initialize SDK ---
        val defaultApiKey = BuildConfig.API_KEY
        var currentApiKey = defaultApiKey
        BarikoiTrace.initialize(this, currentApiKey)
        log("SDK initialized with API key from flavor")

        // --- API Key ---
        findViewById<ImageButton>(R.id.ivApiKey).setOnClickListener {
            val input = EditText(this).apply {
                hint = "Enter API Key"
                setText(currentApiKey)
                setSelection(text.length)
            }
            AlertDialog.Builder(this)
                .setTitle("API Key")
                .setView(input)
                .setPositiveButton("Apply") { _, _ ->
                    val newKey = input.text.toString().trim()
                    if (newKey.isNotEmpty()) {
                        currentApiKey = newKey
                        BarikoiTrace.initialize(this@DemoActivity, newKey)
                        log("API key updated and SDK re-initialized")
                        toast("API key updated")
                    } else {
                        toast("API key cannot be empty")
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // --- Permissions ---
        requestPermissionsIfNeeded()

        // --- SDK Log Listener ---
        BarikoiTrace.setLogListener(object : BarikoiTrace.TraceLogListener {
            override fun onLog(level: String, tag: String, message: String) {
                log("[$tag] $message")
            }
        })

        // --- Server URL ---
        val inputBaseUrl = findViewById<TextInputEditText>(R.id.inputBaseUrl)
        val inputMqttUrl = findViewById<TextInputEditText>(R.id.inputMqttUrl)

        // Set default URLs based on flavor
        val defaultBaseUrl: String
        val defaultMqttUrl: String

        when (BuildConfig.FLAVOR) {
            "akg" -> {
                defaultBaseUrl = "http://slv.abulkhairgroup.com:3881/api/v1"
                defaultMqttUrl = "tcp://slv.abulkhairgroup.com:4883"
                log("Using AKG flavor URLs")
            }
            else -> {
                defaultBaseUrl = "https://api.trace.bmapsbd.com/api/v1/"
                defaultMqttUrl = "tcp://broker.trace.bmapsbd.com:1883"
                log("Using default Barikoi URLs")
            }
        }

        inputBaseUrl.setText(defaultBaseUrl)
        inputMqttUrl.setText(defaultMqttUrl)


        findViewById<MaterialButton>(R.id.btnSetUrl).setOnClickListener {
            val base = inputBaseUrl.text.toString()
            val mqtt = inputMqttUrl.text.toString()
            if (base.isNotEmpty()) BarikoiTrace.setBaseUrl(base)
            if (mqtt.isNotEmpty()) BarikoiTrace.setMqttUrl(mqtt)
            log("URLs set: base=$base, mqtt=$mqtt")
        }

        // --- User ---
        val inputPhone = findViewById<TextInputEditText>(R.id.inputPhone)
        val inputName = findViewById<TextInputEditText>(R.id.inputName)

        val cachedUser = BarikoiTrace.getUser()
        if (cachedUser?.phone != null) {
            inputPhone.setText(cachedUser.phone)
            inputName.setText(cachedUser.name)
            log("Cached user: ${cachedUser.name} (${cachedUser.phone})")
        }

        findViewById<MaterialButton>(R.id.btnSetUser).setOnClickListener {
            val phone = inputPhone.text.toString()
            val name = inputName.text.toString()
            if (phone.isEmpty()) {
                toast("Enter phone number")
                return@setOnClickListener
            }
            log("Creating user: $name, $phone")
            BarikoiTrace.setOrCreateUser(name, null, phone, object : BarikoiTrace.TraceUserCallback {
                override fun onSuccess(user: TraceUser) {
                    log("User set: ${user.name} (${user.userId})")
                    tvStatus.text = "User: ${user.name}"
                    toast("User set: ${user.name}")
                }

                override fun onFailure(error: TraceError) {
                    log("User error: ${error.message}")
                    toast(error.message)
                }
            })
        }

        // --- Tracking ---
        val inputInterval = findViewById<TextInputEditText>(R.id.inputInterval)
        val inputDistance = findViewById<TextInputEditText>(R.id.inputDistance)
        val inputAccuracy = findViewById<TextInputEditText>(R.id.inputAccuracy)

        val switchTrip = findViewById<SwitchMaterial>(R.id.switchTrip)
        val btnToggleTracking = findViewById<MaterialButton>(R.id.btnToggleTracking)

        if (BarikoiTrace.isLocationTracking() || BarikoiTrace.isOnTrip()) {
            btnToggleTracking.text = "Stop"
            btnToggleTracking.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFFE53935.toInt())
            log("Tracking already active")
        }
        if (BarikoiTrace.isOnTrip()) {
            switchTrip.isChecked = true
        }

        btnToggleTracking.setOnClickListener {
            if (BarikoiTrace.isLocationTracking()) {
                log("Stopping tracking")
                BarikoiTrace.stopTracking()
                tvStatus.text = "Tracking: stopped"
                btnToggleTracking.text = "Start"
                btnToggleTracking.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFF43bc5d.toInt())
                updateTripStatus()
            } else {
                val mode = buildTraceMode(spinnerMode, inputInterval, inputDistance, inputAccuracy)
                val withTrip = switchTrip.isChecked
                log("Starting tracking: $mode, withTrip=$withTrip")
                BarikoiTrace.startTracking(mode, withTrip)
                tvStatus.text = "Tracking: active"
                btnToggleTracking.text = "Stop"
                btnToggleTracking.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFFE53935.toInt())
                updateTripStatus()
            }
        }

        // --- Broadcast ---
        val switchBroadcast = findViewById<SwitchMaterial>(R.id.switchBroadcast)
        switchBroadcast.setOnCheckedChangeListener { _, isChecked ->
            BarikoiTrace.setBroadcastingEnabled(isChecked)
            log("Broadcast: $isChecked")
        }

        // --- Trip ---
        updateTripStatus()

        // --- Clear Log ---
        findViewById<MaterialButton>(R.id.btnClearLog).setOnClickListener {
            logBuilder.clear()
            tvLog.text = ""
        }
    }

    private fun buildTraceMode(
        spinner: Spinner,
        inputInterval: TextInputEditText,
        inputDistance: TextInputEditText,
        inputAccuracy: TextInputEditText
    ): TraceMode {
        val selected = spinner.selectedItem.toString()

        when (selected) {
            "ACTIVE" -> return TraceMode.ACTIVE
            "REACTIVE" -> return TraceMode.REACTIVE
            "PASSIVE" -> return TraceMode.PASSIVE
        }

        val interval = inputInterval.text.toString().toIntOrNull() ?: 0
        val distance = inputDistance.text.toString().toIntOrNull() ?: 0
        val accuracy = inputAccuracy.text.toString().toIntOrNull() ?: 100

        return TraceMode.Builder()
            .setUpdateInterval(interval)
            .setDistanceFilter(distance)
            .setAccuracyFilter(accuracy)
            .setDebugModeOn()
            .build()
    }

    private fun updateTripStatus() {
        val tripId = BarikoiTrace.getTripId()
        tvTripStatus.text = if (tripId != null) "On trip: $tripId" else "No active trip"
    }

    private fun log(message: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        val line = "[$time] $message\n"
        Log.d("DemoActivity", message)
        runOnUiThread {
            logBuilder.append(line)
            tvLog.text = logBuilder.toString()
            logScrollView.post { logScrollView.fullScroll(ScrollView.FOCUS_DOWN) }
        }
    }

    private fun toast(msg: String) {
        runOnUiThread { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() }
    }

    private fun requestPermissionsIfNeeded() {
        if (!BarikoiTrace.isLocationPermissionsGranted()) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                10221
            )
        }
        if (!BarikoiTrace.isLocationSettingsOn()) {
            BarikoiTrace.requestLocationServices(this)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 10221) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                log("Location permission granted")
                // Request notification permission after location permission is granted
                BarikoiTrace.requestNotificationPermission(this)
            } else {
                log("Location permission denied")
                toast("Location permission denied")
            }
        } else if (requestCode == 10226) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                log("Notification permission granted")
            } else {
                log("Notification permission denied")
                toast("Notification permission denied")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        BarikoiTrace.setLogListener(null)
    }
}
