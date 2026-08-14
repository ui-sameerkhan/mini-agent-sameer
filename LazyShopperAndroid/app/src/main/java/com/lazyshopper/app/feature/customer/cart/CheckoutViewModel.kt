package com.lazyshopper.app.feature.customer.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lazyshopper.app.core.data.local.SessionManager
import com.lazyshopper.app.core.data.remote.ApiResult
import com.lazyshopper.app.core.data.remote.dto.Address
import com.lazyshopper.app.core.data.remote.dto.Order
import com.lazyshopper.app.core.data.remote.dto.QuoteResponse
import com.lazyshopper.app.core.ui.ActionState
import com.lazyshopper.app.core.ui.UiState
import com.lazyshopper.app.feature.customer.address.AddressRepository
import com.lazyshopper.app.feature.customer.orders.OrdersRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/** One-shot request handed to the Composable so it can launch [RazorpayCheckoutActivity]. */
data class RazorpayLaunchRequest(
    val localOrderId: String,
    val razorpayOrderId: String,
    val amountPaise: Long,
    val keyId: String,
)

data class CheckoutUiState(
    val addresses: UiState<List<Address>> = UiState.Loading,
    val selectedAddressId: String? = null,
    val useManualAddress: Boolean = false,
    val manualAddress: String = "",
    val phone: String = "",
    val deliverySlot: String = "Morning",
    val paymentMethod: String = "cod",
    val quote: UiState<QuoteResponse> = UiState.Idle,
    val placeOrderState: ActionState = ActionState.Idle,
    val razorpayLaunch: RazorpayLaunchRequest? = null,
    val completedOrder: Order? = null,
    val paymentStatus: String? = null, // "cod" | "paid" | "failed"
)

val DELIVERY_SLOTS = listOf("Morning", "Afternoon", "Evening", "Night")

@HiltViewModel
class CheckoutViewModel @Inject constructor(
    private val cartRepository: CartRepository,
    private val addressRepository: AddressRepository,
    private val ordersRepository: OrdersRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _state = MutableStateFlow(CheckoutUiState())
    val state: StateFlow<CheckoutUiState> = _state.asStateFlow()

    val cartLines = cartRepository.lines

    init {
        loadAddresses()
    }

    private fun loadAddresses() {
        viewModelScope.launch {
            when (val r = addressRepository.list()) {
                is ApiResult.Success -> {
                    val default = r.data.firstOrNull { it.is_default } ?: r.data.firstOrNull()
                    _state.value = _state.value.copy(
                        addresses = UiState.Success(r.data),
                        selectedAddressId = default?.id,
                        phone = default?.phone ?: _state.value.phone,
                        useManualAddress = r.data.isEmpty(),
                    )
                    refreshQuote()
                }
                is ApiResult.Failure -> _state.value = _state.value.copy(addresses = UiState.Error(r.message), useManualAddress = true)
            }
        }
    }

    fun selectAddress(id: String) {
        val addr = (_state.value.addresses as? UiState.Success)?.data?.firstOrNull { it.id == id }
        _state.value = _state.value.copy(selectedAddressId = id, phone = addr?.phone ?: _state.value.phone, useManualAddress = false)
        refreshQuote()
    }

    fun useManualAddress(v: Boolean) {
        _state.value = _state.value.copy(useManualAddress = v)
    }

    fun setManualAddress(v: String) {
        _state.value = _state.value.copy(manualAddress = v)
    }

    fun setPhone(v: String) {
        _state.value = _state.value.copy(phone = v)
    }

    fun setDeliverySlot(v: String) {
        _state.value = _state.value.copy(deliverySlot = v)
        refreshQuote()
    }

    fun setPaymentMethod(v: String) {
        _state.value = _state.value.copy(paymentMethod = v)
    }

    private fun currentAddressText(): String {
        val s = _state.value
        if (s.useManualAddress) return s.manualAddress.trim()
        val selected = (s.addresses as? UiState.Success)?.data?.firstOrNull { it.id == s.selectedAddressId }
        return selected?.address?.trim() ?: s.manualAddress.trim()
    }

    fun refreshQuote() {
        viewModelScope.launch {
            val s = _state.value
            _state.value = s.copy(quote = UiState.Loading)
            when (val r = cartRepository.quote(currentAddressText(), s.phone.trim(), s.deliverySlot)) {
                is ApiResult.Success -> _state.value = _state.value.copy(quote = UiState.Success(r.data))
                is ApiResult.Failure -> _state.value = _state.value.copy(quote = UiState.Error(r.message))
            }
        }
    }

    fun placeOrder() {
        val s = _state.value
        val address = currentAddressText()
        val phone = s.phone.trim()
        if (address.isBlank() || phone.isBlank()) {
            _state.value = s.copy(placeOrderState = ActionState.Failed("Please provide a delivery address and phone number"))
            return
        }
        if (cartRepository.isEmpty()) {
            _state.value = s.copy(placeOrderState = ActionState.Failed("Your cart is empty"))
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(placeOrderState = ActionState.InFlight)
            val email = sessionManager.sessionFlow.first().email
            val input = cartRepository.buildCheckoutInput(
                paymentMethod = s.paymentMethod,
                address = address,
                phone = phone,
                deliverySlot = s.deliverySlot,
                customerEmail = email?.ifBlank { null },
            )
            when (val r = ordersRepository.checkout(input)) {
                is ApiResult.Success -> {
                    val resp = r.data
                    cartRepository.clear()
                    cartRepository.resetPromos()
                    if (resp.razorpay != null) {
                        _state.value = _state.value.copy(
                            placeOrderState = ActionState.Done,
                            completedOrder = resp.order,
                            razorpayLaunch = RazorpayLaunchRequest(
                                localOrderId = resp.order.id,
                                razorpayOrderId = resp.razorpay.order_id,
                                amountPaise = resp.razorpay.amount,
                                keyId = resp.razorpay.key_id,
                            ),
                        )
                    } else {
                        // COD (or any method without a razorpay block) — order is placed outright.
                        _state.value = _state.value.copy(
                            placeOrderState = ActionState.Done,
                            completedOrder = resp.order,
                            paymentStatus = "cod",
                        )
                    }
                }
                is ApiResult.Failure -> _state.value = _state.value.copy(placeOrderState = ActionState.Failed(r.message))
            }
        }
    }

    fun consumeRazorpayLaunch() {
        _state.value = _state.value.copy(razorpayLaunch = null)
    }

    fun onRazorpayResult(paymentId: String?, razorpayOrderId: String?, signature: String?, error: String?) {
        val order = _state.value.completedOrder ?: return
        if (error != null || paymentId == null || razorpayOrderId == null || signature == null) {
            _state.value = _state.value.copy(paymentStatus = "failed")
            return
        }
        viewModelScope.launch {
            when (ordersRepository.verifyRazorpay(razorpayOrderId, paymentId, signature)) {
                is ApiResult.Success -> _state.value = _state.value.copy(paymentStatus = "paid")
                is ApiResult.Failure -> _state.value = _state.value.copy(paymentStatus = "failed")
            }
        }
    }

    fun customerEmailOrNull(callback: (String?) -> Unit) {
        viewModelScope.launch { callback(sessionManager.sessionFlow.first().email) }
    }
}
