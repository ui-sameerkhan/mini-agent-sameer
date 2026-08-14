package com.lazyshopper.app.feature.shopkeeper.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lazyshopper.app.core.data.local.SessionManager
import com.lazyshopper.app.core.data.remote.ApiResult
import com.lazyshopper.app.core.data.remote.dto.Order
import com.lazyshopper.app.core.ui.ActionState
import com.lazyshopper.app.core.ui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ShopkeeperOrdersUiState(
    val orders: UiState<List<Order>> = UiState.Loading,
    val myUserId: String? = null,
    val selectedOrderId: String? = null,
    val statusUpdateState: ActionState = ActionState.Idle,
)

@HiltViewModel
class ShopkeeperOrdersViewModel @Inject constructor(
    private val repository: ShopkeeperOrdersRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _state = MutableStateFlow(ShopkeeperOrdersUiState())
    val state: StateFlow<ShopkeeperOrdersUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(orders = UiState.Loading) }
            val userId = sessionManager.sessionFlow.first().userId
            when (val result = repository.receivedOrders()) {
                is ApiResult.Success -> _state.update {
                    it.copy(
                        myUserId = userId,
                        orders = UiState.Success(result.data.sortedByDescending { o -> o.created_at }),
                    )
                }
                is ApiResult.Failure -> _state.update { it.copy(myUserId = userId, orders = UiState.Error(result.message)) }
            }
        }
    }

    fun selectOrder(orderId: String?) = _state.update { it.copy(selectedOrderId = orderId, statusUpdateState = ActionState.Idle) }

    fun updateStatus(orderId: String, status: String) {
        viewModelScope.launch {
            _state.update { it.copy(statusUpdateState = ActionState.InFlight) }
            when (val result = repository.updateStatus(orderId, status)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(statusUpdateState = ActionState.Done) }
                    load()
                }
                is ApiResult.Failure -> _state.update { it.copy(statusUpdateState = ActionState.Failed(result.message)) }
            }
        }
    }
}
