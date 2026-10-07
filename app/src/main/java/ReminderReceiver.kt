package com.example.pillpoint

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val medicineName = intent.getStringExtra("medicine_name") ?: "Medication"
        Toast.makeText(context, "Time to take your medication: $medicineName", Toast.LENGTH_LONG).show()
    }
}