package com.ktc.sitepulse.ui.workers

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.ktc.sitepulse.ui.ImportKind
import com.ktc.sitepulse.data.model.Worker
import com.ktc.sitepulse.domain.QrCodeUtil
import com.ktc.sitepulse.ui.SitePulseViewModel
import com.ktc.sitepulse.ui.components.DesignationField
import com.ktc.sitepulse.ui.components.ImportConfirmDialog
import com.ktc.sitepulse.ui.components.TypedDeleteConfirmDialog
import com.ktc.sitepulse.ui.theme.SpAmberMid
import com.ktc.sitepulse.ui.theme.SpGreenMid
import com.ktc.sitepulse.ui.theme.SpRed
import com.ktc.sitepulse.util.displayName
import kotlinx.coroutines.launch

private const val PAGE_SIZE = 50

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun WorkersScreen(viewModel: SitePulseViewModel) {
    val workers by viewModel.workers.collectAsState()
    val sites by viewModel.sites.collectAsState()
    val statusMessages by viewModel.statusMessages.collectAsState()
    val pendingImport by viewModel.pendingImport.collectAsState()
    val pendingDelete by viewModel.pendingDelete.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var query by remember { mutableStateOf("") }
    var siteFilter by remember { mutableStateOf("ALL") }
    var page by remember { mutableStateOf(1) }
    var editing by remember { mutableStateOf<Worker?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    var siteDropdownExpanded by remember { mutableStateOf(false) }
    var showingQr by remember { mutableStateOf<Worker?>(null) }
    var generatingQrPdf by remember { mutableStateOf(false) }
    var qrPdfStatus by remember { mutableStateOf<String?>(null) }

    val excelPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.startImport(ImportKind.WORKERS, it, it.displayName(context)) }
    }
    val outsourcePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.startImport(ImportKind.OUTSOURCE, it, it.displayName(context)) }
    }

    val filtered = workers.filter { w ->
        val matchesQuery = query.isBlank() || w.name.contains(query, true) || w.id.contains(query) || w.designation.contains(query, true)
        val matchesSite = when (siteFilter) {
            "ALL" -> true
            "NONE" -> w.site.isNullOrBlank()
            else -> w.site == siteFilter
        }
        matchesQuery && matchesSite
    }
    val totalPages = ((filtered.size - 1) / PAGE_SIZE + 1).coerceAtLeast(1)
    val pageItems = filtered.drop((page - 1) * PAGE_SIZE).take(PAGE_SIZE)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        UploadZone(
            emoji = "📊", title = "Upload Workers from Excel",
            hint = "Required columns: Employee ID, Employee Name, Designation",
            status = statusMessages["xlstatus"],
            onPick = { excelPicker.launch("*/*") },
        )
        UploadZone(
            emoji = "🏢", title = "Upload Outsource Manpower",
            hint = "Columns: Worker ID, Name, Company Name, Designation/Trade (optional)",
            status = statusMessages["outsourceStatus"],
            onPick = { outsourcePicker.launch("*/*") },
        )

        OutlinedButton(
            onClick = { viewModel.requestDeleteAllWorkers() },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = SpRed),
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        ) { Text("Delete All Workers") }
        statusMessages["deleteStatus"]?.let { Text(it, color = SpRed, modifier = Modifier.padding(top = 6.dp)) }

        OutlinedTextField(
            value = query, onValueChange = { query = it; page = 1 },
            placeholder = { Text("🔍 Search name / ID / trade…") },
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp), singleLine = true,
        )

        ExposedDropdownMenuBox(expanded = siteDropdownExpanded, onExpandedChange = { siteDropdownExpanded = it }, modifier = Modifier.padding(top = 8.dp)) {
            OutlinedTextField(
                value = when (siteFilter) { "ALL" -> "All Sites"; "NONE" -> "Not Assigned"; else -> siteFilter },
                onValueChange = {}, readOnly = true, label = { Text("Site Filter") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = siteDropdownExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
            )
            ExposedDropdownMenu(expanded = siteDropdownExpanded, onDismissRequest = { siteDropdownExpanded = false }) {
                DropdownMenuItem(text = { Text("All Sites") }, onClick = { siteFilter = "ALL"; page = 1; siteDropdownExpanded = false })
                DropdownMenuItem(text = { Text("Not Assigned") }, onClick = { siteFilter = "NONE"; page = 1; siteDropdownExpanded = false })
                sites.forEach { s -> DropdownMenuItem(text = { Text(s.code) }, onClick = { siteFilter = s.code; page = 1; siteDropdownExpanded = false }) }
            }
        }

        Button(
            onClick = { showAdd = true },
            colors = ButtonDefaults.buttonColors(containerColor = SpAmberMid),
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        ) { Text("Add Employee", fontWeight = FontWeight.Bold) }

        OutlinedButton(
            onClick = {
                generatingQrPdf = true
                qrPdfStatus = "Generating…"
                scope.launch {
                    try {
                        val file = viewModel.generateWorkerQrBadgesPdf(filtered)
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/pdf"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share / print QR ID badges"))
                        qrPdfStatus = "✅ ${filtered.size} badge(s) ready: ${file.name}"
                    } catch (e: Exception) {
                        qrPdfStatus = "❌ ${e.message ?: e.toString()}"
                    } finally {
                        generatingQrPdf = false
                    }
                }
            },
            enabled = !generatingQrPdf && filtered.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) { Text(if (generatingQrPdf) "Generating…" else "🖨️ Bulk Generate QR Badges (${filtered.size})") }
        Text(
            "Generates one printable PDF with every employee matching the current search/site filter above — use All Sites + clear search to include everyone.",
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp),
        )
        qrPdfStatus?.let { Text(it, modifier = Modifier.padding(top = 6.dp)) }

        Text(
            "Showing ${pageItems.size} of ${filtered.size} • Page $page/$totalPages",
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
        )

        pageItems.forEach { w ->
            Card(Modifier.fillMaxWidth().padding(bottom = 8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Row(Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            Text(w.name, fontWeight = FontWeight.SemiBold)
                            Text("${w.designation} · ID ${w.id}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Row(Modifier.padding(top = 4.dp)) {
                        w.site?.let { Text(it, color = SpGreenMid, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 8.dp)) }
                        w.company?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                    Row(Modifier.padding(top = 8.dp)) {
                        OutlinedButton(onClick = { editing = w }) { Text("Edit") }
                        OutlinedButton(onClick = { showingQr = w }, modifier = Modifier.padding(start = 8.dp)) { Text("📷 QR") }
                        OutlinedButton(onClick = { viewModel.requestDeleteWorker(w.id, w.name) }, modifier = Modifier.padding(start = 8.dp)) { Text("Delete") }
                    }
                }
            }
        }

        if (totalPages > 1) {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                OutlinedButton(onClick = { if (page > 1) page-- }, enabled = page > 1) { Text("Prev") }
                Text("Page $page / $totalPages", Modifier.weight(1f).padding(horizontal = 12.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                OutlinedButton(onClick = { if (page < totalPages) page++ }, enabled = page < totalPages) { Text("Next") }
            }
        }
    }

    if (showAdd) {
        WorkerEditDialog(existing = null, sites = sites, onDismiss = { showAdd = false }, onSave = { w ->
            scope.launch { viewModel.saveWorker(w); showAdd = false }
        })
    }
    editing?.let { w ->
        WorkerEditDialog(existing = w, sites = sites, onDismiss = { editing = null }, onSave = { updated ->
            scope.launch { viewModel.saveWorker(updated); editing = null }
        })
    }
    pendingImport?.let { pending ->
        ImportConfirmDialog(pending, onConfirm = viewModel::confirmPendingImport, onDismiss = viewModel::cancelPendingImport)
    }
    pendingDelete?.takeIf { it.kind == "worker" || it.kind == "allWorkers" }?.let { pending ->
        TypedDeleteConfirmDialog(pending, onConfirm = { typed -> viewModel.confirmPendingDelete(typed) }, onDismiss = viewModel::cancelPendingDelete)
    }
    showingQr?.let { w ->
        WorkerQrDialog(worker = w, onDismiss = { showingQr = null })
    }
}

@Composable
private fun WorkerQrDialog(worker: Worker, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val bitmap = remember(worker.id) { QrCodeUtil.generate(worker.id) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ID Badge QR — ${worker.name}") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Image(bitmap = bitmap.asImageBitmap(), contentDescription = "QR code for worker ${worker.id}", modifier = Modifier.size(220.dp))
                Text("Worker ID: ${worker.id}", modifier = Modifier.padding(top = 10.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Scan this at check-in instead of typing the ID.", modifier = Modifier.padding(top = 4.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            Button(onClick = {
                val file = QrCodeUtil.saveToCache(context, bitmap, worker.id)
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Share ID badge QR"))
            }) { Text("Share") }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
private fun UploadZone(emoji: String, title: String, hint: String, status: String?, onPick: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(top = 10.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(emoji, style = MaterialTheme.typography.titleLarge)
            Text(title, fontWeight = FontWeight.Bold)
            Text(hint, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 6.dp))
            OutlinedButton(onClick = onPick) { Text("Choose File") }
            status?.let { Text(it, modifier = Modifier.padding(top = 6.dp)) }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun WorkerEditDialog(existing: Worker?, sites: List<com.ktc.sitepulse.data.model.Site>, onDismiss: () -> Unit, onSave: (Worker) -> Unit) {
    var id by remember { mutableStateOf(existing?.id ?: "") }
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var designation by remember { mutableStateOf(existing?.designation ?: "") }
    var company by remember { mutableStateOf(existing?.company ?: "") }
    var site by remember { mutableStateOf(existing?.site ?: "") }
    var siteExpanded by remember { mutableStateOf(false) }
    var annualLeaveDays by remember { mutableStateOf((existing?.annualLeaveDays ?: 30L).toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add Employee" else "Edit Employee") },
        text = {
            Column {
                OutlinedTextField(id, { id = it }, label = { Text("Employee ID *") }, modifier = Modifier.fillMaxWidth(), enabled = existing == null)
                OutlinedTextField(name, { name = it }, label = { Text("Full Name *") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                DesignationField(designation, { designation = it }, modifier = Modifier.padding(top = 8.dp))
                OutlinedTextField(company, { company = it }, label = { Text("Company (optional)") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                OutlinedTextField(
                    annualLeaveDays, { annualLeaveDays = it.filter { c -> c.isDigit() } },
                    label = { Text("Annual Leave Days") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                ExposedDropdownMenuBox(expanded = siteExpanded, onExpandedChange = { siteExpanded = it }, modifier = Modifier.padding(top = 8.dp)) {
                    OutlinedTextField(
                        value = site.ifBlank { "Unassigned" }, onValueChange = {}, readOnly = true,
                        label = { Text("Assigned Project Site") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = siteExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                    )
                    ExposedDropdownMenu(expanded = siteExpanded, onDismissRequest = { siteExpanded = false }) {
                        DropdownMenuItem(text = { Text("Unassigned") }, onClick = { site = ""; siteExpanded = false })
                        sites.forEach { s -> DropdownMenuItem(text = { Text(s.code) }, onClick = { site = s.code; siteExpanded = false }) }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (id.isNotBlank() && name.isNotBlank() && designation.isNotBlank()) {
                    onSave(
                        Worker(
                            sno = existing?.sno ?: 0, id = id.trim(), name = name.trim(), designation = designation.trim(),
                            company = company.ifBlank { null }, site = site.ifBlank { null },
                            alignedDate = existing?.alignedDate, status = existing?.status ?: "active", leftDate = existing?.leftDate,
                            annualLeaveDays = annualLeaveDays.toLongOrNull() ?: 30L,
                        )
                    )
                }
            }) { Text("Save") }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
