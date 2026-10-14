package com.example.pillpoint

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest

class MyAccount : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var btnMenu: ImageButton
    private lateinit var navView: NavigationView

    private lateinit var etFirstName: EditText
    private lateinit var etLastName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPhone: EditText
    private lateinit var etAddress: EditText
    private lateinit var etPassword: EditText
    private lateinit var switchNotifications: SwitchMaterial
    private lateinit var btnHelpSupport: Button
    private lateinit var btnSaveAccount: Button
    private lateinit var btnAccountLogout: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_account)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Initialize Layout & Navigation Drawer
        drawerLayout = findViewById(R.id.drawerLayout)
        btnMenu = findViewById(R.id.btnMenu)
        navView = findViewById(R.id.navView)

        // Initialize Form Inputs & Preferences
        etFirstName = findViewById(R.id.etFirstName)
        etLastName = findViewById(R.id.etLastName)
        etEmail = findViewById(R.id.etEmail)
        etPhone = findViewById(R.id.etPhone)
        etAddress = findViewById(R.id.etAddress)
        etPassword = findViewById(R.id.etPassword)
        switchNotifications = findViewById(R.id.switchNotifications)
        btnHelpSupport = findViewById(R.id.btnHelpSupport)
        btnSaveAccount = findViewById(R.id.btnSaveAccount)
        btnAccountLogout = findViewById(R.id.btnAccountLogout)

        // Load existing user profile & settings data
        loadUserProfile()

        // Setup Navigation Drawer Header
        setupDrawerHeader()

        // Hamburger Menu click opens drawer
        btnMenu.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // Drawer navigation
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_dashboard -> {
                    startActivity(Intent(this, Home::class.java))
                    finish()
                }
                R.id.nav_shop -> startActivity(Intent(this, Shop::class.java))
                R.id.nav_cart -> startActivity(Intent(this, Cart::class.java))
                R.id.nav_account -> { /* Already here */ }
                R.id.nav_prescription -> {
                    startActivity(Intent(this, Prescriptions::class.java))
                    finish()
                }
                R.id.nav_reminders -> {
                    startActivity(Intent(this, Reminder::class.java))
                    finish()
                }
                R.id.nav_store -> startActivity(Intent(this, Store_Locator::class.java))
                R.id.nav_track -> startActivity(Intent(this, TrackOrder::class.java))
                R.id.nav_logout -> logoutUser()
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        // Help & Support Button
        btnHelpSupport.setOnClickListener {
            Toast.makeText(this, "Opening Help & Support...", Toast.LENGTH_SHORT).show()
        }

        // Save Changes Action
        btnSaveAccount.setOnClickListener {
            saveUserProfile()
        }

        // Logout Action
        btnAccountLogout.setOnClickListener {
            logoutUser()
        }
    }

    private fun setupDrawerHeader() {
        val headerView = navView.getHeaderView(0)
        val txtEmail = headerView.findViewById<TextView>(R.id.txtEmail)
        val txtUserName = headerView.findViewById<TextView>(R.id.txtUserName)

        val currentUser = auth.currentUser
        val userEmail = currentUser?.email ?: SessionManager.email(this).ifEmpty { intent.getStringExtra("USER_EMAIL").orEmpty() }

        txtEmail.text = if (userEmail.isNotEmpty()) userEmail else "user@email.com"

        val displayName = currentUser?.displayName
        if (!displayName.isNullOrBlank()) {
            val firstName = displayName.split(" ").firstOrNull() ?: ""
            txtUserName.text = "Welcome, $firstName"
        } else {
            txtUserName.text = "Welcome"
        }
    }

    private fun loadUserProfile() {
        val currentUser = auth.currentUser
        val prefs = getSharedPreferences("UserProfilePrefs", Context.MODE_PRIVATE)

        if (currentUser != null) {
            etEmail.setText(currentUser.email ?: "")

            val fullName = currentUser.displayName ?: ""
            if (fullName.isNotBlank()) {
                val nameParts = fullName.trim().split("\\s+".toRegex())
                if (nameParts.isNotEmpty()) {
                    etFirstName.setText(nameParts[0])
                    if (nameParts.size > 1) {
                        etLastName.setText(nameParts.subList(1, nameParts.size).joinToString(" "))
                    }
                }
            }

            val savedPhone = prefs.getString("user_phone_${currentUser.uid}", currentUser.phoneNumber ?: "")
            val savedAddress = prefs.getString("user_address_${currentUser.uid}", "")
            val savedFirstName = prefs.getString("user_first_name_${currentUser.uid}", "")
            val savedLastName = prefs.getString("user_last_name_${currentUser.uid}", "")
            val enableReminders = prefs.getBoolean("enable_reminders_${currentUser.uid}", true)

            if (!savedFirstName.isNullOrBlank()) etFirstName.setText(savedFirstName)
            if (!savedLastName.isNullOrBlank()) etLastName.setText(savedLastName)
            if (!savedPhone.isNullOrBlank()) etPhone.setText(savedPhone)
            if (!savedAddress.isNullOrBlank()) etAddress.setText(savedAddress)
            switchNotifications.isChecked = enableReminders
        }
    }

    private fun saveUserProfile() {
        val firstName = etFirstName.text.toString().trim()
        val lastName = etLastName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val phone = etPhone.text.toString().trim()
        val address = etAddress.text.toString().trim()
        val newPassword = etPassword.text.toString()
        val enableReminders = switchNotifications.isChecked

        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || phone.isEmpty() || address.isEmpty()) {
            Toast.makeText(this, "Please fill in all primary account fields.", Toast.LENGTH_SHORT).show()
            return
        }

        val currentUser = auth.currentUser
        if (currentUser != null) {
            val fullName = "$firstName $lastName"
            val profileUpdates = userProfileChangeRequest {
                displayName = fullName
            }

            // Save details & preference switch to SharedPreferences linked to user UID
            val prefs = getSharedPreferences("UserProfilePrefs", Context.MODE_PRIVATE)
            prefs.edit().apply {
                putString("user_first_name_${currentUser.uid}", firstName)
                putString("user_last_name_${currentUser.uid}", lastName)
                putString("user_email_${currentUser.uid}", email)
                putString("user_phone_${currentUser.uid}", phone)
                putString("user_address_${currentUser.uid}", address)
                putBoolean("enable_reminders_${currentUser.uid}", enableReminders)
                apply()
            }

            currentUser.updateProfile(profileUpdates)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        setupDrawerHeader()

                        if (newPassword.isNotBlank()) {
                            currentUser.updatePassword(newPassword)
                                .addOnCompleteListener { passTask ->
                                    if (passTask.isSuccessful) {
                                        Toast.makeText(this, "Account details, preferences, and password updated!", Toast.LENGTH_SHORT).show()
                                        etPassword.text.clear()
                                    } else {
                                        Toast.makeText(this, "Failed to update password: ${passTask.exception?.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                        } else {
                            Toast.makeText(this, "Account details & settings saved successfully!", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this, "Failed to update profile: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }
    }

    private fun logoutUser() {
        SessionManager.logout(this)
        auth.signOut()
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, Login::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }
}