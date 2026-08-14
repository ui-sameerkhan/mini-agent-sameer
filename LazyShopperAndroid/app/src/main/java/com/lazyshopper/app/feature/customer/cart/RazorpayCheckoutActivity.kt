package com.lazyshopper.app.feature.customer.cart

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.razorpay.Checkout
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import org.json.JSONObject

/**
 * Thin, self-contained bridge to the Razorpay Android Checkout SDK.
 *
 * The Razorpay SDK requires the hosting Activity itself to implement its payment-result
 * listener interface (it casts the Activity passed to `Checkout.open()`), so this can't be driven
 * purely from a Composable/ViewModel. This Activity is launched via
 * `ActivityResultContracts.StartActivityForResult()` from [CheckoutScreen] and returns the
 * razorpay_payment_id / razorpay_order_id / razorpay_signature (or an error) as result extras,
 * which [com.lazyshopper.app.feature.customer.cart.CheckoutViewModel] then forwards to
 * `POST /api/payments/razorpay/verify`.
 *
 * IMPORTANT — deployment note: this Activity must be declared in `AndroidManifest.xml` (a file
 * outside this feature's directory) for the launch to resolve at runtime, e.g.:
 * ```xml
 * <activity
 *     android:name=".feature.customer.cart.RazorpayCheckoutActivity"
 *     android:theme="@android:style/Theme.Translucent.NoTitleBar"
 *     android:exported="false" />
 * ```
 * Since this workstream is scoped to `feature/customer/` only, that manifest entry was not added
 * here and needs to be added by whoever owns the shared manifest.
 */
class RazorpayCheckoutActivity : Activity(), PaymentResultWithDataListener {

    private var localOrderId: String? = null
    private var razorpayOrderId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        localOrderId = intent.getStringExtra(EXTRA_LOCAL_ORDER_ID)
        razorpayOrderId = intent.getStringExtra(EXTRA_RAZORPAY_ORDER_ID)
        val amountPaise = intent.getLongExtra(EXTRA_AMOUNT_PAISE, 0L)
        val keyId = intent.getStringExtra(EXTRA_KEY_ID).orEmpty()
        val customerEmail = intent.getStringExtra(EXTRA_CUSTOMER_EMAIL).orEmpty()
        val customerPhone = intent.getStringExtra(EXTRA_CUSTOMER_PHONE).orEmpty()

        val checkout = Checkout()
        checkout.setKeyID(keyId)

        try {
            val options = JSONObject().apply {
                put("name", "Lazy Shopper")
                put("description", "Order payment")
                put("currency", "INR")
                put("order_id", razorpayOrderId)
                put("amount", amountPaise)
                put("retry", JSONObject().apply { put("enabled", false) })
                val prefill = JSONObject()
                if (customerEmail.isNotBlank()) prefill.put("email", customerEmail)
                if (customerPhone.isNotBlank()) prefill.put("contact", customerPhone)
                put("prefill", prefill)
            }
            checkout.open(this, options)
        } catch (e: Exception) {
            finishWithError(e.message ?: "Could not open Razorpay checkout")
        }
    }

    override fun onPaymentSuccess(razorpayPaymentId: String?, paymentData: PaymentData?) {
        val result = Intent().apply {
            putExtra(EXTRA_LOCAL_ORDER_ID, localOrderId)
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
            putExtra(EXTRA_LOCAL_ORDER_ID, localOrderId)
            putExtra(EXTRA_ERROR, message)
        }
        setResult(RESULT_CANCELED, result)
        finish()
    }

    companion object {
        const val EXTRA_LOCAL_ORDER_ID = "local_order_id"
        const val EXTRA_RAZORPAY_ORDER_ID = "razorpay_order_id"
        const val EXTRA_AMOUNT_PAISE = "amount_paise"
        const val EXTRA_KEY_ID = "key_id"
        const val EXTRA_CUSTOMER_EMAIL = "customer_email"
        const val EXTRA_CUSTOMER_PHONE = "customer_phone"
        const val EXTRA_RAZORPAY_PAYMENT_ID = "razorpay_payment_id"
        const val EXTRA_RAZORPAY_SIGNATURE = "razorpay_signature"
        const val EXTRA_ERROR = "error"

        fun buildIntent(
            activity: Activity,
            localOrderId: String,
            razorpayOrderId: String,
            amountPaise: Long,
            keyId: String,
            customerEmail: String?,
            customerPhone: String?,
        ): Intent = Intent(activity, RazorpayCheckoutActivity::class.java).apply {
            putExtra(EXTRA_LOCAL_ORDER_ID, localOrderId)
            putExtra(EXTRA_RAZORPAY_ORDER_ID, razorpayOrderId)
            putExtra(EXTRA_AMOUNT_PAISE, amountPaise)
            putExtra(EXTRA_KEY_ID, keyId)
            putExtra(EXTRA_CUSTOMER_EMAIL, customerEmail)
            putExtra(EXTRA_CUSTOMER_PHONE, customerPhone)
        }
    }
}
