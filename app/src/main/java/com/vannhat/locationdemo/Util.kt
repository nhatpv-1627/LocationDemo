package com.vannhat.locationdemo

import android.content.Context
import android.location.Location
import java.text.DateFormat
import java.util.*

const val PERMISSION_REQUEST_CODE = 66
const val UPDATE_LOCATION_BROADCAST = "UPDATE_LOCATION_BROADCAST"
const val EXTRA_LOCATION = "55"

fun getLocationText(location: Location?): String =
    location?.let { return "Coordinates(${location.latitude},${location.longitude})" }
        ?: "Unknown location"

fun getUpdatedTime(context: Context) = context.getString(
    R.string.location_updated, DateFormat.getDateTimeInstance().format(
        Date()
    )
)
