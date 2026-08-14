package com.lazyshopper.app.feature.customer.orders

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lazyshopper.app.core.theme.LsPrimary
import com.lazyshopper.app.core.ui.UiState
import com.lazyshopper.app.core.ui.components.ErrorState
import com.lazyshopper.app.core.ui.components.FullScreenLoading
import com.lazyshopper.app.core.ui.components.ScreenPadding

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderTrackingScreen(
    onBack: () -> Unit,
    onChat: (orderId: String) -> Unit,
    viewModel: OrderTrackingViewModel = hiltViewModel(),
) {
    val trackingState by viewModel.tracking.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Track order") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } },
            )
        },
    ) { padding ->
        when (val s = trackingState) {
            is UiState.Loading -> FullScreenLoading(Modifier.padding(padding))
            is UiState.Error -> ErrorState(s.message, Modifier.padding(padding), onRetry = viewModel::refresh)
            is UiState.Success -> {
                val tracking = s.data
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(ScreenPadding),
                ) {
                    Surface(color = LsPrimary.copy(alpha = 0.1f), shape = MaterialTheme.shapes.medium) {
                        Text(
                            statusLabel(tracking.status, tracking.delivery_status),
                            style = MaterialTheme.typography.titleMedium,
                            color = LsPrimary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        )
                    }
                    Spacer(Modifier.height(14.dp))

                    TrackingMapView(
                        customerLat = tracking.customer?.lat,
                        customerLng = tracking.customer?.lng,
                        partnerLat = tracking.partner?.lat,
                        partnerLng = tracking.partner?.lng,
                        partnerName = tracking.partner?.name,
                        modifier = Modifier.fillMaxWidth().height(280.dp),
                    )

                    Spacer(Modifier.height(16.dp))

                    val partner = tracking.partner
                    if (partner != null && (!partner.name.isNullOrBlank() || !partner.phone.isNullOrBlank())) {
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.fillMaxWidth().padding(14.dp)) {
                                Text("Your delivery partner", style = MaterialTheme.typography.titleSmall)
                                Spacer(Modifier.height(4.dp))
                                Text(partner.name ?: "Assigned", style = MaterialTheme.typography.bodyMedium)
                                Spacer(Modifier.height(12.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (!partner.phone.isNullOrBlank()) {
                                        OutlinedButton(onClick = {
                                            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${partner.phone}")))
                                        }) { Text("Call") }
                                    }
                                    OutlinedButton(onClick = { onChat(viewModel.orderId) }) { Text("Chat with rider") }
                                }
                            }
                        }
                    } else {
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.fillMaxWidth().padding(14.dp), horizontalAlignment = Alignment.Start) {
                                Text("Waiting for a delivery partner to be assigned…", style = MaterialTheme.typography.bodyMedium)
                                Spacer(Modifier.height(12.dp))
                                OutlinedButton(onClick = { onChat(viewModel.orderId) }) { Text("Chat about this order") }
                            }
                        }
                    }
                }
            }
            UiState.Idle -> {}
        }
    }
}

private fun statusLabel(status: String, deliveryStatus: String?): String = when {
    status == "delivered" -> "Delivered"
    status == "cancelled" -> "Order cancelled"
    status == "out_for_delivery" || deliveryStatus == "picked_up" -> "Out for delivery"
    status == "packed" || status == "ready" -> "Packed, waiting for pickup"
    status == "preparing" -> "Being prepared"
    status == "accepted" -> "Order accepted"
    else -> "Order placed"
}
