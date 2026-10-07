package com.example.pillpoint.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "store_locations")
data class StoreLocation(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val storeName: String,
    val address: String,
    val phone: String,
    val operatingHours: String,
    val latitude: Double,
    val longitude: Double
)