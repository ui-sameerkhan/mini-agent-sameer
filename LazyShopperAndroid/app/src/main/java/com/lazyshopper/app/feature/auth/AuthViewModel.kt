package com.lazyshopper.app.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lazyshopper.app.core.data.remote.ApiResult
import com.lazyshopper.app.core.ui.ActionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AuthMode { LOGIN, REGISTER }

data class AuthUiState(
    val mode: AuthMode = AuthMode.LOGIN,
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val role: String = "customer",
    val shopName: String = "",
    val phone: String = "",
    val actionState: ActionState = ActionState.Idle,
    val loggedInRole: String? = null,
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun setMode(mode: AuthMode) = _state.update { it.copy(mode = mode, actionState = ActionState.Idle) }
    fun setName(v: String) = _state.update { it.copy(name = v) }
    fun setEmail(v: String) = _state.update { it.copy(email = v) }
    fun setPassword(v: String) = _state.update { it.copy(password = v) }
    fun setRole(v: String) = _state.update { it.copy(role = v) }
    fun setShopName(v: String) = _state.update { it.copy(shopName = v) }
    fun setPhone(v: String) = _state.update { it.copy(phone = v) }

    private inline fun MutableStateFlow<AuthUiState>.update(transform: (AuthUiState) -> AuthUiState) {
        value = transform(value)
    }

    fun submit() {
        val s = _state.value
        if (s.email.isBlank() || s.password.isBlank()) {
            _state.update { it.copy(actionState = ActionState.Failed("Email and password are required")) }
            return
        }
        if (s.mode == AuthMode.REGISTER && s.name.isBlank()) {
            _state.update { it.copy(actionState = ActionState.Failed("Name is required")) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(actionState = ActionState.InFlight) }
            val result = if (s.mode == AuthMode.LOGIN) {
                repository.login(s.email.trim(), s.password)
            } else {
                repository.register(
                    name = s.name.trim(),
                    email = s.email.trim(),
                    password = s.password,
                    role = s.role,
                    shopName = s.shopName.ifBlank { null },
                    phone = s.phone.ifBlank { null },
                )
            }
            when (result) {
                is ApiResult.Success -> _state.update {
                    it.copy(actionState = ActionState.Done, loggedInRole = result.data.user.role)
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(actionState = ActionState.Failed(result.message))
                }
            }
        }
    }
}
