package com.example.pillpoint

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Central Firebase helper for Pill Point Delivery.
 *
 * Firebase Authentication manages user sign-in.
 * Cloud Firestore stores application data.
 */
object FirebaseHelper {

    // Firebase Authentication instance.
    val auth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    // Cloud Firestore database instance.
    val db: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }

    // Returns the UID of the currently signed-in user.
    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    // Checks whether a user is signed in.
    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }
}