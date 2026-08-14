package com.lazyshopper.app.feature.admin.people

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lazyshopper.app.core.data.remote.ApiResult
import com.lazyshopper.app.core.data.remote.dto.AdminDeliveryEarningsResponse
import com.lazyshopper.app.core.data.remote.dto.DeliveryPartnerInput
import com.lazyshopper.app.core.data.remote.dto.DeliveryPartnerSummary
import com.lazyshopper.app.core.data.remote.dto.DeliverySettlementBatch
import com.lazyshopper.app.core.ui.ActionState
import com.lazyshopper.app.core.ui.UiState
import com.lazyshopper.app.core.ui.components.LsTextField
import com.lazyshopper.app.feature.admin.common.AdminLazyList
import com.lazyshopper.app.feature.admin.common.AdminScreenScaffold
import com.lazyshopper.app.feature.admin.common.CompactRow
import com.lazyshopper.app.feature.admin.common.EmptyOrList
import com.lazyshopper.app.feature.admin.common.FormDialog
import com.lazyshopper.app.feature.admin.common.StatTileRow
import com.lazyshopper.app.feature.admin.common.StatusChip
import com.lazyshopper.app.feature.admin.common.money
import com.lazyshopper.app.feature.admin.data.AdminPeopleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeliveryPartnersViewModel @Inject constructor(private val repo: AdminPeopleRepository) : ViewModel() {
    private val _partners = MutableStateFlow<UiState<List<DeliveryPartnerSummary>>>(UiState.Loading)
    val partners: StateFlow<UiState<List<DeliveryPartnerSummary>>> = _partners.asStateFlow()
    private val _earnings = MutableStateFlow<UiState<AdminDeliveryEarningsResponse>>(UiState.Loading)
    val earnings: StateFlow<UiState<AdminDeliveryEarningsResponse>> = _earnings.asStateFlow()
    private val _settlements = MutableStateFlow<UiState<List<DeliverySettlementBatch>>>(UiState.Loading)
    val settlements: StateFlow<UiState<List<DeliverySettlementBatch>>> = _settlements.asStateFlow()
    private val _actionState = MutableStateFlow<ActionState>(ActionState.Idle)
    val actionState: StateFlow<ActionState> = _actionState.asStateFlow()

    fun loadAll() {
        viewModelScope.launch {
            _partners.value = UiState.Loading
            when (val r = repo.deliveryPartners()) {
                is ApiResult.Success -> _partners.value = UiState.Success(r.data)
                is ApiResult.Failure -> _partners.value = UiState.Error(r.message)
            }
        }
        viewModelScope.launch {
            _earnings.value = UiState.Loading
            when (val r = repo.deliveryEarnings()) {
                is ApiResult.Success -> _earnings.value = UiState.Success(r.data)
                is ApiResult.Failure -> _earnings.value = UiState.Error(r.message)
            }
        }
        viewModelScope.launch {
            _settlements.value = UiState.Loading
            when (val r = repo.deliverySettlements()) {
                is ApiResult.Success -> _settlements.value = UiState.Success(r.data)
                is ApiResult.Failure -> _settlements.value = UiState.Error(r.message)
            }
        }
    }

    fun create(input: DeliveryPartnerInput) {
        viewModelScope.launch {
            _actionState.value = ActionState.InFlight
            when (val r = repo.createDeliveryPartner(input)) {
                is ApiResult.Success -> { _actionState.value = ActionState.Done; loadAll() }
                is ApiResult.Failure -> _actionState.value = ActionState.Failed(r.message)
            }
        }
    }

    fun update(id: String, name: String?, phone: String?, password: String?) {
        viewModelScope.launch {
            _actionState.value = ActionState.InFlight
            when (val r = repo.updateDeliveryPartner(id, name, phone, password)) {
                is ApiResult.Success -> { _actionState.value = ActionState.Done; loadAll() }
                is ApiResult.Failure -> _actionState.value = ActionState.Failed(r.message)
            }
        }
    }

    fun settle(id: String) = viewModelScope.launch {
        when (val r = repo.settleDeliveryPayout(id)) {
            is ApiResult.Success -> loadAll()
            is ApiResult.Failure -> _actionState.value = ActionState.Failed(r.message)
        }
    }
}

@Composable
fun AdminDeliveryPartnersScreen(onMenuClick: () -> Unit, viewModel: DeliveryPartnersViewModel = hiltViewModel()) {
    val partnersState by viewModel.partners.collectAsState()
    val earningsState by viewModel.earnings.collectAsState()
    val settlementsState by viewModel.settlements.collectAsState()
    val actionState by viewModel.actionState.collectAsState()
    LaunchedEffect(Unit) { viewModel.loadAll() }

    var tab by remember { mutableIntStateOf(0) }
    var creating by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<DeliveryPartnerSummary?>(null) }

    AdminScreenScaffold(
        title = "Delivery Partners",
        onMenuClick = onMenuClick,
        fab = { FloatingActionButton(onClick = { creating = true }) { Icon(Icons.Default.Add, contentDescription = "Add partner") } },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Partners") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Earnings") })
                Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text("Settlements") })
            }
            when (tab) {
                0 -> {
                    val s = partnersState
                    val items = (s as? UiState.Success)?.data.orEmpty()
                    EmptyOrList(loading = s is UiState.Loading, error = (s as? UiState.Error)?.message, isEmpty = items.isEmpty() && s is UiState.Success, emptyMessage = "No delivery partners yet", onRetry = viewModel::loadAll) {
                        AdminLazyList {
                            items(items, key = { it.id }) { p ->
                                CompactRow(
                                    title = p.name ?: "-",
                                    subtitle = "${p.email ?: "-"} · ★ ${p.avg_rating} (${p.review_count})",
                                    meta = "active ${p.active_orders} · delivered ${p.delivered_orders}",
                                    badge = { StatusChip(p.status ?: "pending") },
                                    trailing = {
                                        Row {
                                            StatusChip(if (p.available) "online" else "offline")
                                            IconButton(onClick = { editing = p }) { Icon(Icons.Default.Edit, contentDescription = "Edit") }
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
                1 -> {
                    val s = earningsState
                    EmptyOrList(loading = s is UiState.Loading, error = (s as? UiState.Error)?.message, isEmpty = false, emptyMessage = "") {
                        val data = (s as UiState.Success).data
                        Column(Modifier.padding(12.dp)) {
                            StatTileRow(listOf("Total earnings" to data.total_earnings.money(), "Pending" to data.total_pending.money(), "Paid" to data.total_paid.money(), "Fee/delivery" to data.fee.money()))
                            Spacer(Modifier.height(12.dp))
                            AdminLazyList {
                                items(data.partners, key = { it.id }) { p ->
                                    CompactRow(
                                        title = p.name ?: "-",
                                        subtitle = "${p.email ?: "-"} · delivered ${p.delivered} · bank ${if (p.has_bank) "on file" else "missing"}",
                                        meta = "earned ${p.earnings.money()} · pending ${p.pending.money()} · paid ${p.paid.money()}",
                                        badge = { StatusChip(p.status ?: "pending") },
                                        trailing = {
                                            TextButton(onClick = { viewModel.settle(p.id) }, enabled = p.pending > 0 && actionState !is ActionState.InFlight) { Text("Settle") }
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
                2 -> {
                    val s = settlementsState
                    val items = (s as? UiState.Success)?.data.orEmpty()
                    EmptyOrList(loading = s is UiState.Loading, error = (s as? UiState.Error)?.message, isEmpty = items.isEmpty() && s is UiState.Success, emptyMessage = "No settlements yet", onRetry = viewModel::loadAll) {
                        AdminLazyList {
                            items(items, key = { it.batch_id }) { b ->
                                CompactRow(
                                    title = b.partner_name ?: "-",
                                    subtitle = "${b.orders} orders · ${b.amount.money()}",
                                    meta = "settled ${b.settled_at ?: "-"} by ${b.settled_by ?: "-"}",
                                    badge = { StatusChip(b.payout_status ?: "manual") },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (creating) {
        DeliveryPartnerCreateDialog(actionState is ActionState.InFlight, onDismiss = { creating = false }, onSave = { input -> viewModel.create(input); creating = false })
    }
    editing?.let { p ->
        DeliveryPartnerEditDialog(p, actionState is ActionState.InFlight, onDismiss = { editing = null }, onSave = { name, phone, pass -> viewModel.update(p.id, name, phone, pass); editing = null })
    }
}

@Composable
private fun DeliveryPartnerCreateDialog(saving: Boolean, onDismiss: () -> Unit, onSave: (DeliveryPartnerInput) -> Unit) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    FormDialog(title = "New delivery partner", onDismiss = onDismiss, saving = saving, onSave = { onSave(DeliveryPartnerInput(name.trim(), email.trim(), phone.ifBlank { null }, password)) }) {
        LsTextField(name, { name = it }, "Name")
        Spacer(Modifier.height(8.dp))
        LsTextField(email, { email = it }, "Email", keyboardType = KeyboardType.Email)
        Spacer(Modifier.height(8.dp))
        LsTextField(phone, { phone = it }, "Phone (optional)", keyboardType = KeyboardType.Phone)
        Spacer(Modifier.height(8.dp))
        LsTextField(password, { password = it }, "Password", isPassword = true)
    }
}

@Composable
private fun DeliveryPartnerEditDialog(existing: DeliveryPartnerSummary, saving: Boolean, onDismiss: () -> Unit, onSave: (String?, String?, String?) -> Unit) {
    var name by remember { mutableStateOf(existing.name.orEmpty()) }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    FormDialog(title = "Edit · ${existing.name}", onDismiss = onDismiss, saving = saving, onSave = { onSave(name.ifBlank { null }, phone.ifBlank { null }, password.ifBlank { null }) }) {
        LsTextField(name, { name = it }, "Name")
        Spacer(Modifier.height(8.dp))
        LsTextField(phone, { phone = it }, "New phone (optional)", keyboardType = KeyboardType.Phone)
        Spacer(Modifier.height(8.dp))
        LsTextField(password, { password = it }, "New password (optional)", isPassword = true)
    }
}
