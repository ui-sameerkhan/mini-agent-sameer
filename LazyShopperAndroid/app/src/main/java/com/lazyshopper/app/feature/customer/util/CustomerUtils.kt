package com.lazyshopper.app.feature.customer.util

import com.lazyshopper.app.BuildConfig
import kotlin.math.roundToInt

/** Builds a fully-qualified URL for a server-hosted file (`GET /api/files/{fid}`), or null if no id. */
fun fileUrl(id: String?): String? {
    if (id.isNullOrBlank()) return null
    val base = BuildConfig.API_BASE_URL.trimEnd('/')
    return "$base/api/files/$id"
}

/** Standard money formatting used across the customer UI: "₹123.45". */
fun formatMoney(amount: Double?): String = "₹%.2f".format(amount ?: 0.0)

/** Formats a quantity nicely — whole numbers without decimals, fractional otherwise. */
fun formatQty(qty: Double): String =
    if (qty == qty.roundToInt().toDouble()) qty.roundToInt().toString() else "%.2f".format(qty)
