package com.lazyshopper.app.feature.customer.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lazyshopper.app.core.data.remote.ApiResult
import com.lazyshopper.app.core.data.remote.dto.Banner
import com.lazyshopper.app.core.data.remote.dto.Category
import com.lazyshopper.app.core.data.remote.dto.HomeResponse
import com.lazyshopper.app.core.data.remote.dto.Offer
import com.lazyshopper.app.core.data.remote.dto.Product
import com.lazyshopper.app.core.data.remote.dto.StateLocations
import com.lazyshopper.app.core.ui.UiState
import com.lazyshopper.app.feature.customer.cart.CartRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val home: UiState<HomeResponse> = UiState.Loading,
    val liveOffers: List<Offer> = emptyList(),
    val banners: List<Banner> = emptyList(),
    val categories: List<Category> = emptyList(),
    val location: LocationSelection = LocationSelection(),
    val locationOptions: List<StateLocations> = emptyList(),
    val showLocationPicker: Boolean = false,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: HomeRepository,
    private val locationStore: LocationPrefStore,
    val cartRepository: CartRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            val loc = locationStore.flow.first()
            _state.value = _state.value.copy(location = loc)
            refresh(loc)
        }
    }

    fun refresh() {
        viewModelScope.launch { refresh(_state.value.location) }
    }

    private suspend fun refresh(loc: LocationSelection) {
        _state.value = _state.value.copy(home = UiState.Loading)
        when (val r = repository.home(loc.state.ifBlank { null }, loc.district.ifBlank { null }, loc.area.ifBlank { null })) {
            is ApiResult.Success -> _state.value = _state.value.copy(home = UiState.Success(r.data))
            is ApiResult.Failure -> _state.value = _state.value.copy(home = UiState.Error(r.message))
        }
        (repository.liveOffers() as? ApiResult.Success)?.let { _state.value = _state.value.copy(liveOffers = it.data) }
        (repository.banners() as? ApiResult.Success)?.let { _state.value = _state.value.copy(banners = it.data) }
        if (_state.value.categories.isEmpty()) {
            (repository.categories() as? ApiResult.Success)?.let { _state.value = _state.value.copy(categories = it.data.filter { c -> c.active }) }
        }
    }

    fun openLocationPicker() {
        viewModelScope.launch {
            _state.value = _state.value.copy(showLocationPicker = true)
            if (_state.value.locationOptions.isEmpty()) {
                (repository.locations() as? ApiResult.Success)?.let { _state.value = _state.value.copy(locationOptions = it.data) }
            }
        }
    }

    fun dismissLocationPicker() {
        _state.value = _state.value.copy(showLocationPicker = false)
    }

    fun setLocation(state: String, district: String, area: String) {
        viewModelScope.launch {
            locationStore.save(state, district, area)
            val loc = LocationSelection(state, district, area)
            _state.value = _state.value.copy(location = loc, showLocationPicker = false)
            repository.saveLocationPref(state, district, area)
            refresh(loc)
        }
    }

    fun addToCart(product: Product) = cartRepository.addToCart(product)

    fun onBannerClicked(bannerId: String) {
        viewModelScope.launch { repository.trackBannerClick(bannerId) }
    }
}
