package com.lazyshopper.app.core.data.remote.api

import com.lazyshopper.app.core.data.remote.dto.*
import kotlinx.serialization.json.JsonObject
import retrofit2.http.*

interface AdminApi {
    // Delivery partner management
    @GET("api/admin/delivery-partners")
    suspend fun deliveryPartners(): List<DeliveryPartnerSummary>

    @PUT("api/admin/orders/{oid}/assign")
    suspend fun assignOrder(@Path("oid") orderId: String, @Query("delivery_id") deliveryId: String): MessageResponse

    @POST("api/admin/delivery-partners")
    suspend fun createDeliveryPartner(@Body body: DeliveryPartnerInput): MessageResponse

    @PUT("api/admin/delivery-partners/{uid}")
    suspend fun updateDeliveryPartner(@Path("uid") id: String, @Body body: JsonObject): MessageResponse

    @GET("api/admin/delivery/earnings")
    suspend fun deliveryEarnings(): AdminDeliveryEarningsResponse

    @POST("api/admin/delivery/payouts/{did}/settle")
    suspend fun settleDeliveryPayout(@Path("did") deliveryId: String): SettleResponse

    @GET("api/admin/delivery/settlements")
    suspend fun deliverySettlements(): List<DeliverySettlementBatch>

    // Shop management
    @PUT("api/admin/shops/{sid}/reassign")
    suspend fun reassignShop(@Path("sid") id: String, @Query("owner_id") ownerId: String): MessageResponse

    @POST("api/admin/shops/{sid}/promote")
    suspend fun promoteShop(@Path("sid") id: String, @Query("days") days: Int = 7): MessageResponse

    @POST("api/admin/shops/{sid}/unpromote")
    suspend fun unpromoteShop(@Path("sid") id: String): MessageResponse

    @GET("api/admin/promotions")
    suspend fun promotions(): PromotionsResponse

    // Reviews moderation
    @GET("api/admin/reviews")
    suspend fun adminReviews(): List<Review>

    @PUT("api/admin/reviews/{rid}/hide")
    suspend fun hideReview(@Path("rid") id: String, @Query("hidden") hidden: Boolean = true): MessageResponse

    @DELETE("api/admin/reviews/{rid}")
    suspend fun deleteReview(@Path("rid") id: String): MessageResponse

    // Stats
    @GET("api/admin/stats")
    suspend fun stats(): AdminStats

    // Categories
    @POST("api/admin/categories")
    suspend fun createCategory(@Body body: CategoryInput): Category

    @PUT("api/admin/categories/{cid}")
    suspend fun updateCategory(@Path("cid") id: String, @Body body: CategoryInput): MessageResponse

    @DELETE("api/admin/categories/{cid}")
    suspend fun deleteCategory(@Path("cid") id: String): MessageResponse

    // Coupons
    @GET("api/admin/coupons")
    suspend fun coupons(): List<Coupon>

    @POST("api/admin/coupons")
    suspend fun createCoupon(@Body body: CouponInput): Coupon

    @PUT("api/admin/coupons/{cid}")
    suspend fun updateCoupon(@Path("cid") id: String, @Body body: CouponInput): MessageResponse

    @DELETE("api/admin/coupons/{cid}")
    suspend fun deleteCoupon(@Path("cid") id: String): MessageResponse

    // Users
    @GET("api/admin/users")
    suspend fun users(
        @Query("role") role: String? = null,
        @Query("status") status: String? = null,
        @Query("search") search: String? = null,
    ): List<User>

    @PUT("api/admin/users/{uid}/status")
    suspend fun setUserStatus(@Path("uid") id: String, @Query("status") status: String): MessageResponse

    @DELETE("api/admin/users/{uid}")
    suspend fun deleteUser(@Path("uid") id: String): MessageResponse

    @GET("api/admin/users/{uid}/orders")
    suspend fun userOrders(@Path("uid") id: String): List<Order>

    // Payments overview
    @GET("api/admin/payments")
    suspend fun payments(): AdminPaymentsResponse

    @GET("api/admin/logs")
    suspend fun logs(): List<AdminLogEntry>

    // Offers
    @GET("api/admin/offers")
    suspend fun offers(): List<Offer>

    @POST("api/admin/offers")
    suspend fun createOffer(@Body body: OfferInput): Offer

    @PUT("api/admin/offers/{oid}")
    suspend fun updateOffer(@Path("oid") id: String, @Body body: OfferInput): MessageResponse

    @PUT("api/admin/offers/{oid}/toggle")
    suspend fun toggleOffer(@Path("oid") id: String, @Query("active") active: Boolean): MessageResponse

    @DELETE("api/admin/offers/{oid}")
    suspend fun deleteOffer(@Path("oid") id: String): MessageResponse

    // Banners
    @GET("api/admin/banners")
    suspend fun adminBanners(): List<Banner>

    @POST("api/admin/banners")
    suspend fun createBanner(@Body body: BannerInput): Banner

    @PUT("api/admin/banners/{bid}")
    suspend fun updateBanner(@Path("bid") id: String, @Body body: BannerInput): MessageResponse

    @PUT("api/admin/banners/{bid}/toggle")
    suspend fun toggleBanner(@Path("bid") id: String, @Query("active") active: Boolean): MessageResponse

    @DELETE("api/admin/banners/{bid}")
    suspend fun deleteBanner(@Path("bid") id: String): MessageResponse

    // Video review moderation
    @GET("api/admin/video-reviews")
    suspend fun videoReviews(): List<Review>

    @PUT("api/admin/video-reviews/{rid}/moderate")
    suspend fun moderateVideoReview(@Path("rid") id: String, @Query("status") status: String): MessageResponse

    @DELETE("api/admin/video-reviews/{rid}")
    suspend fun deleteVideoReview(@Path("rid") id: String): MessageResponse

    // Order distribution / cancel / refund
    @GET("api/admin/orders/{oid}/distribution")
    suspend fun orderDistribution(@Path("oid") id: String): OrderDistributionResponse

    @POST("api/admin/orders/{oid}/cancel")
    suspend fun cancelOrder(@Path("oid") id: String): MessageResponse

    @POST("api/admin/orders/{oid}/refund")
    suspend fun refundOrder(@Path("oid") id: String): MessageResponse

    // Product quick flags
    @PUT("api/admin/products/{pid}/flags")
    suspend fun setProductFlags(@Path("pid") id: String, @Body body: JsonObject): MessageResponse

    // Notifications
    @POST("api/admin/notifications")
    suspend fun createNotification(@Body body: NotificationInput): NotificationDto

    @GET("api/admin/notifications")
    suspend fun adminNotifications(): List<NotificationDto>

    @DELETE("api/admin/notifications/{nid}")
    suspend fun deleteNotification(@Path("nid") id: String): MessageResponse

    // Email settings
    @GET("api/admin/email-settings")
    suspend fun emailSettings(): EmailSettingsResponse

    @PUT("api/admin/email-settings")
    suspend fun updateEmailSettings(@Body body: JsonObject): MessageResponse

    @POST("api/admin/email-settings/test")
    suspend fun testEmail(@Query("email") email: String): MessageResponse

    // Commission / rider payout / profit
    @GET("api/admin/profit-summary")
    suspend fun profitSummary(@Query("month") month: String? = null): ProfitSummaryResponse

    @GET("api/admin/rider-payout-settings")
    suspend fun riderPayoutSettings(): RiderPayoutSettings

    @PUT("api/admin/rider-payout-settings")
    suspend fun updateRiderPayoutSettings(@Body body: JsonObject): MessageResponse

    @GET("api/admin/commission-settings")
    suspend fun commissionSettings(): CommissionSettings

    @PUT("api/admin/commission-settings")
    suspend fun updateCommissionSettings(@Body body: JsonObject): MessageResponse

    @GET("api/admin/commission-breakdown")
    suspend fun commissionBreakdown(): CommissionBreakdownResponse

    // Shopkeeper payouts
    @GET("api/admin/payouts")
    suspend fun payouts(): PayoutsResponse

    @POST("api/admin/payouts/{sid}/schedule")
    suspend fun schedulePayout(@Path("sid") id: String, @Body body: JsonObject): ScheduleResponse

    @POST("api/admin/payouts/{sid}/settle")
    suspend fun settlePayout(@Path("sid") id: String): SettleResponse

    @POST("api/admin/payouts/sync/{payoutId}")
    suspend fun syncPayout(@Path("payoutId") payoutId: String): JsonObject

    @GET("api/admin/settlements")
    suspend fun settlements(): List<Settlement>

    @POST("api/admin/payouts/settle-all")
    suspend fun settleAll(): SettleAllResponse

    @GET("api/admin/payouts/{sid}/orders")
    suspend fun payoutOrders(@Path("sid") id: String): PayoutOrdersResponse
}

interface AnalyticsApi {
    @GET("api/admin/analytics")
    suspend fun analytics(@Query("days") days: Int = 30): AdminAnalyticsResponse

    @GET("api/admin/stock-report")
    suspend fun stockReport(): StockReportResponse

    @GET("api/admin/coupon-analytics")
    suspend fun couponAnalytics(): CouponAnalyticsResponse
}

interface CronsApi {
    @POST("api/cron/stock-digest")
    suspend fun stockDigest(): JsonObject

    @POST("api/cron/weekly-sales")
    suspend fun weeklySales(): JsonObject

    @POST("api/cron/abandoned-cart")
    suspend fun abandonedCart(): JsonObject

    @POST("api/cron/promotion-expiry")
    suspend fun promotionExpiry(): JsonObject

    @POST("api/cron/weekly-auto-settle")
    suspend fun weeklyAutoSettle(): JsonObject
}
