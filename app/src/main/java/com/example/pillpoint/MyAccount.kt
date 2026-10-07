package com.example.pillpoint

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

class MyAccount : AppCompatActivity() {

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
                    startActivity(Intent(this, MyAccount::class.java))
                    finish()
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
            val firstName = etFirstName.text.toString().trim()
            val lastName = etLastName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val phone = etPhone.text.toString().trim()

            if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || phone.isEmpty()) {
                Toast.makeText(this, "Please fill in all primary account fields.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Toast.makeText(this, "Account details updated successfully!", Toast.LENGTH_SHORT).show()
        }

        // Handle Logout Button Action
        btnAccountLogout.setOnClickListener {
            logoutUser()
        }
    }

    private fun logoutUser() {
        SessionManager.logout(this)
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, login::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }
}