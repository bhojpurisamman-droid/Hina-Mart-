package com.example.hinamart.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.hinamart.data.model.*
import com.example.hinamart.ui.components.StatusBadge
import com.example.hinamart.ui.viewmodel.HinaMartViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    viewModel: HinaMartViewModel,
    onBackClick: () -> Unit
) {
    val products by viewModel.products.collectAsStateWithLifecycle()
    val orders by viewModel.orders.collectAsStateWithLifecycle()
    val coupons by viewModel.activeCoupons.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Dashboard/Products, 1: Orders, 2: Coupons, 3: Push Notifications

    // Dialog States
    var showProductDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<Product?>(null) }
    var showCouponDialog by remember { mutableStateOf(false) }
    var showOrderStatusDialog by remember { mutableStateOf<Order?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Hina Mart Store Admin", fontWeight = FontWeight.Bold)
                        Text("Live Store & Order Operations", style = MaterialTheme.typography.labelSmall, color = AmberAccent)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                ExtendedFloatingActionButton(
                    onClick = {
                        editingProduct = null
                        showProductDialog = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Add Product", fontWeight = FontWeight.Bold) }
                )
            } else if (selectedTab == 2) {
                ExtendedFloatingActionButton(
                    onClick = { showCouponDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("New Coupon", fontWeight = FontWeight.Bold) }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("admin_panel_screen")
        ) {
            // KPI Summary Row
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val totalRevenue = orders.sumOf { it.total }
                    KpiCard(title = "Total Revenue", value = "₹${String.format("%.0f", totalRevenue)}", icon = Icons.Default.CurrencyRupee, color = EmeraldTeal, modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(8.dp))
                    KpiCard(title = "Orders", value = "${orders.size}", icon = Icons.Default.ShoppingBag, color = AmberAccent, modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(8.dp))
                    KpiCard(title = "Products", value = "${products.size}", icon = Icons.Default.Inventory, color = InfoBlue, modifier = Modifier.weight(1f))
                }
            }

            // Tabs
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Products", fontWeight = FontWeight.Bold) })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Orders (${orders.size})", fontWeight = FontWeight.Bold) })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Coupons", fontWeight = FontWeight.Bold) })
                Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text("Push Alert", fontWeight = FontWeight.Bold) })
            }

            // Tab Content
            when (selectedTab) {
                0 -> {
                    // PRODUCTS TAB
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 90.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(products, key = { it.id }) { prod ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        model = prod.imageUrl,
                                        contentDescription = prod.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    )

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(prod.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), maxLines = 1)
                                        Text("${prod.category} • ${prod.unit}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            text = "₹${String.format("%.0f", prod.price)} • Stock: ${prod.stockQuantity}",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        )
                                    }

                                    // Actions
                                    IconButton(
                                        onClick = {
                                            editingProduct = prod
                                            showProductDialog = true
                                        }
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                                    }

                                    IconButton(
                                        onClick = {
                                            viewModel.deleteProduct(prod.id) {}
                                        }
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ErrorRed)
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // ORDERS TAB
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(orders, key = { it.id }) { ord ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("Order #${ord.orderNumber}", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                            Text(ord.userName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        StatusBadge(status = ord.status)
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = "${ord.items.size} items • Total: ₹${String.format("%.0f", ord.total)} • ${ord.paymentMethod}",
                                        style = MaterialTheme.typography.bodySmall
                                    )

                                    if (ord.trackingNotes.isNotBlank()) {
                                        Text(
                                            text = "Note: ${ord.trackingNotes}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Button(
                                        onClick = { showOrderStatusDialog = ord },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Update Order Status & Dispatch")
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // COUPONS TAB
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 90.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(coupons, key = { it.id }) { cpn ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(cpn.code, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold), color = MaterialTheme.colorScheme.primary)
                                        Text("${cpn.discountPercentage}% Discount • Max ₹${String.format("%.0f", cpn.maxDiscount)}", style = MaterialTheme.typography.bodyMedium)
                                        Text("Min Basket: ₹${String.format("%.0f", cpn.minOrderAmount)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }

                                    Surface(
                                        color = EmeraldLight,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = if (cpn.isActive) "ACTIVE" else "EXPIRED",
                                            color = EmeraldDark,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                3 -> {
                    // PUSH NOTIFICATIONS BROADCAST TAB
                    var notifTitle by remember { mutableStateOf("⚡ Weekend Special: 40% OFF Organic Honey!") }
                    var notifMsg by remember { mutableStateOf("Stock up on fresh pantry essentials before stocks run out.") }
                    var notifType by remember { mutableStateOf("PROMO") }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Broadcast Push Notification to Customers",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )

                        Text(
                            text = "Send real-time alerts directly to the device notification tray. Users will hear notification tones and see instant banners.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = notifTitle,
                            onValueChange = { notifTitle = it },
                            label = { Text("Notification Title") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = notifMsg,
                            onValueChange = { notifMsg = it },
                            label = { Text("Message Body") },
                            minLines = 3,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("PROMO", "ORDER", "SYSTEM").forEach { type ->
                                FilterChip(
                                    selected = notifType == type,
                                    onClick = { notifType = type },
                                    label = { Text(type) }
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (notifTitle.isNotBlank()) {
                                    viewModel.sendBroadcastNotification(notifTitle, notifMsg, notifType)
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CoralAccent),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Send Push Broadcast Now", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Color.White))
                        }
                    }
                }
            }
        }
    }

    // Add / Edit Product Dialog
    if (showProductDialog) {
        val isEdit = editingProduct != null
        var name by remember { mutableStateOf(editingProduct?.name ?: "") }
        var description by remember { mutableStateOf(editingProduct?.description ?: "") }
        var category by remember { mutableStateOf(editingProduct?.category ?: "Fresh Fruits") }
        var price by remember { mutableStateOf(editingProduct?.price?.toString() ?: "199") }
        var originalPrice by remember { mutableStateOf(editingProduct?.originalPrice?.toString() ?: "249") }
        var unit by remember { mutableStateOf(editingProduct?.unit ?: "1 kg / bundle") }
        var imageUrl by remember { mutableStateOf(editingProduct?.imageUrl ?: "https://images.unsplash.com/photo-1550258987-190a2d41a8ba?w=500&auto=format&fit=crop&q=60") }
        var stockQuantity by remember { mutableStateOf(editingProduct?.stockQuantity?.toString() ?: "30") }
        var isFeatured by remember { mutableStateOf(editingProduct?.isFeatured ?: false) }
        var inStock by remember { mutableStateOf(editingProduct?.inStock ?: true) }

        AlertDialog(
            onDismissRequest = { showProductDialog = false },
            title = { Text(if (isEdit) "Edit Product" else "Add New Catalog Product") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Product Name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") }, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Price (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                        OutlinedTextField(value = originalPrice, onValueChange = { originalPrice = it }, label = { Text("Orig. Price (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = unit, onValueChange = { unit = it }, label = { Text("Unit / Weight") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = stockQuantity, onValueChange = { stockQuantity = it }, label = { Text("Stock Qty") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                    }
                    OutlinedTextField(value = imageUrl, onValueChange = { imageUrl = it }, label = { Text("Image URL") }, modifier = Modifier.fillMaxWidth())

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Featured Product")
                        Switch(checked = isFeatured, onCheckedChange = { isFeatured = it })
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("In Stock")
                        Switch(checked = inStock, onCheckedChange = { inStock = it })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsedPrice = price.toDoubleOrNull() ?: 1.0
                        val parsedOrig = originalPrice.toDoubleOrNull() ?: parsedPrice
                        val discountPct = if (parsedOrig > parsedPrice) (((parsedOrig - parsedPrice) / parsedOrig) * 100).toInt() else 0

                        val newOrUpdated = Product(
                            id = editingProduct?.id ?: 0L,
                            name = name.ifBlank { "New Product" },
                            description = description.ifBlank { "Fresh and delicious quality food item." },
                            price = parsedPrice,
                            originalPrice = parsedOrig,
                            imageUrl = imageUrl.ifBlank { "https://images.unsplash.com/photo-1542838132-92c53300491e?w=500" },
                            category = category.ifBlank { "Groceries" },
                            unit = unit.ifBlank { "1 pack" },
                            inStock = inStock,
                            stockQuantity = stockQuantity.toIntOrNull() ?: 20,
                            rating = editingProduct?.rating ?: 5.0f,
                            reviewCount = editingProduct?.reviewCount ?: 1,
                            isFeatured = isFeatured,
                            discountPercent = discountPct
                        )

                        viewModel.saveProduct(newOrUpdated) {
                            showProductDialog = false
                        }
                    }
                ) {
                    Text(if (isEdit) "Update Product" else "Save Product")
                }
            },
            dismissButton = {
                TextButton(onClick = { showProductDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Update Order Status Dialog
    if (showOrderStatusDialog != null) {
        val ord = showOrderStatusDialog!!
        var currentStat by remember { mutableStateOf(ord.status) }
        var trackingNotes by remember { mutableStateOf(ord.trackingNotes) }

        AlertDialog(
            onDismissRequest = { showOrderStatusDialog = null },
            title = { Text("Update Order #${ord.orderNumber}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Select New Status:")
                    OrderStatus.entries.forEach { st ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (currentStat == st) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { currentStat = st }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = currentStat == st, onClick = { currentStat = st })
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(st.displayName, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = trackingNotes,
                        onValueChange = { trackingNotes = it },
                        label = { Text("Courier / Tracking Dispatch Note") },
                        placeholder = { Text("e.g. Courier #102 dispatched with temperature-controlled bag.") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateOrderStatus(ord.id, currentStat, trackingNotes)
                        showOrderStatusDialog = null
                    }
                ) {
                    Text("Update & Notify Customer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showOrderStatusDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // New Coupon Dialog
    if (showCouponDialog) {
        var code by remember { mutableStateOf("") }
        var discountPct by remember { mutableStateOf("20") }
        var minAmount by remember { mutableStateOf("499") }
        var maxDisc by remember { mutableStateOf("200") }

        AlertDialog(
            onDismissRequest = { showCouponDialog = false },
            title = { Text("Create Promo Coupon") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = code, onValueChange = { code = it.uppercase() }, label = { Text("Coupon Code (e.g. SUMMER30)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = discountPct, onValueChange = { discountPct = it }, label = { Text("Discount Percentage (%)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = minAmount, onValueChange = { minAmount = it }, label = { Text("Min Basket Value (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = maxDisc, onValueChange = { maxDisc = it }, label = { Text("Max Discount Allowed (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (code.isNotBlank()) {
                            val c = Coupon(
                                code = code.uppercase(),
                                discountPercentage = discountPct.toIntOrNull() ?: 10,
                                minOrderValue = minAmount.toDoubleOrNull() ?: 0.0,
                                maxDiscount = maxDisc.toDoubleOrNull() ?: 20.0,
                                description = "$discountPct% Discount on basket"
                            )
                            viewModel.createCoupon(c) {
                                showCouponDialog = false
                            }
                        }
                    }
                ) {
                    Text("Create Coupon")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCouponDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun KpiCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold, color = color)
            )
        }
    }
}
