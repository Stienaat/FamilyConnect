package com.example.familieconnect

import android.widget.TextView
import org.osmdroid.views.MapView

data class AdminDialogContext(
    val activity: MainActivity,
    val map: MapView,
    val statusView: TextView,
    val attributionView: TextView
)