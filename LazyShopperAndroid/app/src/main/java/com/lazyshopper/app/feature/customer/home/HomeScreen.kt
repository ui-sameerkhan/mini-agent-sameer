package com.lazyshopper.app.feature.customer.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.lazyshopper.app.core.data.remote.dto.Banner
import com.lazyshopper.app.core.data.remote.dto.Offer
import com.lazyshopper.app.core.data.remote.dto.Product
import com.lazyshopper.app.core.data.remote.dto.Shop
import com.lazyshopper.app.core.theme.LsAccent
import com.lazyshopper.app.core.theme.LsMuted
import com.lazyshopper.app.core.theme.LsPrimary
import com.lazyshopper.app.core.ui.UiState
import com.lazyshopper.app.core.ui.components.ErrorState
import com.lazyshopper.app.core.ui.components.FullScreenLoading
import com.lazyshopper.app.feature.customer.catalog.ProductCard
import com.lazyshopper.app.feature.customer.catalog.ProductDetailSheet
import com.lazyshopper.app.feature.customer.catalog.ShopRow
import com.lazyshopper.app.feature.customer.util.fileUrl
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onSearchClick: () -> Unit,
    onCartClick: () -> Unit,
    onCategoryClick: (String) -> Unit,
    onShopClick: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val cartLines by viewModel.cartRepository.lines.collectAsState()
    var selectedProduct by remember { mutableStateOf<Product?>(null) }

    LaunchedEffect(Unit) { viewModel.load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    TextButton(onClick = viewModel::openLocationPicker) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = LsPrimary)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            state.location.label.ifBlank { "Select your location" },
                            maxLines = 1,
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onSearchClick) { Icon(Icons.Default.Search, contentDescription = "Search") }
                    val cartCount = cartLines.values.sumOf { it.qty }.toInt()
                    IconButton(onClick = onCartClick) {
                        BadgedBox(badge = { if (cartCount > 0) Badge { Text(cartCount.toString()) } }) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = "Cart")
                        }
                    }
                },
            )
        },
    ) { padding ->
        when (val homeState = state.home) {
            is UiState.Loading -> FullScreenLoading(Modifier.padding(padding))
            is UiState.Error -> ErrorState(homeState.message, Modifier.padding(padding), onRetry = viewModel::refresh)
            is UiState.Success -> {
                val data = homeState.data
                LazyColumn(modifier = Modifier.padding(padding), contentPadding = PaddingValues(bottom = 24.dp)) {
                    if (state.banners.isNotEmpty()) {
                        item { BannerCarousel(state.banners) }
                    }
                    if (state.liveOffers.isNotEmpty()) {
                        item { LiveOffersStrip(state.liveOffers) }
                    }
                    if (state.categories.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(12.dp))
                            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(state.categories, key = { it.id }) { cat ->
                                    FilterChip(selected = false, onClick = { onCategoryClick(cat.key) }, label = { Text(cat.label) })
                                }
                            }
                        }
                    }

                    if (data.nearby_stores.isNotEmpty()) {
                        item { ShopCarousel("Nearby stores", data.nearby_stores, onShopClick) }
                    }
                    if (data.promoted_shops.isNotEmpty()) {
                        item { ShopCarousel("Promoted stores", data.promoted_shops, onShopClick) }
                    }
                    if (data.featured_shops.isNotEmpty()) {
                        item { ShopCarousel("Featured stores", data.featured_shops, onShopClick) }
                    }
                    if (data.best_selling.isNotEmpty()) {
                        item { ProductSection("Best selling", data.best_selling, onClick = { selectedProduct = it }, onAddToCart = viewModel::addToCart) }
                    }
                    if (data.products.isNotEmpty()) {
                        item { ProductSection("Popular near you", data.products, onClick = { selectedProduct = it }, onAddToCart = viewModel::addToCart) }
                    }
                    if (data.sweets.isNotEmpty()) {
                        item { ProductSection("Sweets & treats", data.sweets, onClick = { selectedProduct = it }, onAddToCart = viewModel::addToCart) }
                    }
                }
            }
            UiState.Idle -> {}
        }
    }

    selectedProduct?.let { product -> ProductDetailSheet(product = product, onDismiss = { selectedProduct = null }) }

    if (state.showLocationPicker) {
        LocationPickerDialog(
            options = state.locationOptions,
            current = state.location,
            onDismiss = viewModel::dismissLocationPicker,
            onConfirm = viewModel::setLocation,
        )
    }
}

@Composable
private fun BannerCarousel(banners: List<Banner>) {
    val pagerState = rememberPagerState(pageCount = { banners.size })
    LaunchedEffect(banners.size) {
        while (true) {
            delay(4000)
            if (banners.isNotEmpty()) {
                val next = (pagerState.currentPage + 1) % banners.size
                pagerState.animateScrollToPage(next)
            }
        }
    }
    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) { page ->
        val banner = banners[page]
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .aspectRatio(16f / 7f),
            shape = RoundedCornerShape(20.dp),
        ) {
            Box {
                AsyncImage(
                    model = banner.image_url ?: fileUrl(banner.image_id),
                    contentDescription = banner.title,
                    modifier = Modifier.fillMaxSize().background(LsMuted),
                    contentScale = ContentScale.Crop,
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.35f))
                        .padding(10.dp),
                ) {
                    Text(banner.title, color = androidx.compose.ui.graphics.Color.White, style = MaterialTheme.typography.titleSmall)
                    if (!banner.subtitle.isNullOrBlank()) {
                        Text(banner.subtitle, color = androidx.compose.ui.graphics.Color.White, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveOffersStrip(offers: List<Offer>) {
    val pagerState = rememberPagerState(pageCount = { offers.size })
    LaunchedEffect(offers.size) {
        while (true) {
            delay(3500)
            if (offers.isNotEmpty()) {
                val next = (pagerState.currentPage + 1) % offers.size
                pagerState.animateScrollToPage(next)
            }
        }
    }
    Column(modifier = Modifier.padding(top = 4.dp)) {
        Text("Live offers", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp))
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), pageSpacing = 8.dp) { page ->
            val offer = offers[page]
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(70.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Row(modifier = Modifier.fillMaxSize().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape).background(LsAccent),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            if (offer.discount_type == "percent") "${offer.value.toInt()}%" else "₹${offer.value.toInt()}",
                            color = androidx.compose.ui.graphics.Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(offer.title, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                        if (offer.min_order > 0.0) {
                            Text("On orders above ₹${offer.min_order.toInt()}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShopCarousel(title: String, shops: List<Shop>, onShopClick: (String) -> Unit) {
    Column(modifier = Modifier.padding(top = 12.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(Modifier.height(8.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(shops, key = { it.id }) { shop ->
                Box(modifier = Modifier.width(240.dp)) { ShopRow(shop) { onShopClick(shop.id) } }
            }
        }
    }
}

@Composable
private fun ProductSection(title: String, products: List<Product>, onClick: (Product) -> Unit, onAddToCart: (Product) -> Unit) {
    Column(modifier = Modifier.padding(top = 12.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(Modifier.height(8.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(products, key = { it.id }) { product ->
                ProductCard(
                    product = product,
                    onClick = { onClick(product) },
                    onAddToCart = { onAddToCart(product) },
                    modifier = Modifier.width(160.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocationPickerDialog(
    options: List<com.lazyshopper.app.core.data.remote.dto.StateLocations>,
    current: LocationSelection,
    onDismiss: () -> Unit,
    onConfirm: (state: String, district: String, area: String) -> Unit,
) {
    var selectedState by remember(current) { mutableStateOf(current.state) }
    var selectedDistrict by remember(current) { mutableStateOf(current.district) }
    var selectedArea by remember(current) { mutableStateOf(current.area) }
    var stateExpanded by remember { mutableStateOf(false) }
    var districtExpanded by remember { mutableStateOf(false) }
    var areaExpanded by remember { mutableStateOf(false) }

    val districts = options.firstOrNull { it.state == selectedState }?.districts.orEmpty()
    val areas = districts.firstOrNull { it.district == selectedDistrict }?.areas.orEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose your location") },
        text = {
            Column {
                ExposedDropdownMenuBox(expanded = stateExpanded, onExpandedChange = { stateExpanded = it }) {
                    OutlinedTextField(
                        value = selectedState,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("State") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = stateExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchorCompat(),
                    )
                    ExposedDropdownMenuDefaults.let {
                        androidx.compose.material3.ExposedDropdownMenu(expanded = stateExpanded, onDismissRequest = { stateExpanded = false }) {
                            options.forEach { opt ->
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text(opt.state) },
                                    onClick = {
                                        selectedState = opt.state
                                        selectedDistrict = ""
                                        selectedArea = ""
                                        stateExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                ExposedDropdownMenuBox(expanded = districtExpanded, onExpandedChange = { if (districts.isNotEmpty()) districtExpanded = it }) {
                    OutlinedTextField(
                        value = selectedDistrict,
                        onValueChange = {},
                        readOnly = true,
                        enabled = districts.isNotEmpty(),
                        label = { Text("District") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = districtExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchorCompat(),
                    )
                    androidx.compose.material3.ExposedDropdownMenu(expanded = districtExpanded, onDismissRequest = { districtExpanded = false }) {
                        districts.forEach { d ->
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text(d.district) },
                                onClick = {
                                    selectedDistrict = d.district
                                    selectedArea = ""
                                    districtExpanded = false
                                },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                ExposedDropdownMenuBox(expanded = areaExpanded, onExpandedChange = { if (areas.isNotEmpty()) areaExpanded = it }) {
                    OutlinedTextField(
                        value = selectedArea,
                        onValueChange = {},
                        readOnly = true,
                        enabled = areas.isNotEmpty(),
                        label = { Text("Area") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = areaExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchorCompat(),
                    )
                    androidx.compose.material3.ExposedDropdownMenu(expanded = areaExpanded, onDismissRequest = { areaExpanded = false }) {
                        areas.forEach { a ->
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text(a) },
                                onClick = { selectedArea = a; areaExpanded = false },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedState, selectedDistrict, selectedArea) },
                enabled = selectedState.isNotBlank(),
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
private fun Modifier.menuAnchorCompat(): Modifier = this
