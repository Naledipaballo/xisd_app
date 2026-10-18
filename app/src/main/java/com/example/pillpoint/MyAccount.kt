package com.example.pillpoint

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
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

        // Initialize Form Inputs
        etFirstName = findViewById(R.id.etFirstName)
        etLastName = findViewById(R.id.etLastName)
        etEmail = findViewById(R.id.etEmail)
        etPhone = findViewById(R.id.etPhone)
        etAddress = findViewById(R.id.etAddress)
        etPassword = findViewById(R.id.etPassword)
        btnSaveAccount = findViewById(R.id.btnSaveAccount)
        btnAccountLogout = findViewById(R.id.btnAccountLogout)

        // Load existing user profile data
        loadUserProfile()

        // Setup Hamburger Menu Button
        btnMenu.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // Setup Drawer Menu item navigation
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_dashboard -> {
                    startActivity(Intent(this, Home::class.java))
                    finish()
                }
                R.id.nav_shop -> {
                    startActivity(Intent(this, Shop::class.java))
                }
                R.id.nav_cart -> {
                    startActivity(Intent(this, Cart::class.java))
                }
                R.id.nav_account -> {
                    // Already on account screen
                }
                R.id.nav_prescription -> {
                    startActivity(Intent(this, Prescriptions::class.java))
                    finish()
                }
                R.id.nav_reminders -> {
                    startActivity(Intent(this, Reminder::class.java))
                    finish()
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

        // Handle Save Changes Action
        btnSaveAccount.setOnClickListener {
            saveUserProfile()
        }

        // Handle Logout Button Action
        btnAccountLogout.setOnClickListener {
            logoutUser()
        }
    }

    private fun loadUserProfile() {
        val currentUser = auth.currentUser
        val prefs = getSharedPreferences("UserProfilePrefs", Context.MODE_PRIVATE)

        if (currentUser != null) {
            // Populate Email
            etEmail.setText(currentUser.email ?: "")

            // Parse Display Name (from Google Sign-In or Firebase Auth)
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

            // Load Phone and Address from local storage if previously saved
            val savedPhone = prefs.getString("user_phone_${currentUser.uid}", currentUser.phoneNumber ?: "")
            val savedAddress = prefs.getString("user_address_${currentUser.uid}", "")
            val savedFirstName = prefs.getString("user_first_name_${currentUser.uid}", "")
            val savedLastName = prefs.getString("user_last_name_${currentUser.uid}", "")

            if (!savedFirstName.isNullOrBlank()) etFirstName.setText(savedFirstName)
            if (!savedLastName.isNullOrBlank()) etLastName.setText(savedLastName)
            if (!savedPhone.isNullOrBlank()) etPhone.setText(savedPhone)
            if (!savedAddress.isNullOrBlank()) etAddress.setText(savedAddress)
        }
    }

    private fun saveUserProfile() {
        val firstName = etFirstName.text.toString().trim()
        val lastName = etLastName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val phone = etPhone.text.toString().trim()
        val address = etAddress.text.toString().trim()
        val newPassword = etPassword.text.toString()

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

            // Save details to SharedPreferences linked to user UID
            val prefs = getSharedPreferences("UserProfilePrefs", Context.MODE_PRIVATE)
            prefs.edit().apply {
                putString("user_first_name_${currentUser.uid}", firstName)
                putString("user_last_name_${currentUser.uid}", lastName)
                putString("user_email_${currentUser.uid}", email)
                putString("user_phone_${currentUser.uid}", phone)
                putString("user_address_${currentUser.uid}", address)
                apply()
            }

            // Update Firebase Display Name
            currentUser.updateProfile(profileUpdates)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        if (newPassword.isNotBlank()) {
                            currentUser.updatePassword(newPassword)
                                .addOnCompleteListener { passTask ->
                                    if (passTask.isSuccessful) {
                                        Toast.makeText(this, "Account details and password updated!", Toast.LENGTH_SHORT).show()
                                        etPassword.text.clear()
                                    } else {
                                        Toast.makeText(this, "Failed to update password: ${passTask.exception?.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                        } else {
                            Toast.makeText(this, "Account details saved successfully!", Toast.LENGTH_SHORT).show()
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