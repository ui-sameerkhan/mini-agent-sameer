package com.lazyshopper.app.feature.shopkeeper.earnings

import com.lazyshopper.app.core.data.remote.ApiResult
import com.lazyshopper.app.core.data.remote.api.AuthApi
import com.lazyshopper.app.core.data.remote.api.OrdersApi
import com.lazyshopper.app.core.data.remote.dto.BankDetails
import com.lazyshopper.app.core.data.remote.dto.BankDetailsInput
import com.lazyshopper.app.core.data.remote.dto.ShopkeeperEarningsResponse
import com.lazyshopper.app.core.data.remote.safeApiCall
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import javax.inject.Inject
import javax.inject.Singleton

/** Full earnings tab: breakdown + settlement history (from `/api/shopkeeper/earnings`) and payout bank details. */
@Singleton
class ShopkeeperEarningsRepository @Inject constructor(
    private val ordersApi: OrdersApi,
    private val authApi: AuthApi,
    private val json: Json,
) {
    suspend fun earnings(): ApiResult<ShopkeeperEarningsResponse> = safeApiCall { ordersApi.shopkeeperEarnings() }

    suspend fun bank(): ApiResult<BankDetails> = safeApiCall { parseBank(authApi.myBank()) }

    suspend fun updateBank(input: BankDetailsInput): ApiResult<BankDetails> =
        safeApiCall { parseBank(authApi.updateBank(input)) }

    private fun parseBank(obj: JsonObject): BankDetails {
        val bank = obj["bank"] ?: return BankDetails()
        if (bank is JsonNull) return BankDetails()
        return json.decodeFromJsonElement(BankDetails.serializer(), bank)
    }
}
