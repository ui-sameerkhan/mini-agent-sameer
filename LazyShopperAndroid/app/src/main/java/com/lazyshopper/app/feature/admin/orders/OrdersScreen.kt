package com.lazyshopper.app.feature.admin.orders

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lazyshopper.app.core.data.remote.ApiResult
import com.lazyshopper.app.core.data.remote.dto.DeliveryPartnerSummary
import com.lazyshopper.app.core.data.remote.dto.Order
import com.lazyshopper.app.core.data.remote.dto.OrderDistributionResponse
import com.lazyshopper.app.core.ui.ActionState
import com.lazyshopper.app.core.ui.UiState
import com.lazyshopper.app.feature.admin.common.AdminLazyList
import com.lazyshopper.app.feature.admin.common.AdminScreenScaffold
import com.lazyshopper.app.feature.admin.common.AdminSearchField
import com.lazyshopper.app.feature.admin.common.CompactRow
import com.lazyshopper.app.feature.admin.common.ConfirmDialog
import com.lazyshopper.app.feature.admin.common.EmptyOrList
import com.lazyshopper.app.feature.admin.common.FilterChipsRow
import com.lazyshopper.app.feature.admin.common.KeyValueRow
import com.lazyshopper.app.feature.admin.common.StatusChip
import com.lazyshopper.app.feature.admin.common.money
import com.lazyshopper.app.feature.admin.data.AdminOrdersRepository
import com.lazyshopper.app.feature.admin.data.AdminPeopleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private val ORDER_STATUSES = listOf("placed", "accepted", "preparing", "ready", "packed", "out_for_delivery", "delivered", "cancelled")

@HiltViewModel
class AdminOrdersViewModel @Inject constructor(
    private val repo: AdminOrdersRepository,
    private val peopleRepo: AdminPeopleRepository,
) : ViewModel() {
    private val _state = MutableStateFlow<UiState<List<Order>>>(UiState.Loading)
    val state: StateFlow<UiState<List<Order>>> = _state.asStateFlow()
    private val _actionState = MutableStateFlow<ActionState>(ActionState.Idle)
    val actionState: StateFlow<ActionState> = _actionState.asStateFlow()
    private val _partners = MutableStateFlow<List<DeliveryPartnerSummary>>(emptyList())
    val partners: StateFlow<List<DeliveryPartnerSummary>> = _partners.asStateFlow()
    private val _distribution = MutableStateFlow<OrderDistributionResponse?>(null)
    val distribution: StateFlow<OrderDistributionResponse?> = _distribution.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            when (val r = repo.allOrders()) {
                is ApiResult.Success -> _state.value = UiState.Success(r.data.sortedByDescending { it.created_at })
                is ApiResult.Failure -> _state.value = UiState.Error(r.message)
            }
        }
        viewModelScope.launch {
            when (val r = peopleRepo.deliveryPartners()) {
                is ApiResult.Success -> _partners.value = r.data
                else -> {}
            }
        }
    }

    fun updateStatus(id: String, status: String) = runAction { repo.updateStatus(id, status) }
    fun assign(orderId: String, deliveryId: String) = runAction { repo.assign(orderId, deliveryId) }
    fun cancel(orderId: String) = runAction { repo.cancel(orderId) }
    fun refund(orderId: String) = runAction { repo.refund(orderId) }

    fun loadDistribution(orderId: String) {
        viewModelScope.launch {
            _distribution.value = null
            when (val r = repo.distribution(orderId)) {
                is ApiResult.Success -> _distribution.value = r.data
                is ApiResult.Failure -> _actionState.value = ActionState.Failed(r.message)
            }
        }
    }
    fun clearDistribution() { _distribution.value = null }

    private fun runAction(block: suspend () -> ApiResult<*>) {
        viewModelScope.launch {
            _actionState.value = ActionState.InFlight
            when (val r = block()) {
                is ApiResult.Success -> { _actionState.value = ActionState.Done; load() }
                is ApiResult.Failure -> _actionState.value = ActionState.Failed(r.message)
            }
        }
    }
}

@Composable
fun AdminOrdersScreen(onMenuClick: () -> Unit, viewModel: AdminOrdersViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val partners by viewModel.partners.collectAsState()
    val distribution by viewModel.distribution.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }

    var query by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf<String?>(null) }
    var assigningOrder by remember { mutableStateOf<Order?>(null) }
    var distributionOrder by remember { mutableStateOf<Order?>(null) }
    var confirmCancel by remember { mutableStateOf<Order?>(null) }
    var confirmRefund by remember { mutableStateOf<Order?>(null) }

    AdminScreenScaffold(title = "Orders", onMenuClick = onMenuClick) { padding ->
        Column(Modifier.padding(padding)) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                AdminSearchField(query, { query = it }, placeholder = "Search by order id / customer…")
                Spacer(Modifier.height(8.dp))
                FilterChipsRow(ORDER_STATUSES, statusFilter, { statusFilter = it })
            }
            val s = state
            val all = (s as? UiState.Success)?.data.orEmpty()
            val filtered = all.filter {
                (statusFilter == null || it.status == statusFilter) &&
                    (query.isBlank() || it.id.contains(query, true) || it.customer_name?.contains(query, true) == true)
            }
            EmptyOrList(loading = s is UiState.Loading, error = (s as? UiState.Error)?.message, isEmpty = filtered.isEmpty() && s is UiState.Success, emptyMessage = "No orders found", onRetry = viewModel::load) {
                AdminLazyList {
                    items(filtered, key = { it.id }) { o ->
                        var menuOpen by remember { mutableStateOf(false) }
                        CompactRow(
                            title = "#${o.id.takeLast(6)} · ${o.customer_name ?: "Customer"}",
                            subtitle = "${o.items.size} items · ${o.payment_method ?: "-"} (${o.payment_status ?: "-"})",
                            meta = "${o.total.money()} · rider: ${o.delivery_partner_name ?: "unassigned"}",
                            badge = { StatusChip(o.status) },
                            trailing = {
                                Row {
                                    IconButton(onClick = { menuOpen = true }) { Icon(Icons.Default.MoreVert, contentDescription = "Actions") }
                                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                        DropdownMenuItem(text = { Text("View split") }, onClick = { menuOpen = false; distributionOrder = o; viewModel.loadDistribution(o.id) })
                                        DropdownMenuItem(text = { Text("Assign rider") }, onClick = { menuOpen = false; assigningOrder = o })
                                        androidx.compose.material3.Divider()
                                        ORDER_STATUSES.forEach { st ->
                                            DropdownMenuItem(text = { Text("Set: $st") }, onClick = { menuOpen = false; viewModel.updateStatus(o.id, st) })
                                        }
                                        androidx.compose.material3.Divider()
                                        DropdownMenuItem(text = { Text("Cancel order") }, onClick = { menuOpen = false; confirmCancel = o })
                                        DropdownMenuItem(text = { Text("Refund order") }, onClick = { menuOpen = false; confirmRefund = o })
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    assigningOrder?.let { o ->
        AssignRiderDialog(order = o, partners = partners, onDismiss = { assigningOrder = null }, onAssign = { pid -> viewModel.assign(o.id, pid); assigningOrder = null })
    }
    distributionOrder?.let { o ->
        DistributionDialog(order = o, distribution = distribution, onDismiss = { distributionOrder = null; viewModel.clearDistribution() })
    }
    confirmCancel?.let { o ->
        ConfirmDialog("Cancel order", "Cancel order #${o.id.takeLast(6)}?", destructive = true, confirmLabel = "Cancel order", onConfirm = { viewModel.cancel(o.id); confirmCancel = null }, onDismiss = { confirmCancel = null })
    }
    confirmRefund?.let { o ->
        ConfirmDialog("Refund order", "Refund order #${o.id.takeLast(6)}?", destructive = true, confirmLabel = "Refund", onConfirm = { viewModel.refund(o.id); confirmRefund = null }, onDismiss = { confirmRefund = null })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssignRiderDialog(order: Order, partners: List<DeliveryPartnerSummary>, onDismiss: () -> Unit, onAssign: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<DeliveryPartnerSummary?>(null) }
    Dialog(onDismissRequest = onDismiss) {
        androidx.compose.material3.Card {
            Column(Modifier.padding(20.dp)) {
                Text("Assign rider · #${order.id.takeLast(6)}", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = selected?.name ?: "Select rider",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        partners.forEach { p ->
                            DropdownMenuItem(text = { Text("${p.name ?: p.id} ${if (p.available) "· online" else "· offline"}") }, onClick = { selected = p; expanded = false })
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = { selected?.let { onAssign(it.id) } }, enabled = selected != null) { Text("Assign") }
                }
            }
        }
    }
}

@Composable
private fun DistributionDialog(order: Order, distribution: OrderDistributionResponse?, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        androidx.compose.material3.Card {
            Column(Modifier.padding(20.dp)) {
                Text("Money split · #${order.id.takeLast(6)}", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                if (distribution == null) {
                    Text("Loading…", style = MaterialTheme.typography.bodySmall)
                } else {
                    KeyValueRow("Order total", distribution.total.money())
                    KeyValueRow("Shopkeeper amount", distribution.shopkeeper_amount.money())
                    KeyValueRow("Commission (${distribution.commission_pct}%)", distribution.commission.money())
                    KeyValueRow("Rider payout", distribution.rider_payout.money())
                    KeyValueRow("  ↳ distance fee", distribution.distance_fee.money())
                    KeyValueRow("  ↳ weight incentive", distribution.weight_incentive.money())
                    KeyValueRow("Distance", "${distribution.distance_km} km")
                    KeyValueRow("Weight", "${distribution.weight_kg} kg")
                }
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Close") }
                }
            }
        }
    }
}
