package com.ktc.sitepulse.ui.officestaff

import android.app.DatePickerDialog
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
import com.ktc.sitepulse.data.model.Attendance
import com.ktc.sitepulse.data.model.Leave
import com.ktc.sitepulse.domain.DateUtils
import com.ktc.sitepulse.ui.SitePulseViewModel
import com.ktc.sitepulse.ui.components.LeaveTypeField
import com.ktc.sitepulse.ui.components.NotificationsCard
import com.ktc.sitepulse.ui.theme.SpAmberMid
import com.ktc.sitepulse.ui.theme.SpBrandBlueMid
import com.ktc.sitepulse.ui.theme.SpGreenMid
import com.ktc.sitepulse.ui.theme.SpRed
import kotlinx.coroutines.launch

@Composable
fun OfficeStaffScreen(viewModel: SitePulseViewModel, onBack: () -> Unit) {
    val statusMessages by viewModel.statusMessages.collectAsState()
    val myLinkedWorkerId by viewModel.myLinkedWorkerId.collectAsState()
    val workers by viewModel.workers.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var workerId by remember { mutableStateOf("") }
    var fromDate by remember { mutableStateOf(DateUtils.todayStrUtc()) }
    var toDate by remember { mutableStateOf(DateUtils.todayStrUtc()) }
    var reason by remember { mutableStateOf("") }
    var leaveType by remember { mutableStateOf("Annual") }

    // Once this account has checked in under a Worker ID, leave applications are filed under
    // that same locked ID rather than letting the free-text field drift to a different one.
    LaunchedEffect(myLinkedWorkerId) { myLinkedWorkerId?.let { workerId = it } }
    val isIdLocked = myLinkedWorkerId != null

    var myLeaves by remember { mutableStateOf<List<Leave>>(emptyList()) }
    var myAttendance by remember { mutableStateOf<List<Attendance>>(emptyList()) }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(refreshTrigger) {
        myLeaves = viewModel.myLeaveRequests()
        myAttendance = viewModel.myAttendanceHistory()
    }

    fun refresh() { refreshTrigger++ }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        OutlinedButton(onClick = onBack) { Text("← Back to Check-In") }

        NotificationsCard(
            viewModel, "Get a push notification for admin announcements — even when the app is closed.",
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        )

        Text("APPLY FOR LEAVE", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 20.dp, bottom = 8.dp))
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)) {
            Column(Modifier.padding(16.dp)) {
                OutlinedTextField(
                    workerId, { if (!isIdLocked) workerId = it },
                    label = { Text(if (isIdLocked) "Your Worker ID (locked)" else "Your Worker ID (optional)") },
                    readOnly = isIdLocked,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (isIdLocked) {
                    Text(
                        "🔒 Permanently linked to Worker ID $myLinkedWorkerId",
                        color = SpBrandBlueMid, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp),
                    )
                }
                workers.find { it.id == workerId }?.let { w ->
                    val used = viewModel.annualLeaveUsedDays(myLeaves, workerId)
                    Text(
                        "Annual Leave: $used of ${w.annualLeaveDays} days used this year",
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp),
                    )
                }
                LeaveTypeField(leaveType, { leaveType = it }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    OutlinedButton(onClick = {
                        val (y, m, d) = fromDate.split("-").map { it.toInt() }
                        DatePickerDialog(context, { _, yy, mm, dd -> fromDate = "%04d-%02d-%02d".format(yy, mm + 1, dd) }, y, m - 1, d).show()
                    }, modifier = Modifier.weight(1f)) { Text("From: $fromDate") }
                    OutlinedButton(onClick = {
                        val (y, m, d) = toDate.split("-").map { it.toInt() }
                        DatePickerDialog(context, { _, yy, mm, dd -> toDate = "%04d-%02d-%02d".format(yy, mm + 1, dd) }, y, m - 1, d).show()
                    }, modifier = Modifier.weight(1f).padding(start = 8.dp)) { Text("To: $toDate") }
                }
                OutlinedTextField(
                    reason, { reason = it }, label = { Text("Reason (optional)") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                Button(
                    onClick = {
                        scope.launch {
                            viewModel.submitLeaveApplication(workerId, fromDate, toDate, reason.ifBlank { null }, leaveType)
                            refresh()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SpBrandBlueMid),
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                ) { Text("Submit Application") }
                statusMessages["myLeaveStatus"]?.let { Text(it, modifier = Modifier.padding(top = 8.dp)) }
            }
        }

        Text("MY LEAVE REQUESTS", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 20.dp, bottom = 8.dp))
        if (myLeaves.isEmpty()) {
            Text("No leave requests yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            myLeaves.sortedByDescending { it.ts }.forEach { leave -> LeaveRequestRow(leave) }
        }

        Text("MY ATTENDANCE HISTORY", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 20.dp, bottom = 8.dp))
        if (myAttendance.isEmpty()) {
            Text("No attendance records yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            myAttendance.forEach { a -> MyAttendanceRow(a) }
        }
    }
}

@Composable
private fun LeaveRequestRow(leave: Leave) {
    val (label, color) = when (leave.status) {
        "approved" -> "APPROVED" to SpGreenMid
        "rejected" -> "REJECTED" to SpRed
        else -> "PENDING" to SpAmberMid
    }
    Card(Modifier.fillMaxWidth().padding(bottom = 8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)) {
        Row(Modifier.padding(12.dp).fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text("${leave.leaveType} · ${leave.fromDate} → ${leave.toDate}", fontWeight = FontWeight.SemiBold)
                leave.reason?.takeIf { it.isNotBlank() }?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            Text(label, color = color, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun MyAttendanceRow(a: Attendance) {
    Card(Modifier.fillMaxWidth().padding(bottom = 8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text("${a.date} · ${a.siteName}", fontWeight = FontWeight.SemiBold)
            Row {
                if (a.hasIn) Text("IN ${DateUtils.formatTimeHm(a.checkIn)}", color = SpGreenMid, modifier = Modifier.padding(end = 12.dp))
                if (a.hasOut) Text("OUT ${DateUtils.formatTimeHm(a.out)}", color = SpRed)
            }
        }
    }
}
