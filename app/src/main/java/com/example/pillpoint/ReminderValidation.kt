package com.example.pillpoint

import java.text.SimpleDateFormat
import java.util.Locale

object ReminderValidation {

    // Validate the entered date and time and require a future time.
    fun parseFutureTriggerTime(
        date: String,
        time: String,
        nowMillis: Long = System.currentTimeMillis()
    ): Long? {
        return try {
            val formatter = SimpleDateFormat(
                "dd/MM/yyyy HH:mm",
                Locale.US
            ).apply {
                isLenient = false
            }

            val parsed = formatter.parse("$date $time")
                ?: return null

            parsed.time.takeIf { it > nowMillis }
        } catch (_: Exception) {
            null
        }
    }

    // Return the repeat interval, or null for a one-time reminder.
    fun repeatIntervalMillis(frequency: String): Long? {
        return when (frequency) {
            "Once Daily" -> 24L * 60L * 60L * 1000L
            "Twice Daily" -> 12L * 60L * 60L * 1000L
            "Weekly" -> 7L * 24L * 60L * 60L * 1000L
            "As Needed" -> null
            else -> null
        }
    }
}