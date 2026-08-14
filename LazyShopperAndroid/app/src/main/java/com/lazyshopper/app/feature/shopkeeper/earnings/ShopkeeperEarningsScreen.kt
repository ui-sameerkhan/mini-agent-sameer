package com.lazyshopper.app.feature.shopkeeper.earnings

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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lazyshopper.app.core.data.remote.dto.Settlement
import com.lazyshopper.app.core.data.remote.dto.ShopkeeperEarningsResponse
import com.lazyshopper.app.core.theme.LsError
import com.lazyshopper.app.core.theme.LsPrimary
import com.lazyshopper.app.core.theme.LsWarning
import com.lazyshopper.app.core.ui.ActionState
import com.lazyshopper.app.core.ui.UiState
import com.lazyshopper.app.core.ui.components.ErrorState
import com.lazyshopper.app.core.ui.components.FullScreenLoading
import com.lazyshopper.app.core.ui.components.LsPrimaryButton
import com.lazyshopper.app.core.ui.components.LsTextField
import com.lazyshopper.app.core.ui.components.ScreenPadding
import com.lazyshopper.app.feature.shopkeeper.common.money

@Composable
fun ShopkeeperEarningsScreen(viewModel: ShopkeeperEarningsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    Column(Modifier.fillMaxSize()) {
        Text("Earnings", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(ScreenPadding))

        when (val screen = state.screen) {
            is UiState.Error -> ErrorState(screen.message, onRetry = viewModel::load)
            is UiState.Idle, is UiState.Loading -> FullScreenLoading()
            is UiState.Success -> {
                val earnings = screen.data
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                ) {
                    SummaryCard(earnings)

                    Spacer(Modifier.height(20.dp))
                    Text("Settlement history", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    if (earnings.history.isEmpty()) {
                        Text("No settlements yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        earnings.history.forEach { settlement ->
                            SettlementRow(settlement, onDownload = { PayoutPdfGenerator.buildAndShare(context, settlement) })
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
        }
    }
}

@Composable
private fun SummaryCard(earnings: ShopkeeperEarningsResponse) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(money(earnings.earning), style = MaterialTheme.typography.headlineMedium, color = LsPrimary)
            Text("Total earning · ${earnings.orders} order(s)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MiniStat("Sales", money(earnings.sales))
                MiniStat("Commission", money(earnings.commission))
                MiniStat("Pending", money(earnings.pending))
                MiniStat("Paid", money(earnings.paid))
            }
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleSmall)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SettlementRow(settlement: Settlement, onDownload: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(settlement.shop_name ?: "Order ${settlement.order_id?.takeLast(6) ?: settlement.id.takeLast(6)}", style = MaterialTheme.typography.bodyMedium)
                Text(
                    settlement.settled_at.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val statusTint = when (settlement.payout_status) {
                    "paid", "settled" -> LsPrimary
                    "failed" -> LsError
                    else -> LsWarning
                }
                Text(settlement.payout_status ?: "pending", style = MaterialTheme.typography.labelSmall, color = statusTint)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(money(settlement.amount), style = MaterialTheme.typography.titleSmall, color = LsPrimary)
                Spacer(Modifier.height(6.dp))
                OutlinedButton(onClick = onDownload) { Text("Statement") }
            }
        }
    }
}
