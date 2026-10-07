package com.example.pillpoint

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class Shop : AppCompatActivity() {

    private lateinit var btnBack: Button
    private lateinit var btnViewCart: Button

    // 19 Add to Cart Buttons
    private lateinit var btnAddCart1: Button
    private lateinit var btnAddCart2: Button
    private lateinit var btnAddCart3: Button
    private lateinit var btnAddCart4: Button
    private lateinit var btnAddCart5: Button
    private lateinit var btnAddCart6: Button
    private lateinit var btnAddCart7: Button
    private lateinit var btnAddCart8: Button
    private lateinit var btnAddCart9: Button
    private lateinit var btnAddCart10: Button
    private lateinit var btnAddCart11: Button
    private lateinit var btnAddCart12: Button
    private lateinit var btnAddCart13: Button
    private lateinit var btnAddCart14: Button
    private lateinit var btnAddCart15: Button
    private lateinit var btnAddCart16: Button
    private lateinit var btnAddCart17: Button
    private lateinit var btnAddCart18: Button
    private lateinit var btnAddCart19: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shop)

        initViews()
        setupClickListeners()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        btnViewCart = findViewById(R.id.btnViewCart)

        btnAddCart1 = findViewById(R.id.btnAddCart1)
        btnAddCart2 = findViewById(R.id.btnAddCart2)
        btnAddCart3 = findViewById(R.id.btnAddCart3)
        btnAddCart4 = findViewById(R.id.btnAddCart4)
        btnAddCart5 = findViewById(R.id.btnAddCart5)
        btnAddCart6 = findViewById(R.id.btnAddCart6)
        btnAddCart7 = findViewById(R.id.btnAddCart7)
        btnAddCart8 = findViewById(R.id.btnAddCart8)
        btnAddCart9 = findViewById(R.id.btnAddCart9)
        btnAddCart10 = findViewById(R.id.btnAddCart10)
        btnAddCart11 = findViewById(R.id.btnAddCart11)
        btnAddCart12 = findViewById(R.id.btnAddCart12)
        btnAddCart13 = findViewById(R.id.btnAddCart13)
        btnAddCart14 = findViewById(R.id.btnAddCart14)
        btnAddCart15 = findViewById(R.id.btnAddCart15)
        btnAddCart16 = findViewById(R.id.btnAddCart16)
        btnAddCart17 = findViewById(R.id.btnAddCart17)
        btnAddCart18 = findViewById(R.id.btnAddCart18)
        btnAddCart19 = findViewById(R.id.btnAddCart19)
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        btnViewCart.setOnClickListener {
            startActivity(Intent(this, Cart::class.java))
        }

        // 1. Allergex Syrup (R18.99)
        btnAddCart1.setOnClickListener {
            addToCart("allergex_syrup", "Allergex Syrup", 18.99, R.drawable.allergexsyrup)
        }

        // 2. Allergex Tablets (R25.99)
        btnAddCart2.setOnClickListener {
            addToCart("allergex_tablets", "Allergex Tablets", 25.99, R.drawable.allergextablets)
        }

        // 3. Andolex Oral Gel (R25.99)
        btnAddCart3.setOnClickListener {
            addToCart("andolex_oral_gel", "Andolex Oral Gel", 25.99, R.drawable.andolexcoralgel)
        }

        // 4. Band-Aid Plastic (R39.99)
        btnAddCart4.setOnClickListener {
            addToCart("band_aid_plastic", "Band-Aid Plastic", 39.99, R.drawable.bandaidplastic)
        }

        // 5. Benelyn Syrup (R79.99)
        btnAddCart5.setOnClickListener {
            addToCart("benelyn_syrup", "Benelyn Syrup", 79.99, R.drawable.benelyinsyrup)
        }

        // 6. Berroca (R120.00)
        btnAddCart6.setOnClickListener {
            addToCart("berroca", "Berroca", 120.00, R.drawable.berocca)
        }

        // 7. Calpol Syrup (R67.99)
        btnAddCart7.setOnClickListener {
            addToCart("calpol_syrup", "Calpol Syrup", 67.99, R.drawable.calpol)
        }

        // 8. Canvex V (R89.00)
        btnAddCart8.setOnClickListener {
            addToCart("canvex_v", "Canvex V", 89.00, R.drawable.canvexv)
        }

        // 9. Compral Tablets (R83.99)
        btnAddCart9.setOnClickListener {
            addToCart("compral_tablets", "Compral Tablets", 83.99, R.drawable.compral)
        }

        // 10. Coryx (R72.99)
        btnAddCart10.setOnClickListener {
            addToCart("coryx", "Coryx", 72.99, R.drawable.coryx)
        }

        // 11. Deep Heat Ointment (R74.99)
        btnAddCart11.setOnClickListener {
            addToCart("deep_heat", "Deep Heat Ointment", 74.99, R.drawable.deepheat)
        }

        // 12. Linctagon Tablets (R178.00)
        btnAddCart12.setOnClickListener {
            addToCart("linctagon_tablets", "Linctagon Tablets", 178.00, R.drawable.linctagontablets)
        }

        // 13. Med-Lemon Sachets (R89.99)
        btnAddCart13.setOnClickListener {
            addToCart("med_lemon", "Med-Lemon Sachets", 89.99, R.drawable.medlemon)
        }

        // 14. Panado Effervescent (R62.99)
        btnAddCart14.setOnClickListener {
            addToCart("panado_effervescent", "Panado Effervescent", 62.99, R.drawable.panadoeffervescent)
        }

        // 15. Panado Tablets (R37.99)
        btnAddCart15.setOnClickListener {
            addToCart("panado_tablets", "Panado Tablets", 37.99, R.drawable.panadotablets)
        }

        // 16. Rennie (R143.20)
        btnAddCart16.setOnClickListener {
            addToCart("rennie", "Rennie", 143.20, R.drawable.rennie)
        }

        // 17. Sinutab Nasal Spray (R74.99)
        btnAddCart17.setOnClickListener {
            addToCart("sinutab_nasal_spray", "Sinutab Nasal Spray", 74.99, R.drawable.sinutabnasalspray)
        }

        // 18. Slow-Mag Capsules (R83.99)
        btnAddCart18.setOnClickListener {
            addToCart("slow_mag_capsules", "Slow-Mag Capsules", 83.99, R.drawable.slowmag)
        }

        // 19. Vicks Cough Syrup (R119.99)
        btnAddCart19.setOnClickListener {
            addToCart("vicks_cough_syrup", "Vicks Cough Syrup", 119.99, R.drawable.vickssyrup)
        }
    }

    private fun addToCart(id: String, name: String, price: Double, imageResId: Int) {
        val item = CartItem(
            id = id,
            name = name,
            price = price,
            imageResId = imageResId
        )
        CartRepository.addItem(item)
        Toast.makeText(this, "$name added to cart", Toast.LENGTH_SHORT).show()
    }
}