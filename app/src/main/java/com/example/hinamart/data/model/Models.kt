package com.example.hinamart.data.model

data class Product(
    val id: Long = 0,
    val name: String,
    val description: String,
    val category: String,
    val price: Double,
    val originalPrice: Double,
    val discountPercent: Int = if (originalPrice > price) (((originalPrice - price) / originalPrice) * 100).toInt() else 0,
    val unit: String = "1 pc",
    val rating: Float = 4.5f,
    val reviewCount: Int = 12,
    val imageUrl: String,
    val inStock: Boolean = true,
    val stockQuantity: Int = 50,
    val isFeatured: Boolean = false,
    val isDealOfTheDay: Boolean = false,
    val tags: List<String> = emptyList()
)

data class Category(
    val id: String,
    val name: String,
    val icon: String, // Icon identifier
    val description: String,
    val itemCount: Int = 0
)

data class CartItem(
    val id: Long = 0,
    val product: Product,
    val quantity: Int = 1,
    val addedTimestamp: Long = System.currentTimeMillis()
)

data class User(
    val id: Long = 0,
    val fullName: String,
    val email: String,
    val phone: String = "",
    val address: String = "",
    val city: String = "",
    val zipCode: String = "",
    val role: String = "CUSTOMER", // CUSTOMER or ADMIN
    val avatarUrl: String = ""
) {
    val isAdmin: Boolean
        get() = role.equals("ADMIN", ignoreCase = true)
}

enum class OrderStatus(val displayName: String, val colorHex: Long) {
    PLACED("Order Placed", 0xFF1E88E5),
    CONFIRMED("Confirmed", 0xFF00897B),
    SHIPPED("Shipped", 0xFFFB8C00),
    OUT_FOR_DELIVERY("Out for Delivery", 0xFF8E24AA),
    DELIVERED("Delivered", 0xFF2E7D32),
    CANCELLED("Cancelled", 0xFFC62828)
}

data class OrderItem(
    val productId: Long,
    val productName: String,
    val price: Double,
    val quantity: Int,
    val imageUrl: String,
    val unit: String
)

data class Order(
    val id: Long = 0,
    val orderNumber: String,
    val userId: Long,
    val userEmail: String,
    val userName: String,
    val userPhone: String,
    val deliveryAddress: String,
    val items: List<OrderItem>,
    val subtotal: Double,
    val discount: Double,
    val deliveryFee: Double,
    val tax: Double,
    val total: Double,
    val paymentMethod: String,
    val paymentStatus: String,
    val status: OrderStatus,
    val createdAt: Long = System.currentTimeMillis(),
    val estimatedDelivery: String = "Within 24-48 Hours",
    val trackingNotes: String = "Your order is being prepared with care.",
    val couponCode: String? = null
) {
    val phone: String
        get() = userPhone
}

data class Coupon(
    val code: String,
    val discountPercentage: Int,
    val maxDiscount: Double = 50.0,
    val minOrderValue: Double = 0.0,
    val description: String = "",
    val isActive: Boolean = true
) {
    val id: String get() = code
    val minOrderAmount: Double get() = minOrderValue
}

data class Review(
    val id: Long = 0,
    val productId: Long,
    val userName: String,
    val rating: Float,
    val comment: String,
    val date: String
)

data class NotificationItem(
    val id: Long = 0,
    val title: String,
    val message: String,
    val type: String, // "ORDER", "PROMO", "SYSTEM"
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val orderId: Long? = null
) {
    val createdAt: Long
        get() = timestamp
}
