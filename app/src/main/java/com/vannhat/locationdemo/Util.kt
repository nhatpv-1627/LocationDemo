package com.vannhat.locationdemo

import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.widget.Toast
import androidx.core.content.ContextCompat
import java.text.DateFormat
import java.util.*

const val PERMISSION_REQUEST_CODE = 66
const val UPDATE_LOCATION_BROADCAST = "UPDATE_LOCATION_BROADCAST"
const val EXTRA_LOCATION = "55"

const val KEY_CAMERA_POSITION = "camera_position"
const val KEY_LOCATION = "location"

fun getLocationText(location: Location?): String =
    location?.let { return "Coordinates: (${location.latitude}, ${location.longitude})" }
        ?: "Unknown location"

fun getUpdatedTime(context: Context) = context.getString(
    R.string.location_updated, DateFormat.getDateTimeInstance().format(
        Date()
    )
)

fun  getPlaceName(location: Location?, context: Context): String? {
    if (location == null) return "Unknown location"
    val geocoder = Geocoder(context, Locale.getDefault())
    return try {
        val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
        "Place: ${addresses.firstOrNull()?.getAddressLine(0) ?: " Unknown"}"
    } catch (e: Exception) {
        Toast.makeText(context, "$e", Toast.LENGTH_SHORT).show()
        null
    }

}

fun hasPermissions(context: Context, vararg permissions: String): Boolean {
    for (permission in permissions) {
        if (ContextCompat.checkSelfPermission(
                context,
                permission
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
    }

    return true
}
