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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.pillpoint.database.AppDatabase
import com.example.pillpoint.repository.AuthRepository
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch

class login : AppCompatActivity() {

    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var loginBtn: Button
    private lateinit var togglePassword: ImageButton
    private lateinit var signup: TextView
    private lateinit var googleSignInBtn: SignInButton

    private lateinit var authRepository: AuthRepository
    private lateinit var googleSignInClient: GoogleSignInClient

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            account?.let { handleGoogleSignInSuccess(it.email.orEmpty(), it.displayName.orEmpty()) }
        } catch (e: ApiException) {
            Toast.makeText(this, "Google Sign-In failed: ${e.statusCode}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        // Initialize Database Repository
        val appDao = AppDatabase.getDatabase(applicationContext).appDao()
        authRepository = AuthRepository(appDao)

        // Bind Views
        email = findViewById(R.id.etEmail)
        password = findViewById(R.id.etPassword)
        email.setText(intent.getStringExtra("USER_EMAIL").orEmpty())
        loginBtn = findViewById(R.id.btnLogin)
        togglePassword = findViewById(R.id.btnTogglePassword)

        var passwordVisible = false
        togglePassword.setOnClickListener {
            passwordVisible = !passwordVisible
            password.inputType = if (passwordVisible) {
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            password.setSelection(password.text.length)
            togglePassword.setImageResource(if (passwordVisible) R.drawable.ic_visibility_off else R.drawable.ic_visibility)
            togglePassword.contentDescription = if (passwordVisible) "Hide password" else "Show password"
        }
        signup = findViewById(R.id.tvSignup)
        googleSignInBtn = findViewById(R.id.btnGoogleSignIn)

        configureGoogleSSO()

        // Standard Login Click Handler
        loginBtn.setOnClickListener {
            handleStandardLogin()
        }

        // Google SSO Click Handler
        googleSignInBtn.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }

        // Sign Up Link Click Handler
        signup.setOnClickListener {
            val intent = Intent(this, Signup::class.java)
            startActivity(intent)
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun configureGoogleSSO() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun handleStandardLogin() {
        val emailText = email.text.toString().trim()
        val passwordText = password.text.toString().trim()

        if (emailText.isEmpty() || passwordText.isEmpty()) {
            Toast.makeText(this, "Please enter your email and Password", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val result = authRepository.loginUser(emailText, passwordText)
            result.onSuccess { user ->
                navigateToHome(user.email)
            }.onFailure { exception ->
                Toast.makeText(this@login, exception.message ?: "Login failed", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun handleGoogleSignInSuccess(googleEmail: String, displayName: String) {
        lifecycleScope.launch {
            val existingUser = AppDatabase.getDatabase(applicationContext).appDao().getUserByEmail(googleEmail)
            if (existingUser != null) {
                navigateToHome(existingUser.email)
            } else {
                // Google Sign-In does NOT create an account automatically.
                // The user must register in Pill Point Delivery first.
                Toast.makeText(
                    this@login,
                    "No Pill Point account was found. Please create an account first.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun navigateToHome(userEmail: String) {
        SessionManager.login(this, userEmail)
        val intent = Intent(this, Home::class.java).apply {
            putExtra("USER_EMAIL", userEmail)
        }
        startActivity(intent)
        finish()
    }
}
