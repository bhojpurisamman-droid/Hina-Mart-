package com.example.hinamart.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.hinamart.data.model.Product
import com.example.hinamart.ui.components.*
import com.example.hinamart.ui.viewmodel.HinaMartViewModel
import com.example.ui.theme.*

@Composable
fun HomeScreen(
    viewModel: HinaMartViewModel,
    onProductClick: (Product) -> Unit,
    onNavigateToCatalog: (String) -> Unit,
    onNavigateToDeals: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val featuredProducts by viewModel.featuredProducts.collectAsStateWithLifecycle()
    val dealOfTheDayProducts by viewModel.dealOfTheDayProducts.collectAsStateWithLifecycle()
    val allProducts by viewModel.products.collectAsStateWithLifecycle()
    val cartItems by viewModel.cartItems.collectAsStateWithLifecycle()

    val cartQuantityMap = remember(cartItems) {
        cartItems.associate { it.product.id to it.quantity }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen"),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // 1. Search Bar Header Action
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onNavigateToCatalog("all") },
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Search fresh fruits, groceries, dairy, snacks...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 2. Categories Quick Selector
        item {
            CategoryChipList(
                categories = categories,
                selectedCategory = "all",
                onSelectCategory = { categoryName ->
                    viewModel.selectCategory(categoryName)
                    onNavigateToCatalog(categoryName)
                }
            )
        }

        // 3. Hero Promo Banner
        item {
            Spacer(modifier = Modifier.height(6.dp))
            PromoBannerCard(
                onExploreOffers = {
                    viewModel.selectCategory("all")
                    onNavigateToDeals()
                }
            )
        }

        // 4. Flash Deal of the Day Section
        if (dealOfTheDayProducts.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "⚡ Flash Deals",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FlashDealTimer()
                    }

                    TextButton(
                        onClick = onNavigateToDeals,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "See All",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(dealOfTheDayProducts) { product ->
                        ProductCard(
                            product = product,
                            onProductClick = onProductClick,
                            onAddToCart = { viewModel.addToCart(it) },
                            cartQuantity = cartQuantityMap[product.id] ?: 0,
                            onUpdateQuantity = { qty -> viewModel.updateCartQuantity(product.id, qty) },
                            modifier = Modifier.width(185.dp)
                        )
                    }
                }
            }
        }

        // 5. Featured Daily Essentials
        if (featuredProducts.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(12.dp))
                SectionHeader(
                    title = "⭐ Featured Essentials",
                    subtitle = "Handpicked best quality items",
                    actionText = "Explore",
                    onActionClick = { onNavigateToCatalog("all") }
                )

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(featuredProducts) { product ->
                        ProductCard(
                            product = product,
                            onProductClick = onProductClick,
                            onAddToCart = { viewModel.addToCart(it) },
                            cartQuantity = cartQuantityMap[product.id] ?: 0,
                            onUpdateQuantity = { qty -> viewModel.updateCartQuantity(product.id, qty) },
                            modifier = Modifier.width(185.dp)
                        )
                    }
                }
            }
        }

        // 6. Free Shipping Banner
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                color = EmeraldLight
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = EmeraldTeal,
                        shape = CircleShape,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.LocalShipping,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "FREE Express Delivery",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = EmeraldDark
                        )
                        Text(
                            text = "On all grocery baskets above ₹499. Delivered in 24 hrs.",
                            style = MaterialTheme.typography.bodySmall,
                            color = OrangeDark
                        )
                    }
                }
            }
        }

        // 7. Popular All Catalog Grid
        item {
            Spacer(modifier = Modifier.height(16.dp))
            SectionHeader(
                title = "🛒 All Products & Staples",
                subtitle = "Everything fresh and pantry ready",
                actionText = "Full Catalog",
                onActionClick = { onNavigateToCatalog("all") }
            )
        }

        // Display products in pairs for standard 2-column layout in lazy column
        val chunkedProducts = allProducts.take(8).chunked(2)
        items(chunkedProducts) { pair ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                for (product in pair) {
                    Box(modifier = Modifier.weight(1f)) {
                        ProductCard(
                            product = product,
                            onProductClick = onProductClick,
                            onAddToCart = { viewModel.addToCart(it) },
                            cartQuantity = cartQuantityMap[product.id] ?: 0,
                            onUpdateQuantity = { qty -> viewModel.updateCartQuantity(product.id, qty) }
                        )
                    }
                }
                if (pair.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
