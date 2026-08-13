package com.lazyshopper.app.core.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lazyshopper.app.core.data.local.Role
import com.lazyshopper.app.feature.auth.AuthScreen
import com.lazyshopper.app.feature.auth.ForgotPasswordScreen
import com.lazyshopper.app.feature.auth.ResetPasswordScreen
import com.lazyshopper.app.feature.splash.SplashScreen

@Composable
fun RootNavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.SPLASH) {
        composable(Routes.SPLASH) {
            SplashScreen(onResolved = { role ->
                val dest = roleGraphRoute(role)
                navController.navigate(dest) {
                    popUpTo(Routes.SPLASH) { inclusive = true }
                }
            })
        }
        composable(Routes.AUTH) {
            AuthScreen(
                onLoggedIn = { role ->
                    navController.navigate(roleGraphRoute(role)) {
                        popUpTo(Routes.AUTH) { inclusive = true }
                    }
                },
                onForgotPassword = { navController.navigate(Routes.FORGOT_PASSWORD) },
            )
        }
        composable(Routes.FORGOT_PASSWORD) {
            ForgotPasswordScreen(onBack = { navController.popBackStack() })
        }
        composable(
            Routes.RESET_PASSWORD,
            arguments = listOf(navArgument("token") { type = NavType.StringType }),
        ) { backStackEntry ->
            val token = backStackEntry.arguments?.getString("token").orEmpty()
            ResetPasswordScreen(token = token, onDone = { navController.navigate(Routes.AUTH) { popUpTo(0) } })
        }

        fun onLogout() {
            navController.navigate(Routes.AUTH) { popUpTo(0) }
        }

        composable(Routes.CUSTOMER_GRAPH) {
            com.lazyshopper.app.feature.customer.CustomerNavGraph(rootNavController = navController, onLogout = ::onLogout)
        }
        composable(Routes.SHOPKEEPER_GRAPH) {
            com.lazyshopper.app.feature.shopkeeper.ShopkeeperNavGraph(rootNavController = navController, onLogout = ::onLogout)
        }
        composable(Routes.DELIVERY_GRAPH) {
            com.lazyshopper.app.feature.delivery.DeliveryNavGraph(rootNavController = navController, onLogout = ::onLogout)
        }
        composable(Routes.ADMIN_GRAPH) {
            com.lazyshopper.app.feature.admin.AdminNavGraph(rootNavController = navController, onLogout = ::onLogout)
        }
    }
}

private fun roleGraphRoute(role: String?): String = when (role) {
    Role.SHOPKEEPER -> Routes.SHOPKEEPER_GRAPH
    Role.DELIVERY -> Routes.DELIVERY_GRAPH
    Role.ADMIN -> Routes.ADMIN_GRAPH
    Role.CUSTOMER -> Routes.CUSTOMER_GRAPH
    else -> Routes.AUTH
}

@Composable
fun RoleGraphPlaceholder(title: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text("Coming soon", style = MaterialTheme.typography.bodyMedium)
    }
}
