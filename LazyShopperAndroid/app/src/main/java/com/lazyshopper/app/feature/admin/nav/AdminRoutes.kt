package com.lazyshopper.app.feature.admin.nav

/**
 * Internal route table for the admin drawer shell. These are local to the admin nav graph
 * (entered once via the shared Routes.ADMIN_GRAPH) — they don't need to match core/navigation/Routes.kt
 * 1:1 since nothing outside feature/admin/ navigates into these directly.
 */
object AdminRoutes {
    const val DASHBOARD = "admin/dashboard"

    const val PRODUCTS = "admin/products"
    const val CATEGORIES = "admin/categories"
    const val COUPONS = "admin/coupons"
    const val OFFERS = "admin/offers"
    const val BANNERS = "admin/banners"
    const val REVIEWS = "admin/reviews"
    const val VIDEO_REVIEWS = "admin/video_reviews"

    const val ORDERS = "admin/orders"
    const val DELIVERY_PARTNERS = "admin/delivery_partners"
    const val KYC = "admin/kyc"

    const val USERS = "admin/users"
    const val SHOPS = "admin/shops"

    const val COMMISSION = "admin/commission"
    const val RIDER_PAYOUT = "admin/rider_payout"
    const val PAYOUTS = "admin/payouts"
    const val PROFIT_SUMMARY = "admin/profit_summary"
    const val PROMOTIONS = "admin/promotions"
    const val REFERRALS = "admin/referrals"

    const val SUPPORT = "admin/support"
    const val SUPPORT_THREAD = "admin/support/{customerId}/{customerName}"
    fun supportThread(customerId: String, customerName: String) =
        "admin/support/$customerId/${android.net.Uri.encode(customerName.ifBlank { "Customer" })}"

    const val ANALYTICS = "admin/analytics"
    const val STOCK_REPORT = "admin/stock_report"
    const val COUPON_ANALYTICS = "admin/coupon_analytics"

    const val EMAIL_SETTINGS = "admin/email_settings"
    const val PAYMENTS = "admin/payments"
    const val LOGS = "admin/logs"
    const val NOTIFICATIONS = "admin/notifications"
}
