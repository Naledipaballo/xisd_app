package com.example.pillpoint

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth

class Home : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
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
        // Force the app to stay in light mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)

        if (!SessionManager.isLoggedIn(this)) {
            redirectToLogin()
            return
        }

        setContentView(R.layout.activity_home)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

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

        // Setup profile icon click listener to open My Account
        ivProfile.setOnClickListener {
            val intent = Intent(this, MyAccount::class.java)
            startActivity(intent)
        }

        // Grab current user email from Firebase or SessionManager
        val currentUser = auth.currentUser
        val userEmail = currentUser?.email ?: SessionManager.email(this).ifEmpty { intent.getStringExtra("USER_EMAIL").orEmpty() }

        if (userEmail.isNotEmpty()) {
            tvWelcomeUser.text = "Welcome back, $userEmail!"
        }

        // Populate Navigation Drawer Header with User Info
        setupDrawerHeader(userEmail)

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
                    logoutUser()
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

    private fun setupDrawerHeader(userEmail: String) {
        val headerView = navView.getHeaderView(0)
        val txtEmail = headerView.findViewById<TextView>(R.id.txtEmail)
        val txtUserName = headerView.findViewById<TextView>(R.id.txtUserName)

        if (userEmail.isNotEmpty()) {
            txtEmail.text = userEmail
        } else {
            txtEmail.text = "user@email.com"
        }

        val currentUser = auth.currentUser
        val displayName = currentUser?.displayName
        if (!displayName.isNullOrBlank()) {
            val firstName = displayName.split(" ").firstOrNull() ?: ""
            txtUserName.text = "Welcome, $firstName"
        } else {
            txtUserName.text = "Welcome"
        }
    }

    private fun logoutUser() {
        SessionManager.logout(this)
        auth.signOut()
        redirectToLogin()
    }

    private fun redirectToLogin() {
        val intent = Intent(this, Login::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}