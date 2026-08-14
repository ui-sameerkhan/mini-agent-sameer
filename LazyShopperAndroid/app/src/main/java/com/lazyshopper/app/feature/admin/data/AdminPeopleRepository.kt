package com.lazyshopper.app.feature.admin.data

import com.lazyshopper.app.core.data.remote.ApiResult
import com.lazyshopper.app.core.data.remote.api.AdminApi
import com.lazyshopper.app.core.data.remote.api.AuthApi
import com.lazyshopper.app.core.data.remote.api.ProductsApi
import com.lazyshopper.app.core.data.remote.dto.AdminDeliveryEarningsResponse
import com.lazyshopper.app.core.data.remote.dto.DeliveryPartnerInput
import com.lazyshopper.app.core.data.remote.dto.DeliveryPartnerSummary
import com.lazyshopper.app.core.data.remote.dto.DeliverySettlementBatch
import com.lazyshopper.app.core.data.remote.dto.KycRecord
import com.lazyshopper.app.core.data.remote.dto.KycReviewInput
import com.lazyshopper.app.core.data.remote.dto.MessageResponse
import com.lazyshopper.app.core.data.remote.dto.Order
import com.lazyshopper.app.core.data.remote.dto.SettleResponse
import com.lazyshopper.app.core.data.remote.dto.Shop
import com.lazyshopper.app.core.data.remote.dto.User
import com.lazyshopper.app.core.data.remote.safeApiCall
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import javax.inject.Inject
import javax.inject.Singleton

/** Users, Shops, Delivery Partners & KYC approvals. */
@Singleton
class AdminPeopleRepository @Inject constructor(
    private val adminApi: AdminApi,
    private val authApi: AuthApi,
    private val productsApi: ProductsApi,
    private val json: Json,
) {
    // Users
    suspend fun users(role: String? = null, status: String? = null, search: String? = null): ApiResult<List<User>> =
        safeApiCall { adminApi.users(role, status, search) }
    suspend fun setUserStatus(id: String, status: String): ApiResult<MessageResponse> = safeApiCall { adminApi.setUserStatus(id, status) }
    suspend fun deleteUser(id: String): ApiResult<MessageResponse> = safeApiCall { adminApi.deleteUser(id) }
    suspend fun userOrders(id: String): ApiResult<List<Order>> = safeApiCall { adminApi.userOrders(id) }

    // Shops
    suspend fun shops(): ApiResult<List<Shop>> = safeApiCall { productsApi.adminShops() }
    suspend fun approveShop(id: String, status: String): ApiResult<MessageResponse> = safeApiCall { productsApi.approveShop(id, status) }
    suspend fun deleteShop(id: String): ApiResult<MessageResponse> = safeApiCall { productsApi.deleteShop(id) }
    suspend fun promoteShop(id: String, days: Int): ApiResult<MessageResponse> = safeApiCall { adminApi.promoteShop(id, days) }
    suspend fun unpromoteShop(id: String): ApiResult<MessageResponse> = safeApiCall { adminApi.unpromoteShop(id) }
    suspend fun reassignShop(id: String, ownerId: String): ApiResult<MessageResponse> = safeApiCall { adminApi.reassignShop(id, ownerId) }

    // Delivery partners
    suspend fun deliveryPartners(): ApiResult<List<DeliveryPartnerSummary>> = safeApiCall { adminApi.deliveryPartners() }
    suspend fun createDeliveryPartner(input: DeliveryPartnerInput): ApiResult<MessageResponse> = safeApiCall { adminApi.createDeliveryPartner(input) }
    suspend fun updateDeliveryPartner(id: String, name: String?, phone: String?, password: String?): ApiResult<MessageResponse> {
        val body = buildJsonObject {
            name?.let { put("name", JsonPrimitive(it)) }
            phone?.let { put("phone", JsonPrimitive(it)) }
            password?.let { put("password", JsonPrimitive(it)) }
        }
        return safeApiCall { adminApi.updateDeliveryPartner(id, body) }
    }
    suspend fun deliveryEarnings(): ApiResult<AdminDeliveryEarningsResponse> = safeApiCall { adminApi.deliveryEarnings() }
    suspend fun settleDeliveryPayout(id: String): ApiResult<SettleResponse> = safeApiCall { adminApi.settleDeliveryPayout(id) }
    suspend fun deliverySettlements(): ApiResult<List<DeliverySettlementBatch>> = safeApiCall { adminApi.deliverySettlements() }

    // KYC — adminKycList returns a raw JsonObject shaped {"kyc": [KycRecord...]}
    suspend fun kycList(status: String? = null): ApiResult<List<KycRecord>> = safeApiCall {
        val obj: JsonObject = authApi.adminKycList(status)
        val arr = obj["kyc"] as? JsonArray ?: JsonArray(emptyList())
        json.decodeFromJsonElement<List<KycRecord>>(arr)
    }
    suspend fun reviewKyc(userId: String, status: String, reason: String? = null): ApiResult<MessageResponse> =
        safeApiCall { authApi.adminKycReview(userId, KycReviewInput(status, reason)) }
}
