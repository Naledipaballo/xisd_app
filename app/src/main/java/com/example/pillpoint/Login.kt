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
    private lateinit var googleLoginButton: Button // Standard Button type matching XML

    private lateinit var googleSignInClient: GoogleSignInClient
    private var passwordVisible = false

    // Activity Result Launcher for Google Sign-In
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            if (account != null && account.idToken != null) {
                firebaseAuthWithGoogle(account.idToken!!)
            }
        } catch (e: ApiException) {
            Toast.makeText(this, "Google Sign-In failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        emailInput = findViewById(R.id.etEmail)
        passwordInput = findViewById(R.id.etPassword)
        loginButton = findViewById(R.id.btnLogin)
        togglePassword = findViewById(R.id.btnTogglePassword)

        // Matches your activity_login.xml ID
        googleLoginButton = findViewById(R.id.btnGoogleSignIn)
        val signupLink: TextView = findViewById(R.id.tvSignup)

        // Configure Google Sign-In using the Web Client ID from strings.xml
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

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

        // Google Login button click listener
        googleLoginButton.setOnClickListener {
            signInWithGoogle()
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

        passwordInput.typeface = Typeface.DEFAULT
        passwordInput.setSelection(cursorPosition.coerceAtMost(passwordInput.text.length))
    }

    private fun signInWithGoogle() {
        // Sign out first to ensure the account chooser is prompted
        googleSignInClient.signOut().addOnCompleteListener {
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Google Login successful!", Toast.LENGTH_SHORT).show()

                    val userEmail = auth.currentUser?.email ?: ""
                    SessionManager.login(this, userEmail)

                    val intent = Intent(this, Home::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Authentication Failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
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

                    SessionManager.login(this, email)

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