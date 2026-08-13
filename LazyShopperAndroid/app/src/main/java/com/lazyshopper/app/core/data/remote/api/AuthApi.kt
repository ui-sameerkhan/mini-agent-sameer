package com.lazyshopper.app.core.data.remote.api

import com.lazyshopper.app.core.data.remote.dto.*
import kotlinx.serialization.json.JsonObject
import okhttp3.MultipartBody
import retrofit2.http.*

interface AuthApi {
    @POST("api/auth/register")
    suspend fun register(@Body body: RegisterInput): AuthResponse

    @POST("api/auth/login")
    suspend fun login(@Body body: LoginInput): AuthResponse

    @POST("api/auth/logout")
    suspend fun logout(): MessageResponse

    @POST("api/auth/forgot-password")
    suspend fun forgotPassword(@Body body: ForgotPasswordInput): MessageResponse

    @POST("api/auth/reset-password")
    suspend fun resetPassword(@Body body: ResetPasswordInput): MessageResponse

    @GET("api/auth/me")
    suspend fun me(): UserResponse

    @POST("api/auth/otp/request")
    suspend fun requestOtp(@Body body: OtpRequestInput): DevOtpResponse

    @POST("api/auth/otp/verify")
    suspend fun verifyOtp(@Body body: OtpVerifyInput): AuthResponse

    @POST("api/auth/firebase-phone")
    suspend fun firebasePhone(@Body body: FirebasePhoneInput): AuthResponse

    @PUT("api/me/profile")
    suspend fun updateProfile(@Body body: ProfileInput): UserResponse

    @PUT("api/me/location")
    suspend fun updateLocationPref(@Body body: LocationPrefInput): JsonObject

    @GET("api/me/delivery/payouts")
    suspend fun myDeliveryPayouts(): List<DeliveryPayoutBatch>

    @GET("api/me/bank")
    suspend fun myBank(): JsonObject

    @PUT("api/me/bank")
    suspend fun updateBank(@Body body: BankDetailsInput): JsonObject

    @Multipart
    @POST("api/kyc/upload")
    suspend fun kycUpload(@Part file: MultipartBody.Part): ImageIdResponse

    @POST("api/kyc/submit")
    suspend fun kycSubmit(@Body body: KycSubmitInput): MessageResponse

    @GET("api/kyc/me")
    suspend fun myKyc(): JsonObject

    @GET("api/admin/kyc")
    suspend fun adminKycList(@Query("status") status: String? = null): JsonObject

    @POST("api/admin/kyc/{userId}/review")
    suspend fun adminKycReview(@Path("userId") userId: String, @Body body: KycReviewInput): MessageResponse

    @GET("api/me/addresses")
    suspend fun myAddresses(): List<Address>

    @POST("api/me/addresses")
    suspend fun addAddress(@Body body: AddressInput): Address

    @DELETE("api/me/addresses/{aid}")
    suspend fun deleteAddress(@Path("aid") id: String): MessageResponse

    @PUT("api/me/addresses/{aid}")
    suspend fun updateAddress(@Path("aid") id: String, @Body body: AddressInput): Address

    @PUT("api/me/addresses/{aid}/default")
    suspend fun setDefaultAddress(@Path("aid") id: String): MessageResponse
}
