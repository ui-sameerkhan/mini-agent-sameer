package com.ktc.sitepulse.ui.components

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ktc.sitepulse.ui.theme.SpAmberMid
import com.ktc.sitepulse.ui.theme.SpGreen
import com.ktc.sitepulse.ui.theme.SpGreenDark
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun SitePulseHeader(statusLabel: String, showSignOut: Boolean, onSignOut: () -> Unit) {
    Surface(
        color = SpGreenDark,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .background(Brush.linearGradient(listOf(SpGreenDark, SpGreen)))
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(Brush.linearGradient(listOf(SpAmberMid, SpAmberMid)), RoundedCornerShape(9.dp)),
                    contentAlignment = Alignment.Center,
                ) { Text("📡", fontSize = 16.sp) }
                Column(modifier = Modifier.padding(start = 10.dp)) {
                    Row {
                        Text("Site", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text("Pulse", color = SpAmberMid, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    }
                    Text(
                        "KTC INTERNATIONAL CONTRACTING",
                        color = Color.White.copy(alpha = 0.62f), fontSize = 9.sp
                    )
                    Text(statusLabel, color = Color(0xFFDCFCE7), fontSize = 10.sp)
                }
            }
            if (showSignOut) {
                Button(
                    onClick = onSignOut,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(8.dp),
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

@Composable
fun SitePulseTabBar(current: SpTab, onSelect: (SpTab) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 4.dp) {
        Row(Modifier.fillMaxWidth().navigationBarsPadding()) {
            SpTab.entries.forEach { tab ->
                TabItem(tab = tab, selected = tab == current, onClick = { onSelect(tab) })
            }
        }
    }
}

@Composable
private fun RowScope.TabItem(tab: SpTab, selected: Boolean, onClick: () -> Unit) {
    val color = if (selected) SpGreen else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = Modifier
            .weight(1f)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(tabIcon(tab), contentDescription = tab.label, tint = color, modifier = Modifier.size(20.dp))
        Text(tab.label, color = color, fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold)
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
