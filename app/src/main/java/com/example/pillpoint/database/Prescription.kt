package com.example.pillpoint.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "prescriptions")
data class Prescription(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userEmail: String,
    val doctorName: String,
    val medicationName: String,
    val dosage: String,
    val scriptUrlOrPath: String,
    val status: String
)