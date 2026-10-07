package com.example.pillpoint

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView

class Reminder : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var btnMenu: ImageButton
    private lateinit var navView: NavigationView

    private lateinit var etMedicationName: EditText
    private lateinit var etDosage: EditText
    private lateinit var etReminderTime: EditText
    private lateinit var etReminderDate: EditText
    private lateinit var spinnerRepeatFrequency: Spinner
    private lateinit var spinnerNotificationType: Spinner
    private lateinit var etNotes: EditText
    private lateinit var btnSaveReminder: Button
    private lateinit var tvViewScheduled: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reminder)

        // Initialize Layout & Navigation Drawer
        drawerLayout = findViewById(R.id.drawerLayout)
        btnMenu = findViewById(R.id.btnMenu)
        navView = findViewById(R.id.navView)

        // Initialize Form Inputs
        etMedicationName = findViewById(R.id.etMedicationName)
        etDosage = findViewById(R.id.etDosage)
        etReminderTime = findViewById(R.id.etReminderTime)
        etReminderDate = findViewById(R.id.etReminderDate)
        spinnerRepeatFrequency = findViewById(R.id.spinnerRepeatFrequency)
        spinnerNotificationType = findViewById(R.id.spinnerNotificationType)
        etNotes = findViewById(R.id.etNotes)
        btnSaveReminder = findViewById(R.id.btnSaveReminder)
        tvViewScheduled = findViewById(R.id.tvViewScheduled)

        // Setup Spinners
        val frequencyOptions = arrayOf("Once Daily", "Twice Daily", "Weekly", "As Needed")
        val freqAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, frequencyOptions)
        spinnerRepeatFrequency.adapter = freqAdapter

        val notificationOptions = arrayOf("Push Notification", "Alarm", "SMS Alert")
        val notifAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, notificationOptions)
        spinnerNotificationType.adapter = notifAdapter

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
                    // Already here
                }
                R.id.nav_store -> {
                    startActivity(Intent(this, Store_Locator::class.java))
                }
                R.id.nav_track -> {
                    startActivity(Intent(this, TrackOrder::class.java))
                }
                R.id.nav_logout -> {
                    SessionManager.logout(this)
                    val intent = Intent(this, login::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                    finish()
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        // Handle Save Reminder Action
        btnSaveReminder.setOnClickListener {
            val medName = etMedicationName.text.toString().trim()
            val dosage = etDosage.text.toString().trim()
            val time = etReminderTime.text.toString().trim()
            val date = etReminderDate.text.toString().trim()

            if (medName.isEmpty() || dosage.isEmpty() || time.isEmpty() || date.isEmpty()) {
                Toast.makeText(this, "Please fill in all required fields marked with *", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Toast.makeText(this, "Medication reminder saved successfully!", Toast.LENGTH_SHORT).show()
        }

        // Handle View Scheduled Reminders link click
        tvViewScheduled.setOnClickListener {
            Toast.makeText(this, "Opening scheduled reminders...", Toast.LENGTH_SHORT).show()
        }
    }
}