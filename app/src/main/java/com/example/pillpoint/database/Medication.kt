package com.example.pillpoint.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medications")
data class Medication(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,
    val price: Double,
    val description: String,
    val imageResName: String
)