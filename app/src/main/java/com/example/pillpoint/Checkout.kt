package com.example.pillpoint

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

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

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val deliveryFeeAmount = 50.00

    private var subtotalAmount = 0.0
    private var totalAmount = 0.0
    private var isPlacingOrder = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_checkout)

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
        // Use the current cart subtotal where possible.
        subtotalAmount = if (CartRepository.items.isNotEmpty()) {
            CartRepository.getSubtotal()
        } else {
            intent.getDoubleExtra("EXTRA_SUBTOTAL", 0.0)
        }
    }

    private fun calculateTotals() {
        val fee = if (subtotalAmount > 0.0) {
            deliveryFeeAmount
        } else {
            0.0
        }

        totalAmount = subtotalAmount + fee

        tvSubtotal.text = String.format("R%.2f", subtotalAmount)
        tvDeliveryFee.text = String.format("R%.2f", fee)
        tvTotalAmount.text = String.format("R%.2f", totalAmount)
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        btnPlaceOrder.setOnClickListener {
            if (!isPlacingOrder && validateInputs()) {
                processOrder()
            }
        }
    }

    private fun validateInputs(): Boolean {
        val name = etFullName.text.toString().trim()
        val phone = etPhone.text.toString().trim()
        val address = etDeliveryAddress.text.toString().trim()

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
            etDeliveryAddress.error = "Please enter your delivery address"
            etDeliveryAddress.requestFocus()
            return false
        }

        if (rgPaymentMethod.checkedRadioButtonId == -1) {
            Toast.makeText(
                this,
                "Please select a payment method",
                Toast.LENGTH_SHORT
            ).show()
            return false
        }

        if (CartRepository.items.isEmpty()) {
            Toast.makeText(
                this,
                "Your cart is empty. Please add products first.",
                Toast.LENGTH_LONG
            ).show()
            return false
        }

        return true
    }

    private fun processOrder() {
        val user = auth.currentUser

        if (user == null) {
            Toast.makeText(
                this,
                "Please log in before placing an order.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        // Take a snapshot of the products being ordered.
        val cartSnapshot = CartRepository.items.toList()

        if (cartSnapshot.isEmpty()) {
            Toast.makeText(
                this,
                "Your cart is empty.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // Identify the selected payment method.
        val paymentMethod = when (rgPaymentMethod.checkedRadioButtonId) {
            R.id.rbCard -> "Credit/Debit Card"
            R.id.rbCod -> "Cash on Delivery"
            else -> {
                Toast.makeText(
                    this,
                    "Please select a payment method.",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }
        }

        // Prepare the products for the Firestore order document.
        val orderItems = cartSnapshot.map { item ->
            hashMapOf<String, Any>(
                "productId" to item.id,
                "name" to item.name,
                "price" to item.price,
                "quantity" to item.quantity,
                "lineTotal" to (item.price * item.quantity)
            )
        }

        val currentSubtotal = cartSnapshot.sumOf {
            it.price * it.quantity
        }

        val currentDeliveryFee =
            if (currentSubtotal > 0.0) deliveryFeeAmount else 0.0

        val currentTotal = currentSubtotal + currentDeliveryFee

        val orderData = hashMapOf<String, Any>(
            "userId" to user.uid,
            "customerName" to etFullName.text.toString().trim(),
            "phone" to etPhone.text.toString().trim(),
            "deliveryAddress" to etDeliveryAddress.text.toString().trim(),
            "paymentMethod" to paymentMethod,
            "items" to orderItems,
            "subtotal" to currentSubtotal,
            "deliveryFee" to currentDeliveryFee,
            "total" to currentTotal,
            "status" to "Pending",
            "createdAt" to FieldValue.serverTimestamp()
        )

        isPlacingOrder = true
        btnPlaceOrder.isEnabled = false
        btnPlaceOrder.text = "Placing Order..."

        // Create a new order document in Firestore.
        db.collection("orders")
            .add(orderData)
            .addOnSuccessListener { orderDocument ->

                val orderId = orderDocument.id

                // Clear the cart only after the order was saved.
                CartRepository.clearCart(
                    onSuccess = {
                        Toast.makeText(
                            this,
                            "Order placed successfully! Order ID: $orderId",
                            Toast.LENGTH_LONG
                        ).show()

                        finish()
                    },
                    onError = { message ->
                        // The order exists, even if clearing the cart fails.
                        Toast.makeText(
                            this,
                            "Order $orderId was saved, but the cart could not be cleared. $message",
                            Toast.LENGTH_LONG
                        ).show()

                        finish()
                    }
                )
            }
            .addOnFailureListener { exception ->
                isPlacingOrder = false
                btnPlaceOrder.isEnabled = true
                btnPlaceOrder.text = "Place Order"

                Toast.makeText(
                    this,
                    "Could not save your order: ${
                        exception.message ?: "Please try again."
                    }",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
}