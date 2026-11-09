package com.example.hinamart.data.repository

import android.content.Context
import com.example.hinamart.data.local.AppDatabase
import com.example.hinamart.data.local.entities.*
import com.example.hinamart.data.model.*
import com.example.hinamart.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HinaMartRepository(
    private val database: AppDatabase,
    private val context: Context
) {
    private val productDao = database.productDao()
    private val cartDao = database.cartDao()
    private val orderDao = database.orderDao()
    private val userDao = database.userDao()
    private val couponDao = database.couponDao()
    private val notificationDao = database.notificationDao()
    private val reviewDao = database.reviewDao()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            seedInitialDataIfNeeded()
        }
    }

    // --- Products ---
    fun getAllProducts(): Flow<List<Product>> {
        return productDao.getAllProducts().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    suspend fun getProductById(id: Long): Product? {
        return withContext(Dispatchers.IO) {
            productDao.getProductById(id)?.toDomainModel()
        }
    }

    fun getProductsByCategory(category: String): Flow<List<Product>> {
        return productDao.getProductsByCategory(category).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    fun getFeaturedProducts(): Flow<List<Product>> {
        return productDao.getFeaturedProducts().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    fun getDealOfTheDayProducts(): Flow<List<Product>> {
        return productDao.getDealOfTheDayProducts().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    fun searchProducts(query: String): Flow<List<Product>> {
        return productDao.searchProducts(query).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    fun getAllCategories(): Flow<List<Category>> {
        return productDao.getAllProducts().map { products ->
            val grouped = products.groupBy { it.category }
            val categoriesList = listOf(
                Category("all", "All Products", "apps", "Browse everything", products.size),
                Category("Fresh Fruits", "Fresh Fruits", "eco", "Crisp, organic orchard fruits", grouped["Fresh Fruits"]?.size ?: 0),
                Category("Vegetables", "Vegetables", "spa", "Farm-fresh greens and veggies", grouped["Vegetables"]?.size ?: 0),
                Category("Dairy & Bakery", "Dairy & Bakery", "cake", "Milk, artisan bread, cheeses", grouped["Dairy & Bakery"]?.size ?: 0),
                Category("Beverages", "Beverages", "local_cafe", "Cold pressed juices, tea & coffee", grouped["Beverages"]?.size ?: 0),
                Category("Pantry & Staples", "Pantry & Staples", "shopping_basket", "Grains, oils, organic pulses", grouped["Pantry & Staples"]?.size ?: 0),
                Category("Snacks & Treats", "Snacks & Treats", "fastfood", "Artisanal chips, dark chocolate, nuts", grouped["Snacks & Treats"]?.size ?: 0),
                Category("Health & Beauty", "Health & Beauty", "favorite", "Natural skincare & wellness", grouped["Health & Beauty"]?.size ?: 0),
                Category("Smart Living", "Smart Living", "devices", "Modern gadgets & home essentials", grouped["Smart Living"]?.size ?: 0)
            )
            categoriesList
        }
    }

    suspend fun saveProduct(product: Product): Long {
        return withContext(Dispatchers.IO) {
            val entity = product.toEntity()
            if (product.id == 0L) {
                productDao.insertProduct(entity)
            } else {
                productDao.updateProduct(entity)
                product.id
            }
        }
    }

    suspend fun deleteProduct(productId: Long) {
        withContext(Dispatchers.IO) {
            productDao.deleteProductById(productId)
            cartDao.deleteItem(productId)
        }
    }

    // --- Cart ---
    fun getCartItems(): Flow<List<CartItem>> {
        return combine(cartDao.getCartItems(), productDao.getAllProducts()) { cartEntities, productEntities ->
            val productMap = productEntities.associateBy { it.id }
            cartEntities.mapNotNull { cartEntity ->
                val productEntity = productMap[cartEntity.productId]
                if (productEntity != null) {
                    CartItem(
                        id = cartEntity.id,
                        product = productEntity.toDomainModel(),
                        quantity = cartEntity.quantity,
                        addedTimestamp = cartEntity.addedTimestamp
                    )
                } else null
            }
        }
    }

    suspend fun addToCart(productId: Long, quantity: Int = 1) {
        withContext(Dispatchers.IO) {
            val existing = cartDao.getCartItemByProduct(productId)
            if (existing != null) {
                cartDao.updateQuantity(productId, existing.quantity + quantity)
            } else {
                cartDao.insertOrUpdate(
                    CartEntity(
                        productId = productId,
                        quantity = quantity,
                        addedTimestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    suspend fun updateCartQuantity(productId: Long, quantity: Int) {
        withContext(Dispatchers.IO) {
            if (quantity <= 0) {
                cartDao.deleteItem(productId)
            } else {
                cartDao.updateQuantity(productId, quantity)
            }
        }
    }

    suspend fun removeFromCart(productId: Long) {
        withContext(Dispatchers.IO) {
            cartDao.deleteItem(productId)
        }
    }

    suspend fun clearCart() {
        withContext(Dispatchers.IO) {
            cartDao.clearCart()
        }
    }

    // --- Orders ---
    fun getAllOrders(): Flow<List<Order>> {
        return orderDao.getAllOrders().map { list -> list.map { it.toDomainModel() } }
    }

    fun getOrdersForUser(userId: Long): Flow<List<Order>> {
        return orderDao.getOrdersForUser(userId).map { list -> list.map { it.toDomainModel() } }
    }

    suspend fun getOrderById(orderId: Long): Order? {
        return withContext(Dispatchers.IO) {
            orderDao.getOrderById(orderId)?.toDomainModel()
        }
    }

    suspend fun placeOrder(
        user: User,
        cartItems: List<CartItem>,
        coupon: Coupon?,
        paymentMethod: String,
        deliveryAddress: String,
        phone: String,
        estimatedDelivery: String
    ): Order {
        return withContext(Dispatchers.IO) {
            val subtotal = cartItems.sumOf { it.product.price * it.quantity }
            var discount = 0.0
            if (coupon != null) {
                val calculated = (subtotal * coupon.discountPercentage) / 100.0
                discount = calculated.coerceAtMost(coupon.maxDiscount)
            }
            val deliveryFee = if (subtotal - discount >= 499.0 || coupon?.code == "FREESHIP") 0.0 else 49.0
            val tax = ((subtotal - discount) * 0.05).coerceAtLeast(0.0)
            val total = ((subtotal - discount) + deliveryFee + tax).coerceAtLeast(0.0)

            val orderNumber = "HN-" + (100000..999999).random()

            val orderEntity = OrderEntity(
                orderNumber = orderNumber,
                userId = user.id,
                userEmail = user.email,
                userName = user.fullName,
                userPhone = phone.ifBlank { user.phone },
                deliveryAddress = deliveryAddress.ifBlank { user.address },
                items = cartItems.map {
                    OrderItem(
                        productId = it.product.id,
                        productName = it.product.name,
                        price = it.product.price,
                        quantity = it.quantity,
                        imageUrl = it.product.imageUrl,
                        unit = it.product.unit
                    )
                },
                subtotal = subtotal,
                discount = discount,
                deliveryFee = deliveryFee,
                tax = tax,
                total = total,
                paymentMethod = paymentMethod,
                paymentStatus = if (paymentMethod == "Cash on Delivery") "Pending (Pay on Delivery)" else "Paid",
                status = OrderStatus.PLACED,
                createdAt = System.currentTimeMillis(),
                estimatedDelivery = estimatedDelivery,
                trackingNotes = "Order received and verified. Packing team is preparing fresh items."
            )

            val orderId = orderDao.insertOrder(orderEntity)
            cartDao.clearCart()

            // Create notification for the user
            val notification = NotificationItem(
                title = "Order Confirmed #$orderNumber",
                message = "Thank you for shopping at Hina Mart! Your order of ₹${String.format("%.0f", total)} is being processed.",
                type = "ORDER",
                orderId = orderId
            )
            addNotification(notification)

            orderEntity.copy(id = orderId).toDomainModel()
        }
    }

    suspend fun updateOrderStatus(orderId: Long, status: OrderStatus, notes: String = "") {
        withContext(Dispatchers.IO) {
            val order = orderDao.getOrderById(orderId)
            val updatedNotes = if (notes.isNotBlank()) notes else when (status) {
                OrderStatus.PLACED -> "Order received and queued for dispatch."
                OrderStatus.CONFIRMED -> "Your order is confirmed and packaging is underway."
                OrderStatus.SHIPPED -> "Items are on the delivery truck with our courier."
                OrderStatus.OUT_FOR_DELIVERY -> "Courier is in your area and approaching your doorstep."
                OrderStatus.DELIVERED -> "Delivered safely! Thank you for ordering with Hina Mart."
                OrderStatus.CANCELLED -> "Order has been cancelled. Any payment will be refunded."
            }

            orderDao.updateOrderStatus(orderId, status, updatedNotes)

            if (order != null) {
                val notification = NotificationItem(
                    title = "Order Update #${order.orderNumber}",
                    message = "Status changed to ${status.displayName}: $updatedNotes",
                    type = "ORDER",
                    orderId = orderId
                )
                addNotification(notification)
            }
        }
    }

    // --- Users & Authentication ---
    fun getActiveUser(): Flow<User?> {
        return userDao.getActiveUser().map { it?.toDomainModel() }
    }

    suspend fun login(email: String, password: String):Result<User> {
        return withContext(Dispatchers.IO) {
            val user = userDao.getUserByEmail(email.trim().lowercase())
            if (user != null && user.passwordHash == password) {
                userDao.logoutAllUsers()
                userDao.setLoggedIn(user.id)
                Result.success(user.toDomainModel())
            } else {
                Result.failure(Exception("Invalid email or password"))
            }
        }
    }

    suspend fun signup(fullName: String, email: String, password: String, phone: String, address: String): Result<User> {
        return withContext(Dispatchers.IO) {
            val normalizedEmail = email.trim().lowercase()
            val existing = userDao.getUserByEmail(normalizedEmail)
            if (existing != null) {
                return@withContext Result.failure(Exception("An account with this email already exists"))
            }

            val newUser = UserEntity(
                fullName = fullName.trim(),
                email = normalizedEmail,
                passwordHash = password,
                phone = phone.trim(),
                address = address.trim(),
                city = "",
                zipCode = "",
                role = "CUSTOMER",
                isLoggedIn = true
            )
            userDao.logoutAllUsers()
            val id = userDao.insertUser(newUser)

            // Welcome notification
            addNotification(
                NotificationItem(
                    title = "Welcome to Hina Mart, $fullName!",
                    message = "Enjoy free shipping and exclusive discounts with code WELCOME50.",
                    type = "PROMO"
                )
            )

            Result.success(newUser.copy(id = id).toDomainModel())
        }
    }

    suspend fun logout() {
        withContext(Dispatchers.IO) {
            userDao.logoutAllUsers()
        }
    }

    suspend fun updateUserProfile(user: User) {
        withContext(Dispatchers.IO) {
            val existing = userDao.getUserById(user.id)
            if (existing != null) {
                userDao.updateUser(
                    existing.copy(
                        fullName = user.fullName,
                        phone = user.phone,
                        address = user.address,
                        city = user.city,
                        zipCode = user.zipCode,
                        role = user.role
                    )
                )
            }
        }
    }

    // --- Coupons ---
    fun getActiveCoupons(): Flow<List<Coupon>> {
        return couponDao.getActiveCoupons().map { list -> list.map { it.toDomainModel() } }
    }

    suspend fun validateCoupon(code: String, subtotal: Double): Result<Coupon> {
        return withContext(Dispatchers.IO) {
            val couponEntity = couponDao.getCoupon(code.trim().uppercase())
            if (couponEntity == null) {
                Result.failure(Exception("Invalid or expired coupon code"))
            } else if (subtotal < couponEntity.minOrderValue) {
                Result.failure(Exception("Minimum order value for ${couponEntity.code} is ₹${String.format("%.0f", couponEntity.minOrderValue)}"))
            } else {
                Result.success(couponEntity.toDomainModel())
            }
        }
    }

    suspend fun createCoupon(coupon: Coupon) {
        withContext(Dispatchers.IO) {
            couponDao.insertCoupon(
                CouponEntity(
                    code = coupon.code.uppercase().trim(),
                    discountPercentage = coupon.discountPercentage,
                    maxDiscount = coupon.maxDiscount,
                    minOrderValue = coupon.minOrderValue,
                    description = coupon.description,
                    isActive = coupon.isActive
                )
            )
        }
    }

    // --- Notifications ---
    fun getAllNotifications(): Flow<List<NotificationItem>> {
        return notificationDao.getAllNotifications().map { list -> list.map { it.toDomainModel() } }
    }

    fun getUnreadNotificationsCount(): Flow<Int> {
        return notificationDao.getUnreadCount()
    }

    suspend fun markNotificationAsRead(id: Long) {
        withContext(Dispatchers.IO) {
            notificationDao.markAsRead(id)
        }
    }

    suspend fun markAllNotificationsAsRead() {
        withContext(Dispatchers.IO) {
            notificationDao.markAllAsRead()
        }
    }

    suspend fun addNotification(notification: NotificationItem) {
        withContext(Dispatchers.IO) {
            val entity = NotificationEntity(
                title = notification.title,
                message = notification.message,
                type = notification.type,
                timestamp = System.currentTimeMillis(),
                isRead = false,
                orderId = notification.orderId
            )
            val id = notificationDao.insertNotification(entity)
            val created = notification.copy(id = id)
            NotificationHelper.showNotification(context, created)
        }
    }

    // --- Reviews ---
    fun getReviewsForProduct(productId: Long): Flow<List<Review>> {
        return reviewDao.getReviewsForProduct(productId).map { list -> list.map { it.toDomainModel() } }
    }

    suspend fun addReview(productId: Long, userName: String, rating: Float, comment: String) {
        withContext(Dispatchers.IO) {
            val reviewEntity = ReviewEntity(
                productId = productId,
                userName = userName,
                rating = rating,
                comment = comment,
                date = "Today"
            )
            reviewDao.insertReview(reviewEntity)

            // Update product rating average
            val product = productDao.getProductById(productId)
            if (product != null) {
                val newCount = product.reviewCount + 1
                val newRating = ((product.rating * product.reviewCount) + rating) / newCount
                productDao.updateProduct(
                    product.copy(
                        reviewCount = newCount,
                        rating = String.format("%.1f", newRating).toFloat()
                    )
                )
            }
        }
    }

    // --- Database Seeding ---
    private suspend fun seedInitialDataIfNeeded() {
        if (productDao.getProductCount() > 0) return

        // Seed Users
        val adminUser = UserEntity(
            fullName = "Hina Mart Manager",
            email = "admin@hinamart.com",
            passwordHash = "admin123",
            phone = "+91 98765 43210",
            address = "Hina Mart Superstore, 100 MG Road",
            city = "Bengaluru",
            zipCode = "560001",
            role = "ADMIN",
            isLoggedIn = false
        )
        val defaultCustomer = UserEntity(
            fullName = "Ananya Sharma",
            email = "customer@hinamart.com",
            passwordHash = "123456",
            phone = "+91 98450 12345",
            address = "Flat 402, Sunshine Heights, 12th Main Indiranagar",
            city = "Bengaluru",
            zipCode = "560038",
            role = "CUSTOMER",
            isLoggedIn = true // Logged in by default for smooth demo experience!
        )
        val adminId = userDao.insertUser(adminUser)
        val customerId = userDao.insertUser(defaultCustomer)

        // Seed Coupons
        val initialCoupons = listOf(
            CouponEntity("WELCOME50", 50, 200.0, 299.0, "50% off up to ₹200 on your first grocery order"),
            CouponEntity("HINAMART20", 20, 150.0, 399.0, "20% off all organic and fresh produce items"),
            CouponEntity("FREESHIP", 100, 49.0, 199.0, "Free standard doorstep delivery on orders above ₹199"),
            CouponEntity("MEGAFEST30", 30, 500.0, 999.0, "30% off big basket purchases for the seasonal festival")
        )
        couponDao.insertCoupons(initialCoupons)

        // Seed Products
        val products = listOf(
            ProductEntity(
                name = "Organic Honeycrisp Apples",
                description = "Extra crisp, sweet, and juicy handpicked Honeycrisp apples grown in certified organic orchards. Rich in dietary fiber and vitamin C.",
                category = "Fresh Fruits",
                price = 199.0,
                originalPrice = 249.0,
                discountPercent = 20,
                unit = "1 kg (approx 4-5 pcs)",
                rating = 4.8f,
                reviewCount = 142,
                imageUrl = "https://images.unsplash.com/photo-1560806887-1e4cd0b6cbd6?w=600&auto=format&fit=crop&q=80",
                inStock = true,
                stockQuantity = 85,
                isFeatured = true,
                isDealOfTheDay = true,
                tags = listOf("Organic", "Fresh", "Fruit", "Top Seller")
            ),
            ProductEntity(
                name = "Premium Alphonso Mangoes",
                description = "King of mangoes (Ratnagiri Alphonso) with golden buttery pulp, heavenly aroma, and unmatched sweetness. Grade A quality.",
                category = "Fresh Fruits",
                price = 499.0,
                originalPrice = 699.0,
                discountPercent = 28,
                unit = "Box of 6 pcs",
                rating = 4.9f,
                reviewCount = 89,
                imageUrl = "https://images.unsplash.com/photo-1553279768-865429fa0078?w=600&auto=format&fit=crop&q=80",
                inStock = true,
                stockQuantity = 40,
                isFeatured = true,
                isDealOfTheDay = true,
                tags = listOf("Seasonal", "Exotic", "Sweet")
            ),
            ProductEntity(
                name = "Fresh Organic Baby Spinach (Palak)",
                description = "Tender, hydro-washed fresh spinach leaves packed with natural iron, folate, and vitamins. Ideal for dal, palak paneer, and salads.",
                category = "Vegetables",
                price = 49.0,
                originalPrice = 69.0,
                discountPercent = 29,
                unit = "250g bunch",
                rating = 4.7f,
                reviewCount = 95,
                imageUrl = "https://images.unsplash.com/photo-1576045057995-568f588f82fb?w=600&auto=format&fit=crop&q=80",
                inStock = true,
                stockQuantity = 110,
                isFeatured = false,
                isDealOfTheDay = false,
                tags = listOf("Organic", "Greens", "Healthy")
            ),
            ProductEntity(
                name = "Hass Avocados (Ready to Eat)",
                description = "Creamy, nutrient-dense imported Hass avocados with perfectly dark pebbly skin. Rich in heart-healthy monounsaturated fats.",
                category = "Fresh Fruits",
                price = 299.0,
                originalPrice = 399.0,
                discountPercent = 25,
                unit = "Pack of 2",
                rating = 4.6f,
                reviewCount = 64,
                imageUrl = "https://images.unsplash.com/photo-1523049673857-eb18f1d7b578?w=600&auto=format&fit=crop&q=80",
                inStock = true,
                stockQuantity = 65,
                isFeatured = true,
                isDealOfTheDay = false,
                tags = listOf("Keto", "Superfood", "Avocado")
            ),
            ProductEntity(
                name = "Farm Fresh Vine Tomatoes",
                description = "Plump, sun-drenched Indian red tomatoes with firm flesh. Excellent for curries, rasam, homemade gravies, and fresh salads.",
                category = "Vegetables",
                price = 39.0,
                originalPrice = 55.0,
                discountPercent = 29,
                unit = "1 kg",
                rating = 4.5f,
                reviewCount = 78,
                imageUrl = "https://images.unsplash.com/photo-1592924357228-91a4daadcfea?w=600&auto=format&fit=crop&q=80",
                inStock = true,
                stockQuantity = 120,
                isFeatured = false,
                isDealOfTheDay = false,
                tags = listOf("Fresh", "Cooking", "Salad")
            ),
            ProductEntity(
                name = "Farm Fresh Whole Pure Milk",
                description = "Pure, fresh pasteurized cow milk with natural cream. Antibiotic-free, rich taste, fortified with calcium and Vitamin D.",
                category = "Dairy & Bakery",
                price = 68.0,
                originalPrice = 75.0,
                discountPercent = 9,
                unit = "1 Litre Pouch",
                rating = 4.9f,
                reviewCount = 230,
                imageUrl = "https://images.unsplash.com/photo-1563636619-e9143da7973b?w=600&auto=format&fit=crop&q=80",
                inStock = true,
                stockQuantity = 75,
                isFeatured = true,
                isDealOfTheDay = false,
                tags = listOf("Dairy", "Pure Milk", "Essential")
            ),
            ProductEntity(
                name = "Artisanal Rustic Sourdough Loaf",
                description = "Naturally fermented 36-hour slow proved sourdough with a crackly golden crust and airy, chewy crumb. Baked fresh every morning.",
                category = "Dairy & Bakery",
                price = 149.0,
                originalPrice = 199.0,
                discountPercent = 25,
                unit = "500g Loaf",
                rating = 4.8f,
                reviewCount = 112,
                imageUrl = "https://images.unsplash.com/photo-1589367920969-ab8e050bbb04?w=600&auto=format&fit=crop&q=80",
                inStock = true,
                stockQuantity = 35,
                isFeatured = true,
                isDealOfTheDay = true,
                tags = listOf("Freshly Baked", "Artisan", "Zero Preservatives")
            ),
            ProductEntity(
                name = "Greek Style Whole Milk Dahi / Yogurt",
                description = "Authentic strained thick Greek yogurt with high natural protein. Velvety smooth, no added sugar, rich in live gut-friendly probiotics.",
                category = "Dairy & Bakery",
                price = 120.0,
                originalPrice = 150.0,
                discountPercent = 20,
                unit = "400g Tub",
                rating = 4.7f,
                reviewCount = 86,
                imageUrl = "https://images.unsplash.com/photo-1488477181946-6428a0291777?w=600&auto=format&fit=crop&q=80",
                inStock = true,
                stockQuantity = 50,
                isFeatured = false,
                isDealOfTheDay = false,
                tags = listOf("High Protein", "Probiotic", "Healthy")
            ),
            ProductEntity(
                name = "Cold Pressed Valencia Orange Juice",
                description = "100% pure Valencia oranges, non-pasteurized cold-pressed nectar with juicy pulp. Never from concentrate, zero added sugar.",
                category = "Beverages",
                price = 149.0,
                originalPrice = 189.0,
                discountPercent = 21,
                unit = "1 Litre Bottle",
                rating = 4.9f,
                reviewCount = 175,
                imageUrl = "https://images.unsplash.com/photo-1613478223719-2ab802602423?w=600&auto=format&fit=crop&q=80",
                inStock = true,
                stockQuantity = 60,
                isFeatured = true,
                isDealOfTheDay = true,
                tags = listOf("Cold Pressed", "Vitamin C", "Refreshing")
            ),
            ProductEntity(
                name = "Premium Coorg Single-Origin Arabica Coffee",
                description = "Medium-dark roasted premium Arabica whole beans from Coorg estate. Rich notes of dark cocoa, roasted hazelnuts, and aromatic spices.",
                category = "Beverages",
                price = 499.0,
                originalPrice = 649.0,
                discountPercent = 23,
                unit = "250g Bag",
                rating = 4.9f,
                reviewCount = 205,
                imageUrl = "https://images.unsplash.com/photo-1559056199-641a0ac8b55e?w=600&auto=format&fit=crop&q=80",
                inStock = true,
                stockQuantity = 45,
                isFeatured = true,
                isDealOfTheDay = false,
                tags = listOf("Gourmet", "Coffee", "Specialty")
            ),
            ProductEntity(
                name = "Extra Virgin Cold Pressed Olive Oil",
                description = "Single estate cold extracted extra virgin olive oil. Acidity below 0.3%, rich fruity aroma with a delicate peppery finish.",
                category = "Pantry & Staples",
                price = 799.0,
                originalPrice = 999.0,
                discountPercent = 20,
                unit = "500 ml Glass Bottle",
                rating = 4.8f,
                reviewCount = 98,
                imageUrl = "https://images.unsplash.com/photo-1474979266404-7eaacbcd87c5?w=600&auto=format&fit=crop&q=80",
                inStock = true,
                stockQuantity = 40,
                isFeatured = false,
                isDealOfTheDay = false,
                tags = listOf("Mediterranean", "Keto", "Pantry")
            ),
            ProductEntity(
                name = "Organic Royal Basmati Rice (Aged 2 Years)",
                description = "Extra-long slender aromatic grains aged for 24 months. Fluffy, non-sticky texture with royal fragrance when cooked for biryani and pulao.",
                category = "Pantry & Staples",
                price = 349.0,
                originalPrice = 429.0,
                discountPercent = 18,
                unit = "5 kg Bag",
                rating = 4.7f,
                reviewCount = 132,
                imageUrl = "https://images.unsplash.com/photo-1586201375761-83865001e31c?w=600&auto=format&fit=crop&q=80",
                inStock = true,
                stockQuantity = 90,
                isFeatured = false,
                isDealOfTheDay = false,
                tags = listOf("Aged Rice", "Aromatic", "Pantry")
            ),
            ProductEntity(
                name = "Artisan Dark Chocolate (85% Cacao)",
                description = "Single-origin fine cocoa beans with subtle Madagascar vanilla notes. Fair-trade certified, pure organic, and vegan friendly.",
                category = "Snacks & Treats",
                price = 199.0,
                originalPrice = 249.0,
                discountPercent = 20,
                unit = "100g Bar",
                rating = 4.8f,
                reviewCount = 88,
                imageUrl = "https://images.unsplash.com/photo-1548907040-4baa42d10919?w=600&auto=format&fit=crop&q=80",
                inStock = true,
                stockQuantity = 150,
                isFeatured = true,
                isDealOfTheDay = false,
                tags = listOf("Fair Trade", "Dark Chocolate", "Antioxidants")
            ),
            ProductEntity(
                name = "Dry Roasted Gourmet Mixed Dry Fruits & Nuts",
                description = "Crunchy blend of premium California almonds, cashew nuts, pistachios, and walnuts, lightly dusted with Himalayan pink salt.",
                category = "Snacks & Treats",
                price = 399.0,
                originalPrice = 499.0,
                discountPercent = 20,
                unit = "250g Pouch",
                rating = 4.7f,
                reviewCount = 115,
                imageUrl = "https://images.unsplash.com/photo-1599599810769-bcde5a160d32?w=600&auto=format&fit=crop&q=80",
                inStock = true,
                stockQuantity = 80,
                isFeatured = false,
                isDealOfTheDay = true,
                tags = listOf("Heart Healthy", "High Protein", "Snack")
            ),
            ProductEntity(
                name = "Natural Soothing Aloe Vera Gel",
                description = "99% pure organic cold-pressed aloe vera gel. Calms irritation, hydrates skin deeply, and serves as a natural daily moisturizer.",
                category = "Health & Beauty",
                price = 249.0,
                originalPrice = 329.0,
                discountPercent = 24,
                unit = "200 ml Pump",
                rating = 4.6f,
                reviewCount = 57,
                imageUrl = "https://images.unsplash.com/photo-1596755094514-f87e34085b2c?w=600&auto=format&fit=crop&q=80",
                inStock = true,
                stockQuantity = 45,
                isFeatured = false,
                isDealOfTheDay = false,
                tags = listOf("Organic", "Skincare", "Cruelty Free")
            ),
            ProductEntity(
                name = "Hina Mart Smart LED Ambient Lamp",
                description = "Touch-controlled minimalist RGB warm lighting lamp with 16 million colors, voice assistant compatibility, and bedtime sleep timer.",
                category = "Smart Living",
                price = 1299.0,
                originalPrice = 1999.0,
                discountPercent = 35,
                unit = "1 Unit",
                rating = 4.9f,
                reviewCount = 160,
                imageUrl = "https://images.unsplash.com/photo-1507473885765-e6ed057f782c?w=600&auto=format&fit=crop&q=80",
                inStock = true,
                stockQuantity = 30,
                isFeatured = true,
                isDealOfTheDay = true,
                tags = listOf("Smart Home", "Aesthetic", "Best Seller")
            )
        )

        productDao.insertProducts(products)

        // Seed Sample Orders for Default Customer
        val pastOrder1 = OrderEntity(
            orderNumber = "HN-829104",
            userId = customerId,
            userEmail = "customer@hinamart.com",
            userName = "Ananya Sharma",
            userPhone = "+91 98450 12345",
            deliveryAddress = "Flat 402, Sunshine Heights, Indiranagar, Bengaluru 560038",
            items = listOf(
                OrderItem(1, "Organic Honeycrisp Apples", 199.0, 2, "https://images.unsplash.com/photo-1560806887-1e4cd0b6cbd6?w=600&auto=format&fit=crop&q=80", "1 kg"),
                OrderItem(6, "Farm Fresh Whole Pure Milk", 68.0, 1, "https://images.unsplash.com/photo-1563636619-e9143da7973b?w=600&auto=format&fit=crop&q=80", "1 Litre"),
                OrderItem(7, "Artisanal Rustic Sourdough Loaf", 149.0, 1, "https://images.unsplash.com/photo-1589367920969-ab8e050bbb04?w=600&auto=format&fit=crop&q=80", "500g Loaf")
            ),
            subtotal = 615.0,
            discount = 0.0,
            deliveryFee = 0.0,
            tax = 30.75,
            total = 645.75,
            paymentMethod = "UPI (Google Pay)",
            paymentStatus = "Paid",
            status = OrderStatus.DELIVERED,
            createdAt = System.currentTimeMillis() - 86400000L * 3,
            estimatedDelivery = "Delivered on Wednesday",
            trackingNotes = "Package handed directly to customer."
        )

        val activeOrder2 = OrderEntity(
            orderNumber = "HN-937218",
            userId = customerId,
            userEmail = "customer@hinamart.com",
            userName = "Ananya Sharma",
            userPhone = "+91 98450 12345",
            deliveryAddress = "Flat 402, Sunshine Heights, Indiranagar, Bengaluru 560038",
            items = listOf(
                OrderItem(2, "Premium Alphonso Mangoes", 499.0, 1, "https://images.unsplash.com/photo-1553279768-865429fa0078?w=600&auto=format&fit=crop&q=80", "Box of 6 pcs"),
                OrderItem(9, "Cold Pressed Valencia Orange Juice", 149.0, 2, "https://images.unsplash.com/photo-1613478223719-2ab802602423?w=600&auto=format&fit=crop&q=80", "1 Litre Bottle"),
                OrderItem(13, "Artisan Dark Chocolate (85% Cacao)", 199.0, 2, "https://images.unsplash.com/photo-1548907040-4baa42d10919?w=600&auto=format&fit=crop&q=80", "100g Bar")
            ),
            subtotal = 1195.0,
            discount = 150.0,
            deliveryFee = 0.0,
            tax = 52.25,
            total = 1097.25,
            paymentMethod = "Cash on Delivery",
            paymentStatus = "Pending",
            status = OrderStatus.OUT_FOR_DELIVERY,
            createdAt = System.currentTimeMillis() - 7200000L,
            estimatedDelivery = "Today, within 45 minutes",
            trackingNotes = "Delivery partner Rajesh is 1.5 km away."
        )

        val savedOrder1Id = orderDao.insertOrder(pastOrder1)
        val savedOrder2Id = orderDao.insertOrder(activeOrder2)

        // Seed Reviews
        val sampleReviews = listOf(
            ReviewEntity(productId = 1, userName = "Emily Watson", rating = 5f, comment = "So fresh and crisp! By far the best apples I've ordered online.", date = "2 days ago"),
            ReviewEntity(productId = 1, userName = "Marcus Chen", rating = 4.5f, comment = "Sweet flavor, perfectly packaged with protective netting.", date = "1 week ago"),
            ReviewEntity(productId = 2, userName = "Priya Sharma", rating = 5f, comment = "Authentic Alphonso flavor! Smelled wonderful the moment the box opened.", date = "3 days ago"),
            ReviewEntity(productId = 7, userName = "Liam O'Connor", rating = 5f, comment = "Crispy crust and beautiful sourdough tang. Toasts brilliantly.", date = "Yesterday")
        )
        reviewDao.insertReviews(sampleReviews)

        // Seed Notifications
        val initialNotifications = listOf(
            NotificationEntity(
                title = "Order Out for Delivery #HN-937218",
                message = "Your grocery package is with delivery partner Rajesh and scheduled to arrive in ~45 mins.",
                type = "ORDER",
                timestamp = System.currentTimeMillis() - 1800000L,
                isRead = false,
                orderId = savedOrder2Id
            ),
            NotificationEntity(
                title = "Flash Deal: 50% Off Mega Sale!",
                message = "Use code WELCOME50 for 50% discount on fresh organic staples and fresh fruits.",
                type = "PROMO",
                timestamp = System.currentTimeMillis() - 86400000L,
                isRead = false
            ),
            NotificationEntity(
                title = "Order Delivered #HN-829104",
                message = "Your past order has been successfully delivered. Tap to view invoice.",
                type = "ORDER",
                timestamp = System.currentTimeMillis() - 86400000L * 3,
                isRead = true,
                orderId = savedOrder1Id
            )
        )
        initialNotifications.forEach { notificationDao.insertNotification(it) }
    }

    // Mapping extension functions
    private fun ProductEntity.toDomainModel() = Product(
        id = id,
        name = name,
        description = description,
        category = category,
        price = price,
        originalPrice = originalPrice,
        discountPercent = discountPercent,
        unit = unit,
        rating = rating,
        reviewCount = reviewCount,
        imageUrl = imageUrl,
        inStock = inStock,
        stockQuantity = stockQuantity,
        isFeatured = isFeatured,
        isDealOfTheDay = isDealOfTheDay,
        tags = tags
    )

    private fun Product.toEntity() = ProductEntity(
        id = id,
        name = name,
        description = description,
        category = category,
        price = price,
        originalPrice = originalPrice,
        discountPercent = discountPercent,
        unit = unit,
        rating = rating,
        reviewCount = reviewCount,
        imageUrl = imageUrl,
        inStock = inStock,
        stockQuantity = stockQuantity,
        isFeatured = isFeatured,
        isDealOfTheDay = isDealOfTheDay,
        tags = tags
    )

    private fun OrderEntity.toDomainModel() = Order(
        id = id,
        orderNumber = orderNumber,
        userId = userId,
        userEmail = userEmail,
        userName = userName,
        userPhone = userPhone,
        deliveryAddress = deliveryAddress,
        items = items,
        subtotal = subtotal,
        discount = discount,
        deliveryFee = deliveryFee,
        tax = tax,
        total = total,
        paymentMethod = paymentMethod,
        paymentStatus = paymentStatus,
        status = status,
        createdAt = createdAt,
        estimatedDelivery = estimatedDelivery,
        trackingNotes = trackingNotes
    )

    private fun UserEntity.toDomainModel() = User(
        id = id,
        fullName = fullName,
        email = email,
        phone = phone,
        address = address,
        city = city,
        zipCode = zipCode,
        role = role,
        avatarUrl = avatarUrl
    )

    private fun CouponEntity.toDomainModel() = Coupon(
        code = code,
        discountPercentage = discountPercentage,
        maxDiscount = maxDiscount,
        minOrderValue = minOrderValue,
        description = description,
        isActive = isActive
    )

    private fun NotificationEntity.toDomainModel() = NotificationItem(
        id = id,
        title = title,
        message = message,
        type = type,
        timestamp = timestamp,
        isRead = isRead,
        orderId = orderId
    )

    private fun ReviewEntity.toDomainModel() = Review(
        id = id,
        productId = productId,
        userName = userName,
        rating = rating,
        comment = comment,
        date = date
    )
}
