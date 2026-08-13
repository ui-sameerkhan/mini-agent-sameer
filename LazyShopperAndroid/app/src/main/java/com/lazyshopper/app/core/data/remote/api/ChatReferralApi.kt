package com.lazyshopper.app.core.data.remote.api

import com.lazyshopper.app.core.data.remote.dto.*
import retrofit2.http.*

interface ChatApi {
    @GET("api/chat/order/{oid}")
    suspend fun orderChat(@Path("oid") orderId: String): OrderChatResponse

    @POST("api/chat/order/{oid}")
    suspend fun sendOrderChat(@Path("oid") orderId: String, @Body body: ChatSend): ChatMessage

    @GET("api/chat/support")
    suspend fun supportChat(): SupportChatResponse

    @POST("api/chat/support")
    suspend fun sendSupportChat(@Body body: ChatSend): ChatMessage

    @GET("api/chat/unread")
    suspend fun unread(): UnreadResponse

    @GET("api/admin/support/threads")
    suspend fun adminSupportThreads(): List<SupportThread>

    @GET("api/admin/support/{customerId}")
    suspend fun adminSupportMessages(@Path("customerId") customerId: String): AdminSupportMessagesResponse

    @POST("api/admin/support/{customerId}")
    suspend fun adminSendSupport(@Path("customerId") customerId: String, @Body body: ChatSend): ChatMessage
}

interface ReferralApi {
    @GET("api/me/referral")
    suspend fun myReferral(): MyReferralResponse

    @GET("api/admin/referral-settings")
    suspend fun referralSettings(): ReferralSettings

    @PUT("api/admin/referral-settings")
    suspend fun updateReferralSettings(@Body body: kotlinx.serialization.json.JsonObject): MessageResponse

    @GET("api/admin/referrals")
    suspend fun adminReferrals(@Query("status") status: String? = null): AdminReferralsResponse

    @POST("api/admin/referrals/{rid}/approve")
    suspend fun approveReferral(@Path("rid") id: String): MessageResponse

    @POST("api/admin/referrals/{rid}/reject")
    suspend fun rejectReferral(@Path("rid") id: String): MessageResponse
}
