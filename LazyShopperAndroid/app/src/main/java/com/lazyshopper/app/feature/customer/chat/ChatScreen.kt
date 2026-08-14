package com.lazyshopper.app.feature.customer.chat

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
import com.lazyshopper.app.core.data.remote.dto.ChatMessage
import com.lazyshopper.app.core.theme.LsPrimary
import com.lazyshopper.app.core.ui.components.FullScreenLoading
import com.lazyshopper.app.core.ui.components.ScreenPadding

/** Order-chat entry point: nav route supplies `orderId`, which [ChatViewModel] reads via SavedStateHandle. */
@Composable
fun OrderChatScreen(onBack: () -> Unit, viewModel: ChatViewModel = hiltViewModel()) = ChatScreen(onBack, viewModel)

/** Support-chat entry point: no `orderId` arg on the route, so [ChatViewModel] falls back to the support thread. */
@Composable
fun SupportChatScreen(onBack: () -> Unit, viewModel: ChatViewModel = hiltViewModel()) = ChatScreen(onBack, viewModel)

@Composable
private fun ChatScreen(
    onBack: () -> Unit,
    viewModel: ChatViewModel,
) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.size - 1)
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(ScreenPadding), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
            Spacer(Modifier.width(4.dp))
            Text(state.title, style = MaterialTheme.typography.titleMedium)
        }
        Divider()

        if (state.loading) {
            FullScreenLoading(Modifier.weight(1f))
        } else if (state.messages.isEmpty()) {
            Column(Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("No messages yet — say hello.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.messages, key = { it.id }) { msg -> ChatBubble(msg) }
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
                placeholder = { Text("Type a message") },
            )
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = viewModel::send, enabled = !state.sending && state.input.isNotBlank()) {
                Icon(Icons.Filled.Send, contentDescription = "Send")
            }
        }
    }
}

@Composable
private fun ChatBubble(msg: ChatMessage) {
    val isMine = msg.sender_role == "customer"
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start) {
        Surface(
            color = if (isMine) LsPrimary else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isMine) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            shape = MaterialTheme.shapes.medium,
        ) {
            Text(msg.text, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), style = MaterialTheme.typography.bodyMedium)
        }
    }
}
