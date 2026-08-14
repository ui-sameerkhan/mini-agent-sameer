package com.lazyshopper.app.feature.shopkeeper.dashboard

import com.lazyshopper.app.core.data.remote.ApiResult
import com.lazyshopper.app.core.data.remote.api.OrdersApi
import com.lazyshopper.app.core.data.remote.api.ProductsApi
import com.lazyshopper.app.core.data.remote.dto.Order
import com.lazyshopper.app.core.data.remote.dto.Product
import com.lazyshopper.app.core.data.remote.dto.Shop
import com.lazyshopper.app.core.data.remote.dto.ShopkeeperEarningsResponse
import com.lazyshopper.app.core.data.remote.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

/** Overview data for the shopkeeper Dashboard tab: earnings summary + shop/product/order counts. */
@Singleton
class ShopkeeperDashboardRepository @Inject constructor(
    private val productsApi: ProductsApi,
    private val ordersApi: OrdersApi,
) {
    suspend fun earnings(): ApiResult<ShopkeeperEarningsResponse> = safeApiCall { ordersApi.shopkeeperEarnings() }

    suspend fun myShops(): ApiResult<List<Shop>> = safeApiCall { productsApi.myShops() }

    suspend fun myProducts(): ApiResult<List<Product>> = safeApiCall { productsApi.myProducts() }

    suspend fun receivedOrders(): ApiResult<List<Order>> = safeApiCall { ordersApi.receivedOrders() }
}
