package com.example.pillpoint

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class Checkout : AppCompatActivity() {

    private lateinit var btnBack: Button
    private lateinit var etFullName: EditText
    private lateinit var etPhone: EditText
    private lateinit var etDeliveryAddress: EditText
    private lateinit var rgPaymentMethod: RadioGroup
    private lateinit var tvSubtotal: TextView
    private lateinit var tvDeliveryFee: TextView
    private lateinit var tvTotalAmount: TextView
    private lateinit var btnPlaceOrder: Button

    private var subtotalAmount: Double = 0.0
    private var deliveryFeeAmount: Double = 50.00 // Default delivery fee
    private var totalAmount: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_checkout) // Ensure layout XML is named activity_checkout.xml

        initViews()
        loadOrderData()
        calculateTotals()
        setupClickListeners()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        etFullName = findViewById(R.id.etFullName)
        etPhone = findViewById(R.id.etPhone)
        etDeliveryAddress = findViewById(R.id.etDeliveryAddress)
        rgPaymentMethod = findViewById(R.id.rgPaymentMethod)
        tvSubtotal = findViewById(R.id.tvSubtotal)
        tvDeliveryFee = findViewById(R.id.tvDeliveryFee)
        tvTotalAmount = findViewById(R.id.tvTotalAmount)
        btnPlaceOrder = findViewById(R.id.btnPlaceOrder)
    }

    private fun loadOrderData() {
        // Get subtotal passed from previous activity (e.g., Cart)
        subtotalAmount = intent.getDoubleExtra("EXTRA_SUBTOTAL", 0.0)
    }

    private fun calculateTotals() {
        totalAmount = subtotalAmount + deliveryFeeAmount

        tvSubtotal.text = String.format("R%.2f", subtotalAmount)
        tvDeliveryFee.text = String.format("R%.2f", deliveryFeeAmount)
        tvTotalAmount.text = String.format("R%.2f", totalAmount)
    }

    private fun setupClickListeners() {
        // Go back to previous screen
        btnBack.setOnClickListener {
            finish()
        }

        // Handle Place Order action
        btnPlaceOrder.setOnClickListener {
            if (validateInputs()) {
                processOrder()
            }
        }
    }

    private fun validateInputs(): Boolean {
        val name = etFullName.text.toString().trim()
        val phone = etPhone.text.toString().trim()
        val address = etDeliveryAddress.text.toString().trim()
        val selectedPaymentId = rgPaymentMethod.checkedRadioButtonId

        if (name.isEmpty()) {
            etFullName.error = "Please enter your full name"
            etFullName.requestFocus()
            return false
        }

        if (phone.isEmpty()) {
            etPhone.error = "Please enter your phone number"
            etPhone.requestFocus()
            return false
        }

        if (address.isEmpty()) {
            etDeliveryAddress.error = "Please enter delivery address"
            etDeliveryAddress.requestFocus()
            return false
        }

        if (selectedPaymentId == -1) {
            Toast.makeText(this, "Please select a payment method", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun processOrder() {
        // TODO: Integrate order API / Database call to save order details
        Toast.makeText(this, "Order placed successfully!", Toast.LENGTH_LONG).show()

        // Navigate to Track Order or Home activity after placing order
        finish()
    }
}