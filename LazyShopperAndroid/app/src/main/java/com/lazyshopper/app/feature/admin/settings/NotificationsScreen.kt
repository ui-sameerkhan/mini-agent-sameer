package com.lazyshopper.app.feature.admin.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lazyshopper.app.core.data.remote.ApiResult
import com.lazyshopper.app.core.data.remote.dto.NotificationDto
import com.lazyshopper.app.core.data.remote.dto.NotificationInput
import com.lazyshopper.app.core.ui.ActionState
import com.lazyshopper.app.core.ui.UiState
import com.lazyshopper.app.core.ui.components.LsPrimaryButton
import com.lazyshopper.app.core.ui.components.LsTextField
import com.lazyshopper.app.feature.admin.common.AdminScreenScaffold
import com.lazyshopper.app.feature.admin.common.CompactRow
import com.lazyshopper.app.feature.admin.common.SectionDivider
import com.lazyshopper.app.feature.admin.data.AdminSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private val AUDIENCES = listOf("all", "customers", "shopkeepers", "delivery")
private val TYPES = listOf("general", "offer", "order", "promo")

@HiltViewModel
class NotificationsViewModel @Inject constructor(private val repo: AdminSettingsRepository) : ViewModel() {
    private val _state = MutableStateFlow<UiState<List<NotificationDto>>>(UiState.Loading)
    val state: StateFlow<UiState<List<NotificationDto>>> = _state.asStateFlow()
    private val _actionState = MutableStateFlow<ActionState>(ActionState.Idle)
    val actionState: StateFlow<ActionState> = _actionState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            when (val r = repo.notifications()) {
                is ApiResult.Success -> _state.value = UiState.Success(r.data)
                is ApiResult.Failure -> _state.value = UiState.Error(r.message)
            }
        }
    }

    fun send(input: NotificationInput) {
        viewModelScope.launch {
            _actionState.value = ActionState.InFlight
            when (val r = repo.sendNotification(input)) {
                is ApiResult.Success -> { _actionState.value = ActionState.Done; load() }
                is ApiResult.Failure -> _actionState.value = ActionState.Failed(r.message)
            }
        }
    }

    fun delete(id: String) = viewModelScope.launch {
        when (val r = repo.deleteNotification(id)) {
            is ApiResult.Success -> load()
            is ApiResult.Failure -> _actionState.value = ActionState.Failed(r.message)
        }
    }
}

@Composable
fun AdminNotificationsScreen(onMenuClick: () -> Unit, viewModel: NotificationsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val actionState by viewModel.actionState.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }

    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var audience by remember { mutableStateOf(AUDIENCES.first()) }
    var type by remember { mutableStateOf(TYPES.first()) }
    var link by remember { mutableStateOf("") }

    AdminScreenScaffold(title = "Notifications", onMenuClick = onMenuClick) { padding ->
        Column(Modifier.padding(padding).padding(12.dp).verticalScroll(rememberScrollState())) {
            Text("Compose notification", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(6.dp))
            LsTextField(title, { title = it }, "Title")
            Spacer(Modifier.height(8.dp))
            LsTextField(body, { body = it }, "Body", singleLine = false)
            Spacer(Modifier.height(8.dp))
            SimpleDropdown("Audience", AUDIENCES, audience) { audience = it }
            Spacer(Modifier.height(8.dp))
            SimpleDropdown("Type", TYPES, type) { type = it }
            Spacer(Modifier.height(8.dp))
            LsTextField(link, { link = it }, "Link (optional)")
            Spacer(Modifier.height(12.dp))
            LsPrimaryButton(
                text = "Send notification",
                loading = actionState is ActionState.InFlight,
                enabled = title.isNotBlank() && body.isNotBlank(),
                onClick = {
                    viewModel.send(NotificationInput(title = title, body = body, audience = audience, type = type, link = link.ifBlank { null }))
                    title = ""; body = ""; link = ""
                },
            )
            if (actionState is ActionState.Failed) {
                Text((actionState as ActionState.Failed).message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            SectionDivider()
            Text("Sent notifications", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(6.dp))
            when (val s = state) {
                is UiState.Loading -> androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.padding(12.dp))
                is UiState.Error -> Text(s.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                is UiState.Success -> {
                    if (s.data.isEmpty()) {
                        Text("No notifications sent yet", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    s.data.forEach { n ->
                        CompactRow(
                            title = n.title,
                            subtitle = n.body,
                            meta = "${n.audience} · ${n.type} · ${n.created_at ?: "-"}${n.read_count?.let { " · read by $it" } ?: ""}",
                            trailing = {
                                IconButton(onClick = { viewModel.delete(n.id) }) { Icon(Icons.Filled.Delete, contentDescription = "Delete") }
                            },
                            modifier = Modifier.padding(vertical = 4.dp),
                        )
                    }
                }
                is UiState.Idle -> {}
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SimpleDropdown(label: String, options: List<String>, selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { opt ->
                DropdownMenuItem(text = { Text(opt) }, onClick = { onSelect(opt); expanded = false })
            }
        }
    }
}
