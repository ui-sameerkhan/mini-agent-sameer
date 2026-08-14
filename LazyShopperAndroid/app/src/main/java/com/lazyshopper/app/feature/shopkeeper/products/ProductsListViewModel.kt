package com.lazyshopper.app.feature.shopkeeper.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lazyshopper.app.core.data.remote.ApiResult
import com.lazyshopper.app.core.data.remote.dto.Product
import com.lazyshopper.app.core.ui.ActionState
import com.lazyshopper.app.core.ui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProductsListUiState(
    val products: UiState<List<Product>> = UiState.Loading,
    val deleteState: ActionState = ActionState.Idle,
)

@HiltViewModel
class ProductsListViewModel @Inject constructor(
    private val repository: ProductsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ProductsListUiState())
    val state: StateFlow<ProductsListUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(products = UiState.Loading) }
            when (val result = repository.myProducts()) {
                is ApiResult.Success -> _state.update {
                    it.copy(products = UiState.Success(result.data.sortedByDescending { p -> p.created_at }))
                }
                is ApiResult.Failure -> _state.update { it.copy(products = UiState.Error(result.message)) }
            }
        }
    }

    fun deleteProduct(id: String) {
        viewModelScope.launch {
            _state.update { it.copy(deleteState = ActionState.InFlight) }
            when (val result = repository.deleteProduct(id)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(deleteState = ActionState.Done) }
                    load()
                }
                is ApiResult.Failure -> _state.update { it.copy(deleteState = ActionState.Failed(result.message)) }
            }
        }
    }
}
