package com.lazyshopper.app.core.data.remote.api

import com.lazyshopper.app.core.data.remote.dto.*
import retrofit2.http.*

interface CartApi {
    @POST("api/coupons/validate")
    suspend fun validateCoupon(@Body body: CouponValidateBody): CouponValidateResponse

    @POST("api/cart/sync")
    suspend fun syncCart(@Body body: CartSyncInput): OkResponse

    @GET("api/cart/recover/{token}")
    suspend fun recoverCart(@Path("token") token: String): CartRecoverResponse
}

interface OrdersApi {
    @POST("api/orders/checkout")
    suspend fun checkout(@Body body: CheckoutInput): CheckoutResponse

    @POST("api/orders/quote")
    suspend fun quote(@Body body: CheckoutInput): QuoteResponse

    @POST("api/payments/razorpay/verify")
    suspend fun verifyRazorpay(@Body body: RazorpayVerifyInput): RazorpayVerifyResponse

    @GET("api/payments/status/{sessionId}")
    suspend fun stripeStatus(@Path("sessionId") sessionId: String): kotlinx.serialization.json.JsonObject

    @POST("api/shopkeeper/promote/checkout")
    suspend fun promoteCheckout(
        @Query("shop_id") shopId: String,
        @Query("days") days: Int = 30,
    ): PromoteCheckoutResponse

    @POST("api/shopkeeper/promote/verify")
    suspend fun promoteVerify(@Body body: RazorpayVerifyInput): PromoteVerifyResponse

    @GET("api/orders/mine")
    suspend fun myOrders(): List<Order>

    @GET("api/orders/received")
    suspend fun receivedOrders(): List<Order>

    @PUT("api/orders/{oid}/status")
    suspend fun updateOrderStatus(@Path("oid") id: String, @Query("status") status: String): MessageResponse

    @PUT("api/delivery/location")
    suspend fun pushLocation(@Body body: LocationInput): MessageResponse

    @PUT("api/delivery/availability")
    suspend fun setAvailability(@Body body: AvailabilityInput): kotlinx.serialization.json.JsonObject

    @GET("api/delivery/available-orders")
    suspend fun availableOrders(): List<Order>

    @POST("api/delivery/orders/{oid}/accept")
    suspend fun acceptOrder(@Path("oid") id: String): MessageResponse

    @POST("api/delivery/orders/{oid}/reject")
    suspend fun rejectOrder(@Path("oid") id: String): MessageResponse

    @POST("api/delivery/orders/{oid}/pickup")
    suspend fun pickupOrder(@Path("oid") id: String): MessageResponse

    @POST("api/delivery/orders/{oid}/deliver")
    suspend fun deliverOrder(@Path("oid") id: String, @Body body: DeliverInput): DeliverResponse

    @GET("api/delivery/orders/mine")
    suspend fun myDeliveryOrders(): List<Order>

    @GET("api/delivery/earnings")
    suspend fun deliveryEarnings(): DeliveryEarningsResponse

    @GET("api/orders/{oid}/tracking")
    suspend fun tracking(@Path("oid") id: String): TrackingResponse

    @GET("api/orders/frequent")
    suspend fun frequentProducts(): List<Product>

    @GET("api/orders/product-history/{pid}")
    suspend fun productHistory(@Path("pid") productId: String): ProductHistoryResponse

    @GET("api/shopkeeper/earnings")
    suspend fun shopkeeperEarnings(): ShopkeeperEarningsResponse
}
