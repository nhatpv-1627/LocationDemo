package com.vannhat.locationdemo

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.location.Location
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.location.*

class LocationUpdatesService : Service() {

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private var location: Location? = null
    private lateinit var notificationManager: NotificationManager
    private var changingConfiguration = false
    private var binder = LocalBinder()
    private lateinit var locationCallback: LocationCallback

    override fun onCreate() {
        super.onCreate()
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult?) {
                super.onLocationResult(result)
                onNewLocation(result?.lastLocation)
            }
        }

        createLocationRequest()
        getLastLocation()

        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.app_name),
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            )
        }
    }

    @SuppressLint("MissingPermission")
    fun requestLocationUpdate() {
        startService(Intent(applicationContext, LocationUpdatesService::class.java))
        fusedLocationProviderClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.myLooper()
        )
    }

    fun removeLocationUpdate() {
        try {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback)
            stopSelf()
        } catch (e: SecurityException) {
            Log.d("ccccc", "Lost permission $e")
        }

    }

    private fun onNewLocation(lastLocation: Location?) {
        location = lastLocation

        //Notify to Activity
        LocalBroadcastManager.getInstance(applicationContext)
            .sendBroadcast(Intent(UPDATE_LOCATION_BROADCAST).also {
                it.putExtra(
                    EXTRA_LOCATION,
                    lastLocation
                )
            })

        // update notification if foreground service is running
        if (isServiceRunningInForeground(this))
            notificationManager.notify(NOTIFICATION_ID, getNotification())

    }

    private fun isServiceRunningInForeground(context: Context): Boolean {
        // getRunningServices was deprecated, but it's still functioning in the app internal
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (javaClass.name == service.service.className && service.foreground)
                return true
        }
        return false
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        changingConfiguration = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun getLastLocation() {
        fusedLocationProviderClient.lastLocation.addOnCompleteListener { task ->
            if (task.isSuccessful && task.result != null) location = task.result
            else Log.d("cccccc", "Failed to get location")
        }
    }

    private fun createLocationRequest() {
        locationRequest = LocationRequest().apply {
            interval = UPDATE_INTERVAL_IN_MILLISECONDS
            fastestInterval = FASTED_UPDATE_INTERVAL_IN_MILLISECONDS
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        // run app
        changingConfiguration = false
        stopForeground(true)
        return binder
    }

    override fun onRebind(intent: Intent?) {
        // Come here when app turns from background (Home app) to foreground (show activity)
        // We'll stop foreground service and hide notification
        changingConfiguration = false
        stopForeground(true)
        super.onRebind(intent)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        // Come here when app running background (Home app).
        // We will run foreground service with notification
        if (!changingConfiguration)
            startForeground(NOTIFICATION_ID, getNotification())
        return true
    }

    private fun getNotification(): Notification {
        val builder = NotificationCompat.Builder(this).apply {
            setContentText(getLocationText(location))
                .setContentTitle(getUpdatedTime(this@LocationUpdatesService))
                .setOngoing(true)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setWhen(System.currentTimeMillis())
                .priority =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                    NotificationManager.IMPORTANCE_HIGH
                else Notification.PRIORITY_HIGH

        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(CHANNEL_ID)
        }
        return builder.build()
    }

    inner class LocalBinder : Binder() {
        fun getService() = this@LocationUpdatesService
    }

    companion object {
        private const val UPDATE_INTERVAL_IN_MILLISECONDS = 5000L
        private const val FASTED_UPDATE_INTERVAL_IN_MILLISECONDS = 1000L
        private const val TAG = "LocationUpdatesService"
        private const val CHANNEL_ID = "c1"
        private const val NOTIFICATION_ID = 68
    }
}
