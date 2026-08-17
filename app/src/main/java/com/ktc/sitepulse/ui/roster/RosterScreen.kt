package com.ktc.sitepulse.ui.roster

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.ktc.sitepulse.ui.ImportKind
import com.ktc.sitepulse.data.model.Leave
import com.ktc.sitepulse.data.model.Worker
import com.ktc.sitepulse.domain.DateUtils
import com.ktc.sitepulse.domain.WorkerSearch
import com.ktc.sitepulse.ui.SitePulseViewModel
import com.ktc.sitepulse.ui.components.ImportConfirmDialog
import com.ktc.sitepulse.ui.components.NotificationsCard
import com.ktc.sitepulse.ui.theme.SpAmberMid
import com.ktc.sitepulse.ui.theme.SpBrandBlueMid
import com.ktc.sitepulse.ui.theme.SpGreenMid
import com.ktc.sitepulse.ui.theme.SpRed
import com.ktc.sitepulse.util.displayName
import kotlinx.coroutines.launch

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun RosterScreen(viewModel: SitePulseViewModel, onBackToCheckIn: () -> Unit) {
    val session by viewModel.session.collectAsState()

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        OutlinedButton(onClick = onBackToCheckIn) { Text("← Back to Check-In") }

        if (session.isAdmin) {
            RosterAdminPanel(viewModel)
        } else {
            RosterSupervisorPanel(viewModel)
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun RosterSupervisorPanel(viewModel: SitePulseViewModel) {
    val sites by viewModel.sites.collectAsState()
    val workers by viewModel.workers.collectAsState()
    val statusMessages by viewModel.statusMessages.collectAsState()

    var site by remember { mutableStateOf("") }
    var siteExpanded by remember { mutableStateOf(false) }
    var workerId by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var trade by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(DateUtils.todayStrUtc()) }

    val existing = WorkerSearch.findExact(workers, workerId)

    NotificationsCard(
        viewModel, "Get a push notification for admin announcements — even when the app is closed.",
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
    )

    Text("Report New Arrival", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 20.dp, bottom = 8.dp))
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)) {
        Column(Modifier.padding(16.dp)) {
            ExposedDropdownMenuBox(expanded = siteExpanded, onExpandedChange = { siteExpanded = it }) {
                OutlinedTextField(
                    value = site.ifBlank { "Select Your Project Code" }, onValueChange = {}, readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = siteExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                )
                ExposedDropdownMenu(expanded = siteExpanded, onDismissRequest = { siteExpanded = false }) {
                    sites.forEach { s -> DropdownMenuItem(text = { Text(s.code) }, onClick = { site = s.code; siteExpanded = false }) }
                }
            }
            OutlinedTextField(
                workerId, { workerId = it }, label = { Text("Worker ID") },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp), singleLine = true,
            )
            val verifyText = when {
                workerId.isBlank() -> ""
                existing != null -> "✅ ${existing.name} — ${existing.designation} (currently: ${existing.site ?: "unassigned"})"
                else -> "🆕 New worker — ID not in the system yet."
            }
            if (verifyText.isNotBlank()) {
                Text(verifyText, color = if (existing != null) SpGreenMid else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
            }
            if (existing == null && workerId.isNotBlank()) {
                OutlinedTextField(name, { name = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                OutlinedTextField(trade, { trade = it }, label = { Text("Designation / Trade") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
            }
            Button(
                onClick = { viewModel.submitNewArrival(site, workerId, name, trade, date) },
                colors = ButtonDefaults.buttonColors(containerColor = SpBrandBlueMid),
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            ) { Text("Report New Arrival") }
            statusMessages["arrivalStatus"]?.let { Text(it, modifier = Modifier.padding(top = 8.dp)) }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun RosterAdminPanel(viewModel: SitePulseViewModel) {
    val statusMessages by viewModel.statusMessages.collectAsState()
    val pendingArrivals by viewModel.pendingArrivals.collectAsState()
    val pendingLeaveRequests by viewModel.pendingLeaveRequests.collectAsState()
    val siteDeviationsToday by viewModel.siteDeviationsToday.collectAsState()
    val pendingImport by viewModel.pendingImport.collectAsState()
    val workers by viewModel.workers.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val rosterPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.startImport(ImportKind.ROSTER, it, it.displayName(context)) }
    }
    val backupPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.startBackupRestore(it) }
    }
    val pendingRestore by viewModel.pendingRestore.collectAsState()
    val versionGate by viewModel.versionGate.collectAsState()

    var announcementText by remember { mutableStateOf("") }
    var minVersionCodeText by remember { mutableStateOf("") }
    var updateUrlText by remember { mutableStateOf("") }
    var updateMessageText by remember { mutableStateOf("") }
    var leaveId by remember { mutableStateOf("") }
    var leaveFrom by remember { mutableStateOf("") }
    var leaveTo by remember { mutableStateOf("") }
    var leaveReason by remember { mutableStateOf("") }
    var rosterSearch by remember { mutableStateOf("") }
    var backupInProgress by remember { mutableStateOf(false) }
    var backupStatus by remember { mutableStateOf("") }
    var leaveHistory by remember { mutableStateOf<List<Leave>>(emptyList()) }
    var leaveHistorySearch by remember { mutableStateOf("") }
    var leaveHistoryRefresh by remember { mutableIntStateOf(0) }

    LaunchedEffect(leaveHistoryRefresh) { leaveHistory = viewModel.allLeaveHistory() }

    NotificationsCard(
        viewModel, "Get a push notification the moment a supervisor reports a new arrival, a leave request comes in, or a site deviation is flagged — even when the app is closed.",
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
    )

    Card(Modifier.fillMaxWidth().padding(top = 12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text("SEND ANNOUNCEMENT", fontWeight = FontWeight.Bold)
            Text(
                "Broadcasts a message to every signed-in user as an in-app banner, and as a push to anyone who's enabled notifications.",
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 6.dp),
            )
            OutlinedTextField(
                announcementText, { announcementText = it }, label = { Text("Message") },
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = { viewModel.sendAnnouncement(announcementText); announcementText = "" },
                colors = ButtonDefaults.buttonColors(containerColor = SpAmberMid),
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            ) { Text("📣 Send to Everyone") }
            statusMessages["announcementStatus"]?.let { Text(it, modifier = Modifier.padding(top = 8.dp)) }
        }
    }

    Card(Modifier.fillMaxWidth().padding(top = 12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text("FORCE UPDATE / APP VERSION", fontWeight = FontWeight.Bold)
            Text(
                "Blocks anyone on an older build the moment you save this — even if their app is already open. "
                    + "This device is currently running version code ${com.ktc.sitepulse.BuildConfig.VERSION_CODE} (${com.ktc.sitepulse.BuildConfig.VERSION_NAME}).",
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 6.dp),
            )
            Text(
                "Currently enforced minimum: ${versionGate?.minVersionCode?.takeIf { it > 0 } ?: "none"}",
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp),
            )
            OutlinedTextField(
                minVersionCodeText, { minVersionCodeText = it.filter { c -> c.isDigit() } },
                label = { Text("Minimum Version Code (0 = no gate)") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                updateUrlText, { updateUrlText = it }, label = { Text("Update download link (shown to blocked users)") },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            OutlinedTextField(
                updateMessageText, { updateMessageText = it }, label = { Text("Message (optional)") },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            Button(
                onClick = { viewModel.saveVersionGate(minVersionCodeText.toLongOrNull() ?: 0L, updateUrlText, updateMessageText) },
                colors = ButtonDefaults.buttonColors(containerColor = SpRed),
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            ) { Text("Save") }
            statusMessages["versionGateStatus"]?.let { Text(it, modifier = Modifier.padding(top = 8.dp)) }
        }
    }

    Card(Modifier.fillMaxWidth().padding(top = 12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text("DATA BACKUP", fontWeight = FontWeight.Bold)
            Text(
                "Firestore is a managed, replicated cloud database — data isn't lost if the app crashes on a phone. "
                    + "This is an extra offline copy: every worker, site, attendance record, leave, blocked attempt, and "
                    + "arrival request, all time, in one spreadsheet you can save to Drive or email yourself.",
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 6.dp),
            )
            Button(
                onClick = {
                    backupInProgress = true
                    backupStatus = "⏳ Collecting all data…"
                    scope.launch {
                        try {
                            val file = viewModel.generateFullBackup()
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "Save full backup"))
                            backupStatus = "✅ Backup ready: ${file.name}"
                        } catch (e: Throwable) {
                            backupStatus = "❌ ${e.diagnosticChain()}"
                        } finally {
                            backupInProgress = false
                        }
                    }
                },
                enabled = !backupInProgress,
                colors = ButtonDefaults.buttonColors(containerColor = SpBrandBlueMid),
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (backupInProgress) "Collecting…" else "⬇ Download Full Backup (.xlsx)") }
            if (backupStatus.isNotBlank()) Text(backupStatus, modifier = Modifier.padding(top = 6.dp))

            Text(
                "Restore adds/updates records from a backup file — it never deletes anything currently in the app.",
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 14.dp, bottom = 6.dp),
            )
            OutlinedButton(
                onClick = { backupPicker.launch("*/*") },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("⬆ Upload Backup (.xlsx)") }
            statusMessages["backupRestoreStatus"]?.let { Text(it, modifier = Modifier.padding(top = 6.dp)) }
        }
    }

    Card(Modifier.fillMaxWidth().padding(top = 12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text("SITE DEVIATIONS — TODAY", fontWeight = FontWeight.Bold)
            Text(
                "Checked in somewhere other than their ERP/biometric roster-aligned site — the attendance still counts, this is just for visibility.",
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 6.dp),
            )
            if (siteDeviationsToday.isEmpty()) {
                Text("None today.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                siteDeviationsToday.forEach { a ->
                    val w = workers.find { it.id == a.workerId }
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text("${w?.name ?: a.workerId} (ID ${a.workerId})", fontWeight = FontWeight.SemiBold)
                            Text(
                                "Aligned: ${a.alignedSite} → Reported: ${a.siteName} (${a.siteCode})",
                                color = SpAmberMid,
                            )
                        }
                        OutlinedButton(onClick = { viewModel.acknowledgeSiteDeviation(a) }) { Text("Acknowledge") }
                    }
                }
            }
            statusMessages["deviationStatus"]?.let { Text(it, color = SpRed, modifier = Modifier.padding(top = 6.dp)) }
        }
    }

    Card(Modifier.fillMaxWidth().padding(top = 12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text("PENDING ARRIVAL REQUESTS", fontWeight = FontWeight.Bold)
            if (pendingArrivals.isEmpty()) {
                Text("No pending requests.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
            } else {
                pendingArrivals.forEach { req ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text("${req.name} (ID ${req.workerId})", fontWeight = FontWeight.SemiBold)
                            Text("${req.designation} → ${req.site} · ${req.requestedDate}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        OutlinedButton(onClick = { viewModel.approveArrival(req) }) { Text("Approve") }
                        OutlinedButton(onClick = { viewModel.rejectArrival(req) }, modifier = Modifier.padding(start = 6.dp)) { Text("Reject") }
                    }
                }
            }
            statusMessages["arrivalReviewStatus"]?.let { Text(it, color = SpRed, modifier = Modifier.padding(top = 6.dp)) }
        }
    }

    Card(Modifier.fillMaxWidth().padding(top = 12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text("PENDING LEAVE REQUESTS", fontWeight = FontWeight.Bold)
            Text(
                "Submitted by office staff — only counts against the Absent Report once approved.",
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 6.dp),
            )
            if (pendingLeaveRequests.isEmpty()) {
                Text("No pending requests.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                pendingLeaveRequests.forEach { leave ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(leave.requestedBy ?: leave.markedBy, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${leave.fromDate} → ${leave.toDate}" + (leave.reason?.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        OutlinedButton(onClick = { viewModel.approveLeaveRequest(leave) }) { Text("Approve") }
                        OutlinedButton(onClick = { viewModel.rejectLeaveRequest(leave) }, modifier = Modifier.padding(start = 6.dp)) { Text("Reject") }
                        OutlinedButton(
                            onClick = { viewModel.deleteLeaveRequest(leave) },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SpRed),
                            modifier = Modifier.padding(start = 6.dp),
                        ) { Text("Delete") }
                    }
                }
            }
            statusMessages["leaveReviewStatus"]?.let { Text(it, color = SpRed, modifier = Modifier.padding(top = 6.dp)) }
        }
    }

    Card(Modifier.fillMaxWidth().padding(top = 12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("LEAVE HISTORY", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                OutlinedButton(onClick = { leaveHistoryRefresh++ }) { Text("🔄 Refresh") }
            }
            Text(
                "Every leave request ever submitted or marked — pending, approved, and rejected, all time.",
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 6.dp),
            )
            OutlinedTextField(
                leaveHistorySearch, { leaveHistorySearch = it }, placeholder = { Text("🔍 Search worker ID / requester…") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            )
            val filtered = leaveHistory.filter {
                leaveHistorySearch.isBlank() || it.workerId.contains(leaveHistorySearch, true) ||
                    (it.requestedBy ?: it.markedBy).contains(leaveHistorySearch, true)
            }.sortedByDescending { it.ts }
            val cappedHistory = if (leaveHistorySearch.isBlank()) filtered.take(150) else filtered
            if (cappedHistory.isEmpty()) {
                Text("No leave records yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                cappedHistory.forEach { leave ->
                    val w = workers.find { it.id == leave.workerId }
                    val (label, color) = when (leave.status) {
                        "approved" -> "APPROVED" to SpGreenMid
                        "rejected" -> "REJECTED" to SpRed
                        else -> "PENDING" to SpAmberMid
                    }
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text("${w?.name ?: leave.workerId} (ID ${leave.workerId})", fontWeight = FontWeight.SemiBold)
                            Text(
                                "${leave.fromDate} → ${leave.toDate}" + (leave.reason?.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(leave.requestedBy ?: leave.markedBy, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        }
                        Text(label, color = color, fontWeight = FontWeight.Bold)
                    }
                }
            }
            if (leaveHistorySearch.isBlank() && filtered.size > cappedHistory.size) {
                Text(
                    "+ ${filtered.size - cappedHistory.size} more not shown — search to narrow.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }

    Text("ROSTER UPLOAD (ADMIN)", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 14.dp, bottom = 8.dp))
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text("Upload Worker Roster by Project Code", fontWeight = FontWeight.Bold)
            Text(
                "Columns needed: Project Code, Worker ID (Aligned Date optional). Name & Designation are matched automatically from existing workers.",
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 6.dp),
            )
            OutlinedButton(onClick = { rosterPicker.launch("*/*") }) { Text("Choose File") }
            statusMessages["rosterUploadStatus"]?.let { Text(it, modifier = Modifier.padding(top = 6.dp)) }
        }
    }

    Card(Modifier.fillMaxWidth().padding(top = 12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text("MARK WORKER ON LEAVE", fontWeight = FontWeight.Bold)
            Text(
                "Workers on approved leave are excluded from the Absent Report.",
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 6.dp),
            )
            OutlinedTextField(leaveId, { leaveId = it }, label = { Text("Worker ID") }, modifier = Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                OutlinedTextField(leaveFrom, { leaveFrom = it }, label = { Text("From (YYYY-MM-DD)") }, modifier = Modifier.weight(1f))
                OutlinedTextField(leaveTo, { leaveTo = it }, label = { Text("To (YYYY-MM-DD)") }, modifier = Modifier.weight(1f).padding(start = 8.dp))
            }
            OutlinedTextField(leaveReason, { leaveReason = it }, label = { Text("Reason (optional)") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
            Button(
                onClick = { viewModel.markLeave(leaveId, leaveFrom, leaveTo, leaveReason.ifBlank { null }) },
                colors = ButtonDefaults.buttonColors(containerColor = SpBrandBlueMid),
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            ) { Text("Mark Leave") }
            statusMessages["leaveStatus"]?.let { Text(it, modifier = Modifier.padding(top = 6.dp)) }
        }
    }

    Card(Modifier.fillMaxWidth().padding(top = 12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text("CURRENT ROSTER BY PROJECT CODE", fontWeight = FontWeight.Bold)
            OutlinedTextField(
                rosterSearch, { rosterSearch = it }, placeholder = { Text("🔍 Search project code / worker ID…") },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            val rostered = workers.filter { !it.site.isNullOrBlank() }
                .filter { rosterSearch.isBlank() || it.site!!.contains(rosterSearch, true) || it.id.contains(rosterSearch) || it.name.contains(rosterSearch, true) }
            // Unfiltered, this list renders every rostered worker eagerly in a plain Column (no
            // lazy recycling) — fine at dozens or low hundreds, but thousands of Row composables
            // at once noticeably janks scrolling on modest phones. Cap it when there's no search
            // narrowing it down; a real search always returns a small, safe-to-render result set.
            val cappedRostered = if (rosterSearch.isBlank()) rostered.take(150) else rostered
            cappedRostered.groupBy { it.site!! }.toSortedMap().forEach { (code, list) ->
                Text(code, fontWeight = FontWeight.Bold, color = SpGreenMid, modifier = Modifier.padding(top = 10.dp))
                list.forEach { w -> RosterWorkerRow(w, onToggle = { viewModel.toggleWorkerStatus(w) }) }
            }
            val hiddenCount = rostered.size - cappedRostered.size
            if (hiddenCount > 0) {
                Text(
                    "+ $hiddenCount more not shown — search by name, ID, or project code to find someone specific.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 10.dp),
                )
            }
            statusMessages["deleteStatus"]?.let { Text(it, color = SpRed, modifier = Modifier.padding(top = 10.dp)) }
        }
    }

    pendingImport?.let { pending ->
        ImportConfirmDialog(pending, onConfirm = viewModel::confirmPendingImport, onDismiss = viewModel::cancelPendingImport)
    }

    pendingRestore?.let { pending ->
        com.ktc.sitepulse.ui.components.RestoreConfirmDialog(pending, onConfirm = viewModel::confirmBackupRestore, onDismiss = viewModel::cancelBackupRestore)
    }
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

@Composable
private fun RosterWorkerRow(w: Worker, onToggle: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(Modifier.weight(1f)) {
            Text(w.name, fontWeight = FontWeight.SemiBold)
            Text(
                "ID ${w.id}" + (w.alignedDate?.let { " · Aligned $it" } ?: "") + (if (w.isLeft) " · Left ${w.leftDate}" else ""),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        OutlinedButton(onClick = onToggle, colors = ButtonDefaults.outlinedButtonColors(contentColor = if (w.isLeft) SpGreenMid else SpRed)) {
            Text(if (w.isLeft) "Reactivate" else "Mark Left")
        }
    }
}
