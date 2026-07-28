package com.barikoi.barikoitracesdkapp

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.ActivityCompat
import com.barikoi.barikoitrace.BarikoiTrace
import com.barikoi.barikoitrace.TraceMode
import com.barikoi.barikoitrace.model.TraceError
import com.barikoi.barikoitrace.model.TraceUser
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private lateinit var switchService: SwitchCompat
    private lateinit var spinnertype: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        BarikoiTrace.initialize(this, "API_KEY")

        BarikoiTrace.requestNotificationPermission(this)
        if (!BarikoiTrace.isLocationPermissionsGranted()) {
            BarikoiTrace.requestLocationPermissions(this)
        }
        if (!BarikoiTrace.isLocationSettingsOn()) {
            BarikoiTrace.requestLocationServices(this)
        }

        // URL configuration
        val baseurlform = findViewById<EditText>(R.id.input_base_url)
        val mqtturlform = findViewById<EditText>(R.id.input_mqtt_url)
        baseurlform.setText("https://api.trace.bmapsbd.com/api/v1/")
        mqtturlform.setText("tcp://broker.trace.bmapsbd.com:1883")

        findViewById<Button>(R.id.button_seturl).setOnClickListener {
            if (baseurlform.text.toString().isNotEmpty())
                BarikoiTrace.setBaseUrl(baseurlform.text.toString())
            if (mqtturlform.text.toString().isNotEmpty())
                BarikoiTrace.setMqttUrl(mqtturlform.text.toString())
            Toast.makeText(this, "url set", Toast.LENGTH_SHORT).show()
        }

        // User setup
        val tvUsername = findViewById<EditText>(R.id.tvUserName)
        tvUsername.setText("01879989798")

        switchService = findViewById(R.id.switchService)
        val tagloc = findViewById<FloatingActionButton>(R.id.fab)
        val setUserBtn = findViewById<Button>(R.id.button_set_user)

        setUserBtn.setOnClickListener {
            if (BarikoiTrace.isOnTrip()) {
                Toast.makeText(this, "cannot change user mid journey!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            BarikoiTrace.setOrCreateUser(
                "Miraz", null, tvUsername.text.toString(),
                object : BarikoiTrace.TraceUserCallback {
                    override fun onSuccess(traceUser: TraceUser) {
                        Toast.makeText(
                            this@MainActivity,
                            "user set: ${traceUser.name} ${traceUser.userId}",
                            Toast.LENGTH_SHORT
                        ).show()
                        tvUsername.setText(traceUser.phone)
                        BarikoiTrace.startTracking(
                            TraceMode.Builder().setUpdateInterval(10).build()
                        )
                    }

                    override fun onFailure(error: TraceError) {
                        val user = BarikoiTrace.getUser()
                        if (user?.phone == tvUsername.text.toString()) {
                            Toast.makeText(
                                this@MainActivity,
                                "user not found online, found in local storage",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(this@MainActivity, error.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }

        // Broadcast switch
        val switchBroadcast = findViewById<SwitchCompat>(R.id.switchBroadcast)
        switchBroadcast.setOnCheckedChangeListener { _, isChecked ->
            BarikoiTrace.setBroadcastingEnabled(isChecked)
        }

        val user = BarikoiTrace.getUser()
        if (user?.phone != null) {
            tvUsername.setText(user.phone)
        } else {
            Toast.makeText(this, "User not set, please fill in the phone number", Toast.LENGTH_SHORT).show()
        }

        BarikoiTrace.setOfflineTracking(true)

        tagloc.setOnClickListener {
            BarikoiTrace.updateCurrentLocation(object : BarikoiTrace.TraceLocationUpdateCallback {
                override fun onLocationUpdate(location: Location) {
                    Toast.makeText(
                        this@MainActivity,
                        "Location Tagged, service running: ${BarikoiTrace.isLocationTracking()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onFailure(error: TraceError) {}
            })
        }

        // Type spinner
        val types = arrayOf("NONE", "ACTIVE", "REACTIVE", "PASSIVE")
        spinnertype = findViewById(R.id.spinnerType)
        val aa = ArrayAdapter(this, android.R.layout.simple_spinner_item, types)
        aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnertype.adapter = aa

        if (BarikoiTrace.isOnTrip()) {
            Log.d("locationupdate", "already running no need to start again")
            switchService.isChecked = true
        }

        BarikoiTrace.checkAppServicePermission(this)

        switchService.setOnCheckedChangeListener { compoundButton, isChecked ->
            if (!compoundButton.isPressed) return@setOnCheckedChangeListener

            if (isChecked) {
                var mode: TraceMode? = null
                val uiText = findViewById<EditText>(R.id.input_updateinterval)
                val dfText = findViewById<EditText>(R.id.input_distancefilter)
                val afText = findViewById<EditText>(R.id.input_accuracy)
                val ui = uiText.text.toString().toIntOrNull() ?: 0
                val df = dfText.text.toString().toIntOrNull() ?: 0
                val af = afText.text.toString().toIntOrNull() ?: 0

                val tb = TraceMode.Builder()
                if (ui > 0) {
                    tb.setUpdateInterval(ui)
                    tb.setPingSyncInterval(ui * 3)
                }
                if (df > 0) tb.setDistanceFilter(df)
                if (af > 0) tb.setAccuracyFilter(af)

                if (spinnertype.selectedItem != "NONE") {
                    mode = when (spinnertype.selectedItem.toString()) {
                        "ACTIVE" -> TraceMode.ACTIVE
                        "REACTIVE" -> TraceMode.REACTIVE
                        "PASSIVE" -> TraceMode.PASSIVE
                        else -> tb.build()
                    }
                    BarikoiTrace.startTracking(mode)
                }

                if (BarikoiTrace.isOnTrip() || BarikoiTrace.isLocationTracking()) {
                    Log.d("locationupdate", "already running")
                    Toast.makeText(
                        applicationContext,
                        "trip already running!! no need to start again",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    tb.setDebugModeOn()
                    if (mode == null) mode = tb.build()
                    if (BarikoiTrace.isLocationPermissionsGranted()) {
                        BarikoiTrace.startTracking(mode)
                    } else {
                        switchService.isChecked = false
                        Toast.makeText(this, "location permission denied", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                BarikoiTrace.stopTracking()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.top_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_demo -> {
                startActivity(android.content.Intent(this, DemoActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            10221 -> {
                if (grantResults.isNotEmpty() &&
                    grantResults.all { it == PackageManager.PERMISSION_GRANTED }
                ) {
                    Toast.makeText(this, "Location permission granted", Toast.LENGTH_SHORT).show()
                    if (!BarikoiTrace.isLocationSettingsOn()) {
                        BarikoiTrace.requestLocationServices(this)
                    }
                } else {
                    Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(
                            this, Manifest.permission.ACCESS_FINE_LOCATION
                        )
                    ) {
                        // User checked "Don't ask again" — open app settings
                        val intent = android.content.Intent(
                            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            android.net.Uri.fromParts("package", packageName, null)
                        )
                        startActivity(intent)
                    }
                }
            }
            10222 -> {
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    Toast.makeText(this, "Background location permission granted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Background location permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
