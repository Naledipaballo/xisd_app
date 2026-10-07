package com.example.pillpoint

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import android.text.InputType
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.pillpoint.database.AppDatabase
import com.example.pillpoint.repository.AuthRepository
import kotlinx.coroutines.launch

class Signup : AppCompatActivity() {

    private lateinit var fullNameInput: EditText
    private lateinit var emailInput: EditText
    private lateinit var phoneInput: EditText
    private lateinit var addressInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var confirmPasswordInput: EditText
    private lateinit var togglePassword: ImageButton
    private lateinit var toggleConfirmPassword: ImageButton
    private lateinit var signupBtn: Button
    private lateinit var loginLink: TextView

    private lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_signup)

        // Initialize Room Database and Auth Repository
        val appDao = AppDatabase.getDatabase(applicationContext).appDao()
        authRepository = AuthRepository(appDao)

        // Bind UI elements to match your layout IDs
        fullNameInput = findViewById(R.id.etFullName)
        emailInput = findViewById(R.id.etEmail)
        phoneInput = findViewById(R.id.etPhone)
        addressInput = findViewById(R.id.etAddress)
        passwordInput = findViewById(R.id.etPassword)
        confirmPasswordInput = findViewById(R.id.etConfirmPassword)
        togglePassword = findViewById(R.id.btnTogglePassword)
        toggleConfirmPassword = findViewById(R.id.btnToggleConfirmPassword)

        setupPasswordToggle(passwordInput, togglePassword)
        setupPasswordToggle(confirmPasswordInput, toggleConfirmPassword)
        signupBtn = findViewById(R.id.btnSignup)
        loginLink = findViewById(R.id.tvLoginLink)

        // Handle Sign Up click event
        signupBtn.setOnClickListener {
            handleUserRegistration()
        }

        // Handle navigation back to Login screen
        loginLink.setOnClickListener {
            finish()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun handleUserRegistration() {
        val name = fullNameInput.text.toString().trim()
        val email = emailInput.text.toString().trim()
        val phone = phoneInput.text.toString().trim()
        val address = addressInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()
        val confirmPassword = confirmPasswordInput.text.toString().trim()

        if (name.isEmpty() || email.isEmpty() || phone.isEmpty() || address.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Please fill in all registration fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (password != confirmPassword) {
            confirmPasswordInput.error = "Passwords do not match"
            Toast.makeText(this, "Password and Confirm Password must match", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.length < 6) {
            passwordInput.error = "Password must be at least 6 characters"
            return
        }

        lifecycleScope.launch {
            val result = authRepository.registerUser(
                fullName = name,
                email = email,
                passwordRaw = password,
                phone = phone,
                address = address
            )

            result.onSuccess { user ->
                Toast.makeText(this@Signup, "Account created successfully!", Toast.LENGTH_SHORT).show()
                val intent = Intent(this@Signup, login::class.java).apply {
                    putExtra("USER_EMAIL", user.email)
                }
                startActivity(intent)
                finishAffinity()
            }.onFailure { exception ->
                Toast.makeText(this@Signup, exception.message ?: "Registration failed", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupPasswordToggle(field: EditText, toggle: ImageButton) {
        var visible = false
        toggle.setOnClickListener {
            visible = !visible
            field.inputType = if (visible) {
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            field.setSelection(field.text.length)
            toggle.setImageResource(if (visible) R.drawable.ic_visibility_off else R.drawable.ic_visibility)
            toggle.contentDescription = if (visible) "Hide password" else "Show password"
        }
    }
}