package com.ktc.sitepulse.ui.checkin

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.ktc.sitepulse.AppContainer
import com.ktc.sitepulse.data.model.Worker
import com.ktc.sitepulse.domain.MarkDirection
import com.ktc.sitepulse.domain.MarkResult
import com.ktc.sitepulse.domain.WorkerSearch
import com.ktc.sitepulse.ui.SitePulseViewModel
import com.ktc.sitepulse.ui.theme.SpAmberMid
import com.ktc.sitepulse.ui.theme.SpAmberSoft
import com.ktc.sitepulse.ui.theme.SpBlue
import com.ktc.sitepulse.ui.theme.SpBrandBlueMid
import com.ktc.sitepulse.ui.theme.SpBrandBlueSoft
import com.ktc.sitepulse.ui.theme.SpGreenMid
import com.ktc.sitepulse.ui.theme.SpGreenSoft
import com.ktc.sitepulse.ui.theme.SpRed
import com.ktc.sitepulse.ui.theme.SpRedSoft
import kotlinx.coroutines.delay

@Composable
fun CheckInScreen(viewModel: SitePulseViewModel, onReportArrival: () -> Unit, onOpenOfficeStaff: () -> Unit) {
    val workers by viewModel.workers.collectAsState()
    val sites by viewModel.sites.collectAsState()
    val session by viewModel.session.collectAsState()
    val markInFlight by viewModel.markInFlight.collectAsState()
    val markResult by viewModel.markResult.collectAsState()
    val myLinkedWorkerId by viewModel.myLinkedWorkerId.collectAsState()
    val context = LocalContext.current

    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<Worker?>(null) }
    var wifiSiteName by remember { mutableStateOf<String?>(null) }
    // Android hides the real WiFi SSID (returns null/"<unknown ssid>") unless the app holds
    // location permission AND the phone's system Location toggle is on — there's no separate
    // "WiFi permission" to grant. Surfaced explicitly so office-WiFi punch-in doesn't silently
    // do nothing with no explanation.
    var wifiBlockedReason by remember { mutableStateOf<WifiBlockReason?>(null) }
    val hasWifiSites = sites.any { !it.wifiSsid.isNullOrBlank() }

    // Office-staff accounts are permanently bound to the first Worker ID they ever checked in
    // with — pre-fill and lock the field so the same account can't drift to a different ID.
    val isIdLocked = session.isOfficeStaff && myLinkedWorkerId != null
    LaunchedEffect(myLinkedWorkerId, session.isOfficeStaff) {
        if (session.isOfficeStaff) myLinkedWorkerId?.let { query = it }
    }

    LaunchedEffect(query, workers) {
        selected = WorkerSearch.findExact(workers, query)
    }

    // Best-effort, informational only — the real check happens inside mark() when tapped.
    LaunchedEffect(sites) {
        val wifiProvider = AppContainer.get(context).wifiProvider
        while (true) {
            wifiBlockedReason = wifiBlockReason(context)
            val ssid = wifiProvider.currentSsid()
            wifiSiteName = sites.find { it.wifiSsid?.equals(ssid, ignoreCase = true) == true }?.name
            delay(5000)
        }
    }

    val suggestions = if (selected == null) WorkerSearch.suggest(workers, query) else emptyList()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("👷", fontSize = 34.sp)
                Text("Enter Worker ID or Name", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 10.dp))

                wifiSiteName?.let {
                    Text("📶 Connected to office WiFi: $it", color = SpBlue, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
                }

                if (wifiSiteName == null && hasWifiSites && wifiBlockedReason != null) {
                    Column(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            when (wifiBlockedReason) {
                                WifiBlockReason.PERMISSION -> "⚠ WiFi office check-in needs Location permission — Android hides the network name without it."
                                WifiBlockReason.LOCATION_OFF -> "⚠ Turn on Location (GPS) in phone settings to enable WiFi office check-in."
                                null -> ""
                            },
                            color = SpAmberMid, fontSize = 11.sp, textAlign = TextAlign.Center,
                        )
                        OutlinedButton(
                            onClick = {
                                val intent = if (wifiBlockedReason == WifiBlockReason.PERMISSION) {
                                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                                } else {
                                    Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                                }
                                context.startActivity(intent)
                            },
                            modifier = Modifier.padding(top = 4.dp),
                        ) { Text(if (wifiBlockedReason == WifiBlockReason.PERMISSION) "Open App Settings" else "Open Location Settings", fontSize = 11.sp) }
                    }
                }

                OutlinedTextField(
                    value = query,
                    onValueChange = { if (!isIdLocked) { query = it; viewModel.clearMarkResult() } },
                    readOnly = isIdLocked,
                    placeholder = { Text("ID or Name") },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Center, fontSize = 18.sp),
                    singleLine = true,
                )

                if (isIdLocked) {
                    Text(
                        "🔒 This account is permanently linked to Worker ID $myLinkedWorkerId",
                        color = SpBlue, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp),
                    )
                }

                if (suggestions.isNotEmpty() && !isIdLocked) {
                    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        suggestions.forEach { w ->
                            Text(
                                "${w.name} — ID ${w.id}",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { query = w.id }
                                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(9.dp))
                                    .padding(11.dp),
                            )
                        }
                    }
                }

                val verifyText = when {
                    query.isBlank() -> ""
                    selected != null -> "✅ ${selected!!.name} — ${selected!!.designation} (ID ${selected!!.id})"
                    suggestions.isEmpty() -> "❌ No worker found matching \"$query\""
                    else -> ""
                }
                if (verifyText.isNotBlank()) {
                    Text(
                        verifyText,
                        color = if (selected != null) SpGreenMid else SpRed,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }

                Button(
                    onClick = { selected?.let { viewModel.mark(MarkDirection.IN, it) } },
                    enabled = selected != null && !markInFlight,
                    colors = ButtonDefaults.buttonColors(containerColor = SpGreenMid),
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                ) { Text("CHECK IN") }

                Button(
                    onClick = { selected?.let { viewModel.mark(MarkDirection.OUT, it) } },
                    enabled = selected != null && !markInFlight,
                    colors = ButtonDefaults.buttonColors(containerColor = SpRed),
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                ) { Text("CHECK OUT") }

                if (markInFlight) {
                    Column(Modifier.padding(top = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(modifier = Modifier.padding(bottom = 6.dp))
                        Text("📡 Checking location…", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    }
                }

                markResult?.let { result -> ResultPanel(result) }
            }
        }

        Button(
            onClick = onReportArrival,
            colors = ButtonDefaults.buttonColors(containerColor = SpGreenSoft, contentColor = SpGreenMid),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) { Text("📋 Report a New Worker Arrival", fontWeight = FontWeight.Bold) }

        if (session.isOfficeStaff) {
            Button(
                onClick = onOpenOfficeStaff,
                colors = ButtonDefaults.buttonColors(containerColor = SpBrandBlueSoft, contentColor = SpBrandBlueMid),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) { Text("🗓️ My Leave & Attendance", fontWeight = FontWeight.Bold) }
        }
    }

    LaunchedEffect(markResult) {
        if (markResult is MarkResult.Success) {
            query = ""
            selected = null
        }
    }
}

private enum class WifiBlockReason { PERMISSION, LOCATION_OFF }

/** Why WiFi SSID detection isn't resolving right now, or null if nothing's blocking it. */
private fun wifiBlockReason(context: Context): WifiBlockReason? {
    val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    if (!hasPermission) return WifiBlockReason.PERMISSION
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    if (!LocationManagerCompat.isLocationEnabled(locationManager)) return WifiBlockReason.LOCATION_OFF
    return null
}

@Composable
private fun ResultPanel(result: MarkResult) {
    val (bg, title, detail) = when (result) {
        is MarkResult.Success -> Triple(SpGreenSoft, result.title, result.detail)
        is MarkResult.Blocked -> Triple(SpRedSoft, result.title, result.detail)
        is MarkResult.Rejected -> Triple(SpAmberSoft, result.title, result.detail)
        is MarkResult.Failure -> Triple(SpRedSoft, "⚠️ ERROR", result.message)
    }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 14.dp)
            .background(bg, RoundedCornerShape(12.dp))
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, textAlign = TextAlign.Center)
        Text(detail, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
    }
}
