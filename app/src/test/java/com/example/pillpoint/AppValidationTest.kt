package com.example.pillpoint

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppValidationTest {

    // ---------------- AUTHENTICATION ----------------

    @Test
    fun validEmailIsAccepted() {
        assertTrue(AppValidation.isValidEmail("user@example.com"))
    }

    @Test
    fun emailWithWhitespaceIsAcceptedAfterTrimming() {
        assertTrue(AppValidation.isValidEmail(" user@example.com "))
    }

    @Test
    fun invalidEmailsAreRejected() {
        assertFalse(AppValidation.isValidEmail(""))
        assertFalse(AppValidation.isValidEmail("user"))
        assertFalse(AppValidation.isValidEmail("user@"))
        assertFalse(AppValidation.isValidEmail("user@example"))
    }

    @Test
    fun matchingPasswordsAreAccepted() {
        assertTrue(
            AppValidation.passwordsMatch("secret123", "secret123")
        )
    }

    @Test
    fun differentPasswordsAreRejected() {
        assertFalse(
            AppValidation.passwordsMatch("secret123", "secret456")
        )
    }

    @Test
    fun emptyPasswordsAreRejected() {
        assertFalse(AppValidation.passwordsMatch("", ""))
    }

    @Test
    fun passwordMustHaveAtLeastSixCharacters() {
        assertFalse(AppValidation.isValidPassword("12345"))
        assertTrue(AppValidation.isValidPassword("123456"))
        assertTrue(AppValidation.isValidPassword("securePass"))
    }

    @Test
    fun requiredFieldsRejectBlankValues() {
        assertTrue(
            AppValidation.hasRequiredFields(
                "Full Name",
                "user@example.com",
                "Address"
            )
        )

        assertFalse(
            AppValidation.hasRequiredFields(
                "Full Name",
                "",
                "Address"
            )
        )

        assertFalse(
            AppValidation.hasRequiredFields("   ")
        )
    }

    // ---------------- CART AND CHECKOUT ----------------

    @Test
    fun cartTotalIsCalculatedCorrectly() {
        // R50.00 x 2 + R25.50 x 1 = R125.50
        val total = AppValidation.calculateCartTotalCents(
            pricesInCents = listOf(5000L, 2550L),
            quantities = listOf(2, 1)
        )

        assertEquals(12550L, total)
    }

    @Test
    fun emptyCartHasZeroTotal() {
        assertEquals(
            0L,
            AppValidation.calculateCartTotalCents(
                emptyList(),
                emptyList()
            )
        )
    }

    @Test
    fun mismatchedCartListsAreRejected() {
        assertNull(
            AppValidation.calculateCartTotalCents(
                pricesInCents = listOf(1000L, 2000L),
                quantities = listOf(1)
            )
        )
    }

    @Test
    fun negativePricesAreRejected() {
        assertNull(
            AppValidation.calculateCartTotalCents(
                pricesInCents = listOf(-100L),
                quantities = listOf(1)
            )
        )
    }

    @Test
    fun zeroOrNegativeQuantitiesAreRejected() {
        assertFalse(AppValidation.isValidQuantity(0))
        assertFalse(AppValidation.isValidQuantity(-1))
        assertTrue(AppValidation.isValidQuantity(1))
    }

    @Test
    fun cartCalculationRejectsInvalidQuantities() {
        assertNull(
            AppValidation.calculateCartTotalCents(
                pricesInCents = listOf(1000L),
                quantities = listOf(0)
            )
        )
    }

    @Test
    fun cartCalculationRejectsOverflow() {
        assertNull(
            AppValidation.calculateCartTotalCents(
                pricesInCents = listOf(Long.MAX_VALUE, 100L),
                quantities = listOf(1, 1)
            )
        )
    }

    // ---------------- MEDICATION SEARCH ----------------

    @Test
    fun medicationSearchIgnoresLetterCase() {
        val results = AppValidation.searchMedicationNames(
            listOf("Paracetamol", "Ibuprofen", "Vitamin C"),
            "para"
        )

        assertEquals(listOf("Paracetamol"), results)
    }

    @Test
    fun medicationSearchTrimsQueryWhitespace() {
        val results = AppValidation.searchMedicationNames(
            listOf("Paracetamol", "Ibuprofen"),
            "  ibu  "
        )

        assertEquals(listOf("Ibuprofen"), results)
    }

    @Test
    fun unmatchedMedicationSearchReturnsEmptyList() {
        val results = AppValidation.searchMedicationNames(
            listOf("Paracetamol", "Ibuprofen"),
            "not-a-medication"
        )

        assertTrue(results.isEmpty())
    }

    @Test
    fun emptyMedicationSearchReturnsAllItems() {
        val medications = listOf("Paracetamol", "Ibuprofen")

        assertEquals(
            medications,
            AppValidation.searchMedicationNames(medications, "")
        )
    }

    // ---------------- FIRESTORE RECORD OWNERSHIP ----------------

    @Test
    fun recordOwnerMatchesSignedInUser() {
        assertTrue(
            AppValidation.userOwnsRecord(
                signedInUserId = "user123",
                recordOwnerId = "user123"
            )
        )
    }

    @Test
    fun anotherUserCannotAccessOwnedRecord() {
        assertFalse(
            AppValidation.userOwnsRecord(
                signedInUserId = "user123",
                recordOwnerId = "user456"
            )
        )
    }

    @Test
    fun missingUserCannotOwnRecord() {
        assertFalse(
            AppValidation.userOwnsRecord(
                signedInUserId = null,
                recordOwnerId = "user123"
            )
        )

        assertFalse(
            AppValidation.userOwnsRecord(
                signedInUserId = "",
                recordOwnerId = "user123"
            )
        )
    }

    @Test
    fun missingRecordOwnerIsRejected() {
        assertFalse(
            AppValidation.userOwnsRecord(
                signedInUserId = "user123",
                recordOwnerId = null
            )
        )
    }

    // ---------------- PRESCRIPTIONS ----------------

    @Test
    fun supportedPrescriptionFileTypesAreAccepted() {
        assertTrue(
            AppValidation.isValidPrescriptionFileName("prescription.pdf")
        )
        assertTrue(
            AppValidation.isValidPrescriptionFileName("photo.jpg")
        )
        assertTrue(
            AppValidation.isValidPrescriptionFileName("photo.jpeg")
        )
        assertTrue(
            AppValidation.isValidPrescriptionFileName("scan.png")
        )
    }

    @Test
    fun unsupportedPrescriptionFileTypesAreRejected() {
        assertFalse(
            AppValidation.isValidPrescriptionFileName("document.exe")
        )
        assertFalse(
            AppValidation.isValidPrescriptionFileName("notes.txt")
        )
        assertFalse(
            AppValidation.isValidPrescriptionFileName("")
        )
    }

    // ---------------- ORDERS ----------------

    @Test
    fun validOrderStatusesAreAccepted() {
        val statuses = listOf(
            "Pending",
            "Processing",
            "Out for Delivery",
            "Delivered",
            "Cancelled"
        )

        statuses.forEach { status ->
            assertTrue(AppValidation.isValidOrderStatus(status))
        }
    }

    @Test
    fun unknownOrderStatusIsRejected() {
        assertFalse(AppValidation.isValidOrderStatus("Unknown"))
        assertFalse(AppValidation.isValidOrderStatus(""))
    }
}