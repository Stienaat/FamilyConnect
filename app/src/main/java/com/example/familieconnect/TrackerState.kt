package com.example.familieconnect

import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker

class TrackerState {

    val markers = mutableMapOf<String, Marker>()

    val markerPositions = mutableMapOf<String, GeoPoint>()

    val zoomedSosDevices = mutableSetOf<String>()

    var followDevice: String? = null
}