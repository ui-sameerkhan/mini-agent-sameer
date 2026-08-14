package com.lazyshopper.app.feature.shopkeeper.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lazyshopper.app.core.data.local.SessionManager
import com.lazyshopper.app.core.data.remote.ApiResult
import com.lazyshopper.app.core.data.remote.dto.ShopkeeperEarningsResponse
import com.lazyshopper.app.feature.shopkeeper.kyc.KycRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private val OPEN_ORDER_STATUSES = setOf("placed", "accepted", "preparing", "ready", "packed", "out_for_delivery")

data class ShopkeeperDashboardUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val kycStatus: String? = null,
    val kycRejectReason: String? = null,
    val earnings: ShopkeeperEarningsResponse? = null,
    val shopCount: Int = 0,
    val approvedShopCount: Int = 0,
    val productCount: Int = 0,
    val pendingOrdersCount: Int = 0,
)

@HiltViewModel
class ShopkeeperDashboardViewModel @Inject constructor(
    private val repository: ShopkeeperDashboardRepository,
    private val kycRepository: KycRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _state = MutableStateFlow(ShopkeeperDashboardUiState())
    val state: StateFlow<ShopkeeperDashboardUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }

            when (val kycResult = kycRepository.myKyc()) {
                is ApiResult.Success -> _state.update {
                    it.copy(kycStatus = kycResult.data.kycStatus, kycRejectReason = kycResult.data.kyc?.reason)
                }
                is ApiResult.Failure -> {} // non-fatal — dashboard still usable
            }

            val userId = sessionManager.sessionFlow.first().userId

            when (val earningsResult = repository.earnings()) {
                is ApiResult.Success -> _state.update { it.copy(earnings = earningsResult.data) }
                is ApiResult.Failure -> _state.update { it.copy(error = earningsResult.message) }
            }

            val shops = (repository.myShops() as? ApiResult.Success)?.data.orEmpty()
            val products = (repository.myProducts() as? ApiResult.Success)?.data.orEmpty()
            val orders = (repository.receivedOrders() as? ApiResult.Success)?.data.orEmpty()

            val pendingCount = orders.count { order ->
                order.status in OPEN_ORDER_STATUSES && order.items.any { it.owner_id == userId }
            }

            _state.update {
                it.copy(
                    loading = false,
                    shopCount = shops.size,
                    approvedShopCount = shops.count { s -> s.status == "approved" },
                    productCount = products.size,
                    pendingOrdersCount = pendingCount,
                )
            }
        }
    }
}
