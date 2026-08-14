package com.lazyshopper.app.feature.admin.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.lazyshopper.app.core.data.remote.dto.EmailSettingsResponse
import com.lazyshopper.app.core.ui.ActionState
import com.lazyshopper.app.core.ui.UiState
import com.lazyshopper.app.core.ui.components.LsPrimaryButton
import com.lazyshopper.app.core.ui.components.LsTextField
import com.lazyshopper.app.feature.admin.common.AdminScreenScaffold
import com.lazyshopper.app.feature.admin.common.EmptyOrList
import com.lazyshopper.app.feature.admin.common.LabeledSwitchRow
import com.lazyshopper.app.feature.admin.common.SectionDivider
import com.lazyshopper.app.feature.admin.data.AdminSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EmailSettingsViewModel @Inject constructor(private val repo: AdminSettingsRepository) : ViewModel() {
    private val _state = MutableStateFlow<UiState<EmailSettingsResponse>>(UiState.Loading)
    val state: StateFlow<UiState<EmailSettingsResponse>> = _state.asStateFlow()
    private val _actionState = MutableStateFlow<ActionState>(ActionState.Idle)
    val actionState: StateFlow<ActionState> = _actionState.asStateFlow()
    private val _testState = MutableStateFlow<ActionState>(ActionState.Idle)
    val testState: StateFlow<ActionState> = _testState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            when (val r = repo.emailSettings()) {
                is ApiResult.Success -> _state.value = UiState.Success(r.data)
                is ApiResult.Failure -> _state.value = UiState.Error(r.message)
            }
        }
    }

    fun save(enabled: Boolean, statuses: Map<String, Boolean>, channels: Map<String, Boolean>) {
        viewModelScope.launch {
            _actionState.value = ActionState.InFlight
            when (val r = repo.updateEmailSettings(enabled, statuses, channels)) {
                is ApiResult.Success -> { _actionState.value = ActionState.Done; load() }
                is ApiResult.Failure -> _actionState.value = ActionState.Failed(r.message)
            }
        }
    }

    fun sendTest(email: String) {
        viewModelScope.launch {
            _testState.value = ActionState.InFlight
            when (val r = repo.testEmail(email)) {
                is ApiResult.Success -> _testState.value = ActionState.Done
                is ApiResult.Failure -> _testState.value = ActionState.Failed(r.message)
            }
        }
    }
}

@Composable
fun AdminEmailSettingsScreen(onMenuClick: () -> Unit, viewModel: EmailSettingsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val actionState by viewModel.actionState.collectAsState()
    val testState by viewModel.testState.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }
    var testEmail by remember { mutableStateOf("") }

    AdminScreenScaffold(title = "Email Settings", onMenuClick = onMenuClick) { padding ->
        val s = state
        EmptyOrList(loading = s is UiState.Loading, error = (s as? UiState.Error)?.message, isEmpty = false, emptyMessage = "", onRetry = viewModel::load) {
            val d = (s as UiState.Success).data
            var enabled by remember(d) { mutableStateOf(d.enabled) }
            var placed by remember(d) { mutableStateOf(d.statuses.placed) }
            var packed by remember(d) { mutableStateOf(d.statuses.packed) }
            var outForDelivery by remember(d) { mutableStateOf(d.statuses.out_for_delivery) }
            var delivered by remember(d) { mutableStateOf(d.statuses.delivered) }
            var cancelled by remember(d) { mutableStateOf(d.statuses.cancelled) }
            var emailChannel by remember(d) { mutableStateOf(d.channels.email) }
            var smsChannel by remember(d) { mutableStateOf(d.channels.sms) }
            var whatsappChannel by remember(d) { mutableStateOf(d.channels.whatsapp) }

            Column(Modifier.padding(padding).padding(12.dp).verticalScroll(rememberScrollState())) {
                LabeledSwitchRow("Order emails enabled", enabled, { enabled = it }, subtitle = "Master switch for all order status notifications")
                SectionDivider()
                Text("Send on status", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(4.dp))
                LabeledSwitchRow("Placed", placed, { placed = it })
                LabeledSwitchRow("Packed", packed, { packed = it })
                LabeledSwitchRow("Out for delivery", outForDelivery, { outForDelivery = it })
                LabeledSwitchRow("Delivered", delivered, { delivered = it })
                LabeledSwitchRow("Cancelled", cancelled, { cancelled = it })
                SectionDivider()
                Text("Channels", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(4.dp))
                LabeledSwitchRow("Email", emailChannel, { emailChannel = it })
                LabeledSwitchRow("SMS", smsChannel, { smsChannel = it })
                LabeledSwitchRow("WhatsApp", whatsappChannel, { whatsappChannel = it })
                Spacer(Modifier.height(12.dp))
                LsPrimaryButton(
                    text = "Save email settings",
                    loading = actionState is ActionState.InFlight,
                    onClick = {
                        viewModel.save(
                            enabled,
                            mapOf("placed" to placed, "packed" to packed, "out_for_delivery" to outForDelivery, "delivered" to delivered, "cancelled" to cancelled),
                            mapOf("email" to emailChannel, "sms" to smsChannel, "whatsapp" to whatsappChannel),
                        )
                    },
                )
                if (actionState is ActionState.Failed) {
                    Text((actionState as ActionState.Failed).message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                SectionDivider()
                Text("Send a test email", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(4.dp))
                Row {
                    LsTextField(testEmail, { testEmail = it }, "Email address", modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = { viewModel.sendTest(testEmail) }, enabled = testEmail.isNotBlank() && testState !is ActionState.InFlight) { Text("Send test") }
                }
                when (val t = testState) {
                    is ActionState.Done -> Text("Test email sent", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                    is ActionState.Failed -> Text(t.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    else -> {}
                }
            }
        }
    }
}
