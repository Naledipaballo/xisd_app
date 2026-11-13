package com.example.pillpoint

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TrackOrder : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private lateinit var orderNumber: EditText
    private lateinit var trackOrderButton: Button
    private lateinit var orderDetailsLayout: LinearLayout

    private lateinit var orderStatus: TextView
    private lateinit var orderDate: TextView
    private lateinit var deliveryAddress: TextView
    private lateinit var pharmacyDetails: TextView
    private lateinit var estimatedDelivery: TextView
    private lateinit var orderItems: TextView

    private lateinit var progressPlaced: TextView
    private lateinit var progressConfirmed: TextView
    private lateinit var progressPreparing: TextView
    private lateinit var progressDelivery: TextView
    private lateinit var progressDelivered: TextView

    private var orderListener: ListenerRegistration? = null
    private var trackedOrderId: String = ""

    private val darkBlue = Color.parseColor("#003049")
    private val grey = Color.parseColor("#777777")
    private val green = Color.parseColor("#16803C")
    private val red = Color.parseColor("#C1121F")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_track_order)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Connect XML views
        orderNumber = findViewById(R.id.orderNumber)
        trackOrderButton = findViewById(R.id.trackOrderButton)
        orderDetailsLayout = findViewById(R.id.orderDetailsLayout)

        orderStatus = findViewById(R.id.orderStatus)
        orderDate = findViewById(R.id.orderDate)
        deliveryAddress = findViewById(R.id.deliveryAddress)
        pharmacyDetails = findViewById(R.id.pharmacyDetails)
        estimatedDelivery = findViewById(R.id.estimatedDelivery)
        orderItems = findViewById(R.id.orderItems)

        progressPlaced = findViewById(R.id.progressPlaced)
        progressConfirmed = findViewById(R.id.progressConfirmed)
        progressPreparing = findViewById(R.id.progressPreparing)
        progressDelivery = findViewById(R.id.progressDelivery)
        progressDelivered = findViewById(R.id.progressDelivered)

        findViewById<Button>(R.id.btnBackTrack).setOnClickListener {
            finish()
        }

        trackOrderButton.setOnClickListener {
            val enteredOrderId = orderNumber.text.toString().trim()

            if (enteredOrderId.isEmpty()) {
                orderNumber.error = "Please enter your order ID"
                return@setOnClickListener
            }

            trackedOrderId = enteredOrderId
            orderDetailsLayout.visibility = View.GONE
            listenToOrder(trackedOrderId)
        }

        // Automatically track an order opened from another screen.
        val receivedOrderId = intent.getStringExtra("ORDER_ID")

        if (!receivedOrderId.isNullOrBlank()) {
            trackedOrderId = receivedOrderId
            orderNumber.setText(receivedOrderId)
        }
    }

    override fun onStart() {
        super.onStart()

        if (trackedOrderId.isNotBlank()) {
            listenToOrder(trackedOrderId)
        }
    }

    override fun onStop() {
        super.onStop()
        orderListener?.remove()
        orderListener = null
    }

    private fun listenToOrder(orderId: String) {

        val currentUser = auth.currentUser

        if (currentUser == null) {
            Toast.makeText(
                this,
                "Please log in to track your order.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        orderListener?.remove()

        trackOrderButton.isEnabled = false
        trackOrderButton.text = "Loading..."

        orderListener = firestore.collection("orders")
            .document(orderId)
            .addSnapshotListener { snapshot, error ->

                trackOrderButton.isEnabled = true
                trackOrderButton.text = "Track Order"

                if (error != null) {
                    Toast.makeText(
                        this,
                        "Unable to retrieve order: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    return@addSnapshotListener
                }

                if (snapshot == null || !snapshot.exists()) {
                    orderDetailsLayout.visibility = View.GONE

                    Toast.makeText(
                        this,
                        "Order not found. Please check your order ID.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@addSnapshotListener
                }

                val data = snapshot.data ?: return@addSnapshotListener

                // Customers may only track their own orders.
                val ownerId = data["userId"]?.toString()

                if (ownerId != currentUser.uid) {
                    orderDetailsLayout.visibility = View.GONE

                    Toast.makeText(
                        this,
                        "You are not authorised to view this order.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@addSnapshotListener
                }

                displayOrderDetails(orderId, data)
            }
    }

    private fun displayOrderDetails(
        orderId: String,
        data: Map<String, Any>
    ) {

        orderDetailsLayout.visibility = View.VISIBLE

        // Order status
        val status = data["status"]?.toString() ?: "Pending"

        orderStatus.text = "Status: $status"
        orderStatus.setTextColor(
            if (status.equals("Cancelled", ignoreCase = true)) red
            else darkBlue
        )

        // Order date
        val createdAt = data["createdAt"]

        orderDate.text = "Order Date: ${formatDate(createdAt)}"

        // Customer delivery address
        val address = data["deliveryAddress"]?.toString()
            ?: "Not provided"

        deliveryAddress.text = "Delivery Address:\n$address"

        // Selected pharmacy details
        val pharmacyName = data["pharmacyName"]?.toString()
        val pharmacyAddress = data["pharmacyAddress"]?.toString()
        val pharmacyPhone = data["pharmacyPhone"]?.toString()
        val pharmacyId = data["pharmacyId"]?.toString()

        val pharmacyText = StringBuilder()

        if (!pharmacyName.isNullOrBlank()) {
            pharmacyText.append("Pharmacy: $pharmacyName\n")
        }

        if (!pharmacyAddress.isNullOrBlank()) {
            pharmacyText.append("Address: $pharmacyAddress\n")
        }

        if (!pharmacyPhone.isNullOrBlank()) {
            pharmacyText.append("Phone: $pharmacyPhone\n")
        }

        if (!pharmacyId.isNullOrBlank()) {
            pharmacyText.append("Pharmacy ID: $pharmacyId")
        }

        pharmacyDetails.text = if (pharmacyText.isNotEmpty()) {
            "Selected Pharmacy:\n$pharmacyText"
        } else {
            "Selected Pharmacy:\nNo pharmacy was selected for this order."
        }

        // Estimated delivery
        val estimated = data["estimatedDelivery"]

        estimatedDelivery.text =
            "Estimated Delivery: ${formatEstimatedDelivery(estimated)}"

        // Delivery progress
        updateDeliveryProgress(status)

        // Order items and payment information
        displayItems(data)
    }

    private fun displayItems(data: Map<String, Any>) {

        val details = StringBuilder()

        val items = data["items"] as? List<*>

        if (items.isNullOrEmpty()) {
            details.append("No order items found.\n")
        } else {

            for (item in items) {

                val itemData = item as? Map<*, *> ?: continue

                val name = itemData["name"]?.toString()
                    ?: "Medication"

                val quantity = itemData["quantity"]?.toString()
                    ?: "1"

                val lineTotal = itemData["lineTotal"]?.toString()
                val price = itemData["price"]?.toString()

                val itemAmount = lineTotal ?: price ?: "0.00"

                details.append("• $name\n")
                details.append("  Quantity: $quantity\n")
                details.append("  Amount: R${formatAmount(itemAmount)}\n\n")
            }
        }

        val subtotal = data["subtotal"]
        val deliveryFee = data["deliveryFee"]
        val total = data["total"]
        val paymentMethod = data["paymentMethod"]?.toString()
            ?: "Not specified"

        details.append("------------------------------\n")
        details.append("Subtotal: R${formatAmount(subtotal)}\n")
        details.append("Delivery Fee: R${formatAmount(deliveryFee)}\n")
        details.append("Total: R${formatAmount(total)}\n")
        details.append("Payment Method: $paymentMethod\n")

        orderItems.text = details.toString()
    }

    private fun updateDeliveryProgress(status: String) {

        val progressViews = listOf(
            progressPlaced,
            progressConfirmed,
            progressPreparing,
            progressDelivery,
            progressDelivered
        )

        val labels = listOf(
            "Order Placed",
            "Order Confirmed",
            "Pharmacy Preparing Order",
            "Out for Delivery",
            "Delivered"
        )

        // Reset all progress steps.
        for (i in progressViews.indices) {
            progressViews[i].text = "○ ${labels[i]}"
            progressViews[i].setTextColor(grey)
        }

        if (status.equals("Cancelled", ignoreCase = true)) {
            for (view in progressViews) {
                view.text = "✕ ${view.text.toString().substringAfter(" ")}"
                view.setTextColor(red)
            }
            return
        }

        val currentStep = when (status.trim().lowercase(Locale.ROOT)) {
            "pending" -> 0
            "confirmed" -> 1
            "processing", "ready for delivery", "preparing" -> 2
            "out for delivery" -> 3
            "delivered" -> 4
            else -> 0
        }

        // Mark completed steps.
        for (i in 0..currentStep) {
            progressViews[i].text = "✓ ${labels[i]}"
            progressViews[i].setTextColor(green)
        }
    }

    private fun formatDate(value: Any?): String {

        return when (value) {
            is Timestamp -> {
                val formatter = SimpleDateFormat(
                    "dd MMM yyyy, HH:mm",
                    Locale.getDefault()
                )
                formatter.format(value.toDate())
            }

            is Date -> {
                val formatter = SimpleDateFormat(
                    "dd MMM yyyy, HH:mm",
                    Locale.getDefault()
                )
                formatter.format(value)
            }

            else -> value?.toString() ?: "Not available"
        }
    }

    private fun formatEstimatedDelivery(value: Any?): String {

        return when (value) {
            is Timestamp -> {
                SimpleDateFormat(
                    "dd MMM yyyy, HH:mm",
                    Locale.getDefault()
                ).format(value.toDate())
            }

            is Date -> {
                SimpleDateFormat(
                    "dd MMM yyyy, HH:mm",
                    Locale.getDefault()
                ).format(value)
            }

            else -> value?.toString() ?: "Not yet available"
        }
    }

    private fun formatAmount(value: Any?): String {

        val amount = value?.toString()?.toDoubleOrNull() ?: 0.0

        return String.format(Locale.US, "%.2f", amount)
    }
}