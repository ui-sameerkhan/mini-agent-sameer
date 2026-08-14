package com.lazyshopper.app.feature.customer.orders

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lazyshopper.app.core.data.remote.dto.Order
import com.lazyshopper.app.core.theme.LsAccent
import com.lazyshopper.app.core.theme.LsError
import com.lazyshopper.app.core.theme.LsPrimary
import com.lazyshopper.app.core.theme.LsWarning
import com.lazyshopper.app.core.ui.UiState
import com.lazyshopper.app.core.ui.components.EmptyState
import com.lazyshopper.app.core.ui.components.ErrorState
import com.lazyshopper.app.core.ui.components.FullScreenLoading
import com.lazyshopper.app.feature.customer.util.formatMoney
import com.lazyshopper.app.feature.customer.util.formatQty

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(
    onTrackOrder: (String) -> Unit,
    viewModel: OrdersViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }

    Scaffold(
        topBar = { TopAppBar(title = { Text("My Orders") }) },
    ) { padding ->
        when (val ordersState = state.orders) {
            is UiState.Loading -> FullScreenLoading(Modifier.padding(padding))
            is UiState.Error -> ErrorState(ordersState.message, Modifier.padding(padding), onRetry = viewModel::load)
            is UiState.Success -> {
                val orders = ordersState.data
                if (orders.isEmpty()) {
                    EmptyState("No orders yet — go find something delicious.", Modifier.padding(padding))
                } else {
                    LazyColumn(
                        modifier = Modifier.padding(padding).fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(orders, key = { it.id }) { order ->
                            OrderCard(
                                order = order,
                                expanded = state.expandedOrderId == order.id,
                                onToggleExpand = { viewModel.toggleExpanded(order.id) },
                                onTrack = { onTrackOrder(order.id) },
                            )
                        }
                    }
                }
            }
            UiState.Idle -> {}
        }
    }
}

@Composable
private fun OrderCard(
    order: Order,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onTrack: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        onClick = onToggleExpand,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Order #${order.id.takeLast(6).uppercase()}", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "${order.items.size} item(s) · ${formatMoney(order.total)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                StatusBadge(order.status)
            }

            if (order.isActive() && !order.delivery_otp.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    color = LsPrimary.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text(
                        "Delivery OTP: ${order.delivery_otp}",
                        style = MaterialTheme.typography.labelLarge,
                        color = LsPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(Modifier.height(10.dp))
                    Divider()
                    Spacer(Modifier.height(10.dp))
                    order.items.forEach { item ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${item.name} x${formatQty(item.qty)}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                            Text(formatMoney(item.line_total), style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                    Spacer(Modifier.height(6.dp))
                    PriceRow("Subtotal", order.subtotal)
                    if (order.discount > 0.0) PriceRow("Discount", -order.discount)
                    if (order.referral_discount > 0.0) PriceRow("Referral discount", -order.referral_discount)
                    if (order.wallet_used > 0.0) PriceRow("Wallet used", -order.wallet_used)
                    PriceRow("Platform fee", order.platform_fee)
                    PriceRow("Delivery fee", order.delivery_fee)
                    Spacer(Modifier.height(4.dp))
                    Divider()
                    Spacer(Modifier.height(4.dp))
                    PriceRow("Total", order.total, emphasize = true)
                    if (!order.address.isNullOrBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(order.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            if (order.isActive()) {
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = onTrack, modifier = Modifier.fillMaxWidth()) {
                    Text("Track order")
                }
            }
        }
    }
}

@Composable
private fun PriceRow(label: String, amount: Double, emphasize: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = if (emphasize) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodySmall)
        Text(formatMoney(amount), style = if (emphasize) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodySmall)
    }
    Spacer(Modifier.height(2.dp))
}

@Composable
private fun StatusBadge(status: String) {
    val (bg, label) = when (status) {
        "delivered" -> LsPrimary to "Delivered"
        "cancelled" -> LsError to "Cancelled"
        "out_for_delivery" -> LsAccent to "Out for delivery"
        "placed" -> LsWarning to "Placed"
        else -> LsWarning to status.replace('_', ' ').replaceFirstChar { it.uppercase() }
    }
    Surface(color = bg.copy(alpha = 0.14f), shape = RoundedCornerShape(50)) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = bg,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}
