package com.ktc.sitepulse.ui.dashboard

import android.app.DatePickerDialog
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ktc.sitepulse.data.model.Attendance
import com.ktc.sitepulse.data.model.Blocked
import com.ktc.sitepulse.data.model.Worker
import com.ktc.sitepulse.domain.DateUtils
import com.ktc.sitepulse.domain.Geo
import com.ktc.sitepulse.domain.ManpowerRow
import com.ktc.sitepulse.domain.WorkerSearch
import com.ktc.sitepulse.ui.SitePulseViewModel
import com.ktc.sitepulse.ui.theme.SpAmberMid
import com.ktc.sitepulse.ui.theme.SpBlue
import com.ktc.sitepulse.ui.theme.SpGreenMid
import com.ktc.sitepulse.ui.theme.SpMuted
import com.ktc.sitepulse.ui.theme.SpRed

@Composable
fun DashboardScreen(viewModel: SitePulseViewModel) {
    val today by viewModel.todayAttendance.collectAsState()
    val blocked by viewModel.blocked.collectAsState()
    val workers by viewModel.workers.collectAsState()
    val summary by viewModel.manpowerSummary.collectAsState()

    val todayStr = DateUtils.todayStrUtc()
    val todayBlocked = blocked.filter { it.date == todayStr }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp),
    ) {
        item { AttendanceHeadlineCard(summary.present, summary.totalEmployees, summary.presentPercent, todayStr) }

        item {
            // Three per row keeps every figure readable on a small site phone without the
            // horizontal scrolling that a real table would force.
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    KpiTile("Present", summary.present.toString(), SpGreenMid, Modifier.weight(1f))
                    KpiTile("Absent", summary.absent.toString(), SpRed, Modifier.weight(1f))
                    KpiTile("On Leave", summary.onLeave.toString(), SpBlue, Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    KpiTile("Late", summary.late.toString(), SpAmberMid, Modifier.weight(1f))
                    KpiTile("Checked Out", summary.checkedOut.toString(), SpMuted, Modifier.weight(1f))
                    KpiTile("Blocked", todayBlocked.size.toString(), SpRed, Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    KpiTile("Total Staff", summary.totalEmployees.toString(), SpBlue, Modifier.weight(1f))
                    KpiTile("KTC", summary.ktcEmployees.toString(), SpGreenMid, Modifier.weight(1f))
                    KpiTile("Supplier", summary.supplierEmployees.toString(), SpAmberMid, Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    KpiTile("Active Projects", summary.activeProjects.toString(), SpGreenMid, Modifier.weight(1f))
                    KpiTile("Total Projects", summary.totalProjects.toString(), SpMuted, Modifier.weight(1f))
                    // Keeps the last row aligned to the same 3-column grid as the ones above.
                    Box(Modifier.weight(1f))
                }
            }
        }

        item { BreakdownCard("Trade-wise Manpower", summary.byTrade, "No employees on the roster yet.") }
        item { BreakdownCard("Project-wise Manpower", summary.byProject, "No employees assigned to a project yet.") }
        item { BreakdownCard("Supplier-wise Manpower", summary.bySupplier, "No employees on the roster yet.") }

        item { WorkerLocatorCard(viewModel, workers) }

        item {
            SectionCard("Recent Attendance Activity") {
                if (today.isEmpty()) {
                    EmptyState("No check-ins recorded today yet.")
                } else {
                    // A fully checked-in workforce can be thousands of rows; the Attendance tab
                    // (and its Excel export) holds the complete list, so this stays a preview.
                    val workerById = remember(workers) { workers.associateBy { it.id } }
                    val recent = remember(today) { today.sortedByDescending { it.lastAction }.take(20) }
                    recent.forEach { a ->
                        val w = workerById[a.workerId]
                        AttendanceRow(a, w?.name ?: a.workerId, w?.designation ?: "")
                    }
                    if (today.size > recent.size) {
                        Text(
                            "+ ${today.size - recent.size} more — open the Attendance tab for the full list.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }
        }

        item {
            SectionCard("Blocked Attempts — Outside Site") {
                if (todayBlocked.isEmpty()) {
                    EmptyState("None today.")
                } else {
                    todayBlocked.sortedByDescending { it.time }.take(15).forEach { b -> BlockedRow(b) }
                }
            }
        }
    }
}

/** The one number a supervisor actually opens the app for, with the day's ratio behind it. */
@Composable
private fun AttendanceHeadlineCard(present: Int, total: Int, percent: Int, date: String) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(Modifier.padding(18.dp)) {
            Text("TODAY'S MANPOWER · $date", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.Bottom) {
                Text("$present", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = SpGreenMid)
                Text(
                    " / $total on site",
                    fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 7.dp),
                )
            }
            ProgressBar(percent)
            Text("$percent% of the active roster has checked in.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
        }
    }
}

@Composable
private fun ProgressBar(percent: Int) {
    Box(
        Modifier.fillMaxWidth().height(8.dp).padding(top = 2.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Box(
            Modifier.fillMaxWidth(percent.coerceIn(0, 100) / 100f).height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(SpGreenMid),
        )
    }
}

@Composable
private fun KpiTile(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(vertical = 14.dp, horizontal = 10.dp)) {
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = color, maxLines = 1)
            Text(
                label.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

/**
 * A collapsible manpower table. Collapsed by default past the top few rows so a company with
 * forty trades doesn't bury the rest of the dashboard under one section — tapping the header
 * expands the full list.
 */
@Composable
private fun BreakdownCard(title: String, rows: List<ManpowerRow>, emptyMessage: String) {
    var expanded by remember { mutableStateOf(false) }
    val previewCount = 5
    val visible = if (expanded) rows else rows.take(previewCount)

    Card(
        Modifier.fillMaxWidth().animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth().clickable(enabled = rows.size > previewCount) { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    title.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f),
                )
                if (rows.size > previewCount) {
                    Text(
                        if (expanded) "Show less ▲" else "All ${rows.size} ▼",
                        fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SpBlue,
                    )
                }
            }

            if (rows.isEmpty()) {
                EmptyState(emptyMessage)
            } else {
                BreakdownHeaderRow()
                visible.forEach { row -> BreakdownDataRow(row) }
                if (!expanded && rows.size > previewCount) {
                    Text(
                        "+ ${rows.size - previewCount} more",
                        fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
                BreakdownTotalRow(rows)
            }
        }
    }
}

@Composable
private fun BreakdownHeaderRow() {
    Row(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 2.dp)) {
        Text("", Modifier.weight(1f))
        listOf("TOT", "PRE", "ABS", "LVE").forEach { h ->
            Text(
                h, Modifier.weight(0.24f), fontSize = 9.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.End,
            )
        }
    }
}

@Composable
private fun BreakdownDataRow(row: ManpowerRow) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(row.label, Modifier.weight(1f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
        NumCell(row.total.toString(), MaterialTheme.colorScheme.onSurface)
        NumCell(row.present.toString(), SpGreenMid)
        NumCell(row.absent.toString(), if (row.absent > 0) SpRed else MaterialTheme.colorScheme.onSurfaceVariant)
        NumCell(row.onLeave.toString(), if (row.onLeave > 0) SpBlue else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun BreakdownTotalRow(rows: List<ManpowerRow>) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("TOTAL", Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        NumCell(rows.sumOf { it.total }.toString(), MaterialTheme.colorScheme.onSurface, bold = true)
        NumCell(rows.sumOf { it.present }.toString(), SpGreenMid, bold = true)
        NumCell(rows.sumOf { it.absent }.toString(), SpRed, bold = true)
        NumCell(rows.sumOf { it.onLeave }.toString(), SpBlue, bold = true)
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.NumCell(value: String, color: Color, bold: Boolean = false) {
    Text(
        value, Modifier.weight(0.24f), fontSize = 13.sp,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium,
        color = color, textAlign = TextAlign.End,
    )
}

@Composable
private fun EmptyState(message: String) {
    Text(
        message,
        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp,
        modifier = Modifier.padding(vertical = 10.dp),
    )
}

@Composable
private fun SectionCard(title: String, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            content()
        }
    }
}

@Composable
private fun WorkerLocatorCard(viewModel: SitePulseViewModel, workers: List<Worker>) {
    val context = LocalContext.current
    var locId by remember { mutableStateOf("") }
    var locDate by remember { mutableStateOf(DateUtils.todayStrUtc()) }
    var result by remember { mutableStateOf<Attendance?>(null) }
    var searched by remember { mutableStateOf(false) }
    var searching by remember { mutableStateOf(false) }

    // Guards against a stale response landing after the id/date has since changed again.
    LaunchedEffect(locId, locDate) {
        if (locId.isBlank()) {
            result = null
            searched = false
            searching = false
            return@LaunchedEffect
        }
        val requestedId = locId
        val requestedDate = locDate
        searching = true
        val found = viewModel.locateWorker(requestedId, requestedDate)
        if (requestedId == locId && requestedDate == locDate) {
            result = found
            searched = true
            searching = false
        }
    }

    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("WORKER LOCATOR", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(
                value = locId, onValueChange = { locId = it },
                placeholder = { Text("Worker ID") },
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

            when {
                searching -> Text("⏳ Searching…", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 10.dp))
                searched -> {
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
}

@Composable
private fun AttendanceRow(a: Attendance, name: String, designation: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Column(Modifier.weight(1f)) {
            Text(name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(
                listOfNotNull(designation.takeIf { it.isNotBlank() }, a.siteCode.takeIf { it.isNotBlank() }).joinToString(" · "),
                fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            if (a.hasIn) Text("IN ${DateUtils.formatTimeHm(a.checkIn)}", fontSize = 10.sp, color = SpGreenMid)
            if (a.hasOut) Text("OUT ${DateUtils.formatTimeHm(a.out)}", fontSize = 10.sp, color = SpRed)
        }
    }
}

@Composable
private fun BlockedRow(b: Blocked) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Column(Modifier.weight(1f)) {
            Text(b.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text("Nearest: ${b.nearestSite} · ${Geo.formatDistance(b.distance)} away", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(DateUtils.formatTimeHm(b.time), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
