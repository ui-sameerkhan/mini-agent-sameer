package com.lazyshopper.app.feature.shopkeeper.shops

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lazyshopper.app.core.data.remote.ApiResult
import com.lazyshopper.app.core.data.remote.dto.Shop
import com.lazyshopper.app.core.data.remote.dto.ShopInput
import com.lazyshopper.app.core.ui.ActionState
import com.lazyshopper.app.core.ui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** One-shot request handed to the Composable so it can launch [ShopPromoteCheckoutActivity]. */
data class PromoteLaunchRequest(
    val shopId: String,
    val shopName: String,
    val razorpayOrderId: String,
    val amountPaise: Long,
    val keyId: String,
)

data class ShopsUiState(
    val shops: UiState<List<Shop>> = UiState.Loading,
    val deleteState: ActionState = ActionState.Idle,
    val timingsState: ActionState = ActionState.Idle,
    val promoteCheckoutState: ActionState = ActionState.Idle,
    val promoteVerifyState: ActionState = ActionState.Idle,
    val promoteLaunch: PromoteLaunchRequest? = null,
)

@HiltViewModel
class ShopsViewModel @Inject constructor(
    private val repository: ShopsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ShopsUiState())
    val state: StateFlow<ShopsUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(shops = UiState.Loading) }
            when (val result = repository.myShops()) {
                is ApiResult.Success -> _state.update { it.copy(shops = UiState.Success(result.data)) }
                is ApiResult.Failure -> _state.update { it.copy(shops = UiState.Error(result.message)) }
            }
        }
    }

    fun deleteShop(id: String) {
        viewModelScope.launch {
            _state.update { it.copy(deleteState = ActionState.InFlight) }
            when (val result = repository.deleteShop(id)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(deleteState = ActionState.Done) }
                    load()
                }
                is ApiResult.Failure -> _state.update { it.copy(deleteState = ActionState.Failed(result.message)) }
            }
        }
    }

    fun setTimings(id: String, openTime: String, closeTime: String) {
        viewModelScope.launch {
            _state.update { it.copy(timingsState = ActionState.InFlight) }
            when (val result = repository.setTimings(id, openTime, closeTime)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(timingsState = ActionState.Done) }
                    load()
                }
                is ApiResult.Failure -> _state.update { it.copy(timingsState = ActionState.Failed(result.message)) }
            }
        }
    }

    fun startPromote(shop: Shop, days: Int) {
        viewModelScope.launch {
            _state.update { it.copy(promoteCheckoutState = ActionState.InFlight) }
            when (val result = repository.promoteCheckout(shop.id, days)) {
                is ApiResult.Success -> _state.update {
                    it.copy(
                        promoteCheckoutState = ActionState.Done,
                        promoteLaunch = PromoteLaunchRequest(
                            shopId = shop.id,
                            shopName = shop.name,
                            razorpayOrderId = result.data.razorpay.order_id,
                            amountPaise = result.data.razorpay.amount,
                            keyId = result.data.razorpay.key_id,
                        ),
                    )
                }
                is ApiResult.Failure -> _state.update { it.copy(promoteCheckoutState = ActionState.Failed(result.message)) }
            }
        }
    }

    fun consumePromoteLaunch() = _state.update { it.copy(promoteLaunch = null) }

    fun onPromoteResult(paymentId: String?, razorpayOrderId: String?, signature: String?, error: String?) {
        if (error != null || paymentId == null || razorpayOrderId == null || signature == null) {
            _state.update { it.copy(promoteVerifyState = ActionState.Failed(error ?: "Payment was not completed")) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(promoteVerifyState = ActionState.InFlight) }
            when (val result = repository.promoteVerify(razorpayOrderId, paymentId, signature)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(promoteVerifyState = ActionState.Done) }
                    load()
                }
                is ApiResult.Failure -> _state.update { it.copy(promoteVerifyState = ActionState.Failed(result.message)) }
            }
        }
    }

    fun resetActionStates() = _state.update {
        it.copy(deleteState = ActionState.Idle, timingsState = ActionState.Idle, promoteVerifyState = ActionState.Idle)
    }
}

@HiltViewModel
class ShopFormViewModel @Inject constructor(
    private val repository: ShopsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ShopFormUiState())
    val state: StateFlow<ShopFormUiState> = _state.asStateFlow()

    fun setName(v: String) = _state.update { it.copy(name = v) }
    fun setState(v: String) = _state.update { it.copy(state = v) }
    fun setDistrict(v: String) = _state.update { it.copy(district = v) }
    fun setArea(v: String) = _state.update { it.copy(area = v) }
    fun setCategory(v: String) = _state.update { it.copy(category = v) }

    fun submit() {
        val s = _state.value
        if (s.name.isBlank() || s.state.isBlank() || s.district.isBlank() || s.area.isBlank()) {
            _state.update { it.copy(submitState = ActionState.Failed("All fields are required")) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(submitState = ActionState.InFlight) }
            val input = ShopInput(
                name = s.name.trim(),
                state = s.state.trim(),
                district = s.district.trim(),
                area = s.area.trim(),
                category = s.category,
            )
            when (val result = repository.createShop(input)) {
                is ApiResult.Success -> _state.update { it.copy(submitState = ActionState.Done) }
                is ApiResult.Failure -> _state.update { it.copy(submitState = ActionState.Failed(result.message)) }
            }
        }
    }
}

data class ShopFormUiState(
    val name: String = "",
    val state: String = "",
    val district: String = "",
    val area: String = "",
    val category: String = SHOP_CATEGORIES.first(),
    val submitState: ActionState = ActionState.Idle,
)
