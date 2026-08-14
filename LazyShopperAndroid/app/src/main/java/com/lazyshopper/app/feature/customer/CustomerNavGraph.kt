package com.lazyshopper.app.feature.customer

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lazyshopper.app.core.data.remote.dto.Product
import com.lazyshopper.app.core.navigation.Routes
import com.lazyshopper.app.feature.customer.account.AccountScreen
import com.lazyshopper.app.feature.customer.address.AddressBookScreen
import com.lazyshopper.app.feature.customer.cart.CartScreen
import com.lazyshopper.app.feature.customer.cart.CartViewModel
import com.lazyshopper.app.feature.customer.cart.CheckoutScreen
import com.lazyshopper.app.feature.customer.cart.PaymentResultScreen
import com.lazyshopper.app.feature.customer.catalog.CategoriesScreen
import com.lazyshopper.app.feature.customer.catalog.CategoryShopsScreen
import com.lazyshopper.app.feature.customer.catalog.ProductDetailSheet
import com.lazyshopper.app.feature.customer.catalog.SearchScreen
import com.lazyshopper.app.feature.customer.catalog.ShopPageScreen
import com.lazyshopper.app.feature.customer.chat.OrderChatScreen
import com.lazyshopper.app.feature.customer.chat.SupportChatScreen
import com.lazyshopper.app.feature.customer.home.HomeScreen
import com.lazyshopper.app.feature.customer.orders.OrderTrackingScreen
import com.lazyshopper.app.feature.customer.orders.OrdersScreen
import com.lazyshopper.app.feature.customer.referral.ReferralScreen

private data class BottomTab(val route: String, val label: String, val icon: ImageVector)

private val bottomTabs = listOf(
    BottomTab(Routes.STOREFRONT, "Home", Icons.Filled.Home),
    BottomTab(Routes.CATEGORIES, "Categories", Icons.Filled.Category),
    BottomTab(Routes.CART, "Cart", Icons.Filled.ShoppingCart),
    BottomTab(Routes.ORDERS, "Orders", Icons.Filled.Receipt),
    BottomTab(Routes.ACCOUNT, "Account", Icons.Filled.Person),
)

/**
 * Entry point called by `RootNavGraph` once a `customer`-role user logs in. Owns its own nested
 * `NavHost` + bottom nav shell for the five top-level destinations (Home/Categories/Cart/Orders/
 * Account); everything else (shop page, search, checkout, address book, order tracking, chat,
 * refer & earn) is pushed on top without the bottom bar, same shape as `DeliveryNavGraph`.
 */
@Composable
fun CustomerNavGraph(rootNavController: NavHostController, onLogout: () -> Unit) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // Cart line count for the bottom-bar badge — CartRepository is a Hilt singleton, so this
    // stays in sync with whatever the Cart/Checkout screens do to the cart regardless of which
    // ViewModel instance touched it.
    val cartViewModel: CartViewModel = hiltViewModel()
    val cartLines by cartViewModel.lines.collectAsState()
    val cartCount = cartLines.values.sumOf { it.qty }.toInt()

    val isMainShellRoute = currentRoute in bottomTabs.map { it.route }

    Scaffold(
        bottomBar = {
            if (isMainShellRoute) {
                NavigationBar {
                    bottomTabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                if (currentRoute != tab.route) {
                                    navController.navigate(tab.route) {
                                        popUpTo(Routes.STOREFRONT) { inclusive = false }
                                        launchSingleTop = true
                                    }
                                }
                            },
                            icon = {
                                if (tab.route == Routes.CART && cartCount > 0) {
                                    BadgedBox(badge = { Badge { Text(cartCount.toString()) } }) {
                                        Icon(tab.icon, contentDescription = tab.label)
                                    }
                                } else {
                                    Icon(tab.icon, contentDescription = tab.label)
                                }
                            },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (isMainShellRoute) {
                FloatingActionButton(onClick = { navController.navigate(Routes.SUPPORT_CHAT) }) {
                    Icon(Icons.Filled.Chat, contentDescription = "Support chat")
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.STOREFRONT,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.STOREFRONT) {
                HomeScreen(
                    onSearchClick = { navController.navigate(Routes.SEARCH) },
                    onCartClick = {
                        navController.navigate(Routes.CART) {
                            popUpTo(Routes.STOREFRONT) { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                    onCategoryClick = { key -> navController.navigate(Routes.categoryShops(key)) },
                    onShopClick = { shopId -> navController.navigate(Routes.shopPage(shopId)) },
                )
            }
            composable(Routes.CATEGORIES) {
                CategoriesScreen(onCategoryClick = { key -> navController.navigate(Routes.categoryShops(key)) })
            }
            composable(
                Routes.CATEGORY_SHOPS,
                arguments = listOf(navArgument("categoryKey") { type = NavType.StringType }),
            ) {
                CategoryShopsScreen(
                    onBack = { navController.popBackStack() },
                    onShopClick = { shopId -> navController.navigate(Routes.shopPage(shopId)) },
                )
            }
            composable(Routes.SEARCH) {
                var selectedProduct by remember { mutableStateOf<Product?>(null) }
                SearchScreen(
                    onBack = { navController.popBackStack() },
                    onProductClick = { selectedProduct = it },
                )
                selectedProduct?.let { product ->
                    ProductDetailSheet(product = product, onDismiss = { selectedProduct = null })
                }
            }
            composable(
                Routes.SHOP_PAGE,
                arguments = listOf(navArgument("shopId") { type = NavType.StringType }),
            ) {
                ShopPageScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.CART) {
                CartScreen(
                    onBack = {
                        navController.navigate(Routes.STOREFRONT) {
                            popUpTo(Routes.STOREFRONT) { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                    onCheckout = { navController.navigate(Routes.CHECKOUT) },
                )
            }
            composable(Routes.CHECKOUT) {
                CheckoutScreen(
                    onBack = { navController.popBackStack() },
                    onPlacedOrder = { orderId, status ->
                        navController.navigate(Routes.paymentResult(orderId, status)) {
                            popUpTo(Routes.STOREFRONT) { inclusive = false }
                        }
                    },
                )
            }
            composable(
                Routes.PAYMENT_RESULT,
                arguments = listOf(
                    navArgument("orderId") { type = NavType.StringType },
                    navArgument("status") { type = NavType.StringType },
                ),
            ) { entry ->
                val orderId = entry.arguments?.getString("orderId").orEmpty()
                val status = entry.arguments?.getString("status").orEmpty()
                PaymentResultScreen(
                    orderId = orderId,
                    status = status,
                    onViewOrder = {
                        navController.navigate(Routes.ORDERS) {
                            popUpTo(Routes.STOREFRONT) { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                    onContinueShopping = {
                        navController.navigate(Routes.STOREFRONT) {
                            popUpTo(Routes.STOREFRONT) { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(Routes.ORDERS) {
                OrdersScreen(onTrackOrder = { orderId -> navController.navigate(Routes.orderTracking(orderId)) })
            }
            composable(
                Routes.ORDER_TRACKING,
                arguments = listOf(navArgument("orderId") { type = NavType.StringType }),
            ) {
                OrderTrackingScreen(
                    onBack = { navController.popBackStack() },
                    onChat = { orderId -> navController.navigate(Routes.orderChat(orderId)) },
                )
            }
            composable(
                Routes.ORDER_CHAT,
                arguments = listOf(navArgument("orderId") { type = NavType.StringType }),
            ) {
                OrderChatScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.SUPPORT_CHAT) {
                SupportChatScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.ADDRESS_BOOK) {
                AddressBookScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.REFER_EARN) {
                ReferralScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.ACCOUNT) {
                AccountScreen(
                    onManageAddresses = { navController.navigate(Routes.ADDRESS_BOOK) },
                    onReferEarn = { navController.navigate(Routes.REFER_EARN) },
                    onLogout = onLogout,
                )
            }
        }
    }
}
