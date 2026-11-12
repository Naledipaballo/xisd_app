package com.example.pillpoint

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AdminLogin : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnAdminLogin: Button
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_login)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        etEmail = findViewById(R.id.etAdminEmail)
        etPassword = findViewById(R.id.etAdminPassword)
        btnAdminLogin = findViewById(R.id.btnAdminLogin)
        progressBar = findViewById(R.id.adminProgressBar)

        btnAdminLogin.setOnClickListener {
            loginAdmin()
        }
    }

    private fun loginAdmin() {

        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(
                this,
                "Please enter your email and password.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        progressBar.visibility = android.view.View.VISIBLE
        btnAdminLogin.isEnabled = false

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->

                val uid = result.user?.uid

                if (uid == null) {
                    showLoginError("Unable to identify administrator.")
                    auth.signOut()
                    return@addOnSuccessListener
                }

                // Check the administrator's Firestore document.
                db.collection("users")
                    .document(uid)
                    .get()
                    .addOnSuccessListener { document ->

                        val role = document.getString("role")

                        if (document.exists() && role == "admin") {

                            progressBar.visibility =
                                android.view.View.GONE

                            val intent = Intent(
                                this,
                                AdminDashboard::class.java
                            )

                            startActivity(intent)
                            finish()

                        } else {

                            auth.signOut()
                            showLoginError(
                                "Access denied. Admin account only."
                            )
                        }
                    }
                    .addOnFailureListener { exception ->
                        auth.signOut()
                        showLoginError(
                            "Could not verify admin access: ${exception.message}"
                        )
                    }
            }
            .addOnFailureListener { exception ->
                showLoginError(
                    "Login failed: ${exception.message}"
                )
            }
    }

    private fun showLoginError(message: String) {
        progressBar.visibility = android.view.View.GONE
        btnAdminLogin.isEnabled = true

        Toast.makeText(
            this,
            message,
            Toast.LENGTH_LONG
        ).show()
    }
}