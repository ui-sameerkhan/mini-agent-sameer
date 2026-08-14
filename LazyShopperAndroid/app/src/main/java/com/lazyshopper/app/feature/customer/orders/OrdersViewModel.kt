package com.lazyshopper.app.feature.customer.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lazyshopper.app.core.data.remote.ApiResult
import com.lazyshopper.app.core.data.remote.dto.Order
import com.lazyshopper.app.core.ui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Terminal order states — everything else is considered "active" (trackable, has a live rider). */
val TERMINAL_ORDER_STATUSES = setOf("delivered", "cancelled")

fun Order.isActive(): Boolean = status !in TERMINAL_ORDER_STATUSES

data class OrdersUiState(
    val orders: UiState<List<Order>> = UiState.Loading,
    val expandedOrderId: String? = null,
)

@HiltViewModel
class OrdersViewModel @Inject constructor(
    private val repository: OrdersRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(OrdersUiState())
    val state: StateFlow<OrdersUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(orders = UiState.Loading)
            when (val r = repository.myOrders()) {
                is ApiResult.Success -> _state.value = _state.value.copy(
                    orders = UiState.Success(r.data.sortedByDescending { it.created_at.orEmpty() }),
                )
                is ApiResult.Failure -> _state.value = _state.value.copy(orders = UiState.Error(r.message))
            }
        }
    }

    fun toggleExpanded(orderId: String) {
        _state.value = _state.value.copy(
            expandedOrderId = if (_state.value.expandedOrderId == orderId) null else orderId,
        )
    }
}
