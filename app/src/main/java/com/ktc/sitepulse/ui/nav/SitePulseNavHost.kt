package com.ktc.sitepulse.ui.nav

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.collectAsState
import com.ktc.sitepulse.data.repo.ParseDiagnostics
import com.ktc.sitepulse.ui.SitePulseViewModel
import com.ktc.sitepulse.ui.theme.SpAmber
import com.ktc.sitepulse.ui.theme.SpAmberSoft
import com.ktc.sitepulse.ui.theme.SpRed
import com.ktc.sitepulse.ui.theme.SpRedSoft
import com.ktc.sitepulse.ui.attendance.AttendanceScreen
import com.ktc.sitepulse.ui.checkin.CheckInScreen
import com.ktc.sitepulse.ui.components.SitePulseHeader
import com.ktc.sitepulse.ui.components.SitePulseTabBar
import com.ktc.sitepulse.ui.components.SpTab
import com.ktc.sitepulse.ui.dashboard.DashboardScreen
import com.ktc.sitepulse.ui.login.LoginScreen
import com.ktc.sitepulse.ui.roster.RosterScreen
import com.ktc.sitepulse.ui.sites.SitesScreen
import com.ktc.sitepulse.ui.workers.WorkersScreen

private const val ROUTE_LOGIN = "login"

@Composable
fun SitePulseRoot() {
    val viewModel: SitePulseViewModel = viewModel()
    val session by viewModel.session.collectAsState()
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    LaunchedEffect(session.isLoggedIn) {
        if (!session.isLoggedIn) {
            navController.navigate(ROUTE_LOGIN) {
                popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                launchSingleTop = true
            }
        } else if (currentRoute == ROUTE_LOGIN || currentRoute == null) {
            navController.navigate(SpTab.CHECKIN.route) {
                popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    val statusLabel = when {
        !session.isLoggedIn -> "Login required"
        session.isAdmin -> "🛡️ Admin: ${session.email}"
        else -> "🧑‍💼 Supervisor: ${session.email}"
    }

    val showTabBar = session.isLoggedIn && session.isAdmin &&
        SpTab.entries.any { it.route == currentRoute }

    Scaffold(
        topBar = { SitePulseHeader(statusLabel, showSignOut = session.isLoggedIn, onSignOut = viewModel::logout) },
        bottomBar = {
            if (showTabBar) {
                val current = SpTab.entries.find { it.route == currentRoute } ?: SpTab.CHECKIN
                SitePulseTabBar(current) { tab ->
                    navController.navigate(tab.route) { launchSingleTop = true }
                }
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            val dataError by viewModel.dataError.collectAsState()
            dataError?.let { msg ->
                Text(
                    "$msg (tap to dismiss)",
                    color = SpRed,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SpRedSoft)
                        .clickable { viewModel.dismissDataError() }
                        .padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            val parseDiagnostic by ParseDiagnostics.lastMessage.collectAsState()
            parseDiagnostic?.let { msg ->
                Text(
                    "$msg (tap to dismiss)",
                    color = SpAmber,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SpAmberSoft)
                        .clickable { ParseDiagnostics.clear() }
                        .padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            NavHost(navController = navController, startDestination = ROUTE_LOGIN) {
                composable(ROUTE_LOGIN) { LoginScreen(viewModel) }
                composable(SpTab.CHECKIN.route) {
                    CheckInScreen(viewModel, onReportArrival = { navController.navigate(SpTab.ROSTER.route) })
                }
                composable(SpTab.DASHBOARD.route) { DashboardScreen(viewModel) }
                composable(SpTab.SITES.route) { SitesScreen(viewModel) }
                composable(SpTab.ATTENDANCE.route) { AttendanceScreen(viewModel) }
                composable(SpTab.WORKERS.route) { WorkersScreen(viewModel) }
                composable(SpTab.ROSTER.route) {
                    RosterScreen(viewModel, onBackToCheckIn = { navController.navigate(SpTab.CHECKIN.route) { launchSingleTop = true } })
                }
            }
        }
    }
}
