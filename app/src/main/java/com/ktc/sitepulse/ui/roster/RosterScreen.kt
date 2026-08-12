package com.ktc.sitepulse.ui.roster

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ktc.sitepulse.ui.ImportKind
import com.ktc.sitepulse.data.model.Worker
import com.ktc.sitepulse.domain.DateUtils
import com.ktc.sitepulse.domain.WorkerSearch
import com.ktc.sitepulse.ui.SitePulseViewModel
import com.ktc.sitepulse.ui.components.ImportConfirmDialog
import com.ktc.sitepulse.ui.theme.SpGreenMid
import com.ktc.sitepulse.ui.theme.SpRed
import com.ktc.sitepulse.util.displayName

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

    Text("Report New Arrival", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp, bottom = 8.dp))
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
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
                colors = ButtonDefaults.buttonColors(containerColor = SpGreenMid),
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
    val pendingImport by viewModel.pendingImport.collectAsState()
    val workers by viewModel.workers.collectAsState()
    val context = LocalContext.current

    val rosterPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.startImport(ImportKind.ROSTER, it, it.displayName(context)) }
    }

    var leaveId by remember { mutableStateOf("") }
    var leaveFrom by remember { mutableStateOf("") }
    var leaveTo by remember { mutableStateOf("") }
    var leaveReason by remember { mutableStateOf("") }
    var rosterSearch by remember { mutableStateOf("") }

    Card(Modifier.fillMaxWidth().padding(top = 12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp)) {
            Text("NOTIFICATIONS", fontWeight = FontWeight.Bold)
            Text(
                "Get a push notification the moment a supervisor reports a new arrival — even when the app is closed.",
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 6.dp),
            )
            Button(onClick = viewModel::enableNotifications, modifier = Modifier.fillMaxWidth()) { Text("🔔 Enable Notifications") }
            statusMessages["pushStatus"]?.let { Text(it, modifier = Modifier.padding(top = 6.dp)) }
        }
    }

    Card(Modifier.fillMaxWidth().padding(top = 12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
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
        }
    }

    Text("ROSTER UPLOAD (ADMIN)", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 14.dp, bottom = 8.dp))
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
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

    Card(Modifier.fillMaxWidth().padding(top = 12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
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
                colors = ButtonDefaults.buttonColors(containerColor = SpGreenMid),
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            ) { Text("Mark Leave") }
            statusMessages["leaveStatus"]?.let { Text(it, modifier = Modifier.padding(top = 6.dp)) }
        }
    }

    Card(Modifier.fillMaxWidth().padding(top = 12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp)) {
            Text("CURRENT ROSTER BY PROJECT CODE", fontWeight = FontWeight.Bold)
            OutlinedTextField(
                rosterSearch, { rosterSearch = it }, placeholder = { Text("🔍 Search project code / worker ID…") },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            val rostered = workers.filter { !it.site.isNullOrBlank() }
                .filter { rosterSearch.isBlank() || it.site!!.contains(rosterSearch, true) || it.id.contains(rosterSearch) || it.name.contains(rosterSearch, true) }
            rostered.groupBy { it.site!! }.toSortedMap().forEach { (code, list) ->
                Text(code, fontWeight = FontWeight.Bold, color = SpGreenMid, modifier = Modifier.padding(top = 10.dp))
                list.forEach { w -> RosterWorkerRow(w, onToggle = { viewModel.toggleWorkerStatus(w) }) }
            }
        }
    }

    pendingImport?.let { pending ->
        ImportConfirmDialog(pending, onConfirm = viewModel::confirmPendingImport, onDismiss = viewModel::cancelPendingImport)
    }
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
