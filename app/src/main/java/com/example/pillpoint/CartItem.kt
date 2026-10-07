package com.example.pillpoint

// Data model representing a single item in the cart
data class CartItem(
    val id: String,
    val name: String,
    val price: Double,
    val imageResId: Int,
    var quantity: Int = 1
)

// Shared Repository Singleton to store cart items in memory across screens
object CartRepository {
    val items = mutableListOf<CartItem>()

    fun addItem(item: CartItem) {
        val existingItem = items.find { it.id == item.id }
        if (existingItem != null) {
            existingItem.quantity += 1
        } else {
            items.add(item)
        }
    }

    fun removeItem(item: CartItem) {
        items.remove(item)
    }

    fun getSubtotal(): Double {
        return items.sumOf { it.price * it.quantity }
    }

    fun clearCart() {
        items.clear()
    }
}