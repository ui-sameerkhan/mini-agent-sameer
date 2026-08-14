package com.lazyshopper.app.feature.shopkeeper.kyc

import com.lazyshopper.app.core.data.remote.ApiResult
import com.lazyshopper.app.core.data.remote.api.AuthApi
import com.lazyshopper.app.core.data.remote.dto.ImageIdResponse
import com.lazyshopper.app.core.data.remote.dto.KycRecord
import com.lazyshopper.app.core.data.remote.dto.KycSubmitInput
import com.lazyshopper.app.core.data.remote.dto.MessageResponse
import com.lazyshopper.app.core.data.remote.safeApiCall
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MultipartBody
import javax.inject.Inject
import javax.inject.Singleton

/** `{"kyc": KycRecord|null, "kyc_status": string|null}` from `GET /api/kyc/me`, parsed into a typed shape. */
data class KycMeResult(val kyc: KycRecord?, val kycStatus: String?)

/**
 * Shopkeeper KYC (Aadhaar/PAN/Selfie/Shop photo + optional GST/FSSAI + location pin). Scoped to
 * feature/shopkeeper/ — the delivery role has its own equivalent repository under
 * feature/delivery/kyc, deliberately not shared, since the two roles submit different document
 * sets to the same `/api/kyc/*` endpoints.
 */
@Singleton
class KycRepository @Inject constructor(
    private val authApi: AuthApi,
    private val json: Json,
) {
    suspend fun uploadDoc(part: MultipartBody.Part): ApiResult<ImageIdResponse> = safeApiCall { authApi.kycUpload(part) }

    suspend fun submit(input: KycSubmitInput): ApiResult<MessageResponse> = safeApiCall { authApi.kycSubmit(input) }

    suspend fun myKyc(): ApiResult<KycMeResult> = safeApiCall {
        val obj = authApi.myKyc()
        val kycElement = obj["kyc"]
        val kyc = if (kycElement == null || kycElement is JsonNull) {
            null
        } else {
            json.decodeFromJsonElement(KycRecord.serializer(), kycElement)
        }
        val status = obj["kyc_status"]?.let { if (it is JsonNull) null else it.jsonPrimitive.contentOrNull }
        KycMeResult(kyc, status)
    }
}
