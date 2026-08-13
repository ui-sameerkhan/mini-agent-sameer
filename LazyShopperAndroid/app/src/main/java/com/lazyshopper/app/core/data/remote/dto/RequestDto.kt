package com.lazyshopper.app.core.data.remote.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

// Request bodies. Field names are kept snake_case to match the wire format 1:1 with
// backend/app_core.py Pydantic models (see api_reference.md section 2) — avoids needing
// @SerialName boilerplate on every property across ~30 request classes.

@Serializable
data class RegisterInput(
    val name: String,
    val email: String,
    val password: String,
    val role: String = "customer",
    val shop_name: String? = null,
    val phone: String? = null,
)

@Serializable
data class LoginInput(val email: String, val password: String)

@Serializable
data class ForgotPasswordInput(val email: String)

@Serializable
data class ResetPasswordInput(val token: String, val new_password: String)

@Serializable
data class ProductInput(
    val name: String,
    val shop_id: String,
    val category: String = "vegetables",
    val unit_type: String,
    val price: Double,
    val mrp: Double? = null,
    val stock: Double = 100.0,
    val subcategory: String? = null,
    val image_id: String? = null,
    val promo_video_id: String? = null,
    val description: String? = "",
    val brand: String? = null,
    val featured: Boolean = false,
    val in_stock: Boolean = true,
    val low_stock_threshold: Double? = null,
    val commission_pct: Double? = null,
)

@Serializable
data class ShopInput(
    val name: String,
    val state: String,
    val district: String,
    val area: String,
    val category: String,
)

@Serializable
data class CartItemDto(val product_id: String, val qty: Double)

@Serializable
data class RazorpayVerifyInput(
    val razorpay_order_id: String,
    val razorpay_payment_id: String,
    val razorpay_signature: String,
)

@Serializable
data class BankDetailsInput(
    val holder: String? = null,
    val account_number: String? = null,
    val ifsc: String? = null,
    val vpa: String? = null,
)

@Serializable
data class FirebasePhoneInput(val id_token: String, val name: String? = null)

@Serializable
data class KycSubmitInput(
    val aadhaar_id: String? = null,
    val pan_id: String? = null,
    val selfie_id: String? = null,
    val dl_id: String? = null,
    val shop_photo_id: String? = null,
    val vehicle_number: String? = null,
    val gst_number: String? = null,
    val fssai_number: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val address_text: String? = null,
)

@Serializable
data class KycReviewInput(val status: String, val reason: String? = null)

@Serializable
data class CheckoutInput(
    val items: List<CartItemDto>,
    val payment_method: String,
    val address: String,
    val phone: String,
    val origin_url: String,
    val delivery_slot: String = "Morning",
    val coupon_code: String? = null,
    val customer_email: String? = null,
    val cust_lat: Double? = null,
    val cust_lng: Double? = null,
    val referral_code: String? = null,
    val use_wallet: Boolean? = false,
)

@Serializable
data class ReviewInput(
    val target_type: String,
    val target_id: String,
    val rating: Int,
    val comment: String? = "",
)

@Serializable
data class DeliverInput(val otp: String)

@Serializable
data class AvailabilityInput(val online: Boolean)

@Serializable
data class LocationInput(val lat: Double, val lng: Double)

@Serializable
data class GoogleAuthInput(val session_id: String)

@Serializable
data class OtpRequestInput(val phone: String)

@Serializable
data class OtpVerifyInput(val phone: String, val code: String, val name: String? = null)

@Serializable
data class ProfileInput(
    val name: String? = null,
    val email: String? = null,
    val password: String? = null,
    val phone: String? = null,
)

@Serializable
data class LocationPrefInput(
    val state: String,
    val district: String? = "",
    val city: String? = "",
    val area: String? = "",
    val pincode: String? = "",
)

@Serializable
data class CategoryInput(val key: String, val label: String, val active: Boolean = true)

@Serializable
data class CouponInput(
    val code: String,
    val type: String,
    val value: Double,
    val min_order: Double = 0.0,
    val active: Boolean = true,
)

@Serializable
data class OfferInput(
    val title: String,
    val offer_type: String,
    val target_id: String? = null,
    val discount_type: String = "percent",
    val value: Double,
    val min_order: Double = 0.0,
    val starts_at: String? = null,
    val ends_at: String? = null,
    val show_badge: Boolean = true,
    val active: Boolean = true,
    val image_id: String? = null,
    val image_url: String? = null,
)

@Serializable
data class BannerInput(
    val title: String,
    val image_id: String? = null,
    val image_url: String? = null,
    val subtitle: String? = "",
    val link: String? = null,
    val banner_type: String = "home",
    val starts_at: String? = null,
    val ends_at: String? = null,
    val sort_order: Int = 0,
    val active: Boolean = true,
)

@Serializable
data class DeliveryPartnerInput(
    val name: String,
    val email: String,
    val phone: String? = null,
    val password: String,
)

@Serializable
data class NotificationInput(
    val title: String,
    val body: String,
    val audience: String = "all",
    val type: String = "general",
    val link: String? = null,
)

@Serializable
data class CartSyncInput(val items: List<JsonElement> = emptyList(), val total: Double = 0.0)

@Serializable
data class AddressInput(
    val label: String = "Home",
    val address: String,
    val phone: String,
    val is_default: Boolean = false,
)

@Serializable
data class ChatSend(val text: String)

@Serializable
data class CouponValidateBody(val code: String, val amount: Double)
