package com.lazyshopper.app.feature.customer.cart

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lazyshopper.app.core.ui.ActionState
import com.lazyshopper.app.core.ui.UiState
import com.lazyshopper.app.core.ui.components.LsPrimaryButton
import com.lazyshopper.app.core.ui.components.LsTextField
import com.lazyshopper.app.core.ui.components.ScreenPadding
import com.lazyshopper.app.feature.customer.util.formatMoney

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    onBack: () -> Unit,
    onPlacedOrder: (orderId: String, status: String) -> Unit,
    viewModel: CheckoutViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    val razorpayLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data = result.data
        viewModel.onRazorpayResult(
            paymentId = data?.getStringExtra(RazorpayCheckoutActivity.EXTRA_RAZORPAY_PAYMENT_ID),
            razorpayOrderId = data?.getStringExtra(RazorpayCheckoutActivity.EXTRA_RAZORPAY_ORDER_ID),
            signature = data?.getStringExtra(RazorpayCheckoutActivity.EXTRA_RAZORPAY_SIGNATURE),
            error = data?.getStringExtra(RazorpayCheckoutActivity.EXTRA_ERROR),
        )
    }

    LaunchedEffect(state.razorpayLaunch) {
        val req = state.razorpayLaunch ?: return@LaunchedEffect
        val activity = context as? Activity
        if (activity != null) {
            val intent = RazorpayCheckoutActivity.buildIntent(
                activity = activity,
                localOrderId = req.localOrderId,
                razorpayOrderId = req.razorpayOrderId,
                amountPaise = req.amountPaise,
                keyId = req.keyId,
                customerEmail = null,
                customerPhone = state.phone,
            )
            razorpayLauncher.launch(intent)
        }
        viewModel.consumeRazorpayLaunch()
    }

    LaunchedEffect(state.phone, state.manualAddress, state.useManualAddress, state.selectedAddressId, state.deliverySlot) {
        kotlinx.coroutines.delay(400)
        viewModel.refreshQuote()
    }

    LaunchedEffect(state.completedOrder, state.paymentStatus) {
        val order = state.completedOrder ?: return@LaunchedEffect
        when (state.paymentStatus) {
            "cod" -> onPlacedOrder(order.id, "cod")
            "paid" -> onPlacedOrder(order.id, "success")
            "failed" -> onPlacedOrder(order.id, "failed")
            else -> {} // razorpay still in flight
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Checkout") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(ScreenPadding),
        ) {
            Text("Delivery address", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            when (val addrState = state.addresses) {
                is UiState.Success -> addrState.data.forEach { addr ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = !state.useManualAddress && state.selectedAddressId == addr.id,
                                onClick = { viewModel.selectAddress(addr.id) },
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = !state.useManualAddress && state.selectedAddressId == addr.id,
                            onClick = { viewModel.selectAddress(addr.id) },
                        )
                        Column {
                            Text(addr.label, style = MaterialTheme.typography.labelLarge)
                            Text(addr.address, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                else -> {}
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(selected = state.useManualAddress, onClick = { viewModel.useManualAddress(true) })
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = state.useManualAddress, onClick = { viewModel.useManualAddress(true) })
                Text("Enter a different address")
            }

            if (state.useManualAddress) {
                Spacer(Modifier.height(4.dp))
                LsTextField(
                    value = state.manualAddress,
                    onValueChange = viewModel::setManualAddress,
                    label = "Full delivery address",
                    singleLine = false,
                )
            }

            Spacer(Modifier.height(12.dp))
            LsTextField(
                value = state.phone,
                onValueChange = viewModel::setPhone,
                label = "Contact phone number",
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone,
            )

            Spacer(Modifier.height(20.dp))
            Text("Delivery slot", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DELIVERY_SLOTS.forEach { slot ->
                    FilterChip(
                        selected = state.deliverySlot == slot,
                        onClick = { viewModel.setDeliverySlot(slot) },
                        label = { Text(slot) },
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Text("Payment method", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.paymentMethod == "cod",
                    onClick = { viewModel.setPaymentMethod("cod") },
                    label = { Text("Cash on Delivery") },
                )
                FilterChip(
                    selected = state.paymentMethod == "razorpay",
                    onClick = { viewModel.setPaymentMethod("razorpay") },
                    label = { Text("Pay Online (Razorpay)") },
                )
            }

            Spacer(Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))
            QuoteBreakdown(state.quote)

            if (state.placeOrderState is ActionState.Failed) {
                Spacer(Modifier.height(8.dp))
                Text(
                    (state.placeOrderState as ActionState.Failed).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Spacer(Modifier.height(20.dp))
            val totalLabel = (state.quote as? UiState.Success)?.data?.total?.let { "Place order — ${formatMoney(it)}" } ?: "Place order"
            LsPrimaryButton(
                text = totalLabel,
                onClick = viewModel::placeOrder,
                loading = state.placeOrderState is ActionState.InFlight,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}
