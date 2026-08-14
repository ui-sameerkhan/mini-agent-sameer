package com.lazyshopper.app.feature.admin.data

import com.lazyshopper.app.core.data.remote.ApiResult
import com.lazyshopper.app.core.data.remote.api.AdminApi
import com.lazyshopper.app.core.data.remote.api.OrdersApi
import com.lazyshopper.app.core.data.remote.dto.MessageResponse
import com.lazyshopper.app.core.data.remote.dto.Order
import com.lazyshopper.app.core.data.remote.dto.OrderDistributionResponse
import com.lazyshopper.app.core.data.remote.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdminOrdersRepository @Inject constructor(
    private val ordersApi: OrdersApi,
    private val adminApi: AdminApi,
) {
    suspend fun allOrders(): ApiResult<List<Order>> = safeApiCall { ordersApi.receivedOrders() }
    suspend fun updateStatus(id: String, status: String): ApiResult<MessageResponse> = safeApiCall { ordersApi.updateOrderStatus(id, status) }
    suspend fun assign(orderId: String, deliveryId: String): ApiResult<MessageResponse> = safeApiCall { adminApi.assignOrder(orderId, deliveryId) }
    suspend fun distribution(orderId: String): ApiResult<OrderDistributionResponse> = safeApiCall { adminApi.orderDistribution(orderId) }
    suspend fun cancel(orderId: String): ApiResult<MessageResponse> = safeApiCall { adminApi.cancelOrder(orderId) }
    suspend fun refund(orderId: String): ApiResult<MessageResponse> = safeApiCall { adminApi.refundOrder(orderId) }
}
