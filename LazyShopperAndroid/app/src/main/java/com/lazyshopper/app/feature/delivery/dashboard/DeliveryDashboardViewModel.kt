package com.lazyshopper.app.feature.delivery.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lazyshopper.app.core.data.remote.ApiResult
import com.lazyshopper.app.core.data.remote.dto.Order
import com.lazyshopper.app.feature.delivery.kyc.DeliveryKycRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

private val TERMINAL_STATUSES = setOf("delivered", "cancelled")
private const val AVAILABLE_ORDERS_POLL_MS = 12_000L
private const val ACTIVE_ORDER_POLL_MS = 15_000L

data class DeliveryDashboardUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val online: Boolean = false,
    val toggleBusy: Boolean = false,
    val accountStatus: String = "pending",
    val kycStatus: String? = null,
    val kycRejectReason: String? = null,
    val availableOrders: List<Order> = emptyList(),
    val activeOrder: Order? = null,
    val riderLat: Double? = null,
    val riderLng: Double? = null,
    val actionBusy: Boolean = false,
    val actionError: String? = null,
    val deliverDialogOrderId: String? = null,
)

@HiltViewModel
class DeliveryDashboardViewModel @Inject constructor(
    private val repository: DeliveryRepository,
    private val kycRepository: DeliveryKycRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(DeliveryDashboardUiState())
    val state: StateFlow<DeliveryDashboardUiState> = _state.asStateFlow()

    private var pollingJob: Job? = null

    init {
        loadInitial()
    }

    fun loadInitial() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }

            when (val meResult = repository.me()) {
                is ApiResult.Success -> _state.update {
                    it.copy(online = meResult.data.user.available, accountStatus = meResult.data.user.status)
                }
                is ApiResult.Failure -> _state.update { it.copy(error = meResult.message) }
            }

            when (val kycResult = kycRepository.myKyc()) {
                is ApiResult.Success -> _state.update {
                    it.copy(kycStatus = kycResult.data.kycStatus, kycRejectReason = kycResult.data.kyc?.reason)
                }
                is ApiResult.Failure -> {} // non-fatal — dashboard still usable, banner just stays hidden
            }

            refreshActiveOrder()

            val s = _state.value
            if (s.online && s.activeOrder == null && s.accountStatus == "active") {
                refreshAvailableOrders()
            }

            _state.update { it.copy(loading = false) }
            restartPolling()
        }
    }

    private fun restartPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            var ticks = 0L
            while (isActive) {
                delay(3_000)
                ticks += 3_000
                val s = _state.value
                if (s.online && s.activeOrder == null && s.accountStatus == "active" && ticks % AVAILABLE_ORDERS_POLL_MS == 0L) {
                    refreshAvailableOrders()
                }
                if (s.activeOrder != null && ticks % ACTIVE_ORDER_POLL_MS == 0L) {
                    refreshActiveOrder()
                }
            }
        }
    }

    private suspend fun refreshAvailableOrders() {
        when (val result = repository.availableOrders()) {
            is ApiResult.Success -> _state.update { it.copy(availableOrders = result.data) }
            is ApiResult.Failure -> {} // silent on background poll failure
        }
    }

    private suspend fun refreshActiveOrder() {
        when (val result = repository.myOrders()) {
            is ApiResult.Success -> {
                val active = result.data.firstOrNull { it.status !in TERMINAL_STATUSES }
                _state.update { it.copy(activeOrder = active, availableOrders = if (active != null) emptyList() else it.availableOrders) }
            }
            is ApiResult.Failure -> {}
        }
    }

    fun setOnline(value: Boolean) {
        viewModelScope.launch {
            _state.update { it.copy(toggleBusy = true, error = null) }
            when (val result = repository.setAvailability(value)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(online = value, toggleBusy = false) }
                    if (value) refreshAvailableOrders() else _state.update { it.copy(availableOrders = emptyList()) }
                }
                is ApiResult.Failure -> _state.update { it.copy(toggleBusy = false, error = result.message) }
            }
        }
    }

    fun updateRiderLocation(lat: Double, lng: Double) = _state.update { it.copy(riderLat = lat, riderLng = lng) }

    fun acceptOrder(orderId: String) {
        viewModelScope.launch {
            _state.update { it.copy(actionBusy = true, actionError = null) }
            when (val result = repository.acceptOrder(orderId)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(actionBusy = false) }
                    refreshActiveOrder()
                }
                is ApiResult.Failure -> _state.update { it.copy(actionBusy = false, actionError = result.message) }
            }
        }
    }

    fun rejectActiveOrder() {
        val orderId = _state.value.activeOrder?.id ?: return
        viewModelScope.launch {
            _state.update { it.copy(actionBusy = true, actionError = null) }
            when (val result = repository.rejectOrder(orderId)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(actionBusy = false, activeOrder = null) }
                    if (_state.value.online) refreshAvailableOrders()
                }
                is ApiResult.Failure -> _state.update { it.copy(actionBusy = false, actionError = result.message) }
            }
        }
    }

    fun pickupActiveOrder() {
        val orderId = _state.value.activeOrder?.id ?: return
        viewModelScope.launch {
            _state.update { it.copy(actionBusy = true, actionError = null) }
            when (val result = repository.pickupOrder(orderId)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(actionBusy = false) }
                    refreshActiveOrder()
                }
                is ApiResult.Failure -> _state.update { it.copy(actionBusy = false, actionError = result.message) }
            }
        }
    }

    fun openDeliverDialog(orderId: String) = _state.update { it.copy(deliverDialogOrderId = orderId, actionError = null) }
    fun closeDeliverDialog() = _state.update { it.copy(deliverDialogOrderId = null, actionError = null) }

    fun deliverOrder(otp: String) {
        val orderId = _state.value.deliverDialogOrderId ?: _state.value.activeOrder?.id ?: return
        viewModelScope.launch {
            _state.update { it.copy(actionBusy = true, actionError = null) }
            when (val result = repository.deliverOrder(orderId, otp)) {
                is ApiResult.Success -> _state.update {
                    it.copy(actionBusy = false, activeOrder = null, deliverDialogOrderId = null)
                }
                is ApiResult.Failure -> _state.update { it.copy(actionBusy = false, actionError = result.message) }
            }
        }
    }
}
