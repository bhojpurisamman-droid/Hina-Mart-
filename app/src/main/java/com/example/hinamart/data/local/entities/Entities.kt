package com.example.hinamart.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.example.hinamart.data.model.OrderItem
import com.example.hinamart.data.model.OrderStatus
import org.json.JSONArray
import org.json.JSONObject

class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        if (value == null) return ""
        val jsonArray = JSONArray()
        value.forEach { jsonArray.put(it) }
        return jsonArray.toString()
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrEmpty()) return emptyList()
        val list = mutableListOf<String>()
        try {
            val jsonArray = JSONArray(value)
            for (i in 0 until jsonArray.length()) {
                list.add(jsonArray.getString(i))
            }
        } catch (e: Exception) {
            // fallback for comma separated
            return value.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        }
        return list
    }

    @TypeConverter
    fun fromOrderItemList(items: List<OrderItem>?): String {
        if (items == null) return "[]"
        val array = JSONArray()
        for (item in items) {
            val obj = JSONObject().apply {
                put("productId", item.productId)
                put("productName", item.productName)
                put("price", item.price)
                put("quantity", item.quantity)
                put("imageUrl", item.imageUrl)
                put("unit", item.unit)
            }
            array.put(obj)
        }
        return array.toString()
    }

    @TypeConverter
    fun toOrderItemList(value: String?): List<OrderItem> {
        if (value.isNullOrEmpty()) return emptyList()
        val list = mutableListOf<OrderItem>()
        try {
            val array = JSONArray(value)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    OrderItem(
                        productId = obj.optLong("productId"),
                        productName = obj.optString("productName"),
                        price = obj.optDouble("price"),
                        quantity = obj.optInt("quantity", 1),
                        imageUrl = obj.optString("imageUrl"),
                        unit = obj.optString("unit", "1 pc")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    @TypeConverter
    fun fromOrderStatus(status: OrderStatus): String = status.name

    @TypeConverter
    fun toOrderStatus(value: String): OrderStatus = try {
        OrderStatus.valueOf(value)
    } catch (e: Exception) {
        OrderStatus.PLACED
    }
}

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String,
    val category: String,
    val price: Double,
    val originalPrice: Double,
    val discountPercent: Int,
    val unit: String,
    val rating: Float,
    val reviewCount: Int,
    val imageUrl: String,
    val inStock: Boolean,
    val stockQuantity: Int,
    val isFeatured: Boolean,
    val isDealOfTheDay: Boolean,
    val tags: List<String>
)

@Entity(tableName = "cart_items")
data class CartEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val quantity: Int,
    val addedTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
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
    val createdAt: Long,
    val estimatedDelivery: String,
    val trackingNotes: String
)

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fullName: String,
    val email: String,
    val passwordHash: String,
    val phone: String,
    val address: String,
    val city: String,
    val zipCode: String,
    val role: String = "CUSTOMER", // CUSTOMER or ADMIN
    val avatarUrl: String = "",
    val isLoggedIn: Boolean = false
)

@Entity(tableName = "coupons")
data class CouponEntity(
    @PrimaryKey val code: String,
    val discountPercentage: Int,
    val maxDiscount: Double,
    val minOrderValue: Double,
    val description: String,
    val isActive: Boolean = true
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val message: String,
    val type: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val orderId: Long? = null
)

@Entity(tableName = "reviews")
data class ReviewEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val userName: String,
    val rating: Float,
    val comment: String,
    val date: String
)
