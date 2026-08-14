package com.lazyshopper.app.feature.shopkeeper.shops

import com.lazyshopper.app.core.data.remote.ApiResult
import com.lazyshopper.app.core.data.remote.api.OrdersApi
import com.lazyshopper.app.core.data.remote.api.ProductsApi
import com.lazyshopper.app.core.data.remote.dto.MessageResponse
import com.lazyshopper.app.core.data.remote.dto.PromoteCheckoutResponse
import com.lazyshopper.app.core.data.remote.dto.PromoteVerifyResponse
import com.lazyshopper.app.core.data.remote.dto.RazorpayVerifyInput
import com.lazyshopper.app.core.data.remote.dto.Shop
import com.lazyshopper.app.core.data.remote.dto.ShopInput
import com.lazyshopper.app.core.data.remote.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

/** Shop categories the backend validates server-side (see api_reference.md §4). */
val SHOP_CATEGORIES = listOf("vegetables", "masala", "kirana", "nonveg", "foodlive", "sweets")

@Singleton
class ShopsRepository @Inject constructor(
    private val productsApi: ProductsApi,
    private val ordersApi: OrdersApi,
) {
    suspend fun myShops(): ApiResult<List<Shop>> = safeApiCall { productsApi.myShops() }

    suspend fun createShop(input: ShopInput): ApiResult<Shop> = safeApiCall { productsApi.createShop(input) }

    suspend fun deleteShop(id: String): ApiResult<MessageResponse> = safeApiCall { productsApi.deleteShop(id) }

    suspend fun setTimings(id: String, openTime: String, closeTime: String): ApiResult<MessageResponse> =
        safeApiCall { productsApi.setShopTimings(id, openTime, closeTime) }

    suspend fun promoteCheckout(shopId: String, days: Int): ApiResult<PromoteCheckoutResponse> =
        safeApiCall { ordersApi.promoteCheckout(shopId, days) }

    suspend fun promoteVerify(
        razorpayOrderId: String,
        razorpayPaymentId: String,
        razorpaySignature: String,
    ): ApiResult<PromoteVerifyResponse> = safeApiCall {
        ordersApi.promoteVerify(RazorpayVerifyInput(razorpayOrderId, razorpayPaymentId, razorpaySignature))
    }
}
