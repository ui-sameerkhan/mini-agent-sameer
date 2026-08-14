package com.lazyshopper.app.feature.admin

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lazyshopper.app.core.theme.LsRoleAdmin
import com.lazyshopper.app.feature.admin.analytics.AdminAnalyticsScreen
import com.lazyshopper.app.feature.admin.catalog.AdminBannersScreen
import com.lazyshopper.app.feature.admin.catalog.AdminCategoriesScreen
import com.lazyshopper.app.feature.admin.catalog.AdminCouponsScreen
import com.lazyshopper.app.feature.admin.catalog.AdminOffersScreen
import com.lazyshopper.app.feature.admin.catalog.AdminProductsScreen
import com.lazyshopper.app.feature.admin.catalog.AdminReviewsScreen
import com.lazyshopper.app.feature.admin.catalog.AdminVideoReviewsScreen
import com.lazyshopper.app.feature.admin.dashboard.DashboardScreen
import com.lazyshopper.app.feature.admin.finance.AdminCommissionScreen
import com.lazyshopper.app.feature.admin.finance.AdminPayoutsScreen
import com.lazyshopper.app.feature.admin.finance.AdminProfitSummaryScreen
import com.lazyshopper.app.feature.admin.finance.AdminPromotionsScreen
import com.lazyshopper.app.feature.admin.finance.AdminReferralScreen
import com.lazyshopper.app.feature.admin.finance.AdminRiderPayoutScreen
import com.lazyshopper.app.feature.admin.nav.AdminRoutes
import com.lazyshopper.app.feature.admin.orders.AdminOrdersScreen
import com.lazyshopper.app.feature.admin.people.AdminDeliveryPartnersScreen
import com.lazyshopper.app.feature.admin.people.AdminKycScreen
import com.lazyshopper.app.feature.admin.people.AdminShopsScreen
import com.lazyshopper.app.feature.admin.people.AdminUsersScreen
import com.lazyshopper.app.feature.admin.settings.AdminEmailSettingsScreen
import com.lazyshopper.app.feature.admin.settings.AdminLogsScreen
import com.lazyshopper.app.feature.admin.settings.AdminNotificationsScreen
import com.lazyshopper.app.feature.admin.settings.AdminPaymentsScreen
import com.lazyshopper.app.feature.admin.support.AdminSupportThreadScreen
import com.lazyshopper.app.feature.admin.support.AdminSupportThreadsScreen
import kotlinx.coroutines.launch

private data class DrawerItem(val label: String, val route: String, val icon: ImageVector)
private data class DrawerSection(val title: String, val items: List<DrawerItem>)

private val drawerSections = listOf(
    DrawerSection(
        "Overview",
        listOf(DrawerItem("Dashboard", AdminRoutes.DASHBOARD, Icons.Filled.Dashboard)),
    ),
    DrawerSection(
        "Catalog",
        listOf(
            DrawerItem("Products", AdminRoutes.PRODUCTS, Icons.Filled.Inventory),
            DrawerItem("Categories", AdminRoutes.CATEGORIES, Icons.Filled.Category),
            DrawerItem("Offers", AdminRoutes.OFFERS, Icons.Filled.LocalOffer),
            DrawerItem("Banners", AdminRoutes.BANNERS, Icons.Filled.Campaign),
            DrawerItem("Coupons", AdminRoutes.COUPONS, Icons.Filled.Redeem),
            DrawerItem("Reviews", AdminRoutes.REVIEWS, Icons.Filled.RateReview),
            DrawerItem("Video Reviews", AdminRoutes.VIDEO_REVIEWS, Icons.Filled.VideoLibrary),
        ),
    ),
    DrawerSection(
        "Orders & Delivery",
        listOf(
            DrawerItem("Orders", AdminRoutes.ORDERS, Icons.Filled.ShoppingCart),
            DrawerItem("Delivery Partners", AdminRoutes.DELIVERY_PARTNERS, Icons.Filled.LocalShipping),
            DrawerItem("KYC Approvals", AdminRoutes.KYC, Icons.Filled.VerifiedUser),
        ),
    ),
    DrawerSection(
        "People",
        listOf(
            DrawerItem("Users", AdminRoutes.USERS, Icons.Filled.People),
            DrawerItem("Shops", AdminRoutes.SHOPS, Icons.Filled.Storefront),
        ),
    ),
    DrawerSection(
        "Money",
        listOf(
            DrawerItem("Commission", AdminRoutes.COMMISSION, Icons.Filled.Percent),
            DrawerItem("Rider Payouts", AdminRoutes.RIDER_PAYOUT, Icons.Filled.LocalShipping),
            DrawerItem("Shopkeeper Payouts", AdminRoutes.PAYOUTS, Icons.Filled.Payments),
            DrawerItem("Profit Summary", AdminRoutes.PROFIT_SUMMARY, Icons.Filled.TrendingUp),
            DrawerItem("Promotions", AdminRoutes.PROMOTIONS, Icons.Filled.AttachMoney),
            DrawerItem("Referrals", AdminRoutes.REFERRALS, Icons.Filled.Share),
        ),
    ),
    DrawerSection(
        "Support",
        listOf(DrawerItem("Chat Inbox", AdminRoutes.SUPPORT, Icons.Filled.SupportAgent)),
    ),
    DrawerSection(
        "Insights",
        listOf(
            DrawerItem("Analytics", AdminRoutes.ANALYTICS, Icons.Filled.Analytics),
            DrawerItem("Stock Report", AdminRoutes.STOCK_REPORT, Icons.Filled.Assignment),
            DrawerItem("Coupon Analytics", AdminRoutes.COUPON_ANALYTICS, Icons.Filled.CardGiftcard),
        ),
    ),
    DrawerSection(
        "Settings",
        listOf(
            DrawerItem("Email Settings", AdminRoutes.EMAIL_SETTINGS, Icons.Filled.Email),
            DrawerItem("Payments", AdminRoutes.PAYMENTS, Icons.Filled.ReceiptLong),
            DrawerItem("Logs", AdminRoutes.LOGS, Icons.Filled.History),
            DrawerItem("Notifications", AdminRoutes.NOTIFICATIONS, Icons.Filled.Notifications),
        ),
    ),
)

/**
 * Entry point called by `RootNavGraph` once an `admin`-role user logs in. Owns a `ModalNavigationDrawer`
 * shell — grouped Overview / Catalog / Orders & Delivery / People / Money / Support / Insights / Settings —
 * wrapping an inner `NavHost` that hosts every admin screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminNavGraph(rootNavController: NavHostController, onLogout: () -> Unit) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    fun openDrawer() = scope.launch { drawerState.open() }
    fun closeDrawer() = scope.launch { drawerState.close() }
    fun navigate(route: String) {
        closeDrawer()
        if (currentRoute != route) {
            navController.navigate(route) {
                popUpTo(AdminRoutes.DASHBOARD) { inclusive = false }
                launchSingleTop = true
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(Modifier.verticalScroll(rememberScrollState()).padding(vertical = 12.dp)) {
                    Text(
                        "Lazy Shopper Admin",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = LsRoleAdmin,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    )
                    drawerSections.forEach { section ->
                        Text(
                            section.title,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                        )
                        section.items.forEach { item ->
                            NavigationDrawerItem(
                                label = { Text(item.label) },
                                icon = { Icon(item.icon, contentDescription = null) },
                                selected = currentRoute == item.route,
                                onClick = { navigate(item.route) },
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                            )
                        }
                        Divider(modifier = Modifier.padding(vertical = 6.dp))
                    }
                    NavigationDrawerItem(
                        label = { Text("Logout") },
                        icon = { Icon(Icons.Filled.Logout, contentDescription = null) },
                        selected = false,
                        onClick = { closeDrawer(); onLogout() },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                    )
                }
            }
        },
    ) {
        NavHost(navController = navController, startDestination = AdminRoutes.DASHBOARD) {
            composable(AdminRoutes.DASHBOARD) {
                DashboardScreen(onMenuClick = { openDrawer() }, onNavigate = { navigate(it) })
            }

            // Catalog
            composable(AdminRoutes.PRODUCTS) { AdminProductsScreen(onMenuClick = { openDrawer() }) }
            composable(AdminRoutes.CATEGORIES) { AdminCategoriesScreen(onMenuClick = { openDrawer() }) }
            composable(AdminRoutes.OFFERS) { AdminOffersScreen(onMenuClick = { openDrawer() }) }
            composable(AdminRoutes.BANNERS) { AdminBannersScreen(onMenuClick = { openDrawer() }) }
            composable(AdminRoutes.COUPONS) { AdminCouponsScreen(onMenuClick = { openDrawer() }) }
            composable(AdminRoutes.REVIEWS) { AdminReviewsScreen(onMenuClick = { openDrawer() }) }
            composable(AdminRoutes.VIDEO_REVIEWS) { AdminVideoReviewsScreen(onMenuClick = { openDrawer() }) }

            // Orders & Delivery
            composable(AdminRoutes.ORDERS) { AdminOrdersScreen(onMenuClick = { openDrawer() }) }
            composable(AdminRoutes.DELIVERY_PARTNERS) { AdminDeliveryPartnersScreen(onMenuClick = { openDrawer() }) }
            composable(AdminRoutes.KYC) { AdminKycScreen(onMenuClick = { openDrawer() }) }

            // People
            composable(AdminRoutes.USERS) { AdminUsersScreen(onMenuClick = { openDrawer() }) }
            composable(AdminRoutes.SHOPS) { AdminShopsScreen(onMenuClick = { openDrawer() }) }

            // Money
            composable(AdminRoutes.COMMISSION) { AdminCommissionScreen(onMenuClick = { openDrawer() }) }
            composable(AdminRoutes.RIDER_PAYOUT) { AdminRiderPayoutScreen(onMenuClick = { openDrawer() }) }
            composable(AdminRoutes.PAYOUTS) { AdminPayoutsScreen(onMenuClick = { openDrawer() }) }
            composable(AdminRoutes.PROFIT_SUMMARY) { AdminProfitSummaryScreen(onMenuClick = { openDrawer() }) }
            composable(AdminRoutes.PROMOTIONS) { AdminPromotionsScreen(onMenuClick = { openDrawer() }) }
            composable(AdminRoutes.REFERRALS) { AdminReferralScreen(onMenuClick = { openDrawer() }) }

            // Support
            composable(AdminRoutes.SUPPORT) {
                AdminSupportThreadsScreen(
                    onMenuClick = { openDrawer() },
                    onOpenThread = { customerId, customerName -> navController.navigate(AdminRoutes.supportThread(customerId, customerName)) },
                )
            }
            composable(
                AdminRoutes.SUPPORT_THREAD,
                arguments = listOf(
                    navArgument("customerId") { type = NavType.StringType },
                    navArgument("customerName") { type = NavType.StringType },
                ),
            ) {
                AdminSupportThreadScreen(onBack = { navController.popBackStack() })
            }

            // Insights — Analytics, Stock report and Coupon analytics are tabs of one screen; all three
            // drawer entries land here with the matching tab preselected.
            composable(AdminRoutes.ANALYTICS) { AdminAnalyticsScreen(onMenuClick = { openDrawer() }, initialTab = 0) }
            composable(AdminRoutes.STOCK_REPORT) { AdminAnalyticsScreen(onMenuClick = { openDrawer() }, initialTab = 1) }
            composable(AdminRoutes.COUPON_ANALYTICS) { AdminAnalyticsScreen(onMenuClick = { openDrawer() }, initialTab = 2) }

            // Settings
            composable(AdminRoutes.EMAIL_SETTINGS) { AdminEmailSettingsScreen(onMenuClick = { openDrawer() }) }
            composable(AdminRoutes.PAYMENTS) { AdminPaymentsScreen(onMenuClick = { openDrawer() }) }
            composable(AdminRoutes.LOGS) { AdminLogsScreen(onMenuClick = { openDrawer() }) }
            composable(AdminRoutes.NOTIFICATIONS) { AdminNotificationsScreen(onMenuClick = { openDrawer() }) }
        }
    }
}
