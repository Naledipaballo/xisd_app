package com.example.pillpoint

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.Gravity
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class Prescriptions : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
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
    private lateinit var ivProfile: ImageView

    private lateinit var prescriptionsContainer: LinearLayout

    private val db: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }

    private var selectedFileUri: Uri? = null
    private var isSubmitting = false

    // Allows the user to choose a PDF, JPEG or PNG.
    private val filePickerLauncher =
        registerForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri: Uri? ->

            if (uri != null) {
                selectedFileUri = uri

                btnChooseFile.text =
                    getFileName(uri) ?: "Prescription selected"
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_prescriptions)

        auth = FirebaseAuth.getInstance()

        if (auth.currentUser == null) {
            redirectToLogin()
            return
        }

        // Initialize the existing layout.
        drawerLayout = findViewById(R.id.drawerLayout)
        btnMenu = findViewById(R.id.btnMenu)
        navView = findViewById(R.id.navView)
        ivProfile = findViewById(R.id.ivProfile)

        etPatientName = findViewById(R.id.etPatientName)
        etDoctorName = findViewById(R.id.etDoctorName)
        etPrescriptionNumber =
            findViewById(R.id.etPrescriptionNumber)
        etPrescriptionDate =
            findViewById(R.id.etPrescriptionDate)
        etAdditionalNotes =
            findViewById(R.id.etAdditionalNotes)

        spinnerDeliveryMethod =
            findViewById(R.id.spinnerDeliveryMethod)

        btnChooseFile = findViewById(R.id.btnChooseFile)
        btnSubmitPrescription =
            findViewById(R.id.btnSubmitPrescription)
        tvNoPrescriptions =
            findViewById(R.id.tvNoPrescriptions)

        // Create a container for prescription rows.
        // The existing tvNoPrescriptions remains the empty-state view.
        prescriptionsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val parent = tvNoPrescriptions.parent as? LinearLayout

        if (parent != null) {
            val index = parent.indexOfChild(tvNoPrescriptions)
            parent.addView(
                prescriptionsContainer,
                index + 1
            )
        }

        setupDeliverySpinner()
        setupDrawerHeader()
        setupNavigationDrawer()

        ivProfile.setOnClickListener {
            startActivity(Intent(this, MyAccount::class.java))
        }

        btnChooseFile.setOnClickListener {
            filePickerLauncher.launch(
                arrayOf(
                    "application/pdf",
                    "image/jpeg",
                    "image/png"
                )
            )
        }

        btnSubmitPrescription.setOnClickListener {
            submitPrescription()
        }

        loadPrescriptions()
    }

    override fun onResume() {
        super.onResume()

        if (::auth.isInitialized && auth.currentUser != null) {
            loadPrescriptions()
        }
    }

    private fun setupDeliverySpinner() {
        val methods = arrayOf(
            "Select Method",
            "Home Delivery",
            "Store Pickup"
        )

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            methods
        )

        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spinnerDeliveryMethod.adapter = adapter
    }

    private fun submitPrescription() {
        if (isSubmitting) return

        val user = auth.currentUser

        if (user == null) {
            Toast.makeText(
                this,
                "Please log in first.",
                Toast.LENGTH_LONG
            ).show()
            redirectToLogin()
            return
        }

        val patient = etPatientName.text.toString().trim()
        val doctor = etDoctorName.text.toString().trim()
        val rxNumber =
            etPrescriptionNumber.text.toString().trim()
        val date = etPrescriptionDate.text.toString().trim()
        val notes = etAdditionalNotes.text.toString().trim()
        val method = spinnerDeliveryMethod.selectedItem.toString()
        val uri = selectedFileUri

        if (
            patient.isEmpty() ||
            doctor.isEmpty() ||
            rxNumber.isEmpty() ||
            date.isEmpty() ||
            method == "Select Method"
        ) {
            Toast.makeText(
                this,
                "Please complete all required fields.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if (uri == null) {
            Toast.makeText(
                this,
                "Please select a prescription file.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val mimeType = contentResolver.getType(uri)

        if (
            mimeType !in listOf(
                "application/pdf",
                "image/jpeg",
                "image/png"
            )
        ) {
            Toast.makeText(
                this,
                "Please select a PDF, JPG or PNG file.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val fileName =
            getFileName(uri) ?: "prescription_file"

        // Metadata only: this does not upload the actual file.
        val record = hashMapOf<String, Any>(
            "userId" to user.uid,
            "patientName" to patient,
            "doctorName" to doctor,
            "prescriptionNumber" to rxNumber,
            "prescriptionDate" to date,
            "additionalNotes" to notes,
            "deliveryMethod" to method,
            "fileName" to fileName,
            "fileType" to (mimeType ?: "unknown"),
            "fileUploaded" to false,
            "status" to "Pending file upload",
            "createdAt" to FieldValue.serverTimestamp()
        )

        isSubmitting = true
        btnSubmitPrescription.isEnabled = false
        btnSubmitPrescription.text = "Saving details..."

        db.collection("prescriptions")
            .add(record)
            .addOnSuccessListener { document ->

                isSubmitting = false
                btnSubmitPrescription.isEnabled = true
                btnSubmitPrescription.text =
                    "Save Prescription Details"

                tvNoPrescriptions.text =
                    "Prescription details saved. " +
                            "The actual file has not been uploaded."

                Toast.makeText(
                    this,
                    "Saved record: ${document.id}",
                    Toast.LENGTH_LONG
                ).show()

                // Reset form after a successful save.
                etPatientName.text.clear()
                etDoctorName.text.clear()
                etPrescriptionNumber.text.clear()
                etPrescriptionDate.text.clear()
                etAdditionalNotes.text.clear()
                spinnerDeliveryMethod.setSelection(0)

                selectedFileUri = null
                btnChooseFile.text = "Choose file"

                loadPrescriptions()
            }
            .addOnFailureListener { error ->

                isSubmitting = false
                btnSubmitPrescription.isEnabled = true
                btnSubmitPrescription.text =
                    "Save Prescription Details"

                Toast.makeText(
                    this,
                    "Could not save: " +
                            (error.message ?: "Unknown error"),
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun loadPrescriptions() {
        val user = auth.currentUser ?: return

        db.collection("prescriptions")
            .whereEqualTo("userId", user.uid)
            .orderBy(
                "createdAt",
                Query.Direction.DESCENDING
            )
            .get()
            .addOnSuccessListener { result ->

                prescriptionsContainer.removeAllViews()

                if (result.isEmpty) {
                    tvNoPrescriptions.visibility =
                        android.view.View.VISIBLE
                    tvNoPrescriptions.text =
                        "No prescription records found."
                    return@addOnSuccessListener
                }

                tvNoPrescriptions.visibility =
                    android.view.View.GONE

                for (document in result.documents) {
                    val patient =
                        document.getString("patientName") ?: "—"
                    val doctor =
                        document.getString("doctorName") ?: "—"
                    val method =
                        document.getString("deliveryMethod") ?: "—"
                    val date =
                        document.getString("prescriptionDate") ?: "—"
                    val status =
                        document.getString("status") ?: "Unknown"
                    val fileName =
                        document.getString("fileName") ?: "—"

                    addPrescriptionRow(
                        patient,
                        doctor,
                        method,
                        date,
                        status,
                        fileName
                    )
                }
            }
            .addOnFailureListener { error ->
                tvNoPrescriptions.visibility =
                    android.view.View.VISIBLE
                tvNoPrescriptions.text =
                    "Could not load records. Check connection and Firestore rules."

                Toast.makeText(
                    this,
                    error.message ?: "Failed to load prescriptions",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun addPrescriptionRow(
        patient: String,
        doctor: String,
        method: String,
        date: String,
        status: String,
        fileName: String
    ) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(6, 10, 6, 10)
            setBackgroundColor(android.graphics.Color.WHITE)
        }

        val values = listOf(
            patient,
            doctor,
            method,
            date,
            status,
            fileName
        )

        values.forEach { value ->
            val cell = TextView(this).apply {
                text = value
                textSize = 10f
                gravity = Gravity.CENTER
                setPadding(3, 4, 3, 4)
                maxLines = 3
                setTextColor(
                    android.graphics.Color.DKGRAY
                )

                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            }

            row.addView(cell)
        }

        prescriptionsContainer.addView(row)
    }

    private fun setupDrawerHeader() {
        if (navView.headerCount == 0) return

        val header = navView.getHeaderView(0)

        val txtEmail =
            header.findViewById<TextView>(R.id.txtEmail)
        val txtUserName =
            header.findViewById<TextView>(R.id.txtUserName)

        val user = auth.currentUser

        val email = user?.email
            ?: SessionManager.email(this)
                .ifEmpty {
                    intent.getStringExtra("USER_EMAIL").orEmpty()
                }

        txtEmail?.text = email.ifEmpty { "user@email.com" }

        val name = user?.displayName

        txtUserName?.text =
            if (!name.isNullOrBlank()) {
                "Welcome, ${name.substringBefore(" ")}"
            } else {
                "Welcome"
            }
    }

    private fun setupNavigationDrawer() {
        btnMenu.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
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
                }

                R.id.nav_prescription -> {
                    // Already on this screen.
                }

                R.id.nav_reminders -> {
                    startActivity(Intent(this, Reminder::class.java))
                }

                R.id.nav_store -> {
                    startActivity(
                        Intent(this, Store_Locator::class.java)
                    )
                }

                R.id.nav_track -> {
                    startActivity(
                        Intent(this, TrackOrder::class.java)
                    )
                }

                R.id.nav_my_orders -> {
                    startActivity(
                        Intent(this, MyOrders::class.java)
                    )
                }

                R.id.nav_logout -> {
                    logoutUser()
                }
            }

            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun getFileName(uri: Uri): String? {
        var result: String? = null

        if (uri.scheme == "content") {
            contentResolver.query(
                uri,
                null,
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(
                        OpenableColumns.DISPLAY_NAME
                    )

                    if (index >= 0) {
                        result = cursor.getString(index)
                    }
                }
            }
        }

        return result
            ?: uri.lastPathSegment?.substringAfterLast('/')
    }

    private fun logoutUser() {
        SessionManager.logout(this)
        auth.signOut()
        redirectToLogin()
    }

    private fun redirectToLogin() {
        val intent = Intent(this, Login::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        startActivity(intent)
        finish()
    }
}