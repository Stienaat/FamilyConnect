package com.example.familieconnect

data class Tracker(
    val name: String,
    val lat: Double,
    val lng: Double,
    val accuracy: Double,
    val battery: Int,
    val active: Boolean,
    val sos: Boolean,
    val time: Long
)

