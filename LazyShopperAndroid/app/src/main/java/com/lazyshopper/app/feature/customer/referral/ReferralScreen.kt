package com.lazyshopper.app.feature.customer.referral

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lazyshopper.app.core.data.remote.dto.Referral
import com.lazyshopper.app.core.theme.LsAccent
import com.lazyshopper.app.core.theme.LsPrimary
import com.lazyshopper.app.core.ui.UiState
import com.lazyshopper.app.core.ui.components.ErrorState
import com.lazyshopper.app.core.ui.components.FullScreenLoading
import com.lazyshopper.app.core.ui.components.LsPrimaryButton
import com.lazyshopper.app.core.ui.components.ScreenPadding
import com.lazyshopper.app.feature.customer.util.formatMoney

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferralScreen(
    onBack: () -> Unit,
    viewModel: ReferralViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Refer & Earn") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } },
            )
        },
    ) { padding ->
        when (val s = state) {
            is UiState.Loading -> FullScreenLoading(Modifier.padding(padding))
            is UiState.Error -> ErrorState(s.message, Modifier.padding(padding), onRetry = viewModel::load)
            is UiState.Success -> {
                val data = s.data
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(ScreenPadding),
                ) {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Your referral code", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(8.dp))
                            Surface(color = LsPrimary.copy(alpha = 0.1f), shape = MaterialTheme.shapes.medium) {
                                Text(
                                    data.code,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = LsPrimary,
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
                                )
                            }
                            Spacer(Modifier.height(16.dp))
                            Text("Wallet balance", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(formatMoney(data.wallet), style = MaterialTheme.typography.titleLarge)
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    LsPrimaryButton(
                        text = "Share invite",
                        onClick = {
                            val shareText = data.share_text ?: "Use my code ${data.code} on Lazy Shopper and we both earn rewards!"
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share referral code"))
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(Modifier.height(20.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        StatTile("Credited", formatMoney(data.credited_total), LsPrimary, Modifier.weight(1f))
                        Spacer(Modifier.width(10.dp))
                        StatTile("Pending", formatMoney(data.pending_total), LsAccent, Modifier.weight(1f))
                    }

                    Spacer(Modifier.height(24.dp))
                    Text("How it works", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Share your code with friends. When they place their first order with it, " +
                            "they get ${formatMoney(data.discount_amount)} off and you earn ${formatMoney(data.reward_amount)} " +
                            "credited to your wallet once their order is delivered.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    if (data.referrals.isNotEmpty()) {
                        Spacer(Modifier.height(24.dp))
                        Text("Your referrals", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        data.referrals.forEach { referral ->
                            ReferralRow(referral)
                            Divider()
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
            UiState.Idle -> {}
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, color = color)
        }
    }
}

@Composable
private fun ReferralRow(referral: Referral) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(referral.referred_name ?: "Referred user", style = MaterialTheme.typography.bodyMedium)
            Text(
                referral.status.replace('_', ' ').replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (referral.reward_amount > 0.0) {
            Text(formatMoney(referral.reward_amount), style = MaterialTheme.typography.bodyMedium, color = LsPrimary)
        }
    }
}
