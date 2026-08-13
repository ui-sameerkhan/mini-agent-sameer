package com.lazyshopper.app.feature.delivery.dashboard

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.lazyshopper.app.core.data.remote.dto.Order
import com.lazyshopper.app.core.theme.LsError
import com.lazyshopper.app.core.theme.LsPrimary
import com.lazyshopper.app.core.theme.LsWarning
import com.lazyshopper.app.core.ui.components.EmptyState
import com.lazyshopper.app.core.ui.components.FullScreenLoading
import com.lazyshopper.app.core.ui.components.LsPrimaryButton
import com.lazyshopper.app.core.ui.components.LsTextField
import com.lazyshopper.app.core.ui.components.ScreenPadding
import com.lazyshopper.app.feature.delivery.map.RiderMapView
import com.lazyshopper.app.feature.delivery.util.fetchCurrentLocation
import com.lazyshopper.app.rider.location.LocationForegroundService
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun DeliveryDashboardScreen(
    onNavigateKyc: () -> Unit,
    onOpenChat: (String) -> Unit,
    onLogout: () -> Unit,
    viewModel: DeliveryDashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    val permissions = buildList {
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
    }
    val permissionsState = rememberMultiplePermissionsState(permissions)

    // Single source of truth for whether the foreground GPS-push service should be running:
    // `state.online`, which itself is re-derived from the server (`user.available`) on load.
    // That means a killed-and-relaunched app converges back to the correct service state on
    // its own without any extra bookkeeping.
    LaunchedEffect(state.online) {
        if (state.online) LocationForegroundService.start(context) else LocationForegroundService.stop(context)
    }

    // Best-effort rider position for the mini-map on the active-delivery card only.
    LaunchedEffect(state.activeOrder?.id) {
        if (state.activeOrder != null) {
            while (isActive) {
                fetchCurrentLocation(context)?.let { (lat, lng) -> viewModel.updateRiderLocation(lat, lng) }
                delay(15_000)
            }
        }
    }

    if (state.loading) {
        FullScreenLoading()
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(ScreenPadding),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Delivery Dashboard", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = onLogout) { Text("Logout") }
        }
        Spacer(Modifier.height(12.dp))

        KycStatusBanner(kycStatus = state.kycStatus, rejectReason = state.kycRejectReason, onComplete = onNavigateKyc)

        Spacer(Modifier.height(12.dp))
        OnlineToggleCard(
            online = state.online,
            busy = state.toggleBusy,
            enabled = state.accountStatus == "active",
            onToggle = { turnOn ->
                if (turnOn && !permissionsState.allPermissionsGranted) {
                    permissionsState.launchMultiplePermissionRequest()
                } else {
                    viewModel.setOnline(turnOn)
                }
            },
        )

        state.error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(20.dp))

        val active = state.activeOrder
        when {
            active != null -> ActiveDeliveryCard(
                order = active,
                riderLat = state.riderLat,
                riderLng = state.riderLng,
                busy = state.actionBusy,
                onReject = viewModel::rejectActiveOrder,
                onPickup = viewModel::pickupActiveOrder,
                onDeliver = { viewModel.openDeliverDialog(active.id) },
                onChat = { onOpenChat(active.id) },
            )
            state.online -> {
                Text("Available orders nearby", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                if (state.availableOrders.isEmpty()) {
                    EmptyState("No orders available right now — we'll keep checking.")
                } else {
                    state.availableOrders.forEach { order ->
                        AvailableOrderCard(order = order, busy = state.actionBusy, onAccept = { viewModel.acceptOrder(order.id) })
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
            else -> EmptyState("You're offline. Go online to start receiving delivery requests.")
        }

        state.actionError?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(24.dp))
    }

    if (state.deliverDialogOrderId != null) {
        DeliverOtpDialog(
            busy = state.actionBusy,
            error = state.actionError,
            onDismiss = viewModel::closeDeliverDialog,
            onConfirm = viewModel::deliverOrder,
        )
    }
}

@Composable
private fun KycStatusBanner(kycStatus: String?, rejectReason: String?, onComplete: () -> Unit) {
    if (kycStatus == "approved") return
    val (text, tint, actionLabel) = when (kycStatus) {
        "rejected" -> Triple("KYC rejected — ${rejectReason ?: "please review and resubmit your documents."}", LsError, "Resubmit")
        "submitted" -> Triple("Your KYC is under review. We'll notify you once approved.", LsWarning, null)
        else -> Triple("Complete your KYC to start accepting deliveries.", LsWarning, "Complete KYC")
    }
    Card(colors = CardDefaults.cardColors(containerColor = tint.copy(alpha = 0.12f)), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            if (actionLabel != null) {
                TextButton(onClick = onComplete) { Text(actionLabel) }
            }
        }
    }
}

@Composable
private fun OnlineToggleCard(online: Boolean, busy: Boolean, enabled: Boolean, onToggle: (Boolean) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(if (online) "You're online" else "You're offline", style = MaterialTheme.typography.titleMedium)
                Text(
                    when {
                        !enabled -> "Available once your KYC is approved"
                        online -> "Sharing live location, receiving orders"
                        else -> "Go online to receive delivery requests"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (busy) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                Switch(checked = online, onCheckedChange = onToggle, enabled = enabled)
            }
        }
    }
}

@Composable
private fun ActiveDeliveryCard(
    order: Order,
    riderLat: Double?,
    riderLng: Double?,
    busy: Boolean,
    onReject: () -> Unit,
    onPickup: () -> Unit,
    onDeliver: () -> Unit,
    onChat: () -> Unit,
) {
    val context = LocalContext.current
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Active delivery", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))
            RiderMapView(
                riderLat = riderLat,
                riderLng = riderLng,
                customerLat = order.cust_lat,
                customerLng = order.cust_lng,
                modifier = Modifier.fillMaxWidth().height(180.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(order.address ?: "Address unavailable", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "${order.items.size} item(s) · ${order.status.replace('_', ' ')}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text("You earn ₹%.2f".format(order.rider_payout), style = MaterialTheme.typography.titleSmall, color = LsPrimary)

            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!order.phone.isNullOrBlank()) {
                    OutlinedButton(onClick = {
                        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${order.phone}")))
                    }) { Text("Call") }
                }
                OutlinedButton(onClick = onChat) { Text("Chat") }
            }

            Spacer(Modifier.height(14.dp))
            val pickedUp = order.status == "out_for_delivery" || order.delivery_status == "picked_up"
            if (!pickedUp) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onReject, enabled = !busy) { Text("Reject") }
                    LsPrimaryButton(text = "Picked up", onClick = onPickup, loading = busy, modifier = Modifier.weight(1f))
                }
            } else {
                LsPrimaryButton(text = "Mark delivered (OTP)", onClick = onDeliver, loading = busy)
            }
        }
    }
}

@Composable
private fun AvailableOrderCard(order: Order, busy: Boolean, onAccept: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(order.address ?: "Address unavailable", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "${order.items.size} item(s) · ${"%.1f".format(order.delivery_distance_km)} km",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text("You earn ₹%.2f".format(order.rider_payout), style = MaterialTheme.typography.titleSmall, color = LsPrimary)
            Spacer(Modifier.height(10.dp))
            LsPrimaryButton(text = "Accept", onClick = onAccept, loading = busy)
        }
    }
}

@Composable
private fun DeliverOtpDialog(busy: Boolean, error: String?, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var otp by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enter delivery OTP") },
        text = {
            Column {
                Text("Ask the customer for the 4-digit OTP to confirm handoff.", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(12.dp))
                LsTextField(value = otp, onValueChange = { if (it.length <= 6) otp = it }, label = "OTP", keyboardType = KeyboardType.Number)
                if (error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(otp) }, enabled = otp.isNotBlank() && !busy) {
                Text(if (busy) "Verifying..." else "Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) { Text("Cancel") }
        },
    )
}
