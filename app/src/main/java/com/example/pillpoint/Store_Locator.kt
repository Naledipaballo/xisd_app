package com.example.pillpoint

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

class Store_Locator : AppCompatActivity() {

    private lateinit var mapWebView: WebView
    private lateinit var etLocationSearch: EditText
    private lateinit var btnSearchLocation: Button

    private val locationPermissionRequest = 1001
    private var currentLat = -26.2041
    private var currentLon = 28.0473 // Johannesburg fallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_store_locator)

        etLocationSearch = findViewById(R.id.etLocationSearch)
        btnSearchLocation = findViewById(R.id.btnSearchLocation)
        mapWebView = findViewById(R.id.mapWebView)

        setupWebView()
        checkLocationPermissionAndLoad()

        btnSearchLocation.setOnClickListener {
            val locationName = etLocationSearch.text.toString().trim()
            if (locationName.isEmpty()) {
                Toast.makeText(this, "Please enter an area or city", Toast.LENGTH_SHORT).show()
            } else {
                searchLocation(locationName)
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        mapWebView.settings.javaScriptEnabled = true
        mapWebView.settings.domStorageEnabled = true
        mapWebView.addJavascriptInterface(WebAppInterface(this), "AndroidBridge")
        mapWebView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                loadMapAtLocation(currentLat, currentLon, "Initial Location")
            }
        }

        // Load Leaflet CSS and JS via CDN directly in the WebView HTML
        val leafletHtml = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
                <style>
                    body, html { margin: 0; padding: 0; height: 100%; width: 100%; }
                    #map { height: 100%; width: 100%; }
                </style>
            </head>
            <body>
                <div id="map"></div>
                <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
                <script>
                    var map = L.map('map').setView([-26.2041, 28.0473], 13);
                    L.tileLayer('https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png', {
                        maxZoom: 19,
                        attribution: '&copy; OpenStreetMap contributors &copy; CARTO'
                    }).addTo(map);

                    var markersLayer = L.layerGroup().addTo(map);

                    function updateMap(lat, lon, label, pharmaciesJson) {
                        map.setView([lat, lon], 14);
                        markersLayer.clearLayers();

                        // Add Center Marker
                        L.marker([lat, lon]).addTo(markersLayer)
                            .bindPopup("<b>" + label + "</b>");

                        // Add Pharmacy Markers
                        var pharmacies = JSON.parse(pharmaciesJson);
                        pharmacies.forEach(function(p) {
                            var marker = L.marker([p.lat, p.lon]).addTo(markersLayer);
                            marker.bindPopup("<b>" + p.name + "</b><br>" + p.address + "<br><br><button onclick=\"selectPharmacy('" + p.name.replace(/'/g, "\\'") + "', '" + p.address.replace(/'/g, "\\'") + "')\" style='background:#0E2F56;color:white;padding:5px 10px;border:none;border-radius:4px;'>Select Store</button>");
                        });
                    }

                    function selectPharmacy(name, address) {
                        AndroidBridge.onPharmacySelected(name, address);
                    }
                </script>
            </body>
            </html>
        """.trimIndent()

        mapWebView.loadDataWithBaseURL("https://localhost", leafletHtml, "text/html", "UTF-8", null)
    }

    private fun checkLocationPermissionAndLoad() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fetchUserLocation()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                locationPermissionRequest
            )
        }
    }

    private fun fetchUserLocation() {
        val manager = getSystemService(LOCATION_SERVICE) as LocationManager
        try {
            val location = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            if (location != null) {
                currentLat = location.latitude
                currentLon = location.longitude
                loadMapAtLocation(currentLat, currentLon, "Your Location")
            }
        } catch (_: SecurityException) { }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == locationPermissionRequest && grantResults.any { it == PackageManager.PERMISSION_GRANTED }) {
            fetchUserLocation()
        }
    }

    private fun loadMapAtLocation(lat: Double, lon: Double, label: String) {
        lifecycleScope.launch {
            val pharmacies = withContext(Dispatchers.IO) {
                queryNearbyPharmacies(lat, lon)
            }

            val jsonArray = org.json.JSONArray()
            pharmacies.forEach { p ->
                val obj = JSONObject().apply {
                    put("name", p.name)
                    put("address", p.address)
                    put("lat", p.latitude)
                    put("lon", p.longitude)
                }
                jsonArray.put(obj)
            }

            val escapedLabel = label.replace("'", "\\'")
            val script = "updateMap($lat, $lon, '$escapedLabel', '${jsonArray.toString().replace("'", "\\'")}');"

            withContext(Dispatchers.Main) {
                mapWebView.evaluateJavascript(script, null)
            }
        }
    }

    private fun searchLocation(locationName: String) {
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    val geocoder = Geocoder(this@Store_Locator, Locale.getDefault())
                    @Suppress("DEPRECATION")
                    val addresses: List<Address>? = geocoder.getFromLocationName(locationName, 1)
                    addresses?.firstOrNull()
                } catch (_: Exception) { null }
            }
            if (result == null) {
                Toast.makeText(this@Store_Locator, "Location not found", Toast.LENGTH_SHORT).show()
            } else {
                currentLat = result.latitude
                currentLon = result.longitude
                loadMapAtLocation(currentLat, currentLon, locationName)
            }
        }
    }

    private fun queryNearbyPharmacies(latitude: Double, longitude: Double): List<Pharmacy> {
        val query = """
            [out:json][timeout:25];
            (node[amenity=pharmacy](around:50000,$latitude,$longitude);way[amenity=pharmacy](around:50000,$latitude,$longitude);relation[amenity=pharmacy](around:50000,$latitude,$longitude););out center tags;
        """.trimIndent()

        var connection: HttpURLConnection? = null
        return try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            connection = URL("https://overpass-api.de/api/interpreter?data=$encoded").openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 20000
            connection.setRequestProperty("User-Agent", "PillPointDeliveryApp/1.0")

            if (connection.responseCode !in 200..299) return emptyList()

            val response = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
            val elements = JSONObject(response).optJSONArray("elements") ?: return emptyList()
            val result = mutableListOf<Pharmacy>()

            for (i in 0 until elements.length()) {
                val item = elements.optJSONObject(i) ?: continue
                val tags = item.optJSONObject("tags") ?: JSONObject()
                val center = item.optJSONObject("center")
                val lat = if (item.has("lat")) item.optDouble("lat", Double.NaN) else center?.optDouble("lat", Double.NaN) ?: Double.NaN
                val lon = if (item.has("lon")) item.optDouble("lon", Double.NaN) else center?.optDouble("lon", Double.NaN) ?: Double.NaN
                if (lat.isNaN() || lon.isNaN()) continue

                val name = tags.optString("name").ifBlank { tags.optString("brand").ifBlank { "Pharmacy" } }
                val address = listOf(
                    tags.optString("addr:housenumber"),
                    tags.optString("addr:street"),
                    tags.optString("addr:suburb")
                ).filter { it.isNotBlank() }.joinToString(", ")

                result.add(Pharmacy(name, address, lat, lon))
            }
            result.distinctBy { "${it.name}|${it.latitude}|${it.longitude}" }.take(50)
        } catch (_: Exception) { emptyList() }
        finally { connection?.disconnect() }
    }

    // JavaScript bridge to handle clicks on the Leaflet popup buttons
    inner class WebAppInterface(private val activity: Activity) {
        @JavascriptInterface
        fun onPharmacySelected(name: String, address: String) {
            activity.runOnUiThread {
                val resultIntent = Intent().apply {
                    putExtra("SELECTED_PHARMACY_NAME", name)
                    putExtra("SELECTED_PHARMACY_ADDRESS", address)
                }
                activity.setResult(Activity.RESULT_OK, resultIntent)
                Toast.makeText(activity, "Selected: $name", Toast.LENGTH_SHORT).show()
                activity.finish()
            }
        }
    }

    data class Pharmacy(val name: String, val address: String, val latitude: Double, val longitude: Double)
}