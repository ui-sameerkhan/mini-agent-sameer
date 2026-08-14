package com.lazyshopper.app.feature.shopkeeper.common

/** `"₹%.2f".format(amount)` per project convention — centralized so every screen matches. */
fun money(amount: Double?): String = "₹%.2f".format(amount ?: 0.0)

fun statusLabel(status: String?): String = when (status) {
    "placed" -> "Placed"
    "accepted" -> "Accepted"
    "preparing" -> "Preparing"
    "ready" -> "Ready"
    "packed" -> "Packed"
    "out_for_delivery" -> "Out for delivery"
    "delivered" -> "Delivered"
    "cancelled" -> "Cancelled"
    "pending" -> "Pending"
    "approved" -> "Approved"
    "rejected" -> "Rejected"
    "submitted" -> "Under review"
    null -> "—"
    else -> status.replaceFirstChar { it.uppercase() }
}
