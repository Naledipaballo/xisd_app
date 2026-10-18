package com.example.pillpoint

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class Signup : AppCompatActivity() {

    private lateinit var firstName: EditText
    private lateinit var lastName: EditText
    private lateinit var email: EditText
    private lateinit var phone: EditText
    private lateinit var address: EditText
    private lateinit var password: EditText
    private lateinit var confirmPassword: EditText

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var signupButton: Button

    private var passwordVisible = false
    private var confirmPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        // Initialize Firebase Authentication and Cloud Firestore.
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Connect the Kotlin variables to the existing XML fields.
        firstName = findViewById(R.id.etFirstName)
        lastName = findViewById(R.id.etLastName)
        email = findViewById(R.id.etEmail)
        phone = findViewById(R.id.etPhone)
        address = findViewById(R.id.etAddress)
        password = findViewById(R.id.etPassword)
        confirmPassword = findViewById(R.id.etConfirmPassword)

        val togglePassword: ImageButton =
            findViewById(R.id.btnTogglePassword)

        val toggleConfirmPassword: ImageButton =
            findViewById(R.id.btnToggleConfirmPassword)

        signupButton = findViewById(R.id.btnSignup)

        val loginLink: TextView =
            findViewById(R.id.tvLoginLink)

        // Show or hide the password.
        togglePassword.setOnClickListener {
            passwordVisible = !passwordVisible
            toggleVisibility(
                password,
                togglePassword,
                passwordVisible
            )
        }

        // Show or hide the confirmation password.
        toggleConfirmPassword.setOnClickListener {
            confirmPasswordVisible = !confirmPasswordVisible
            toggleVisibility(
                confirmPassword,
                toggleConfirmPassword,
                confirmPasswordVisible
            )
        }

        // Create an account when the signup button is clicked.
        signupButton.setOnClickListener {
            createAccount()
        }

        // Navigate to the login screen.
        loginLink.setOnClickListener {
            startActivity(Intent(this, Login::class.java))
            finish()
        }
    }

    // Toggle password visibility without losing cursor position.
    private fun toggleVisibility(
        field: EditText,
        toggleButton: ImageButton,
        visible: Boolean
    ) {
        val cursorPosition =
            field.selectionStart.coerceAtLeast(0)

        if (visible) {
            field.inputType =
                InputType.TYPE_CLASS_TEXT or
                        InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD

            toggleButton.setImageResource(
                R.drawable.ic_visibility_off
            )
        } else {
            field.inputType =
                InputType.TYPE_CLASS_TEXT or
                        InputType.TYPE_TEXT_VARIATION_PASSWORD

            toggleButton.setImageResource(
                R.drawable.ic_visibility
            )
        }

        field.typeface = Typeface.DEFAULT

        field.setSelection(
            cursorPosition.coerceAtMost(field.text.length)
        )
    }

    // Validate the form and create the Firebase account.
    private fun createAccount() {

        val firstNameText = firstName.text.toString().trim()
        val lastNameText = lastName.text.toString().trim()
        val emailText = email.text.toString().trim()
        val phoneText = phone.text.toString().trim()
        val addressText = address.text.toString().trim()
        val passwordText = password.text.toString()
        val confirmText = confirmPassword.text.toString()

        // Check required fields.
        if (
            firstNameText.isBlank() ||
            lastNameText.isBlank() ||
            emailText.isBlank() ||
            passwordText.isBlank() ||
            confirmText.isBlank()
        ) {
            showMessage("Please complete all required fields.")
            return
        }

        // Validate the email address.
        if (!Patterns.EMAIL_ADDRESS.matcher(emailText).matches()) {
            email.error = "Enter a valid email address."
            email.requestFocus()
            return
        }

        // Firebase email/password authentication requires a
        // password of at least 6 characters.
        if (passwordText.length < 6) {
            password.error =
                "Password must be at least 6 characters."
            password.requestFocus()
            return
        }

        // Ensure both password fields match.
        if (passwordText != confirmText) {
            confirmPassword.error = "Passwords do not match."
            confirmPassword.requestFocus()
            return
        }

        // Prevent repeated taps while signup is processing.
        signupButton.isEnabled = false

        // Step 1: Create the account in Firebase Authentication.
        auth.createUserWithEmailAndPassword(
            emailText,
            passwordText
        ).addOnCompleteListener(this) { task ->

            if (task.isSuccessful) {

                val firebaseUser = auth.currentUser

                if (firebaseUser == null) {
                    signupButton.isEnabled = true
                    showMessage(
                        "Account created, but user details could not be loaded."
                    )
                    return@addOnCompleteListener
                }

                // Use the actual Firebase Authentication UID.
                val userId = firebaseUser.uid

                // Step 2: Prepare the user profile for Firestore.
                val userData = hashMapOf<String, Any>(
                    "userId" to userId,
                    "firstName" to firstNameText,
                    "lastName" to lastNameText,
                    "email" to emailText,
                    "phone" to phoneText,
                    "address" to addressText,
                    "createdAt" to FieldValue.serverTimestamp()
                )

                // Step 3: Save the profile in the users collection.
                db.collection("users")
                    .document(userId)
                    .set(userData)
                    .addOnSuccessListener {

                        signupButton.isEnabled = true

                        showMessage(
                            "Account created successfully."
                        )

                        // Open the login screen.
                        val intent = Intent(
                            this,
                            Login::class.java
                        )

                        intent.putExtra(
                            "USER_EMAIL",
                            emailText
                        )

                        startActivity(intent)
                        finish()
                    }
                    .addOnFailureListener { exception ->

                        signupButton.isEnabled = true

                        // Authentication succeeded, but saving the
                        // profile failed. The account may still exist.
                        showMessage(
                            "Account created, but profile saving failed: " +
                                    (exception.message ?: "Unknown error")
                        )
                    }

            } else {

                signupButton.isEnabled = true

                val errorMessage =
                    task.exception?.message
                        ?: "Please try again."

                showMessage(
                    "Signup failed: $errorMessage"
                )
            }
        }
    }

    // Display a message to the user.
    private fun showMessage(message: String) {
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_LONG
        ).show()
    }
}