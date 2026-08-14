package com.lazyshopper.app.feature.customer.orders

import com.lazyshopper.app.core.data.remote.ApiResult
import com.lazyshopper.app.core.data.remote.api.OrdersApi
import com.lazyshopper.app.core.data.remote.dto.CheckoutInput
import com.lazyshopper.app.core.data.remote.dto.CheckoutResponse
import com.lazyshopper.app.core.data.remote.dto.Order
import com.lazyshopper.app.core.data.remote.dto.RazorpayVerifyInput
import com.lazyshopper.app.core.data.remote.dto.RazorpayVerifyResponse
import com.lazyshopper.app.core.data.remote.dto.TrackingResponse
import com.lazyshopper.app.core.data.remote.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrdersRepository @Inject constructor(
    private val ordersApi: OrdersApi,
) {
    suspend fun checkout(input: CheckoutInput): ApiResult<CheckoutResponse> =
        safeApiCall { ordersApi.checkout(input) }

    suspend fun verifyRazorpay(orderId: String, paymentId: String, signature: String): ApiResult<RazorpayVerifyResponse> =
        safeApiCall { ordersApi.verifyRazorpay(RazorpayVerifyInput(orderId, paymentId, signature)) }

    suspend fun myOrders(): ApiResult<List<Order>> = safeApiCall { ordersApi.myOrders() }

    suspend fun tracking(orderId: String): ApiResult<TrackingResponse> = safeApiCall { ordersApi.tracking(orderId) }

    suspend fun frequentProducts() = safeApiCall { ordersApi.frequentProducts() }
}
