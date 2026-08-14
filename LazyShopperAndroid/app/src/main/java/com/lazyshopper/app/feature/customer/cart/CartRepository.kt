package com.lazyshopper.app.feature.customer.cart

import com.lazyshopper.app.core.data.remote.ApiResult
import com.lazyshopper.app.core.data.remote.api.CartApi
import com.lazyshopper.app.core.data.remote.api.OrdersApi
import com.lazyshopper.app.core.data.remote.dto.CartItemDto
import com.lazyshopper.app.core.data.remote.dto.CartSyncInput
import com.lazyshopper.app.core.data.remote.dto.CheckoutInput
import com.lazyshopper.app.core.data.remote.dto.CouponValidateBody
import com.lazyshopper.app.core.data.remote.dto.CouponValidateResponse
import com.lazyshopper.app.core.data.remote.dto.Product
import com.lazyshopper.app.core.data.remote.dto.QuoteResponse
import com.lazyshopper.app.core.data.remote.safeApiCall
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import javax.inject.Inject
import javax.inject.Singleton

/** A single cart line: the full product (so the UI has name/image/price without re-fetching) + chosen qty. */
data class CartLine(val product: Product, val qty: Double)

/**
 * Client-side source of truth for the cart. There is no server "add to cart" endpoint — only a
 * best-effort `/cart/sync` for abandoned-cart recovery emails — so the cart lives here, keyed by
 * product id, and is opportunistically pushed to the server after every mutation.
 */
@Singleton
class CartRepository @Inject constructor(
    private val cartApi: CartApi,
    private val ordersApi: OrdersApi,
) {
    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _lines = MutableStateFlow<Map<String, CartLine>>(emptyMap())
    val lines: StateFlow<Map<String, CartLine>> = _lines.asStateFlow()

    // Coupon/referral/wallet selections live here (not in a screen-scoped ViewModel) so they
    // survive navigation from the Cart screen to the Checkout screen, which are separate
    // Hilt-ViewModel-scoped nav destinations.
    private val _couponCode = MutableStateFlow("")
    val couponCode: StateFlow<String> = _couponCode.asStateFlow()

    private val _couponMessage = MutableStateFlow<String?>(null)
    val couponMessage: StateFlow<String?> = _couponMessage.asStateFlow()

    private val _referralCode = MutableStateFlow("")
    val referralCode: StateFlow<String> = _referralCode.asStateFlow()

    private val _useWallet = MutableStateFlow(false)
    val useWallet: StateFlow<Boolean> = _useWallet.asStateFlow()

    fun setCouponCode(v: String) {
        _couponCode.value = v
        _couponMessage.value = null
    }

    fun setCouponMessage(v: String?) {
        _couponMessage.value = v
    }

    fun setReferralCode(v: String) {
        _referralCode.value = v
    }

    fun setUseWallet(v: Boolean) {
        _useWallet.value = v
    }

    /** Resets promo selections once an order has been successfully placed. */
    fun resetPromos() {
        _couponCode.value = ""
        _couponMessage.value = null
        _referralCode.value = ""
        _useWallet.value = false
    }

    fun qtyOf(productId: String): Double = _lines.value[productId]?.qty ?: 0.0

    fun addToCart(product: Product, qty: Double = 1.0) {
        val current = _lines.value
        val newQty = (current[product.id]?.qty ?: 0.0) + qty
        _lines.value = current + (product.id to CartLine(product, newQty))
        syncToServer()
    }

    fun setQty(product: Product, qty: Double) {
        val current = _lines.value
        _lines.value = if (qty <= 0.0) {
            current - product.id
        } else {
            current + (product.id to CartLine(product, qty))
        }
        syncToServer()
    }

    fun increment(product: Product) = setQty(product, qtyOf(product.id) + 1.0)

    fun decrement(product: Product) = setQty(product, qtyOf(product.id) - 1.0)

    fun removeLine(productId: String) {
        _lines.value = _lines.value - productId
        syncToServer()
    }

    fun clear() {
        _lines.value = emptyMap()
        syncToServer()
    }

    fun subtotal(): Double = _lines.value.values.sumOf { it.product.price * it.qty }

    fun isEmpty(): Boolean = _lines.value.isEmpty()

    /** Live price breakdown preview — safe to call on every cart/coupon/referral/wallet change. */
    suspend fun quote(
        address: String,
        phone: String,
        deliverySlot: String,
        custLat: Double? = null,
        custLng: Double? = null,
    ): ApiResult<QuoteResponse> {
        val items = _lines.value.values.map { CartItemDto(it.product.id, it.qty) }
        return safeApiCall {
            ordersApi.quote(
                CheckoutInput(
                    items = items,
                    payment_method = "cod",
                    address = address,
                    phone = phone,
                    origin_url = "",
                    delivery_slot = deliverySlot.ifBlank { "Morning" },
                    coupon_code = _couponCode.value.trim().ifBlank { null },
                    cust_lat = custLat,
                    cust_lng = custLng,
                    referral_code = _referralCode.value.trim().ifBlank { null },
                    use_wallet = _useWallet.value,
                ),
            )
        }
    }

    /** Builds the real checkout payload from the current cart + promo selections. */
    fun buildCheckoutInput(
        paymentMethod: String,
        address: String,
        phone: String,
        deliverySlot: String,
        customerEmail: String?,
        custLat: Double? = null,
        custLng: Double? = null,
    ): CheckoutInput = CheckoutInput(
        items = _lines.value.values.map { CartItemDto(it.product.id, it.qty) },
        payment_method = paymentMethod,
        address = address,
        phone = phone,
        origin_url = "https://www.lazyshopper.in/",
        delivery_slot = deliverySlot.ifBlank { "Morning" },
        coupon_code = _couponCode.value.trim().ifBlank { null },
        customer_email = customerEmail,
        cust_lat = custLat,
        cust_lng = custLng,
        referral_code = _referralCode.value.trim().ifBlank { null },
        use_wallet = _useWallet.value,
    )

    suspend fun validateCoupon(code: String): ApiResult<CouponValidateResponse> =
        safeApiCall { cartApi.validateCoupon(CouponValidateBody(code.trim().uppercase(), subtotal())) }

    /** Fire-and-forget best-effort persistence for abandoned-cart recovery emails. */
    private fun syncToServer() {
        val snapshot = _lines.value.values.toList()
        repoScope.launch {
            val items = snapshot.map { line ->
                buildJsonObject {
                    put("product_id", JsonPrimitive(line.product.id))
                    put("name", JsonPrimitive(line.product.name))
                    put("qty", JsonPrimitive(line.qty))
                    put("price", JsonPrimitive(line.product.price))
                    put("shop_id", JsonPrimitive(line.product.shop_id))
                }
            }
            val total = snapshot.sumOf { it.product.price * it.qty }
            runCatching { cartApi.syncCart(CartSyncInput(items = items, total = total)) }
        }
    }
}
