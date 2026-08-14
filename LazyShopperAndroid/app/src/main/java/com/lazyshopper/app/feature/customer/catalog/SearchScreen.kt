package com.lazyshopper.app.feature.customer.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lazyshopper.app.core.data.remote.ApiResult
import com.lazyshopper.app.core.data.remote.dto.Product
import com.lazyshopper.app.core.ui.UiState
import com.lazyshopper.app.core.ui.components.EmptyState
import com.lazyshopper.app.core.ui.components.ErrorState
import com.lazyshopper.app.core.ui.components.FullScreenLoading
import com.lazyshopper.app.feature.customer.home.LocationPrefStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: CatalogRepository,
    private val locationStore: LocationPrefStore,
) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _results = MutableStateFlow<UiState<List<Product>>>(UiState.Idle)
    val results: StateFlow<UiState<List<Product>>> = _results.asStateFlow()

    init {
        viewModelScope.launch {
            _query.debounce(400).distinctUntilChanged().collectLatest { q ->
                val trimmed = q.trim()
                if (trimmed.isBlank()) {
                    _results.value = UiState.Idle
                    return@collectLatest
                }
                _results.value = UiState.Loading
                val loc = locationStore.flow.first()
                when (val r = repository.search(trimmed, loc.state.ifBlank { null }, loc.district.ifBlank { null })) {
                    is ApiResult.Success -> _results.value = UiState.Success(r.data)
                    is ApiResult.Failure -> _results.value = UiState.Error(r.message)
                }
            }
        }
    }

    fun setQuery(v: String) {
        _query.value = v
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onProductClick: (Product) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val query by viewModel.query.collectAsState()
    val results by viewModel.results.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = query,
                        onValueChange = viewModel::setQuery,
                        placeholder = { Text("Search for groceries, sweets…") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } },
            )
        },
    ) { padding ->
        when (val s = results) {
            UiState.Idle -> EmptyState("Start typing to search products across nearby stores.", androidx.compose.ui.Modifier.padding(padding))
            is UiState.Loading -> FullScreenLoading(androidx.compose.ui.Modifier.padding(padding))
            is UiState.Error -> ErrorState(s.message, androidx.compose.ui.Modifier.padding(padding))
            is UiState.Success -> {
                if (s.data.isEmpty()) {
                    EmptyState("No products found for \"$query\".", androidx.compose.ui.Modifier.padding(padding))
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = androidx.compose.ui.Modifier.padding(padding),
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(s.data, key = { it.id }) { product ->
                            ProductCard(product = product, onClick = { onProductClick(product) })
                        }
                    }
                }
            }
        }
    }
}
