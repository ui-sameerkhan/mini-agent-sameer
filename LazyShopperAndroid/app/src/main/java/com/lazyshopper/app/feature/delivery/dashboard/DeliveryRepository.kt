package com.lazyshopper.app.feature.delivery.dashboard

import com.lazyshopper.app.core.data.remote.ApiResult
import com.lazyshopper.app.core.data.remote.api.AuthApi
import com.lazyshopper.app.core.data.remote.api.OrdersApi
import com.lazyshopper.app.core.data.remote.dto.AvailabilityInput
import com.lazyshopper.app.core.data.remote.dto.BankDetails
import com.lazyshopper.app.core.data.remote.dto.BankDetailsInput
import com.lazyshopper.app.core.data.remote.dto.DeliverInput
import com.lazyshopper.app.core.data.remote.dto.DeliveryEarningsResponse
import com.lazyshopper.app.core.data.remote.dto.DeliveryPayoutBatch
import com.lazyshopper.app.core.data.remote.dto.LocationInput
import com.lazyshopper.app.core.data.remote.dto.MessageResponse
import com.lazyshopper.app.core.data.remote.dto.Order
import com.lazyshopper.app.core.data.remote.dto.UserResponse
import com.lazyshopper.app.core.data.remote.safeApiCall
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import javax.inject.Inject
import javax.inject.Singleton

/** Everything a delivery partner does after KYC is approved: go online, push GPS, work orders, get paid. */
@Singleton
class DeliveryRepository @Inject constructor(
    private val ordersApi: OrdersApi,
    private val authApi: AuthApi,
    private val json: Json,
) {
    suspend fun me(): ApiResult<UserResponse> = safeApiCall { authApi.me() }

    suspend fun pushLocation(lat: Double, lng: Double): ApiResult<MessageResponse> =
        safeApiCall { ordersApi.pushLocation(LocationInput(lat, lng)) }

    suspend fun setAvailability(online: Boolean): ApiResult<JsonObject> =
        safeApiCall { ordersApi.setAvailability(AvailabilityInput(online)) }

    suspend fun availableOrders(): ApiResult<List<Order>> = safeApiCall { ordersApi.availableOrders() }

    suspend fun acceptOrder(orderId: String): ApiResult<MessageResponse> = safeApiCall { ordersApi.acceptOrder(orderId) }

    suspend fun rejectOrder(orderId: String): ApiResult<MessageResponse> = safeApiCall { ordersApi.rejectOrder(orderId) }

    suspend fun pickupOrder(orderId: String): ApiResult<MessageResponse> = safeApiCall { ordersApi.pickupOrder(orderId) }

    suspend fun deliverOrder(orderId: String, otp: String) =
        safeApiCall { ordersApi.deliverOrder(orderId, DeliverInput(otp)) }

    suspend fun myOrders(): ApiResult<List<Order>> = safeApiCall { ordersApi.myDeliveryOrders() }

    suspend fun earnings(): ApiResult<DeliveryEarningsResponse> = safeApiCall { ordersApi.deliveryEarnings() }

    suspend fun payoutBatches(): ApiResult<List<DeliveryPayoutBatch>> = safeApiCall { authApi.myDeliveryPayouts() }

    suspend fun bank(): ApiResult<BankDetails> = safeApiCall { parseBank(authApi.myBank()) }

    suspend fun updateBank(input: BankDetailsInput): ApiResult<BankDetails> =
        safeApiCall { parseBank(authApi.updateBank(input)) }

    private fun parseBank(obj: JsonObject): BankDetails {
        val bank = obj["bank"] ?: return BankDetails()
        if (bank is JsonNull) return BankDetails()
        return json.decodeFromJsonElement(BankDetails.serializer(), bank)
    }
}
