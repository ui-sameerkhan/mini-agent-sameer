package com.lazyshopper.app.feature.customer.catalog

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lazyshopper.app.core.data.remote.ApiResult
import com.lazyshopper.app.core.data.remote.dto.Category
import com.lazyshopper.app.core.ui.UiState
import com.lazyshopper.app.core.ui.components.ErrorState
import com.lazyshopper.app.core.ui.components.FullScreenLoading
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val repository: CatalogRepository,
) : ViewModel() {
    private val _state = MutableStateFlow<UiState<List<Category>>>(UiState.Loading)
    val state: StateFlow<UiState<List<Category>>> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            when (val r = repository.categories()) {
                is ApiResult.Success -> _state.value = UiState.Success(r.data.filter { it.active })
                is ApiResult.Failure -> _state.value = UiState.Error(r.message)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    onCategoryClick: (key: String) -> Unit,
    viewModel: CategoriesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }

    Scaffold(topBar = { TopAppBar(title = { Text("Categories") }) }) { padding ->
        when (val s = state) {
            is UiState.Loading -> FullScreenLoading(Modifier.padding(padding))
            is UiState.Error -> ErrorState(s.message, Modifier.padding(padding), onRetry = viewModel::load)
            is UiState.Success -> LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
            ) {
                items(s.data, key = { it.id }) { category ->
                    Card(
                        modifier = Modifier.padding(8.dp),
                        shape = RoundedCornerShape(18.dp),
                        onClick = { onCategoryClick(category.key) },
                    ) {
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier.padding(24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(category.label, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
            UiState.Idle -> {}
        }
    }
}
