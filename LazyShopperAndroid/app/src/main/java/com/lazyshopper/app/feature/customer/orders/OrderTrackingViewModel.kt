package com.lazyshopper.app.feature.customer.orders

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lazyshopper.app.core.data.remote.ApiResult
import com.lazyshopper.app.core.data.remote.dto.TrackingResponse
import com.lazyshopper.app.core.ui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OrderTrackingViewModel @Inject constructor(
    private val repository: OrdersRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val orderId: String = checkNotNull(savedStateHandle["orderId"])

    private val _tracking = MutableStateFlow<UiState<TrackingResponse>>(UiState.Loading)
    val tracking: StateFlow<UiState<TrackingResponse>> = _tracking.asStateFlow()

    private var pollingJob: Job? = null

    init {
        refresh(showLoading = true)
        startPolling()
    }

    fun refresh() = refresh(showLoading = false)

    private fun refresh(showLoading: Boolean) {
        viewModelScope.launch {
            if (showLoading) _tracking.value = UiState.Loading
            when (val r = repository.tracking(orderId)) {
                is ApiResult.Success -> _tracking.value = UiState.Success(r.data)
                is ApiResult.Failure -> if (_tracking.value !is UiState.Success) _tracking.value = UiState.Error(r.message)
            }
        }
    }

    private fun startPolling() {
        pollingJob = viewModelScope.launch {
            while (isActive) {
                delay(10_000)
                refresh(showLoading = false)
            }
        }
    }

    override fun onCleared() {
        pollingJob?.cancel()
        super.onCleared()
    }
}
