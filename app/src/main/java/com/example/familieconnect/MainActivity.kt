package com.example.familieconnect

import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import kotlin.concurrent.thread
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import android.widget.TextView
import android.view.MotionEvent
import android.widget.*
import android.text.method.ScrollingMovementMethod

class MainActivity : ComponentActivity() {

    private val client = OkHttpClient()
    private lateinit var map: MapView
    private lateinit var btnConnected: Button
    private lateinit var txtStatus: android.widget.TextView
    private lateinit var txtMapAttribution: TextView
    private lateinit var btnSos: Button
    private var adminStatusText: TextView? = null
    private var trackingOn = true
    private var sosOn = false
    private val ADMIN_PIN = "1234"
    private val DEFAULT_GROUP_CODE = "1234"
    private val markers = mutableMapOf<String, Marker>()
    private val markerPositions = mutableMapOf<String, GeoPoint>()
    private var connectedOn = false
    private lateinit var nameInput: EditText
    private val zoomedSosDevices = mutableSetOf<String>()

    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private var refreshTask: Runnable? = null
    private var activityAlive = false
    private var zoomIndex = 0
    private var lastTapTime = 0L
    private var followDevice: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityAlive = true
        val prefs =
            getSharedPreferences("tracker", MODE_PRIVATE)

// OSMDroid configuratie
        val osmConfig = Configuration.getInstance()

        osmConfig.load(
            applicationContext,
            getSharedPreferences("osmdroid", MODE_PRIVATE)
        )

        osmConfig.userAgentValue = "FamilieConnect/1.0"

        osmConfig.isDebugMapView = false

        nameInput = EditText(this).apply {
            hint = "Naam"
            setText(
                prefs.getString("device_name", "")
            )
        }


        btnConnected = Button(this).apply {
            text = "Made by\nFScreations ©"
            textSize = 8f

            setCompoundDrawablesWithIntrinsicBounds(
                0,
                R.drawable.logo,
                0,
                0
            )

            setBackgroundColor(Color.TRANSPARENT)
            elevation = 0f
        }
        txtStatus = android.widget.TextView(this).apply {
            text = "..."
            gravity = Gravity.CENTER
            textSize = 12f
            setPadding(12, 8, 12, 8)
            typeface = android.graphics.Typeface.MONOSPACE

            background = roundedButton(Color.rgb(230, 230, 255))
        }
        txtStatus.setOnClickListener {

            val firstName =
                markerPositions.keys.firstOrNull()

            if (firstName != null) {
                val point = markerPositions[firstName]

                if (point != null) {
                    map.controller.animateTo(point)

                }
            }
        }
        val btnConfig = Button(this).apply {
            text = "CONF"
            textSize = 10f
            background = roundedButton(Color.GREEN)
        }
        btnSos = Button(this).apply {
            text = "SOS\nUIT"
            setBackgroundColor(Color.GRAY)
        }

        map = MapView(this).apply {
            setTileSource(MapSources.get(this@MainActivity))
            setMultiTouchControls(true)
            controller.setZoom(16.0)
            controller.setCenter(GeoPoint(51.23, 4.97))
        }

        txtMapAttribution = TextView(this).apply {
            text = MapSources.getAttribution(this@MainActivity)
            textSize = 9f
            setTextColor(Color.DKGRAY)
            setBackgroundColor(Color.argb(190, 255, 255, 255))
            setPadding(8, 4, 8, 4)
        }
        map.setOnTouchListener { _, event ->

            if (event.action == MotionEvent.ACTION_UP) {

                val now = System.currentTimeMillis()

                if (now - lastTapTime < 350) {

                    val names = markerPositions.keys.sorted()

                    if (names.isNotEmpty()) {

                        if (zoomIndex >= names.size) {
                            zoomIndex = 0
                        }

                        val point = markerPositions[names[zoomIndex]]

                        if (point != null) {
                            map.controller.animateTo(point)

                        }

                        zoomIndex++
                    }
                }

                lastTapTime = now
            }

            false
        }
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(30, 80, 30, 30)

            addView(nameInput)

            val mapContainer = android.widget.FrameLayout(this@MainActivity)

            mapContainer.addView(
                map,
                android.widget.FrameLayout.LayoutParams(
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                )
            )
            mapContainer.addView(
                txtMapAttribution,
                android.widget.FrameLayout.LayoutParams(
                    android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                    android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.TOP or Gravity.END
                ).apply {
                    setMargins(20, 20, 20, 20)
                }
            )
            mapContainer.addView(
                btnConnected,
                android.widget.FrameLayout.LayoutParams(
                    280,
                    300,
                    Gravity.BOTTOM or Gravity.START
                ).apply {
                    setMargins(20, 20, 20, 150)
                }
            )

            mapContainer.addView(
                btnConfig,
                FrameLayout.LayoutParams(
                    150,
                    150,
                    Gravity.BOTTOM or Gravity.END
                ).apply {
                    setMargins(20, 20, 190, 140)
                }
            )
            mapContainer.addView(
                btnSos,
                android.widget.FrameLayout.LayoutParams(
                    150,
                    150,
                    Gravity.BOTTOM or Gravity.END
                ).apply {
                    setMargins(20, 20, 20, 140)
                }
            )
            addView(
                mapContainer,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    1f
                )
            )
        }

        setContentView(layout)
        askPermissions()
        nameInput.setOnFocusChangeListener { _, hasFocus ->

            if (!hasFocus) {

                prefs.edit()
                    .putString(
                        "device_name",
                        nameInput.text.toString().trim()
                    )
                    .apply()
            }
        }
        refreshTask = object : Runnable {
            override fun run() {
                val code =
                    prefs.getString("group_code", DEFAULT_GROUP_CODE)
                        ?: DEFAULT_GROUP_CODE

                prefs.edit()
                    .putString("group_code", code)
                    .apply()

                if (code.isNotEmpty()) {
                    loadTrackers(code)
                }

                if (activityAlive) {
                    handler.postDelayed(this, 2000)
                }
            }
        }

        handler.post(refreshTask!!)
        updateButtons()


        btnSos.setOnClickListener {

            sosOn = !sosOn

            if (sosOn && !trackingOn) {
                trackingOn = true
                askPermissions()
            }

            getSharedPreferences("tracker", MODE_PRIVATE)
                .edit()
                .putBoolean("sos_active", sosOn)
                .apply()

            updateButtons()
        }
        btnConfig.setOnClickListener {

            AdminDialog.show(
                AdminDialogContext(
                    activity = this,
                    map = map,
                    statusView = txtStatus,
                    attributionView = txtMapAttribution
                )
            )
        }
    }

    private fun roundedButton(
        color: Int
    ): android.graphics.drawable.GradientDrawable {
        return android.graphics.drawable.GradientDrawable().apply {
            cornerRadius = 40f
            setColor(color)
        }
    }
    fun showAdminDialog()  {
        val prefs = getSharedPreferences("tracker", MODE_PRIVATE)

        var language = prefs.getString("language", "nl") ?: "nl"

        val pinInput = EditText(this).apply {
            hint = "Beheer PIN"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }

        val codeInputAdmin = EditText(this).apply {
            hint = if (language == "en") "Family code" else "Gezinscode"
            setText(prefs.getString("group_code", DEFAULT_GROUP_CODE))
            visibility = android.view.View.GONE
        }

        val mapLabel = TextView(this).apply {
            text = if (language == "en") "Map" else "Kaart"
            textSize = 14f
            setPadding(0, 20, 0, 6)
            visibility = android.view.View.GONE
        }

        val mapSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_dropdown_item,
                MapSources.PROVIDERS.map { it.label }
            )

            val savedProvider = MapSources.getSelectedKey(this@MainActivity)
            val selectedIndex = MapSources.PROVIDERS.indexOfFirst {
                it.key == savedProvider
            }.coerceAtLeast(0)

            setSelection(selectedIndex)
            visibility = android.view.View.GONE
        }

        val adminStatus = TextView(this).apply {
            text = txtStatus.text
            textSize = 13f
            typeface = Typeface.MONOSPACE
            setTextColor(Color.BLUE)
            setPadding(0, 20, 0, 20)
        }

        adminStatusText = adminStatus

        val readmeText = TextView(this).apply {
            textSize = 13f
            setPadding(0, 20, 0, 0)
            movementMethod = ScrollingMovementMethod()
        }

        AdminDialogTexts.refresh(
            language,
            readmeText,
            codeInputAdmin,
            mapLabel
        )
        val btnNL = ImageButton(this).apply {
            setImageResource(R.drawable.flag_nl)
            background = null
            setPadding(8, 8, 8, 8)
            setOnClickListener {
                language = "nl"
                prefs.edit().putString("language", "nl").apply()

                AdminDialogTexts.refresh(
                    language,
                    readmeText,
                    codeInputAdmin,
                    mapLabel
                )
            }
        }

        val btnEN = ImageButton(this).apply {
            setImageResource(R.drawable.flag_en)
            background = null
            setPadding(8, 8, 8, 8)
            setOnClickListener {
                language = "en"
                prefs.edit().putString("language", "en").apply()

                AdminDialogTexts.refresh(
                    language,
                    readmeText,
                    codeInputAdmin,
                    mapLabel
                )
            }
        }

        val langRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            addView(
                btnNL,
                LinearLayout.LayoutParams(90, 90).apply {
                    setMargins(0, 0, 30, 0)
                }
            )

            addView(
                btnEN,
                LinearLayout.LayoutParams(90, 90)
            )
        }

        val scroll = ScrollView(this).apply {
            addView(readmeText)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                500
            )
        }

        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 25, 40, 10)

            addView(langRow)
            addView(pinInput)
            addView(codeInputAdmin)
            addView(mapLabel)
            addView(mapSpinner)
            addView(adminStatus)
            addView(scroll)
        }

        val dialog =
            android.app.AlertDialog.Builder(this)
                .setTitle(if (language == "en") "Admin" else "Beheer")
                .setView(box)
                .setPositiveButton("✖\uFE0F", null)
                .setNegativeButton(if (language == "en") "Cancel" else "❌", null)

                .create()
        dialog.window?.setBackgroundDrawable(
            android.graphics.drawable.ColorDrawable(
                android.graphics.Color.TRANSPARENT
            )
        )

        dialog.setOnShowListener {

            val okButton =
                dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)

            okButton.setOnClickListener {

                Toast.makeText(this, "OK geklikt", Toast.LENGTH_SHORT).show()

                val enteredPin =
                    pinInput.text.toString().trim()

                if (codeInputAdmin.visibility == android.view.View.GONE) {

                    if (enteredPin == ADMIN_PIN) {

                        Toast.makeText(this, "PIN OK", Toast.LENGTH_SHORT).show()

                        pinInput.visibility = android.view.View.GONE
                        codeInputAdmin.visibility = android.view.View.VISIBLE
                        mapLabel.visibility = android.view.View.VISIBLE
                        mapSpinner.visibility = android.view.View.VISIBLE

                        okButton.text =
                            if (language == "en") "Save" else "Opslaan"

                        dialog.setTitle(
                            if (language == "en") "Family code" else "Gezinscode"
                        )

                    } else {
                        pinInput.error =
                            if (language == "en") "Wrong PIN" else "Foute PIN"
                    }

                } else {

                    val newCode =
                        codeInputAdmin.text.toString().trim()

                    val selectedProvider =
                        MapSources.PROVIDERS[mapSpinner.selectedItemPosition].key

                    prefs.edit()
                        .putString("group_code", newCode)
                        .putString("device_name", nameInput.text.toString().trim())
                        .putString(MapSources.PREF_KEY, selectedProvider)
                        .commit()

                    map.setTileSource(MapSources.get(this))
                    txtMapAttribution.text = MapSources.getAttribution(this)
                    map.invalidate()

                    Toast.makeText(
                        this,
                        if (language == "en")
                            "Saved"
                        else
                            "Opgeslagen",
                        Toast.LENGTH_LONG
                    ).show()

                    stopService(Intent(this, TrackingService::class.java))
                    askPermissions()

                    dialog.dismiss()
                }
            }
        }

        dialog.setOnDismissListener {
            adminStatusText = null
        }

        dialog.show()
        dialog.window?.setBackgroundDrawableResource(
            R.drawable.dialog_bg
        )
    }
    private val permissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->

            if (
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
            ) {

                startTracking()
            }
        }

    private fun askPermissions() {

        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        permissionLauncher.launch(
            permissions.toTypedArray()
        )
    }

    private fun startTracking() {

        val prefs =
            getSharedPreferences("tracker", MODE_PRIVATE)

        prefs.edit()
            .putString(
                "device_name",
                nameInput.text.toString().trim()
            )

            .putBoolean(
                "sos_active",
                sosOn
            )
            .apply()

        ContextCompat.startForegroundService(
            this,
            Intent(this, TrackingService::class.java)
        )
    }

    private fun updateButtons() {
        btnSos.background =
            roundedButton(
                if (sosOn)
                    Color.RED
                else
                    Color.GRAY
            )
        btnSos.text = "SOS"

    }

    private fun setConnectionState(connected: Boolean, count: Int = 0) {
        connectedOn = connected

    }

    private fun loadTrackers(code: String) {
        thread {
            try {
                val request = Request.Builder()
                    .url("https://my-tracker-dc65.onrender.com/api/location?pin=$code")
                    .build()

                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    response.close()
                    setConnectionState(false)
                    return@thread
                }

                val body = response.body?.string() ?: "[]"
                response.close()

                val json = JSONArray(body)

                val activeCount =
                    (0 until json.length()).count { i ->
                        val item = json.getJSONObject(i)
                        val active = item.optBoolean("active", false)
                        val time = item.optLong("time", 0L)
                        val age = System.currentTimeMillis() - time
                        active && age < 5 * 60 * 1000
                    }

                setConnectionState(true, activeCount)

                val info = StringBuilder()

                runOnUiThread {
                    map.overlayManager.removeAll(markers.values)
                    markers.clear()

                    for (i in 0 until json.length()) {
                        val item = json.getJSONObject(i)

                        val name = item.optString("device", "Onbekend")
                        val lat = item.optDouble("lat", 0.0)
                        val lng = item.optDouble("lng", 0.0)
                        val active = item.optBoolean("active", false)
                        val time = item.optLong("time", 0L)
                        val age = System.currentTimeMillis() - time
                        val recent = age < 5 * 60 * 1000
                        val expired = age > 999 * 1000
                        val sos = item.optBoolean("sos", false)
                        val accuracy = item.optDouble("accuracy", 999.0)
                        val battery = item.optInt("battery", -1)

                        if (!active) continue
                        if (expired) continue

                        val seconds =
                            kotlin.math.min(age / 1000, 999)

                        val batteryIcon =
                            when {
                                battery >= 50 -> "🟢 accu"
                                battery >= 25 -> "🟠 accu"
                                else -> "🔴 accu"
                            }

                        info.append(
                            String.format(
                                "%-6s %3ds | %3dm | %s\n",
                                name.take(6),
                                seconds,
                                accuracy.toInt(),
                                batteryIcon
                            )
                        )

                        val iconRes = when {
                            accuracy <= 25 -> R.drawable.marker_green
                            accuracy <= 75 -> R.drawable.marker_orange
                            else -> R.drawable.marker_red
                        }

                        if (map.parent == null) {
                            continue
                        }

                        val point = GeoPoint(lat, lng)
                        markerPositions[name] = point

                        if (followDevice == name) {
                            map.controller.animateTo(point)
                        }

                        if (sos && recent && !zoomedSosDevices.contains(name)) {
                            zoomedSosDevices.add(name)
                            map.controller.animateTo(point)
                        }

                        if (!sos) {
                            zoomedSosDevices.remove(name)
                        }

                        val marker = Marker(map).apply {
                            position = point

                            icon = MarkerIconFactory.create(
                                resources = resources,
                                baseDrawableId = iconRes,
                                label = name,
                                sos = sos && recent,
                                follow = followDevice == name
                            )

                            alpha = if (recent) 1.0f else 0.35f
                            title = null
                            snippet = null
                            closeInfoWindow()
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

                            setOnMarkerClickListener { clickedMarker, _ ->
                                followDevice =
                                    if (followDevice == name) null else name

                                map.controller.animateTo(clickedMarker.position)
                                true
                            }
                        }

                        map.overlayManager.add(marker)
                        markers[name] = marker
                    }

                    val status = info.toString()
                    txtStatus.text = status
                    adminStatusText?.text = status

                    map.invalidate()
                    map.postInvalidateDelayed(100)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                setConnectionState(false)
            }
        }
    }


    private fun requestDisableBatteryOptimization() {
        val packageName = packageName

        val intent = Intent(
            android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
        ).apply {
            data = android.net.Uri.parse("package:$packageName")
        }

        startActivity(intent)
    }

    private val README_NL = """
        
README

FamilieConnect is bedoeld voor kleine groepen, typisch een gezin of familie, om elkaar eenvoudig te kunnen volgen en indien nodig snel hulp te bieden.

Iedere groep werkt met een unieke gezinscode. Alleen personen die deze code kennen, kunnen de andere leden van de groep zien.

INSTALLATIE

1. Installeer de app met het APK-bestand.
2. Sta alle gevraagde rechten toe.
3. Schakel batterijoptimalisatie uit. En controleer regelmatig of die nog uit staat.

Dit is belangrijk voor betrouwbare werking op de achtergrond.

GEBRUIK

Met FamilieConnect kunt U.

• Kijken waar de groepsleden zich exact bevinden.
• Iemand in nood? Druk op Uw toestel op 'SOS' om een noodsignaal uit te sturen.
• Dubbeltik op de kaart om naar de marker van een lid te navigeren. Een tweede keer gaat naar het volgende lid enz...
• Uw kind gaat op reis met de bus. Tik op haar marker en rij met haar mee.

MARKERS

Groen = actuele en zeer nauwkeurige positie.
Oranje = minder nauwkeurig, maar bruikbaar.
Rood = laatste bekende positie. Het lid is daar geweest, maar nu mss niet meer.
geen marker te zien, dan staat het toestel van dit lid uit of heeft platte batterij.

installeer de app.
open de app en breng Uw naam in.
IN CONF, vervang de default beheercode door uw eigen code en sla op.
Maak een gezinscode en sla op.
deze code breng U in, in de toestellen van alle leden. Zonder beheercp-ode, kan niemand deze wijzigen.

FamilieConnect bewaart geen routegeschiedenis.
Alleen de laatst bekende positie wordt gebruikt.
""".trimIndent()

    private val README_EN = """
README

FamilieConnect is designed for small groups, typically a family, to easily keep track of each other and provide quick assistance when needed.

Each group uses a unique family code. Only people who know this code can see the other members of the group.

INSTALLATION

1. Install the app using the APK file.
2. Grant all requested permissions.
3. Disable battery optimization and check regularly that it remains disabled.

This is important to ensure reliable background operation.

USING FAMILIECONNECT

With FamilieConnect you can:

• See the exact location of all group members.
• In an emergency, press the SOS button on your device to send an alert to all group members.
• Double-tap the map to navigate to a member's marker. Double-tap again to move to the next member, and so on.
• Is your child travelling by bus on a school trip? Tap their marker and follow their journey in real time.

MARKERS

Green = current and highly accurate position.

Orange = less accurate, but still reliable and usable.

Red = last known position. The member was there recently, but may no longer be there now.

No marker visible = the member's device is switched off or the battery is empty.

SETUP

1. Install the app.
2. Open the app and enter your name.
3. Cick on CONF and replace the default administrator PIN with your own PIN and save it.
4. Create a family code and save it.
5. Enter the same family code on the devices of all group members.

Without the administrator PIN, nobody can change these settings.

PRIVACY

FamilieConnect does not store route history.

Only the most recent known position is used.

""".trimIndent()
}