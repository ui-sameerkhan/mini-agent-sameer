package com.lazyshopper.app.core.navigation

/** Central route table. Each role has its own nested graph, entered after auth resolves the user's role. */
object Routes {
    // Root / auth
    const val SPLASH = "splash"
    const val AUTH = "auth"
    const val FORGOT_PASSWORD = "forgot_password"
    const val RESET_PASSWORD = "reset_password/{token}"
    const val LOCATION_SELECT = "location_select"
    const val KYC_FORM = "kyc_form"

    // Customer graph
    const val CUSTOMER_GRAPH = "customer_graph"
    const val STOREFRONT = "storefront"
    const val CATEGORIES = "categories"
    const val CATEGORY_SHOPS = "category_shops/{categoryKey}"
    const val SEARCH = "search"
    const val SHOP_PAGE = "shop/{shopId}"
    const val CART = "cart"
    const val ORDERS = "orders"
    const val ORDER_TRACKING = "orders/{orderId}/track"
    const val ADDRESS_BOOK = "address_book"
    const val REFER_EARN = "refer_earn"
    const val ACCOUNT = "account"
    const val PAYMENT_RESULT = "payment_result/{orderId}/{status}"
    const val ORDER_CHAT = "chat/order/{orderId}"
    const val SUPPORT_CHAT = "chat/support"

    // Shopkeeper graph
    const val SHOPKEEPER_GRAPH = "shopkeeper_graph"
    const val SHOPKEEPER_DASHBOARD = "shopkeeper_dashboard"
    const val SHOPKEEPER_PRODUCTS = "shopkeeper_products"
    const val SHOPKEEPER_PRODUCT_FORM = "shopkeeper_product_form?productId={productId}"
    const val SHOPKEEPER_SHOPS = "shopkeeper_shops"
    const val SHOPKEEPER_SHOP_FORM = "shopkeeper_shop_form?shopId={shopId}"
    const val SHOPKEEPER_ORDERS = "shopkeeper_orders"
    const val SHOPKEEPER_EARNINGS = "shopkeeper_earnings"

    // Delivery graph
    const val DELIVERY_GRAPH = "delivery_graph"
    const val DELIVERY_DASHBOARD = "delivery_dashboard"
    const val DELIVERY_EARNINGS = "delivery_earnings"

    // Admin graph
    const val ADMIN_GRAPH = "admin_graph"
    const val ADMIN_DASHBOARD = "admin_dashboard"
    const val ADMIN_PRODUCTS = "admin_products"
    const val ADMIN_OFFERS = "admin_offers"
    const val ADMIN_BANNERS = "admin_banners"
    const val ADMIN_ORDERS = "admin_orders"
    const val ADMIN_DELIVERY = "admin_delivery"
    const val ADMIN_USERS = "admin_users"
    const val ADMIN_SHOPS = "admin_shops"
    const val ADMIN_KYC = "admin_kyc"
    const val ADMIN_COMMISSION = "admin_commission"
    const val ADMIN_PAYOUTS = "admin_payouts"
    const val ADMIN_PROMOTIONS = "admin_promotions"
    const val ADMIN_REFERRALS = "admin_referrals"
    const val ADMIN_SUPPORT = "admin_support"
    const val ADMIN_ANALYTICS = "admin_analytics"

    fun categoryShops(key: String) = "category_shops/$key"
    fun shopPage(shopId: String) = "shop/$shopId"
    fun orderTracking(orderId: String) = "orders/$orderId/track"
    fun orderChat(orderId: String) = "chat/order/$orderId"
    fun resetPassword(token: String) = "reset_password/$token"
    fun paymentResult(orderId: String, status: String) = "payment_result/$orderId/$status"
    fun shopkeeperProductForm(productId: String? = null) =
        "shopkeeper_product_form" + if (productId != null) "?productId=$productId" else ""
    fun shopkeeperShopForm(shopId: String? = null) =
        "shopkeeper_shop_form" + if (shopId != null) "?shopId=$shopId" else ""
}
