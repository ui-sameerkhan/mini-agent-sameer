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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import com.ktc.sitepulse.data.model.Site
import com.ktc.sitepulse.data.model.Worker
import com.ktc.sitepulse.domain.DateUtils
import com.ktc.sitepulse.domain.Geo
import com.ktc.sitepulse.domain.ManpowerRow
import com.ktc.sitepulse.domain.WorkerSearch
import com.ktc.sitepulse.ui.DASHBOARD_ALL_PROJECTS
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
    val sites by viewModel.sites.collectAsState()
    val summary by viewModel.manpowerSummary.collectAsState()
    val deviations by viewModel.siteDeviationsToday.collectAsState()
    val selectedDate by viewModel.dashboardDate.collectAsState()
    val selectedProject by viewModel.dashboardProject.collectAsState()
    val loading by viewModel.dashboardLoading.collectAsState()

    val todayStr = DateUtils.todayStrUtc()
    val isToday = selectedDate == todayStr
    // Blocked attempts and the live activity feed are only meaningful for today — the "blocked"
    // collection is a rolling 14-day live subscription, not a per-date history.
    val scopedBlocked = blocked.filter {
        it.date == selectedDate &&
            (selectedProject == DASHBOARD_ALL_PROJECTS || it.nearestSite.contains(selectedProject, ignoreCase = true))
    }
    val scopedActivity = remember(today, selectedProject) {
        if (selectedProject == DASHBOARD_ALL_PROJECTS) today else today.filter { it.siteCode == selectedProject }
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp),
    ) {
        item {
            ScopeBar(
                date = selectedDate,
                isToday = isToday,
                project = selectedProject,
                sites = sites,
                onDateChange = viewModel::setDashboardDate,
                onProjectChange = viewModel::setDashboardProject,
                onReset = viewModel::resetDashboardScope,
            )
        }

        if (loading) {
            item { LoadingCard("Loading manpower for $selectedDate…") }
        }

        item { AttendanceHeadlineCard(summary.present, summary.totalEmployees, summary.presentPercent, selectedDate, isToday) }

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
                    KpiTile("Blocked", scopedBlocked.size.toString(), SpRed, Modifier.weight(1f))
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

        // Site deviations sit here rather than only in Roster: whoever is watching manpower is
        // the person who needs to see a worker marked somewhere they aren't rostered, and for a
        // timekeeper this dashboard is the whole job.
        if (isToday) {
            item {
                SectionCard("Site Deviations — Marked Off Roster") {
                    if (deviations.isEmpty()) {
                        EmptyState("None today — everyone marked where the roster expects them.")
                    } else {
                        val workerById = remember(workers) { workers.associateBy { it.id } }
                        deviations.take(20).forEach { a ->
                            val w = workerById[a.workerId]
                            Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                                Column(Modifier.weight(1f)) {
                                    Text(w?.name ?: a.workerId, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text(
                                        "Rostered ${a.alignedSite ?: "—"} · marked at ${a.siteCode}",
                                        fontSize = 11.sp, color = SpAmberMid,
                                    )
                                }
                                Text(DateUtils.formatTimeHm(a.checkIn), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        if (deviations.size > 20) {
                            Text(
                                "+ ${deviations.size - 20} more — review them in Roster.",
                                fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                    }
                }
            }
        }
        item { BreakdownCard("Project-wise Manpower", summary.byProject, "No employees assigned to a project yet.") }
        item { BreakdownCard("Supplier-wise Manpower", summary.bySupplier, "No employees on the roster yet.") }

        item { WorkerLocatorCard(viewModel, workers, scopeDate = selectedDate) }

        // Live check-in feed, grouped under project headings the way the roster sheet reads.
        // Only shown for today — the "attendance" live subscription covers today only, so on a
        // past date this would silently show today's rows under yesterday's heading.
        if (isToday) {
            item {
                SectionCard("Today's Attendance by Project") {
                    if (scopedActivity.isEmpty()) {
                        EmptyState(
                            if (selectedProject == DASHBOARD_ALL_PROJECTS) "No check-ins recorded today yet."
                            else "No check-ins recorded at $selectedProject today.",
                        )
                    } else {
                        val workerById = remember(workers) { workers.associateBy { it.id } }
                        // A fully checked-in workforce can be thousands of rows; the Attendance
                        // tab (and its Excel export) holds the complete list, so this is capped.
                        val capped = remember(scopedActivity) { scopedActivity.take(300) }
                        capped.groupBy { it.siteCode }.forEach { (code, rows) ->
                            val siteName = rows.firstOrNull()?.siteName ?: code
                            Text(
                                "$siteName ($code) — ${rows.size}",
                                fontWeight = FontWeight.Bold, fontSize = 13.sp,
                                modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
                            )
                            rows.sortedByDescending { it.lastAction }.forEach { a ->
                                val w = workerById[a.workerId]
                                AttendanceRow(a, w?.name ?: a.workerId, w?.designation ?: "")
                            }
                        }
                        if (scopedActivity.size > capped.size) {
                            Text(
                                "+ ${scopedActivity.size - capped.size} more — open the Attendance tab for the full list.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                    }
                }
            }

            item {
                SectionCard("Blocked Attempts — Outside Site") {
                    if (scopedBlocked.isEmpty()) {
                        EmptyState("None today.")
                    } else {
                        scopedBlocked.sortedByDescending { it.time }.take(15).forEach { b -> BlockedRow(b) }
                    }
                }
            }
        }
    }
}

/**
 * Date + project scope for everything below it. Both default to "today, all projects", so the
 * dashboard opens on the view a supervisor wants 95% of the time and only asks for a tap when
 * they want history or a single job.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun ScopeBar(
    date: String,
    isToday: Boolean,
    project: String,
    sites: List<Site>,
    onDateChange: (String) -> Unit,
    onProjectChange: (String) -> Unit,
    onReset: () -> Unit,
) {
    val context = LocalContext.current
    var projectExpanded by remember { mutableStateOf(false) }
    val projectLabel = if (project == DASHBOARD_ALL_PROJECTS) {
        "All Projects"
    } else {
        sites.firstOrNull { it.code == project }?.let { "${it.name} (${it.code})" } ?: project
    }

    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "VIEWING", fontSize = 10.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f),
                )
                if (!isToday || project != DASHBOARD_ALL_PROJECTS) {
                    Text(
                        "Reset",
                        fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SpBlue,
                        modifier = Modifier.clickable { onReset() },
                    )
                }
            }

            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        val (y, m, d) = date.split("-").map { it.toInt() }
                        DatePickerDialog(context, { _, yy, mm, dd ->
                            onDateChange("%04d-%02d-%02d".format(yy, mm + 1, dd))
                        }, y, m - 1, d).show()
                    },
                    modifier = Modifier.weight(1f),
                ) { Text(if (isToday) "📅 Today" else "📅 $date", maxLines = 1) }

                if (!isToday) {
                    OutlinedButton(onClick = { onDateChange(DateUtils.todayStrUtc()) }) { Text("Today") }
                }
            }

            ExposedDropdownMenuBox(
                expanded = projectExpanded,
                onExpandedChange = { projectExpanded = it },
                modifier = Modifier.padding(top = 8.dp),
            ) {
                OutlinedTextField(
                    value = projectLabel, onValueChange = {}, readOnly = true,
                    label = { Text("Project") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = projectExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                )
                ExposedDropdownMenu(expanded = projectExpanded, onDismissRequest = { projectExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("All Projects") },
                        onClick = { onProjectChange(DASHBOARD_ALL_PROJECTS); projectExpanded = false },
                    )
                    sites.forEach { s ->
                        DropdownMenuItem(
                            text = { Text("${s.name} (${s.code})") },
                            onClick = { onProjectChange(s.code); projectExpanded = false },
                        )
                    }
                }
            }

            if (!isToday) {
                Text(
                    "Showing a past date — the live check-in feed and blocked attempts are only kept for today.",
                    fontSize = 11.sp, color = SpAmberMid, modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun LoadingCard(message: String) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            Text(message, modifier = Modifier.padding(start = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** The one number a supervisor actually opens the app for, with the day's ratio behind it. */
@Composable
private fun AttendanceHeadlineCard(present: Int, total: Int, percent: Int, date: String, isToday: Boolean) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(
                (if (isToday) "TODAY'S MANPOWER · " else "MANPOWER ON ") + date,
                fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
private fun WorkerLocatorCard(viewModel: SitePulseViewModel, workers: List<Worker>, scopeDate: String) {
    val context = LocalContext.current
    var locId by remember { mutableStateOf("") }
    var locDate by remember { mutableStateOf(scopeDate) }
    var result by remember { mutableStateOf<Attendance?>(null) }
    var searched by remember { mutableStateOf(false) }
    var searching by remember { mutableStateOf(false) }

    // Follows the date picked at the top of the dashboard, so changing the scope moves the whole
    // screen to that day at once. Its own picker still overrides it for a one-off lookup.
    LaunchedEffect(scopeDate) { locDate = scopeDate }

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
