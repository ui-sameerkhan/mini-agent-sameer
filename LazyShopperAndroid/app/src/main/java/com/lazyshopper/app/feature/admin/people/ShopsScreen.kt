package com.lazyshopper.app.feature.admin.people

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lazyshopper.app.core.data.remote.ApiResult
import com.lazyshopper.app.core.data.remote.dto.Shop
import com.lazyshopper.app.core.ui.ActionState
import com.lazyshopper.app.core.ui.UiState
import com.lazyshopper.app.core.ui.components.LsTextField
import com.lazyshopper.app.feature.admin.common.AdminLazyList
import com.lazyshopper.app.feature.admin.common.AdminScreenScaffold
import com.lazyshopper.app.feature.admin.common.AdminSearchField
import com.lazyshopper.app.feature.admin.common.CompactRow
import com.lazyshopper.app.feature.admin.common.ConfirmDialog
import com.lazyshopper.app.feature.admin.common.EmptyOrList
import com.lazyshopper.app.feature.admin.common.FilterChipsRow
import com.lazyshopper.app.feature.admin.common.StatusChip
import com.lazyshopper.app.feature.admin.common.Tone
import com.lazyshopper.app.feature.admin.data.AdminPeopleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private val SHOP_STATUSES = listOf("pending", "approved", "rejected", "suspended")

@HiltViewModel
class ShopsViewModel @Inject constructor(private val repo: AdminPeopleRepository) : ViewModel() {
    private val _state = MutableStateFlow<UiState<List<Shop>>>(UiState.Loading)
    val state: StateFlow<UiState<List<Shop>>> = _state.asStateFlow()
    private val _actionState = MutableStateFlow<ActionState>(ActionState.Idle)
    val actionState: StateFlow<ActionState> = _actionState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            when (val r = repo.shops()) {
                is ApiResult.Success -> _state.value = UiState.Success(r.data)
                is ApiResult.Failure -> _state.value = UiState.Error(r.message)
            }
        }
    }

    fun approve(id: String, status: String) = runAction { repo.approveShop(id, status) }
    fun delete(id: String) = runAction { repo.deleteShop(id) }
    fun promote(id: String, days: Int) = runAction { repo.promoteShop(id, days) }
    fun unpromote(id: String) = runAction { repo.unpromoteShop(id) }
    fun reassign(id: String, ownerId: String) = runAction { repo.reassignShop(id, ownerId) }

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
fun AdminShopsScreen(onMenuClick: () -> Unit, viewModel: ShopsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }
    var query by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf<Shop?>(null) }
    var reassigning by remember { mutableStateOf<Shop?>(null) }
    var promoting by remember { mutableStateOf<Shop?>(null) }

    AdminScreenScaffold(title = "Shops", onMenuClick = onMenuClick) { padding ->
        Column(Modifier.padding(padding)) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                AdminSearchField(query, { query = it }, placeholder = "Search shops…")
                Spacer(Modifier.height(8.dp))
                FilterChipsRow(SHOP_STATUSES, statusFilter, { statusFilter = it })
            }
            val s = state
            val all = (s as? UiState.Success)?.data.orEmpty()
            val filtered = all.filter {
                (statusFilter == null || it.status == statusFilter) &&
                    (query.isBlank() || it.name.contains(query, true) || it.owner_name?.contains(query, true) == true)
            }
            EmptyOrList(loading = s is UiState.Loading, error = (s as? UiState.Error)?.message, isEmpty = filtered.isEmpty() && s is UiState.Success, emptyMessage = "No shops found", onRetry = viewModel::load) {
                AdminLazyList {
                    items(filtered, key = { it.id }) { shop ->
                        var menuOpen by remember { mutableStateOf(false) }
                        CompactRow(
                            title = shop.name,
                            subtitle = "${shop.owner_name ?: "-"} · ${shop.category} · ${shop.area}, ${shop.district}",
                            meta = "★ ${shop.avg_rating ?: 0.0} (${shop.review_count ?: 0}) · ${shop.product_count ?: 0} products",
                            badge = { StatusChip(shop.status ?: "pending") },
                            trailing = {
                                Row {
                                    if (shop.promoted == true) StatusChip("promoted", tone = Tone.INFO)
                                    IconButton(onClick = { menuOpen = true }) { Icon(Icons.Default.MoreVert, contentDescription = "Actions") }
                                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                        SHOP_STATUSES.forEach { st ->
                                            if (st != shop.status) DropdownMenuItem(text = { Text("Set: $st") }, onClick = { menuOpen = false; viewModel.approve(shop.id, st) })
                                        }
                                        androidx.compose.material3.Divider()
                                        if (shop.promoted == true) {
                                            DropdownMenuItem(text = { Text("Unpromote") }, onClick = { menuOpen = false; viewModel.unpromote(shop.id) })
                                        } else {
                                            DropdownMenuItem(text = { Text("Promote…") }, onClick = { menuOpen = false; promoting = shop })
                                        }
                                        DropdownMenuItem(text = { Text("Reassign owner…") }, onClick = { menuOpen = false; reassigning = shop })
                                        DropdownMenuItem(text = { Text("Delete") }, onClick = { menuOpen = false; confirmDelete = shop })
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    reassigning?.let { shop ->
        var ownerId by remember { mutableStateOf("") }
        Dialog(onDismissRequest = { reassigning = null }) {
            androidx.compose.material3.Card {
                Column(Modifier.padding(20.dp)) {
                    Text("Reassign owner · ${shop.name}", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))
                    LsTextField(ownerId, { ownerId = it }, "New owner (shopkeeper) user id")
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.padding(top = 4.dp)) {
                        TextButton(onClick = { reassigning = null }) { Text("Cancel") }
                        Spacer(Modifier.width(8.dp))
                        TextButton(onClick = { viewModel.reassign(shop.id, ownerId.trim()); reassigning = null }, enabled = ownerId.isNotBlank()) { Text("Reassign") }
                    }
                }
            }
        }
    }
    promoting?.let { shop ->
        var days by remember { mutableStateOf("7") }
        Dialog(onDismissRequest = { promoting = null }) {
            androidx.compose.material3.Card {
                Column(Modifier.padding(20.dp)) {
                    Text("Promote · ${shop.name}", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))
                    LsTextField(days, { days = it }, "Days (7 / 30 / 90)", keyboardType = KeyboardType.Number)
                    Spacer(Modifier.height(16.dp))
                    Row {
                        TextButton(onClick = { promoting = null }) { Text("Cancel") }
                        Spacer(Modifier.width(8.dp))
                        TextButton(onClick = { viewModel.promote(shop.id, days.toIntOrNull() ?: 7); promoting = null }) { Text("Promote") }
                    }
                }
            }
        }
    }
    confirmDelete?.let { shop ->
        ConfirmDialog("Delete shop", "Delete \"${shop.name}\"? Its products will also be removed.", destructive = true, confirmLabel = "Delete", onConfirm = { viewModel.delete(shop.id); confirmDelete = null }, onDismiss = { confirmDelete = null })
    }
}
