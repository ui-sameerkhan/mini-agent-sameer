package com.lazyshopper.app.feature.admin.finance

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lazyshopper.app.core.data.remote.ApiResult
import com.lazyshopper.app.core.data.remote.dto.PromotionsResponse
import com.lazyshopper.app.core.ui.UiState
import com.lazyshopper.app.feature.admin.common.AdminLazyList
import com.lazyshopper.app.feature.admin.common.AdminScreenScaffold
import com.lazyshopper.app.feature.admin.common.CompactRow
import com.lazyshopper.app.feature.admin.common.EmptyOrList
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
class PromotionsViewModel @Inject constructor(private val repo: AdminFinanceRepository) : ViewModel() {
    private val _state = MutableStateFlow<UiState<PromotionsResponse>>(UiState.Loading)
    val state: StateFlow<UiState<PromotionsResponse>> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            when (val r = repo.promotions()) {
                is ApiResult.Success -> _state.value = UiState.Success(r.data)
                is ApiResult.Failure -> _state.value = UiState.Error(r.message)
            }
        }
    }
}

@Composable
fun AdminPromotionsScreen(onMenuClick: () -> Unit, viewModel: PromotionsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }

    AdminScreenScaffold(title = "Shop Promotions", onMenuClick = onMenuClick) { padding ->
        val s = state
        EmptyOrList(
            loading = s is UiState.Loading,
            error = (s as? UiState.Error)?.message,
            isEmpty = (s as? UiState.Success)?.data?.promotions.isNullOrEmpty() && s is UiState.Success,
            emptyMessage = "No shop promotions purchased yet",
            onRetry = viewModel::load,
        ) {
            val d = (s as UiState.Success).data
            Column(Modifier.padding(padding)) {
                StatTileRow(
                    listOf("Total promotions" to d.count.toString(), "Revenue" to d.revenue.money()),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )
                AdminLazyList {
                    items(d.promotions, key = { it.razorpay_order_id ?: it.created_at ?: it.hashCode().toString() }) { p ->
                        CompactRow(
                            title = p.shop_name ?: p.shop_id ?: "-",
                            subtitle = "${p.owner_name ?: "-"} · ${p.days} days",
                            meta = "amount ${p.amount.money()} · ${p.created_at ?: "-"}",
                            badge = { StatusChip(p.status ?: "-") },
                        )
                    }
                }
            }
        }
    }
}
