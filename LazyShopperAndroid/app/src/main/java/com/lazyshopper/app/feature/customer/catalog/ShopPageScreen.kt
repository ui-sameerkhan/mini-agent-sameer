package com.lazyshopper.app.feature.customer.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.item
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lazyshopper.app.core.data.remote.ApiResult
import com.lazyshopper.app.core.data.remote.dto.Product
import com.lazyshopper.app.core.data.remote.dto.Review
import com.lazyshopper.app.core.data.remote.dto.ReviewsResponse
import com.lazyshopper.app.core.data.remote.dto.Shop
import com.lazyshopper.app.core.theme.LsAccent
import com.lazyshopper.app.core.ui.UiState
import com.lazyshopper.app.core.ui.components.ErrorState
import com.lazyshopper.app.core.ui.components.FullScreenLoading
import com.lazyshopper.app.feature.customer.cart.CartRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ShopPageUiState(
    val shop: UiState<Shop> = UiState.Loading,
    val products: UiState<List<Product>> = UiState.Loading,
    val reviews: UiState<ReviewsResponse> = UiState.Idle,
    val videos: List<Review> = emptyList(),
)

@HiltViewModel
class ShopPageViewModel @Inject constructor(
    private val repository: CatalogRepository,
    val cartRepository: CartRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val shopId: String = savedStateHandle.get<String>("shopId").orEmpty()

    private val _state = MutableStateFlow(ShopPageUiState())
    val state: StateFlow<ShopPageUiState> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(shop = UiState.Loading, products = UiState.Loading, reviews = UiState.Loading)
            when (val r = repository.shop(shopId)) {
                is ApiResult.Success -> _state.value = _state.value.copy(shop = UiState.Success(r.data))
                is ApiResult.Failure -> _state.value = _state.value.copy(shop = UiState.Error(r.message))
            }
            when (val r = repository.products(shopId = shopId)) {
                is ApiResult.Success -> _state.value = _state.value.copy(products = UiState.Success(r.data))
                is ApiResult.Failure -> _state.value = _state.value.copy(products = UiState.Error(r.message))
            }
            when (val r = repository.reviews("shop", shopId)) {
                is ApiResult.Success -> _state.value = _state.value.copy(reviews = UiState.Success(r.data))
                is ApiResult.Failure -> _state.value = _state.value.copy(reviews = UiState.Error(r.message))
            }
            when (val r = repository.videos("shop", shopId)) {
                is ApiResult.Success -> _state.value = _state.value.copy(videos = r.data)
                is ApiResult.Failure -> {}
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopPageScreen(
    onBack: () -> Unit,
    viewModel: ShopPageViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var selectedProduct by remember { mutableStateOf<Product?>(null) }

    LaunchedEffect(Unit) { viewModel.load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text((state.shop as? UiState.Success)?.data?.name ?: "Shop") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } },
            )
        },
    ) { padding ->
        when (val shopState = state.shop) {
            is UiState.Loading -> FullScreenLoading(Modifier.padding(padding))
            is UiState.Error -> ErrorState(shopState.message, Modifier.padding(padding), onRetry = viewModel::load)
            is UiState.Success -> {
                val shop = shopState.data
                LazyColumn(modifier = Modifier.padding(padding), contentPadding = PaddingValues(bottom = 24.dp)) {
                    item {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                Text(shop.category.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (shop.avg_rating != null && shop.avg_rating > 0.0) {
                                    Spacer(Modifier.height(0.dp))
                                    Text("  •  ", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Icon(Icons.Default.Star, contentDescription = null, tint = LsAccent, modifier = Modifier.height(14.dp))
                                    Text(" %.1f (${shop.review_count ?: 0})".format(shop.avg_rating), style = MaterialTheme.typography.labelMedium)
                                }
                            }
                            Text("${shop.area}, ${shop.district}, ${shop.state}", style = MaterialTheme.typography.bodySmall)
                            if (shop.open_time != null && shop.close_time != null) {
                                Text("Open ${shop.open_time} – ${shop.close_time}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        HorizontalDivider()
                    }

                    item {
                        Text("Products", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))
                    }

                    when (val productsState = state.products) {
                        is UiState.Loading -> item { FullScreenLoading(Modifier.height(200.dp)) }
                        is UiState.Error -> item { ErrorState(productsState.message) }
                        is UiState.Success -> {
                            if (productsState.data.isEmpty()) {
                                item { Text("No products listed yet.", modifier = Modifier.padding(16.dp)) }
                            } else {
                                item {
                                    ProductGridStatic(
                                        products = productsState.data,
                                        onProductClick = { selectedProduct = it },
                                        onAddToCart = { viewModel.cartRepository.addToCart(it) },
                                    )
                                }
                            }
                        }
                        UiState.Idle -> {}
                    }

                    item {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Text("Reviews", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))
                    }
                    when (val reviewsState = state.reviews) {
                        is UiState.Success -> {
                            val data = reviewsState.data
                            if (data.reviews.isEmpty()) {
                                item { Text("No reviews yet.", modifier = Modifier.padding(horizontal = 16.dp)) }
                            } else {
                                item {
                                    Text(
                                        "★ %.1f from ${data.count} review${if (data.count == 1) "" else "s"}".format(data.average),
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                                items(data.reviews.take(20), key = { it.id }) { review -> ReviewRow(review) }
                            }
                        }
                        else -> {}
                    }
                }
            }
            UiState.Idle -> {}
        }
    }

    selectedProduct?.let { product ->
        ProductDetailSheet(product = product, onDismiss = { selectedProduct = null })
    }
}

@Composable
fun ProductGridStatic(products: List<Product>, onProductClick: (Product) -> Unit, onAddToCart: (Product) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxWidth()
            .height((((products.size + 1) / 2) * 240).dp),
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        userScrollEnabled = false,
    ) {
        gridItems(products, key = { it.id }) { product ->
            ProductCard(product = product, onClick = { onProductClick(product) }, onAddToCart = { onAddToCart(product) })
        }
    }
}

@Composable
fun ReviewRow(review: Review) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text(review.user_name ?: "Customer", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(0.dp))
            Text("  " + "★".repeat(review.rating), color = LsAccent, style = MaterialTheme.typography.labelMedium)
        }
        if (!review.comment.isNullOrBlank()) {
            Text(review.comment, style = MaterialTheme.typography.bodySmall)
        }
    }
}
