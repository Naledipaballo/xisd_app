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
import com.google.firebase.database.FirebaseDatabase

class Signup : AppCompatActivity() {

    private lateinit var firstName: EditText
    private lateinit var lastName: EditText
    private lateinit var email: EditText
    private lateinit var phone: EditText
    private lateinit var address: EditText
    private lateinit var password: EditText
    private lateinit var confirmPassword: EditText
    private lateinit var auth: FirebaseAuth

    private var passwordVisible = false
    private var confirmPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        firstName = findViewById(R.id.etFirstName)
        lastName = findViewById(R.id.etLastName)
        email = findViewById(R.id.etEmail)
        phone = findViewById(R.id.etPhone)
        address = findViewById(R.id.etAddress)
        password = findViewById(R.id.etPassword)
        confirmPassword = findViewById(R.id.etConfirmPassword)

        val togglePassword: ImageButton = findViewById(R.id.btnTogglePassword)
        val toggleConfirmPassword: ImageButton = findViewById(R.id.btnToggleConfirmPassword)
        val signupButton: Button = findViewById(R.id.btnSignup)
        val loginLink: TextView = findViewById(R.id.tvLoginLink)

        togglePassword.setOnClickListener {
            passwordVisible = !passwordVisible
            toggleVisibility(password, togglePassword, passwordVisible)
        }

        toggleConfirmPassword.setOnClickListener {
            confirmPasswordVisible = !confirmPasswordVisible
            toggleVisibility(confirmPassword, toggleConfirmPassword, confirmPasswordVisible)
        }

        signupButton.setOnClickListener {
            createAccount()
        }

        loginLink.setOnClickListener {
            startActivity(Intent(this, Login::class.java))
            finish()
        }
    }

    private fun toggleVisibility(field: EditText, toggleButton: ImageButton, visible: Boolean) {
        val cursorPosition = field.selectionStart.coerceAtLeast(0)

        if (visible) {
            field.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            toggleButton.setImageResource(R.drawable.ic_visibility_off)
        } else {
            field.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            toggleButton.setImageResource(R.drawable.ic_visibility)
        }

        field.typeface = Typeface.DEFAULT
        field.setSelection(cursorPosition.coerceAtMost(field.text.length))
    }

    private fun createAccount() {
        val firstNameText = firstName.text.toString().trim()
        val lastNameText = lastName.text.toString().trim()
        val emailText = email.text.toString().trim()
        val phoneText = phone.text.toString().trim()
        val addressText = address.text.toString().trim()
        val passwordText = password.text.toString()
        val confirmText = confirmPassword.text.toString()

        if (firstNameText.isBlank() || lastNameText.isBlank() || emailText.isBlank() ||
            passwordText.isBlank() || confirmText.isBlank()
        ) {
            showMessage("Please complete all required fields.")
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(emailText).matches()) {
            email.error = "Enter a valid email address."
            return
        }

        if (passwordText.length < 6) {
            password.error = "Password must be at least 6 characters."
            return
        }

        if (passwordText != confirmText) {
            confirmPassword.error = "Passwords do not match."
            return
        }

        val signupButton: Button = findViewById(R.id.btnSignup)
        signupButton.isEnabled = false

        // Create user using Firebase Authentication
        auth.createUserWithEmailAndPassword(emailText, passwordText)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val firebaseUser = auth.currentUser
                    val userId = firebaseUser?.uid ?: emailText.replace(".", "_")

                    val database = FirebaseDatabase.getInstance()
                    val usersRef = database.getReference("users")

                    val userData = mapOf(
                        "firstName" to firstNameText,
                        "lastName" to lastNameText,
                        "email" to emailText,
                        "phone" to phoneText,
                        "address" to addressText
                    )

                    usersRef.child(userId).setValue(userData)
                        .addOnSuccessListener {
                            signupButton.isEnabled = true
                            showMessage("Account created successfully.")
                            val intent = Intent(this, Login::class.java)
                            intent.putExtra("USER_EMAIL", emailText)
                            startActivity(intent)
                            finish()
                        }
                        .addOnFailureListener { e ->
                            signupButton.isEnabled = true
                            showMessage("Failed to save profile: ${e.message}")
                        }
                } else {
                    signupButton.isEnabled = true
                    showMessage("Authentication failed: ${task.exception?.message}")
                }
            }
    }

    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}