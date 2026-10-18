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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class Login : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var loginButton: Button
    private lateinit var togglePassword: ImageButton
    private lateinit var googleLoginButton: Button
    private lateinit var googleSignInClient: GoogleSignInClient

    private var passwordVisible = false

    // Handles the result returned by Google Sign-In.
    private val googleSignInLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->

            if (result.resultCode != RESULT_OK || result.data == null) {
                showMessage("Google Sign-In was cancelled.")
                return@registerForActivityResult
            }

            val task = GoogleSignIn.getSignedInAccountFromIntent(
                result.data
            )

            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken

                if (idToken.isNullOrBlank()) {
                    showMessage(
                        "Google ID token is missing. Check your Firebase configuration."
                    )
                } else {
                    firebaseAuthWithGoogle(idToken)
                }

            } catch (e: ApiException) {
                showMessage(
                    "Google Sign-In failed: ${e.message}"
                )
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize Firebase Authentication.
        auth = FirebaseAuth.getInstance()

        // Connect to the existing activity_login.xml IDs.
        emailInput = findViewById(R.id.etEmail)
        passwordInput = findViewById(R.id.etPassword)
        loginButton = findViewById(R.id.btnLogin)
        togglePassword = findViewById(R.id.btnTogglePassword)
        googleLoginButton = findViewById(R.id.btnGoogleSignIn)

        val signupLink: TextView = findViewById(R.id.tvSignup)

        // Configure Google Sign-In with the Web Client ID.
        val googleOptions =
            GoogleSignInOptions.Builder(
                GoogleSignInOptions.DEFAULT_SIGN_IN
            )
                .requestIdToken(
                    getString(R.string.default_web_client_id)
                )
                .requestEmail()
                .build()

        googleSignInClient = GoogleSignIn.getClient(
            this,
            googleOptions
        )

        // Prefill the email after returning from Signup.
        val prefilledEmail =
            intent.getStringExtra("USER_EMAIL")

        if (!prefilledEmail.isNullOrEmpty()) {
            emailInput.setText(prefilledEmail)
            passwordInput.requestFocus()
        }

        // Show or hide the password.
        togglePassword.setOnClickListener {
            passwordVisible = !passwordVisible
            togglePasswordVisibility()
        }

        // Email and password login.
        loginButton.setOnClickListener {
            loginUser()
        }

        // Google login.
        googleLoginButton.setOnClickListener {
            signInWithGoogle()
        }

        // Navigate to Signup.
        signupLink.setOnClickListener {
            startActivity(Intent(this, Signup::class.java))
            finish()
        }
    }

    // Toggles password visibility while preserving cursor position.
    private fun togglePasswordVisibility() {
        val cursorPosition =
            passwordInput.selectionStart.coerceAtLeast(0)

        if (passwordVisible) {
            passwordInput.inputType =
                InputType.TYPE_CLASS_TEXT or
                        InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD

            togglePassword.setImageResource(
                R.drawable.ic_visibility_off
            )
        } else {
            passwordInput.inputType =
                InputType.TYPE_CLASS_TEXT or
                        InputType.TYPE_TEXT_VARIATION_PASSWORD

            togglePassword.setImageResource(
                R.drawable.ic_visibility
            )
        }

        passwordInput.typeface = Typeface.DEFAULT

        passwordInput.setSelection(
            cursorPosition.coerceAtMost(
                passwordInput.text.length
            )
        )
    }

    // Launches Google account selection.
    private fun signInWithGoogle() {
        googleLoginButton.isEnabled = false

        googleSignInClient.signOut()
            .addOnCompleteListener {
                googleLoginLauncher()
            }
    }

    private fun googleLoginLauncher() {
        try {
            googleSignInLauncher.launch(
                googleSignInClient.signInIntent
            )
        } catch (e: Exception) {
            googleLoginButton.isEnabled = true
            showMessage(
                "Could not open Google Sign-In: ${e.message}"
            )
        }
    }

    // Exchanges Google's ID token for a Firebase credential.
    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(
            idToken,
            null
        )

        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->

                googleLoginButton.isEnabled = true

                if (task.isSuccessful) {
                    openHome()
                } else {
                    showMessage(
                        "Google authentication failed: " +
                                (task.exception?.message
                                    ?: "Please try again.")
                    )
                }
            }
    }

    // Logs in an existing email/password account.
    private fun loginUser() {
        val email = emailInput.text.toString().trim()

        // Do not trim passwords; spaces can be part of a password.
        val password = passwordInput.text.toString()

        if (email.isBlank()) {
            emailInput.error = "Enter your email address."
            emailInput.requestFocus()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.error = "Enter a valid email address."
            emailInput.requestFocus()
            return
        }

        if (password.isEmpty()) {
            passwordInput.error = "Enter your password."
            passwordInput.requestFocus()
            return
        }

        loginButton.isEnabled = false

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->

                loginButton.isEnabled = true

                if (task.isSuccessful) {
                    openHome()
                } else {
                    showMessage(
                        "Login failed: " +
                                (task.exception?.message
                                    ?: "Check your email and password.")
                    )
                }
            }
    }

    // Opens Home after Firebase confirms authentication.
    private fun openHome() {
        val firebaseUser = auth.currentUser

        if (firebaseUser == null) {
            showMessage("Unable to verify your signed-in account.")
            return
        }

        val userEmail = firebaseUser.email.orEmpty()

        // Preserve the existing session handling in your project.
        SessionManager.login(this, userEmail)

        showMessage("Login successful!")

        val intent = Intent(this, Home::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        startActivity(intent)
        finish()
    }

    private fun showMessage(message: String) {
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_LONG
        ).show()
    }
}