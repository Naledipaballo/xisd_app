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

    private val deliveryFeeAmount = 35.00

    private var subtotalAmount = 0.0
    private var totalAmount = 0.0
    private var isPlacingOrder = false

    // Selected pharmacy information
    private var selectedPharmacyId = ""
    private var selectedPharmacyName = ""
    private var selectedPharmacyAddress = ""
    private var selectedPharmacyPhone = ""

    // Receive the selected pharmacy from Store_Locator
    private val storeLocatorLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->

        if (result.resultCode == RESULT_OK) {

            val data = result.data

            selectedPharmacyId =
                data?.getStringExtra("SELECTED_PHARMACY_ID") ?: ""

            selectedPharmacyName =
                data?.getStringExtra("SELECTED_PHARMACY_NAME") ?: ""

            selectedPharmacyAddress =
                data?.getStringExtra("SELECTED_PHARMACY_ADDRESS") ?: ""

            selectedPharmacyPhone =
                data?.getStringExtra("SELECTED_PHARMACY_PHONE") ?: ""

            if (selectedPharmacyId.isNotBlank()) {

                // Display selected pharmacy.
                tvStoreLocator.text =
                    "Selected Pharmacy: $selectedPharmacyName\n" +
                            "$selectedPharmacyAddress\n" +
                            "Phone: $selectedPharmacyPhone\n" +
                            "Tap to change pharmacy"

                Toast.makeText(
                    this,
                    "$selectedPharmacyName selected successfully",
                    Toast.LENGTH_SHORT
                ).show()

            } else {

                Toast.makeText(
                    this,
                    "Could not retrieve pharmacy information.",
                    Toast.LENGTH_SHORT
                ).show()
            }
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

    // ---------------------------------------------------------
    // INITIALISE VIEWS
    // ---------------------------------------------------------

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

    // ---------------------------------------------------------
    // LOAD CART DATA
    // ---------------------------------------------------------

    private fun loadOrderData() {

        subtotalAmount = if (CartRepository.items.isNotEmpty()) {

            CartRepository.getSubtotal()

        } else {

            intent.getDoubleExtra("EXTRA_SUBTOTAL", 0.0)
        }
    }

    // ---------------------------------------------------------
    // CALCULATE TOTALS
    // ---------------------------------------------------------

    private fun calculateTotals() {

        val fee = if (subtotalAmount > 0.0) {
            deliveryFeeAmount
        } else {
            0.0
        }

        totalAmount = subtotalAmount + fee

        tvDeliveryFee.text = String.format("R%.2f", fee)

        tvTotalAmount.text = String.format("R%.2f", totalAmount)

        btnPlaceOrder.text = String.format(
            "Confirm & Place Order (R%.2f)",
            totalAmount
        )
    }

    // ---------------------------------------------------------
    // BUTTON LISTENERS
    // ---------------------------------------------------------

    private fun setupListeners() {

        btnBack.setOnClickListener {
            finish()
        }

        // Show or hide card details.
        rgPaymentMethod.setOnCheckedChangeListener { _, checkedId ->

            if (checkedId == R.id.rbCard) {

                layoutCardDetails.visibility = View.VISIBLE

            } else {

                layoutCardDetails.visibility = View.GONE
            }
        }

        // Open pharmacy Store Locator.
        tvStoreLocator.setOnClickListener {

            val intent = Intent(
                this,
                Store_Locator::class.java
            )

            storeLocatorLauncher.launch(intent)
        }

        // Place order.
        btnPlaceOrder.setOnClickListener {

            if (!isPlacingOrder && validateInputs()) {

                processOrder()
            }
        }
    }

    // ---------------------------------------------------------
    // VALIDATE CHECKOUT INFORMATION
    // ---------------------------------------------------------

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

        val checkedPaymentId =
            rgPaymentMethod.checkedRadioButtonId

        if (checkedPaymentId == -1) {

            Toast.makeText(
                this,
                "Please select a payment method",
                Toast.LENGTH_SHORT
            ).show()

            return false
        }

        // Validate card details when card payment is selected.
        if (checkedPaymentId == R.id.rbCard) {

            val cardNumber =
                etCardNumber.text.toString().trim()

            val expiry =
                etExpiry.text.toString().trim()

            val cvv =
                etCvv.text.toString().trim()

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

    // ---------------------------------------------------------
    // SAVE ORDER TO FIRESTORE
    // ---------------------------------------------------------

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

        // Determine payment method.
        val paymentMethod = when (
            rgPaymentMethod.checkedRadioButtonId
        ) {

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

        // Prepare cart products.
        val orderItems = cartSnapshot.map { item ->

            hashMapOf<String, Any>(
                "productId" to item.id,
                "name" to item.name,
                "price" to item.price,
                "quantity" to item.quantity,
                "lineTotal" to (item.price * item.quantity)
            )
        }

        // Calculate order amounts.
        val currentSubtotal = cartSnapshot.sumOf {
            it.price * it.quantity
        }

        val currentDeliveryFee =
            if (currentSubtotal > 0.0) deliveryFeeAmount else 0.0

        val currentTotal =
            currentSubtotal + currentDeliveryFee

        // Main order information.
        val orderData = hashMapOf<String, Any>(

            "userId" to user.uid,

            "customerName" to
                    etFullName.text.toString().trim(),

            "phone" to
                    etPhone.text.toString().trim(),

            "deliveryAddress" to
                    etDeliveryAddress.text.toString().trim(),

            "paymentMethod" to paymentMethod,

            "items" to orderItems,

            "subtotal" to currentSubtotal,

            "deliveryFee" to currentDeliveryFee,

            "total" to currentTotal,

            "status" to "Pending",

            "createdAt" to FieldValue.serverTimestamp()
        )

        // Add selected pharmacy details if a pharmacy was selected.
        if (selectedPharmacyId.isNotBlank()) {

            orderData["pharmacyId"] = selectedPharmacyId

            orderData["pharmacyName"] = selectedPharmacyName

            orderData["pharmacyAddress"] = selectedPharmacyAddress

            orderData["pharmacyPhone"] = selectedPharmacyPhone
        }

        isPlacingOrder = true

        btnPlaceOrder.isEnabled = false

        btnPlaceOrder.text = "Placing Order..."

        // Save order in Cloud Firestore.
        db.collection("orders")
            .add(orderData)

            .addOnSuccessListener { orderDocument ->

                val orderId = orderDocument.id

                // Clear cart after successful order creation.
                CartRepository.clearCart(

                    onSuccess = {

                        Toast.makeText(
                            this,
                            "Order placed successfully! Order ID: $orderId",
                            Toast.LENGTH_LONG
                        ).show()

                        finish()
                    },

                    onError = { errorMessage ->

                        Toast.makeText(
                            this,
                            "Order $orderId was saved, but the cart could not be cleared. $errorMessage",
                            Toast.LENGTH_LONG
                        ).show()

                        finish()
                    }
                )
            }

            .addOnFailureListener { exception ->

                isPlacingOrder = false

                btnPlaceOrder.isEnabled = true

                calculateTotals()

                Toast.makeText(
                    this,
                    "Could not save your order: ${exception.message ?: "Please try again."}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
}