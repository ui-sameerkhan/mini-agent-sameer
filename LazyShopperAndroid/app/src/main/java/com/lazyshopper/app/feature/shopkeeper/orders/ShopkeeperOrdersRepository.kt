package com.lazyshopper.app.feature.shopkeeper.orders

import com.lazyshopper.app.core.data.remote.ApiResult
import com.lazyshopper.app.core.data.remote.api.OrdersApi
import com.lazyshopper.app.core.data.remote.dto.MessageResponse
import com.lazyshopper.app.core.data.remote.dto.Order
import com.lazyshopper.app.core.data.remote.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Statuses a shopkeeper is expected to move an order through on their end — pickup/delivery
 * transitions (`out_for_delivery` -> `delivered`) belong to the delivery partner's app instead.
 */
val SHOPKEEPER_ORDER_STATUSES = listOf("accepted", "preparing", "ready", "packed", "cancelled")

@Singleton
class ShopkeeperOrdersRepository @Inject constructor(
    private val ordersApi: OrdersApi,
) {
    suspend fun receivedOrders(): ApiResult<List<Order>> = safeApiCall { ordersApi.receivedOrders() }

    suspend fun updateStatus(orderId: String, status: String): ApiResult<MessageResponse> =
        safeApiCall { ordersApi.updateOrderStatus(orderId, status) }
}
