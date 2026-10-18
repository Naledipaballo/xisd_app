package com.example.pillpoint

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

// Represents one product in the shopping cart.
data class CartItem(
    val id: String,
    val name: String,
    val price: Double,
    val imageResId: Int,
    var quantity: Int = 1
)

// Handles cart operations using Cloud Firestore.
object CartRepository {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    // Cached cart items for the current screen.
    val items = mutableListOf<CartItem>()

    // Add a product to the signed-in user's Firestore cart.
    fun addItem(
        item: CartItem,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val user = auth.currentUser

        if (user == null) {
            onError("Please log in before adding items to your cart.")
            return
        }

        val documentId = "${user.uid}_${item.id}"
        val cartDocument = db.collection("cart").document(documentId)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(cartDocument)
            val currentQuantity =
                snapshot.getLong("quantity")?.toInt() ?: 0

            val newQuantity = currentQuantity + 1

            val cartData = hashMapOf<String, Any>(
                "userId" to user.uid,
                "medicationID" to item.id,
                "medicationName" to item.name,
                "unitPrice" to item.price,
                "quantity" to newQuantity,
                "addedAt" to FieldValue.serverTimestamp()
            )

            transaction.set(cartDocument, cartData)
            newQuantity
        }.addOnSuccessListener { newQuantity ->

            val existingItem = items.find { it.id == item.id }

            if (existingItem != null) {
                existingItem.quantity = newQuantity
            } else {
                items.add(item.copy(quantity = newQuantity))
            }

            onSuccess()
        }.addOnFailureListener { exception ->
            onError(
                exception.message ?: "Could not add the product to your cart."
            )
        }
    }

    // Calculate the total cost of the products before delivery.
    fun getSubtotal(): Double {
        return items.sumOf { it.price * it.quantity }
    }

    // Load cart items belonging to the signed-in user.
    fun loadItems(
        context: Context,
        onSuccess: (List<CartItem>) -> Unit,
        onError: (String) -> Unit
    ) {
        val user = auth.currentUser

        if (user == null) {
            items.clear()
            onError("Please log in to view your cart.")
            return
        }

        // Fetch collection and filter client-side to safely handle both 'userId' and 'userid' field casings
        db.collection("cart")
            .get()
            .addOnSuccessListener { documents ->
                Log.d("CartDebug", "Total documents fetched from cart collection: ${documents.size()}")

                val loadedItems = documents.documents.mapNotNull { document ->
                    val docUserId = document.getString("userId") ?: document.getString("userid")

                    if (docUserId != user.uid) return@mapNotNull null

                    val productId = document.getString("medicationID")
                        ?: document.getString("productId")
                        ?: document.getString("id")
                        ?: return@mapNotNull null

                    val name = document.getString("medicationName")
                        ?: document.getString("name")
                        ?: ""

                    val price = document.getDouble("unitPrice")
                        ?: document.getDouble("price")
                        ?: 0.0

                    val quantity = document.getLong("quantity")?.toInt() ?: 1

                    CartItem(
                        id = productId,
                        name = name,
                        price = price,
                        imageResId = getImageResource(context, productId),
                        quantity = quantity
                    )
                }

                Log.d("CartDebug", "Successfully filtered ${loadedItems.size} items for user ${user.uid}")

                items.clear()
                items.addAll(loadedItems)

                onSuccess(items.toList())
            }
            .addOnFailureListener { exception ->
                Log.e("CartDebug", "Failed to load cart items: ${exception.message}")
                onError(
                    exception.message ?: "Failed to load cart items."
                )
            }
    }

    // Remove a product from Firestore and the local cart.
    fun removeItem(
        item: CartItem,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val user = auth.currentUser

        if (user == null) {
            onError("Please log in to manage your cart.")
            return
        }

        val documentId = "${user.uid}_${item.id}"

        db.collection("cart")
            .document(documentId)
            .delete()
            .addOnSuccessListener {
                items.removeAll { it.id == item.id }
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onError(
                    exception.message ?: "Could not remove this product."
                )
            }
    }

    // Clear all cart items belonging to the current user from Firestore and local list.
    fun clearCart(
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val user = auth.currentUser
        if (user == null) {
            onError("User not logged in")
            return
        }

        db.collection("cart")
            .get()
            .addOnSuccessListener { documents ->
                val batch = db.batch()
                for (doc in documents) {
                    val docUserId = doc.getString("userId") ?: doc.getString("userid")
                    if (docUserId == user.uid) {
                        batch.delete(doc.reference)
                    }
                }
                batch.commit()
                    .addOnSuccessListener {
                        items.clear()
                        onSuccess()
                    }
                    .addOnFailureListener { e ->
                        onError(e.message ?: "Failed to clear cart items")
                    }
            }
            .addOnFailureListener { e ->
                onError(e.message ?: "Failed to query cart items")
            }
    }

    // Map each Firestore product ID to the drawable image in the app.
    private fun getImageResource(
        context: Context,
        productId: String
    ): Int {
        val drawableName = when (productId) {
            "med001", "allergex_tablets" -> "allergextablets"
            "allergex_syrup" -> "allergexsyrup"
            "andolex_oral_gel" -> "andolexcoralgel"
            "band_aid_plastic" -> "bandaidplastic"
            "benelyn_syrup" -> "benelyinsyrup"
            "berroca" -> "berocca"
            "calpol_syrup" -> "calpol"
            "canvex_v" -> "canvexv"
            "compral_tablets" -> "compral"
            "coryx" -> "coryx"
            "deep_heat" -> "deepheat"
            "linctagon_tablets" -> "linctagontablets"
            "med_lemon" -> "medlemon"
            "panado_effervescent" -> "panadoeffervescent"
            "panado_tablets" -> "panadotablets"
            "rennie" -> "rennie"
            "sinutab_nasal_spray" -> "sinutabnasalspray"
            "slow_mag_capsules" -> "slowmag"
            "vicks_cough_syrup" -> "vickssyrup"
            else -> ""
        }

        if (drawableName.isBlank()) {
            return 0
        }

        return context.resources.getIdentifier(
            drawableName,
            "drawable",
            context.packageName
        )
    }
}