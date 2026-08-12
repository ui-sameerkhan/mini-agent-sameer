package com.ktc.sitepulse.ui.dashboard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ktc.sitepulse.data.model.Attendance
import com.ktc.sitepulse.data.model.Blocked
import com.ktc.sitepulse.domain.DateUtils
import com.ktc.sitepulse.domain.Geo
import com.ktc.sitepulse.ui.SitePulseViewModel
import com.ktc.sitepulse.ui.theme.SpAmberMid
import com.ktc.sitepulse.ui.theme.SpBlue
import com.ktc.sitepulse.ui.theme.SpGreenMid
import com.ktc.sitepulse.ui.theme.SpRed

@Composable
fun DashboardScreen(viewModel: SitePulseViewModel) {
    val today by viewModel.todayAttendance.collectAsState()
    val blocked by viewModel.blocked.collectAsState()
    val sites by viewModel.sites.collectAsState()
    val workers by viewModel.workers.collectAsState()

    val todayStr = DateUtils.todayStrUtc()
    val todayBlocked = blocked.filter { it.date == todayStr }
    val present = today.count { it.hasIn }
    val checkedOut = today.count { it.hasOut }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Row(Modifier.fillMaxWidth()) {
                KpiTile("Present (on-site)", present.toString(), SpGreenMid, Modifier.weight(1f))
                KpiTile("Blocked (outside)", todayBlocked.size.toString(), SpRed, Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth().padding(top = 10.dp)) {
                KpiTile("Active Sites", sites.size.toString(), SpBlue, Modifier.weight(1f))
                KpiTile("Checked Out", checkedOut.toString(), SpAmberMid, Modifier.weight(1f))
            }
        }
        item { SectionCard("Today's Attendance by Project") {
            if (today.isEmpty()) {
                Text("No check-ins yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                today.groupBy { it.siteCode }.forEach { (code, rows) ->
                    val siteName = rows.firstOrNull()?.siteName ?: code
                    Text("$siteName ($code)", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                    rows.sortedByDescending { it.lastAction }.forEach { a -> AttendanceRow(a, workers.find { it.id == a.workerId }?.name ?: a.workerId, workers.find { it.id == a.workerId }?.designation ?: "") }
                }
            }
        } }
        item { SectionCard("Blocked Attempts — Outside Site") {
            if (todayBlocked.isEmpty()) {
                Text("None today.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                todayBlocked.sortedByDescending { it.time }.take(15).forEach { b -> BlockedRow(b) }
            }
        } }
    }
}

@Composable
private fun KpiTile(label: String, value: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier.padding(4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(14.dp)) {
            Text(value, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = color)
            Text(label.uppercase(), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Card(
        Modifier.fillMaxWidth().padding(top = 12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            content()
        }
    }
}

@Composable
private fun AttendanceRow(a: Attendance, name: String, designation: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(Modifier.weight(1f)) {
            Text(name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(designation, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
            if (a.hasIn) Text("IN ${DateUtils.formatTimeHm(a.checkIn)}", fontSize = 10.sp, color = SpGreenMid)
            if (a.hasOut) Text("OUT ${DateUtils.formatTimeHm(a.out)}", fontSize = 10.sp, color = SpRed)
        }
    }
}

@Composable
private fun BlockedRow(b: Blocked) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(Modifier.weight(1f)) {
            Text(b.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text("Nearest: ${b.nearestSite} · ${Geo.formatDistance(b.distance)} away", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(DateUtils.formatTimeHm(b.time), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
