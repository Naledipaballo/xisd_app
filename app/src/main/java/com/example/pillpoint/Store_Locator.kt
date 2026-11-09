package com.example.pillpoint

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
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
    private var currentLat = -26.2041
    private var currentLon = 28.0473 // Fallback coordinates

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
                loadMapWithPharmacies(currentLat, currentLon, "Your Location")
            }
        }

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
                    
                    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                        maxZoom: 19,
                        attribution: '&copy; OpenStreetMap contributors'
                    }).addTo(map);

                    var markersLayer = L.layerGroup().addTo(map);

                    function updateMap(lat, lon, label, pharmaciesJson) {
                        map.setView([lat, lon], 14);
                        markersLayer.clearLayers();

                        // Add Center Location Marker
                        L.marker([lat, lon]).addTo(markersLayer)
                            .bindPopup("<b>" + label + "</b>").openPopup();

                        // Render Pharmacy Markers
                        try {
                            var pharmacies = JSON.parse(pharmaciesJson);
                            pharmacies.forEach(function(p) {
                                var marker = L.marker([p.lat, p.lon]).addTo(markersLayer);
                                var popupContent = "<b>" + p.name + "</b><br>" + p.address + 
                                    "<br><br><button onclick=\"selectPharmacy('" + p.name.replace(/'/g, "\\'") + "', '" + p.address.replace(/'/g, "\\'") + "')\" style='background:#0E2F56;color:white;padding:6px 12px;border:none;border-radius:4px;font-weight:bold;'>Select Store</button>";
                                marker.bindPopup(popupContent);
                            });
                        } catch(e) {
                            console.error(e);
                        }
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
                loadMapWithPharmacies(currentLat, currentLon, "Your Location")
            }
        } catch (_: SecurityException) { }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == locationPermissionRequest && grantResults.any { it == PackageManager.PERMISSION_GRANTED }) {
            fetchUserLocation()
        }
    }

    private fun loadMapWithPharmacies(lat: Double, lon: Double, label: String) {
        lifecycleScope.launch {
            val pharmacies = withContext(Dispatchers.IO) {
                fetchPharmaciesFromOverpass(lat, lon)
            }

            val jsonArray = JSONArray()
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
            val jsonString = jsonArray.toString().replace("'", "\\'")
            val script = "updateMap($lat, $lon, '$escapedLabel', '$jsonString');"

            withContext(Dispatchers.Main) {
                mapWebView.evaluateJavascript(script, null)
            }
        }
    }

    private fun fetchPharmaciesFromOverpass(lat: Double, lon: Double): List<Pharmacy> {
        val query = """
            [out:json][timeout:25];
            (
              node["amenity"="pharmacy"](around:50000,$lat,$lon);
              way["amenity"="pharmacy"](around:50000,$lat,$lon);
              node["shop"="chemist"](around:50000,$lat,$lon);
              way["shop"="chemist"](around:50000,$lat,$lon);
              node["healthcare"="pharmacy"](around:50000,$lat,$lon);
            );
            out center tags;
        """.trimIndent()

        var connection: HttpURLConnection? = null
        return try {
            val url = URL("https://overpass-api.de/api/interpreter?data=" + URLEncoder.encode(query, "UTF-8"))
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 20000
            connection.setRequestProperty("User-Agent", "PillPointDelivery/1.0")

            if (connection.responseCode != 200) return emptyList()

            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val response = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                response.append(line)
            }
            reader.close()

            val root = JSONObject(response.toString())
            val elements = root.optJSONArray("elements") ?: return emptyList()
            val list = mutableListOf<Pharmacy>()

            for (i in 0 until elements.length()) {
                val item = elements.getJSONObject(i)
                val tags = item.optJSONObject("tags") ?: JSONObject()
                val center = item.optJSONObject("center")

                val pLat = item.optDouble("lat", center?.optDouble("lat", Double.NaN) ?: Double.NaN)
                val pLon = item.optDouble("lon", center?.optDouble("lon", Double.NaN) ?: Double.NaN)
                if (pLat.isNaN() || pLon.isNaN()) continue

                val name = tags.optString("name").ifBlank {
                    tags.optString("brand").ifBlank { "Pharmacy" }
                }
                val street = tags.optString("addr:street")
                val suburb = tags.optString("addr:suburb").ifBlank { tags.optString("addr:city") }
                val address = listOf(street, suburb).filter { it.isNotBlank() }.joinToString(", ").ifBlank { name }

                list.add(Pharmacy(name, address, pLat, pLon))
            }
            list.distinctBy { "${it.latitude},${it.longitude}" }
        } catch (_: Exception) {
            emptyList()
        } finally {
            connection?.disconnect()
        }
    }

    // Uses OpenStreetMap's Nominatim Web API for ultra-reliable address searching
    private fun searchLocation(locationName: String) {
        lifecycleScope.launch {
            val coords = withContext(Dispatchers.IO) {
                try {
                    val encodedName = URLEncoder.encode(locationName, "UTF-8")
                    val url = URL("https://nominatim.openstreetmap.org/search?q=$encodedName&format=json&limit=1")
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.setRequestProperty("User-Agent", "PillPointDelivery/1.0")

                    if (connection.responseCode == 200) {
                        val reader = BufferedReader(InputStreamReader(connection.inputStream))
                        val response = StringBuilder()
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            response.append(line)
                        }
                        reader.close()

                        val jsonArray = JSONArray(response.toString())
                        if (jsonArray.length() > 0) {
                            val obj = jsonArray.getJSONObject(0)
                            val lat = obj.getString("lat").toDouble()
                            val lon = obj.getString("lon").toDouble()
                            Pair(lat, lon)
                        } else {
                            null
                        }
                    } else {
                        null
                    }
                } catch (_: Exception) {
                    null
                }
            }

            if (coords == null) {
                Toast.makeText(this@Store_Locator, "Location not found", Toast.LENGTH_SHORT).show()
            } else {
                currentLat = coords.first
                currentLon = coords.second
                loadMapWithPharmacies(currentLat, currentLon, locationName)
            }
        }
    }

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