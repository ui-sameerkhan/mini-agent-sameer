package com.lazyshopper.app.feature.shopkeeper.shops

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.razorpay.Checkout
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import org.json.JSONObject

/**
 * Thin, self-contained bridge to the Razorpay Android Checkout SDK for the shop-promotion flow
 * (`POST /api/shopkeeper/promote/checkout` -> this -> `POST /api/shopkeeper/promote/verify`).
 *
 * The Razorpay SDK requires the hosting Activity itself to implement its payment-result listener
 * interface (it casts the Activity passed to `Checkout.open()`), so this can't be driven purely
 * from a Composable/ViewModel — hence a dedicated tiny Activity, launched via
 * `ActivityResultContracts.StartActivityForResult()` from [ShopsScreen].
 *
 * Kept as a local, shopkeeper-owned copy (distinct class name) rather than reusing
 * `feature.customer.cart.RazorpayCheckoutActivity`, so this package has no compile-time
 * dependency on a parallel workstream's files.
 *
 * IMPORTANT — deployment note: this Activity must be declared in `AndroidManifest.xml` for the
 * launch to resolve at runtime — added as part of this workstream alongside the FileProvider entry
 * (see AndroidManifest.xml comment).
 */
class ShopPromoteCheckoutActivity : Activity(), PaymentResultWithDataListener {

    private var shopId: String? = null
    private var razorpayOrderId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        shopId = intent.getStringExtra(EXTRA_SHOP_ID)
        razorpayOrderId = intent.getStringExtra(EXTRA_RAZORPAY_ORDER_ID)
        val amountPaise = intent.getLongExtra(EXTRA_AMOUNT_PAISE, 0L)
        val keyId = intent.getStringExtra(EXTRA_KEY_ID).orEmpty()
        val shopName = intent.getStringExtra(EXTRA_SHOP_NAME).orEmpty()

        val checkout = Checkout()
        checkout.setKeyID(keyId)

        try {
            val options = JSONObject().apply {
                put("name", "Lazy Shopper")
                put("description", "Promote $shopName")
                put("currency", "INR")
                put("order_id", razorpayOrderId)
                put("amount", amountPaise)
                put("retry", JSONObject().apply { put("enabled", false) })
            }
            checkout.open(this, options)
        } catch (e: Exception) {
            finishWithError(e.message ?: "Could not open Razorpay checkout")
        }
    }

    override fun onPaymentSuccess(razorpayPaymentId: String?, paymentData: PaymentData?) {
        val result = Intent().apply {
            putExtra(EXTRA_SHOP_ID, shopId)
            putExtra(EXTRA_RAZORPAY_PAYMENT_ID, razorpayPaymentId ?: paymentData?.paymentId)
            putExtra(EXTRA_RAZORPAY_ORDER_ID, paymentData?.orderId ?: razorpayOrderId)
            putExtra(EXTRA_RAZORPAY_SIGNATURE, paymentData?.signature)
        }
        setResult(RESULT_OK, result)
        finish()
    }

    override fun onPaymentError(code: Int, response: String?, paymentData: PaymentData?) {
        finishWithError(response ?: "Payment failed (code $code)")
    }

    private fun finishWithError(message: String) {
        val result = Intent().apply {
            putExtra(EXTRA_SHOP_ID, shopId)
            putExtra(EXTRA_ERROR, message)
        }
        setResult(RESULT_CANCELED, result)
        finish()
    }

    companion object {
        const val EXTRA_SHOP_ID = "shop_id"
        const val EXTRA_SHOP_NAME = "shop_name"
        const val EXTRA_RAZORPAY_ORDER_ID = "razorpay_order_id"
        const val EXTRA_AMOUNT_PAISE = "amount_paise"
        const val EXTRA_KEY_ID = "key_id"
        const val EXTRA_RAZORPAY_PAYMENT_ID = "razorpay_payment_id"
        const val EXTRA_RAZORPAY_SIGNATURE = "razorpay_signature"
        const val EXTRA_ERROR = "error"

        fun buildIntent(
            activity: Activity,
            shopId: String,
            shopName: String,
            razorpayOrderId: String,
            amountPaise: Long,
            keyId: String,
        ): Intent = Intent(activity, ShopPromoteCheckoutActivity::class.java).apply {
            putExtra(EXTRA_SHOP_ID, shopId)
            putExtra(EXTRA_SHOP_NAME, shopName)
            putExtra(EXTRA_RAZORPAY_ORDER_ID, razorpayOrderId)
            putExtra(EXTRA_AMOUNT_PAISE, amountPaise)
            putExtra(EXTRA_KEY_ID, keyId)
        }
    }
}
