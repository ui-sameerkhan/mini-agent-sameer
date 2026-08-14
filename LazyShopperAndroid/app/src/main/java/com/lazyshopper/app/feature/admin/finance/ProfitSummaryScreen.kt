package com.lazyshopper.app.feature.admin.finance

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lazyshopper.app.core.data.remote.ApiResult
import com.lazyshopper.app.core.data.remote.dto.ProfitSummaryResponse
import com.lazyshopper.app.core.ui.UiState
import com.lazyshopper.app.core.ui.components.LsTextField
import com.lazyshopper.app.feature.admin.common.AdminScreenScaffold
import com.lazyshopper.app.feature.admin.common.EmptyOrList
import com.lazyshopper.app.feature.admin.common.KeyValueRow
import com.lazyshopper.app.feature.admin.common.SectionDivider
import com.lazyshopper.app.feature.admin.common.StatTileRow
import com.lazyshopper.app.feature.admin.common.money
import com.lazyshopper.app.feature.admin.data.AdminFinanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

private fun currentMonth(): String = YearMonth.now().toString() // "YYYY-MM"

@HiltViewModel
class ProfitSummaryViewModel @Inject constructor(private val repo: AdminFinanceRepository) : ViewModel() {
    private val _state = MutableStateFlow<UiState<ProfitSummaryResponse>>(UiState.Loading)
    val state: StateFlow<UiState<ProfitSummaryResponse>> = _state.asStateFlow()
    private val _month = MutableStateFlow(currentMonth())
    val month: StateFlow<String> = _month.asStateFlow()

    fun setMonth(m: String) {
        _month.value = m
    }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            when (val r = repo.profitSummary(_month.value)) {
                is ApiResult.Success -> _state.value = UiState.Success(r.data)
                is ApiResult.Failure -> _state.value = UiState.Error(r.message)
            }
        }
    }
}

@Composable
fun AdminProfitSummaryScreen(onMenuClick: () -> Unit, viewModel: ProfitSummaryViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val month by viewModel.month.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }
    var monthInput by remember(month) { mutableStateOf(month) }

    AdminScreenScaffold(title = "Profit Summary", onMenuClick = onMenuClick) { padding ->
        Column(Modifier.padding(padding).padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LsTextField(
                    value = monthInput,
                    onValueChange = { monthInput = it },
                    label = "Month (YYYY-MM)",
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = { viewModel.setMonth(monthInput); viewModel.load() }) { Text("Go") }
            }
            Spacer(Modifier.height(8.dp))
            val s = state
            EmptyOrList(loading = s is UiState.Loading, error = (s as? UiState.Error)?.message, isEmpty = false, emptyMessage = "", onRetry = viewModel::load) {
                val d = (s as UiState.Success).data
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    StatTileRow(
                        listOf(
                            "Revenue" to d.revenue.money(),
                            "Costs" to d.costs.money(),
                            "Net profit" to d.net_profit.money(),
                            "Orders" to d.orders.toString(),
                        ),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("Breakdown for ${d.month}", style = MaterialTheme.typography.titleSmall)
                    SectionDivider()
                    KeyValueRow("GMV", d.gmv.money())
                    KeyValueRow("Commission earned", d.commission.money())
                    KeyValueRow("Shopkeeper payouts", d.shopkeeper_payouts.money())
                    KeyValueRow("Rider payouts", d.rider_payouts.money())
                    KeyValueRow("Delivery fee collected", d.delivery_collected.money())
                    KeyValueRow("Free delivery cost", d.free_delivery_cost.money())
                    KeyValueRow("Referral discount cost", d.referral_discount_cost.money())
                    KeyValueRow("Referral reward cost", d.referral_reward_cost.money())
                    KeyValueRow("Total referral cost", d.referral_cost.money())
                    SectionDivider()
                    KeyValueRow("Total revenue", d.revenue.money())
                    KeyValueRow("Total costs", d.costs.money())
                    KeyValueRow("Net profit", d.net_profit.money())
                }
            }
        }
    }
}
