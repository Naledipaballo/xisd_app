package com.example.pillpoint

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

class Reminder : AppCompatActivity() {

    // Firebase services
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // Navigation drawer views
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var btnMenu: ImageButton
    private lateinit var ivProfile: View

    // Reminder form views
    private lateinit var etMedicationName: EditText
    private lateinit var etDosage: EditText
    private lateinit var etReminderTime: EditText
    private lateinit var etReminderDate: EditText
    private lateinit var spinnerRepeatFrequency: Spinner
    private lateinit var spinnerNotificationType: Spinner
    private lateinit var etNotes: EditText
    private lateinit var btnSaveReminder: Button
    private lateinit var tvViewScheduled: TextView

    // Saved reminder views
    private lateinit var reminderScrollView: ScrollView
    private lateinit var tvNoReminders: TextView
    private lateinit var remindersContainer: LinearLayout

    // Request permission to show notifications on Android 13+.
    private val notificationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            val message = if (granted) {
                "Notifications are enabled."
            } else {
                "Allow notifications in Android Settings to receive reminders."
            }

            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reminder)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // A signed-in user is required.
        if (auth.currentUser == null) {
            startActivity(Intent(this, Login::class.java))
            finish()
            return
        }

        initializeViews()
        setupSpinners()
        setupDrawerHeader()
        setupNavigationDrawer()

        // Create the Android notification channel.
        ReminderReceiver.createNotificationChannel(this)
        requestNotificationPermission()

        btnSaveReminder.setOnClickListener {
            saveReminder()
        }

        tvViewScheduled.setOnClickListener {
            reminderScrollView.post {
                reminderScrollView.smoothScrollTo(
                    0,
                    remindersContainer.top
                )
            }
        }

        loadReminders()
    }

    private fun initializeViews() {
        drawerLayout = findViewById(R.id.drawerLayout)
        navView = findViewById(R.id.navView)
        btnMenu = findViewById(R.id.btnMenu)
        ivProfile = findViewById(R.id.ivProfile)

        etMedicationName = findViewById(R.id.etMedicationName)
        etDosage = findViewById(R.id.etDosage)
        etReminderTime = findViewById(R.id.etReminderTime)
        etReminderDate = findViewById(R.id.etReminderDate)

        spinnerRepeatFrequency =
            findViewById(R.id.spinnerRepeatFrequency)

        spinnerNotificationType =
            findViewById(R.id.spinnerNotificationType)

        etNotes = findViewById(R.id.etNotes)
        btnSaveReminder = findViewById(R.id.btnSaveReminder)
        tvViewScheduled = findViewById(R.id.tvViewScheduled)

        reminderScrollView = findViewById(R.id.reminderScrollView)
        tvNoReminders = findViewById(R.id.tvNoReminders)
        remindersContainer = findViewById(R.id.remindersContainer)
    }

    private fun setupSpinners() {
        val repeatOptions = listOf(
            "Once Daily",
            "Twice Daily",
            "Weekly",
            "As Needed"
        )

        val notificationOptions = listOf(
            "Push Notification",
            "Alarm",
            "SMS Alert"
        )

        val repeatAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            repeatOptions
        ).apply {
            setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
            )
        }

        spinnerRepeatFrequency.adapter = repeatAdapter

        val notificationAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            notificationOptions
        ).apply {
            setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
            )
        }

        spinnerNotificationType.adapter = notificationAdapter
    }

    private fun requestNotificationPermission() {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(
                Manifest.permission.POST_NOTIFICATIONS
            )
        }
    }

    // Parse the selected date and time.
    // Expected format: dd/MM/yyyy and HH:mm.
    private fun getTriggerTime(): Long? {
        val date = etReminderDate.text.toString().trim()
        val time = etReminderTime.text.toString().trim()

        if (date.isBlank() || time.isBlank()) return null

        return try {
            val formatter = SimpleDateFormat(
                "dd/MM/yyyy HH:mm",
                Locale.getDefault()
            ).apply {
                isLenient = false
            }

            val parsedDate = formatter.parse("$date $time")
                ?: return null

            if (parsedDate.time <= System.currentTimeMillis()) {
                null
            } else {
                parsedDate.time
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun saveReminder() {
        val user = auth.currentUser ?: run {
            Toast.makeText(
                this,
                "Please log in again.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val medicationName =
            etMedicationName.text.toString().trim()

        val dosage =
            etDosage.text.toString().trim()

        val reminderDate =
            etReminderDate.text.toString().trim()

        val reminderTime =
            etReminderTime.text.toString().trim()

        val repeatFrequency =
            spinnerRepeatFrequency.selectedItem.toString()

        val notificationType =
            spinnerNotificationType.selectedItem.toString()

        val notes =
            etNotes.text.toString().trim()

        if (
            medicationName.isBlank() ||
            dosage.isBlank() ||
            reminderDate.isBlank() ||
            reminderTime.isBlank()
        ) {
            Toast.makeText(
                this,
                "Enter the medication, dosage, date and time.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val triggerTime = getTriggerTime()

        if (triggerTime == null) {
            Toast.makeText(
                this,
                "Enter a valid future date (dd/MM/yyyy) and time (HH:mm).",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        btnSaveReminder.isEnabled = false

        val reminderData = hashMapOf<String, Any>(
            "userId" to user.uid,
            "medicationName" to medicationName,
            "dosage" to dosage,
            "reminderDate" to reminderDate,
            "reminderTime" to reminderTime,
            "repeatFrequency" to repeatFrequency,
            "notificationType" to notificationType,
            "notes" to notes,
            "enabled" to true,
            "createdAt" to FieldValue.serverTimestamp()
        )

        db.collection("reminders")
            .add(reminderData)
            .addOnSuccessListener { documentReference ->

                // Schedule the local Android alarm after Firestore saves.
                scheduleMedicationReminder(
                    reminderId = documentReference.id,
                    medicationName = medicationName,
                    dosage = dosage,
                    triggerTime = triggerTime,
                    repeatFrequency = repeatFrequency
                )

                Toast.makeText(
                    this,
                    "Medication reminder saved.",
                    Toast.LENGTH_LONG
                ).show()

                clearForm()
                loadReminders()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(
                    this,
                    "Could not save reminder: " +
                            (exception.localizedMessage ?: "Unknown error"),
                    Toast.LENGTH_LONG
                ).show()
            }
            .addOnCompleteListener {
                btnSaveReminder.isEnabled = true
            }
    }

    // Schedule a one-time or repeating notification.
    private fun scheduleMedicationReminder(
        reminderId: String,
        medicationName: String,
        dosage: String,
        triggerTime: Long,
        repeatFrequency: String
    ) {
        try {
            val alarmManager =
                getSystemService(Context.ALARM_SERVICE) as AlarmManager

            val requestCode =
                reminderId.hashCode() and 0x7fffffff

            val intent = Intent(
                this,
                ReminderReceiver::class.java
            ).apply {
                putExtra(
                    ReminderReceiver.EXTRA_MEDICATION_NAME,
                    medicationName
                )
                putExtra(
                    ReminderReceiver.EXTRA_DOSAGE,
                    dosage
                )
                putExtra(
                    ReminderReceiver.EXTRA_REMINDER_ID,
                    requestCode
                )
            }

            val pendingIntent = PendingIntent.getBroadcast(
                this,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )

            when (repeatFrequency) {
                "Once Daily" -> {
                    alarmManager.setInexactRepeating(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        AlarmManager.INTERVAL_DAY,
                        pendingIntent
                    )
                }

                "Twice Daily" -> {
                    alarmManager.setInexactRepeating(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        AlarmManager.INTERVAL_DAY / 2,
                        pendingIntent
                    )
                }

                "Weekly" -> {
                    alarmManager.setInexactRepeating(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        AlarmManager.INTERVAL_DAY * 7,
                        pendingIntent
                    )
                }

                else -> {
                    // "As Needed" is scheduled as a one-time reminder.
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
            }

        } catch (exception: Exception) {
            Toast.makeText(
                this,
                "Reminder saved, but scheduling failed: " +
                        (exception.localizedMessage ?: "Unknown error"),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun clearForm() {
        etMedicationName.text.clear()
        etDosage.text.clear()
        etReminderDate.text.clear()
        etReminderTime.text.clear()
        etNotes.text.clear()

        spinnerRepeatFrequency.setSelection(0)
        spinnerNotificationType.setSelection(0)
    }

    // Load only the signed-in user's reminders.
    private fun loadReminders() {
        val user = auth.currentUser ?: return

        db.collection("reminders")
            .whereEqualTo("userId", user.uid)
            .get()
            .addOnSuccessListener { snapshot ->

                remindersContainer.removeAllViews()

                val documents = snapshot.documents.sortedByDescending {
                    it.getTimestamp("createdAt")?.toDate()?.time ?: 0L
                }

                if (documents.isEmpty()) {
                    tvNoReminders.visibility = View.VISIBLE
                    tvNoReminders.text =
                        "You have no saved medication reminders."
                    return@addOnSuccessListener
                }

                tvNoReminders.visibility = View.GONE

                documents.forEach { document ->
                    addReminderCard(
                        documentId = document.id,
                        medicationName =
                            document.getString("medicationName")
                                ?: "Medication",
                        dosage =
                            document.getString("dosage")
                                ?: "Not specified",
                        date =
                            document.getString("reminderDate")
                                ?: "Not specified",
                        time =
                            document.getString("reminderTime")
                                ?: "Not specified",
                        frequency =
                            document.getString("repeatFrequency")
                                ?: "Not specified",
                        notificationType =
                            document.getString("notificationType")
                                ?: "Not specified",
                        notes =
                            document.getString("notes").orEmpty()
                    )
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(
                    this,
                    "Could not load reminders: " +
                            (exception.localizedMessage ?: "Unknown error"),
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun addReminderCard(
        documentId: String,
        medicationName: String,
        dosage: String,
        date: String,
        time: String,
        frequency: String,
        notificationType: String,
        notes: String
    ) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            setBackgroundResource(R.drawable.edit_text_background)
        }

        fun addText(
            value: String,
            size: Float = 15f,
            bold: Boolean = false
        ) {
            val textView = TextView(this).apply {
                text = value
                textSize = size

                setTextColor(
                    ContextCompat.getColor(
                        this@Reminder,
                        R.color.dark_blue
                    )
                )

                if (bold) {
                    setTypeface(
                        null,
                        android.graphics.Typeface.BOLD
                    )
                }

                setPadding(0, dp(3), 0, dp(3))
            }

            card.addView(textView)
        }

        addText(medicationName, 18f, true)
        addText("Dosage: $dosage")
        addText("Date: $date")
        addText("Time: $time")
        addText("Frequency: $frequency")
        addText("Notification: $notificationType")

        if (notes.isNotBlank()) {
            addText("Notes: $notes")
        }

        val deleteButton = Button(this).apply {
            text = "Delete Reminder"
            setOnClickListener {
                deleteReminder(documentId)
            }
        }

        card.addView(deleteButton)

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(0, dp(8), 0, dp(12))
        }

        remindersContainer.addView(card, params)
    }

    // Delete the Firestore record and cancel its scheduled alarm.
    private fun deleteReminder(documentId: String) {
        val user = auth.currentUser ?: return

        db.collection("reminders")
            .document(documentId)
            .get()
            .addOnSuccessListener { document ->

                if (!document.exists()) {
                    Toast.makeText(
                        this,
                        "Reminder not found.",
                        Toast.LENGTH_SHORT
                    ).show()
                    loadReminders()
                    return@addOnSuccessListener
                }

                if (document.getString("userId") != user.uid) {
                    Toast.makeText(
                        this,
                        "You cannot delete this reminder.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@addOnSuccessListener
                }

                cancelMedicationReminder(documentId)

                db.collection("reminders")
                    .document(documentId)
                    .delete()
                    .addOnSuccessListener {
                        Toast.makeText(
                            this,
                            "Reminder deleted.",
                            Toast.LENGTH_SHORT
                        ).show()
                        loadReminders()
                    }
                    .addOnFailureListener { exception ->
                        Toast.makeText(
                            this,
                            "Could not delete reminder: " +
                                    (exception.localizedMessage ?: "Unknown error"),
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(
                    this,
                    "Could not check reminder: " +
                            (exception.localizedMessage ?: "Unknown error"),
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun cancelMedicationReminder(reminderId: String) {
        val requestCode =
            reminderId.hashCode() and 0x7fffffff

        val intent = Intent(
            this,
            ReminderReceiver::class.java
        )

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager =
            getSystemService(Context.ALARM_SERVICE) as AlarmManager

        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun setupDrawerHeader() {
        val headerView = navView.getHeaderView(0)

        val txtEmail =
            headerView.findViewById<TextView>(R.id.txtEmail)

        val txtUserName =
            headerView.findViewById<TextView>(R.id.txtUserName)

        val email = auth.currentUser?.email ?: "Not signed in"

        txtEmail?.text = email
        txtUserName?.text =
            email.substringBefore("@").ifBlank { "Pill Point User" }

        ivProfile.setOnClickListener {
            startActivity(Intent(this, MyAccount::class.java))
        }
    }

    private fun setupNavigationDrawer() {
        btnMenu.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        navView.setNavigationItemSelectedListener { item ->
            drawerLayout.closeDrawer(GravityCompat.START)

            when (item.itemId) {
                R.id.nav_dashboard -> {
                    startActivity(Intent(this, Home::class.java))
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
                    // Already on this screen.
                }

                R.id.nav_store -> {
                    startActivity(Intent(this, Store_Locator::class.java))
                }

                R.id.nav_track -> {
                    startActivity(Intent(this, TrackOrder::class.java))
                }

                R.id.nav_my_orders -> {
                    startActivity(Intent(this, MyOrders::class.java))
                }

                R.id.nav_logout -> {
                    logoutUser()
                }
            }

            true
        }
    }

    private fun logoutUser() {
        auth.signOut()
        SessionManager.logout(this)

        val intent = Intent(this, Login::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        startActivity(intent)
        finish()
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}