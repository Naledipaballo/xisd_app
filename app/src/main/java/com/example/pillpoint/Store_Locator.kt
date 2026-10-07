package com.example.pillpoint

import android.Manifest
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
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
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

class Store_Locator : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var etLocationSearch: EditText
    private lateinit var btnSearchLocation: Button
    private var myLocationOverlay: MyLocationNewOverlay? = null

    private val locationPermissionRequest = 1001
    private val defaultPoint = GeoPoint(-26.2041, 28.0473) // Johannesburg fallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance().userAgentValue = "$packageName/1.0"
        setContentView(R.layout.activity_store_locator)

        etLocationSearch = findViewById(R.id.etLocationSearch)
        btnSearchLocation = findViewById(R.id.btnSearchLocation)
        mapView = findViewById(R.id.map)

        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(13.0)
        mapView.controller.setCenter(defaultPoint)

        setupLocationOverlay()
        loadLocationAndPharmacies(defaultPoint, "Johannesburg")

        btnSearchLocation.setOnClickListener {
            val locationName = etLocationSearch.text.toString().trim()
            if (locationName.isEmpty()) {
                Toast.makeText(this, "Please enter an area or city", Toast.LENGTH_SHORT).show()
            } else {
                searchLocation(locationName)
            }
        }
    }

    private fun setupLocationOverlay() {
        if (!hasLocationPermission()) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                locationPermissionRequest
            )
            return
        }
        enableMyLocation()
    }

    private fun enableMyLocation() {
        if (myLocationOverlay != null) return
        val overlay = MyLocationNewOverlay(GpsMyLocationProvider(this), mapView)
        overlay.enableMyLocation()
        overlay.enableFollowLocation()
        mapView.overlays.add(overlay)
        myLocationOverlay = overlay

        val location = getLastKnownLocation()
        if (location != null) {
            val point = GeoPoint(location.latitude, location.longitude)
            loadLocationAndPharmacies(point, "Your Location")
        }
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun getLastKnownLocation(): Location? {
        val manager = getSystemService(LOCATION_SERVICE) as LocationManager
        var best: Location? = null
        try {
            for (provider in manager.getProviders(true)) {
                val location = manager.getLastKnownLocation(provider) ?: continue
                if (best == null || location.accuracy < best!!.accuracy) best = location
            }
        } catch (_: SecurityException) { }
        return best
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == locationPermissionRequest && grantResults.any { it == PackageManager.PERMISSION_GRANTED }) {
            enableMyLocation()
        }
    }

    private fun loadLocationAndPharmacies(center: GeoPoint, label: String) {
        mapView.overlays.removeAll { it is Marker }
        val centerMarker = Marker(mapView).apply {
            position = center
            title = label
            snippet = "Searching for nearby pharmacies..."
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
        mapView.overlays.add(centerMarker)
        mapView.controller.animateTo(center)
        mapView.controller.setZoom(14.0)
        mapView.invalidate()
        loadNearbyPharmacies(center)
    }

    private fun searchLocation(locationName: String) {
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    val geocoder = Geocoder(this@Store_Locator, Locale.getDefault())
                    @Suppress("DEPRECATION")
                    val addresses: List<Address>? = geocoder.getFromLocationName(locationName, 1)
                    addresses?.firstOrNull()?.let { GeoPoint(it.latitude, it.longitude) }
                } catch (_: Exception) { null }
            }
            if (result == null) {
                Toast.makeText(this@Store_Locator, "Location not found", Toast.LENGTH_SHORT).show()
            } else {
                loadLocationAndPharmacies(result, locationName)
            }
        }
    }

    private fun loadNearbyPharmacies(center: GeoPoint) {
        lifecycleScope.launch {
            val pharmacies = withContext(Dispatchers.IO) {
                queryNearbyPharmacies(center.latitude, center.longitude)
            }
            if (pharmacies.isEmpty()) {
                Toast.makeText(this@Store_Locator, "No pharmacies found within 5 km. Try another area.", Toast.LENGTH_LONG).show()
                return@launch
            }
            pharmacies.forEach { pharmacy ->
                val marker = Marker(mapView).apply {
                    position = GeoPoint(pharmacy.latitude, pharmacy.longitude)
                    title = pharmacy.name
                    snippet = pharmacy.address.ifBlank { "Nearby pharmacy" }
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
                mapView.overlays.add(marker)
            }
            mapView.invalidate()
            Toast.makeText(this@Store_Locator, "Found ${pharmacies.size} nearby pharmacies", Toast.LENGTH_SHORT).show()
        }
    }

    private fun queryNearbyPharmacies(latitude: Double, longitude: Double): List<Pharmacy> {
        val query = """
            [out:json][timeout:15];
            (node[amenity=pharmacy](around:5000,$latitude,$longitude);way[amenity=pharmacy](around:5000,$latitude,$longitude);relation[amenity=pharmacy](around:5000,$latitude,$longitude););out center tags;
        """.trimIndent()
        var connection: HttpURLConnection? = null
        return try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            connection = URL("https://overpass-api.de/api/interpreter?data=$encoded").openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 15000
            connection.setRequestProperty("User-Agent", "$packageName PharmacyLocator/1.0")
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
                val name = tags.optString("name").ifBlank { "Nearby Pharmacy" }
                val address = listOf(tags.optString("addr:housenumber"), tags.optString("addr:street"), tags.optString("addr:suburb"))
                    .filter { it.isNotBlank() }.joinToString(", ")
                result.add(Pharmacy(name, address, lat, lon))
            }
            result.distinctBy { "${it.name}|${it.latitude}|${it.longitude}" }.take(30)
        } catch (_: Exception) { emptyList() }
        finally { connection?.disconnect() }
    }

    override fun onResume() { super.onResume(); if (::mapView.isInitialized) mapView.onResume() }
    override fun onPause() { if (::mapView.isInitialized) mapView.onPause(); super.onPause() }
    override fun onDestroy() {
        myLocationOverlay?.disableMyLocation()
        if (::mapView.isInitialized) mapView.onDetach()
        super.onDestroy()
    }

    data class Pharmacy(val name: String, val address: String, val latitude: Double, val longitude: Double)
}
