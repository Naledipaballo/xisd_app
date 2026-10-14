package com.example.pillpoint

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class Login : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var loginButton: Button
    private lateinit var togglePassword: ImageButton

    private var passwordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        emailInput = findViewById(R.id.etEmail)
        passwordInput = findViewById(R.id.etPassword)
        loginButton = findViewById(R.id.btnLogin)
        togglePassword = findViewById(R.id.btnTogglePassword)
        val signupLink: TextView = findViewById(R.id.tvSignup)

        // Pre-fill email if coming directly from Signup screen
        val prefilledEmail = intent.getStringExtra("USER_EMAIL")
        if (!prefilledEmail.isNullOrEmpty()) {
            emailInput.setText(prefilledEmail)
        }

        // Toggle password visibility listener
        togglePassword.setOnClickListener {
            passwordVisible = !passwordVisible
            togglePasswordVisibility()
        }

        loginButton.setOnClickListener {
            loginUser()
        }

        signupLink.setOnClickListener {
            startActivity(Intent(this, Signup::class.java))
            finish()
        }
    }

    private fun togglePasswordVisibility() {
        val cursorPosition = passwordInput.selectionStart.coerceAtLeast(0)

        if (passwordVisible) {
            passwordInput.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            togglePassword.setImageResource(R.drawable.ic_visibility_off)
        } else {
            passwordInput.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            togglePassword.setImageResource(R.drawable.ic_visibility)
        }

        // Preserve standard font typeface and cursor position
        passwordInput.typeface = Typeface.DEFAULT
        passwordInput.setSelection(cursorPosition.coerceAtMost(passwordInput.text.length))
    }

    private fun loginUser() {
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()

        if (email.isBlank() || password.isBlank()) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
            return
        }

        loginButton.isEnabled = false

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                loginButton.isEnabled = true
                if (task.isSuccessful) {
                    Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()

                    // Save session state so Home passes the isLoggedIn check
                    SessionManager.login(this, email)

                    // Navigate to Home dashboard and clear navigation stack
                    val intent = Intent(this, Home::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Authentication failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }
}