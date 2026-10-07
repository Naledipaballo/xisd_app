package com.example.pillpoint.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.pillpoint.database.AppDao
import com.example.pillpoint.database.AppDatabase
import com.example.pillpoint.databinding.ActivitySettingsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private var loggedInUserEmail: String = String()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val appDao = AppDatabase.getDatabase(applicationContext).appDao()

        loggedInUserEmail = intent.getStringExtra("USER_EMAIL").orEmpty()

        if (loggedInUserEmail.isNotEmpty()) {
            loadUserProfile(appDao)
        }

        binding.btnSaveProfile.setOnClickListener {
            saveUserProfile(appDao)
        }

        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            val status = if (isChecked) "enabled" else "disabled"
            Toast.makeText(this, "Medication reminders $status", Toast.LENGTH_SHORT).show()
        }

        binding.btnHelpSupport.setOnClickListener {
            Toast.makeText(
                this,
                "PillPoint Support: support@pillpoint.co.za | Hotline: 0800 745 576",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun loadUserProfile(appDao: AppDao) {
        lifecycleScope.launch(Dispatchers.IO) {
            val user = appDao.getUserByEmail(loggedInUserEmail)
            withContext(Dispatchers.Main) {
                user?.let {
                    binding.etFullName.setText(it.fullName)
                    binding.etPhone.setText(it.phone)
                    binding.etAddress.setText(it.deliveryAddress)
                }
            }
        }
    }

    private fun saveUserProfile(appDao: AppDao) {
        val name = binding.etFullName.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val address = binding.etAddress.text.toString().trim()

        var isValid = true

        if (name.isEmpty()) {
            binding.tilFullName.error = "Please enter your full name"
            isValid = false
        } else {
            binding.tilFullName.error = null
        }

        if (phone.isEmpty()) {
            binding.tilPhone.error = "Please enter your phone number"
            isValid = false
        } else {
            binding.tilPhone.error = null
        }

        if (address.isEmpty()) {
            binding.tilAddress.error = "Please enter your delivery address"
            isValid = false
        } else {
            binding.tilAddress.error = null
        }

        if (!isValid) return

        lifecycleScope.launch(Dispatchers.IO) {
            appDao.updateUserProfileFields(
                email = loggedInUserEmail,
                name = name,
                phone = phone,
                address = address
            )
            withContext(Dispatchers.Main) {
                Toast.makeText(this@SettingsActivity, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}