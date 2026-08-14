package com.lazyshopper.app.feature.admin.finance

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
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
import com.lazyshopper.app.core.data.remote.dto.CommissionBreakdownResponse
import com.lazyshopper.app.core.data.remote.dto.CommissionSettings
import com.lazyshopper.app.core.ui.ActionState
import com.lazyshopper.app.core.ui.UiState
import com.lazyshopper.app.core.ui.components.LsPrimaryButton
import com.lazyshopper.app.core.ui.components.LsTextField
import com.lazyshopper.app.feature.admin.common.AdminScreenScaffold
import com.lazyshopper.app.feature.admin.common.EmptyOrList
import com.lazyshopper.app.feature.admin.common.KeyValueRow
import com.lazyshopper.app.feature.admin.common.SectionDivider
import com.lazyshopper.app.feature.admin.common.money
import com.lazyshopper.app.feature.admin.data.AdminFinanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private val KNOWN_CATEGORIES = listOf("vegetables", "masala", "kirana", "nonveg", "foodlive", "sweets")

@HiltViewModel
class CommissionViewModel @Inject constructor(private val repo: AdminFinanceRepository) : ViewModel() {
    private val _settings = MutableStateFlow<UiState<CommissionSettings>>(UiState.Loading)
    val settings: StateFlow<UiState<CommissionSettings>> = _settings.asStateFlow()
    private val _breakdown = MutableStateFlow<UiState<CommissionBreakdownResponse>>(UiState.Loading)
    val breakdown: StateFlow<UiState<CommissionBreakdownResponse>> = _breakdown.asStateFlow()
    private val _actionState = MutableStateFlow<ActionState>(ActionState.Idle)
    val actionState: StateFlow<ActionState> = _actionState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _settings.value = UiState.Loading
            when (val r = repo.commissionSettings()) {
                is ApiResult.Success -> _settings.value = UiState.Success(r.data)
                is ApiResult.Failure -> _settings.value = UiState.Error(r.message)
            }
        }
        viewModelScope.launch {
            _breakdown.value = UiState.Loading
            when (val r = repo.commissionBreakdown()) {
                is ApiResult.Success -> _breakdown.value = UiState.Success(r.data)
                is ApiResult.Failure -> _breakdown.value = UiState.Error(r.message)
            }
        }
    }

    fun save(globalDefault: Double, minPayout: Double, categories: Map<String, Double>, settlementDays: Int) {
        viewModelScope.launch {
            _actionState.value = ActionState.InFlight
            when (val r = repo.updateCommissionSettings(globalDefault, minPayout, categories, null, settlementDays)) {
                is ApiResult.Success -> { _actionState.value = ActionState.Done; load() }
                is ApiResult.Failure -> _actionState.value = ActionState.Failed(r.message)
            }
        }
    }
}

@Composable
fun AdminCommissionScreen(onMenuClick: () -> Unit, viewModel: CommissionViewModel = hiltViewModel()) {
    val settingsState by viewModel.settings.collectAsState()
    val breakdownState by viewModel.breakdown.collectAsState()
    val actionState by viewModel.actionState.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }
    var tab by remember { mutableIntStateOf(0) }

    AdminScreenScaffold(title = "Commission", onMenuClick = onMenuClick) { padding ->
        Column(Modifier.padding(padding)) {
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Settings") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Breakdown") })
            }
            when (tab) {
                0 -> {
                    val s = settingsState
                    EmptyOrList(loading = s is UiState.Loading, error = (s as? UiState.Error)?.message, isEmpty = false, emptyMessage = "", onRetry = viewModel::load) {
                        val settings = (s as UiState.Success).data
                        CommissionSettingsForm(settings, actionState is ActionState.InFlight, onSave = viewModel::save)
                    }
                }
                1 -> {
                    val s = breakdownState
                    EmptyOrList(loading = s is UiState.Loading, error = (s as? UiState.Error)?.message, isEmpty = false, emptyMessage = "", onRetry = viewModel::load) {
                        val b = (s as UiState.Success).data
                        Column(Modifier.padding(12.dp).verticalScroll(rememberScrollState())) {
                            KeyValueRow("Total order value", b.total_order_value.money())
                            KeyValueRow("Total commission", b.total_commission.money())
                            KeyValueRow("Shopkeeper payable", b.total_shopkeeper_payable.money())
                            KeyValueRow("Pending payout", b.pending_payout.money())
                            KeyValueRow("Completed payout", b.completed_payout.money())
                            KeyValueRow("Cancelled / refunded", b.cancelled_refunded.money())
                            SectionDivider()
                            Text("By category", style = MaterialTheme.typography.titleSmall)
                            b.categories.forEach { c -> KeyValueRow(c.category, "${c.sales.money()} · comm ${c.commission.money()}") }
                            SectionDivider()
                            Text("By shop", style = MaterialTheme.typography.titleSmall)
                            b.shops.forEach { sh -> KeyValueRow(sh.name ?: sh.shop_id, "${sh.sales.money()} · comm ${sh.commission.money()}") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommissionSettingsForm(settings: CommissionSettings, saving: Boolean, onSave: (Double, Double, Map<String, Double>, Int) -> Unit) {
    var globalDefault by remember(settings) { mutableStateOf(settings.global_default.toString()) }
    var minPayout by remember(settings) { mutableStateOf(settings.min_payout.toString()) }
    var settlementDays by remember(settings) { mutableStateOf(settings.settlement_cycle_days.toString()) }
    val catValues = remember(settings) {
        KNOWN_CATEGORIES.associateWith { cat -> mutableStateOf((settings.categories[cat] ?: settings.global_default).toString()) }.toMutableMap()
    }

    Column(Modifier.padding(12.dp).verticalScroll(rememberScrollState())) {
        LsTextField(globalDefault, { globalDefault = it }, "Global default commission (%)", keyboardType = KeyboardType.Decimal)
        Spacer(Modifier.height(8.dp))
        LsTextField(minPayout, { minPayout = it }, "Minimum payout (₹)", keyboardType = KeyboardType.Decimal)
        Spacer(Modifier.height(8.dp))
        LsTextField(settlementDays, { settlementDays = it }, "Settlement cycle (days)", keyboardType = KeyboardType.Number)
        Spacer(Modifier.height(12.dp))
        Text("Per-category commission (%)", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(4.dp))
        KNOWN_CATEGORIES.forEach { cat ->
            val state = catValues.getValue(cat)
            var v by state
            LsTextField(v, { v = it }, cat, keyboardType = KeyboardType.Decimal)
            Spacer(Modifier.height(6.dp))
        }
        Spacer(Modifier.height(12.dp))
        LsPrimaryButton(
            text = "Save commission settings",
            loading = saving,
            onClick = {
                val categories = catValues.mapValues { it.value.value.toDoubleOrNull() ?: 0.0 }
                onSave(globalDefault.toDoubleOrNull() ?: 10.0, minPayout.toDoubleOrNull() ?: 0.0, categories, settlementDays.toIntOrNull() ?: 7)
            },
        )
    }
}
