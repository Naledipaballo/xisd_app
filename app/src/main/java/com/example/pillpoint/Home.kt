package com.example.pillpoint

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView

class Home : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var btnMenu: ImageButton
    private lateinit var navView: NavigationView

    private lateinit var btnShop: Button
    private lateinit var btnCart: Button
    private lateinit var btnPrescription: Button
    private lateinit var btnReminders: Button
    private lateinit var btnTrack: Button
    private lateinit var btnStore: Button
    private lateinit var tvWelcomeUser: TextView
    private lateinit var ivProfile: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!SessionManager.isLoggedIn(this)) {
            redirectToLogin()
            return
        }

        setContentView(R.layout.activity_home)

        // Initialize UI components
        drawerLayout = findViewById(R.id.drawerLayout)
        btnMenu = findViewById(R.id.btnMenu)
        navView = findViewById(R.id.navView)
        ivProfile = findViewById(R.id.ivProfile)

        btnShop = findViewById(R.id.btnShopMedication)
        btnCart = findViewById(R.id.btnViewCart)
        btnPrescription = findViewById(R.id.btnUploadPrescription)
        btnReminders = findViewById(R.id.btnReminders)
        btnTrack = findViewById(R.id.btnTrackOrder)
        btnStore = findViewById(R.id.btnStoreLocator)
        tvWelcomeUser = findViewById(R.id.tvWelcomeUser)

        //Setup profile icon click listener toopen  my account
        ivProfile.setOnClickListener{
            val intent = Intent(this, MyAccount::class.java)
            startActivity(intent)
        }

        // Grab user email passed from Login/Signup if available
        val userEmail = SessionManager.email(this).ifEmpty { intent.getStringExtra("USER_EMAIL").orEmpty() }
        if (userEmail.isNotEmpty()) {
            tvWelcomeUser.text = "Welcome back, $userEmail!"
        }

        // Hamburger menu click opens the drawer from the left
        btnMenu.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // Handle navigation drawer item clicks
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_dashboard -> {
                    // Already on dashboard
                }
                R.id.nav_shop -> {
                    startActivity(Intent(this, Shop::class.java))
                }
                R.id.nav_cart -> {
                    startActivity(Intent(this, Cart::class.java))
                }
                R.id.nav_account -> {
                    startActivity(Intent(this, MyAccount::class.java))
                }
                R.id.nav_prescription -> {
                    startActivity(Intent(this, Prescriptions::class.java))
                }
                R.id.nav_reminders -> {
                    startActivity(Intent(this, Reminder::class.java))
                }
                R.id.nav_store -> {
                    startActivity(Intent(this, Store_Locator::class.java))
                }
                R.id.nav_track -> {
                    startActivity(Intent(this, TrackOrder::class.java))
                }
                R.id.nav_logout -> {
                    SessionManager.logout(this)
                    val intent = Intent(this, login::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        // Handle direct grid button clicks on the dashboard
        btnShop.setOnClickListener {
            startActivity(Intent(this, Shop::class.java))
        }

        btnCart.setOnClickListener {
            startActivity(Intent(this, Cart::class.java))
        }

        btnPrescription.setOnClickListener {
            startActivity(Intent(this, Prescriptions::class.java))
        }

        btnReminders.setOnClickListener {
            startActivity(Intent(this, Reminder::class.java))
        }

        btnTrack.setOnClickListener {
            startActivity(Intent(this, TrackOrder::class.java))
        }

        btnStore.setOnClickListener {
            startActivity(Intent(this, Store_Locator::class.java))
        }
    }

    private fun redirectToLogin() {
        val intent = Intent(this, login::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }


}