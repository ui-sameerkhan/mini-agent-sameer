package com.lazyshopper.app.feature.shopkeeper.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lazyshopper.app.core.data.remote.dto.EarningsSeriesPoint
import com.lazyshopper.app.core.theme.LsPrimary
import com.lazyshopper.app.core.ui.components.ErrorState
import com.lazyshopper.app.core.ui.components.FullScreenLoading
import com.lazyshopper.app.core.ui.components.ScreenPadding
import com.lazyshopper.app.feature.shopkeeper.common.money
import com.lazyshopper.app.feature.shopkeeper.kyc.ShopkeeperKycStatusBanner

@Composable
fun ShopkeeperDashboardScreen(
    onNavigateProducts: () -> Unit,
    onNavigateShops: () -> Unit,
    onNavigateOrders: () -> Unit,
    onNavigateEarnings: () -> Unit,
    onNavigateKyc: () -> Unit,
    onLogout: () -> Unit,
    viewModel: ShopkeeperDashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    if (state.loading) {
        FullScreenLoading()
        return
    }

    if (state.error != null && state.earnings == null) {
        ErrorState(state.error.orEmpty(), onRetry = viewModel::load)
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(ScreenPadding),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Dashboard", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = onLogout) { Text("Logout") }
        }
        Spacer(Modifier.height(12.dp))

        ShopkeeperKycStatusBanner(kycStatus = state.kycStatus, rejectReason = state.kycRejectReason, onComplete = onNavigateKyc)
        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard("${state.approvedShopCount}/${state.shopCount}", "Shops", Modifier.weight(1f), onClick = onNavigateShops)
            StatCard("${state.productCount}", "Products", Modifier.weight(1f), onClick = onNavigateProducts)
            StatCard("${state.pendingOrdersCount}", "Open orders", Modifier.weight(1f), onClick = onNavigateOrders)
        }

        Spacer(Modifier.height(20.dp))
        val earnings = state.earnings
        if (earnings != null) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Earnings", style = MaterialTheme.typography.titleMedium)
                        TextButton(onClick = onNavigateEarnings) { Text("Details") }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(money(earnings.earning), style = MaterialTheme.typography.headlineMedium, color = LsPrimary)
                    Text("Total earning · ${earnings.orders} order(s)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        MiniStat("Sales", money(earnings.sales))
                        MiniStat("Commission", money(earnings.commission))
                        MiniStat("Pending", money(earnings.pending))
                        MiniStat("Paid", money(earnings.paid))
                    }

                    val points = earnings.series_weekly.ifEmpty { earnings.series }
                    if (points.isNotEmpty()) {
                        Spacer(Modifier.height(20.dp))
                        Text("Trend", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        EarningsBarChart(points = points, modifier = Modifier.fillMaxWidth().height(140.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun StatCard(value: String, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(modifier = modifier, onClick = onClick) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(value, style = MaterialTheme.typography.titleLarge, color = LsPrimary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(2.dp))
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
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

/**
 * Minimal dependency-free bar chart for the earnings trend. Avoids pinning to the exact Vico
 * 1.14 compose-m3 API surface (which this sandbox can't compile against to verify) in favor of a
 * small Canvas draw that's easy to read and guaranteed to match the Compose BOM already in use.
 */
@Composable
private fun EarningsBarChart(points: List<EarningsSeriesPoint>, modifier: Modifier = Modifier) {
    val max = (points.maxOfOrNull { it.earning } ?: 0.0).coerceAtLeast(1.0)
    val barColor = LsPrimary
    val axisColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)

    Column(modifier) {
        Canvas(modifier = Modifier.fillMaxWidth().weight(1f)) {
            val barCount = points.size
            if (barCount == 0) return@Canvas
            val slotWidth = size.width / barCount
            val barWidth = (slotWidth * 0.55f).coerceAtLeast(4f)
            drawLine(axisColor, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 2f)
            points.forEachIndexed { index, point ->
                val ratio = (point.earning / max).toFloat().coerceIn(0f, 1f)
                val barHeight = size.height * ratio
                val left = index * slotWidth + (slotWidth - barWidth) / 2f
                drawRect(
                    color = barColor,
                    topLeft = Offset(left, size.height - barHeight),
                    size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            val labelPoints = if (points.size > 6) points.filterIndexed { i, _ -> i % (points.size / 6).coerceAtLeast(1) == 0 } else points
            labelPoints.forEach { point ->
                Text(point.label.take(5), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
