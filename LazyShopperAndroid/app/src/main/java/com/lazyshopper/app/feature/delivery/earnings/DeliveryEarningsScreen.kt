package com.lazyshopper.app.feature.delivery.earnings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lazyshopper.app.core.data.remote.dto.DeliveryEarningsResponse
import com.lazyshopper.app.core.data.remote.dto.DeliveryPayoutBatch
import com.lazyshopper.app.core.data.remote.dto.Order
import com.lazyshopper.app.core.theme.LsPrimary
import com.lazyshopper.app.core.ui.ActionState
import com.lazyshopper.app.core.ui.UiState
import com.lazyshopper.app.core.ui.components.ErrorState
import com.lazyshopper.app.core.ui.components.FullScreenLoading
import com.lazyshopper.app.core.ui.components.LsPrimaryButton
import com.lazyshopper.app.core.ui.components.LsTextField
import com.lazyshopper.app.core.ui.components.ScreenPadding

@Composable
fun DeliveryEarningsScreen(viewModel: DeliveryEarningsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    Column(Modifier.fillMaxSize()) {
        Text("Earnings", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(ScreenPadding))

        when (val screen = state.screen) {
            is UiState.Error -> ErrorState(screen.message, onRetry = viewModel::load)
            is UiState.Success -> {
                val data = screen.data
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                ) {
                    SummaryCard(data.earnings)

                    Spacer(Modifier.height(20.dp))
                    Text("Delivered orders", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    if (data.earnings.orders.isEmpty()) {
                        Text("No deliveries yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        data.earnings.orders.forEach { order ->
                            DeliveredOrderRow(order)
                            Spacer(Modifier.height(8.dp))
                        }
                    }

                    Spacer(Modifier.height(20.dp))
                    Text("Payout history", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    if (data.payouts.isEmpty()) {
                        Text("No settled payouts yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        data.payouts.forEach { batch ->
                            PayoutRow(batch)
                            Spacer(Modifier.height(8.dp))
                        }
                    }

                    Spacer(Modifier.height(20.dp))
                    Text("Payout bank details", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    LsTextField(value = state.holder, onValueChange = viewModel::setHolder, label = "Account holder name")
                    Spacer(Modifier.height(8.dp))
                    LsTextField(value = state.accountNumber, onValueChange = viewModel::setAccountNumber, label = "Account number")
                    Spacer(Modifier.height(8.dp))
                    LsTextField(value = state.ifsc, onValueChange = viewModel::setIfsc, label = "IFSC code")
                    Spacer(Modifier.height(8.dp))
                    LsTextField(value = state.vpa, onValueChange = viewModel::setVpa, label = "UPI ID (optional)")

                    if (state.bankSaveState is ActionState.Failed) {
                        Spacer(Modifier.height(6.dp))
                        Text((state.bankSaveState as ActionState.Failed).message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    if (state.bankSaveState is ActionState.Done) {
                        Spacer(Modifier.height(6.dp))
                        Text("Saved", color = LsPrimary, style = MaterialTheme.typography.bodySmall)
                    }

                    Spacer(Modifier.height(12.dp))
                    LsPrimaryButton(
                        text = "Save bank details",
                        onClick = viewModel::saveBank,
                        loading = state.bankSaveState is ActionState.InFlight,
                    )
                    Spacer(Modifier.height(32.dp))
                }
            }
            else -> FullScreenLoading()
        }
    }
}

@Composable
private fun SummaryCard(earnings: DeliveryEarningsResponse) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("₹%.2f".format(earnings.total_earnings), style = MaterialTheme.typography.headlineMedium, color = LsPrimary)
            Text("Total earnings", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("${earnings.total_deliveries}", style = MaterialTheme.typography.titleMedium)
                    Text("Deliveries", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column {
                    Text("%.1f★".format(earnings.avg_rating), style = MaterialTheme.typography.titleMedium)
                    Text("Rating (${earnings.review_count})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun DeliveredOrderRow(order: Order) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(order.address?.take(40) ?: order.id, style = MaterialTheme.typography.bodyMedium)
                Text(
                    order.delivered_at ?: order.created_at.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text("₹%.2f".format(order.rider_payout), style = MaterialTheme.typography.titleSmall, color = LsPrimary)
        }
    }
}

@Composable
private fun PayoutRow(batch: DeliveryPayoutBatch) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("${batch.orders} deliveries", style = MaterialTheme.typography.bodyMedium)
                Text(batch.settled_at.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("₹%.2f".format(batch.amount), style = MaterialTheme.typography.titleSmall, color = LsPrimary)
                Text(batch.payout_status ?: "settled", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
