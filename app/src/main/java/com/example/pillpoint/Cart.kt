package com.example.pillpoint

import android.content.Intent
import android.graphics.Color
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cart)

        initViews()
        renderCart()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        renderCart()
    }

    private fun initViews() {
        cartItemsContainer = findViewById(R.id.cartItemsContainer)
        txtSubtotal = findViewById(R.id.txtSubtotal)
        txtDelivery = findViewById(R.id.txtDelivery)
        txtTotal = findViewById(R.id.txtTotal)
        btnContinueShopping = findViewById(R.id.btnContinueShopping)
        btnCheckout = findViewById(R.id.btnCheckout)
    }

    private fun renderCart() {
        cartItemsContainer.removeAllViews()

        if (CartRepository.items.isEmpty()) {
            val emptyTextView = TextView(this).apply {
                text = "Your cart is empty"
                textSize = 18f
                gravity = Gravity.CENTER
                setPadding(0, 40, 0, 40)
            }
            cartItemsContainer.addView(emptyTextView)
        } else {
            for (item in CartRepository.items) {
                cartItemsContainer.addView(createItemView(item))
            }
        }

        updateSummary()
    }

    private fun createItemView(item: CartItem): LinearLayout {
        val context = this

        val itemLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(20, 20, 20, 20)
            setBackgroundColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 20
            }
        }

        val imageView = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(180, 180)
            setImageResource(item.imageResId)
        }

        val infoLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                setPadding(24, 0, 0, 0)
            }
        }

        val nameText = TextView(context).apply {
            text = item.name
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        val priceText = TextView(context).apply {
            text = String.format("Price: R%.2f", item.price)
        }

        val qtyText = TextView(context).apply {
            text = "Quantity: ${item.quantity}"
        }

        infoLayout.addView(nameText)
        infoLayout.addView(priceText)
        infoLayout.addView(qtyText)

        val btnRemove = Button(context).apply {
            text = "Remove"
            setBackgroundColor(Color.parseColor("#0A2540"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                CartRepository.removeItem(item)
                renderCart()
            }
        }

        itemLayout.addView(imageView)
        itemLayout.addView(infoLayout)
        itemLayout.addView(btnRemove)

        return itemLayout
    }

    private fun updateSummary() {
        val subtotal = CartRepository.getSubtotal()
        val total = if (subtotal > 0) subtotal + deliveryFee else 0.0

        txtSubtotal.text = String.format("Subtotal: R%.2f", subtotal)
        txtDelivery.text = String.format("Delivery Fee: R%.2f", if (subtotal > 0) deliveryFee else 0.0)
        txtTotal.text = String.format("Total: R%.2f", total)
    }

    private fun setupListeners() {
        btnContinueShopping.setOnClickListener {
            finish()
        }

        btnCheckout.setOnClickListener {
            if (CartRepository.items.isEmpty()) {
                Toast.makeText(this, "Your cart is empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val intent = Intent(this, Checkout::class.java).apply {
                putExtra("EXTRA_SUBTOTAL", CartRepository.getSubtotal())
            }
            startActivity(intent)
        }
    }
}