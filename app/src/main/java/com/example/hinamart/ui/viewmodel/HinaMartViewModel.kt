package com.example.hinamart.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.hinamart.data.model.*
import com.example.hinamart.data.repository.HinaMartRepository
import com.example.hinamart.utils.NetworkObserver
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class SortOption(val title: String) {
    POPULAR("Most Popular"),
    PRICE_LOW_HIGH("Price: Low to High"),
    PRICE_HIGH_LOW("Price: High to Low"),
    RATING("Highest Rated"),
    DISCOUNT("Biggest Discount")
}

class HinaMartViewModel(
    private val repository: HinaMartRepository,
    private val networkObserver: NetworkObserver? = null
) : ViewModel() {

    // --- Network Connectivity Stream ---
    private val _manualOnlineCheck = MutableStateFlow<Boolean?>(null)

    val isOnline: StateFlow<Boolean> = if (networkObserver != null) {
        combine(networkObserver.observe(), _manualOnlineCheck) { observerState, manualState ->
            manualState ?: observerState
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = networkObserver.isCurrentlyOnline()
        )
    } else {
        MutableStateFlow(true).asStateFlow()
    }

    fun retryConnection() {
        viewModelScope.launch {
            if (networkObserver != null) {
                val current = networkObserver.isCurrentlyOnline()
                _manualOnlineCheck.value = current
                if (current) {
                    showSnackbar("Connected to Internet! Refreshing catalog...")
                } else {
                    showSnackbar("Still no internet connection. Please check Wi-Fi / Mobile data.")
                }
            }
        }
    }

    // --- State Streams ---
    val products = repository.getAllProducts().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val categories = repository.getAllCategories().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val featuredProducts = repository.getFeaturedProducts().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val dealOfTheDayProducts = repository.getDealOfTheDayProducts().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val cartItems = repository.getCartItems().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val activeUser = repository.getActiveUser().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val orders = repository.getAllOrders().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val notifications = repository.getAllNotifications().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val unreadNotificationsCount = repository.getUnreadNotificationsCount().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val activeCoupons = repository.getActiveCoupons().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // --- Filter & Search State ---
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("all")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _selectedSort = MutableStateFlow(SortOption.POPULAR)
    val selectedSort: StateFlow<SortOption> = _selectedSort.asStateFlow()

    val filteredProducts = combine(
        products,
        _searchQuery,
        _selectedCategory,
        _selectedSort
    ) { allProducts, query, category, sort ->
        var list = allProducts

        // Filter by category
        if (category != "all" && category.isNotBlank()) {
            list = list.filter { it.category.equals(category, ignoreCase = true) }
        }

        // Filter by search query
        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            list = list.filter {
                it.name.lowercase().contains(q) ||
                it.description.lowercase().contains(q) ||
                it.category.lowercase().contains(q) ||
                it.tags.any { tag -> tag.lowercase().contains(q) }
            }
        }

        // Sort
        when (sort) {
            SortOption.POPULAR -> list.sortedByDescending { it.isFeatured }
            SortOption.PRICE_LOW_HIGH -> list.sortedBy { it.price }
            SortOption.PRICE_HIGH_LOW -> list.sortedByDescending { it.price }
            SortOption.RATING -> list.sortedByDescending { it.rating }
            SortOption.DISCOUNT -> list.sortedByDescending { it.discountPercent }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // --- Cart & Coupon Management ---
    private val _appliedCoupon = MutableStateFlow<Coupon?>(null)
    val appliedCoupon: StateFlow<Coupon?> = _appliedCoupon.asStateFlow()

    private val _couponError = MutableStateFlow<String?>(null)
    val couponError: StateFlow<String?> = _couponError.asStateFlow()

    val cartSubtotal = cartItems.map { items ->
        items.sumOf { it.product.price * it.quantity }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val cartOriginalTotal = cartItems.map { items ->
        items.sumOf { it.product.originalPrice * it.quantity }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val cartDiscount = combine(cartSubtotal, appliedCoupon) { subtotal, coupon ->
        if (coupon != null) {
            val calculated = (subtotal * coupon.discountPercentage) / 100.0
            calculated.coerceAtMost(coupon.maxDiscount)
        } else {
            0.0
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val cartDeliveryFee = combine(cartSubtotal, cartDiscount, appliedCoupon) { subtotal, discount, coupon ->
        if (subtotal - discount >= 35.0 || coupon?.code == "FREESHIP" || subtotal == 0.0) 0.0 else 4.99
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val cartTax = combine(cartSubtotal, cartDiscount) { subtotal, discount ->
        ((subtotal - discount) * 0.05).coerceAtLeast(0.0)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val cartTotal = combine(cartSubtotal, cartDiscount, cartDeliveryFee, cartTax) { sub, disc, del, tax ->
        ((sub - disc) + del + tax).coerceAtLeast(0.0)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val cartTotalSavings = combine(cartOriginalTotal, cartSubtotal, cartDiscount) { orig, sub, disc ->
        (orig - sub + disc).coerceAtLeast(0.0)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // --- Selected Product & Order Details ---
    private val _selectedProduct = MutableStateFlow<Product?>(null)
    val selectedProduct: StateFlow<Product?> = _selectedProduct.asStateFlow()

    private val _selectedOrder = MutableStateFlow<Order?>(null)
    val selectedOrder: StateFlow<Order?> = _selectedOrder.asStateFlow()

    private val _productReviews = MutableStateFlow<List<Review>>(emptyList())
    val productReviews: StateFlow<List<Review>> = _productReviews.asStateFlow()

    // --- UI Transient States (Toasts / Messages) ---
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    fun showSnackbar(message: String) {
        _snackbarMessage.value = message
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    // --- Filter Setters ---
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun selectSort(sort: SortOption) {
        _selectedSort.value = sort
    }

    // --- Cart Actions ---
    fun addToCart(product: Product, quantity: Int = 1) {
        viewModelScope.launch {
            repository.addToCart(product.id, quantity)
            showSnackbar("Added ${product.name} to cart")
        }
    }

    fun updateCartQuantity(productId: Long, quantity: Int) {
        viewModelScope.launch {
            repository.updateCartQuantity(productId, quantity)
        }
    }

    fun removeFromCart(productId: Long) {
        viewModelScope.launch {
            repository.removeFromCart(productId)
            showSnackbar("Item removed from cart")
        }
    }

    fun clearCart() {
        viewModelScope.launch {
            repository.clearCart()
        }
    }

    fun applyCoupon(code: String) {
        viewModelScope.launch {
            _couponError.value = null
            val result = repository.validateCoupon(code, cartSubtotal.value)
            if (result.isSuccess) {
                _appliedCoupon.value = result.getOrNull()
                showSnackbar("Coupon '${code.uppercase()}' applied successfully!")
            } else {
                _couponError.value = result.exceptionOrNull()?.message ?: "Invalid coupon"
            }
        }
    }

    fun removeCoupon() {
        _appliedCoupon.value = null
        _couponError.value = null
        showSnackbar("Coupon removed")
    }

    // --- Detail Selection ---
    fun selectProduct(product: Product) {
        _selectedProduct.value = product
        viewModelScope.launch {
            repository.getReviewsForProduct(product.id).collect {
                _productReviews.value = it
            }
        }
    }

    fun selectOrder(order: Order) {
        _selectedOrder.value = order
    }

    fun addReview(productId: Long, rating: Float, comment: String) {
        viewModelScope.launch {
            val user = activeUser.value
            val userName = user?.fullName ?: "Verified Buyer"
            repository.addReview(productId, userName, rating, comment)
            showSnackbar("Review submitted! Thank you.")
            // Refresh selected product
            val updated = repository.getProductById(productId)
            if (updated != null) {
                _selectedProduct.value = updated
            }
        }
    }

    // --- Checkout Flow ---
    suspend fun placeOrder(
        paymentMethod: String,
        address: String,
        phone: String,
        deliverySpeed: String
    ): Result<Order> {
        val user = activeUser.value
        val items = cartItems.value
        if (items.isEmpty()) {
            return Result.failure(Exception("Your cart is empty!"))
        }

        val effectiveUser = user ?: User(
            id = 1,
            fullName = "Guest Shopper",
            email = "guest@hinamart.com",
            phone = phone,
            address = address
        )

        return try {
            val order = repository.placeOrder(
                user = effectiveUser,
                cartItems = items,
                coupon = _appliedCoupon.value,
                paymentMethod = paymentMethod,
                deliveryAddress = address,
                phone = phone,
                estimatedDelivery = deliverySpeed
            )
            _appliedCoupon.value = null
            _selectedOrder.value = order
            Result.success(order)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Order Status Management ---
    fun updateOrderStatus(orderId: Long, status: OrderStatus, notes: String = "") {
        viewModelScope.launch {
            repository.updateOrderStatus(orderId, status, notes)
            showSnackbar("Order status updated to ${status.displayName}")
            // Update selected order if viewing
            if (_selectedOrder.value?.id == orderId) {
                val updated = repository.getOrderById(orderId)
                if (updated != null) {
                    _selectedOrder.value = updated
                }
            }
        }
    }

    fun cancelOrder(orderId: Long) {
        viewModelScope.launch {
            repository.updateOrderStatus(
                orderId,
                OrderStatus.CANCELLED,
                "Order was cancelled by the customer."
            )
            showSnackbar("Order cancelled successfully")
        }
    }

    // --- Auth Actions ---
    fun login(email: String, pass: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = repository.login(email, pass)
            if (result.isSuccess) {
                showSnackbar("Welcome back, ${result.getOrNull()?.fullName}!")
                onResult(true, null)
            } else {
                onResult(false, result.exceptionOrNull()?.message ?: "Login failed")
            }
        }
    }

    fun signup(
        name: String,
        email: String,
        pass: String,
        phone: String,
        address: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.signup(name, email, pass, phone, address)
            if (result.isSuccess) {
                showSnackbar("Account created! Welcome to Hina Mart.")
                onResult(true, null)
            } else {
                onResult(false, result.exceptionOrNull()?.message ?: "Signup failed")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            showSnackbar("Logged out successfully")
        }
    }

    fun updateProfile(user: User) {
        viewModelScope.launch {
            repository.updateUserProfile(user)
            showSnackbar("Profile updated successfully")
        }
    }

    // --- Notifications ---
    fun markNotificationAsRead(id: Long) {
        viewModelScope.launch {
            repository.markNotificationAsRead(id)
        }
    }

    fun markAllNotificationsAsRead() {
        viewModelScope.launch {
            repository.markAllNotificationsAsRead()
            showSnackbar("All notifications marked as read")
        }
    }

    fun sendBroadcastNotification(title: String, message: String, type: String = "PROMO") {
        viewModelScope.launch {
            repository.addNotification(
                NotificationItem(
                    title = title,
                    message = message,
                    type = type
                )
            )
            showSnackbar("Push notification sent to all devices!")
        }
    }

    // --- Admin Operations ---
    fun saveProduct(product: Product, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.saveProduct(product)
            showSnackbar("Product '${product.name}' saved successfully")
            onComplete()
        }
    }

    fun deleteProduct(productId: Long, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.deleteProduct(productId)
            showSnackbar("Product deleted from catalog")
            onComplete()
        }
    }

    fun createCoupon(coupon: Coupon, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.createCoupon(coupon)
            showSnackbar("Coupon '${coupon.code}' created!")
            onComplete()
        }
    }
}

class HinaMartViewModelFactory(
    private val repository: HinaMartRepository,
    private val networkObserver: NetworkObserver? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HinaMartViewModel::class.java)) {
            return HinaMartViewModel(repository, networkObserver) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
