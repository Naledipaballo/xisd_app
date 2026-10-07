package com.example.pillpoint

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView

class Prescriptions : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var btnMenu: ImageButton
    private lateinit var navView: NavigationView

    private lateinit var etPatientName: EditText
    private lateinit var etDoctorName: EditText
    private lateinit var etPrescriptionNumber: EditText
    private lateinit var etPrescriptionDate: EditText
    private lateinit var etAdditionalNotes: EditText
    private lateinit var spinnerDeliveryMethod: Spinner
    private lateinit var btnChooseFile: Button
    private lateinit var btnSubmitPrescription: Button
    private lateinit var tvNoPrescriptions: TextView

    private var selectedFileUri: Uri? = null

    // File picker launcher for PDF, JPG, PNG
    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            selectedFileUri = uri
            val fileName = getFileName(uri)
            btnChooseFile.text = fileName ?: "File Selected"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_prescriptions)

        // Initialize navigation drawer. The prescription layout includes these views.
        drawerLayout = findViewById(R.id.drawerLayout)
        btnMenu = findViewById(R.id.btnMenu)
        navView = findViewById(R.id.navView)

        // Initialize Form Inputs
        etPatientName = findViewById(R.id.etPatientName)
        etDoctorName = findViewById(R.id.etDoctorName)
        etPrescriptionNumber = findViewById(R.id.etPrescriptionNumber)
        etPrescriptionDate = findViewById(R.id.etPrescriptionDate)
        etAdditionalNotes = findViewById(R.id.etAdditionalNotes)
        spinnerDeliveryMethod = findViewById(R.id.spinnerDeliveryMethod)
        btnChooseFile = findViewById(R.id.btnChooseFile)
        btnSubmitPrescription = findViewById(R.id.btnSubmitPrescription)
        tvNoPrescriptions = findViewById(R.id.tvNoPrescriptions)

        // Setup Delivery Method Spinner
        val deliveryMethods = arrayOf("Select Method", "Home Delivery", "Store Pickup")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, deliveryMethods)
        spinnerDeliveryMethod.adapter = adapter

        // Setup Hamburger Menu Button
        btnMenu.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // Setup Drawer Menu item navigation
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_dashboard -> {
                    startActivity(Intent(this, Home::class.java))
                    finish()
                }
                R.id.nav_shop -> {
                    startActivity(Intent(this, Shop::class.java))
                }
                R.id.nav_cart -> {
                    startActivity(Intent(this, Cart::class.java))
                }
                R.id.nav_account -> {
                    startActivity(Intent(this, MyAccount::class.java))
                    finish()
                }
                R.id.nav_prescription -> {
                    // Already here
                }
                R.id.nav_reminders -> {
                    startActivity(Intent(this, Reminder::class.java))
                }
                R.id.nav_store -> {
                    startActivity(Intent(this, Store_Locator::class.java))
                }
                R.id.nav_track -> {
                    startActivity(Intent(this, TrackOrder::class.java))
                }
                R.id.nav_logout -> {
                    SessionManager.logout(this)
                    val intent = Intent(this, login::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                    finish()
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        // Handle File Chooser Click
        btnChooseFile.setOnClickListener {
            filePickerLauncher.launch("*/*")
        }

        // Handle Prescription Submission
        btnSubmitPrescription.setOnClickListener {
            val patient = etPatientName.text.toString().trim()
            val doctor = etDoctorName.text.toString().trim()
            val rxNumber = etPrescriptionNumber.text.toString().trim()
            val date = etPrescriptionDate.text.toString().trim()
            val method = spinnerDeliveryMethod.selectedItem.toString()

            if (patient.isEmpty() || doctor.isEmpty() || rxNumber.isEmpty() || date.isEmpty() || method == "Select Method") {
                Toast.makeText(this, "Please fill in all required fields and select a delivery method.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Simulating a successful upload entry update
            tvNoPrescriptions.text = "$patient | $doctor | $method | $date | Pending | Uploaded"
            Toast.makeText(this, "Prescription uploaded successfully!", Toast.LENGTH_SHORT).show()
        }
    }

    // Helper method to retrieve uploaded file name from URI
    private fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor.use {
                if (it != null && it.moveToFirst()) {
                    val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        result = it.getString(index)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != -1) {
                if (cut != null) {
                    result = result?.substring(cut + 1)
                }
            }
        }
        return result
    }
}