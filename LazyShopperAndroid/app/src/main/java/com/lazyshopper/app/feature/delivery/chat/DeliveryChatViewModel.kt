package com.lazyshopper.app.feature.delivery.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lazyshopper.app.core.data.remote.ApiResult
import com.lazyshopper.app.core.data.remote.api.ChatApi
import com.lazyshopper.app.core.data.remote.dto.ChatMessage
import com.lazyshopper.app.core.data.remote.dto.ChatSend
import com.lazyshopper.app.core.data.remote.safeApiCall
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val messages: List<ChatMessage> = emptyList(),
    val customerName: String? = null,
    val input: String = "",
    val sending: Boolean = false,
)

@HiltViewModel
class DeliveryChatViewModel @Inject constructor(
    private val chatApi: ChatApi,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val orderId: String = checkNotNull(savedStateHandle["orderId"])

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    private var pollingJob: Job? = null

    init {
        refresh(showLoading = true)
        startPolling()
    }

    private fun refresh(showLoading: Boolean) {
        viewModelScope.launch {
            if (showLoading) _state.update { it.copy(loading = true) }
            when (val result = safeApiCall { chatApi.orderChat(orderId) }) {
                is ApiResult.Success -> _state.update {
                    it.copy(loading = false, messages = result.data.messages, customerName = result.data.customer_name, error = null)
                }
                is ApiResult.Failure -> _state.update { it.copy(loading = false, error = result.message) }
            }
        }
    }

    private fun startPolling() {
        pollingJob = viewModelScope.launch {
            while (isActive) {
                delay(5_000)
                refresh(showLoading = false)
            }
        }
    }

    fun setInput(v: String) = _state.update { it.copy(input = v) }

    fun send() {
        val text = _state.value.input.trim()
        if (text.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(sending = true) }
            when (val result = safeApiCall { chatApi.sendOrderChat(orderId, ChatSend(text)) }) {
                is ApiResult.Success -> _state.update { it.copy(sending = false, input = "", messages = it.messages + result.data) }
                is ApiResult.Failure -> _state.update { it.copy(sending = false, error = result.message) }
            }
        }
    }
}
