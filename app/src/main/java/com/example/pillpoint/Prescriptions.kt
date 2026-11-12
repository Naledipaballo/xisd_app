package com.example.pillpoint

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.Gravity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.Locale

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

    // Allows the customer to select a PDF, JPG or PNG.
    // The selected file is NOT uploaded because Storage is unavailable.
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

        initializeViews()

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

    private fun initializeViews() {

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
            findViewById(R.id.etAdditionalNo)

        spinnerDeliveryMethod =
            findViewById(R.id.spinnerDeliveryMethod)

        btnChooseFile = findViewById(R.id.btnChooseFile)

        btnSubmitPrescription =
            findViewById(R.id.btnSubmitPrescription)

        tvNoPrescriptions =
            findViewById(R.id.tvNoPrescriptions)

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

    // Save prescription information in Firestore.
    private fun submitPrescription() {

        if (isSubmitting) return

        val user = auth.currentUser

        if (user == null) {
            redirectToLogin()
            return
        }

        val patient = etPatientName.text.toString().trim()
        val doctor = etDoctorName.text.toString().trim()

        val rxNumber =
            etPrescriptionNumber.text.toString().trim()

        val date =
            etPrescriptionDate.text.toString().trim()

        val notes =
            etAdditionalNotes.text.toString().trim()

        val method =
            spinnerDeliveryMethod.selectedItem.toString()

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

        var mimeType = "Not uploaded"
        var fileName = "Not uploaded"

        // We only record the selected file's information.
        // No actual file is sent to Firebase.
        if (uri != null) {

            mimeType =
                contentResolver.getType(uri) ?: "unknown"

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

            fileName =
                getFileName(uri) ?: "prescription_file"
        }

        val record = hashMapOf<String, Any>(
            "userId" to user.uid,
            "patientName" to patient,
            "doctorName" to doctor,
            "prescriptionNumber" to rxNumber,
            "prescriptionDate" to date,
            "additionalNo" to notes,
            "deliveryMethod" to method,
            "fileName" to fileName,
            "fileType" to mimeType,
            "fileUpload" to false,
            "status" to "Pending",
            "createdAt" to FieldValue.serverTimestamp()
        )

        isSubmitting = true

        btnSubmitPrescription.isEnabled = false
        btnSubmitPrescription.text = "Saving..."

        db.collection("prescriptions")
            .add(record)
            .addOnSuccessListener { document ->

                isSubmitting = false

                btnSubmitPrescription.isEnabled = true
                btnSubmitPrescription.text =
                    "Save Prescription Details"

                Toast.makeText(
                    this,
                    "Prescription details saved successfully.",
                    Toast.LENGTH_LONG
                ).show()

                resetForm()

                loadPrescriptions()
            }
            .addOnFailureListener { error ->

                isSubmitting = false

                btnSubmitPrescription.isEnabled = true
                btnSubmitPrescription.text =
                    "Save Prescription Details"

                Toast.makeText(
                    this,
                    "Could not save prescription: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    // Clear the form after saving.
    private fun resetForm() {

        etPatientName.text.clear()
        etDoctorName.text.clear()
        etPrescriptionNumber.text.clear()
        etPrescriptionDate.text.clear()
        etAdditionalNotes.text.clear()

        spinnerDeliveryMethod.setSelection(0)

        selectedFileUri = null
        btnChooseFile.text = "Choose file"
    }

    // Load only the signed-in customer's prescriptions.
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
                        View.VISIBLE

                    tvNoPrescriptions.text =
                        "No prescription records found."

                    return@addOnSuccessListener
                }

                tvNoPrescriptions.visibility =
                    View.GONE

                for (document in result.documents) {

                    val patient =
                        document.getString("patientName")
                            ?: "Not provided"

                    val doctor =
                        document.getString("doctorName")
                            ?: "Not provided"

                    val method =
                        document.getString("deliveryMethod")
                            ?: "Not provided"

                    val date =
                        document.getString("prescriptionDate")
                            ?: "Not provided"

                    val number =
                        document.getString("prescriptionNumber")
                            ?: "Not provided"

                    val status =
                        document.getString("status")
                            ?: "Pending"

                    val reason =
                        document.getString("rejectionReason")
                            ?: ""

                    val fileName =
                        document.getString("fileName")
                            ?: "Not uploaded"

                    val fileUpload =
                        document.getBoolean("fileUpload")
                            ?: false

                    addPrescriptionRow(
                        number,
                        patient,
                        doctor,
                        method,
                        date,
                        status,
                        reason,
                        fileName,
                        fileUpload
                    )
                }
            }
            .addOnFailureListener { error ->

                tvNoPrescriptions.visibility =
                    View.VISIBLE

                tvNoPrescriptions.text =
                    "Could not load prescriptions. Please try again."

                Toast.makeText(
                    this,
                    error.message
                        ?: "Failed to load prescriptions",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    // Display each prescription as a readable card.
    private fun addPrescriptionRow(
        number: String,
        patient: String,
        doctor: String,
        method: String,
        date: String,
        status: String,
        reason: String,
        fileName: String,
        fileUpload: Boolean
    ) {

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = roundedBackground(Color.WHITE)
        }

        addCardText(
            card,
            "Prescription #$number",
            18,
            Color.rgb(0, 48, 73),
            true
        )

        addCardText(card, "Patient: $patient")
        addCardText(card, "Doctor: $doctor")
        addCardText(card, "Prescription date: $date")
        addCardText(card, "Delivery method: $method")

        addCardText(
            card,
            "File uploaded: ${if (fileUpload) "Yes" else "No"}"
        )

        addCardText(
            card,
            "File name: $fileName"
        )

        addCardText(
            card,
            "Status: $status",
            16,
            getStatusColor(status),
            true
        )

        if (
            status.equals("Rejected", ignoreCase = true) &&
            reason.isNotBlank()
        ) {

            addCardText(
                card,
                "Reason for rejection: $reason",
                14,
                Color.rgb(193, 18, 31),
                true
            )
        }

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(
                dp(10),
                dp(8),
                dp(10),
                dp(10)
            )
        }

        prescriptionsContainer.addView(card, params)
    }

    private fun addCardText(
        parent: LinearLayout,
        value: String,
        size: Int = 14,
        color: Int = Color.DKGRAY,
        bold: Boolean = false
    ) {

        val textView = TextView(this).apply {

            text = value
            textSize = size.toFloat()
            setTextColor(color)

            if (bold) {
                setTypeface(null, Typeface.BOLD)
            }

            setPadding(0, dp(5), 0, dp(5))
        }

        parent.addView(textView)
    }

    private fun getStatusColor(status: String): Int {

        return when (
            status.lowercase(Locale.getDefault())
        ) {
            "approved" -> Color.rgb(35, 130, 75)

            "rejected" -> Color.rgb(193, 18, 31)

            else -> Color.rgb(190, 110, 0)
        }
    }

    private fun roundedBackground(color: Int): GradientDrawable {

        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(12).toFloat()
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
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

        txtEmail?.text =
            email.ifEmpty { "user@email.com" }

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
                    // Already on the prescriptions screen.
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

            flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        startActivity(intent)
        finish()
    }
}