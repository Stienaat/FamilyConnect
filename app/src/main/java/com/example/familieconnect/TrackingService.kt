package com.example.familieconnect

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.pm.PackageManager
import android.os.IBinder
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import kotlin.concurrent.thread
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager

class TrackingService : Service() {

    private val client = OkHttpClient()
    private lateinit var fused: FusedLocationProviderClient
    private lateinit var callback: LocationCallback

    private val SERVER_URL =
        "https://my-tracker-dc65.onrender.com/api/location"

    override fun    onCreate() {
        super.onCreate()

        fused = LocationServices.getFusedLocationProviderClient(this)

        createNotificationChannel()
        startForeground(1, createNotification())

        startLocationUpdates()
    }

    private fun startLocationUpdates() {
        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5000L

        )
            .setMinUpdateDistanceMeters(0f)
            .build()

        callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return

                val prefs =
                    getSharedPreferences("tracker", MODE_PRIVATE)

                val deviceName =
                    prefs.getString("device_name", "Onbekend") ?: "Onbekend"

                val groupCode =
                    prefs.getString("group_code", "1234")
                val sosActive =
                    prefs.getBoolean("sos_active", false)


                val batteryIntent =
                    registerReceiver(
                        null,
                        IntentFilter(Intent.ACTION_BATTERY_CHANGED)
                    )

                val level =
                    batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1

                val scale =
                    batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1

                val batteryPercent =
                    if (level >= 0 && scale > 0)
                        (level * 100 / scale)
                    else
                        -1

                //             val powerManager =
                //                 getSystemService(POWER_SERVICE) as PowerManager
                //
                //            val batteryOptimized =
                //                !powerManager.isIgnoringBatteryOptimizations(packageName)
                val json = JSONObject().apply {
                    put("device", deviceName)
                    put("group", groupCode)
                    put("lat", location.latitude)
                    put("lng", location.longitude)
                    put("accuracy", location.accuracy)
                    put("time", System.currentTimeMillis())
                    put("sos", sosActive)
                    put("active", true)
                    put("battery", batteryPercent)
                    put("batteryOptimized", false)

                }

                postLocation(json)
            }
        }

        if (
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            stopSelf()
            return
        }

        fused.requestLocationUpdates(
            request,
            callback,
            mainLooper
        )
    }

    private fun postLocation(json: JSONObject) {
        android.util.Log.d("FAMILIE", "POST $json")
        thread {
            try {
                val body =
                    json.toString()
                        .toRequestBody("application/json".toMediaType())

                val request =
                    Request.Builder()
                        .url(SERVER_URL)
                        .post(body)
                        .build()

                val response =
                    client.newCall(request).execute()

                android.util.Log.d(
                    "FAMILIE",
                    "SERVER ${response.code}"
                )

                response.close()

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            "tracking",
            "FamilyTracker locatie",
            NotificationManager.IMPORTANCE_LOW
        )

        val manager =
            getSystemService(NotificationManager::class.java)

        manager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, "tracking")
            .setContentTitle("FamilyTracker actief")
            .setContentText("Locatie wordt gedeeld")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()

        if (::callback.isInitialized) {
            fused.removeLocationUpdates(callback)
        }
    }

    override fun onBind(intent: android.content.Intent?): IBinder? = null
}
