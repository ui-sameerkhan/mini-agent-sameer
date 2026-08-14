package com.lazyshopper.app.feature.admin.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lazyshopper.app.core.data.remote.ApiResult
import com.lazyshopper.app.core.data.remote.dto.AdminPaymentsResponse
import com.lazyshopper.app.core.ui.UiState
import com.lazyshopper.app.feature.admin.common.AdminLazyList
import com.lazyshopper.app.feature.admin.common.AdminScreenScaffold
import com.lazyshopper.app.feature.admin.common.CompactRow
import com.lazyshopper.app.feature.admin.common.EmptyOrList
import com.lazyshopper.app.feature.admin.common.StatTileRow
import com.lazyshopper.app.feature.admin.common.StatusChip
import com.lazyshopper.app.feature.admin.common.money
import com.lazyshopper.app.feature.admin.data.AdminSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PaymentsViewModel @Inject constructor(private val repo: AdminSettingsRepository) : ViewModel() {
    private val _state = MutableStateFlow<UiState<AdminPaymentsResponse>>(UiState.Loading)
    val state: StateFlow<UiState<AdminPaymentsResponse>> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            when (val r = repo.payments()) {
                is ApiResult.Success -> _state.value = UiState.Success(r.data)
                is ApiResult.Failure -> _state.value = UiState.Error(r.message)
            }
        }
    }
}

@Composable
fun AdminPaymentsScreen(onMenuClick: () -> Unit, viewModel: PaymentsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }
    var tab by remember { mutableIntStateOf(0) }

    AdminScreenScaffold(title = "Payments Overview", onMenuClick = onMenuClick) { padding ->
        val s = state
        EmptyOrList(loading = s is UiState.Loading, error = (s as? UiState.Error)?.message, isEmpty = false, emptyMessage = "", onRetry = viewModel::load) {
            val d = (s as UiState.Success).data
            Column(Modifier.padding(padding)) {
                StatTileRow(
                    listOf("Online paid" to d.online_paid.money(), "COD total" to d.cod_total.money()),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )
                TabRow(selectedTabIndex = tab) {
                    Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Recent orders") })
                    Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Transactions") })
                }
                when (tab) {
                    0 -> AdminLazyList {
                        items(d.orders, key = { it.id }) { o ->
                            CompactRow(
                                title = o.customer_name ?: "-",
                                subtitle = "order #${o.id.takeLast(6)} · ${o.payment_method ?: "-"}",
                                meta = "${o.total.money()} · ${o.created_at ?: "-"}",
                                badge = { StatusChip(o.payment_status ?: "-") },
                            )
                        }
                    }
                    1 -> AdminLazyList {
                        items(d.transactions, key = { it.razorpay_order_id ?: it.order_id ?: it.hashCode().toString() }) { t ->
                            CompactRow(
                                title = t.order_id?.let { "order #${it.takeLast(6)}" } ?: (t.razorpay_order_id ?: "-"),
                                subtitle = "${t.currency ?: "INR"} ${t.amount.money()} · ${t.razorpay_payment_id ?: "-"}",
                                meta = t.created_at ?: "",
                                badge = { StatusChip(t.payment_status ?: t.status ?: "-") },
                            )
                        }
                    }
                }
            }
        }
    }
}
