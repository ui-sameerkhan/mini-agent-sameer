package com.ktc.sitepulse.ui.dashboard

import android.app.DatePickerDialog
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ktc.sitepulse.data.model.Attendance
import com.ktc.sitepulse.data.model.Blocked
import com.ktc.sitepulse.domain.DateUtils
import com.ktc.sitepulse.domain.Geo
import com.ktc.sitepulse.domain.WorkerSearch
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
        item { WorkerLocatorCard(viewModel, workers) }
        item {
            Row(Modifier.fillMaxWidth().padding(top = 12.dp)) {
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
private fun WorkerLocatorCard(viewModel: SitePulseViewModel, workers: List<com.ktc.sitepulse.data.model.Worker>) {
    val context = LocalContext.current
    var locId by remember { mutableStateOf("") }
    var locDate by remember { mutableStateOf(DateUtils.todayStrUtc()) }
    var result by remember { mutableStateOf<Attendance?>(null) }
    var searched by remember { mutableStateOf(false) }

    // Guards against a stale response landing after the id/date has since changed again.
    LaunchedEffect(locId, locDate) {
        if (locId.isBlank()) {
            result = null
            searched = false
            return@LaunchedEffect
        }
        val requestedId = locId
        val requestedDate = locDate
        val found = viewModel.locateWorker(requestedId, requestedDate)
        if (requestedId == locId && requestedDate == locDate) {
            result = found
            searched = true
        }
    }

    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp)) {
            Text("WORKER LOCATOR", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(
                value = locId, onValueChange = { locId = it },
                placeholder = { Text("WORKER ID") },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp), singleLine = true,
            )
            OutlinedButton(
                onClick = {
                    val (y, m, d) = locDate.split("-").map { it.toInt() }
                    DatePickerDialog(context, { _, yy, mm, dd ->
                        locDate = "%04d-%02d-%02d".format(yy, mm + 1, dd)
                    }, y, m - 1, d).show()
                },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) { Text("📅 $locDate") }

            if (searched) {
                val worker = WorkerSearch.findExact(workers, locId)
                Column(Modifier.padding(top = 10.dp)) {
                    when {
                        worker == null -> Text("❌ No worker found with ID \"$locId\".", color = SpRed)
                        result == null -> Text("⚠ ${worker.name} has no attendance record on $locDate.", color = SpAmberMid)
                        else -> {
                            val a = result!!
                            Text("${worker.name} — ${worker.designation}", fontWeight = FontWeight.Bold)
                            Text("${a.siteName} (${a.siteCode})", color = SpBlue, fontSize = 13.sp)
                            if (a.hasIn) Text("IN: ${DateUtils.formatTimeHm(a.checkIn)}" + (a.inGps?.let { " · ${it.lat}, ${it.lng}" } ?: ""), color = SpGreenMid, fontSize = 12.sp)
                            if (a.hasOut) Text("OUT: ${DateUtils.formatTimeHm(a.out)}" + (a.outGps?.let { " · ${it.lat}, ${it.lng}" } ?: ""), color = SpRed, fontSize = 12.sp)
                        }
                    }
                }
            }
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
