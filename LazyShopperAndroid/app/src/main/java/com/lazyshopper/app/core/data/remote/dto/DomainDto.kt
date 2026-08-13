package com.lazyshopper.app.core.data.remote.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ProductOffer(
    val title: String,
    val discount_type: String,
    val value: Double,
    val final_price: Double,
    val show_badge: Boolean,
    val pct: Int,
)

@Serializable
data class Product(
    val id: String,
    val name: String,
    val shop_id: String,
    val category: String,
    val unit_type: String,
    val price: Double,
    val mrp: Double? = null,
    val stock: Double = 0.0,
    val subcategory: String? = null,
    val image_id: String? = null,
    val promo_video_id: String? = null,
    val description: String? = null,
    val brand: String? = null,
    val featured: Boolean = false,
    val in_stock: Boolean = true,
    val low_stock_threshold: Double? = null,
    val commission_pct: Double? = null,
    val shop_name: String? = null,
    val area: String? = null,
    val district: String? = null,
    val state: String? = null,
    val owner_id: String? = null,
    val owner_name: String? = null,
    val status: String? = null,
    val created_at: String? = null,
    val avg_rating: Double? = null,
    val review_count: Int? = null,
    val offer: ProductOffer? = null,
    val order_count: Int? = null,
    val last_qty: Double? = null,
)

@Serializable
data class Shop(
    val id: String,
    val name: String,
    val state: String,
    val district: String,
    val area: String,
    val category: String,
    val owner_id: String? = null,
    val owner_name: String? = null,
    val status: String? = null,
    val created_at: String? = null,
    val open_time: String? = null,
    val close_time: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val promoted: Boolean? = null,
    val promoted_until: String? = null,
    val promoted_at: String? = null,
    val avg_rating: Double? = null,
    val review_count: Int? = null,
    val product_count: Int? = null,
)

@Serializable
data class OrderItem(
    val product_id: String,
    val name: String,
    val unit_type: String,
    val price: Double,
    val qty: Double,
    val line_total: Double,
    val shop_id: String? = null,
    val owner_name: String? = null,
    val owner_id: String? = null,
    val category: String? = null,
    val commission_pct: Double = 0.0,
    val commission_amount: Double = 0.0,
    val shopkeeper_amount: Double = 0.0,
    val platform_amount: Double = 0.0,
    val settlement_status: String? = null,
)

@Serializable
data class Order(
    val id: String,
    val customer_id: String? = null,
    val customer_name: String? = null,
    val customer_email: String? = null,
    val items: List<OrderItem> = emptyList(),
    val subtotal: Double = 0.0,
    val platform_fee: Double = 0.0,
    val delivery_fee: Double = 0.0,
    val total: Double = 0.0,
    val delivery_slot: String? = null,
    val address: String? = null,
    val phone: String? = null,
    val payment_method: String? = null,
    val payment_status: String? = null,
    val status: String = "placed",
    val delivery_otp: String? = null,
    val delivery_partner_id: String? = null,
    val delivery_partner_name: String? = null,
    val delivery_status: String? = null,
    val cust_lat: Double? = null,
    val cust_lng: Double? = null,
    val delivery_distance_km: Double = 0.0,
    val total_weight_kg: Double = 0.0,
    val rider_payout: Double = 0.0,
    val free_delivery: Boolean = false,
    val free_delivery_reason: String? = null,
    val referral_code: String? = null,
    val referral_discount: Double = 0.0,
    val referrer_id: String? = null,
    val referral_valid: Boolean = false,
    val referral_reason: String? = null,
    val wallet_available: Double = 0.0,
    val wallet_used: Double = 0.0,
    val created_at: String? = null,
    val discount: Double = 0.0,
    val coupon_code: String? = null,
    val razorpay_order_id: String? = null,
    val razorpay_payment_id: String? = null,
    val session_id: String? = null,
    val delivered_at: String? = null,
    val assigned_at: String? = null,
    val auto_assigned: Boolean? = null,
    val wallet_refunded: Boolean? = null,
    val refund_status: String? = null,
    val refunded_at: String? = null,
    val refund_amount: Double? = null,
)

@Serializable
data class Review(
    val id: String,
    val target_type: String,
    val target_id: String,
    val user_id: String? = null,
    val user_name: String? = null,
    val rating: Int,
    val comment: String? = "",
    val hidden: Boolean = false,
    val created_at: String? = null,
    val video_id: String? = null,
    val has_video: Boolean? = null,
    val video_status: String? = null,
)

@Serializable
data class NotificationDto(
    val id: String,
    val title: String,
    val body: String,
    val audience: String = "all",
    val type: String = "general",
    val link: String? = null,
    val created_at: String? = null,
    val read: Boolean = false,
    val read_count: Int? = null,
)

@Serializable
data class Offer(
    val id: String,
    val title: String,
    val offer_type: String,
    val target_id: String? = null,
    val discount_type: String = "percent",
    val value: Double = 0.0,
    val min_order: Double = 0.0,
    val starts_at: String? = null,
    val ends_at: String? = null,
    val show_badge: Boolean = true,
    val active: Boolean = true,
    val image_id: String? = null,
    val image_url: String? = null,
    val created_at: String? = null,
    val state: String? = null,
)

@Serializable
data class Banner(
    val id: String,
    val title: String,
    val image_id: String? = null,
    val image_url: String? = null,
    val subtitle: String? = null,
    val link: String? = null,
    val banner_type: String = "home",
    val starts_at: String? = null,
    val ends_at: String? = null,
    val sort_order: Int = 0,
    val active: Boolean = true,
    val created_at: String? = null,
    val clicks: Int? = null,
    val impressions: Int? = null,
    val state: String? = null,
)

@Serializable
data class Coupon(
    val id: String,
    val code: String,
    val type: String,
    val value: Double,
    val min_order: Double = 0.0,
    val active: Boolean = true,
    val auto_generated: Boolean? = null,
)

@Serializable
data class Category(val id: String, val key: String, val label: String, val active: Boolean = true)

@Serializable
data class KycRecord(
    val user_id: String,
    val role: String,
    val status: String,
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
    val submitted_at: String? = null,
    val reason: String? = null,
    val reviewed_at: String? = null,
    val user_name: String? = null,
    val user_email: String? = null,
    val user_phone: String? = null,
)

@Serializable
data class Address(
    val id: String,
    val label: String = "Home",
    val address: String,
    val phone: String,
    val is_default: Boolean = false,
)

@Serializable
data class LocationPref(
    val state: String,
    val district: String? = "",
    val city: String? = "",
    val area: String? = "",
    val pincode: String? = "",
)

@Serializable
data class User(
    val id: String,
    val name: String? = null,
    val email: String? = null,
    val role: String = "customer",
    val shop_name: String? = null,
    val status: String = "active",
    val available: Boolean = false,
    val location_pref: LocationPref? = null,
    val phone: String? = null,
    val picture: String? = null,
    val referral_code: String? = null,
    val referral_wallet: Double = 0.0,
    val created_at: String? = null,
)

@Serializable
data class BankDetails(
    val holder: String? = null,
    val account_number: String? = null,
    val ifsc: String? = null,
    val vpa: String? = null,
)

@Serializable
data class ChatMessage(
    val id: String,
    val thread_type: String,
    val order_id: String? = null,
    val sender_role: String,
    val sender_id: String? = null,
    val sender_name: String? = null,
    val text: String,
    val created_at: String? = null,
    val customer_id: String? = null,
    val customer_name: String? = null,
    val customer_email: String? = null,
    val read_by_admin: Boolean? = null,
)

@Serializable
data class Referral(
    val id: String,
    val referrer_id: String,
    val referred_id: String? = null,
    val referred_name: String? = null,
    val code: String? = null,
    val order_id: String? = null,
    val discount_amount: Double = 0.0,
    val reward_amount: Double = 0.0,
    val status: String = "order_placed",
    val flags: List<String> = emptyList(),
    val reason: String? = null,
    val created_at: String? = null,
    val updated_at: String? = null,
    val credited_at: String? = null,
    val referrer_name: String? = null,
    val referrer_phone: String? = null,
)

@Serializable
data class Settlement(
    val id: String,
    val payout_id: String? = null,
    val owner_id: String? = null,
    val order_id: String? = null,
    val amount: Double = 0.0,
    val platform_amount: Double = 0.0,
    val settled_at: String? = null,
    val settled_by: String? = null,
    val payout_status: String? = null,
    val shopkeeper_name: String? = null,
    val shop_name: String? = null,
)

@Serializable
data class PaymentTransaction(
    val razorpay_order_id: String? = null,
    val session_id: String? = null,
    val order_id: String? = null,
    val user_id: String? = null,
    val amount: Double = 0.0,
    val currency: String? = null,
    val status: String? = null,
    val payment_status: String? = null,
    val created_at: String? = null,
    val razorpay_payment_id: String? = null,
)

/** Generic passthrough for server endpoints with no fixed request/response schema (raw dict bodies). */
typealias RawJson = Map<String, JsonElement>
