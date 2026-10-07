package com.example.pillpoint.repository

import com.example.pillpoint.database.AppDao
import com.example.pillpoint.database.User
import org.mindrot.jbcrypt.BCrypt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepository(private val appDao: AppDao) {

    /**
     * Hashes a plain-text password using BCrypt salt encryption.
     */
    private fun hashPassword(password: String): String {
        return BCrypt.hashpw(password, BCrypt.gensalt())
    }

    /**
     * Compares a plain-text password against a stored BCrypt hash.
     */
    private fun verifyPassword(plainText: String, hashed: String): Boolean {
        return try {
            BCrypt.checkpw(plainText, hashed)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Registers a new user with an encrypted password.
     */
    suspend fun registerUser(
        fullName: String,
        email: String,
        passwordRaw: String,
        phone: String,
        address: String
    ): Result<User> = withContext(Dispatchers.IO) {
        try {
            // Check if user already exists
            val existingUser = appDao.getUserByEmail(email)
            if (existingUser != null) {
                return@withContext Result.failure(Exception("An account with this email already exists."))
            }

            // Encrypt raw password
            val hashedPassword = hashPassword(passwordRaw)

            val newUser = User(
                fullName = fullName,
                email = email,
                passwordHash = hashedPassword,
                phone = phone,
                deliveryAddress = address
            )

            // Save to Room DB
            appDao.insertUser(newUser)
            Result.success(newUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Authenticates user login by checking stored encrypted hash.
     */
    suspend fun loginUser(email: String, passwordRaw: String): Result<User> = withContext(Dispatchers.IO) {
        try {
            val user = appDao.getUserByEmail(email)
                ?: return@withContext Result.failure(Exception("Account not found. Please register first."))

            if (verifyPassword(passwordRaw, user.passwordHash)) {
                Result.success(user)
            } else {
                Result.failure(Exception("Invalid email or password."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}