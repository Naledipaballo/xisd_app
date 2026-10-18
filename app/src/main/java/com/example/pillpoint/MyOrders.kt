package com.example.pillpoint

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MyOrders : AppCompatActivity() {

    private lateinit var ordersContainer: LinearLayout
    private lateinit var emptyOrdersText: TextView
    private lateinit var refreshOrdersButton: Button

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_orders)

        ordersContainer = findViewById(R.id.ordersContainer)
        emptyOrdersText = findViewById(R.id.emptyOrdersText)
        refreshOrdersButton = findViewById(R.id.refreshOrdersButton)

        findViewById<Button>(R.id.btnBackMyOrders).setOnClickListener {
            finish()
        }

        refreshOrdersButton.setOnClickListener {
            loadOrders()
        }

        loadOrders()
    }

    override fun onResume() {
        super.onResume()
        if (::ordersContainer.isInitialized) {
            loadOrders()
        }
    }

    private fun loadOrders() {
        val user = auth.currentUser

        if (user == null) {
            Toast.makeText(
                this,
                "Please sign in to view your orders.",
                Toast.LENGTH_LONG
            ).show()
            finish()
            return
        }

        refreshOrdersButton.isEnabled = false
        refreshOrdersButton.text = "Loading..."
        ordersContainer.removeAllViews()
        emptyOrdersText.visibility = View.GONE

        db.collection("orders")
            .whereEqualTo("userId", user.uid)
            .get()
            .addOnSuccessListener { result ->
                refreshOrdersButton.isEnabled = true
                refreshOrdersButton.text = "Refresh Orders"
                ordersContainer.removeAllViews()

                if (result.isEmpty) {
                    emptyOrdersText.text = "You haven't placed any orders yet."
                    emptyOrdersText.visibility = View.VISIBLE
                    return@addOnSuccessListener
                }

                // Show newest orders first when a createdAt timestamp exists.
                val documents = result.documents.sortedByDescending { document ->
                    (document.get("createdAt") as? Timestamp)?.seconds ?: 0L
                }

                documents.forEach { document ->
                    val status = document.getString("status") ?: "Pending"
                    val total = (document.get("total") as? Number)?.toDouble() ?: 0.0
                    val address = document.getString("deliveryAddress")
                        ?: "Address not available"
                    val createdAt = document.get("createdAt") as? Timestamp
                    val dateText = createdAt?.toDate()?.let { formatDate(it) }
                        ?: "Date not available"

                    addOrderCard(
                        orderId = document.id,
                        status = status,
                        total = total,
                        address = address,
                        date = dateText
                    )
                }
            }
            .addOnFailureListener { error ->
                refreshOrdersButton.isEnabled = true
                refreshOrdersButton.text = "Refresh Orders"
                emptyOrdersText.text =
                    "Could not load orders. ${error.localizedMessage ?: "Please try again."}"
                emptyOrdersText.visibility = View.VISIBLE
            }
    }

    private fun addOrderCard(
        orderId: String,
        status: String,
        total: Double,
        address: String,
        date: String
    ) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            setBackgroundColor(android.graphics.Color.WHITE)
            isClickable = true
            isFocusable = true
        }

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = dp(12)
        }
        card.layoutParams = params

        card.addView(makeText("Order ID: $orderId", 14f, "#003049", true))
        card.addView(makeText("Status: $status", 17f, "#C1121F", true))
        card.addView(makeText("Total: ${money(total)}", 16f, "#003049", true))
        card.addView(makeText("Date: $date", 14f, "#444444", false))
        card.addView(makeText("Delivery address: $address", 14f, "#444444", false))

        card.addView(
            makeText("Tap to track this order →", 14f, "#003049", true).apply {
                setPadding(0, dp(10), 0, 0)
            }
        )

        card.setOnClickListener {
            val intent = Intent(this, TrackOrder::class.java)
            intent.putExtra("ORDER_ID", orderId)
            startActivity(intent)
        }

        ordersContainer.addView(card)
    }

    private fun makeText(
        value: String,
        size: Float,
        color: String,
        bold: Boolean
    ): TextView {
        return TextView(this).apply {
            text = value
            textSize = size
            setTextColor(android.graphics.Color.parseColor(color))
            if (bold) setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, dp(4), 0, dp(4))
        }
    }

    private fun formatDate(date: Date): String {
        return SimpleDateFormat(
            "dd MMM yyyy, HH:mm",
            Locale.getDefault()
        ).format(date)
    }

    private fun money(amount: Double): String {
        return String.format(Locale.getDefault(), "R%.2f", amount)
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}