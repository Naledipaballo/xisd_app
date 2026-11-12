package com.example.pillpoint

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.util.Locale

class AdminDashboard : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var tvTotalOrders: TextView
    private lateinit var tvPendingOrders: TextView
    private lateinit var tvProcessingOrders: TextView
    private lateinit var tvDeliveredOrders: TextView

    private lateinit var tvTotalPrescriptions: TextView
    private lateinit var tvPendingPrescriptions: TextView

    private lateinit var adminDashboardProgress: ProgressBar

    private lateinit var btnManageOrders: Button
    private lateinit var btnManagePrescriptions: Button
    private lateinit var btnAdminLogout: Button

    private var ordersListener: ListenerRegistration? = null
    private var prescriptionsListener: ListenerRegistration? = null

    private var isAdmin = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        initializeViews()
        setButtonListeners()
        verifyAdmin()
    }

    // Connect XML views to Kotlin variables.
    private fun initializeViews() {
        tvTotalOrders = findViewById(R.id.tvTotalOrders)
        tvPendingOrders = findViewById(R.id.tvPendingOrders)
        tvProcessingOrders = findViewById(R.id.tvProcessingOrders)
        tvDeliveredOrders = findViewById(R.id.tvDeliveredOrders)

        tvTotalPrescriptions = findViewById(R.id.tvTotalPrescriptions)
        tvPendingPrescriptions = findViewById(R.id.tvPendingPrescriptions)

        adminDashboardProgress = findViewById(R.id.adminDashboardProgress)

        btnManageOrders = findViewById(R.id.btnManageOrders)
        btnManagePrescriptions = findViewById(R.id.btnManagePrescriptions)
        btnAdminLogout = findViewById(R.id.btnAdminLogout)
    }

    // Set up the dashboard buttons.
    private fun setButtonListeners() {

        btnManageOrders.setOnClickListener {
            if (isAdmin) {
                startActivity(Intent(this, AdminOrders::class.java))
            }
        }

        btnManagePrescriptions.setOnClickListener {
            if (isAdmin) {
                startActivity(
                    Intent(this, AdminPrescriptions::class.java)
                )
            }
        }

        btnAdminLogout.setOnClickListener {
            logoutAdmin()
        }
    }

    // Confirm that the signed-in account is an administrator.
    private fun verifyAdmin() {
        val currentUser = auth.currentUser

        if (currentUser == null) {
            returnToAdminLogin("Please log in as administrator.")
            return
        }

        adminDashboardProgress.visibility = View.VISIBLE

        db.collection("users")
            .document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->

                adminDashboardProgress.visibility = View.GONE

                if (document.exists() &&
                    document.getString("role")
                        ?.equals("admin", ignoreCase = true) == true
                ) {
                    isAdmin = true
                    loadOrders()
                    loadPrescriptions()
                } else {
                    returnToAdminLogin(
                        "Access denied. Administrator account required."
                    )
                }
            }
            .addOnFailureListener { error ->
                adminDashboardProgress.visibility = View.GONE

                Toast.makeText(
                    this,
                    "Could not verify admin account: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()

                auth.signOut()
                finish()
            }
    }

    // Load live order statistics from Firestore.
    private fun loadOrders() {

        ordersListener = db.collection("orders")
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    Toast.makeText(
                        this,
                        "Could not load orders: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    return@addSnapshotListener
                }

                val documents = snapshot?.documents ?: emptyList()

                var pending = 0
                var processing = 0
                var delivered = 0

                for (document in documents) {

                    val status = document.getString("status")
                        ?.trim()
                        ?.lowercase(Locale.getDefault())
                        ?: "pending"

                    when (status) {
                        "pending" -> pending

                        "confirmed",
                        "processing",
                        "ready for delivery",
                        "out for delivery" -> processing

                        "delivered" -> delivered
                    }

                    if (status == "pending") {
                        pending++
                    }

                    if (
                        status == "confirmed" ||
                        status == "processing" ||
                        status == "ready for delivery" ||
                        status == "out for delivery"
                    ) {
                        processing++
                    }

                    if (status == "delivered") {
                        delivered++
                    }
                }

                tvTotalOrders.text = documents.size.toString()
                tvPendingOrders.text = pending.toString()
                tvProcessingOrders.text = processing.toString()
                tvDeliveredOrders.text = delivered.toString()
            }
    }

    // Load live prescription statistics from Firestore.
    private fun loadPrescriptions() {

        prescriptionsListener = db.collection("prescriptions")
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    Toast.makeText(
                        this,
                        "Could not load prescriptions: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    return@addSnapshotListener
                }

                val documents = snapshot?.documents ?: emptyList()

                var pending = 0

                for (document in documents) {

                    val status = document.getString("status")
                        ?.trim()
                        ?.lowercase(Locale.getDefault())
                        ?: "pending"

                    if (
                        status.isBlank() ||
                        status == "pending" ||
                        status == "pending review"
                    ) {
                        pending++
                    }
                }

                tvTotalPrescriptions.text = documents.size.toString()
                tvPendingPrescriptions.text = pending.toString()
            }
    }

    // Sign out the administrator.
    private fun logoutAdmin() {
        removeListeners()
        auth.signOut()

        val intent = Intent(this, AdminLogin::class.java)
        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intent)
        finish()
    }

    // Return to the admin login screen if verification fails.
    private fun returnToAdminLogin(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()

        auth.signOut()

        val intent = Intent(this, AdminLogin::class.java)
        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intent)
        finish()
    }

    // Remove Firestore listeners to prevent memory leaks.
    private fun removeListeners() {
        ordersListener?.remove()
        prescriptionsListener?.remove()

        ordersListener = null
        prescriptionsListener = null
    }

    override fun onDestroy() {
        removeListeners()
        super.onDestroy()
    }
}