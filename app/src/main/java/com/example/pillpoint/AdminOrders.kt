package com.example.pillpoint

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.Locale

class AdminOrders : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var orderContainer: LinearLayout
    private lateinit var progressBar: ProgressBar

    private var orderListener: ListenerRegistration? = null
    private var adminVerified = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_orders)

        db = FirebaseFirestore.getInstance()

        orderContainer = findViewById(R.id.adminOrderContainer)
        progressBar = findViewById(R.id.ordersProgressBar)

        verifyAdmin()
    }

    private fun verifyAdmin() {
        val user = FirebaseAuth.getInstance().currentUser

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
                    loadOrders()
                } else {
                    FirebaseAuth.getInstance().signOut()
                    openAdminLogin()
                }
            }
            .addOnFailureListener {

                if (isFinishing || isDestroyed) return@addOnFailureListener

                Toast.makeText(
                    this,
                    "Unable to verify admin access.",
                    Toast.LENGTH_LONG
                ).show()

                openAdminLogin()
            }
    }

    private fun loadOrders() {

        orderListener?.remove()
        orderListener = null

        progressBar.visibility = View.VISIBLE

        orderListener = db.collection("orders")
            .addSnapshotListener { snapshot, error ->

                if (isFinishing || isDestroyed) return@addSnapshotListener

                progressBar.visibility = View.GONE

                if (error != null) {
                    Toast.makeText(
                        this,
                        "Error loading orders: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    return@addSnapshotListener
                }

                orderContainer.removeAllViews()

                if (snapshot == null || snapshot.isEmpty) {
                    addMessage("No orders have been received yet.")
                    return@addSnapshotListener
                }

                val orders = snapshot.documents.sortedByDescending {
                    it.getTimestamp("createdAt")?.seconds ?: 0L
                }

                for (document in orders) {

                    val customerName =
                        document.getString("customerName") ?: "Unknown Customer"

                    val status =
                        document.getString("status") ?: "Pending"

                    val total =
                        document.getDouble("total") ?: 0.0

                    val date = document.getTimestamp("createdAt")
                        ?.toDate()?.let {
                            SimpleDateFormat(
                                "dd MMM yyyy, HH:mm",
                                Locale.getDefault()
                            ).format(it)
                        } ?: "Date unavailable"

                    addOrderCard(
                        document.id,
                        customerName,
                        status,
                        total,
                        date
                    )
                }
            }
    }

    private fun addOrderCard(
        orderId: String,
        customerName: String,
        status: String,
        total: Double,
        date: String
    ) {

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 18, 20, 18)
            setBackgroundResource(
                android.R.drawable.dialog_holo_light_frame
            )
        }

        fun addText(
            value: String,
            size: Float,
            color: Int,
            bold: Boolean = false
        ) {
            val text = TextView(this).apply {
                this.text = value
                textSize = size
                setTextColor(color)

                if (bold) {
                    setTypeface(
                        null,
                        android.graphics.Typeface.BOLD
                    )
                }

                setPadding(0, 5, 0, 5)
            }

            card.addView(text)
        }

        addText(
            "Order ID: $orderId",
            13f,
            0xFF555555.toInt()
        )

        addText(
            "Customer: $customerName",
            16f,
            0xFF003049.toInt(),
            true
        )

        addText(
            "Date: $date",
            13f,
            0xFF555555.toInt()
        )

        addText(
            "Total: R${"%.2f".format(Locale.US, total)}",
            15f,
            0xFF003049.toInt(),
            true
        )

        addText(
            "Status: $status",
            14f,
            0xFFC1121F.toInt(),
            true
        )

        val button = Button(this).apply {

            text = "VIEW ORDER DETAILS"

            setTextColor(0xFFFFFFFF.toInt())

            setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(
                    0xFF003049.toInt()
                )
            )

            setOnClickListener {

                val intent = Intent(
                    this@AdminOrders,
                    AdminOrderDetails::class.java
                )

                intent.putExtra("ORDER_ID", orderId)

                startActivity(intent)
            }
        }

        card.addView(button)

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        params.bottomMargin = 16

        orderContainer.addView(card, params)
    }

    private fun addMessage(message: String) {

        val text = TextView(this).apply {
            this.text = message
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(0xFF003049.toInt())
            setPadding(20, 40, 20, 40)
        }

        orderContainer.addView(text)
    }

    private fun openAdminLogin() {

        val intent = Intent(
            this,
            AdminLogin::class.java
        )

        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intent)
        finish()
    }

    override fun onStop() {
        super.onStop()

        orderListener?.remove()
        orderListener = null
    }

    override fun onDestroy() {
        orderListener?.remove()
        orderListener = null

        super.onDestroy()
    }
}