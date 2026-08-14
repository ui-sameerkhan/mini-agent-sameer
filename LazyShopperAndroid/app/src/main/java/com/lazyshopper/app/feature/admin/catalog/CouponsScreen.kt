package com.lazyshopper.app.feature.admin.catalog

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lazyshopper.app.core.data.remote.ApiResult
import com.lazyshopper.app.core.data.remote.dto.Coupon
import com.lazyshopper.app.core.data.remote.dto.CouponInput
import com.lazyshopper.app.core.ui.ActionState
import com.lazyshopper.app.core.ui.UiState
import com.lazyshopper.app.core.ui.components.LsTextField
import com.lazyshopper.app.feature.admin.common.AdminLazyList
import com.lazyshopper.app.feature.admin.common.AdminScreenScaffold
import com.lazyshopper.app.feature.admin.common.CompactRow
import com.lazyshopper.app.feature.admin.common.ConfirmDialog
import com.lazyshopper.app.feature.admin.common.EmptyOrList
import com.lazyshopper.app.feature.admin.common.FormDialog
import com.lazyshopper.app.feature.admin.common.LabeledSwitchRow
import com.lazyshopper.app.feature.admin.common.StatusChip
import com.lazyshopper.app.feature.admin.common.money
import com.lazyshopper.app.feature.admin.data.AdminCatalogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CouponsViewModel @Inject constructor(private val repo: AdminCatalogRepository) : ViewModel() {
    private val _state = MutableStateFlow<UiState<List<Coupon>>>(UiState.Loading)
    val state: StateFlow<UiState<List<Coupon>>> = _state.asStateFlow()
    private val _actionState = MutableStateFlow<ActionState>(ActionState.Idle)
    val actionState: StateFlow<ActionState> = _actionState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            when (val r = repo.coupons()) {
                is ApiResult.Success -> _state.value = UiState.Success(r.data)
                is ApiResult.Failure -> _state.value = UiState.Error(r.message)
            }
        }
    }

    fun save(existingId: String?, input: CouponInput) {
        viewModelScope.launch {
            _actionState.value = ActionState.InFlight
            val r = if (existingId != null) repo.updateCoupon(existingId, input) else repo.createCoupon(input)
            when (r) {
                is ApiResult.Success -> { _actionState.value = ActionState.Done; load() }
                is ApiResult.Failure -> _actionState.value = ActionState.Failed(r.message)
            }
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            when (val r = repo.deleteCoupon(id)) {
                is ApiResult.Success -> load()
                is ApiResult.Failure -> _actionState.value = ActionState.Failed(r.message)
            }
        }
    }
}

@Composable
fun AdminCouponsScreen(onMenuClick: () -> Unit, viewModel: CouponsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val actionState by viewModel.actionState.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }

    var editing by remember { mutableStateOf<Coupon?>(null) }
    var creating by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf<Coupon?>(null) }

    AdminScreenScaffold(
        title = "Coupons",
        onMenuClick = onMenuClick,
        fab = { FloatingActionButton(onClick = { creating = true }) { Icon(Icons.Default.Add, contentDescription = "Add coupon") } },
    ) { padding ->
        val s = state
        val items = (s as? UiState.Success)?.data.orEmpty()
        EmptyOrList(loading = s is UiState.Loading, error = (s as? UiState.Error)?.message, isEmpty = items.isEmpty() && s is UiState.Success, emptyMessage = "No coupons yet", onRetry = viewModel::load) {
            AdminLazyList(modifier = Modifier.padding(padding)) {
                items(items, key = { it.id }) { c ->
                    CompactRow(
                        title = c.code,
                        subtitle = if (c.type == "percent") "${c.value.toInt()}% off · min order ${c.min_order.money()}" else "${c.value.money()} off · min order ${c.min_order.money()}",
                        meta = if (c.auto_generated == true) "auto-generated" else null,
                        badge = { StatusChip(if (c.active) "active" else "inactive") },
                        trailing = {
                            Row {
                                IconButton(onClick = { editing = c }) { Icon(Icons.Default.Edit, contentDescription = "Edit") }
                                IconButton(onClick = { confirmDelete = c }) { Icon(Icons.Default.Delete, contentDescription = "Delete") }
                            }
                        },
                    )
                }
            }
        }
    }

    if (creating) {
        CouponFormDialog(null, actionState is ActionState.InFlight, onDismiss = { creating = false }, onSave = { input -> viewModel.save(null, input); creating = false })
    }
    editing?.let { c ->
        CouponFormDialog(c, actionState is ActionState.InFlight, onDismiss = { editing = null }, onSave = { input -> viewModel.save(c.id, input); editing = null })
    }
    confirmDelete?.let { c ->
        ConfirmDialog("Delete coupon", "Delete \"${c.code}\"?", destructive = true, confirmLabel = "Delete", onConfirm = { viewModel.delete(c.id); confirmDelete = null }, onDismiss = { confirmDelete = null })
    }
}

@Composable
private fun CouponFormDialog(existing: Coupon?, saving: Boolean, onDismiss: () -> Unit, onSave: (CouponInput) -> Unit) {
    var code by remember { mutableStateOf(existing?.code.orEmpty()) }
    var isPercent by remember { mutableStateOf((existing?.type ?: "percent") == "percent") }
    var value by remember { mutableStateOf(existing?.value?.toString().orEmpty()) }
    var minOrder by remember { mutableStateOf(existing?.min_order?.toString() ?: "0") }
    var active by remember { mutableStateOf(existing?.active ?: true) }

    FormDialog(
        title = if (existing == null) "New coupon" else "Edit coupon",
        onDismiss = onDismiss,
        saving = saving,
        onSave = {
            onSave(CouponInput(code.trim().uppercase(), if (isPercent) "percent" else "flat", value.toDoubleOrNull() ?: 0.0, minOrder.toDoubleOrNull() ?: 0.0, active))
        },
    ) {
        LsTextField(code, { code = it }, "Coupon code")
        Spacer(Modifier.height(8.dp))
        LabeledSwitchRow(if (isPercent) "Type: Percent off" else "Type: Flat amount off", isPercent, { isPercent = it })
        Spacer(Modifier.height(8.dp))
        LsTextField(value, { value = it }, if (isPercent) "Percent value" else "Flat value (₹)", keyboardType = KeyboardType.Decimal)
        Spacer(Modifier.height(8.dp))
        LsTextField(minOrder, { minOrder = it }, "Minimum order (₹)", keyboardType = KeyboardType.Decimal)
        Spacer(Modifier.height(8.dp))
        LabeledSwitchRow("Active", active, { active = it })
    }
}
