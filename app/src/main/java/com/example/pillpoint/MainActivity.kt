package com.example.pillpoint

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Display the splash screen
        setContentView(R.layout.activity_splash)

        // Wait for 3 seconds before opening the Login screen
        Handler(Looper.getMainLooper()).postDelayed({

            val intent = Intent(this, login::class.java)
            startActivity(intent)

            // Close the splash activity
            finish()

        }, 3000)
    }
}
