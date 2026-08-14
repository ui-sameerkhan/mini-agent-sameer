package com.lazyshopper.app.feature.customer.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lazyshopper.app.core.data.remote.ApiResult
import com.lazyshopper.app.core.data.remote.dto.Shop
import com.lazyshopper.app.core.theme.LsAccent
import com.lazyshopper.app.core.ui.UiState
import com.lazyshopper.app.core.ui.components.EmptyState
import com.lazyshopper.app.core.ui.components.ErrorState
import com.lazyshopper.app.core.ui.components.FullScreenLoading
import com.lazyshopper.app.feature.customer.home.LocationPrefStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoryShopsViewModel @Inject constructor(
    private val repository: CatalogRepository,
    private val locationStore: LocationPrefStore,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val categoryKey: String = savedStateHandle.get<String>("categoryKey").orEmpty()

    private val _state = MutableStateFlow<UiState<List<Shop>>>(UiState.Loading)
    val state: StateFlow<UiState<List<Shop>>> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            val loc = locationStore.flow.first()
            when (
                val r = repository.shops(
                    category = categoryKey,
                    state = loc.state.ifBlank { null },
                    district = loc.district.ifBlank { null },
                    area = loc.area.ifBlank { null },
                )
            ) {
                is ApiResult.Success -> _state.value = UiState.Success(r.data)
                is ApiResult.Failure -> _state.value = UiState.Error(r.message)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryShopsScreen(
    onBack: () -> Unit,
    onShopClick: (String) -> Unit,
    viewModel: CategoryShopsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(viewModel.categoryKey.replaceFirstChar { it.uppercase() }) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } },
            )
        },
    ) { padding ->
        when (val s = state) {
            is UiState.Loading -> FullScreenLoading(Modifier.padding(padding))
            is UiState.Error -> ErrorState(s.message, Modifier.padding(padding), onRetry = viewModel::load)
            is UiState.Success -> {
                if (s.data.isEmpty()) {
                    EmptyState("No stores found in this category near you yet.", Modifier.padding(padding))
                } else {
                    LazyColumn(
                        modifier = Modifier.padding(padding),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(s.data, key = { it.id }) { shop -> ShopRow(shop) { onShopClick(shop.id) } }
                    }
                }
            }
            UiState.Idle -> {}
        }
    }
}

@Composable
fun ShopRow(shop: Shop, onClick: () -> Unit) {
    Card(shape = RoundedCornerShape(16.dp), onClick = onClick) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(shop.name, style = MaterialTheme.typography.titleSmall)
                    if (shop.promoted == true) {
                        Text(" · Promoted", style = MaterialTheme.typography.labelSmall, color = LsAccent)
                    }
                }
                Text("${shop.area}, ${shop.district}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                shop.product_count?.let { Text("$it products", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            if (shop.avg_rating != null && shop.avg_rating > 0.0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = LsAccent, modifier = Modifier.size(16.dp))
                    Text(" %.1f".format(shop.avg_rating), style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
