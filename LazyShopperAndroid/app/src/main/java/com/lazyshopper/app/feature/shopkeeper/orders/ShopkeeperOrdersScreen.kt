package com.lazyshopper.app.feature.shopkeeper.orders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lazyshopper.app.core.data.remote.dto.Order
import com.lazyshopper.app.core.data.remote.dto.OrderItem
import com.lazyshopper.app.core.theme.LsError
import com.lazyshopper.app.core.theme.LsPrimary
import com.lazyshopper.app.core.theme.LsWarning
import com.lazyshopper.app.core.ui.ActionState
import com.lazyshopper.app.core.ui.UiState
import com.lazyshopper.app.core.ui.components.EmptyState
import com.lazyshopper.app.core.ui.components.ErrorState
import com.lazyshopper.app.core.ui.components.FullScreenLoading
import com.lazyshopper.app.core.ui.components.ScreenPadding
import com.lazyshopper.app.feature.shopkeeper.common.money
import com.lazyshopper.app.feature.shopkeeper.common.statusLabel

/** Line items belonging to this shopkeeper only — a multi-vendor cart's order can carry other shops' items too. */
private fun Order.myItems(myUserId: String?): List<OrderItem> =
    if (myUserId == null) items else items.filter { it.owner_id == myUserId }

@Composable
fun ShopkeeperOrdersScreen(viewModel: ShopkeeperOrdersViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    val selected = (state.orders as? UiState.Success)?.data?.firstOrNull { it.id == state.selectedOrderId }
    if (selected != null) {
        OrderDetailScreen(
            order = selected,
            myUserId = state.myUserId,
            busy = state.statusUpdateState is ActionState.InFlight,
            error = (state.statusUpdateState as? ActionState.Failed)?.message,
            onBack = { viewModel.selectOrder(null) },
            onSetStatus = { status -> viewModel.updateStatus(selected.id, status) },
        )
        return
    }

    Column(Modifier.fillMaxSize()) {
        Text("Orders", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(ScreenPadding))
        when (val s = state.orders) {
            is UiState.Loading -> FullScreenLoading()
            is UiState.Error -> ErrorState(s.message, onRetry = viewModel::load)
            is UiState.Idle -> {}
            is UiState.Success -> {
                if (s.data.isEmpty()) {
                    EmptyState("No orders yet.")
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(s.data, key = { it.id }) { order ->
                            OrderCard(order = order, myUserId = state.myUserId, onClick = { viewModel.selectOrder(order.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderCard(order: Order, myUserId: String?, onClick: () -> Unit) {
    val myItems = order.myItems(myUserId)
    val myTotal = myItems.sumOf { it.line_total }
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("#${order.id.takeLast(6)}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                StatusChip(order.status)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                order.created_at.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${myItems.size} item(s) of yours · ${money(myTotal)}",
                style = MaterialTheme.typography.bodyMedium,
            )
            if (order.customer_name != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    "Customer: ${order.customer_name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun StatusChip(status: String) {
    val tint = when (status) {
        "delivered" -> LsPrimary
        "cancelled" -> LsError
        else -> LsWarning
    }
    Text(statusLabel(status), style = MaterialTheme.typography.labelMedium, color = tint)
}

@Composable
private fun OrderDetailScreen(
    order: Order,
    myUserId: String?,
    busy: Boolean,
    error: String?,
    onBack: () -> Unit,
    onSetStatus: (String) -> Unit,
) {
    val myItems = order.myItems(myUserId)
    val myTotal = myItems.sumOf { it.line_total }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(ScreenPadding),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
            Text("Order #${order.id.takeLast(6)}", style = MaterialTheme.typography.headlineSmall)
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth().padding(start = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Status", style = MaterialTheme.typography.labelLarge)
            StatusChip(order.status)
        }

        Spacer(Modifier.height(16.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Customer", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(4.dp))
                Text(order.customer_name ?: "—", style = MaterialTheme.typography.bodyMedium)
                if (!order.phone.isNullOrBlank()) {
                    Text(order.phone.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (!order.address.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(order.address.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (!order.delivery_slot.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text("Slot: ${order.delivery_slot}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("Your items", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        if (myItems.isEmpty()) {
            Text("None of this order's items belong to your shop(s).", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    myItems.forEachIndexed { index, item ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${item.name} × ${item.qty}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            Text(money(item.line_total), style = MaterialTheme.typography.bodyMedium)
                        }
                        if (index != myItems.lastIndex) Spacer(Modifier.height(6.dp))
                    }
                    HorizontalDivider(Modifier.padding(vertical = 10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Your total", style = MaterialTheme.typography.titleSmall)
                        Text(money(myTotal), style = MaterialTheme.typography.titleSmall, color = LsPrimary)
                    }
                }
            }
        }

        if (order.items.size != myItems.size) {
            Spacer(Modifier.height(8.dp))
            Text(
                "This is a multi-vendor order — ${order.items.size - myItems.size} item(s) belong to other shops.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(20.dp))
        Text("Update status", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SHOPKEEPER_ORDER_STATUSES.forEach { status ->
                OutlinedButton(onClick = { onSetStatus(status) }, enabled = !busy) {
                    Text(statusLabel(status) + if (order.status == status) " ✓" else "")
                }
            }
        }
        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(24.dp))
    }
}
