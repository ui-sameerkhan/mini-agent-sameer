package com.lazyshopper.app.feature.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lazyshopper.app.core.data.remote.ApiResult
import com.lazyshopper.app.core.ui.ActionState
import com.lazyshopper.app.core.ui.components.LsPrimaryButton
import com.lazyshopper.app.core.ui.components.LsTextField
import com.lazyshopper.app.core.ui.components.ScreenPadding
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PasswordResetViewModel @Inject constructor(
    private val repository: AuthRepository,
) : ViewModel() {
    private val _actionState = MutableStateFlow<ActionState>(ActionState.Idle)
    val actionState: StateFlow<ActionState> = _actionState.asStateFlow()
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun forgotPassword(email: String) {
        viewModelScope.launch {
            _actionState.value = ActionState.InFlight
            when (val result = repository.forgotPassword(email)) {
                is ApiResult.Success -> {
                    _actionState.value = ActionState.Done
                    _message.value = result.data.message
                }
                is ApiResult.Failure -> _actionState.value = ActionState.Failed(result.message)
            }
        }
    }

    fun resetPassword(token: String, newPassword: String) {
        viewModelScope.launch {
            _actionState.value = ActionState.InFlight
            when (val result = repository.resetPassword(token, newPassword)) {
                is ApiResult.Success -> {
                    _actionState.value = ActionState.Done
                    _message.value = result.data.message
                }
                is ApiResult.Failure -> _actionState.value = ActionState.Failed(result.message)
            }
        }
    }
}

@Composable
fun ForgotPasswordScreen(
    onBack: () -> Unit,
    viewModel: PasswordResetViewModel = hiltViewModel(),
) {
    var email by remember { mutableStateOf("") }
    val actionState by viewModel.actionState.collectAsState()
    val message by viewModel.message.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(ScreenPadding)) {
        Spacer(Modifier.height(24.dp))
        Text("Reset your password", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "Enter your account email and we'll send you a reset link.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))
        LsTextField(value = email, onValueChange = { email = it }, label = "Email", keyboardType = KeyboardType.Email)
        Spacer(Modifier.height(16.dp))
        if (message != null) {
            Text(message.orEmpty(), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(12.dp))
        }
        if (actionState is ActionState.Failed) {
            Text((actionState as ActionState.Failed).message, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(12.dp))
        }
        LsPrimaryButton(
            text = "Send reset link",
            onClick = { viewModel.forgotPassword(email.trim()) },
            loading = actionState is ActionState.InFlight,
            enabled = email.isNotBlank(),
        )
        Spacer(Modifier.height(12.dp))
        LsPrimaryButton(text = "Back to login", onClick = onBack)
    }
}

@Composable
fun ResetPasswordScreen(
    token: String,
    onDone: () -> Unit,
    viewModel: PasswordResetViewModel = hiltViewModel(),
) {
    var newPassword by remember { mutableStateOf("") }
    val actionState by viewModel.actionState.collectAsState()

    androidx.compose.runtime.LaunchedEffect(actionState) {
        if (actionState is ActionState.Done) onDone()
    }

    Column(modifier = Modifier.fillMaxSize().padding(ScreenPadding)) {
        Spacer(Modifier.height(24.dp))
        Text("Set a new password", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(20.dp))
        LsTextField(value = newPassword, onValueChange = { newPassword = it }, label = "New password", isPassword = true)
        Spacer(Modifier.height(16.dp))
        if (actionState is ActionState.Failed) {
            Text((actionState as ActionState.Failed).message, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(12.dp))
        }
        LsPrimaryButton(
            text = "Reset password",
            onClick = { viewModel.resetPassword(token, newPassword) },
            loading = actionState is ActionState.InFlight,
            enabled = newPassword.length >= 6,
        )
    }
}
