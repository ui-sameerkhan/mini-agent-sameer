package com.lazyshopper.app.feature.admin.finance

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lazyshopper.app.core.data.remote.ApiResult
import com.lazyshopper.app.core.data.remote.dto.PayoutOrderDetail
import com.lazyshopper.app.core.data.remote.dto.PayoutShopkeeperRow
import com.lazyshopper.app.core.data.remote.dto.PayoutsResponse
import com.lazyshopper.app.core.data.remote.dto.Settlement
import com.lazyshopper.app.core.ui.ActionState
import com.lazyshopper.app.core.ui.UiState
import com.lazyshopper.app.feature.admin.common.AdminLazyList
import com.lazyshopper.app.feature.admin.common.AdminScreenScaffold
import com.lazyshopper.app.feature.admin.common.CompactRow
import com.lazyshopper.app.feature.admin.common.ConfirmDialog
import com.lazyshopper.app.feature.admin.common.EmptyOrList
import com.lazyshopper.app.feature.admin.common.KeyValueRow
import com.lazyshopper.app.feature.admin.common.StatTileRow
import com.lazyshopper.app.feature.admin.common.StatusChip
import com.lazyshopper.app.feature.admin.common.money
import com.lazyshopper.app.feature.admin.data.AdminFinanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PayoutsViewModel @Inject constructor(private val repo: AdminFinanceRepository) : ViewModel() {
    private val _payouts = MutableStateFlow<UiState<PayoutsResponse>>(UiState.Loading)
    val payouts: StateFlow<UiState<PayoutsResponse>> = _payouts.asStateFlow()
    private val _settlements = MutableStateFlow<UiState<List<Settlement>>>(UiState.Loading)
    val settlements: StateFlow<UiState<List<Settlement>>> = _settlements.asStateFlow()
    private val _actionState = MutableStateFlow<ActionState>(ActionState.Idle)
    val actionState: StateFlow<ActionState> = _actionState.asStateFlow()
    private val _orderDetail = MutableStateFlow<Map<String, List<PayoutOrderDetail>>>(emptyMap())
    val orderDetail: StateFlow<Map<String, List<PayoutOrderDetail>>> = _orderDetail.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _payouts.value = UiState.Loading
            when (val r = repo.payouts()) {
                is ApiResult.Success -> _payouts.value = UiState.Success(r.data)
                is ApiResult.Failure -> _payouts.value = UiState.Error(r.message)
            }
        }
        viewModelScope.launch {
            _settlements.value = UiState.Loading
            when (val r = repo.settlements()) {
                is ApiResult.Success -> _settlements.value = UiState.Success(r.data)
                is ApiResult.Failure -> _settlements.value = UiState.Error(r.message)
            }
        }
    }

    fun settle(id: String) = viewModelScope.launch {
        _actionState.value = ActionState.InFlight
        when (val r = repo.settlePayout(id)) {
            is ApiResult.Success -> { _actionState.value = ActionState.Done; load() }
            is ApiResult.Failure -> _actionState.value = ActionState.Failed(r.message)
        }
    }

    fun settleAll() = viewModelScope.launch {
        _actionState.value = ActionState.InFlight
        when (val r = repo.settleAll()) {
            is ApiResult.Success -> { _actionState.value = ActionState.Done; load() }
            is ApiResult.Failure -> _actionState.value = ActionState.Failed(r.message)
        }
    }

    fun schedule(id: String, days: Int) = viewModelScope.launch {
        when (val r = repo.schedulePayout(id, days = days)) {
            is ApiResult.Success -> load()
            is ApiResult.Failure -> _actionState.value = ActionState.Failed(r.message)
        }
    }

    fun toggleOrders(shopkeeperId: String) {
        viewModelScope.launch {
            if (_orderDetail.value.containsKey(shopkeeperId)) {
                _orderDetail.value = _orderDetail.value - shopkeeperId
                return@launch
            }
            when (val r = repo.payoutOrders(shopkeeperId)) {
                is ApiResult.Success -> _orderDetail.value = _orderDetail.value + (shopkeeperId to r.data.orders)
                is ApiResult.Failure -> _actionState.value = ActionState.Failed(r.message)
            }
        }
    }
}

@Composable
fun AdminPayoutsScreen(onMenuClick: () -> Unit, viewModel: PayoutsViewModel = hiltViewModel()) {
    val payoutsState by viewModel.payouts.collectAsState()
    val settlementsState by viewModel.settlements.collectAsState()
    val actionState by viewModel.actionState.collectAsState()
    val orderDetail by viewModel.orderDetail.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }
    var tab by remember { mutableIntStateOf(0) }
    var confirmSettleAll by remember { mutableStateOf(false) }

    AdminScreenScaffold(title = "Shopkeeper Payouts", onMenuClick = onMenuClick) { padding ->
        Column(Modifier.padding(padding)) {
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Payouts") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Settlement log") })
            }
            when (tab) {
                0 -> {
                    val s = payoutsState
                    EmptyOrList(loading = s is UiState.Loading, error = (s as? UiState.Error)?.message, isEmpty = false, emptyMessage = "", onRetry = viewModel::load) {
                        val data = (s as UiState.Success).data
                        Column {
                            StatTileRow(
                                listOf(
                                    "Total sales" to data.total_sales.money(),
                                    "Total profit" to data.total_profit.money(),
                                    "Pending" to data.total_pending.money(),
                                    "Paid" to data.total_paid.money(),
                                ),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            )
                            Row(Modifier.padding(horizontal = 12.dp)) {
                                Text("Cycle: every ${data.settlement_cycle_days} days", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 8.dp))
                                Spacer(Modifier.width(12.dp))
                                TextButton(onClick = { confirmSettleAll = true }, enabled = actionState !is ActionState.InFlight) { Text("Settle all eligible") }
                            }
                            AdminLazyList {
                                items(data.shopkeepers, key = { it.shopkeeper_id }) { row ->
                                    Column {
                                        CompactRow(
                                            title = row.shop_name ?: row.name ?: "-",
                                            subtitle = "${row.name ?: "-"} · ${row.email ?: "-"} · ${row.orders} orders",
                                            meta = "sales ${row.sales.money()} · earning ${row.earning.money()}",
                                            badge = { StatusChip(row.settlement_status ?: "pending") },
                                            trailing = {
                                                Row {
                                                    TextButton(onClick = { viewModel.toggleOrders(row.shopkeeper_id) }) { Text("Orders (${row.orders})") }
                                                }
                                            },
                                            onClick = { viewModel.toggleOrders(row.shopkeeper_id) },
                                        ) {
                                            Row {
                                                Text("Pending ${row.pending.money()} · Paid ${row.paid.money()}", style = MaterialTheme.typography.labelSmall)
                                            }
                                            Row(Modifier.padding(top = 4.dp)) {
                                                TextButton(onClick = { viewModel.settle(row.shopkeeper_id) }, enabled = row.eligible && actionState !is ActionState.InFlight) { Text("Settle now") }
                                                Spacer(Modifier.width(4.dp))
                                                TextButton(onClick = { viewModel.schedule(row.shopkeeper_id, 7) }, enabled = actionState !is ActionState.InFlight) { Text("Schedule +7d") }
                                            }
                                            val orders = orderDetail[row.shopkeeper_id]
                                            if (orders != null) {
                                                Spacer(Modifier.height(6.dp))
                                                orders.forEach { o ->
                                                    KeyValueRow("#${o.order_id.takeLast(6)} (${o.status ?: "-"})", o.earning.money())
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    val s = settlementsState
                    val items = (s as? UiState.Success)?.data.orEmpty()
                    EmptyOrList(loading = s is UiState.Loading, error = (s as? UiState.Error)?.message, isEmpty = items.isEmpty() && s is UiState.Success, emptyMessage = "No settlements yet", onRetry = viewModel::load) {
                        AdminLazyList {
                            items(items, key = { it.id }) { st ->
                                CompactRow(
                                    title = st.shopkeeper_name ?: st.owner_id ?: "-",
                                    subtitle = "${st.shop_name ?: "-"} · order ${st.order_id ?: "-"}",
                                    meta = "amount ${st.amount.money()} · settled ${st.settled_at ?: "-"}",
                                    badge = { StatusChip(st.payout_status ?: "manual") },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (confirmSettleAll) {
        ConfirmDialog("Settle all eligible payouts", "This settles every shopkeeper with pending ≥ minimum payout. Continue?", confirmLabel = "Settle all", onConfirm = { viewModel.settleAll(); confirmSettleAll = false }, onDismiss = { confirmSettleAll = false })
    }
}
