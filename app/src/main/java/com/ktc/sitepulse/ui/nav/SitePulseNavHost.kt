package com.ktc.sitepulse.ui.nav

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.collectAsState
import com.ktc.sitepulse.BuildConfig
import com.ktc.sitepulse.data.repo.ParseDiagnostics
import com.ktc.sitepulse.ui.SitePulseViewModel
import com.ktc.sitepulse.util.CrashReporter
import com.ktc.sitepulse.ui.theme.SpAmber
import com.ktc.sitepulse.ui.theme.SpAmberSoft
import com.ktc.sitepulse.ui.theme.SpBrandBlueMid
import com.ktc.sitepulse.ui.theme.SpBrandBlueSoft
import com.ktc.sitepulse.ui.theme.SpRed
import com.ktc.sitepulse.ui.theme.SpRedSoft
import com.ktc.sitepulse.ui.attendance.AttendanceScreen
import com.ktc.sitepulse.ui.checkin.CheckInScreen
import com.ktc.sitepulse.ui.components.SitePulseHeader
import com.ktc.sitepulse.ui.components.SitePulseTabBar
import com.ktc.sitepulse.ui.components.SpTab
import com.ktc.sitepulse.ui.dashboard.DashboardScreen
import com.ktc.sitepulse.ui.forceupdate.ForceUpdateScreen
import com.ktc.sitepulse.ui.login.LoginScreen
import com.ktc.sitepulse.ui.officestaff.OfficeStaffScreen
import com.ktc.sitepulse.ui.roster.RosterScreen
import com.ktc.sitepulse.ui.sites.SitesScreen
import com.ktc.sitepulse.ui.welcome.WelcomeScreen
import com.ktc.sitepulse.ui.workers.WorkersScreen

private const val ROUTE_WELCOME = "welcome"
private const val ROUTE_LOGIN = "login"

@Composable
fun SitePulseRoot() {
    val viewModel: SitePulseViewModel = viewModel()
    val session by viewModel.session.collectAsState()
    val isTimekeeper by viewModel.isTimekeeper.collectAsState()
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // Remote force-update gate — checked before anything else renders, live (not just at cold
    // start), so an admin can shut off this exact installed build at will. See versionGate.
    val versionGate by viewModel.versionGate.collectAsState()
    versionGate?.takeIf { it.minVersionCode > BuildConfig.VERSION_CODE }?.let { gate ->
        ForceUpdateScreen(gate = gate, onSignOut = viewModel::logout)
        return
    }

    val context = LocalContext.current
    var lastCrash by remember { mutableStateOf(CrashReporter.lastCrash(context)) }
    lastCrash?.let { crash ->
        AlertDialog(
            onDismissRequest = {},
            title = { Text("The app crashed last time it closed") },
            text = {
                SelectionContainer {
                    Text(crash, modifier = Modifier.verticalScroll(rememberScrollState()).heightIn(max = 400.dp))
                }
            },
            confirmButton = {
                Button(onClick = { CrashReporter.clear(context); lastCrash = null }) { Text("Dismiss") }
            },
        )
    }

    // isTimekeeper resolves asynchronously (a Firestore doc-existence check, unlike the
    // synchronous email-pattern checks the other roles use) — it's false for an instant right
    // after login, so the first pass below always lands a timekeeper on Check-In like anyone
    // else; the second pass, once it resolves true, catches that and redirects to Attendance,
    // their actual landing screen (no Check-In access — see showTabBar/CheckInScreen gating).
    LaunchedEffect(session.isLoggedIn, isTimekeeper) {
        if (session.isLoggedIn) {
            val target = if (isTimekeeper) SpTab.ATTENDANCE.route else SpTab.CHECKIN.route
            val needsRedirect = currentRoute == ROUTE_WELCOME || currentRoute == ROUTE_LOGIN || currentRoute == null ||
                (isTimekeeper && currentRoute == SpTab.CHECKIN.route)
            if (needsRedirect) {
                navController.navigate(target) {
                    popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                    launchSingleTop = true
                }
            }
        } else if (currentRoute != null && currentRoute != ROUTE_WELCOME && currentRoute != ROUTE_LOGIN) {
            // Signed out from within the app (not a fresh launch) — skip the splash, go straight
            // to Sign In. Welcome only shows once, at the very start of a logged-out session.
            navController.navigate(ROUTE_LOGIN) {
                popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    val statusLabel = when {
        !session.isLoggedIn -> "Login required"
        session.isAdmin -> "🛡️ Admin: ${session.email}"
        isTimekeeper -> "🧾 Timekeeper: ${session.email}"
        session.isOfficeStaff -> "🏢 Office Staff: ${session.email}"
        else -> "🧑‍💼 Supervisor: ${session.email}"
    }

    val showTabBar = session.isLoggedIn && session.isAdmin &&
        SpTab.entries.any { it.route == currentRoute }

    Scaffold(
        topBar = {
            if (currentRoute != ROUTE_WELCOME) {
                SitePulseHeader(statusLabel, showSignOut = session.isLoggedIn, onSignOut = viewModel::logout)
            }
        },
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
            // Admin broadcast — visible to every signed-in role, not just admin (that's the point).
            if (session.isLoggedIn && currentRoute != ROUTE_WELCOME && currentRoute != ROUTE_LOGIN) {
                val announcement by viewModel.activeAnnouncement.collectAsState()
                announcement?.let { a ->
                    Text(
                        "📣 ${a.message} (tap to dismiss)",
                        color = SpBrandBlueMid,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SpBrandBlueSoft)
                            .clickable { viewModel.dismissAnnouncement(a.docId) }
                            .padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            // Raw Firestore error/parse-failure text is only actionable by whoever manages the
            // Firebase project — showing it to supervisors or office staff (who can't do
            // anything about it) just reads as the app being broken. Admin-only.
            if (session.isAdmin) {
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
            }
            NavHost(navController = navController, startDestination = ROUTE_WELCOME) {
                composable(ROUTE_WELCOME) {
                    WelcomeScreen(onGetStarted = { navController.navigate(ROUTE_LOGIN) { launchSingleTop = true } })
                }
                composable(ROUTE_LOGIN) { LoginScreen(viewModel) }
                composable(SpTab.CHECKIN.route) {
                    CheckInScreen(
                        viewModel,
                        onReportArrival = { navController.navigate(SpTab.ROSTER.route) },
                        onOpenOfficeStaff = { navController.navigate("office") },
                    )
                }
                composable("office") {
                    OfficeStaffScreen(viewModel, onBack = { navController.navigate(SpTab.CHECKIN.route) { launchSingleTop = true } })
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
