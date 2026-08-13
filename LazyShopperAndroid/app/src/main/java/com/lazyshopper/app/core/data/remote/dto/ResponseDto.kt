package com.lazyshopper.app.core.data.remote.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable data class MessageResponse(val message: String? = null, val id: String? = null)
@Serializable data class OkResponse(val ok: Boolean = true)
@Serializable data class AuthResponse(val user: User, val token: String, val is_new: Boolean? = null)
@Serializable data class UserResponse(val user: User)
@Serializable data class ImageIdResponse(val image_id: String)
@Serializable data class VideoIdResponse(val video_id: String)
@Serializable data class DevOtpResponse(val message: String, val dev_otp: String)

@Serializable
data class HomeResponse(
    val offers: List<Coupon> = emptyList(),
    val best_selling: List<Product> = emptyList(),
    val nearby_stores: List<Shop> = emptyList(),
    val promoted_shops: List<Shop> = emptyList(),
    val featured_shops: List<Shop> = emptyList(),
    val products: List<Product> = emptyList(),
    val sweets: List<Product> = emptyList(),
)

@Serializable
data class DistrictAreas(val district: String, val areas: List<String> = emptyList())

@Serializable
data class StateLocations(val state: String, val districts: List<DistrictAreas> = emptyList())

@Serializable
data class ReviewsResponse(val reviews: List<Review> = emptyList(), val average: Double = 0.0, val count: Int = 0)

@Serializable
data class ReviewEligibleEntry(val target_id: String, val target_type: String, val rating: Int)

@Serializable
data class ReviewsEligibleResponse(
    val product_ids: List<String> = emptyList(),
    val shop_ids: List<String> = emptyList(),
    val partner_ids: List<String> = emptyList(),
    val my_reviews: List<ReviewEligibleEntry> = emptyList(),
)

@Serializable data class UnreadCountResponse(val count: Int)

@Serializable data class CouponValidateResponse(val code: String, val discount: Double)

@Serializable
data class CartRecoverResponse(val items: List<JsonElement> = emptyList(), val coupon_code: String? = null)

@Serializable
data class RazorpayOrderBlock(val order_id: String, val amount: Long, val currency: String = "INR", val key_id: String)

@Serializable
data class CheckoutResponse(
    val order: Order,
    val checkout_url: String? = null,
    val razorpay: RazorpayOrderBlock? = null,
)

@Serializable
data class QuoteResponse(
    val subtotal: Double = 0.0,
    val platform_fee: Double = 0.0,
    val delivery_fee: Double = 0.0,
    val distance_km: Double = 0.0,
    val free_delivery: Boolean = false,
    val free_delivery_reason: String? = null,
    val free_delivery_min: Double = 0.0,
    val referral_discount: Double = 0.0,
    val referral_valid: Boolean = false,
    val referral_reason: String? = null,
    val wallet_available: Double = 0.0,
    val wallet_used: Double = 0.0,
    val discount: Double = 0.0,
    val total: Double = 0.0,
)

@Serializable data class RazorpayVerifyResponse(val status: String, val order_id: String? = null)

@Serializable
data class PromoteCheckoutResponse(
    val razorpay: RazorpayOrderBlock,
    val amount: Int,
    val days: Int,
    val shop_name: String,
)

@Serializable data class PromoteVerifyResponse(val status: String, val promoted_until: String, val days: Int)

@Serializable
data class DeliverResponse(val message: String, val earning: Double = 0.0)

@Serializable
data class TrackingPerson(
    val lat: Double? = null,
    val lng: Double? = null,
    val name: String? = null,
    val phone: String? = null,
    val updated_at: String? = null,
)

@Serializable
data class TrackingResponse(
    val status: String,
    val delivery_status: String? = null,
    val customer: TrackingPerson? = null,
    val partner: TrackingPerson? = null,
)

@Serializable
data class EarningsSeriesPoint(val label: String, val earning: Double)

@Serializable
data class ShopkeeperEarningsResponse(
    val sales: Double = 0.0,
    val commission: Double = 0.0,
    val earning: Double = 0.0,
    val pending: Double = 0.0,
    val paid: Double = 0.0,
    val orders: Int = 0,
    val history: List<Settlement> = emptyList(),
    val series: List<EarningsSeriesPoint> = emptyList(),
    val series_weekly: List<EarningsSeriesPoint> = emptyList(),
)

@Serializable
data class DeliveryEarningsResponse(
    val total_earnings: Double = 0.0,
    val total_deliveries: Int = 0,
    val fee_per_delivery: Double = 0.0,
    val orders: List<Order> = emptyList(),
    val avg_rating: Double = 0.0,
    val review_count: Int = 0,
)

@Serializable
data class DeliveryPayoutBatch(
    val batch_id: String,
    val partner_name: String? = null,
    val orders: Int = 0,
    val amount: Double = 0.0,
    val settled_at: String? = null,
    val payout_status: String? = null,
)

@Serializable
data class ProductHistoryResponse(val count: Int = 0, val last_ordered_at: String? = null)

// ---------- Admin ----------

@Serializable
data class DeliveryPartnerSummary(
    val id: String,
    val name: String? = null,
    val email: String? = null,
    val status: String? = null,
    val available: Boolean = false,
    val active_orders: Int = 0,
    val delivered_orders: Int = 0,
    val avg_rating: Double = 0.0,
    val review_count: Int = 0,
)

@Serializable
data class AdminDeliveryPartnerEarning(
    val id: String,
    val name: String? = null,
    val email: String? = null,
    val delivered: Int = 0,
    val earnings: Double = 0.0,
    val pending: Double = 0.0,
    val paid: Double = 0.0,
    val has_bank: Boolean = false,
    val status: String? = null,
    val available: Boolean = false,
)

@Serializable
data class AdminDeliveryEarningsResponse(
    val fee: Double = 0.0,
    val partners: List<AdminDeliveryPartnerEarning> = emptyList(),
    val total_earnings: Double = 0.0,
    val total_pending: Double = 0.0,
    val total_paid: Double = 0.0,
)

@Serializable
data class SettleResponse(val settled_amount: Double = 0.0, val orders: Int = 0, val payout_status: String? = null)

@Serializable
data class DeliverySettlementBatch(
    val batch_id: String,
    val partner_id: String? = null,
    val partner_name: String? = null,
    val partner_email: String? = null,
    val orders: Int = 0,
    val amount: Double = 0.0,
    val settled_at: String? = null,
    val settled_by: String? = null,
    val payout_id: String? = null,
    val payout_status: String? = null,
)

@Serializable
data class PromotionRecord(
    val razorpay_order_id: String? = null,
    val shop_id: String? = null,
    val shopkeeper_id: String? = null,
    val days: Int = 0,
    val amount: Double = 0.0,
    val status: String? = null,
    val created_at: String? = null,
    val shop_name: String? = null,
    val owner_name: String? = null,
)

@Serializable
data class PromotionsResponse(val promotions: List<PromotionRecord> = emptyList(), val count: Int = 0, val revenue: Double = 0.0)

@Serializable
data class AdminStats(
    val total_products: Int = 0,
    val total_orders: Int = 0,
    val total_users: Int = 0,
    val total_customers: Int = 0,
    val total_shopkeepers: Int = 0,
    val revenue: Double = 0.0,
    val total_shops: Int = 0,
    val pending_shops: Int = 0,
    val pending_products: Int = 0,
    val pending_shopkeepers: Int = 0,
    val total_delivery: Int = 0,
    val pending_delivery: Int = 0,
)

@Serializable
data class AdminOrderSummary(
    val id: String,
    val customer_name: String? = null,
    val total: Double = 0.0,
    val payment_method: String? = null,
    val payment_status: String? = null,
    val created_at: String? = null,
)

@Serializable
data class AdminPaymentsResponse(
    val transactions: List<PaymentTransaction> = emptyList(),
    val online_paid: Double = 0.0,
    val cod_total: Double = 0.0,
    val orders: List<AdminOrderSummary> = emptyList(),
)

@Serializable
data class AdminLogEntry(
    val id: String,
    val admin_email: String? = null,
    val action: String? = null,
    val detail: String? = null,
    val created_at: String? = null,
)

@Serializable
data class OrderDistributionResponse(
    val total: Double = 0.0,
    val shopkeeper_amount: Double = 0.0,
    val commission: Double = 0.0,
    val commission_pct: Double = 0.0,
    val rider_payout: Double = 0.0,
    val distance_fee: Double = 0.0,
    val weight_incentive: Double = 0.0,
    val distance_km: Double = 0.0,
    val weight_kg: Double = 0.0,
)

@Serializable
data class EmailChannels(val email: Boolean = true, val sms: Boolean = false, val whatsapp: Boolean = false)

@Serializable
data class EmailStatuses(
    val placed: Boolean = true,
    val packed: Boolean = true,
    val out_for_delivery: Boolean = true,
    val delivered: Boolean = true,
    val cancelled: Boolean = true,
)

@Serializable
data class EmailSettingsResponse(
    val key: String = "email",
    val enabled: Boolean = true,
    val statuses: EmailStatuses = EmailStatuses(),
    val channels: EmailChannels = EmailChannels(),
)

@Serializable
data class ProfitSummaryResponse(
    val month: String,
    val orders: Int = 0,
    val gmv: Double = 0.0,
    val commission: Double = 0.0,
    val shopkeeper_payouts: Double = 0.0,
    val rider_payouts: Double = 0.0,
    val delivery_collected: Double = 0.0,
    val free_delivery_cost: Double = 0.0,
    val referral_discount_cost: Double = 0.0,
    val referral_reward_cost: Double = 0.0,
    val referral_cost: Double = 0.0,
    val revenue: Double = 0.0,
    val costs: Double = 0.0,
    val net_profit: Double = 0.0,
)

@Serializable
data class WeightSlab(val upto: Double? = null, val fee: Double = 0.0)

@Serializable
data class RiderPayoutSettings(
    val key: String = "rider_payout",
    val base_fee: Double = 20.0,
    val base_km: Double = 2.0,
    val per_km: Double = 5.0,
    val free_delivery_min: Double = 499.0,
    val first_order_free: Boolean = true,
    val weight_slabs: List<WeightSlab> = emptyList(),
)

@Serializable
data class CommissionSettings(
    val key: String = "commission",
    val global_default: Double = 10.0,
    val min_payout: Double = 0.0,
    val categories: Map<String, Double> = emptyMap(),
    val shops: Map<String, Double> = emptyMap(),
    val settlement_cycle_days: Int = 7,
)

@Serializable
data class CategoryBreakdown(val category: String, val sales: Double = 0.0, val commission: Double = 0.0, val payable: Double = 0.0)

@Serializable
data class ShopBreakdown(val shop_id: String, val name: String? = null, val sales: Double = 0.0, val commission: Double = 0.0, val payable: Double = 0.0)

@Serializable
data class ShopOption(val id: String, val name: String? = null)

@Serializable
data class CommissionBreakdownResponse(
    val total_order_value: Double = 0.0,
    val total_commission: Double = 0.0,
    val total_shopkeeper_payable: Double = 0.0,
    val pending_payout: Double = 0.0,
    val completed_payout: Double = 0.0,
    val cancelled_refunded: Double = 0.0,
    val categories: List<CategoryBreakdown> = emptyList(),
    val shops: List<ShopBreakdown> = emptyList(),
    val shop_options: List<ShopOption> = emptyList(),
)

@Serializable
data class PayoutShopkeeperRow(
    val shopkeeper_id: String,
    val name: String? = null,
    val email: String? = null,
    val shop_name: String? = null,
    val settlement_status: String? = null,
    val sales: Double = 0.0,
    val platform_profit: Double = 0.0,
    val earning: Double = 0.0,
    val pending: Double = 0.0,
    val paid: Double = 0.0,
    val eligible: Boolean = false,
    val scheduled_pending: Boolean = false,
    val next_settlement_date: String? = null,
    val scheduled_date: String? = null,
    val orders: Int = 0,
)

@Serializable
data class PayoutsResponse(
    val total_sales: Double = 0.0,
    val total_profit: Double = 0.0,
    val total_earning: Double = 0.0,
    val total_pending: Double = 0.0,
    val total_paid: Double = 0.0,
    val settlement_cycle_days: Int = 7,
    val shopkeepers: List<PayoutShopkeeperRow> = emptyList(),
)

@Serializable
data class ScheduleResponse(val message: String, val scheduled_date: String)

@Serializable
data class SettleAllResponse(val settled_amount: Double = 0.0, val orders: Int = 0, val shopkeepers: Int = 0)

@Serializable
data class PayoutOrderDetail(
    val order_id: String,
    val delivered_at: String? = null,
    val auto_settlement_date: String? = null,
    val sales: Double = 0.0,
    val commission_pct: Double? = null,
    val commission_amount: Double = 0.0,
    val earning: Double = 0.0,
    val status: String? = null,
    val payout_id: String? = null,
    val actual_settlement_date: String? = null,
)

@Serializable
data class PayoutOrdersResponse(val orders: List<PayoutOrderDetail> = emptyList())

// ---------- Analytics ----------

@Serializable
data class AnalyticsSeriesPoint(val date: String, val orders: Int = 0, val revenue: Double = 0.0)

@Serializable
data class TopProductStat(val name: String, val revenue: Double = 0.0, val qty: Double = 0.0)

@Serializable
data class TopStoreStat(val name: String, val revenue: Double = 0.0, val orders: Int = 0)

@Serializable
data class TopBannerStat(val name: String, val impressions: Int = 0, val clicks: Int = 0, val ctr: Int = 0)

@Serializable
data class AdminAnalyticsResponse(
    val series: List<AnalyticsSeriesPoint> = emptyList(),
    val top_products: List<TopProductStat> = emptyList(),
    val top_stores: List<TopStoreStat> = emptyList(),
    val top_banners: List<TopBannerStat> = emptyList(),
    val status_counts: Map<String, Int> = emptyMap(),
    val active_users: Int = 0,
    val live_offers: Int = 0,
    val total_revenue: Double = 0.0,
    val total_orders: Int = 0,
)

@Serializable
data class StockReportProduct(
    val id: String,
    val name: String,
    val shop_name: String? = null,
    val category: String? = null,
    val brand: String? = null,
    val unit_type: String? = null,
    val stock: Double = 0.0,
    val threshold: Double = 0.0,
    val image_id: String? = null,
    val out: Boolean = false,
)

@Serializable
data class StockReportResponse(
    val threshold_default: Double = 0.0,
    val count: Int = 0,
    val out_of_stock: Int = 0,
    val products: List<StockReportProduct> = emptyList(),
)

@Serializable
data class CouponAnalyticsEntry(
    val code: String,
    val uses: Int = 0,
    val total_discount: Double = 0.0,
    val revenue: Double = 0.0,
    val type: String? = null,
    val value: Double = 0.0,
    val active: Boolean = true,
    val auto_generated: Boolean? = null,
)

@Serializable
data class CouponAnalyticsResponse(
    val total_coupons: Int = 0,
    val total_uses: Int = 0,
    val total_discount: Double = 0.0,
    val revenue_from_coupons: Double = 0.0,
    val coupons: List<CouponAnalyticsEntry> = emptyList(),
)

// ---------- Chat ----------

@Serializable
data class OrderChatResponse(
    val messages: List<ChatMessage> = emptyList(),
    val rider_name: String? = null,
    val customer_name: String? = null,
)

@Serializable
data class SupportChatResponse(val messages: List<ChatMessage> = emptyList(), val support_email: String? = null)

@Serializable
data class UnreadResponse(
    val total: Int = 0,
    val support: Int = 0,
    val support_admin: Int = 0,
    val orders: Map<String, Int> = emptyMap(),
)

@Serializable
data class SupportThread(
    val customer_id: String,
    val customer_name: String? = null,
    val customer_email: String? = null,
    val last: String? = null,
    val last_at: String? = null,
    val unread: Int = 0,
)

@Serializable
data class AdminSupportMessagesResponse(val messages: List<ChatMessage> = emptyList())

// ---------- Referral ----------

@Serializable
data class MyReferralResponse(
    val code: String,
    val wallet: Double = 0.0,
    val enabled: Boolean = true,
    val discount_amount: Double = 0.0,
    val reward_amount: Double = 0.0,
    val credited_total: Double = 0.0,
    val pending_total: Double = 0.0,
    val referrals: List<Referral> = emptyList(),
    val share_text: String? = null,
)

@Serializable
data class ReferralSettings(
    val key: String = "referral",
    val enabled: Boolean = true,
    val discount_amount: Double = 20.0,
    val reward_amount: Double = 20.0,
    val min_order_value: Double = 0.0,
    val max_discount: Double = 20.0,
    val reward_limit_per_user: Int = 0,
)

@Serializable
data class AdminReferralsResponse(
    val referrals: List<Referral> = emptyList(),
    val counts: Map<String, Int> = emptyMap(),
    val total_rewarded: Double = 0.0,
)
