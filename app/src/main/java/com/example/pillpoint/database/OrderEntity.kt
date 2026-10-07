package com.example.pillpoint.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey(autoGenerate = true) val orderId: Int = 0,
    val userEmail: String,
    val itemsSummary: String,
    val totalPrice: Double,
    val paymentMethod: String,
    val status: String,
    val deliveryAddress: String,
    val orderDate: String
)