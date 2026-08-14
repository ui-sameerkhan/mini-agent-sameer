package com.lazyshopper.app.feature.admin.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lazyshopper.app.core.data.remote.ApiResult
import com.lazyshopper.app.core.data.remote.dto.AdminStats
import com.lazyshopper.app.core.ui.UiState
import com.lazyshopper.app.feature.admin.common.AdminScreenScaffold
import com.lazyshopper.app.feature.admin.common.EmptyOrList
import com.lazyshopper.app.feature.admin.common.StatTile
import com.lazyshopper.app.feature.admin.common.money
import com.lazyshopper.app.feature.admin.data.AdminDashboardRepository
import com.lazyshopper.app.feature.admin.nav.AdminRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(private val repo: AdminDashboardRepository) : ViewModel() {
    private val _state = MutableStateFlow<UiState<AdminStats>>(UiState.Loading)
    val state: StateFlow<UiState<AdminStats>> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            when (val r = repo.stats()) {
                is ApiResult.Success -> _state.value = UiState.Success(r.data)
                is ApiResult.Failure -> _state.value = UiState.Error(r.message)
            }
        }
    }
}

/** Overview / stats tiles with quick links into the busiest approval queues. */
@Composable
fun DashboardScreen(
    onMenuClick: () -> Unit,
    onNavigate: (String) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }

    AdminScreenScaffold(title = "Dashboard", onMenuClick = onMenuClick) { padding ->
        val s = state
        EmptyOrList(
            loading = s is UiState.Loading,
            error = (s as? UiState.Error)?.message,
            isEmpty = false,
            emptyMessage = "",
            onRetry = viewModel::load,
        ) {
            val stats = (s as? UiState.Success)?.data ?: AdminStats()
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxWidth().padding(padding).padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                    Text("Overview", style = MaterialTheme.typography.titleMedium)
                }
                items(overviewTiles(stats)) { t ->
                    StatTile(t.title, t.value, modifier = Modifier.fillMaxWidth())
                }
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                    Text("Needs your attention", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
                }
                items(queueTiles(stats)) { t ->
                    StatTile(t.title, t.value, modifier = Modifier.fillMaxWidth(), onClick = { onNavigate(t.route) })
                }
            }
        }
    }
}

private data class Tile(val title: String, val value: String, val route: String = "")

private fun overviewTiles(s: AdminStats) = listOf(
    Tile("Revenue", s.revenue.money()),
    Tile("Total orders", s.total_orders.toString()),
    Tile("Total users", s.total_users.toString()),
    Tile("Customers", s.total_customers.toString()),
    Tile("Shopkeepers", s.total_shopkeepers.toString()),
    Tile("Total shops", s.total_shops.toString()),
    Tile("Total products", s.total_products.toString()),
    Tile("Delivery partners", s.total_delivery.toString()),
)

private fun queueTiles(s: AdminStats) = listOf(
    Tile("Pending shops", s.pending_shops.toString(), AdminRoutes.SHOPS),
    Tile("Pending products", s.pending_products.toString(), AdminRoutes.PRODUCTS),
    Tile("Pending shopkeepers", s.pending_shopkeepers.toString(), AdminRoutes.USERS),
    Tile("Pending delivery", s.pending_delivery.toString(), AdminRoutes.KYC),
)
