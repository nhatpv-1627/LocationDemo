package com.vannhat.locationdemo

import android.content.Context
import android.location.Location
import java.text.DateFormat
import java.util.*

const val PERMISSION_REQUEST_CODE = 66
const val UPDATE_LOCATION_BROADCAST = "UPDATE_LOCATION_BROADCAST"
const val EXTRA_LOCATION = "55"
const val GEOFENCING_RADIUS_IN_METTER = 100

fun getLocationText(location: Location?): String =
    location?.let { return "Coordinates(${location.latitude},${location.longitude})" }
        ?: "Unknown location"

fun getUpdatedTime(context: Context) = context.getString(
    R.string.location_updated, DateFormat.getDateTimeInstance().format(
        Date()
    )
)

fun getSampleGeofencesData() = listOf(
    GeofenceData(1, "Cau Phu Loc",
        Location("").apply {
            latitude = 16.07584903025737
            longitude = 108.17844492250079
        }),
    GeofenceData(2, "Hung Gia Tran",
        Location("").apply {
            latitude = 16.078877199318306
            longitude = 108.17130636940668
        }),
    GeofenceData(3, "Petrolimex 27",
        Location("").apply {
            latitude = 16.087776975305175
            longitude = 108.15422459587711
        }),
    GeofenceData(4, "cau Thuan Phuoc",
        Location("").apply {
            latitude = 16.09594900284843
            longitude = 108.22215575083074
        }),GeofenceData(4, "Nearby",
        Location("").apply {
            latitude = 16.095694384638893
            longitude = 108.22206833048557
        })
)

fun classifyGeofences(
    geofences: List<GeofenceData>,
    currentLocation: Location?
): Pair<List<GeofenceData>, List<GeofenceData>>? {
    if (currentLocation == null) return null
    val enteredList = mutableListOf<GeofenceData>()
    val exitList = mutableListOf<GeofenceData>()
    geofences.forEach {
        if (currentLocation.distanceTo(it.location) <= GEOFENCING_RADIUS_IN_METTER)
            enteredList.add(it)
        else
            exitList.add(it)
    }

    return Pair(enteredList, exitList)
}

