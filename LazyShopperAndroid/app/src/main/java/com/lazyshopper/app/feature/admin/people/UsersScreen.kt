package com.lazyshopper.app.feature.admin.people

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.lazyshopper.app.core.data.remote.dto.Order
import com.lazyshopper.app.core.data.remote.dto.User
import com.lazyshopper.app.core.ui.ActionState
import com.lazyshopper.app.core.ui.UiState
import com.lazyshopper.app.feature.admin.common.AdminLazyList
import com.lazyshopper.app.feature.admin.common.AdminScreenScaffold
import com.lazyshopper.app.feature.admin.common.AdminSearchField
import com.lazyshopper.app.feature.admin.common.CompactRow
import com.lazyshopper.app.feature.admin.common.ConfirmDialog
import com.lazyshopper.app.feature.admin.common.EmptyOrList
import com.lazyshopper.app.feature.admin.common.FilterChipsRow
import com.lazyshopper.app.feature.admin.common.StatusChip
import com.lazyshopper.app.feature.admin.common.money
import com.lazyshopper.app.feature.admin.data.AdminPeopleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private val ROLES = listOf("customer", "shopkeeper", "delivery")
private val STATUSES = listOf("active", "pending", "suspended", "rejected")

@HiltViewModel
class UsersViewModel @Inject constructor(private val repo: AdminPeopleRepository) : ViewModel() {
    private val _state = MutableStateFlow<UiState<List<User>>>(UiState.Loading)
    val state: StateFlow<UiState<List<User>>> = _state.asStateFlow()
    private val _actionState = MutableStateFlow<ActionState>(ActionState.Idle)
    val actionState: StateFlow<ActionState> = _actionState.asStateFlow()
    private val _orders = MutableStateFlow<UiState<List<Order>>?>(null)
    val orders: StateFlow<UiState<List<Order>>?> = _orders.asStateFlow()

    fun load(role: String? = null, status: String? = null, search: String? = null) {
        viewModelScope.launch {
            _state.value = UiState.Loading
            when (val r = repo.users(role, status, search)) {
                is ApiResult.Success -> _state.value = UiState.Success(r.data)
                is ApiResult.Failure -> _state.value = UiState.Error(r.message)
            }
        }
    }

    fun setStatus(id: String, status: String, reload: () -> Unit) = viewModelScope.launch {
        when (val r = repo.setUserStatus(id, status)) {
            is ApiResult.Success -> reload()
            is ApiResult.Failure -> _actionState.value = ActionState.Failed(r.message)
        }
    }

    fun delete(id: String, reload: () -> Unit) = viewModelScope.launch {
        when (val r = repo.deleteUser(id)) {
            is ApiResult.Success -> reload()
            is ApiResult.Failure -> _actionState.value = ActionState.Failed(r.message)
        }
    }

    fun loadOrders(userId: String) {
        viewModelScope.launch {
            _orders.value = UiState.Loading
            when (val r = repo.userOrders(userId)) {
                is ApiResult.Success -> _orders.value = UiState.Success(r.data)
                is ApiResult.Failure -> _orders.value = UiState.Error(r.message)
            }
        }
    }
    fun clearOrders() { _orders.value = null }
}

@Composable
fun AdminUsersScreen(onMenuClick: () -> Unit, viewModel: UsersViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val orders by viewModel.orders.collectAsState()
    var query by remember { mutableStateOf("") }
    var roleFilter by remember { mutableStateOf<String?>(null) }
    var statusFilter by remember { mutableStateOf<String?>(null) }
    var historyUser by remember { mutableStateOf<User?>(null) }
    var confirmDelete by remember { mutableStateOf<User?>(null) }

    fun reload() = viewModel.load(roleFilter, statusFilter, query.ifBlank { null })
    LaunchedEffect(roleFilter, statusFilter) { reload() }

    AdminScreenScaffold(title = "Users", onMenuClick = onMenuClick) { padding ->
        Column(Modifier.padding(padding)) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                AdminSearchField(query, { query = it }, placeholder = "Search name / email…")
                Spacer(Modifier.height(6.dp))
                TextButton(onClick = { reload() }) { Text("Apply search") }
                Spacer(Modifier.height(4.dp))
                Text("Role", style = MaterialTheme.typography.labelSmall)
                FilterChipsRow(ROLES, roleFilter, { roleFilter = it })
                Spacer(Modifier.height(6.dp))
                Text("Status", style = MaterialTheme.typography.labelSmall)
                FilterChipsRow(STATUSES, statusFilter, { statusFilter = it })
            }
            val s = state
            val items = (s as? UiState.Success)?.data.orEmpty()
            EmptyOrList(loading = s is UiState.Loading, error = (s as? UiState.Error)?.message, isEmpty = items.isEmpty() && s is UiState.Success, emptyMessage = "No users found", onRetry = { reload() }) {
                AdminLazyList {
                    items(items, key = { it.id }) { u ->
                        var menuOpen by remember { mutableStateOf(false) }
                        CompactRow(
                            title = u.name ?: "(no name)",
                            subtitle = "${u.email ?: "-"} · ${u.role}${u.shop_name?.let { " · $it" } ?: ""}",
                            meta = "wallet ${u.referral_wallet.money()} · ${u.phone ?: "no phone"}",
                            badge = { StatusChip(u.status) },
                            trailing = {
                                Row {
                                    IconButton(onClick = { menuOpen = true }) { Icon(Icons.Default.MoreVert, contentDescription = "Actions") }
                                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                        if (u.role == "customer") DropdownMenuItem(text = { Text("View order history") }, onClick = { menuOpen = false; historyUser = u; viewModel.loadOrders(u.id) })
                                        STATUSES.forEach { st ->
                                            if (st != u.status) DropdownMenuItem(text = { Text("Set status: $st") }, onClick = { menuOpen = false; viewModel.setStatus(u.id, st) { reload() } })
                                        }
                                        DropdownMenuItem(text = { Text("Delete") }, onClick = { menuOpen = false; confirmDelete = u })
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    historyUser?.let { u ->
        Dialog(onDismissRequest = { historyUser = null; viewModel.clearOrders() }) {
            Card {
                Column(Modifier.padding(20.dp)) {
                    Text("Orders · ${u.name}", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    when (val o = orders) {
                        is UiState.Success -> if (o.data.isEmpty()) Text("No orders yet") else o.data.take(30).forEach { ord ->
                            Text("#${ord.id.takeLast(6)} · ${ord.status} · ${ord.total.money()}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 2.dp))
                        }
                        is UiState.Error -> Text(o.message, color = MaterialTheme.colorScheme.error)
                        else -> Text("Loading…")
                    }
                    Spacer(Modifier.height(12.dp))
                    TextButton(onClick = { historyUser = null; viewModel.clearOrders() }) { Text("Close") }
                }
            }
        }
    }
    confirmDelete?.let { u ->
        ConfirmDialog("Delete user", "Delete \"${u.name}\"? Their shops/products will also be removed.", destructive = true, confirmLabel = "Delete", onConfirm = { viewModel.delete(u.id) { reload() }; confirmDelete = null }, onDismiss = { confirmDelete = null })
    }
}
