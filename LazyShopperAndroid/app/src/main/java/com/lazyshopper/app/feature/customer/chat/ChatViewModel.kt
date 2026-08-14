package com.lazyshopper.app.feature.customer.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lazyshopper.app.core.data.remote.ApiResult
import com.lazyshopper.app.core.data.remote.dto.ChatMessage
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
    val title: String = "Chat",
    val input: String = "",
    val sending: Boolean = false,
)

/**
 * One reusable chat surface for both threads the customer app exposes: order chat (rider <->
 * customer, scoped to a single order) and support chat (customer <-> admin, one thread per
 * customer). Which one is active is decided purely by whether the nav route supplied an
 * `orderId` arg — [OrderChatScreen]/[SupportChatScreen] below are just entry points that make
 * that explicit at the call site instead of every navigation wiring having to know the trick.
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: ChatRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val orderId: String? = savedStateHandle["orderId"]
    val isOrderChat: Boolean = orderId != null

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
            val oid = orderId
            if (oid != null) {
                when (val r = repository.orderChat(oid)) {
                    is ApiResult.Success -> _state.update {
                        it.copy(
                            loading = false,
                            error = null,
                            messages = r.data.messages,
                            title = r.data.rider_name?.let { name -> "Chat with $name" } ?: "Order chat",
                        )
                    }
                    is ApiResult.Failure -> _state.update { it.copy(loading = false, error = r.message) }
                }
            } else {
                when (val r = repository.supportChat()) {
                    is ApiResult.Success -> _state.update {
                        it.copy(loading = false, error = null, messages = r.data.messages, title = "Support chat")
                    }
                    is ApiResult.Failure -> _state.update { it.copy(loading = false, error = r.message) }
                }
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
            val oid = orderId
            val result = if (oid != null) repository.sendOrderChat(oid, text) else repository.sendSupportChat(text)
            when (result) {
                is ApiResult.Success -> _state.update { it.copy(sending = false, input = "", messages = it.messages + result.data) }
                is ApiResult.Failure -> _state.update { it.copy(sending = false, error = result.message) }
            }
        }
    }

    override fun onCleared() {
        pollingJob?.cancel()
        super.onCleared()
    }
}
