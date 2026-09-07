package com.ktc.sitepulse.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Login
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ktc.sitepulse.ui.theme.SpBrandBlue
import com.ktc.sitepulse.ui.theme.SpBrandBlueDark
import com.ktc.sitepulse.ui.theme.SpBrandBlueMid
import com.ktc.sitepulse.ui.theme.SpBrandBlueSoft
import com.ktc.sitepulse.ui.theme.SpBrandGold
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun SitePulseHeader(statusLabel: String, showSignOut: Boolean, onSignOut: () -> Unit) {
    Surface(
        color = SpBrandBlueDark,
        shape = RoundedCornerShape(bottomStart = 22.dp, bottomEnd = 22.dp),
        shadowElevation = 10.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .background(
                    Brush.linearGradient(listOf(SpBrandBlueDark, SpBrandBlueMid, SpBrandBlue)),
                    RoundedCornerShape(bottomStart = 22.dp, bottomEnd = 22.dp),
                )
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .shadow(6.dp, RoundedCornerShape(13.dp), spotColor = Color.Black.copy(alpha = 0.4f))
                        .background(Brush.linearGradient(listOf(SpBrandGold, Color(0xFFFFE07A))), RoundedCornerShape(13.dp)),
                    contentAlignment = Alignment.Center,
                ) { Text("📡", fontSize = 17.sp) }
                Column(modifier = Modifier.padding(start = 11.dp)) {
                    Row {
                        Text("Site", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Pulse", color = SpBrandGold, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    Text(
                        "KTC INTERNATIONAL CONTRACTING",
                        color = Color.White.copy(alpha = 0.62f), fontSize = 9.sp, letterSpacing = 0.4.sp,
                    )
                    Text(statusLabel, color = Color(0xFFDCFCE7), fontSize = 10.sp, modifier = Modifier.padding(top = 1.dp))
                }
            }
            if (showSignOut) {
                Button(
                    onClick = onSignOut,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.14f)),
                    shape = RoundedCornerShape(12.dp),
                ) { Text("Sign Out", color = Color.White, fontSize = 12.sp) }
            }
        }
    }
}

enum class SpTab(val route: String, val label: String) {
    CHECKIN("checkin", "Check-In"),
    DASHBOARD("dashboard", "Dashboard"),
    SITES("sites", "Sites"),
    ATTENDANCE("attendance", "Attendance"),
    WORKERS("workers", "Workers"),
    ROSTER("roster", "Roster"),
}

/**
 * [visibleRoutes] comes from Permissions.visibleRoutes() — the same check that guards the data,
 * so a tab is never shown for a screen the user's role can't load. Hiding it is for usability;
 * the real boundary is the Firestore rules.
 */
@Composable
fun SitePulseTabBar(current: SpTab, visibleRoutes: Set<String>, onSelect: (SpTab) -> Unit) {
    val tabs = SpTab.entries.filter { it.route in visibleRoutes }
    if (tabs.isEmpty()) return
    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp) {
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 6.dp, vertical = 6.dp)) {
            tabs.forEach { tab ->
                TabItem(tab = tab, selected = tab == current, onClick = { onSelect(tab) })
            }
        }
    }
}

@Composable
private fun RowScope.TabItem(tab: SpTab, selected: Boolean, onClick: () -> Unit) {
    val color by animateColorAsState(if (selected) SpBrandBlueMid else MaterialTheme.colorScheme.onSurfaceVariant, label = "tabColor")
    val bg by animateColorAsState(if (selected) SpBrandBlueSoft else Color.Transparent, label = "tabBg")
    Column(
        modifier = Modifier
            .weight(1f)
            .padding(horizontal = 2.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .background(bg, RoundedCornerShape(14.dp))
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(tabIcon(tab), contentDescription = tab.label, tint = color, modifier = Modifier.size(20.dp))
        Text(tab.label, color = color, fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 2.dp))
    }
}

private fun tabIcon(tab: SpTab) = when (tab) {
    SpTab.CHECKIN -> Icons.Filled.LocationOn
    SpTab.DASHBOARD -> Icons.Filled.Dashboard
    SpTab.SITES -> Icons.Filled.LocationOn
    SpTab.ATTENDANCE -> Icons.Filled.Assignment
    SpTab.WORKERS -> Icons.Filled.Groups
    SpTab.ROSTER -> Icons.Filled.Assignment
}

fun todayDisplayString(): String =
    LocalDate.now().format(DateTimeFormatter.ofPattern("EEE, d MMM"))
