package com.example.pillpoint

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Display the splash screen
        setContentView(R.layout.activity_splash)

        // Wait for 3 seconds before navigating
        Handler(Looper.getMainLooper()).postDelayed({

            val currentUser = FirebaseAuth.getInstance().currentUser
            val isLoggedIn = currentUser != null || SessionManager.isLoggedIn(this)

            val intent = if (isLoggedIn) {
                Intent(this, Home::class.java)
            } else {
                Intent(this, Login::class.java)
            }

            startActivity(intent)

            // Close the splash activity so pressing back doesn't return here
            finish()

        }, 3000)
    }
}