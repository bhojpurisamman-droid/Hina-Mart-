package com.example.hinamart.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.hinamart.data.model.Order
import com.example.hinamart.data.model.Product
import com.example.hinamart.ui.components.HinaTopAppBar
import com.example.hinamart.ui.components.NoInternetScreen
import com.example.hinamart.ui.components.OfflineBanner
import com.example.hinamart.ui.screens.admin.AdminPanelScreen
import com.example.hinamart.ui.screens.auth.AuthScreen
import com.example.hinamart.ui.screens.cart.CartScreen
import com.example.hinamart.ui.screens.catalog.CatalogScreen
import com.example.hinamart.ui.screens.checkout.CheckoutScreen
import com.example.hinamart.ui.screens.detail.ProductDetailScreen
import com.example.hinamart.ui.screens.home.HomeScreen
import com.example.hinamart.ui.screens.notifications.NotificationsScreen
import com.example.hinamart.ui.screens.orders.OrderDetailScreen
import com.example.hinamart.ui.screens.orders.OrdersScreen
import com.example.hinamart.ui.screens.profile.ProfileScreen
import com.example.hinamart.ui.screens.splash.SplashScreen
import com.example.hinamart.ui.viewmodel.HinaMartViewModel
import com.example.ui.theme.*

enum class AppScreen {
    SPLASH,
    MAIN_TABS,
    PRODUCT_DETAIL,
    CHECKOUT,
    ORDER_DETAIL,
    ADMIN_PANEL,
    NOTIFICATIONS,
    AUTH
}

enum class BottomTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home, "tab_home"),
    CATALOG("Shop", Icons.Filled.Storefront, Icons.Outlined.Storefront, "tab_shop"),
    CART("Cart", Icons.Filled.ShoppingCart, Icons.Outlined.ShoppingCart, "tab_cart"),
    ORDERS("Orders", Icons.Filled.ReceiptLong, Icons.Outlined.ReceiptLong, "tab_orders"),
    PROFILE("Profile", Icons.Filled.Person, Icons.Outlined.Person, "tab_profile")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HinaMartApp(viewModel: HinaMartViewModel) {
    val context = LocalContext.current

    // Request Android 13+ Notification Runtime Permission
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    var currentScreen by remember { mutableStateOf(AppScreen.SPLASH) }
    var currentTab by remember { mutableStateOf(BottomTab.HOME) }
    val screenStack = remember { mutableStateListOf<AppScreen>() }

    fun navigateTo(screen: AppScreen) {
        screenStack.add(currentScreen)
        currentScreen = screen
    }

    fun navigateBack() {
        if (screenStack.isNotEmpty()) {
            currentScreen = screenStack.removeLast()
        } else if (currentScreen != AppScreen.MAIN_TABS) {
            currentScreen = AppScreen.MAIN_TABS
        }
    }

    // Hardware Back-button handler
    BackHandler(enabled = currentScreen != AppScreen.SPLASH) {
        if (currentScreen == AppScreen.MAIN_TABS) {
            if (currentTab != BottomTab.HOME) {
                currentTab = BottomTab.HOME
            } else {
                // At root, let system handle exit
            }
        } else {
            navigateBack()
        }
    }

    val cartItems by viewModel.cartItems.collectAsStateWithLifecycle()
    val totalCartCount = remember(cartItems) { cartItems.sumOf { it.quantity } }
    val unreadNotifCount by viewModel.unreadNotificationsCount.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                OfflineBanner(isOnline = isOnline)
                if (currentScreen == AppScreen.MAIN_TABS && currentTab != BottomTab.CART && currentTab != BottomTab.PROFILE && isOnline) {
                    HinaTopAppBar(
                        title = when (currentTab) {
                            BottomTab.HOME -> "Hina Mart"
                            BottomTab.CATALOG -> "Catalog & Groceries"
                            BottomTab.ORDERS -> "Your Orders"
                            else -> "Hina Mart"
                        },
                        showBackButton = false,
                        unreadNotificationCount = unreadNotifCount,
                        onNotificationClick = { navigateTo(AppScreen.NOTIFICATIONS) },
                        cartItemCount = totalCartCount,
                        onCartClick = {
                            currentTab = BottomTab.CART
                        }
                    )
                }
            }
        },
        bottomBar = {
            if (currentScreen == AppScreen.MAIN_TABS && isOnline) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    BottomTab.entries.forEach { tab ->
                        val isSelected = currentTab == tab
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { currentTab = tab },
                            modifier = Modifier.testTag(tab.testTag),
                            icon = {
                                if (tab == BottomTab.CART && totalCartCount > 0) {
                                    BadgedBox(
                                        badge = {
                                            Badge(
                                                containerColor = MaterialTheme.colorScheme.primary,
                                                contentColor = Color.White
                                            ) {
                                                Text("$totalCartCount")
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.title
                                        )
                                    }
                                } else {
                                    Icon(
                                        imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.title
                                    )
                                }
                            },
                            label = {
                                Text(
                                    text = tab.title,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (currentScreen == AppScreen.SPLASH) {
                SplashScreen(
                    onSplashFinished = {
                        currentScreen = AppScreen.MAIN_TABS
                    }
                )
            } else if (!isOnline) {
                NoInternetScreen(
                    onRetry = {
                        viewModel.retryConnection()
                    }
                )
            } else {
                when (currentScreen) {
                    AppScreen.SPLASH -> {}

                    AppScreen.MAIN_TABS -> {
                    when (currentTab) {
                        BottomTab.HOME -> {
                            HomeScreen(
                                viewModel = viewModel,
                                onProductClick = { product ->
                                    viewModel.selectProduct(product)
                                    navigateTo(AppScreen.PRODUCT_DETAIL)
                                },
                                onNavigateToCatalog = { category ->
                                    viewModel.selectCategory(category)
                                    currentTab = BottomTab.CATALOG
                                },
                                onNavigateToDeals = {
                                    viewModel.selectCategory("all")
                                    currentTab = BottomTab.CATALOG
                                }
                            )
                        }

                        BottomTab.CATALOG -> {
                            CatalogScreen(
                                viewModel = viewModel,
                                onProductClick = { product ->
                                    viewModel.selectProduct(product)
                                    navigateTo(AppScreen.PRODUCT_DETAIL)
                                }
                            )
                        }

                        BottomTab.CART -> {
                            CartScreen(
                                viewModel = viewModel,
                                onNavigateToCheckout = { navigateTo(AppScreen.CHECKOUT) },
                                onNavigateToCatalog = { currentTab = BottomTab.CATALOG },
                                onProductClick = { product ->
                                    viewModel.selectProduct(product)
                                    navigateTo(AppScreen.PRODUCT_DETAIL)
                                }
                            )
                        }

                        BottomTab.ORDERS -> {
                            OrdersScreen(
                                viewModel = viewModel,
                                onOrderClick = { order ->
                                    viewModel.selectOrder(order)
                                    navigateTo(AppScreen.ORDER_DETAIL)
                                },
                                onStartShopping = { currentTab = BottomTab.CATALOG }
                            )
                        }

                        BottomTab.PROFILE -> {
                            ProfileScreen(
                                viewModel = viewModel,
                                onNavigateToOrders = { currentTab = BottomTab.ORDERS },
                                onNavigateToAdmin = { navigateTo(AppScreen.ADMIN_PANEL) },
                                onNavigateToNotifications = { navigateTo(AppScreen.NOTIFICATIONS) },
                                onOpenAuth = { navigateTo(AppScreen.AUTH) }
                            )
                        }
                    }
                }

                AppScreen.PRODUCT_DETAIL -> {
                    ProductDetailScreen(
                        viewModel = viewModel,
                        onBackClick = { navigateBack() },
                        onNavigateToCart = {
                            screenStack.clear()
                            currentTab = BottomTab.CART
                            currentScreen = AppScreen.MAIN_TABS
                        },
                        onNavigateToCheckout = {
                            navigateTo(AppScreen.CHECKOUT)
                        }
                    )
                }

                AppScreen.CHECKOUT -> {
                    CheckoutScreen(
                        viewModel = viewModel,
                        onBackClick = { navigateBack() },
                        onOrderSuccess = { order ->
                            viewModel.selectOrder(order)
                            screenStack.clear()
                            currentTab = BottomTab.ORDERS
                            currentScreen = AppScreen.ORDER_DETAIL
                        }
                    )
                }

                AppScreen.ORDER_DETAIL -> {
                    OrderDetailScreen(
                        viewModel = viewModel,
                        onBackClick = { navigateBack() },
                        onReorderToCart = {
                            screenStack.clear()
                            currentTab = BottomTab.CART
                            currentScreen = AppScreen.MAIN_TABS
                        }
                    )
                }

                AppScreen.ADMIN_PANEL -> {
                    AdminPanelScreen(
                        viewModel = viewModel,
                        onBackClick = { navigateBack() }
                    )
                }

                AppScreen.NOTIFICATIONS -> {
                    NotificationsScreen(
                        viewModel = viewModel,
                        onBackClick = { navigateBack() },
                        onNavigateToOrders = {
                            screenStack.clear()
                            currentTab = BottomTab.ORDERS
                            currentScreen = AppScreen.MAIN_TABS
                        },
                        onNavigateToDeals = {
                            screenStack.clear()
                            currentTab = BottomTab.CATALOG
                            currentScreen = AppScreen.MAIN_TABS
                        }
                    )
                }

                AppScreen.AUTH -> {
                    AuthScreen(
                        viewModel = viewModel,
                        onAuthSuccess = { navigateBack() },
                        onClose = { navigateBack() }
                    )
                }
            }
        }
    }
}
}
