package com.example.pillpoint

import java.util.Locale

/**
 * Validation and calculation rules shared by Pill Point Delivery.
 * Keep these functions independent of Android views and Firebase
 * so they can be tested with local JUnit tests.
 */
object AppValidation {

    fun isValidEmail(email: String): Boolean {
        val value = email.trim()
        return value.matches(
            Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        )
    }

    fun passwordsMatch(
        password: String,
        confirmPassword: String
    ): Boolean {
        return password.isNotEmpty() &&
                password == confirmPassword
    }

    // Firebase Email/Password accounts require at least six characters
    // under the default password policy.
    fun isValidPassword(password: String): Boolean {
        return password.length >= 6
    }

    fun hasRequiredFields(vararg fields: String): Boolean {
        return fields.all { it.isNotBlank() }
    }

    fun isValidQuantity(quantity: Int): Boolean {
        return quantity > 0
    }

    /**
     * Prices are represented in cents to avoid floating-point
     * rounding errors in cart and checkout calculations.
     */
    fun calculateCartTotalCents(
        pricesInCents: List<Long>,
        quantities: List<Int>
    ): Long? {
        if (pricesInCents.size != quantities.size) return null
        if (pricesInCents.isEmpty()) return 0L
        if (pricesInCents.any { it < 0L }) return null
        if (quantities.any { it <= 0 }) return null

        return try {
            pricesInCents.indices.fold(0L) { total, index ->
                Math.addExact(
                    total,
                    Math.multiplyExact(
                        pricesInCents[index],
                        quantities[index].toLong()
                    )
                )
            }
        } catch (_: ArithmeticException) {
            null
        }
    }

    fun searchMedicationNames(
        medicationNames: List<String>,
        query: String
    ): List<String> {
        val searchTerm = query.trim()
        if (searchTerm.isEmpty()) return medicationNames

        return medicationNames.filter {
            it.contains(searchTerm, ignoreCase = true)
        }
    }

    fun userOwnsRecord(
        signedInUserId: String?,
        recordOwnerId: String?
    ): Boolean {
        return !signedInUserId.isNullOrBlank() &&
                !recordOwnerId.isNullOrBlank() &&
                signedInUserId == recordOwnerId
    }

    fun isValidPrescriptionFileName(
        fileName: String
    ): Boolean {
        val cleanName = fileName.trim()
        if (cleanName.isEmpty()) return false

        val extension = cleanName
            .substringAfterLast('.', missingDelimiterValue = "")
            .lowercase(Locale.ROOT)

        return extension in setOf("pdf", "jpg", "jpeg", "png")
    }

    fun isValidOrderStatus(status: String): Boolean {
        return status in setOf(
            "Pending",
            "Processing",
            "Out for Delivery",
            "Delivered",
            "Cancelled"
        )
    }
}