package com.lazyshopper.app.feature.admin.support

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lazyshopper.app.core.data.remote.ApiResult
import com.lazyshopper.app.core.data.remote.dto.ChatMessage
import com.lazyshopper.app.core.data.remote.dto.SupportThread
import com.lazyshopper.app.core.theme.LsRoleAdmin
import com.lazyshopper.app.core.ui.UiState
import com.lazyshopper.app.feature.admin.common.AdminLazyList
import com.lazyshopper.app.feature.admin.common.AdminScreenScaffold
import com.lazyshopper.app.feature.admin.common.CompactRow
import com.lazyshopper.app.feature.admin.common.EmptyOrList
import com.lazyshopper.app.feature.admin.common.StatusChip
import com.lazyshopper.app.feature.admin.common.Tone
import com.lazyshopper.app.core.ui.components.FullScreenLoading
import com.lazyshopper.app.feature.admin.data.AdminSupportRepository
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

// ---------- Threads inbox ----------

@HiltViewModel
class SupportThreadsViewModel @Inject constructor(private val repo: AdminSupportRepository) : ViewModel() {
    private val _state = MutableStateFlow<UiState<List<SupportThread>>>(UiState.Loading)
    val state: StateFlow<UiState<List<SupportThread>>> = _state.asStateFlow()
    private var pollingJob: Job? = null

    fun load(showLoading: Boolean = true) {
        viewModelScope.launch {
            if (showLoading) _state.value = UiState.Loading
            when (val r = repo.threads()) {
                is ApiResult.Success -> _state.value = UiState.Success(r.data)
                is ApiResult.Failure -> if (showLoading) _state.value = UiState.Error(r.message)
            }
        }
    }

    fun startPolling() {
        if (pollingJob?.isActive == true) return
        pollingJob = viewModelScope.launch {
            while (isActive) {
                delay(5_000)
                load(showLoading = false)
            }
        }
    }

    override fun onCleared() {
        pollingJob?.cancel()
    }
}

@Composable
fun AdminSupportThreadsScreen(
    onMenuClick: () -> Unit,
    onOpenThread: (String, String) -> Unit,
    viewModel: SupportThreadsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.load(); viewModel.startPolling() }

    AdminScreenScaffold(title = "Support Inbox", onMenuClick = onMenuClick) { padding ->
        val s = state
        EmptyOrList(
            loading = s is UiState.Loading,
            error = (s as? UiState.Error)?.message,
            isEmpty = (s as? UiState.Success)?.data.isNullOrEmpty() && s is UiState.Success,
            emptyMessage = "No customer conversations yet",
            onRetry = { viewModel.load() },
        ) {
            val threads = (s as UiState.Success).data
            Column(Modifier.padding(padding)) {
                AdminLazyList {
                    items(threads, key = { it.customer_id }) { t ->
                        CompactRow(
                            title = t.customer_name ?: t.customer_email ?: t.customer_id,
                            subtitle = t.last ?: "No messages yet",
                            meta = t.last_at ?: "",
                            badge = if (t.unread > 0) {
                                { StatusChip("${t.unread} new", tone = Tone.WARNING) }
                            } else null,
                            onClick = { onOpenThread(t.customer_id, t.customer_name ?: t.customer_email ?: "Customer") },
                        )
                    }
                }
            }
        }
    }
}

// ---------- Thread conversation ----------

data class SupportChatUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val messages: List<ChatMessage> = emptyList(),
    val input: String = "",
    val sending: Boolean = false,
)

@HiltViewModel
class SupportThreadViewModel @Inject constructor(
    private val repo: AdminSupportRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val customerId: String = checkNotNull(savedStateHandle["customerId"])
    val customerName: String = savedStateHandle.get<String>("customerName") ?: "Customer"

    private val _state = MutableStateFlow(SupportChatUiState())
    val state: StateFlow<SupportChatUiState> = _state.asStateFlow()
    private var pollingJob: Job? = null

    init {
        refresh(showLoading = true)
        startPolling()
    }

    private fun refresh(showLoading: Boolean) {
        viewModelScope.launch {
            if (showLoading) _state.update { it.copy(loading = true) }
            when (val r = repo.messages(customerId)) {
                is ApiResult.Success -> _state.update { it.copy(loading = false, messages = r.data.messages, error = null) }
                is ApiResult.Failure -> _state.update { it.copy(loading = false, error = r.message) }
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
            when (val r = repo.send(customerId, text)) {
                is ApiResult.Success -> _state.update { it.copy(sending = false, input = "", messages = it.messages + r.data) }
                is ApiResult.Failure -> _state.update { it.copy(sending = false, error = r.message) }
            }
        }
    }

    override fun onCleared() {
        pollingJob?.cancel()
    }
}

@Composable
fun AdminSupportThreadScreen(onBack: () -> Unit, viewModel: SupportThreadViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.size - 1)
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
            Spacer(Modifier.width(4.dp))
            Text(viewModel.customerName, style = MaterialTheme.typography.titleMedium)
        }
        Divider()

        if (state.loading) {
            FullScreenLoading(Modifier.weight(1f))
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.messages, key = { it.id }) { msg -> AdminChatBubble(msg) }
            }
        }

        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.bodySmall)
        }

        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = state.input,
                onValueChange = viewModel::setInput,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Reply to customer") },
            )
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = viewModel::send, enabled = !state.sending && state.input.isNotBlank()) {
                Icon(Icons.Filled.Send, contentDescription = "Send")
            }
        }
    }
}

@Composable
private fun AdminChatBubble(msg: ChatMessage) {
    val isMine = msg.sender_role == "admin"
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start) {
        Surface(
            color = if (isMine) LsRoleAdmin else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isMine) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            shape = MaterialTheme.shapes.medium,
        ) {
            Text(msg.text, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), style = MaterialTheme.typography.bodyMedium)
        }
    }
}
