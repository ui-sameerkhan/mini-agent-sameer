package com.lazyshopper.app.feature.shopkeeper.shops

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lazyshopper.app.core.data.remote.dto.Shop
import com.lazyshopper.app.core.theme.LsAccent
import com.lazyshopper.app.core.theme.LsError
import com.lazyshopper.app.core.theme.LsPrimary
import com.lazyshopper.app.core.theme.LsWarning
import com.lazyshopper.app.core.ui.ActionState
import com.lazyshopper.app.core.ui.UiState
import com.lazyshopper.app.core.ui.components.EmptyState
import com.lazyshopper.app.core.ui.components.ErrorState
import com.lazyshopper.app.core.ui.components.FullScreenLoading
import com.lazyshopper.app.core.ui.components.LsPrimaryButton
import com.lazyshopper.app.core.ui.components.LsTextField
import com.lazyshopper.app.core.ui.components.ScreenPadding
import com.lazyshopper.app.feature.shopkeeper.common.statusLabel

private val PROMO_PLANS = listOf(7 to 99, 30 to 299, 90 to 799)

@Composable
fun ShopsScreen(
    onAddShop: () -> Unit,
    viewModel: ShopsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var timingsShop by remember { mutableStateOf<Shop?>(null) }
    var promoteShop by remember { mutableStateOf<Shop?>(null) }
    var deleteShop by remember { mutableStateOf<Shop?>(null) }

    val razorpayLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data = result.data
        viewModel.onPromoteResult(
            paymentId = data?.getStringExtra(ShopPromoteCheckoutActivity.EXTRA_RAZORPAY_PAYMENT_ID),
            razorpayOrderId = data?.getStringExtra(ShopPromoteCheckoutActivity.EXTRA_RAZORPAY_ORDER_ID),
            signature = data?.getStringExtra(ShopPromoteCheckoutActivity.EXTRA_RAZORPAY_SIGNATURE),
            error = data?.getStringExtra(ShopPromoteCheckoutActivity.EXTRA_ERROR),
        )
    }

    LaunchedEffect(state.promoteLaunch) {
        val launch = state.promoteLaunch ?: return@LaunchedEffect
        val activity = context as? Activity
        if (activity != null) {
            razorpayLauncher.launch(
                ShopPromoteCheckoutActivity.buildIntent(
                    activity = activity,
                    shopId = launch.shopId,
                    shopName = launch.shopName,
                    razorpayOrderId = launch.razorpayOrderId,
                    amountPaise = launch.amountPaise,
                    keyId = launch.keyId,
                ),
            )
        }
        viewModel.consumePromoteLaunch()
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddShop) { Icon(Icons.Default.Add, contentDescription = "Add shop") }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            when (val s = state.shops) {
                is UiState.Loading -> FullScreenLoading()
                is UiState.Error -> ErrorState(s.message, onRetry = viewModel::load)
                is UiState.Idle -> {}
                is UiState.Success -> {
                    if (s.data.isEmpty()) {
                        EmptyState("No shops yet. Add your first shop to start selling.", actionLabel = "Add shop", onAction = onAddShop)
                    } else {
                        LazyColumn(
                            contentPadding = ScreenPadding,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            items(s.data, key = { it.id }) { shop ->
                                ShopCard(
                                    shop = shop,
                                    onTimings = { timingsShop = shop },
                                    onPromote = { promoteShop = shop },
                                    onDelete = { deleteShop = shop },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    timingsShop?.let { shop ->
        TimingsDialog(
            shop = shop,
            busy = state.timingsState is ActionState.InFlight,
            error = (state.timingsState as? ActionState.Failed)?.message,
            onDismiss = { timingsShop = null; viewModel.resetActionStates() },
            onSave = { open, close -> viewModel.setTimings(shop.id, open, close) },
        )
        LaunchedEffect(state.timingsState) {
            if (state.timingsState is ActionState.Done) { timingsShop = null; viewModel.resetActionStates() }
        }
    }

    promoteShop?.let { shop ->
        PromoteDialog(
            shop = shop,
            checkoutBusy = state.promoteCheckoutState is ActionState.InFlight,
            verifyBusy = state.promoteVerifyState is ActionState.InFlight,
            error = (state.promoteVerifyState as? ActionState.Failed)?.message
                ?: (state.promoteCheckoutState as? ActionState.Failed)?.message,
            onDismiss = { promoteShop = null; viewModel.resetActionStates() },
            onPick = { days -> viewModel.startPromote(shop, days) },
        )
        LaunchedEffect(state.promoteVerifyState) {
            if (state.promoteVerifyState is ActionState.Done) { promoteShop = null; viewModel.resetActionStates() }
        }
    }

    deleteShop?.let { shop ->
        AlertDialog(
            onDismissRequest = { deleteShop = null },
            title = { Text("Delete ${shop.name}?") },
            text = { Text("This also deletes every product under this shop. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteShop(shop.id); deleteShop = null }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleteShop = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun ShopCard(shop: Shop, onTimings: () -> Unit, onPromote: () -> Unit, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(shop.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                StatusChip(shop.status)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "${shop.area}, ${shop.district}, ${shop.state}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "${statusLabel(shop.category)} · ${shop.product_count ?: 0} product(s)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (shop.open_time != null || shop.close_time != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    "Open ${shop.open_time ?: "--:--"} – ${shop.close_time ?: "--:--"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (shop.promoted == true) {
                Spacer(Modifier.height(2.dp))
                Text("★ Promoted until ${shop.promoted_until ?: "—"}", style = MaterialTheme.typography.bodySmall, color = LsAccent)
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onTimings) { Text("Timings") }
                if (shop.status == "approved") {
                    OutlinedButton(onClick = onPromote) { Text("★ Promote") }
                }
                OutlinedButton(onClick = onDelete) { Text("Delete") }
            }
        }
    }
}

@Composable
private fun StatusChip(status: String?) {
    val tint = when (status) {
        "approved" -> LsPrimary
        "rejected", "suspended" -> LsError
        else -> LsWarning
    }
    Text(
        statusLabel(status),
        style = MaterialTheme.typography.labelMedium,
        color = tint,
    )
}

@Composable
private fun TimingsDialog(shop: Shop, busy: Boolean, error: String?, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var open by remember { mutableStateOf(shop.open_time ?: "09:00") }
    var close by remember { mutableStateOf(shop.close_time ?: "21:00") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Timings — ${shop.name}") },
        text = {
            Column {
                LsTextField(value = open, onValueChange = { open = it }, label = "Open time (HH:MM)")
                Spacer(Modifier.height(10.dp))
                LsTextField(value = close, onValueChange = { close = it }, label = "Close time (HH:MM)")
                if (error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(open.trim(), close.trim()) }, enabled = !busy) {
                Text(if (busy) "Saving..." else "Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("Cancel") } },
    )
}

@Composable
private fun PromoteDialog(shop: Shop, checkoutBusy: Boolean, verifyBusy: Boolean, error: String?, onDismiss: () -> Unit, onPick: (Int) -> Unit) {
    val busy = checkoutBusy || verifyBusy
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Promote ${shop.name}") },
        text = {
            Column {
                Text("Boost visibility on the customer home screen.", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                PROMO_PLANS.forEach { (days, price) ->
                    OutlinedButton(
                        onClick = { onPick(days) },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    ) {
                        Text("$days days — ₹$price")
                    }
                }
                if (busy) {
                    Spacer(Modifier.height(8.dp))
                    Text(if (checkoutBusy) "Preparing payment..." else "Verifying payment...", style = MaterialTheme.typography.bodySmall)
                }
                if (error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("Close") } },
    )
}

@Composable
fun ShopFormScreen(onDone: () -> Unit, viewModel: ShopFormViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.submitState) {
        if (state.submitState is ActionState.Done) onDone()
    }

    Column(Modifier.fillMaxSize().padding(ScreenPadding)) {
        Text("Add shop", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        LsTextField(value = state.name, onValueChange = viewModel::setName, label = "Shop name")
        Spacer(Modifier.height(12.dp))
        LsTextField(value = state.state, onValueChange = viewModel::setState, label = "State")
        Spacer(Modifier.height(12.dp))
        LsTextField(value = state.district, onValueChange = viewModel::setDistrict, label = "District")
        Spacer(Modifier.height(12.dp))
        LsTextField(value = state.area, onValueChange = viewModel::setArea, label = "Area")
        Spacer(Modifier.height(16.dp))
        Text("Category", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(8.dp))
        Column {
            SHOP_CATEGORIES.forEach { cat ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    androidx.compose.material3.RadioButton(
                        selected = state.category == cat,
                        onClick = { viewModel.setCategory(cat) },
                    )
                    Text(statusLabel(cat), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        if (state.submitState is ActionState.Failed) {
            Spacer(Modifier.height(8.dp))
            Text((state.submitState as ActionState.Failed).message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(20.dp))
        LsPrimaryButton(text = "Create shop", onClick = viewModel::submit, loading = state.submitState is ActionState.InFlight)
    }
}
