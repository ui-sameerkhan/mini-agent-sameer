package com.ktc.sitepulse.ui.attendance

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.ktc.sitepulse.data.model.Attendance
import com.ktc.sitepulse.data.model.Site
import com.ktc.sitepulse.data.model.Worker
import com.ktc.sitepulse.domain.DateUtils
import com.ktc.sitepulse.domain.ReportEngine
import com.ktc.sitepulse.ui.SitePulseViewModel
import com.ktc.sitepulse.ui.theme.SpAmberMid
import kotlinx.coroutines.launch

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(viewModel: SitePulseViewModel) {
    val session by viewModel.session.collectAsState()
    val isTimekeeper by viewModel.isTimekeeper.collectAsState()
    val canCorrect = session.isAdmin || isTimekeeper
    val sites by viewModel.sites.collectAsState()
    val workers by viewModel.workers.collectAsState()
    val statusMessages by viewModel.statusMessages.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedDate by remember { mutableStateOf(DateUtils.todayStrUtc()) }
    var dayRows by remember { mutableStateOf<List<Attendance>>(emptyList()) }
    var dlSite by remember { mutableStateOf("ALL") }
    var dlRange by remember { mutableStateOf("day") }
    var dlSiteExpanded by remember { mutableStateOf(false) }
    var dlRangeExpanded by remember { mutableStateOf(false) }
    var downloading by remember { mutableStateOf(false) }
    var downloadStatus by remember { mutableStateOf("") }
    var editingRecord by remember { mutableStateOf<Attendance?>(null) }
    var addingNew by remember { mutableStateOf(false) }
    var bulkMarking by remember { mutableStateOf(false) }

    LaunchedEffect(selectedDate) { dayRows = viewModel.attendanceForDate(selectedDate) }

    fun refreshDay() { scope.launch { dayRows = viewModel.attendanceForDate(selectedDate) } }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        OutlinedButton(onClick = {
            val (y, m, d) = selectedDate.split("-").map { it.toInt() }
            DatePickerDialog(context, { _, yy, mm, dd ->
                selectedDate = "%04d-%02d-%02d".format(yy, mm + 1, dd)
            }, y, m - 1, d).show()
        }, modifier = Modifier.fillMaxWidth()) { Text("📅 Date: $selectedDate") }

        if (canCorrect) {
            OutlinedButton(onClick = { addingNew = true }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text("+ Add Manual Attendance Record")
            }
            OutlinedButton(onClick = { bulkMarking = true }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text("📋 Bulk Mark Attendance (Multiple Employees)")
            }
            statusMessages["attendanceEditStatus"]?.let { Text(it, modifier = Modifier.padding(top = 6.dp)) }
        }

        Card(Modifier.fillMaxWidth().padding(top = 12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("DOWNLOAD ATTENDANCE REPORT", fontWeight = FontWeight.Bold)

                ExposedDropdownMenuBox(expanded = dlSiteExpanded, onExpandedChange = { dlSiteExpanded = it }, modifier = Modifier.padding(top = 8.dp)) {
                    OutlinedTextField(
                        value = if (dlSite == "ALL") "All Projects" else dlSite, onValueChange = {}, readOnly = true,
                        label = { Text("Project Scope") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dlSiteExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                    )
                    ExposedDropdownMenu(expanded = dlSiteExpanded, onDismissRequest = { dlSiteExpanded = false }) {
                        DropdownMenuItem(text = { Text("All Projects") }, onClick = { dlSite = "ALL"; dlSiteExpanded = false })
                        sites.forEach { s ->
                            DropdownMenuItem(text = { Text(s.code) }, onClick = { dlSite = s.code; dlSiteExpanded = false })
                        }
                    }
                }

                ExposedDropdownMenuBox(expanded = dlRangeExpanded, onExpandedChange = { dlRangeExpanded = it }, modifier = Modifier.padding(top = 8.dp)) {
                    OutlinedTextField(
                        value = if (dlRange == "day") "Selected Date" else "Full Month", onValueChange = {}, readOnly = true,
                        label = { Text("Range") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dlRangeExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                    )
                    ExposedDropdownMenu(expanded = dlRangeExpanded, onDismissRequest = { dlRangeExpanded = false }) {
                        DropdownMenuItem(text = { Text("Selected Date") }, onClick = { dlRange = "day"; dlRangeExpanded = false })
                        DropdownMenuItem(text = { Text("Full Month") }, onClick = { dlRange = "month"; dlRangeExpanded = false })
                    }
                }

                Button(
                    onClick = {
                        downloading = true
                        downloadStatus = "Generating…"
                        scope.launch {
                            try {
                                val dateOrMonth = if (dlRange == "day") selectedDate else DateUtils.monthOf(selectedDate)
                                val file = viewModel.generateReport(
                                    ReportEngine.Params(dateOrMonth, dlRange, dlSite, generatedBy = viewModel.session.value.email)
                                )
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share attendance report"))
                                downloadStatus = "✅ Report ready: ${file.name}"
                            } catch (e: Throwable) {
                                // Apache POI on Android can throw Error subtypes (e.g. NoClassDefFoundError
                                // for a class stripped by shrinking) that a plain `catch (Exception)` misses
                                // entirely, crashing the whole app instead of just failing this one action.
                                // ExceptionInInitializerError in particular carries the real failure in
                                // .cause with a null message of its own — walk the whole chain so it's
                                // actually diagnosable without logcat access.
                                downloadStatus = "❌ ${e.diagnosticChain()}"
                            } finally {
                                downloading = false
                            }
                        }
                    },
                    enabled = !downloading,
                    colors = ButtonDefaults.buttonColors(containerColor = com.ktc.sitepulse.ui.theme.SpBrandBlueMid),
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                ) { Text(if (downloading) "Generating…" else "Download Excel Report") }

                if (downloadStatus.isNotBlank()) Text(downloadStatus, modifier = Modifier.padding(top = 6.dp))
            }
        }

        if (dayRows.isEmpty()) {
            Text("No records.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 12.dp))
        } else {
            // A large workforce with everyone checked in the same day can put thousands of rows
            // through this plain (non-lazy) Column — both the eager rendering and an O(n) find()
            // per row get expensive at that scale, so look workers up by a map instead and cap
            // what's rendered on-screen (the downloaded Excel report is unaffected either way).
            val workerById = workers.associateBy { it.id }
            val cappedRows = dayRows.take(300)
            cappedRows.groupBy { it.siteCode }.forEach { (code, rows) ->
                Card(Modifier.fillMaxWidth().padding(top = 12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Text("${rows.firstOrNull()?.siteName ?: code} ($code)", fontWeight = FontWeight.Bold)
                        rows.forEach { a ->
                            val w = workerById[a.workerId]
                            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(w?.name ?: a.workerId)
                                    if (a.corrected) {
                                        Text("✏️ Manually corrected", color = SpAmberMid, fontSize = 10.sp)
                                    }
                                }
                                Text(DateUtils.formatTimeHm(a.checkIn))
                                Text(" / ")
                                Text(DateUtils.formatTimeHm(a.out))
                                if (canCorrect) {
                                    OutlinedButton(onClick = { editingRecord = a }, modifier = Modifier.padding(start = 8.dp)) { Text("Edit") }
                                }
                            }
                        }
                    }
                }
            }
            if (dayRows.size > cappedRows.size) {
                Text(
                    "+ ${dayRows.size - cappedRows.size} more record(s) not shown here — download the Excel report for the full list.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 10.dp),
                )
            }
        }
    }

    if (addingNew) {
        EditAttendanceDialog(
            date = selectedDate, existing = null, initialWorkerId = "", sites = sites,
            onDismiss = { addingNew = false },
            onSave = { workerId, fields ->
                scope.launch {
                    viewModel.correctAttendance(selectedDate, workerId, fields)
                    refreshDay()
                    addingNew = false
                }
            },
        )
    }
    editingRecord?.let { record ->
        EditAttendanceDialog(
            date = record.date, existing = record, initialWorkerId = record.workerId, sites = sites,
            onDismiss = { editingRecord = null },
            onSave = { workerId, fields ->
                scope.launch {
                    viewModel.correctAttendance(record.date, workerId, fields)
                    refreshDay()
                    editingRecord = null
                }
            },
        )
    }
    if (bulkMarking) {
        BulkAttendanceDialog(
            date = selectedDate, sites = sites, workers = workers,
            onDismiss = { bulkMarking = false },
            onSave = { workerIds, fields ->
                scope.launch {
                    viewModel.correctAttendanceBulk(selectedDate, workerIds, fields)
                    refreshDay()
                    bulkMarking = false
                }
            },
        )
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun BulkAttendanceDialog(
    date: String,
    sites: List<Site>,
    workers: List<Worker>,
    onDismiss: () -> Unit,
    onSave: (workerIds: List<String>, fields: Map<String, Any?>) -> Unit,
) {
    val context = LocalContext.current
    var siteCode by remember { mutableStateOf("") }
    var siteExpanded by remember { mutableStateOf(false) }
    var shift by remember { mutableStateOf("Day") }
    var shiftExpanded by remember { mutableStateOf(false) }
    var inTime by remember { mutableStateOf("") }
    var outTime by remember { mutableStateOf("") }
    var outNextDay by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    val selected = remember { mutableStateListOf<String>() }

    fun pickTime(current: String, onPicked: (String) -> Unit) {
        val parts = current.split(":").mapNotNull { it.toIntOrNull() }
        TimePickerDialog(context, { _, hh, mm -> onPicked("%02d:%02d".format(hh, mm)) }, parts.getOrNull(0) ?: 9, parts.getOrNull(1) ?: 0, true).show()
    }

    val filteredWorkers = workers.filter { w ->
        (siteCode.isBlank() || w.site == siteCode) &&
            (search.isBlank() || w.name.contains(search, true) || w.id.contains(search, true))
    }
    val visibleWorkers = filteredWorkers.take(200)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Bulk Mark Attendance — $date") },
        text = {
            Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState())) {
                ExposedDropdownMenuBox(expanded = siteExpanded, onExpandedChange = { siteExpanded = it }) {
                    OutlinedTextField(
                        value = siteCode.ifBlank { "Select Site *" }, onValueChange = {}, readOnly = true, label = { Text("Site") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = siteExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                    )
                    ExposedDropdownMenu(expanded = siteExpanded, onDismissRequest = { siteExpanded = false }) {
                        sites.forEach { s ->
                            DropdownMenuItem(text = { Text("${s.code} — ${s.name}") }, onClick = { siteCode = s.code; siteExpanded = false; selected.clear() })
                        }
                    }
                }
                ExposedDropdownMenuBox(expanded = shiftExpanded, onExpandedChange = { shiftExpanded = it }, modifier = Modifier.padding(top = 8.dp)) {
                    OutlinedTextField(
                        value = shift, onValueChange = {}, readOnly = true, label = { Text("Shift") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = shiftExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                    )
                    ExposedDropdownMenu(expanded = shiftExpanded, onDismissRequest = { shiftExpanded = false }) {
                        listOf("Day", "Night").forEach { s -> DropdownMenuItem(text = { Text(s) }, onClick = { shift = s; shiftExpanded = false }) }
                    }
                }
                OutlinedButton(onClick = { pickTime(inTime) { inTime = it } }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Text(if (inTime.isBlank()) "Set Check-In Time (applies to everyone selected)" else "Check-In: $inTime")
                }
                OutlinedButton(onClick = { pickTime(outTime) { outTime = it } }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Text(if (outTime.isBlank()) "Set Check-Out Time (optional)" else "Check-Out: $outTime")
                }
                if (outTime.isNotBlank()) {
                    Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = outNextDay, onCheckedChange = { outNextDay = it })
                        Text("Check-out is on the next calendar day (night shift)", fontSize = 11.sp)
                    }
                }

                OutlinedTextField(
                    search, { search = it }, label = { Text("Search workers…") },
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp), singleLine = true,
                )
                Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${selected.size} selected", Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    TextButton(onClick = { selected.clear(); selected.addAll(visibleWorkers.map { it.id }) }) { Text("Select Shown") }
                    TextButton(onClick = { selected.clear() }) { Text("Clear") }
                }
                if (siteCode.isBlank()) {
                    Text("Select a site to list its workers.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                } else {
                    visibleWorkers.forEach { w ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = selected.contains(w.id),
                                onCheckedChange = { checked -> if (checked) selected.add(w.id) else selected.remove(w.id) },
                            )
                            Text("${w.name} (ID ${w.id})", Modifier.weight(1f), fontSize = 13.sp)
                        }
                    }
                    if (filteredWorkers.size > visibleWorkers.size) {
                        Text(
                            "+ ${filteredWorkers.size - visibleWorkers.size} more — search to narrow.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
                Text(
                    "⚠️ Applies the same check-in/out time to every selected worker — tagged as a manual correction and visible on every Excel export.",
                    color = SpAmberMid, fontSize = 11.sp, modifier = Modifier.padding(top = 10.dp),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val site = sites.find { it.code == siteCode }
                    val fields = mutableMapOf<String, Any?>(
                        "siteCode" to siteCode, "siteName" to (site?.name ?: ""),
                        "shift" to shift, "markedVia" to "manual", "lastAction" to DateUtils.nowIso(),
                    )
                    if (inTime.isNotBlank()) {
                        val (h, m) = inTime.split(":").map { it.toInt() }
                        fields["in"] = DateUtils.isoFromLocalTime(date, h, m)
                    }
                    if (outTime.isNotBlank()) {
                        val (h, m) = outTime.split(":").map { it.toInt() }
                        fields["out"] = DateUtils.isoFromLocalTime(date, h, m, plusDays = if (outNextDay) 1 else 0)
                    }
                    onSave(selected.toList(), fields)
                },
                enabled = siteCode.isNotBlank() && selected.isNotEmpty() && inTime.isNotBlank(),
            ) { Text("Mark ${selected.size} Worker(s)") }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun EditAttendanceDialog(
    date: String,
    existing: Attendance?,
    initialWorkerId: String,
    sites: List<Site>,
    onDismiss: () -> Unit,
    onSave: (workerId: String, fields: Map<String, Any?>) -> Unit,
) {
    val context = LocalContext.current
    var workerId by remember { mutableStateOf(initialWorkerId) }
    var siteCode by remember { mutableStateOf(existing?.siteCode ?: "") }
    var siteExpanded by remember { mutableStateOf(false) }
    var shift by remember { mutableStateOf(existing?.shift ?: "Day") }
    var shiftExpanded by remember { mutableStateOf(false) }
    val inHm = DateUtils.localHourMinute(existing?.checkIn)
    val outHm = DateUtils.localHourMinute(existing?.out)
    var inTime by remember { mutableStateOf(inHm?.let { "%02d:%02d".format(it.first, it.second) } ?: "") }
    var outTime by remember { mutableStateOf(outHm?.let { "%02d:%02d".format(it.first, it.second) } ?: "") }
    var outNextDay by remember { mutableStateOf(false) }

    fun pickTime(current: String, onPicked: (String) -> Unit) {
        val parts = current.split(":").mapNotNull { it.toIntOrNull() }
        val h = parts.getOrNull(0) ?: 9
        val m = parts.getOrNull(1) ?: 0
        TimePickerDialog(context, { _, hh, mm -> onPicked("%02d:%02d".format(hh, mm)) }, h, m, true).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add Manual Attendance Record" else "Edit Attendance Record") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    workerId, { workerId = it }, label = { Text("Worker ID") },
                    enabled = existing == null, modifier = Modifier.fillMaxWidth(),
                )
                Text("Date: $date", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp, bottom = 4.dp))
                ExposedDropdownMenuBox(expanded = siteExpanded, onExpandedChange = { siteExpanded = it }, modifier = Modifier.padding(top = 4.dp)) {
                    OutlinedTextField(
                        value = siteCode.ifBlank { "Select Site" }, onValueChange = {}, readOnly = true, label = { Text("Site") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = siteExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                    )
                    ExposedDropdownMenu(expanded = siteExpanded, onDismissRequest = { siteExpanded = false }) {
                        sites.forEach { s -> DropdownMenuItem(text = { Text("${s.code} — ${s.name}") }, onClick = { siteCode = s.code; siteExpanded = false }) }
                    }
                }
                ExposedDropdownMenuBox(expanded = shiftExpanded, onExpandedChange = { shiftExpanded = it }, modifier = Modifier.padding(top = 8.dp)) {
                    OutlinedTextField(
                        value = shift, onValueChange = {}, readOnly = true, label = { Text("Shift") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = shiftExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                    )
                    ExposedDropdownMenu(expanded = shiftExpanded, onDismissRequest = { shiftExpanded = false }) {
                        listOf("Day", "Night").forEach { s -> DropdownMenuItem(text = { Text(s) }, onClick = { shift = s; shiftExpanded = false }) }
                    }
                }
                OutlinedButton(onClick = { pickTime(inTime) { inTime = it } }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Text(if (inTime.isBlank()) "Set Check-In Time" else "Check-In: $inTime")
                }
                OutlinedButton(onClick = { pickTime(outTime) { outTime = it } }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Text(if (outTime.isBlank()) "Set Check-Out Time" else "Check-Out: $outTime")
                }
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = outNextDay, onCheckedChange = { outNextDay = it })
                    Text("Check-out is on the next calendar day (night shift)", fontSize = 11.sp)
                }
                Text(
                    "⚠️ This is a manual correction — it's tagged as such and visible on every Excel export for audit.",
                    color = SpAmberMid, fontSize = 11.sp, modifier = Modifier.padding(top = 10.dp),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val site = sites.find { it.code == siteCode }
                    val fields = mutableMapOf<String, Any?>(
                        "siteCode" to siteCode, "siteName" to (site?.name ?: existing?.siteName ?: ""),
                        "shift" to shift, "markedVia" to "manual", "lastAction" to DateUtils.nowIso(),
                    )
                    if (inTime.isNotBlank()) {
                        val (h, m) = inTime.split(":").map { it.toInt() }
                        fields["in"] = DateUtils.isoFromLocalTime(date, h, m)
                    }
                    if (outTime.isNotBlank()) {
                        val (h, m) = outTime.split(":").map { it.toInt() }
                        fields["out"] = DateUtils.isoFromLocalTime(date, h, m, plusDays = if (outNextDay) 1 else 0)
                    }
                    onSave(workerId.trim(), fields)
                },
                enabled = workerId.isNotBlank() && siteCode.isNotBlank(),
            ) { Text("Save") }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Walks the full cause chain — ExceptionInInitializerError's own .message is always null; the real reason is in .cause. */
private fun Throwable.diagnosticChain(): String {
    val parts = mutableListOf<String>()
    var t: Throwable? = this
    var depth = 0
    while (t != null && depth < 6) {
        parts.add("${t::class.simpleName}: ${t.message}")
        t = t.cause.takeIf { it !== t }
        depth++
    }
    return parts.joinToString(" ← caused by ")
}
