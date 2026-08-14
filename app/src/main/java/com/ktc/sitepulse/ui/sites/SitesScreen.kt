package com.ktc.sitepulse.ui.sites

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ktc.sitepulse.AppContainer
import com.ktc.sitepulse.data.model.Site
import com.ktc.sitepulse.ui.SitePulseViewModel
import com.ktc.sitepulse.ui.components.TypedDeleteConfirmDialog
import com.ktc.sitepulse.ui.theme.SpAmberMid
import com.ktc.sitepulse.ui.theme.SpBlue
import kotlinx.coroutines.launch

@Composable
fun SitesScreen(viewModel: SitePulseViewModel) {
    val sites by viewModel.sites.collectAsState()
    val pendingDelete by viewModel.pendingDelete.collectAsState()
    var editing by remember { mutableStateOf<Site?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Button(
            onClick = { showAdd = true },
            colors = ButtonDefaults.buttonColors(containerColor = SpAmberMid),
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Add Project Site", fontWeight = FontWeight.Bold) }

        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 12.dp)) {
            if (sites.isEmpty()) {
                Text(
                    "No sites yet. Add your first project site — attendance is only allowed inside a site zone.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            sites.forEach { site ->
                Card(Modifier.fillMaxWidth().padding(bottom = 10.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Row(Modifier.fillMaxWidth()) {
                            Text(site.name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text(site.code, color = SpBlue, fontWeight = FontWeight.Bold)
                        }
                        Text("${"%.5f".format(site.lat)}, ${"%.5f".format(site.lng)} · radius ${site.radius}m", fontSize = 12.sp)
                        site.wifiSsid?.takeIf { it.isNotBlank() }?.let {
                            Text("📶 Office WiFi: $it", fontSize = 12.sp, color = SpBlue)
                        }
                        Row(Modifier.padding(top = 8.dp)) {
                            OutlinedButton(onClick = {
                                val uri = Uri.parse("https://maps.google.com/?q=${site.lat},${site.lng}")
                                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                            }) { Text("Map") }
                            OutlinedButton(onClick = { editing = site }, modifier = Modifier.padding(start = 8.dp)) { Text("Edit") }
                            OutlinedButton(
                                onClick = { viewModel.requestDeleteSite(site.code, site.name) },
                                modifier = Modifier.padding(start = 8.dp),
                            ) { Text("Delete") }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        SiteEditDialog(existing = null, onDismiss = { showAdd = false }, onSave = { s ->
            scope.launch { viewModel.saveSite(s); showAdd = false }
        })
    }
    editing?.let { site ->
        SiteEditDialog(existing = site, onDismiss = { editing = null }, onSave = { s ->
            scope.launch { viewModel.saveSite(s); editing = null }
        })
    }
    pendingDelete?.takeIf { it.kind == "site" }?.let { pending ->
        TypedDeleteConfirmDialog(pending, onConfirm = { typed -> viewModel.confirmPendingDelete(typed) }, onDismiss = viewModel::cancelPendingDelete)
    }
}

@Composable
private fun SiteEditDialog(existing: Site?, onDismiss: () -> Unit, onSave: (Site) -> Unit) {
    var code by remember { mutableStateOf(existing?.code ?: "") }
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var lat by remember { mutableStateOf(existing?.lat?.toString() ?: "") }
    var lng by remember { mutableStateOf(existing?.lng?.toString() ?: "") }
    var radius by remember { mutableStateOf((existing?.radius ?: 500).toString()) }
    var wifiSsid by remember { mutableStateOf(existing?.wifiSsid ?: "") }
    var gpsStatus by remember { mutableStateOf("") }
    var wifiStatus by remember { mutableStateOf("") }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "🏗️ Add Project Site" else "🏗️ Edit Project Site") },
        text = {
            Column {
                OutlinedTextField(code, { code = it.uppercase() }, label = { Text("Project Code *") }, modifier = Modifier.fillMaxWidth(), enabled = existing == null)
                OutlinedTextField(name, { name = it }, label = { Text("Site Name *") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            gpsStatus = "Getting location…"
                            try {
                                val fix = AppContainer.get(context).locationProvider.getCurrentFix()
                                lat = "%.6f".format(fix.lat)
                                lng = "%.6f".format(fix.lng)
                                gpsStatus = "📍 Captured (±${fix.accuracyM.toInt()}m accuracy)"
                            } catch (e: Exception) {
                                gpsStatus = "GPS failed: ${e.message}"
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                ) { Text("📍 Use My Current Location") }
                if (gpsStatus.isNotBlank()) Text(gpsStatus, fontSize = 12.sp)
                Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    OutlinedTextField(lat, { lat = it }, label = { Text("Latitude *") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(lng, { lng = it }, label = { Text("Longitude *") }, modifier = Modifier.weight(1f).padding(start = 8.dp))
                }
                OutlinedTextField(radius, { radius = it }, label = { Text("Geofence Radius (meters) *") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))

                Text(
                    "Optional: office staff connected to this WiFi network can punch in/out without GPS.",
                    fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                )
                OutlinedTextField(wifiSsid, { wifiSsid = it }, label = { Text("Office WiFi Network Name (SSID)") }, modifier = Modifier.fillMaxWidth())
                OutlinedButton(
                    onClick = {
                        val ssid = AppContainer.get(context).wifiProvider.currentSsid()
                        if (ssid != null) {
                            wifiSsid = ssid
                            wifiStatus = "📶 Using current network: $ssid"
                        } else {
                            wifiStatus = "Not connected to WiFi (or network name unavailable — enable Location services and try again)"
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                ) { Text("📶 Use My Current WiFi Network") }
                if (wifiStatus.isNotBlank()) Text(wifiStatus, fontSize = 12.sp)
            }
        },
        confirmButton = {
            Button(onClick = {
                val latD = lat.toDoubleOrNull()
                val lngD = lng.toDoubleOrNull()
                val radI = radius.toLongOrNull() ?: 500
                if (code.isNotBlank() && name.isNotBlank() && latD != null && lngD != null) {
                    onSave(
                        Site(
                            code = code.trim().uppercase(), name = name.trim(), lat = latD, lng = lngD, radius = radI,
                            wifiSsid = wifiSsid.trim().ifBlank { null },
                        )
                    )
                }
            }) { Text("Save Site") }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

