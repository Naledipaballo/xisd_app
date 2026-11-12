package com.example.pillpoint

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Locale

class AdminOrderDetails : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // Order information
    private lateinit var tvOrderId: TextView
    private lateinit var tvCustomerName: TextView
    private lateinit var tvCustomerPhone: TextView
    private lateinit var tvDeliveryAddress: TextView
    private lateinit var tvOrderDate: TextView
    private lateinit var tvOrderItems: TextView

    // Payment information
    private lateinit var tvSubtotal: TextView
    private lateinit var tvDeliveryFee: TextView
    private lateinit var tvTotal: TextView
    private lateinit var tvPaymentMethod: TextView

    // Order status
    private lateinit var tvCurrentStatus: TextView
    private lateinit var spinnerOrderStatus: Spinner
    private lateinit var btnUpdateOrderStatus: Button

    // History
    private lateinit var statusHistoryContainer: LinearLayout
    private lateinit var tvNoStatusHistory: TextView

    private lateinit var btnBack: Button

    private var orderId: String = ""
    private var currentStatus: String = "Pending"

    private var orderListener: ListenerRegistration? = null
    private var adminVerified = false
    private var statusUpdateInProgress = false

    private val orderStatuses = arrayOf(
        "Pending",
        "Confirmed",
        "Processing",
        "Ready for Delivery",
        "Out for Delivery",
        "Delivered",
        "Cancelled"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_order_details)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        initViews()
        setupStatusSpinner()

        orderId = intent.getStringExtra("ORDER_ID") ?: ""

        if (orderId.isBlank()) {
            Toast.makeText(
                this,
                "Order ID is missing.",
                Toast.LENGTH_LONG
            ).show()
            finish()
            return
        }

        tvOrderId.text = "Order ID: $orderId"

        btnBack.setOnClickListener {
            finish()
        }

        btnUpdateOrderStatus.setOnClickListener {
            updateOrderStatus()
        }

        verifyAdmin()
    }

    private fun initViews() {

        tvOrderId = findViewById(R.id.tvAdminOrderId)
        tvCustomerName = findViewById(R.id.tvAdminCustomerName)
        tvCustomerPhone = findViewById(R.id.tvAdminCustomerPhone)
        tvDeliveryAddress = findViewById(R.id.tvAdminDeliveryAddress)
        tvOrderDate = findViewById(R.id.tvAdminOrderDate)
        tvOrderItems = findViewById(R.id.tvAdminOrderItems)

        tvSubtotal = findViewById(R.id.tvAdminSubtotal)
        tvDeliveryFee = findViewById(R.id.tvAdminDeliveryFee)
        tvTotal = findViewById(R.id.tvAdminTotal)
        tvPaymentMethod = findViewById(R.id.tvAdminPaymentMethod)

        tvCurrentStatus = findViewById(R.id.tvCurrentOrderStatus)
        spinnerOrderStatus = findViewById(R.id.spinnerOrderStatus)
        btnUpdateOrderStatus = findViewById(R.id.btnUpdateOrderStatus)

        statusHistoryContainer = findViewById(R.id.statusHistoryContainer)
        tvNoStatusHistory = findViewById(R.id.tvNoStatusHistory)

        btnBack = findViewById(R.id.btnBackAdminOrder)
    }

    private fun setupStatusSpinner() {

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            orderStatuses
        )

        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spinnerOrderStatus.adapter = adapter
    }

    private fun verifyAdmin() {

        val user = auth.currentUser

        if (user == null) {
            openAdminLogin()
            return
        }

        db.collection("users")
            .document(user.uid)
            .get()
            .addOnSuccessListener { document ->

                if (isFinishing || isDestroyed) return@addOnSuccessListener

                if (document.exists() &&
                    document.getString("role") == "admin"
                ) {
                    adminVerified = true
                    startOrderListener()
                } else {
                    Toast.makeText(
                        this,
                        "Access denied. Admin account required.",
                        Toast.LENGTH_LONG
                    ).show()

                    auth.signOut()
                    openAdminLogin()
                }
            }
            .addOnFailureListener { error ->

                if (isFinishing || isDestroyed) return@addOnFailureListener

                Toast.makeText(
                    this,
                    "Unable to verify admin: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()

                finish()
            }
    }

    override fun onStart() {
        super.onStart()

        if (adminVerified) {
            startOrderListener()
        }
    }

    override fun onStop() {
        super.onStop()

        orderListener?.remove()
        orderListener = null
    }

    private fun openAdminLogin() {

        val intent = android.content.Intent(
            this,
            AdminLogin::class.java
        )

        intent.flags =
            android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                    android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intent)
        finish()
    }

    // Listen for live order changes
    private fun startOrderListener() {

        if (orderId.isBlank() || !adminVerified) return

        orderListener?.remove()
        orderListener = null

        orderListener = db.collection("orders")
            .document(orderId)
            .addSnapshotListener { document, error ->

                if (isFinishing || isDestroyed) return@addSnapshotListener

                if (error != null) {
                    Toast.makeText(
                        this,
                        "Unable to load order: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    return@addSnapshotListener
                }

                if (document == null || !document.exists()) {
                    Toast.makeText(
                        this,
                        "Order not found.",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                    return@addSnapshotListener
                }

                val data = document.data ?: return@addSnapshotListener

                displayOrder(data)
                loadStatusHistory()
            }
    }

    // Display order details
    private fun displayOrder(data: Map<String, Any>) {

        tvCustomerName.text =
            "Customer: ${data["customerName"] ?: "Not provided"}"

        tvCustomerPhone.text =
            "Phone: ${data["phone"] ?: "Not provided"}"

        tvDeliveryAddress.text =
            "Delivery Address: ${data["deliveryAddress"] ?: "Not provided"}"

        // Order date
        val timestamp = data["createdAt"] as? Timestamp

        if (timestamp != null) {

            val formatter = SimpleDateFormat(
                "dd MMM yyyy, HH:mm",
                Locale.getDefault()
            )

            tvOrderDate.text =
                "Order Date: ${formatter.format(timestamp.toDate())}"

        } else {
            tvOrderDate.text = "Order Date: Not available"
        }

        // Medication items
        val items = data["items"] as? List<*>
        val itemsText = StringBuilder()

        if (!items.isNullOrEmpty()) {

            for (item in items) {

                val itemMap = item as? Map<*, *> ?: continue

                val name =
                    itemMap["name"] as? String ?: "Medication"

                val quantity =
                    (itemMap["quantity"] as? Number)?.toInt() ?: 1

                val price =
                    (itemMap["price"] as? Number)?.toDouble() ?: 0.0

                val lineTotal =
                    (itemMap["lineTotal"] as? Number)?.toDouble()
                        ?: price * quantity

                itemsText.append("$name\n")
                itemsText.append("Quantity: $quantity\n")
                itemsText.append(
                    "Unit Price: R${"%.2f".format(Locale.US, price)}\n"
                )
                itemsText.append(
                    "Line Total: R${"%.2f".format(Locale.US, lineTotal)}\n\n"
                )
            }

        } else {
            itemsText.append("No items found.")
        }

        tvOrderItems.text = itemsText.toString()

        // Payment summary
        val subtotal =
            (data["subtotal"] as? Number)?.toDouble() ?: 0.0

        val deliveryFee =
            (data["deliveryFee"] as? Number)?.toDouble() ?: 0.0

        val total =
            (data["total"] as? Number)?.toDouble() ?: 0.0

        tvSubtotal.text =
            "Subtotal: R${"%.2f".format(Locale.US, subtotal)}"

        tvDeliveryFee.text =
            "Delivery Fee: R${"%.2f".format(Locale.US, deliveryFee)}"

        tvTotal.text =
            "Total: R${"%.2f".format(Locale.US, total)}"

        tvPaymentMethod.text =
            "Payment Method: ${data["paymentMethod"] ?: "Not provided"}"

        // Current status
        currentStatus = data["status"] as? String ?: "Pending"

        tvCurrentStatus.text = "Current Status: $currentStatus"

        val selectedIndex = orderStatuses.indexOfFirst {
            it.equals(currentStatus, ignoreCase = true)
        }

        if (selectedIndex >= 0) {
            spinnerOrderStatus.setSelection(selectedIndex)
        } else {
            spinnerOrderStatus.setSelection(0)
        }
    }

    // Update order status and save history
    private fun updateOrderStatus() {

        val user = auth.currentUser

        if (user == null || !adminVerified) {
            Toast.makeText(
                this,
                "Admin authentication required.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if (statusUpdateInProgress) return

        val selectedStatus =
            spinnerOrderStatus.selectedItem.toString()

        if (selectedStatus == currentStatus) {
            Toast.makeText(
                this,
                "The order already has this status.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        statusUpdateInProgress = true
        btnUpdateOrderStatus.isEnabled = false
        btnUpdateOrderStatus.text = "Updating..."

        val previousStatus = currentStatus

        val orderRef = db.collection("orders")
            .document(orderId)

        val historyRef = orderRef
            .collection("statusHistory")
            .document()

        val batch = db.batch()

        // Update the main order document
        batch.update(
            orderRef,
            mapOf(
                "status" to selectedStatus,
                "statusUpdatedAt" to FieldValue.serverTimestamp(),
                "statusUpdatedBy" to user.uid
            )
        )

        // Save the status history record
        batch.set(
            historyRef,
            mapOf(
                "fromStatus" to previousStatus,
                "toStatus" to selectedStatus,
                "updatedAt" to FieldValue.serverTimestamp(),
                "updatedBy" to user.uid,
                "adminEmail" to (user.email ?: "Admin")
            )
        )

        batch.commit()
            .addOnSuccessListener {

                currentStatus = selectedStatus

                tvCurrentStatus.text =
                    "Current Status: $selectedStatus"

                loadStatusHistory()

                Toast.makeText(
                    this,
                    "Order status updated successfully.",
                    Toast.LENGTH_LONG
                ).show()

                statusUpdateInProgress = false
                btnUpdateOrderStatus.isEnabled = true
                btnUpdateOrderStatus.text = "Update Order Status"
            }
            .addOnFailureListener { error ->

                Toast.makeText(
                    this,
                    "Failed to update status: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()

                statusUpdateInProgress = false
                btnUpdateOrderStatus.isEnabled = true
                btnUpdateOrderStatus.text = "Update Order Status"
            }
    }

    // Load order status history
    private fun loadStatusHistory() {

        db.collection("orders")
            .document(orderId)
            .collection("statusHistory")
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->

                if (isFinishing || isDestroyed) return@addOnSuccessListener

                statusHistoryContainer.removeAllViews()

                if (snapshot.isEmpty) {
                    tvNoStatusHistory.visibility = View.VISIBLE
                    return@addOnSuccessListener
                }

                tvNoStatusHistory.visibility = View.GONE

                for (document in snapshot.documents) {

                    val fromStatus =
                        document.getString("fromStatus") ?: "New Order"

                    val toStatus =
                        document.getString("toStatus") ?: "Unknown"

                    val adminEmail =
                        document.getString("adminEmail") ?: "Admin"

                    val timestamp =
                        document.getTimestamp("updatedAt")

                    val dateText = timestamp?.toDate()?.let {

                        SimpleDateFormat(
                            "dd MMM yyyy, HH:mm",
                            Locale.getDefault()
                        ).format(it)

                    } ?: "Date unavailable"

                    val historyItem = TextView(this).apply {

                        text = "$fromStatus → $toStatus\n" +
                                "Updated by: $adminEmail\n" +
                                "Date: $dateText"

                        textSize = 14f

                        setTextColor(
                            Color.parseColor("#003049")
                        )

                        setPadding(0, 12, 0, 12)
                    }

                    statusHistoryContainer.addView(historyItem)

                    val divider = View(this).apply {
                        setBackgroundColor(
                            Color.parseColor("#DDDDDD")
                        )
                    }

                    statusHistoryContainer.addView(
                        divider,
                        LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            1
                        )
                    )
                }
            }
            .addOnFailureListener { error ->

                Toast.makeText(
                    this,
                    "Could not load status history: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
}