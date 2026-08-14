package com.lazyshopper.app.feature.customer.cart

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.lazyshopper.app.core.data.remote.dto.QuoteResponse
import com.lazyshopper.app.core.ui.UiState
import com.lazyshopper.app.core.ui.components.EmptyState
import com.lazyshopper.app.core.ui.components.LsPrimaryButton
import com.lazyshopper.app.core.ui.components.LsTextField
import com.lazyshopper.app.core.ui.components.ScreenPadding
import com.lazyshopper.app.feature.customer.util.fileUrl
import com.lazyshopper.app.feature.customer.util.formatMoney
import com.lazyshopper.app.feature.customer.util.formatQty

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    onBack: () -> Unit,
    onCheckout: () -> Unit,
    viewModel: CartViewModel = hiltViewModel(),
) {
    val lines by viewModel.lines.collectAsState()
    val couponCode by viewModel.couponCode.collectAsState()
    val couponMessage by viewModel.couponMessage.collectAsState()
    val referralCode by viewModel.referralCode.collectAsState()
    val useWallet by viewModel.useWallet.collectAsState()
    val quote by viewModel.quote.collectAsState()

    LaunchedEffect(lines.size) { viewModel.refreshQuoteDebounced() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Your Cart") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                },
            )
        },
    ) { padding ->
        if (lines.isEmpty()) {
            EmptyState(
                message = "Your cart is empty. Add some fresh groceries!",
                modifier = Modifier.padding(padding),
                actionLabel = "Browse products",
                onAction = onBack,
            )
            return@Scaffold
        }

        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(lines.values.toList(), key = { it.product.id }) { line ->
                    CartLineRow(
                        line = line,
                        onIncrement = { viewModel.increment(line) },
                        onDecrement = { viewModel.decrement(line) },
                        onRemove = { viewModel.removeLine(line.product.id) },
                    )
                }

                item {
                    Spacer(Modifier.height(8.dp))
                    Text("Have a coupon?", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LsTextField(
                            value = couponCode,
                            onValueChange = viewModel::setCoupon,
                            label = "Coupon code",
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(8.dp))
                        TextButton(onClick = { viewModel.applyCoupon() }) { Text("Apply") }
                    }
                    couponMessage?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                    }
                    Spacer(Modifier.height(12.dp))

                    Text("Referral code", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(6.dp))
                    LsTextField(value = referralCode, onValueChange = viewModel::setReferralCode, label = "Friend's referral code (optional)")

                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = useWallet, onCheckedChange = viewModel::setUseWallet)
                        Text("Use my referral wallet balance", style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(Modifier.height(12.dp))
                }

                item {
                    QuoteBreakdown(quote)
                    Spacer(Modifier.height(80.dp))
                }
            }

            HorizontalDivider()
            Column(modifier = Modifier.padding(ScreenPadding)) {
                val totalText = (quote as? UiState.Success)?.data?.total?.let { formatMoney(it) }
                    ?: formatMoney(lines.values.sumOf { it.product.price * it.qty })
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total payable", style = MaterialTheme.typography.titleMedium)
                    Text(totalText, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.height(10.dp))
                LsPrimaryButton(text = "Proceed to checkout", onClick = onCheckout)
            }
        }
    }
}

@Composable
private fun CartLineRow(
    line: CartLine,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onRemove: () -> Unit,
) {
    Card(shape = RoundedCornerShape(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = fileUrl(line.product.image_id),
                contentDescription = line.product.name,
                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop,
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(line.product.name, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                Text(
                    "${formatMoney(line.product.price)} / ${line.product.unit_type}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDecrement, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(16.dp))
                    }
                    Text(formatQty(line.qty), modifier = Modifier.width(28.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    IconButton(onClick = onIncrement, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(16.dp))
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(formatMoney(line.product.price * line.qty), style = MaterialTheme.typography.titleSmall)
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
internal fun QuoteBreakdown(quote: UiState<QuoteResponse>) {
    when (quote) {
        is UiState.Success -> {
            val q = quote.data
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    BreakdownRow("Subtotal", formatMoney(q.subtotal))
                    BreakdownRow("Platform fee", formatMoney(q.platform_fee))
                    BreakdownRow(
                        "Delivery fee",
                        if (q.free_delivery) "FREE" else formatMoney(q.delivery_fee),
                        highlight = q.free_delivery,
                    )
                    if (q.discount > 0.0) BreakdownRow("Coupon discount", "-${formatMoney(q.discount)}")
                    if (q.referral_discount > 0.0) BreakdownRow("Referral discount", "-${formatMoney(q.referral_discount)}")
                    if (q.wallet_used > 0.0) BreakdownRow("Wallet applied", "-${formatMoney(q.wallet_used)}")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    BreakdownRow("Total", formatMoney(q.total), bold = true)
                    if (!q.free_delivery && q.free_delivery_min > 0.0 && q.subtotal < q.free_delivery_min) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Add ${formatMoney(q.free_delivery_min - q.subtotal)} more for FREE delivery!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                    if (referralInvalid(q)) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            q.referral_reason ?: "Referral code not applicable",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
        is UiState.Loading -> Text("Calculating price…", style = MaterialTheme.typography.bodySmall)
        is UiState.Error -> Text(quote.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        UiState.Idle -> {}
    }
}

private fun referralInvalid(q: QuoteResponse): Boolean = !q.referral_valid && !q.referral_reason.isNullOrBlank()

@Composable
internal fun BreakdownRow(label: String, value: String, bold: Boolean = false, highlight: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            label,
            style = if (bold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
        )
        Text(
            value,
            style = if (bold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

