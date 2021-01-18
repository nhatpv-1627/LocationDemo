package com.vannhat.locationdemo

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {

    private var locationUpdatesService: LocationUpdatesService? = null
    private var isBounding = false
    private lateinit var btnLocation: Button
    private lateinit var btnCrash: Button
    private lateinit var btnGoToMap: Button
    private lateinit var locationReceiver: LocationReceiver
    private var isTrackingLocation = false
    private lateinit var adapter: LocationAdapter
    private lateinit var rvLocation: RecyclerView

    // firebase analytic
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    private var powerModeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (pm.isDeviceIdleMode) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
                    setTurnScreenOn(true)
                }
            }
        }
    }

    private var serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            locationUpdatesService = (service as LocationUpdatesService.LocalBinder).getService()
            isBounding = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            locationUpdatesService = null
            isBounding = false
        }
    }

    override fun onStart() {
        super.onStart()
        bindService(
            Intent(this, LocationUpdatesService::class.java),
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btnLocation = findViewById(R.id.btnLocation)
        btnGoToMap = findViewById(R.id.btnMap)
        btnCrash = findViewById(R.id.btnCrash)
        rvLocation = findViewById(R.id.rvLocation)
        if (!checkPermission()) {
            requestPermission()
        }
        locationReceiver = LocationReceiver()
        LocalBroadcastManager.getInstance(this).registerReceiver(
            locationReceiver,
            IntentFilter(UPDATE_LOCATION_BROADCAST)
        )
        btnLocation.setOnClickListener {
            if (!checkPermission()) requestPermission()
            else {
                if (isTrackingLocation) removeLocationUpdate()
                else requestLocationUpdate()
            }
        }

        btnCrash.setOnClickListener {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_ITEM) {
                param(FirebaseAnalytics.Param.ITEM_ID, 1)
                param(FirebaseAnalytics.Param.ITEM_NAME, "hello")
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "image")
            }
            // GOTO setting with idle mode
//            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
//            if (powerManager.isIgnoringBatteryOptimizations(packageName)) {
//                Toast.makeText(this, "Ignored", Toast.LENGTH_SHORT).show()
//            } else {
//                startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
//            }
        }

        btnGoToMap.setOnClickListener {
            startActivity(Intent(this, MapActivity::class.java))
        }

        adapter = LocationAdapter(mutableListOf())
        rvLocation.adapter = adapter

        firebaseAnalytics = Firebase.analytics

        registerReceiver(
            powerModeReceiver,
            IntentFilter(PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED)
        )
    }

    private fun removeLocationUpdate() {
        isTrackingLocation = false
        btnLocation.text = getString(R.string.request_location_update)
        locationUpdatesService?.removeLocationUpdate()
    }

    private fun requestLocationUpdate() {
        isTrackingLocation = true
        btnLocation.text = getString(R.string.remove_location_update)
        locationUpdatesService?.requestLocationUpdate()
    }

    fun addLocationToList(newLocation: String) {
        adapter.addToTop(newLocation)
        rvLocation.scrollToPosition(0)
    }

    override fun onStop() {
        if (isBounding) {
            unbindService(serviceConnection)
            isBounding = !isBounding
        }
        super.onStop()
    }

    override fun onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(locationReceiver)
        unregisterReceiver(powerModeReceiver)
        locationUpdatesService?.removeLocationUpdate()
        super.onDestroy()
    }

    private fun requestPermission() {
        val shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        if (shouldProvideRationale) {
            showSnackBar(getString(R.string.ok)) {
                ActivityCompat.requestPermissions(
                    this,
                    listOf(Manifest.permission.ACCESS_FINE_LOCATION).toTypedArray(),
                    PERMISSION_REQUEST_CODE
                )
            }
        } else {
            ActivityCompat.requestPermissions(
                this,
                listOf(Manifest.permission.ACCESS_FINE_LOCATION).toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun showSnackBar(buttonText: String, action: (view: View) -> Unit) {
        Snackbar.make(
            findViewById(R.id.layout_main),
            getString(R.string.permission_rationale),
            Snackbar.LENGTH_INDEFINITE
        ).setAction(buttonText) {
            action(it)
        }.show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            when {
                grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED -> {

                }
                grantResults.isEmpty() -> {
                }
                else -> {
                    showSnackBar(getString(R.string.settings)) {
                        val intent = Intent().apply {
                            val uri = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                            data = uri
                            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        startActivity(intent)
                    }
                }
            }
        }
    }

    private fun checkPermission(): Boolean =
        PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

    inner class LocationReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.getParcelableExtra<Location>(EXTRA_LOCATION)?.let { location ->
                location.distanceTo(location)
                Toast.makeText(
                    this@MainActivity,
                    "Location: (${location.latitude}, ${location.longitude})",
                    Toast.LENGTH_SHORT
                ).show()
                this@MainActivity.addLocationToList(
                    " ${getUpdatedTime(this@MainActivity)} \n ${
                        getLocationText(location)
                    }  \n ${getPlaceName(location, this@MainActivity)}"
                )
            }
        }

    }
}
