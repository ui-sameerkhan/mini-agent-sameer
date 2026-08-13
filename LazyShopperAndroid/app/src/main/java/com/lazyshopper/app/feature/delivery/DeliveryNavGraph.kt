package com.lazyshopper.app.feature.delivery

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.padding
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
import com.lazyshopper.app.feature.delivery.chat.DeliveryChatScreen
import com.lazyshopper.app.feature.delivery.dashboard.DeliveryDashboardScreen
import com.lazyshopper.app.feature.delivery.earnings.DeliveryEarningsScreen
import com.lazyshopper.app.feature.delivery.kyc.DeliveryKycScreen

private data class BottomTab(val route: String, val label: String, val icon: ImageVector)

private val bottomTabs = listOf(
    BottomTab(Routes.DELIVERY_DASHBOARD, "Dashboard", Icons.Filled.LocalShipping),
    BottomTab(Routes.DELIVERY_EARNINGS, "Earnings", Icons.Filled.AccountBalanceWallet),
)

/**
 * Entry point called by `RootNavGraph` once a `delivery`-role user logs in. Owns its own nested
 * `NavHost` + bottom nav shell — the rider mostly lives on the Dashboard, with Earnings as the
 * one other tab, and KYC/Chat pushed on top without the bottom bar.
 */
@Composable
fun DeliveryNavGraph(rootNavController: NavHostController, onLogout: () -> Unit) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            if (currentRoute == Routes.DELIVERY_DASHBOARD || currentRoute == Routes.DELIVERY_EARNINGS) {
                NavigationBar {
                    bottomTabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                if (currentRoute != tab.route) {
                                    navController.navigate(tab.route) {
                                        popUpTo(Routes.DELIVERY_DASHBOARD) { inclusive = false }
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
            startDestination = Routes.DELIVERY_DASHBOARD,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.DELIVERY_DASHBOARD) {
                DeliveryDashboardScreen(
                    onNavigateKyc = { navController.navigate(Routes.KYC_FORM) },
                    onOpenChat = { orderId -> navController.navigate(Routes.orderChat(orderId)) },
                    onLogout = onLogout,
                )
            }
            composable(Routes.DELIVERY_EARNINGS) {
                DeliveryEarningsScreen()
            }
            composable(Routes.KYC_FORM) {
                DeliveryKycScreen(onDone = { navController.popBackStack() })
            }
            composable(
                Routes.ORDER_CHAT,
                arguments = listOf(navArgument("orderId") { type = NavType.StringType }),
            ) {
                DeliveryChatScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
