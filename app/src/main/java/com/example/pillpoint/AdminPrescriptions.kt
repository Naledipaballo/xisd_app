package com.example.pillpoint


import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.FieldValue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AdminPrescriptions : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var prescriptionContainer: LinearLayout
    private lateinit var progressBar: ProgressBar

    private var prescriptionListener: ListenerRegistration? = null
    private var adminUid: String = ""

    private val blue = Color.rgb(0, 48, 73)
    private val lightBlue = Color.rgb(223, 238, 243)
    private val red = Color.rgb(193, 18, 31)
    private val green = Color.rgb(35, 130, 75)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_prescriptions)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        prescriptionContainer = findViewById(R.id.prescriptionContainer)
        progressBar = findViewById(R.id.prescriptionsProgressBar)

        findViewById<Button>(R.id.btnBackToAdminDashboard).setOnClickListener {
            finish()
        }

        verifyAdmin()
    }

    // Check that the signed-in account is an administrator.
    private fun verifyAdmin() {
        val user = auth.currentUser

        if (user == null) {
            Toast.makeText(this, "Please log in as admin.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        adminUid = user.uid

        db.collection("users")
            .document(adminUid)
            .get()
            .addOnSuccessListener { document ->

                if (document.exists() &&
                    document.getString("role") == "admin"
                ) {
                    loadPrescriptions()
                } else {
                    Toast.makeText(
                        this,
                        "Administrator access denied.",
                        Toast.LENGTH_LONG
                    ).show()

                    auth.signOut()
                    finish()
                }
            }
            .addOnFailureListener {
                Toast.makeText(
                    this,
                    "Could not verify administrator account.",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
    }

    // Listen for prescription submissions in Firestore.
    private fun loadPrescriptions() {
        progressBar.visibility = View.VISIBLE

        prescriptionListener = db.collection("prescriptions")
            .addSnapshotListener { snapshot, error ->

                progressBar.visibility = View.GONE

                if (error != null) {
                    Toast.makeText(
                        this,
                        "Failed to load prescriptions: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    return@addSnapshotListener
                }

                prescriptionContainer.removeAllViews()

                val documents = snapshot?.documents
                    ?.sortedByDescending {
                        it.getTimestamp("createdAt")?.toDate()?.time ?: 0L
                    }
                    ?: emptyList()

                if (documents.isEmpty()) {
                    addText(
                        prescriptionContainer,
                        "No prescriptions have been submitted yet.",
                        16,
                        blue
                    )
                    return@addSnapshotListener
                }

                documents.forEach { document ->
                    createPrescriptionCard(document)
                }
            }
    }

    // Create a card for each prescription.
    private fun createPrescriptionCard(
        document: com.google.firebase.firestore.DocumentSnapshot
    ) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = roundedBackground(Color.WHITE, 14)
        }

        val prescriptionNumber =
            document.getString("prescriptionNumber") ?: "Not provided"

        val patientName =
            document.getString("patientName") ?: "Not provided"

        val doctorName =
            document.getString("doctorName") ?: "Not provided"

        val prescriptionDate =
            document.getString("prescriptionDate") ?: "Not provided"

        val deliveryMethod =
            document.getString("deliveryMethod") ?: "Not provided"

        val additionalNo =
            document.getString("additionalNo") ?: "Not provided"

        val fileName =
            document.getString("fileName") ?: "Not provided"

        val fileType =
            document.getString("fileType") ?: "Not provided"

        val fileUploaded =
            document.getBoolean("fileUpload") ?: false

        val status =
            document.getString("status") ?: "Pending"

        val rejectionReason =
            document.getString("rejectionReason") ?: ""

        addText(
            card,
            "Prescription #$prescriptionNumber",
            19,
            blue,
            true
        )

        addText(card, "Patient: $patientName", 15)
        addText(card, "Doctor: $doctorName", 15)
        addText(card, "Prescription date: $prescriptionDate", 14)
        addText(card, "Additional number: $additionalNo", 14)
        addText(card, "Delivery method: $deliveryMethod", 14)
        addText(card, "Submitted: ${formatDate(document.getTimestamp("createdAt"))}", 13)

        addText(card, "File uploaded: ${if (fileUploaded) "Yes" else "No"}", 14)

        if (fileUploaded) {
            addText(card, "File name: $fileName", 13)
            addText(card, "File type: $fileType", 13)
            addText(
                card,
                "The file is not stored, so it cannot be opened yet.",
                12,
                red
            )
        }

        addText(card, "Status: $status", 15, statusColor(status), true)

        if (status.equals("Rejected", ignoreCase = true) &&
            rejectionReason.isNotBlank()
        ) {
            addText(card, "Rejection reason: $rejectionReason", 14, red)
        }

        val buttonRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        val approveButton = Button(this).apply {
            text = "Approve"
            setTextColor(Color.WHITE)
            background = roundedBackground(green, 8)
            isAllCaps = false
        }

        val rejectButton = Button(this).apply {
            text = "Reject"
            setTextColor(Color.WHITE)
            background = roundedBackground(red, 8)
            isAllCaps = false
        }

        val buttonParams = LinearLayout.LayoutParams(
            0,
            dp(48),
            1f
        ).apply {
            marginEnd = dp(6)
            topMargin = dp(12)
        }

        buttonRow.addView(approveButton, buttonParams)

        val rejectParams = LinearLayout.LayoutParams(
            0,
            dp(48),
            1f
        ).apply {
            marginStart = dp(6)
            topMargin = dp(12)
        }

        buttonRow.addView(rejectButton, rejectParams)

        val isFinalStatus =
            status.equals("Approved", true) ||
                    status.equals("Rejected", true)

        if (isFinalStatus) {
            approveButton.isEnabled = false
            rejectButton.isEnabled = false
            approveButton.alpha = 0.5f
            rejectButton.alpha = 0.5f
        }

        approveButton.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Approve prescription")
                .setMessage("Are you sure you want to approve this prescription?")
                .setPositiveButton("Approve") { _, _ ->
                    updatePrescriptionStatus(
                        document.id,
                        "Approved",
                        ""
                    )
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        rejectButton.setOnClickListener {
            showRejectDialog(document.id)
        }

        card.addView(buttonRow)

        val cardParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(dp(12), dp(8), dp(12), dp(12))
        }

        prescriptionContainer.addView(card, cardParams)
    }

    // Ask the admin to provide a reason for rejecting a prescription.
    private fun showRejectDialog(documentId: String) {
        val input = EditText(this).apply {
            hint = "Enter reason for rejection"
            minLines = 2
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }

        AlertDialog.Builder(this)
            .setTitle("Reject prescription")
            .setMessage("Please explain why this prescription is being rejected.")
            .setView(input)
            .setPositiveButton("Reject") { _, _ ->

                val reason = input.text.toString().trim()

                if (reason.isBlank()) {
                    Toast.makeText(
                        this,
                        "Please enter a rejection reason.",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    updatePrescriptionStatus(
                        documentId,
                        "Rejected",
                        reason
                    )
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // Save the admin's decision to Firestore.
    private fun updatePrescriptionStatus(
        documentId: String,
        newStatus: String,
        reason: String
    ) {
        progressBar.visibility = View.VISIBLE

        val updates = hashMapOf<String, Any>(
            "status" to newStatus,
            "rejectionReason" to reason,
            "reviewedAt" to FieldValue.serverTimestamp(),
            "reviewedBy" to adminUid
        )

        db.collection("prescriptions")
            .document(documentId)
            .update(updates)
            .addOnSuccessListener {
                progressBar.visibility = View.GONE

                Toast.makeText(
                    this,
                    "Prescription $newStatus successfully.",
                    Toast.LENGTH_LONG
                ).show()
            }
            .addOnFailureListener { error ->
                progressBar.visibility = View.GONE

                Toast.makeText(
                    this,
                    "Could not update prescription: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun addText(
        parent: LinearLayout,
        value: String,
        size: Int,
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
            setPadding(0, dp(4), 0, dp(4))
        }

        parent.addView(textView)
    }

    private fun formatDate(timestamp: Timestamp?): String {
        if (timestamp == null) return "Not available"

        val formatter = SimpleDateFormat(
            "dd MMM yyyy, HH:mm",
            Locale.getDefault()
        )

        return formatter.format(timestamp.toDate())
    }

    private fun statusColor(status: String): Int {
        return when (status.lowercase(Locale.getDefault())) {
            "approved" -> green
            "rejected" -> red
            else -> Color.rgb(190, 110, 0)
        }
    }

    private fun roundedBackground(
        color: Int,
        radius: Int
    ): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    override fun onDestroy() {
        prescriptionListener?.remove()
        super.onDestroy()
    }
}