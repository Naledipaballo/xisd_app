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

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // Layout views
    private lateinit var etOrderNumber: EditText
    private lateinit var btnTrackOrder: Button
    private lateinit var orderDetailsLayout: LinearLayout
    private lateinit var tvOrderStatus: TextView
    private lateinit var tvOrderDate: TextView
    private lateinit var tvDeliveryAddress: TextView
    private lateinit var tvEstimatedDelivery: TextView
    private lateinit var tvOrderItems: TextView
    private lateinit var btnBackTrack: Button

    // Delivery progress views
    private lateinit var progressPlaced: TextView
    private lateinit var progressConfirmed: TextView
    private lateinit var progressPreparing: TextView
    private lateinit var progressDelivery: TextView
    private lateinit var progressDelivered: TextView

    // Firestore listener
    private var orderListener: ListenerRegistration? = null
    private var trackedOrderId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_track_order)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        initViews()

        // Back button
        btnBackTrack.setOnClickListener {
            finish()
        }

        // Track button
        btnTrackOrder.setOnClickListener {
            val orderId = etOrderNumber.text.toString().trim()

            if (orderId.isEmpty()) {
                etOrderNumber.error = "Please enter your order ID"
                return@setOnClickListener
            }

            displayOrderDetails(orderId)
        }

        // If opened from My Orders, automatically track that order
        val orderId = intent.getStringExtra("ORDER_ID")

        if (!orderId.isNullOrEmpty()) {
            etOrderNumber.setText(orderId)
            trackedOrderId = orderId
        }
    }

    private fun initViews() {

        etOrderNumber = findViewById(R.id.orderNumber)
        btnTrackOrder = findViewById(R.id.trackOrderButton)
        orderDetailsLayout = findViewById(R.id.orderDetailsLayout)

        tvOrderStatus = findViewById(R.id.orderStatus)
        tvOrderDate = findViewById(R.id.orderDate)
        tvDeliveryAddress = findViewById(R.id.deliveryAddress)
        tvEstimatedDelivery = findViewById(R.id.estimatedDelivery)
        tvOrderItems = findViewById(R.id.orderItems)

        btnBackTrack = findViewById(R.id.btnBackTrack)

        progressPlaced = findViewById(R.id.progressPlaced)
        progressConfirmed = findViewById(R.id.progressConfirmed)
        progressPreparing = findViewById(R.id.progressPreparing)
        progressDelivery = findViewById(R.id.progressDelivery)
        progressDelivered = findViewById(R.id.progressDelivered)
    }

    override fun onStart() {
        super.onStart()

        // Start listening if an order ID was supplied
        trackedOrderId?.let {
            listenToOrder(it)
        }
    }

    override fun onStop() {
        super.onStop()

        // Stop listening when the screen is no longer visible
        orderListener?.remove()
        orderListener = null
    }

    private fun displayOrderDetails(orderId: String) {

        val user = auth.currentUser

        if (user == null) {
            Toast.makeText(
                this,
                "Please log in to track your order.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        trackedOrderId = orderId

        // Remove any previous listener
        orderListener?.remove()
        orderListener = null

        orderDetailsLayout.visibility = View.GONE
        btnTrackOrder.isEnabled = false
        btnTrackOrder.text = "Loading..."

        listenToOrder(orderId)
    }

    private fun listenToOrder(orderId: String) {

        val user = auth.currentUser

        if (user == null) {
            Toast.makeText(
                this,
                "Please log in first.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        // Remove an existing listener before creating another one
        orderListener?.remove()

        orderListener = db.collection("orders")
            .document(orderId)
            .addSnapshotListener { document, error ->

                btnTrackOrder.isEnabled = true
                btnTrackOrder.text = "Track Order"

                if (error != null) {
                    orderDetailsLayout.visibility = View.GONE

                    Toast.makeText(
                        this,
                        "Unable to load order: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()

                    return@addSnapshotListener
                }

                if (document == null || !document.exists()) {
                    orderDetailsLayout.visibility = View.GONE

                    Toast.makeText(
                        this,
                        "Order not found. Please check your order ID.",
                        Toast.LENGTH_LONG
                    ).show()

                    return@addSnapshotListener
                }

                val data = document.data ?: return@addSnapshotListener

                // Make sure the signed-in customer owns this order
                val orderUserId = data["userId"] as? String

                if (orderUserId != user.uid) {
                    orderDetailsLayout.visibility = View.GONE

                    Toast.makeText(
                        this,
                        "You are not authorised to view this order.",
                        Toast.LENGTH_LONG
                    ).show()

                    orderListener?.remove()
                    orderListener = null
                    trackedOrderId = null

                    return@addSnapshotListener
                }

                // Display the latest order information
                showOrder(data)
            }
    }

    private fun showOrder(data: Map<String, Any>) {

        orderDetailsLayout.visibility = View.VISIBLE

        val status = data["status"] as? String ?: "Pending"
        val address = data["deliveryAddress"] as? String ?: "Not provided"
        val paymentMethod = data["paymentMethod"] as? String ?: "Not provided"

        val subtotal = (data["subtotal"] as? Number)?.toDouble() ?: 0.0
        val deliveryFee = (data["deliveryFee"] as? Number)?.toDouble() ?: 0.0
        val total = (data["total"] as? Number)?.toDouble() ?: 0.0

        // Order status
        tvOrderStatus.text = "Status: $status"

        // Update the delivery progress tracker
        updateDeliveryProgress(status)

        // Delivery address
        tvDeliveryAddress.text = "Delivery Address: $address"

        // Order date
        val timestamp = data["createdAt"] as? Timestamp

        if (timestamp != null) {
            val date = timestamp.toDate()
            val dateFormat = SimpleDateFormat(
                "dd MMM yyyy, HH:mm",
                Locale.getDefault()
            )

            tvOrderDate.text = "Order Date: ${dateFormat.format(date)}"
        } else {
            tvOrderDate.text = "Order Date: Not available"
        }

        // Estimated delivery
        tvEstimatedDelivery.text = when {
            status.equals("Delivered", ignoreCase = true) ->
                "Delivery: Your order has been delivered."

            status.equals("Cancelled", ignoreCase = true) ->
                "Delivery: This order has been cancelled."

            else ->
                "Estimated Delivery: Not yet provided by the pharmacy."
        }

        // Order items
        val items = data["items"] as? List<*>
        val itemsText = StringBuilder()

        if (!items.isNullOrEmpty()) {

            for (item in items) {

                val itemMap = item as? Map<*, *> ?: continue

                val name = itemMap["name"] as? String ?: "Medication"
                val quantity = (itemMap["quantity"] as? Number)?.toInt() ?: 1
                val price = (itemMap["price"] as? Number)?.toDouble() ?: 0.0
                val lineTotal =
                    (itemMap["lineTotal"] as? Number)?.toDouble()
                        ?: (price * quantity)

                itemsText.append("• $name\n")
                itemsText.append("  Quantity: $quantity\n")
                itemsText.append("  Unit Price: R${"%.2f".format(Locale.US, price)}\n")
                itemsText.append("  Line Total: R${"%.2f".format(Locale.US, lineTotal)}\n\n")
            }

        } else {
            itemsText.append("No order items found.")
        }

        // Payment and totals
        itemsText.append("------------------------------\n")
        itemsText.append(
            "Subtotal: R${"%.2f".format(Locale.US, subtotal)}\n"
        )
        itemsText.append(
            "Delivery Fee: R${"%.2f".format(Locale.US, deliveryFee)}\n"
        )
        itemsText.append(
            "Total: R${"%.2f".format(Locale.US, total)}\n"
        )
        itemsText.append("Payment Method: $paymentMethod")

        tvOrderItems.text = itemsText.toString()
    }

    private fun updateDeliveryProgress(status: String) {

        val steps = listOf(
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

        val currentStep = when (status.trim().lowercase(Locale.ROOT)) {
            "pending" -> 0
            "confirmed" -> 1
            "processing", "ready for delivery" -> 2
            "out for delivery" -> 3
            "delivered" -> 4
            else -> -1
        }

        steps.forEachIndexed { index, textView ->

            if (currentStep >= 0 && index <= currentStep) {

                textView.text = "✓ ${labels[index]}"
                textView.setTextColor(Color.parseColor("#16803C"))

            } else {

                textView.text = "○ ${labels[index]}"
                textView.setTextColor(Color.parseColor("#777777"))
            }
        }

        // Show cancelled orders in red
        if (status.equals("Cancelled", ignoreCase = true)) {

            tvOrderStatus.setTextColor(
                Color.parseColor("#C1121F")
            )

        } else {

            tvOrderStatus.setTextColor(
                Color.parseColor("#003049")
            )
        }
    }
}