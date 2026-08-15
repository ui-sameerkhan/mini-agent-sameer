package com.ktc.sitepulse.ui.attendance

import android.app.DatePickerDialog
import android.content.Intent
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.ktc.sitepulse.data.model.Attendance
import com.ktc.sitepulse.domain.DateUtils
import com.ktc.sitepulse.domain.ReportEngine
import com.ktc.sitepulse.ui.SitePulseViewModel
import kotlinx.coroutines.launch

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(viewModel: SitePulseViewModel) {
    val sites by viewModel.sites.collectAsState()
    val workers by viewModel.workers.collectAsState()
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

    LaunchedEffect(selectedDate) { dayRows = viewModel.attendanceForDate(selectedDate) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        OutlinedButton(onClick = {
            val (y, m, d) = selectedDate.split("-").map { it.toInt() }
            DatePickerDialog(context, { _, yy, mm, dd ->
                selectedDate = "%04d-%02d-%02d".format(yy, mm + 1, dd)
            }, y, m - 1, d).show()
        }, modifier = Modifier.fillMaxWidth()) { Text("📅 Date: $selectedDate") }

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
                            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Text(w?.name ?: a.workerId, Modifier.weight(1f))
                                Text(DateUtils.formatTimeHm(a.checkIn))
                                Text(" / ")
                                Text(DateUtils.formatTimeHm(a.out))
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
