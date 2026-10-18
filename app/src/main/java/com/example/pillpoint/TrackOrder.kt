package com.example.pillpoint

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TrackOrder : AppCompatActivity() {

    private lateinit var etOrderNumber: EditText
    private lateinit var btnTrackOrder: Button
    private lateinit var llOrderDetails: LinearLayout
    private lateinit var tvOrderStatus: TextView
    private lateinit var tvOrderDate: TextView
    private lateinit var tvDeliveryAddress: TextView
    private lateinit var tvEstimatedDelivery: TextView
    private lateinit var tvOrderItems: TextView

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_track_order)

        initViews()

        findViewById<Button>(R.id.btnBackTrack).setOnClickListener {
            finish()
        }

        btnTrackOrder.setOnClickListener {
            val orderId = etOrderNumber.text.toString().trim()

            if (orderId.isEmpty()) {
                etOrderNumber.error = "Please enter your order ID"
                etOrderNumber.requestFocus()
            } else {
                displayOrderDetails(orderId)
            }
        }

        // Open a specific order when another screen supplies its ID.
        intent.getStringExtra("ORDER_ID")?.takeIf { it.isNotBlank() }?.let {
            etOrderNumber.setText(it)
            displayOrderDetails(it)
        }
    }

    private fun initViews() {
        etOrderNumber = findViewById(R.id.orderNumber)
        btnTrackOrder = findViewById(R.id.trackOrderButton)
        llOrderDetails = findViewById(R.id.orderDetailsLayout)
        tvOrderStatus = findViewById(R.id.orderStatus)
        tvOrderDate = findViewById(R.id.orderDate)
        tvDeliveryAddress = findViewById(R.id.deliveryAddress)
        tvEstimatedDelivery = findViewById(R.id.estimatedDelivery)

        // This TextView must be added to activity_track_order.xml.
        tvOrderItems = findViewById(R.id.orderItems)

        llOrderDetails.visibility = View.GONE
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

        btnTrackOrder.isEnabled = false
        btnTrackOrder.text = "Searching..."

        db.collection("orders")
            .document(orderId)
            .get()
            .addOnSuccessListener { document ->
                btnTrackOrder.isEnabled = true
                btnTrackOrder.text = "Track Order"

                if (!document.exists()) {
                    showError("Order not found. Please check the order ID.")
                    return@addOnSuccessListener
                }

                val data = document.data ?: emptyMap<String, Any>()

                // Only the owner of the order can see its details.
                if (data["userId"] != user.uid) {
                    showError("Order not found for your account.")
                    return@addOnSuccessListener
                }

                showOrder(data)
            }
            .addOnFailureListener { exception ->
                btnTrackOrder.isEnabled = true
                btnTrackOrder.text = "Track Order"
                showError(
                    exception.message ?: "Could not load your order."
                )
            }
    }

    private fun showOrder(data: Map<String, Any>) {
        val status = data["status"]?.toString() ?: "Pending"
        tvOrderStatus.text = "Status: $status"

        val timestamp = data["createdAt"] as? Timestamp
        tvOrderDate.text = if (timestamp != null) {
            "Order Date: ${formatDate(timestamp.toDate())}"
        } else {
            "Order Date: Not available"
        }

        val address = data["deliveryAddress"]?.toString()
            ?: "Not provided"
        tvDeliveryAddress.text = "Delivery Address: $address"

        tvEstimatedDelivery.text =
            "Estimated Delivery: To be confirmed by the pharmacy"

        val lines = mutableListOf<String>()
        val items = data["items"] as? List<*>

        if (items.isNullOrEmpty()) {
            lines.add("Product details are not available.")
        } else {
            items.forEach { value ->
                val item = value as? Map<*, *> ?: return@forEach
                val name = item["name"]?.toString() ?: "Product"
                val quantity =
                    (item["quantity"] as? Number)?.toInt() ?: 1
                val price =
                    (item["price"] as? Number)?.toDouble() ?: 0.0
                val lineTotal =
                    (item["lineTotal"] as? Number)?.toDouble()
                        ?: price * quantity

                lines.add("$name × $quantity")
                lines.add(
                    "Unit price: ${money(price)} | " +
                            "Line total: ${money(lineTotal)}"
                )
                lines.add("")
            }
        }

        val subtotal = numberValue(data["subtotal"])
        val deliveryFee = numberValue(data["deliveryFee"])
        val total = numberValue(data["total"])
        val payment = data["paymentMethod"]?.toString()
            ?: "Not specified"

        lines.add("Subtotal: ${money(subtotal)}")
        lines.add("Delivery fee: ${money(deliveryFee)}")
        lines.add("Total: ${money(total)}")
        lines.add("Payment method: $payment")

        tvOrderItems.text = lines.joinToString("\n")
        llOrderDetails.visibility = View.VISIBLE
    }

    private fun showError(message: String) {
        llOrderDetails.visibility = View.GONE
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun numberValue(value: Any?): Double {
        return (value as? Number)?.toDouble() ?: 0.0
    }

    private fun money(amount: Double): String {
        return String.format(Locale.getDefault(), "R%.2f", amount)
    }

    private fun formatDate(date: Date): String {
        return SimpleDateFormat(
            "dd MMM yyyy, HH:mm",
            Locale.getDefault()
        ).format(date)
    }
}