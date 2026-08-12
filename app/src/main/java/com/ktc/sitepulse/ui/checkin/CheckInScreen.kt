package com.ktc.sitepulse.ui.checkin

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ktc.sitepulse.data.model.Worker
import com.ktc.sitepulse.domain.MarkDirection
import com.ktc.sitepulse.domain.MarkResult
import com.ktc.sitepulse.domain.WorkerSearch
import com.ktc.sitepulse.ui.SitePulseViewModel
import com.ktc.sitepulse.ui.theme.SpAmberSoft
import com.ktc.sitepulse.ui.theme.SpGreenMid
import com.ktc.sitepulse.ui.theme.SpGreenSoft
import com.ktc.sitepulse.ui.theme.SpRed
import com.ktc.sitepulse.ui.theme.SpRedSoft

@Composable
fun CheckInScreen(viewModel: SitePulseViewModel, onReportArrival: () -> Unit) {
    val workers by viewModel.workers.collectAsState()
    val markInFlight by viewModel.markInFlight.collectAsState()
    val markResult by viewModel.markResult.collectAsState()

    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<Worker?>(null) }

    LaunchedEffect(query, workers) {
        selected = WorkerSearch.findExact(workers, query)
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

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it; viewModel.clearMarkResult() },
                    placeholder = { Text("ID or Name") },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Center, fontSize = 18.sp),
                    singleLine = true,
                )

                if (suggestions.isNotEmpty()) {
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
    }

    LaunchedEffect(markResult) {
        if (markResult is MarkResult.Success) {
            query = ""
            selected = null
        }
    }
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
