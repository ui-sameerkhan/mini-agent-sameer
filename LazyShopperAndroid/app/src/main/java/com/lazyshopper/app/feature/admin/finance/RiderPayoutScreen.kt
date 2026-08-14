package com.lazyshopper.app.feature.admin.finance

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import com.lazyshopper.app.core.data.remote.dto.RiderPayoutSettings
import com.lazyshopper.app.core.data.remote.dto.WeightSlab
import com.lazyshopper.app.core.ui.ActionState
import com.lazyshopper.app.core.ui.UiState
import com.lazyshopper.app.core.ui.components.LsPrimaryButton
import com.lazyshopper.app.core.ui.components.LsTextField
import com.lazyshopper.app.feature.admin.common.AdminScreenScaffold
import com.lazyshopper.app.feature.admin.common.EmptyOrList
import com.lazyshopper.app.feature.admin.common.LabeledSwitchRow
import com.lazyshopper.app.feature.admin.data.AdminFinanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RiderPayoutViewModel @Inject constructor(private val repo: AdminFinanceRepository) : ViewModel() {
    private val _state = MutableStateFlow<UiState<RiderPayoutSettings>>(UiState.Loading)
    val state: StateFlow<UiState<RiderPayoutSettings>> = _state.asStateFlow()
    private val _actionState = MutableStateFlow<ActionState>(ActionState.Idle)
    val actionState: StateFlow<ActionState> = _actionState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            when (val r = repo.riderPayoutSettings()) {
                is ApiResult.Success -> _state.value = UiState.Success(r.data)
                is ApiResult.Failure -> _state.value = UiState.Error(r.message)
            }
        }
    }

    fun save(baseFee: Double, baseKm: Double, perKm: Double, freeMin: Double, firstOrderFree: Boolean, slabs: List<WeightSlab>) {
        viewModelScope.launch {
            _actionState.value = ActionState.InFlight
            when (val r = repo.updateRiderPayoutSettings(baseFee, baseKm, perKm, freeMin, firstOrderFree, slabs)) {
                is ApiResult.Success -> { _actionState.value = ActionState.Done; load() }
                is ApiResult.Failure -> _actionState.value = ActionState.Failed(r.message)
            }
        }
    }
}

@Composable
fun AdminRiderPayoutScreen(onMenuClick: () -> Unit, viewModel: RiderPayoutViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val actionState by viewModel.actionState.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }

    AdminScreenScaffold(title = "Rider Payout Settings", onMenuClick = onMenuClick) { padding ->
        val s = state
        EmptyOrList(loading = s is UiState.Loading, error = (s as? UiState.Error)?.message, isEmpty = false, emptyMessage = "", onRetry = viewModel::load) {
            RiderPayoutForm(Modifier.padding(padding), (s as UiState.Success).data, actionState is ActionState.InFlight, viewModel::save)
        }
    }
}

@Composable
private fun RiderPayoutForm(modifier: Modifier, settings: RiderPayoutSettings, saving: Boolean, onSave: (Double, Double, Double, Double, Boolean, List<WeightSlab>) -> Unit) {
    var baseFee by remember(settings) { mutableStateOf(settings.base_fee.toString()) }
    var baseKm by remember(settings) { mutableStateOf(settings.base_km.toString()) }
    var perKm by remember(settings) { mutableStateOf(settings.per_km.toString()) }
    var freeMin by remember(settings) { mutableStateOf(settings.free_delivery_min.toString()) }
    var firstOrderFree by remember(settings) { mutableStateOf(settings.first_order_free) }
    val slabs = remember(settings) {
        mutableStateListOf(*settings.weight_slabs.map { it.upto to it.fee }.toTypedArray())
    }

    Column(modifier.padding(12.dp).verticalScroll(rememberScrollState())) {
        LsTextField(baseFee, { baseFee = it }, "Base fee (₹)", keyboardType = KeyboardType.Decimal)
        Spacer(Modifier.height(8.dp))
        LsTextField(baseKm, { baseKm = it }, "Base distance (km)", keyboardType = KeyboardType.Decimal)
        Spacer(Modifier.height(8.dp))
        LsTextField(perKm, { perKm = it }, "Fee per extra km (₹)", keyboardType = KeyboardType.Decimal)
        Spacer(Modifier.height(8.dp))
        LsTextField(freeMin, { freeMin = it }, "Free delivery above order value (₹, 0 = disabled)", keyboardType = KeyboardType.Decimal)
        Spacer(Modifier.height(8.dp))
        LabeledSwitchRow("First order free delivery", firstOrderFree, { firstOrderFree = it })
        Spacer(Modifier.height(12.dp))
        Text("Weight incentive slabs", style = MaterialTheme.typography.titleSmall)
        Text("Upto weight (kg, blank = and above) → incentive fee (₹)", style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(6.dp))
        slabs.forEachIndexed { i, (upto, fee) ->
            var uptoText by remember(settings, i) { mutableStateOf(upto?.toString().orEmpty()) }
            var feeText by remember(settings, i) { mutableStateOf(fee.toString()) }
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                LsTextField(uptoText, { uptoText = it; slabs[i] = uptoText.toDoubleOrNull() to (feeText.toDoubleOrNull() ?: 0.0) }, "Upto kg", modifier = Modifier.width(120.dp), keyboardType = KeyboardType.Decimal)
                Spacer(Modifier.width(8.dp))
                LsTextField(feeText, { feeText = it; slabs[i] = (uptoText.toDoubleOrNull()) to (feeText.toDoubleOrNull() ?: 0.0) }, "Fee ₹", modifier = Modifier.width(100.dp), keyboardType = KeyboardType.Decimal)
                IconButton(onClick = { slabs.removeAt(i) }) { Icon(Icons.Default.Delete, contentDescription = "Remove slab") }
            }
            Spacer(Modifier.height(6.dp))
        }
        TextButton(onClick = { slabs.add(null to 0.0) }) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(4.dp))
            Text("Add slab")
        }
        Spacer(Modifier.height(12.dp))
        LsPrimaryButton(
            text = "Save rider payout settings",
            loading = saving,
            onClick = {
                onSave(
                    baseFee.toDoubleOrNull() ?: 20.0,
                    baseKm.toDoubleOrNull() ?: 2.0,
                    perKm.toDoubleOrNull() ?: 5.0,
                    freeMin.toDoubleOrNull() ?: 0.0,
                    firstOrderFree,
                    slabs.map { WeightSlab(it.first, it.second) },
                )
            },
        )
    }
}
