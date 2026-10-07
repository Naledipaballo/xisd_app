package com.example.pillpoint.database

import androidx.room.*

@Dao
interface AppDao {

    // --- USER AUTHENTICATION & SETTINGS ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<User>)

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): User?

    @Update
    suspend fun updateUserProfile(user: User)

    @Query("UPDATE users SET fullName = :name, phone = :phone, deliveryAddress = :address WHERE email = :email")
    suspend fun updateUserProfileFields(email: String, name: String, phone: String, address: String)

    // --- MEDICATIONS ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedications(medications: List<Medication>)

    @Query("SELECT * FROM medications")
    suspend fun getAllMedications(): List<Medication>

    // --- CART ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addToCart(item: CartEntity)

    @Query("SELECT * FROM cart_items WHERE userEmail = :email")
    suspend fun getUserCart(email: String): List<CartEntity>

    @Query("DELETE FROM cart_items WHERE userEmail = :email")
    suspend fun clearCart(email: String)

    // --- ORDERS ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createOrder(order: OrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrders(orders: List<OrderEntity>)

    @Query("SELECT * FROM orders WHERE userEmail = :email ORDER BY orderId DESC")
    suspend fun getUserOrders(email: String): List<OrderEntity>

    // --- PRESCRIPTIONS ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrescriptions(prescriptions: List<Prescription>)

    @Query("SELECT * FROM prescriptions WHERE userEmail = :email")
    suspend fun getUserPrescriptions(email: String): List<Prescription>

    // --- REMINDERS ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminders(reminders: List<Reminder>)

    @Query("SELECT * FROM reminders WHERE userEmail = :email")
    suspend fun getUserReminders(email: String): List<Reminder>

    // --- STORE LOCATIONS ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStoreLocations(locations: List<StoreLocation>)

    @Query("SELECT * FROM store_locations")
    suspend fun getAllStores(): List<StoreLocation>
}