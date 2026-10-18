package com.example.pillpoint

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class Cart : AppCompatActivity() {

    private lateinit var cartItemsContainer: LinearLayout
    private lateinit var txtSubtotal: TextView
    private lateinit var txtDelivery: TextView
    private lateinit var txtTotal: TextView
    private lateinit var btnContinueShopping: Button
    private lateinit var btnCheckout: Button

    private val deliveryFee = 50.00
    private var isLoading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cart)

        initViews()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        loadCart()
    }

    private fun initViews() {
        cartItemsContainer = findViewById(R.id.cartItemsContainer)
        txtSubtotal = findViewById(R.id.txtSubtotal)
        txtDelivery = findViewById(R.id.txtDelivery)
        txtTotal = findViewById(R.id.txtTotal)
        btnContinueShopping = findViewById(R.id.btnContinueShopping)
        btnCheckout = findViewById(R.id.btnCheckout)
    }

    // Retrieve the signed-in user's cart from Firestore via CartRepository.
    private fun loadCart() {
        if (isLoading) return
        isLoading = true
        btnCheckout.isEnabled = false

        CartRepository.loadItems(
            context = this,
            onSuccess = { loadedItems ->
                isLoading = false
                btnCheckout.isEnabled = true
                renderCart(loadedItems)
            },
            onError = { message ->
                isLoading = false
                btnCheckout.isEnabled = true

                Toast.makeText(
                    this,
                    message,
                    Toast.LENGTH_LONG
                ).show()

                renderCart(CartRepository.items.toList())
            }
        )
    }

    private fun renderCart(items: List<CartItem>) {
        cartItemsContainer.removeAllViews()

        if (items.isEmpty()) {
            val emptyTextView = TextView(this).apply {
                text = "Your cart is empty"
                textSize = 18f
                gravity = Gravity.CENTER
                setPadding(0, 40, 0, 40)
            }

            cartItemsContainer.addView(emptyTextView)
        } else {
            items.forEach { item ->
                cartItemsContainer.addView(createItemView(item))
            }
        }

        updateSummary()
        btnCheckout.isEnabled = items.isNotEmpty() && !isLoading
    }

    private fun createItemView(item: CartItem): LinearLayout {
        val itemLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(20, 20, 20, 20)
            setBackgroundColor(Color.WHITE)

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 20
            }
        }

        val imageView = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(180, 180)

            if (item.imageResId != 0) {
                setImageResource(item.imageResId)
            } else {
                setImageDrawable(null)
            }

            contentDescription = item.name
            scaleType = ImageView.ScaleType.FIT_CENTER
        }

        val infoLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL

            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                setMargins(16, 0, 12, 0)
            }
        }

        val nameText = TextView(this).apply {
            text = item.name
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
        }

        val priceText = TextView(this).apply {
            text = String.format("Price: R%.2f", item.price)
            textSize = 14f
        }

        val qtyText = TextView(this).apply {
            text = "Quantity: ${item.quantity}"
            textSize = 14f
        }

        val lineTotalText = TextView(this).apply {
            text = String.format(
                "Item total: R%.2f",
                item.price * item.quantity
            )
            textSize = 14f
        }

        infoLayout.addView(nameText)
        infoLayout.addView(priceText)
        infoLayout.addView(qtyText)
        infoLayout.addView(lineTotalText)

        val btnRemove = Button(this).apply {
            text = "Remove"
            setBackgroundColor(Color.parseColor("#0A2540"))
            setTextColor(Color.WHITE)

            setOnClickListener {
                isEnabled = false

                CartRepository.removeItem(
                    item = item,
                    onSuccess = {
                        Toast.makeText(
                            this@Cart,
                            "${item.name} removed",
                            Toast.LENGTH_SHORT
                        ).show()

                        loadCart()
                    },
                    onError = { message ->
                        isEnabled = true

                        Toast.makeText(
                            this@Cart,
                            message,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                )
            }
        }

        itemLayout.addView(imageView)
        itemLayout.addView(infoLayout)
        itemLayout.addView(btnRemove)

        return itemLayout
    }

    private fun updateSummary() {
        val subtotal = CartRepository.getSubtotal()
        val fee = if (subtotal > 0.0) deliveryFee else 0.0
        val total = subtotal + fee

        txtSubtotal.text = String.format(
            "Subtotal: R%.2f",
            subtotal
        )

        txtDelivery.text = String.format(
            "Delivery Fee: R%.2f",
            fee
        )

        txtTotal.text = String.format(
            "Total: R%.2f",
            total
        )
    }

    private fun setupListeners() {
        btnContinueShopping.setOnClickListener {
            finish()
        }

        btnCheckout.setOnClickListener {
            if (isLoading) {
                Toast.makeText(
                    this,
                    "Please wait while your cart loads.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            if (CartRepository.items.isEmpty()) {
                Toast.makeText(
                    this,
                    "Your cart is empty",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val intent = Intent(this, Checkout::class.java).apply {
                putExtra(
                    "EXTRA_SUBTOTAL",
                    CartRepository.getSubtotal()
                )
            }

            startActivity(intent)
        }
    }
}