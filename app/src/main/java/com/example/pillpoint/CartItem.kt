package com.example.pillpoint

import android.content.Context
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
    // If the product already exists, increase its quantity.
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
                "productId" to item.id,
                "name" to item.name,
                "price" to item.price,
                "quantity" to newQuantity,
                "updatedAt" to FieldValue.serverTimestamp()
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

        db.collection("cart")
            .whereEqualTo("userId", user.uid)
            .get()
            .addOnSuccessListener { documents ->

                val loadedItems = documents.documents.mapNotNull { document ->
                    val productId =
                        document.getString("productId")
                            ?: document.getString("id")
                            ?: return@mapNotNull null

                    val name = document.getString("name") ?: ""
                    val price = document.getDouble("price") ?: 0.0
                    val quantity =
                        document.getLong("quantity")?.toInt() ?: 1

                    CartItem(
                        id = productId,
                        name = name,
                        price = price,
                        imageResId = getImageResource(context, productId),
                        quantity = quantity
                    )
                }

                items.clear()
                items.addAll(loadedItems)

                onSuccess(items.toList())
            }
            .addOnFailureListener { exception ->
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

    // Clear all cart items belonging to the current user.
    fun clearCart(
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val user = auth.currentUser

        if (user == null) {
            onError("Please log in to clear your cart.")
            return
        }

        db.collection("cart")
            .whereEqualTo("userId", user.uid)
            .get()
            .addOnSuccessListener { documents ->

                val batch = db.batch()

                for (document in documents.documents) {
                    batch.delete(document.reference)
                }

                batch.commit()
                    .addOnSuccessListener {
                        items.clear()
                        onSuccess()
                    }
                    .addOnFailureListener { exception ->
                        onError(
                            exception.message ?: "Could not clear your cart."
                        )
                    }
            }
            .addOnFailureListener { exception ->
                onError(
                    exception.message ?: "Could not retrieve your cart."
                )
            }
    }

    // Map each Firestore product ID to the drawable image in the app.
    private fun getImageResource(
        context: Context,
        productId: String
    ): Int {
        val drawableName = when (productId) {
            "allergex_syrup" -> "allergexsyrup"
            "allergex_tablets" -> "allergextablets"
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