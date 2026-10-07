package com.example.pillpoint.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cart_items")
data class CartEntity(
    @PrimaryKey(autoGenerate = true) val cartItemId: Int = 0,
    val userEmail: String,
    val medicationId: String,
    val medicationName: String,
    val price: Double,
    var quantity: Int,
    val imageResName: String
)