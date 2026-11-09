package com.example.pillpoint

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class Checkout : AppCompatActivity() {

    private lateinit var btnBack: Button
    private lateinit var etFullName: EditText
    private lateinit var etPhone: EditText
    private lateinit var etDeliveryAddress: EditText
    private lateinit var tvStoreLocator: TextView
    private lateinit var rgPaymentMethod: RadioGroup
    private lateinit var layoutCardDetails: LinearLayout
    private lateinit var etCardNumber: EditText
    private lateinit var etExpiry: EditText
    private lateinit var etCvv: EditText
    private lateinit var tvDeliveryFee: TextView
    private lateinit var tvTotalAmount: TextView
    private lateinit var btnPlaceOrder: Button

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val deliveryFeeAmount = 35.00 // Updated delivery fee

    private var subtotalAmount = 0.0
    private var totalAmount = 0.0
    private var isPlacingOrder = false

    // Result launcher to handle returning selected pharmacy from Store_Locator
    private val storeLocatorLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val pharmacyName = result.data?.getStringExtra("SELECTED_PHARMACY_NAME") ?: ""
            val pharmacyAddress = result.data?.getStringExtra("SELECTED_PHARMACY_ADDRESS") ?: ""

            // Auto-populate delivery address with the selected pharmacy
            val combinedAddress = if (pharmacyAddress.isNotBlank()) "$pharmacyName, $pharmacyAddress" else pharmacyName
            etDeliveryAddress.setText(combinedAddress)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_checkout)

        initViews()
        loadOrderData()
        calculateTotals()
        setupListeners()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        etFullName = findViewById(R.id.etFullName)
        etPhone = findViewById(R.id.etPhone)
        etDeliveryAddress = findViewById(R.id.etDeliveryAddress)
        tvStoreLocator = findViewById(R.id.tvStoreLocator)
        rgPaymentMethod = findViewById(R.id.rgPaymentMethod)
        layoutCardDetails = findViewById(R.id.layoutCardDetails)
        etCardNumber = findViewById(R.id.etCardNumber)
        etExpiry = findViewById(R.id.etExpiry)
        etCvv = findViewById(R.id.etCvv)
        tvDeliveryFee = findViewById(R.id.tvDeliveryFee)
        tvTotalAmount = findViewById(R.id.tvTotalAmount)
        btnPlaceOrder = findViewById(R.id.btnPlaceOrder)
    }

    private fun loadOrderData() {
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

        tvDeliveryFee.text = String.format("R%.2f", fee)
        tvTotalAmount.text = String.format("R%.2f", totalAmount)

        // Dynamically update button text with grand total
        btnPlaceOrder.text = String.format("Confirm & Place Order (R%.2f)", totalAmount)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        // Toggle visibility of card inputs based on payment option selection
        rgPaymentMethod.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.rbCard) {
                layoutCardDetails.visibility = View.VISIBLE
            } else {
                layoutCardDetails.visibility = View.GONE
            }
        }

        tvStoreLocator.setOnClickListener {
            val intent = Intent(this, Store_Locator::class.java)
            storeLocatorLauncher.launch(intent)
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

        val checkedPaymentId = rgPaymentMethod.checkedRadioButtonId
        if (checkedPaymentId == -1) {
            Toast.makeText(
                this,
                "Please select a payment method",
                Toast.LENGTH_SHORT
            ).show()
            return false
        }

        // Card validation if Credit/Debit card is chosen
        if (checkedPaymentId == R.id.rbCard) {
            val cardNumber = etCardNumber.text.toString().trim()
            val expiry = etExpiry.text.toString().trim()
            val cvv = etCvv.text.toString().trim()

            if (cardNumber.length < 15) {
                etCardNumber.error = "Enter a valid card number"
                etCardNumber.requestFocus()
                return false
            }
            if (expiry.isEmpty()) {
                etExpiry.error = "Enter expiry date (MM/YY)"
                etExpiry.requestFocus()
                return false
            }
            if (cvv.length < 3) {
                etCvv.error = "Enter a valid CVV"
                etCvv.requestFocus()
                return false
            }
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

        val cartSnapshot = CartRepository.items.toList()

        if (cartSnapshot.isEmpty()) {
            Toast.makeText(
                this,
                "Your cart is empty.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val paymentMethod = when (rgPaymentMethod.checkedRadioButtonId) {
            R.id.rbCard -> "Credit/Debit Card"
            R.id.rbEft -> "Instant EFT"
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

        db.collection("orders")
            .add(orderData)
            .addOnSuccessListener { orderDocument ->
                val orderId = orderDocument.id

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
                        val errorText = "Order $orderId was saved, but the cart could not be cleared. $message"
                        Toast.makeText(this@Checkout, errorText, Toast.LENGTH_LONG).show()
                        finish()
                    }
                )
            }
            .addOnFailureListener { exception ->
                isPlacingOrder = false
                btnPlaceOrder.isEnabled = true
                calculateTotals() // Reset button text

                Toast.makeText(
                    this,
                    "Could not save your order: ${exception.message ?: "Please try again."}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
}