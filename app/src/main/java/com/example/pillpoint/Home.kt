package com.example.pillpoint

import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

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
    private lateinit var btnMyOrders: Button

    private lateinit var tvWelcomeUser: TextView
    private lateinit var ivProfile: ImageView

    private val db: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(
            AppCompatDelegate.MODE_NIGHT_NO
        )
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()

        // Check whether the user is signed in to Firebase.
        val currentUser = auth.currentUser

        if (currentUser == null) {
            // Do not allow access to the dashboard without Firebase login.
            redirectToLogin()
            return
        }

        setContentView(R.layout.activity_home)

        // Initialize the dashboard layout.
        drawerLayout = findViewById(R.id.drawerLayout)
        btnMenu = findViewById(R.id.btnMenu)
        navView = findViewById(R.id.navView)
        ivProfile = findViewById(R.id.ivProfile)

        // Initialize dashboard buttons.
        btnShop = findViewById(R.id.btnShopMedication)
        btnCart = findViewById(R.id.btnViewCart)
        btnPrescription = findViewById(R.id.btnUploadPrescription)
        btnReminders = findViewById(R.id.btnReminders)
        btnTrack = findViewById(R.id.btnTrackOrder)
        btnStore = findViewById(R.id.btnStoreLocator)
        btnMyOrders = findViewById(R.id.btnMyOrders)

        tvWelcomeUser = findViewById(R.id.tvWelcomeUser)

        // Display the signed-in user's email address.
        val userEmail = currentUser.email.orEmpty()

        tvWelcomeUser.text = if (userEmail.isNotEmpty()) {
            "Welcome back, $userEmail!"
        } else {
            "Welcome back to Pill Point Delivery!"
        }

        setupDrawerHeader(userEmail)
        setupNavigationDrawer()
        setupDashboardButtons()

    }

    /**
     * Refresh dashboard statistics whenever the dashboard becomes visible.
     */
    override fun onResume() {
        super.onResume()

        if (::auth.isInitialized && auth.currentUser != null) {
            loadDashboardStats()
        }
    }

    /**
     * Configure the profile details shown in the navigation drawer.
     */
    private fun setupDrawerHeader(userEmail: String) {
        if (navView.headerCount > 0) {
            val headerView = navView.getHeaderView(0)

            val txtEmail =
                headerView.findViewById<TextView>(R.id.txtEmail)

            val txtUserName =
                headerView.findViewById<TextView>(R.id.txtUserName)

            txtEmail?.text = userEmail.ifEmpty {
                "user@email.com"
            }

            val displayName = auth.currentUser?.displayName

            txtUserName?.text = if (!displayName.isNullOrBlank()) {
                "Welcome, ${displayName.substringBefore(" ")}"
            } else {
                "Welcome"
            }
        }
    }

    /**
     * Set up the hamburger menu and its navigation actions.
     */
    private fun setupNavigationDrawer() {
        btnMenu.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {

                R.id.nav_dashboard -> {
                    // The user is already on the dashboard.
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
                    startActivity(
                        Intent(this, Prescriptions::class.java)
                    )
                }

                R.id.nav_reminders -> {
                    startActivity(Intent(this, Reminder::class.java))
                }

                R.id.nav_store -> {
                    startActivity(
                        Intent(this, Store_Locator::class.java)
                    )
                }

                R.id.nav_track -> {
                    startActivity(
                        Intent(this, TrackOrder::class.java)
                    )
                }

                R.id.nav_my_orders -> {
                    openMyOrders()
                }

                R.id.nav_logout -> {
                    logoutUser()
                }
            }

            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    /**
     * Connect the dashboard buttons to their respective screens.
     */
    private fun setupDashboardButtons() {

        // Open the medication shop.
        btnShop.setOnClickListener {
            startActivity(Intent(this, Shop::class.java))
        }

        // Open the shopping cart.
        btnCart.setOnClickListener {
            startActivity(Intent(this, Cart::class.java))
        }

        // Open the prescription upload screen.
        btnPrescription.setOnClickListener {
            startActivity(Intent(this, Prescriptions::class.java))
        }

        // Open medication reminders.
        btnReminders.setOnClickListener {
            startActivity(Intent(this, Reminder::class.java))
        }

        // Open order tracking.
        btnTrack.setOnClickListener {
            startActivity(Intent(this, TrackOrder::class.java))
        }

        // Open the pharmacy store locator.
        btnStore.setOnClickListener {
            startActivity(Intent(this, Store_Locator::class.java))
        }

        // Open the user's orders.
        btnMyOrders.setOnClickListener {
            openMyOrders()
        }

        // Open the user's account.
        ivProfile.setOnClickListener {
            startActivity(Intent(this, MyAccount::class.java))
        }
    }

    /**
     * Open the signed-in user's orders.
     */
    private fun openMyOrders() {
        startActivity(Intent(this, MyOrders::class.java))
    }

    /**
     * Read the user's statistics from Cloud Firestore.
     *
     * Expected collections:
     * orders: userId, total
     * cart: userId, quantity
     * prescriptions: userId
     * reminders: userId
     */
    private fun loadDashboardStats() {
        val user = auth.currentUser ?: return
        val uid = user.uid

        // Reset values while data is loading.
        findViewById<TextView>(R.id.tvCartCount).text = "..."
        findViewById<TextView>(R.id.tvPrescriptionCount).text = "..."
        findViewById<TextView>(R.id.tvReminderCount).text = "..."
        findViewById<TextView>(R.id.tvTotalValue).text = "..."

        // 1. Cart: calculate the total number of medication units.
        db.collection("cart")
            .whereEqualTo("userId", uid)
            .get()
            .addOnSuccessListener { result ->

                var cartQuantity = 0L

                for (document in result.documents) {
                    val quantity = document.getLong("quantity") ?: 1L
                    cartQuantity += quantity
                }

                findViewById<TextView>(R.id.tvCartCount).text =
                    cartQuantity.toString()
            }
            .addOnFailureListener { error ->
                Log.e("Home", "Failed to load cart statistics", error)
                findViewById<TextView>(R.id.tvCartCount).text = "—"
            }

        // 2. Prescriptions: count this user's prescription documents.
        db.collection("prescriptions")
            .whereEqualTo("userId", uid)
            .get()
            .addOnSuccessListener { result ->

                findViewById<TextView>(R.id.tvPrescriptionCount).text =
                    result.size().toString()
            }
            .addOnFailureListener { error ->
                Log.e("Home", "Failed to load prescription statistics", error)
                findViewById<TextView>(R.id.tvPrescriptionCount).text = "—"
            }

        // 3. Reminders: count this user's medication reminder documents.
        db.collection("reminders")
            .whereEqualTo("userId", uid)
            .get()
            .addOnSuccessListener { result ->

                findViewById<TextView>(R.id.tvReminderCount).text =
                    result.size().toString()
            }
            .addOnFailureListener { error ->
                Log.e("Home", "Failed to load reminder statistics", error)
                findViewById<TextView>(R.id.tvReminderCount).text = "—"
            }

        // 4. Total: add the totals from this user's orders.
        db.collection("orders")
            .whereEqualTo("userId", uid)
            .get()
            .addOnSuccessListener { result ->

                var totalValue = 0.0

                for (document in result.documents) {
                    val orderTotal =
                        (document.get("total") as? Number)?.toDouble()
                            ?: 0.0

                    totalValue += orderTotal
                }

                findViewById<TextView>(R.id.tvTotalValue).text =
                    String.format(
                        Locale("en", "ZA"),
                        "R%.2f",
                        totalValue
                    )
            }
            .addOnFailureListener { error ->
                Log.e("Home", "Failed to load order totals", error)
                findViewById<TextView>(R.id.tvTotalValue).text = "—"
            }
    }

    /**
     * Sign out and return the user to the login screen.
     */
    private fun logoutUser() {
        auth.signOut()

        // Clear any legacy local session, if your project has this class.
        SessionManager.logout(this)

        redirectToLogin()
    }

    /**
     * Return to Login and clear the previous activity stack.
     */
    private fun redirectToLogin() {
        val intent = Intent(this, Login::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        startActivity(intent)
        finish()
    }
}