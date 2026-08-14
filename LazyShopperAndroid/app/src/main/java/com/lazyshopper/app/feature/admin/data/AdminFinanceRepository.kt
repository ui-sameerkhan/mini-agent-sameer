package com.lazyshopper.app.feature.admin.data

import com.lazyshopper.app.core.data.remote.ApiResult
import com.lazyshopper.app.core.data.remote.api.AdminApi
import com.lazyshopper.app.core.data.remote.api.ReferralApi
import com.lazyshopper.app.core.data.remote.dto.AdminReferralsResponse
import com.lazyshopper.app.core.data.remote.dto.CommissionBreakdownResponse
import com.lazyshopper.app.core.data.remote.dto.CommissionSettings
import com.lazyshopper.app.core.data.remote.dto.MessageResponse
import com.lazyshopper.app.core.data.remote.dto.PayoutOrdersResponse
import com.lazyshopper.app.core.data.remote.dto.PayoutsResponse
import com.lazyshopper.app.core.data.remote.dto.ProfitSummaryResponse
import com.lazyshopper.app.core.data.remote.dto.PromotionsResponse
import com.lazyshopper.app.core.data.remote.dto.ReferralSettings
import com.lazyshopper.app.core.data.remote.dto.RiderPayoutSettings
import com.lazyshopper.app.core.data.remote.dto.ScheduleResponse
import com.lazyshopper.app.core.data.remote.dto.SettleAllResponse
import com.lazyshopper.app.core.data.remote.dto.SettleResponse
import com.lazyshopper.app.core.data.remote.dto.Settlement
import com.lazyshopper.app.core.data.remote.dto.WeightSlab
import com.lazyshopper.app.core.data.remote.safeApiCall
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

/** Commission, Rider Payout settings, Shopkeeper Payouts, Profit Summary, Promotions & Referrals. */
@Singleton
class AdminFinanceRepository @Inject constructor(
    private val adminApi: AdminApi,
    private val referralApi: ReferralApi,
) {
    // Commission
    suspend fun commissionSettings(): ApiResult<CommissionSettings> = safeApiCall { adminApi.commissionSettings() }
    suspend fun updateCommissionSettings(
        globalDefault: Double?,
        minPayout: Double?,
        categories: Map<String, Double>?,
        shops: Map<String, Double>?,
        settlementCycleDays: Int?,
    ): ApiResult<MessageResponse> {
        val body = buildJsonObject {
            globalDefault?.let { put("global_default", it) }
            minPayout?.let { put("min_payout", it) }
            categories?.let { m -> put("categories", buildJsonObject { m.forEach { (k, v) -> put(k, v) } }) }
            shops?.let { m -> put("shops", buildJsonObject { m.forEach { (k, v) -> put(k, v) } }) }
            settlementCycleDays?.let { put("settlement_cycle_days", it) }
        }
        return safeApiCall { adminApi.updateCommissionSettings(body) }
    }
    suspend fun commissionBreakdown(): ApiResult<CommissionBreakdownResponse> = safeApiCall { adminApi.commissionBreakdown() }

    // Rider payout settings
    suspend fun riderPayoutSettings(): ApiResult<RiderPayoutSettings> = safeApiCall { adminApi.riderPayoutSettings() }
    suspend fun updateRiderPayoutSettings(
        baseFee: Double?,
        baseKm: Double?,
        perKm: Double?,
        freeDeliveryMin: Double?,
        firstOrderFree: Boolean?,
        weightSlabs: List<WeightSlab>?,
    ): ApiResult<MessageResponse> {
        val body = buildJsonObject {
            baseFee?.let { put("base_fee", it) }
            baseKm?.let { put("base_km", it) }
            perKm?.let { put("per_km", it) }
            freeDeliveryMin?.let { put("free_delivery_min", it) }
            firstOrderFree?.let { put("first_order_free", it) }
            weightSlabs?.let { slabs ->
                put(
                    "weight_slabs",
                    JsonArray(
                        slabs.map { s ->
                            buildJsonObject {
                                put("upto", s.upto?.let { JsonPrimitive(it) } ?: JsonNull)
                                put("fee", s.fee)
                            }
                        },
                    ),
                )
            }
        }
        return safeApiCall { adminApi.updateRiderPayoutSettings(body) }
    }

    // Shopkeeper payouts
    suspend fun payouts(): ApiResult<PayoutsResponse> = safeApiCall { adminApi.payouts() }
    suspend fun settlePayout(id: String): ApiResult<SettleResponse> = safeApiCall { adminApi.settlePayout(id) }
    suspend fun settleAll(): ApiResult<SettleAllResponse> = safeApiCall { adminApi.settleAll() }
    suspend fun schedulePayout(id: String, date: String? = null, days: Int? = null): ApiResult<ScheduleResponse> {
        val body = buildJsonObject {
            date?.let { put("date", it) }
            days?.let { put("days", it) }
        }
        return safeApiCall { adminApi.schedulePayout(id, body) }
    }
    suspend fun payoutOrders(id: String): ApiResult<PayoutOrdersResponse> = safeApiCall { adminApi.payoutOrders(id) }
    suspend fun settlements(): ApiResult<List<Settlement>> = safeApiCall { adminApi.settlements() }

    // Profit summary
    suspend fun profitSummary(month: String? = null): ApiResult<ProfitSummaryResponse> = safeApiCall { adminApi.profitSummary(month) }

    // Promotions
    suspend fun promotions(): ApiResult<PromotionsResponse> = safeApiCall { adminApi.promotions() }

    // Referrals
    suspend fun referralSettings(): ApiResult<ReferralSettings> = safeApiCall { referralApi.referralSettings() }
    suspend fun updateReferralSettings(
        enabled: Boolean?,
        discountAmount: Double?,
        rewardAmount: Double?,
        minOrderValue: Double?,
        maxDiscount: Double?,
        rewardLimitPerUser: Int?,
    ): ApiResult<MessageResponse> {
        val body = buildJsonObject {
            enabled?.let { put("enabled", it) }
            discountAmount?.let { put("discount_amount", it) }
            rewardAmount?.let { put("reward_amount", it) }
            minOrderValue?.let { put("min_order_value", it) }
            maxDiscount?.let { put("max_discount", it) }
            rewardLimitPerUser?.let { put("reward_limit_per_user", it) }
        }
        return safeApiCall { referralApi.updateReferralSettings(body) }
    }
    suspend fun referrals(status: String? = null): ApiResult<AdminReferralsResponse> = safeApiCall { referralApi.adminReferrals(status) }
    suspend fun approveReferral(id: String): ApiResult<MessageResponse> = safeApiCall { referralApi.approveReferral(id) }
    suspend fun rejectReferral(id: String): ApiResult<MessageResponse> = safeApiCall { referralApi.rejectReferral(id) }
}
