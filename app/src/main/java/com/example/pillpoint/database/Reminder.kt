package com.example.pillpoint.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userEmail: String,
    val medicineName: String,
    val reminderTime: String,
    val dosageInfo: String,
    val isActive: Boolean = true
)