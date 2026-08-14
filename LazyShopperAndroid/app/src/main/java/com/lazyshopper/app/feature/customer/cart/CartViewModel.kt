package com.lazyshopper.app.feature.customer.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lazyshopper.app.core.data.remote.ApiResult
import com.lazyshopper.app.core.data.remote.dto.QuoteResponse
import com.lazyshopper.app.core.ui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CartViewModel @Inject constructor(
    val cartRepository: CartRepository,
) : ViewModel() {

    val lines: StateFlow<Map<String, CartLine>> = cartRepository.lines
    val couponCode: StateFlow<String> = cartRepository.couponCode
    val couponMessage: StateFlow<String?> = cartRepository.couponMessage
    val referralCode: StateFlow<String> = cartRepository.referralCode
    val useWallet: StateFlow<Boolean> = cartRepository.useWallet

    private val _quote = MutableStateFlow<UiState<QuoteResponse>>(UiState.Idle)
    val quote: StateFlow<UiState<QuoteResponse>> = _quote.asStateFlow()

    private var quoteJob: Job? = null

    fun increment(line: CartLine) {
        cartRepository.increment(line.product)
        refreshQuoteDebounced()
    }

    fun decrement(line: CartLine) {
        cartRepository.decrement(line.product)
        refreshQuoteDebounced()
    }

    fun removeLine(productId: String) {
        cartRepository.removeLine(productId)
        refreshQuoteDebounced()
    }

    fun setCoupon(v: String) = cartRepository.setCouponCode(v)

    fun applyCoupon() {
        val code = couponCode.value.trim()
        if (code.isBlank()) return
        viewModelScope.launch {
            when (val r = cartRepository.validateCoupon(code)) {
                is ApiResult.Success -> {
                    cartRepository.setCouponMessage("Coupon applied: -${"₹%.2f".format(r.data.discount)}")
                    refreshQuoteDebounced()
                }
                is ApiResult.Failure -> cartRepository.setCouponMessage(r.message)
            }
        }
    }

    fun setReferralCode(v: String) {
        cartRepository.setReferralCode(v)
        refreshQuoteDebounced()
    }

    fun setUseWallet(v: Boolean) {
        cartRepository.setUseWallet(v)
        refreshQuoteDebounced()
    }

    /** Re-fetch the price breakdown after any cart/coupon/referral/wallet change (debounced). */
    fun refreshQuoteDebounced(address: String = "", phone: String = "", deliverySlot: String = "Morning") {
        quoteJob?.cancel()
        if (cartRepository.isEmpty()) {
            _quote.value = UiState.Success(QuoteResponse())
            return
        }
        quoteJob = viewModelScope.launch {
            delay(250)
            _quote.value = UiState.Loading
            when (val r = cartRepository.quote(address = address, phone = phone, deliverySlot = deliverySlot)) {
                is ApiResult.Success -> _quote.value = UiState.Success(r.data)
                is ApiResult.Failure -> _quote.value = UiState.Error(r.message)
            }
        }
    }
}
