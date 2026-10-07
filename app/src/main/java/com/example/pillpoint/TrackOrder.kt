package com.example.pillpoint

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class TrackOrder : AppCompatActivity() {

    private lateinit var etOrderNumber: EditText
    private lateinit var btnTrackOrder: Button
    private lateinit var llOrderDetails: LinearLayout
    private lateinit var tvOrderStatus: TextView
    private lateinit var tvOrderDate: TextView
    private lateinit var tvDeliveryAddress: TextView
    private lateinit var tvEstimatedDelivery: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_track_order)

        initViews()
        setupClickListeners()
    }

    private fun initViews() {
        etOrderNumber = findViewById(R.id.orderNumber)
        btnTrackOrder = findViewById(R.id.trackOrderButton)
        llOrderDetails = findViewById(R.id.orderDetailsLayout)
        tvOrderStatus = findViewById(R.id.orderStatus)
        tvOrderDate = findViewById(R.id.orderDate)
        tvDeliveryAddress = findViewById(R.id.deliveryAddress)
        tvEstimatedDelivery = findViewById(R.id.estimatedDelivery)

        // Initially hide order details until an order number is searched
        llOrderDetails.visibility = View.GONE
    }

    private fun setupClickListeners() {
        btnTrackOrder.setOnClickListener {
            val orderNum = etOrderNumber.text.toString().trim()

            if (orderNum.isEmpty()) {
                etOrderNumber.error = "Please enter an order number"
                etOrderNumber.requestFocus()
                return@setOnClickListener
            }

            // Fetch and update order status details
            displayOrderDetails(orderNum)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun onBackPressed(view: View) {
        finish()
    }

    private fun displayOrderDetails(orderNum: String) {
        // Populate tracking details dynamically
        tvOrderStatus.text = "Status: Out for Delivery"
        tvOrderDate.text = "Order Date: Oct 24, 2026"
        tvDeliveryAddress.text = "Delivery Address: 123 Main Street, Apt 4B"
        tvEstimatedDelivery.text = "Estimated Delivery: Today by 5:00 PM"

        // Make the order details section visible
        llOrderDetails.visibility = View.VISIBLE

        Toast.makeText(this, "Order #$orderNum found", Toast.LENGTH_SHORT).show()
    }
}