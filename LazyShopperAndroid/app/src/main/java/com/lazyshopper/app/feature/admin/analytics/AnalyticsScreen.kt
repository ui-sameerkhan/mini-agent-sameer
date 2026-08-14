package com.lazyshopper.app.feature.admin.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lazyshopper.app.core.data.remote.ApiResult
import com.lazyshopper.app.core.data.remote.dto.AdminAnalyticsResponse
import com.lazyshopper.app.core.data.remote.dto.AnalyticsSeriesPoint
import com.lazyshopper.app.core.data.remote.dto.CouponAnalyticsResponse
import com.lazyshopper.app.core.data.remote.dto.StockReportResponse
import com.lazyshopper.app.core.theme.LsRoleAdmin
import com.lazyshopper.app.core.ui.UiState
import com.lazyshopper.app.feature.admin.common.AdminLazyList
import com.lazyshopper.app.feature.admin.common.AdminScreenScaffold
import com.lazyshopper.app.feature.admin.common.CompactRow
import com.lazyshopper.app.feature.admin.common.EmptyOrList
import com.lazyshopper.app.feature.admin.common.KeyValueRow
import com.lazyshopper.app.feature.admin.common.SectionDivider
import com.lazyshopper.app.feature.admin.common.StatTileRow
import com.lazyshopper.app.feature.admin.common.StatusChip
import com.lazyshopper.app.feature.admin.common.Tone
import com.lazyshopper.app.feature.admin.common.money
import com.lazyshopper.app.feature.admin.data.AdminAnalyticsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AnalyticsViewModel @Inject constructor(private val repo: AdminAnalyticsRepository) : ViewModel() {
    private val _analytics = MutableStateFlow<UiState<AdminAnalyticsResponse>>(UiState.Loading)
    val analytics: StateFlow<UiState<AdminAnalyticsResponse>> = _analytics.asStateFlow()
    private val _stock = MutableStateFlow<UiState<StockReportResponse>>(UiState.Loading)
    val stock: StateFlow<UiState<StockReportResponse>> = _stock.asStateFlow()
    private val _coupons = MutableStateFlow<UiState<CouponAnalyticsResponse>>(UiState.Loading)
    val coupons: StateFlow<UiState<CouponAnalyticsResponse>> = _coupons.asStateFlow()
    private val _days = MutableStateFlow(30)
    val days: StateFlow<Int> = _days.asStateFlow()

    fun setDays(d: Int) {
        _days.value = d
        loadAnalytics()
    }

    fun loadAnalytics() {
        viewModelScope.launch {
            _analytics.value = UiState.Loading
            when (val r = repo.analytics(_days.value)) {
                is ApiResult.Success -> _analytics.value = UiState.Success(r.data)
                is ApiResult.Failure -> _analytics.value = UiState.Error(r.message)
            }
        }
    }

    fun loadStock() {
        viewModelScope.launch {
            _stock.value = UiState.Loading
            when (val r = repo.stockReport()) {
                is ApiResult.Success -> _stock.value = UiState.Success(r.data)
                is ApiResult.Failure -> _stock.value = UiState.Error(r.message)
            }
        }
    }

    fun loadCoupons() {
        viewModelScope.launch {
            _coupons.value = UiState.Loading
            when (val r = repo.couponAnalytics()) {
                is ApiResult.Success -> _coupons.value = UiState.Success(r.data)
                is ApiResult.Failure -> _coupons.value = UiState.Error(r.message)
            }
        }
    }
}

@Composable
fun AdminAnalyticsScreen(onMenuClick: () -> Unit, initialTab: Int = 0, viewModel: AnalyticsViewModel = hiltViewModel()) {
    val analyticsState by viewModel.analytics.collectAsState()
    val stockState by viewModel.stock.collectAsState()
    val couponsState by viewModel.coupons.collectAsState()
    val days by viewModel.days.collectAsState()
    LaunchedEffect(Unit) { viewModel.loadAnalytics(); viewModel.loadStock(); viewModel.loadCoupons() }
    var tab by remember { mutableIntStateOf(initialTab) }

    AdminScreenScaffold(title = "Analytics & Insights", onMenuClick = onMenuClick) { padding ->
        Column(Modifier.padding(padding)) {
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Overview") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Stock report") })
                Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text("Coupons") })
            }
            when (tab) {
                0 -> {
                    val s = analyticsState
                    Column {
                        DayRangeSelector(
                            selected = days,
                            onSelect = viewModel::setDays,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                        EmptyOrList(loading = s is UiState.Loading, error = (s as? UiState.Error)?.message, isEmpty = false, emptyMessage = "", onRetry = viewModel::loadAnalytics) {
                            val d = (s as UiState.Success).data
                            Column(Modifier.verticalScroll(rememberScrollState())) {
                                StatTileRow(
                                    listOf(
                                        "Revenue" to d.total_revenue.money(),
                                        "Orders" to d.total_orders.toString(),
                                        "Active users" to d.active_users.toString(),
                                        "Live offers" to d.live_offers.toString(),
                                    ),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                )
                                Text("Revenue trend", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                                RevenueBarChart(d.series, modifier = Modifier.fillMaxWidth().height(160.dp).padding(horizontal = 12.dp))
                                Spacer12()
                                Text("Order status breakdown", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(horizontal = 12.dp))
                                Column(Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                                    d.status_counts.forEach { (status, count) -> KeyValueRow(status, count.toString()) }
                                }
                                SectionDivider(Modifier.padding(horizontal = 12.dp))
                                Text("Top products", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(horizontal = 12.dp))
                                Column(Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                                    d.top_products.forEach { p -> KeyValueRow(p.name, "${p.revenue.money()} · qty ${p.qty}") }
                                }
                                SectionDivider(Modifier.padding(horizontal = 12.dp))
                                Text("Top stores", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(horizontal = 12.dp))
                                Column(Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                                    d.top_stores.forEach { st -> KeyValueRow(st.name, "${st.revenue.money()} · ${st.orders} orders") }
                                }
                                SectionDivider(Modifier.padding(horizontal = 12.dp))
                                Text("Top banners", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(horizontal = 12.dp))
                                Column(Modifier.padding(horizontal = 12.dp, vertical = 4.dp, )) {
                                    d.top_banners.forEach { b -> KeyValueRow(b.name, "${b.impressions} views · ${b.clicks} clicks · ctr ${b.ctr}%") }
                                }
                                Spacer12()
                            }
                        }
                    }
                }
                1 -> {
                    val s = stockState
                    EmptyOrList(
                        loading = s is UiState.Loading,
                        error = (s as? UiState.Error)?.message,
                        isEmpty = (s as? UiState.Success)?.data?.products.isNullOrEmpty() && s is UiState.Success,
                        emptyMessage = "No low-stock products",
                        onRetry = viewModel::loadStock,
                    ) {
                        val d = (s as UiState.Success).data
                        Column {
                            StatTileRow(
                                listOf(
                                    "Low / out of stock" to d.count.toString(),
                                    "Out of stock" to d.out_of_stock.toString(),
                                    "Default threshold" to d.threshold_default.toString(),
                                ),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            )
                            AdminLazyList {
                                items(d.products, key = { it.id }) { p ->
                                    CompactRow(
                                        title = p.name,
                                        subtitle = "${p.shop_name ?: "-"} · ${p.category ?: "-"} · ${p.brand ?: "-"}",
                                        meta = "stock ${p.stock} ${p.unit_type ?: ""} · threshold ${p.threshold}",
                                        badge = { StatusChip(if (p.out) "out of stock" else "low stock", tone = if (p.out) Tone.ERROR else Tone.WARNING) },
                                    )
                                }
                            }
                        }
                    }
                }
                2 -> {
                    val s = couponsState
                    EmptyOrList(
                        loading = s is UiState.Loading,
                        error = (s as? UiState.Error)?.message,
                        isEmpty = (s as? UiState.Success)?.data?.coupons.isNullOrEmpty() && s is UiState.Success,
                        emptyMessage = "No coupon usage yet",
                        onRetry = viewModel::loadCoupons,
                    ) {
                        val d = (s as UiState.Success).data
                        Column {
                            StatTileRow(
                                listOf(
                                    "Coupons" to d.total_coupons.toString(),
                                    "Uses" to d.total_uses.toString(),
                                    "Discount given" to d.total_discount.money(),
                                    "Revenue" to d.revenue_from_coupons.money(),
                                ),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            )
                            AdminLazyList {
                                items(d.coupons, key = { it.code }) { c ->
                                    CompactRow(
                                        title = c.code,
                                        subtitle = "${c.type ?: "-"} ${c.value} · used ${c.uses}x",
                                        meta = "discount ${c.total_discount.money()} · revenue ${c.revenue.money()}",
                                        badge = { StatusChip(if (c.active) "active" else "inactive") },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Spacer12() = androidx.compose.foundation.layout.Spacer(Modifier.height(12.dp))

@Composable
private fun DayRangeSelector(selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.Row(modifier, horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)) {
        listOf(7, 14, 30, 90).forEach { d ->
            androidx.compose.material3.FilterChip(selected = selected == d, onClick = { onSelect(d) }, label = { Text("${d}d") })
        }
    }
}

/** Minimal dependency-free bar chart for the revenue/orders series — avoids pulling in a full charting lib for one screen. */
@Composable
private fun RevenueBarChart(series: List<AnalyticsSeriesPoint>, modifier: Modifier = Modifier) {
    if (series.isEmpty()) {
        Text("No data for this range", style = MaterialTheme.typography.bodySmall, modifier = modifier.padding(8.dp))
        return
    }
    val maxRevenue = series.maxOf { it.revenue }.coerceAtLeast(1.0)
    val barColor = LsRoleAdmin
    val axisColor = MaterialTheme.colorScheme.outlineVariant
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawLine(axisColor, Offset(0f, h), Offset(w, h), strokeWidth = 2f)
        val slot = w / series.size
        val barWidth = (slot * 0.6f).coerceAtLeast(2f)
        series.forEachIndexed { i, point ->
            val barHeight = (point.revenue / maxRevenue).toFloat() * (h - 4f)
            val left = i * slot + (slot - barWidth) / 2f
            drawRect(
                color = barColor,
                topLeft = Offset(left, h - barHeight),
                size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
            )
        }
    }
}
