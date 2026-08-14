package com.lazyshopper.app.feature.shopkeeper

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lazyshopper.app.core.navigation.Routes
import com.lazyshopper.app.feature.shopkeeper.dashboard.ShopkeeperDashboardScreen
import com.lazyshopper.app.feature.shopkeeper.earnings.ShopkeeperEarningsScreen
import com.lazyshopper.app.feature.shopkeeper.kyc.ShopkeeperKycScreen
import com.lazyshopper.app.feature.shopkeeper.orders.ShopkeeperOrdersScreen
import com.lazyshopper.app.feature.shopkeeper.products.ProductFormScreen
import com.lazyshopper.app.feature.shopkeeper.products.ProductsListScreen
import com.lazyshopper.app.feature.shopkeeper.shops.ShopFormScreen
import com.lazyshopper.app.feature.shopkeeper.shops.ShopsScreen

private data class BottomTab(val route: String, val label: String, val icon: ImageVector)

private val bottomTabs = listOf(
    BottomTab(Routes.SHOPKEEPER_DASHBOARD, "Dashboard", Icons.Filled.Storefront),
    BottomTab(Routes.SHOPKEEPER_PRODUCTS, "Products", Icons.Filled.Inventory),
    BottomTab(Routes.SHOPKEEPER_ORDERS, "Orders", Icons.Filled.ReceiptLong),
    BottomTab(Routes.SHOPKEEPER_EARNINGS, "Earnings", Icons.Filled.AccountBalanceWallet),
)

/**
 * Entry point called by `RootNavGraph` once a `shopkeeper`-role user logs in. Owns its own nested
 * `NavHost` + bottom nav shell. Shops and KYC aren't bottom tabs — they're reached from the
 * Dashboard (shop stat card / KYC banner) and pushed on top without the bottom bar, same pattern
 * as `DeliveryNavGraph`.
 */
@Composable
fun ShopkeeperNavGraph(rootNavController: NavHostController, onLogout: () -> Unit) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            if (bottomTabs.any { it.route == currentRoute }) {
                NavigationBar {
                    bottomTabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                if (currentRoute != tab.route) {
                                    navController.navigate(tab.route) {
                                        popUpTo(Routes.SHOPKEEPER_DASHBOARD) { inclusive = false }
                                        launchSingleTop = true
                                    }
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.SHOPKEEPER_DASHBOARD,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.SHOPKEEPER_DASHBOARD) {
                ShopkeeperDashboardScreen(
                    onNavigateProducts = { navController.navigate(Routes.SHOPKEEPER_PRODUCTS) },
                    onNavigateShops = { navController.navigate(Routes.SHOPKEEPER_SHOPS) },
                    onNavigateOrders = { navController.navigate(Routes.SHOPKEEPER_ORDERS) },
                    onNavigateEarnings = { navController.navigate(Routes.SHOPKEEPER_EARNINGS) },
                    onNavigateKyc = { navController.navigate(Routes.KYC_FORM) },
                    onLogout = onLogout,
                )
            }
            composable(Routes.SHOPKEEPER_PRODUCTS) {
                ProductsListScreen(
                    onAddProduct = { navController.navigate(Routes.shopkeeperProductForm()) },
                    onEditProduct = { productId -> navController.navigate(Routes.shopkeeperProductForm(productId)) },
                )
            }
            composable(
                Routes.SHOPKEEPER_PRODUCT_FORM,
                arguments = listOf(navArgument("productId") { type = NavType.StringType; nullable = true; defaultValue = null }),
            ) { backStackEntry ->
                val productId = backStackEntry.arguments?.getString("productId")
                ProductFormScreen(productId = productId, onDone = { navController.popBackStack() })
            }
            composable(Routes.SHOPKEEPER_ORDERS) {
                ShopkeeperOrdersScreen()
            }
            composable(Routes.SHOPKEEPER_EARNINGS) {
                ShopkeeperEarningsScreen()
            }
            composable(Routes.SHOPKEEPER_SHOPS) {
                ShopsScreen(onAddShop = { navController.navigate(Routes.shopkeeperShopForm()) })
            }
            composable(
                Routes.SHOPKEEPER_SHOP_FORM,
                arguments = listOf(navArgument("shopId") { type = NavType.StringType; nullable = true; defaultValue = null }),
            ) {
                ShopFormScreen(onDone = { navController.popBackStack() })
            }
            composable(Routes.KYC_FORM) {
                ShopkeeperKycScreen(onDone = { navController.popBackStack() }, onBack = { navController.popBackStack() })
            }
        }
    }
}
