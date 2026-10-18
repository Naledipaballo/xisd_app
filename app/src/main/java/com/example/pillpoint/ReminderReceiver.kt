package com.example.pillpoint

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class ReminderReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "medication_reminders"

        const val EXTRA_MEDICATION_NAME = "medication_name"
        const val EXTRA_DOSAGE = "dosage"
        const val EXTRA_REMINDER_ID = "reminder_id"

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Medication Reminders",
                    NotificationManager.IMPORTANCE_HIGH
                )

                channel.description =
                    "Notifications for scheduled medication reminders"

                val manager = context.getSystemService(
                    Context.NOTIFICATION_SERVICE
                ) as NotificationManager

                manager.createNotificationChannel(channel)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        createNotificationChannel(context)

        // Android 13 and newer require notification permission.
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val medicationName =
            intent.getStringExtra(EXTRA_MEDICATION_NAME)
                ?: "Your medication"

        val dosage =
            intent.getStringExtra(EXTRA_DOSAGE)
                ?: "Check your prescribed dosage"

        val notificationId =
            intent.getIntExtra(EXTRA_REMINDER_ID, 1001)

        val notification = NotificationCompat.Builder(
            context,
            CHANNEL_ID
        )
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Pill Point Delivery")
            .setContentText("Time to take $medicationName")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Medication: $medicationName\nDosage: $dosage"
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context)
            .notify(notificationId, notification)
    }
}