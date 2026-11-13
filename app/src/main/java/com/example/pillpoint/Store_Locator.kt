package com.example.pillpoint

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class Store_Locator : AppCompatActivity() {

    private lateinit var mapWebView: WebView
    private lateinit var etLocationSearch: EditText
    private lateinit var btnSearchLocation: Button

    private val locationPermissionRequest = 1001

    private val firestore = FirebaseFirestore.getInstance()

    private var pharmacyListener: ListenerRegistration? = null

    private var pharmacies = mutableListOf<Pharmacy>()

    private var mapReady = false

    private var currentLat = -26.2041
    private var currentLon = 28.0473

    private var currentLocationLabel = "Your Location"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_store_locator)

        etLocationSearch = findViewById(R.id.etLocationSearch)
        btnSearchLocation = findViewById(R.id.btnSearchLocation)
        mapWebView = findViewById(R.id.mapWebView)

        setupWebView()

        // Listen for pharmacy changes in Firestore.
        loadPharmaciesFromFirestore()

        // Request location permission.
        checkLocationPermissionAndLoad()

        btnSearchLocation.setOnClickListener {

            val locationName =
                etLocationSearch.text.toString().trim()

            if (locationName.isEmpty()) {

                Toast.makeText(
                    this,
                    "Please enter an area or city",
                    Toast.LENGTH_SHORT
                ).show()

            } else {

                searchLocation(locationName)

            }
        }
    }

    // ---------------------------------------------------------
    // WEBVIEW AND LEAFLET MAP
    // ---------------------------------------------------------

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {

        mapWebView.settings.javaScriptEnabled = true
        mapWebView.settings.domStorageEnabled = true

        mapWebView.addJavascriptInterface(
            WebAppInterface(this),
            "AndroidBridge"
        )

        mapWebView.webViewClient = object : WebViewClient() {

            override fun onPageFinished(
                view: WebView?,
                url: String?
            ) {

                super.onPageFinished(view, url)

                mapReady = true

                // Display pharmacies after the map loads.
                updateMapWithPharmacies()
            }
        }

        val leafletHtml = """
            <!DOCTYPE html>
            <html>
            <head>

                <meta name="viewport"
                    content="width=device-width, initial-scale=1.0">

                <link rel="stylesheet"
                    href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"/>

                <style>

                    body, html {
                        margin: 0;
                        padding: 0;
                        height: 100%;
                        width: 100%;
                    }

                    #map {
                        height: 100%;
                        width: 100%;
                    }

                    .pharmacy-popup {
                        min-width: 160px;
                        font-family: Arial, sans-serif;
                    }

                    .select-button {
                        background: #003049;
                        color: white;
                        padding: 8px 12px;
                        border: none;
                        border-radius: 4px;
                        margin-top: 8px;
                        cursor: pointer;
                    }

                </style>

            </head>

            <body>

                <div id="map"></div>

                <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>

                <script>

                    var map = L.map('map').setView(
                        [-26.2041, 28.0473],
                        13
                    );

                    L.tileLayer(
                        'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',
                        {
                            maxZoom: 19,
                            attribution: '&copy; OpenStreetMap contributors'
                        }
                    ).addTo(map);

                    var markersLayer = L.layerGroup().addTo(map);

                    // Update the map using Firestore pharmacy data.
                    function updateMap(lat, lon, label, pharmaciesJson) {

                        map.setView([lat, lon], 13);

                        markersLayer.clearLayers();

                        // Add current location marker.
                        L.marker([lat, lon])
                            .addTo(markersLayer)
                            .bindPopup(label);

                        try {

                            var pharmacies = JSON.parse(pharmaciesJson);

                            pharmacies.forEach(function(p) {

                                if (
                                    typeof p.lat !== "number" ||
                                    typeof p.lon !== "number"
                                ) {
                                    return;
                                }

                                var marker = L.marker([
                                    p.lat,
                                    p.lon
                                ]).addTo(markersLayer);

                                // Create popup safely.
                                var popup = document.createElement("div");

                                popup.className = "pharmacy-popup";

                                var title = document.createElement("strong");

                                title.textContent = p.name;

                                var address = document.createElement("p");

                                address.textContent = p.address;

                                var phone = document.createElement("p");

                                phone.textContent =
                                    "Phone: " + (p.phone || "Not provided");

                                var hours = document.createElement("p");

                                hours.textContent =
                                    "Operating hours: " +
                                    (p.operatingHours || "Not provided");

                                var button = document.createElement("button");

                                button.className = "select-button";

                                button.textContent = "Select Store";

                                button.addEventListener("click", function() {

                                    AndroidBridge.onPharmacySelected(
                                        p.id,
                                        p.name,
                                        p.address,
                                        p.phone || ""
                                    );

                                });

                                popup.appendChild(title);
                                popup.appendChild(address);
                                popup.appendChild(phone);
                                popup.appendChild(hours);
                                popup.appendChild(button);

                                marker.bindPopup(popup);

                            });

                        } catch (error) {

                            console.error(
                                "Error displaying pharmacies:",
                                error
                            );

                        }

                    }

                </script>

            </body>
            </html>
        """.trimIndent()

        mapWebView.loadDataWithBaseURL(
            "https://localhost",
            leafletHtml,
            "text/html",
            "UTF-8",
            null
        )
    }

    // ---------------------------------------------------------
    // FIRESTORE PHARMACY COLLECTION
    // ---------------------------------------------------------

    private fun loadPharmaciesFromFirestore() {

        pharmacyListener = firestore
            .collection("pharmacies")
            .addSnapshotListener { snapshot, error ->

                if (error != null) {

                    Toast.makeText(
                        this,
                        "Unable to load pharmacies: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()

                    return@addSnapshotListener
                }

                if (snapshot == null) return@addSnapshotListener

                pharmacies.clear()

                for (document in snapshot.documents) {

                    val isActive = document.getBoolean("isActive")

                    // Ignore pharmacies marked inactive.
                    if (isActive == false) {
                        continue
                    }

                    val latitude = getNumber(
                        document.get("latitude")
                            ?: document.get("lat")
                    )

                    val longitude = getNumber(
                        document.get("longitude")
                            ?: document.get("lng")
                    )

                    // A pharmacy must have valid coordinates
                    // to appear as a map marker.
                    if (
                        latitude == null ||
                        longitude == null ||
                        latitude !in -90.0..90.0 ||
                        longitude !in -180.0..180.0
                    ) {
                        continue
                    }

                    val pharmacy = Pharmacy(

                        id = document.id,

                        name = document.getString("name")
                            ?: document.getString("pharmacyName")
                            ?: "Unnamed Pharmacy",

                        address = document.getString("address")
                            ?: "Address not available",

                        phone = document.getString("phone")
                            ?: "Not provided",

                        email = document.getString("email")
                            ?: "Not provided",

                        province = document.getString("province")
                            ?: "",

                        city = document.getString("city")
                            ?: "",

                        operatingHours =
                            document.getString("operatingHours")
                                ?: document.getString("openingHours")
                                ?: "Not provided",

                        latitude = latitude,

                        longitude = longitude

                    )

                    pharmacies.add(pharmacy)
                }

                // Refresh map when Firestore changes.
                updateMapWithPharmacies()

                Toast.makeText(
                    this,
                    "${pharmacies.size} pharmacies loaded",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    // Supports Firestore Double, Long and Integer values.
    private fun getNumber(value: Any?): Double? {

        return when (value) {

            is Number -> value.toDouble()

            is String -> value.toDoubleOrNull()

            else -> null
        }
    }

    // ---------------------------------------------------------
    // UPDATE LEAFLET MAP
    // ---------------------------------------------------------

    private fun updateMapWithPharmacies() {

        if (!mapReady) return

        val jsonArray = JSONArray()

        pharmacies.forEach { pharmacy ->

            val obj = JSONObject().apply {

                put("id", pharmacy.id)

                put("name", pharmacy.name)

                put("address", pharmacy.address)

                put("phone", pharmacy.phone)

                put("email", pharmacy.email)

                put("province", pharmacy.province)

                put("city", pharmacy.city)

                put("operatingHours", pharmacy.operatingHours)

                put("lat", pharmacy.latitude)

                put("lon", pharmacy.longitude)
            }

            jsonArray.put(obj)
        }

        val script = """
            updateMap(
                $currentLat,
                $currentLon,
                ${JSONObject.quote(currentLocationLabel)},
                ${JSONObject.quote(jsonArray.toString())}
            );
        """.trimIndent()

        mapWebView.evaluateJavascript(script, null)
    }

    // ---------------------------------------------------------
    // LOCATION PERMISSIONS
    // ---------------------------------------------------------

    private fun checkLocationPermissionAndLoad() {

        val finePermission =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            )

        val coarsePermission =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )

        if (
            finePermission == PackageManager.PERMISSION_GRANTED ||
            coarsePermission == PackageManager.PERMISSION_GRANTED
        ) {

            fetchUserLocation()

        } else {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                locationPermissionRequest
            )
        }
    }

    private fun fetchUserLocation() {

        val manager =
            getSystemService(LOCATION_SERVICE) as LocationManager

        try {

            val location =
                manager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: manager.getLastKnownLocation(
                        LocationManager.NETWORK_PROVIDER
                    )

            if (location != null) {

                currentLat = location.latitude
                currentLon = location.longitude

                currentLocationLabel = "Your Location"

                updateMapWithPharmacies()
            }

        } catch (_: SecurityException) {

            Toast.makeText(
                this,
                "Location permission is required",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {

        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (requestCode == locationPermissionRequest) {

            if (
                grantResults.any {
                    it == PackageManager.PERMISSION_GRANTED
                }
            ) {

                fetchUserLocation()

            } else {

                Toast.makeText(
                    this,
                    "Location permission denied. Showing default map location.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // ---------------------------------------------------------
    // SEARCH LOCATION USING NOMINATIM
    // ---------------------------------------------------------

    private fun searchLocation(locationName: String) {

        lifecycleScope.launch {

            val coords = withContext(Dispatchers.IO) {

                var connection: HttpURLConnection? = null

                try {

                    val encodedName =
                        URLEncoder.encode(locationName, "UTF-8")

                    val url = URL(
                        "https://nominatim.openstreetmap.org/search" +
                                "?q=$encodedName&format=json&limit=1&countrycodes=za"
                    )

                    connection =
                        url.openConnection() as HttpURLConnection

                    connection.requestMethod = "GET"

                    connection.connectTimeout = 15000

                    connection.readTimeout = 15000

                    connection.setRequestProperty(
                        "User-Agent",
                        "PillPointDelivery/1.0"
                    )

                    if (connection.responseCode == 200) {

                        val reader = BufferedReader(
                            InputStreamReader(connection.inputStream)
                        )

                        val response = StringBuilder()

                        var line: String?

                        while (
                            reader.readLine().also { line = it } != null
                        ) {
                            response.append(line)
                        }

                        reader.close()

                        val jsonArray =
                            JSONArray(response.toString())

                        if (jsonArray.length() > 0) {

                            val obj = jsonArray.getJSONObject(0)

                            val lat =
                                obj.getString("lat").toDouble()

                            val lon =
                                obj.getString("lon").toDouble()

                            Pair(lat, lon)

                        } else {

                            null
                        }

                    } else {

                        null
                    }

                } catch (_: Exception) {

                    null

                } finally {

                    connection?.disconnect()
                }
            }

            if (coords == null) {

                Toast.makeText(
                    this@Store_Locator,
                    "Location not found",
                    Toast.LENGTH_SHORT
                ).show()

            } else {

                currentLat = coords.first

                currentLon = coords.second

                currentLocationLabel = locationName

                updateMapWithPharmacies()

            }
        }
    }

    // ---------------------------------------------------------
    // SELECT PHARMACY
    // ---------------------------------------------------------

    inner class WebAppInterface(
        private val activity: Activity
    ) {

        @JavascriptInterface
        fun onPharmacySelected(
            id: String,
            name: String,
            address: String,
            phone: String
        ) {

            activity.runOnUiThread {

                val resultIntent = Intent().apply {

                    putExtra("SELECTED_PHARMACY_ID", id)

                    putExtra("SELECTED_PHARMACY_NAME", name)

                    putExtra("SELECTED_PHARMACY_ADDRESS", address)

                    putExtra("SELECTED_PHARMACY_PHONE", phone)
                }

                activity.setResult(
                    Activity.RESULT_OK,
                    resultIntent
                )

                Toast.makeText(
                    activity,
                    "Selected: $name",
                    Toast.LENGTH_SHORT
                ).show()

                activity.finish()
            }
        }
    }

    // ---------------------------------------------------------
    // PHARMACY DATA MODEL
    // ---------------------------------------------------------

    data class Pharmacy(

        val id: String,

        val name: String,

        val address: String,

        val phone: String,

        val email: String,

        val province: String,

        val city: String,

        val operatingHours: String,

        val latitude: Double,

        val longitude: Double
    )

    // Remove Firestore listener when Activity closes.
    override fun onDestroy() {

        pharmacyListener?.remove()

        mapWebView.removeJavascriptInterface("AndroidBridge")

        mapWebView.destroy()

        super.onDestroy()
    }
}