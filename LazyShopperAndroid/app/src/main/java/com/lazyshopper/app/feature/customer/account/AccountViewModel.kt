package com.lazyshopper.app.feature.customer.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lazyshopper.app.core.data.remote.ApiResult
import com.lazyshopper.app.core.data.remote.dto.NotificationDto
import com.lazyshopper.app.core.data.remote.dto.User
import com.lazyshopper.app.core.ui.ActionState
import com.lazyshopper.app.core.ui.UiState
import com.lazyshopper.app.feature.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileFormState(
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val password: String = "",
)

data class AccountUiState(
    val profile: UiState<User> = UiState.Loading,
    val form: ProfileFormState = ProfileFormState(),
    val editing: Boolean = false,
    val saveState: ActionState = ActionState.Idle,
    val notifications: List<NotificationDto> = emptyList(),
    val unreadCount: Int = 0,
    val notificationsLoading: Boolean = false,
)

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val repository: AccountRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AccountUiState())
    val state: StateFlow<AccountUiState> = _state.asStateFlow()

    init {
        loadProfile()
        loadNotifications()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _state.update { it.copy(profile = UiState.Loading) }
            when (val r = repository.me()) {
                is ApiResult.Success -> _state.update {
                    it.copy(
                        profile = UiState.Success(r.data.user),
                        form = ProfileFormState(name = r.data.user.name.orEmpty(), email = r.data.user.email.orEmpty(), phone = r.data.user.phone.orEmpty()),
                    )
                }
                is ApiResult.Failure -> _state.update { it.copy(profile = UiState.Error(r.message)) }
            }
        }
    }

    fun loadNotifications() {
        viewModelScope.launch {
            _state.update { it.copy(notificationsLoading = true) }
            (repository.notifications() as? ApiResult.Success)?.let { r ->
                _state.update { it.copy(notifications = r.data, notificationsLoading = false) }
            } ?: _state.update { it.copy(notificationsLoading = false) }
            (repository.notificationsUnreadCount() as? ApiResult.Success)?.let { r ->
                _state.update { it.copy(unreadCount = r.data.count) }
            }
        }
    }

    fun markNotificationRead(id: String) {
        viewModelScope.launch {
            repository.markNotificationRead(id)
            loadNotifications()
        }
    }

    fun markAllNotificationsRead() {
        viewModelScope.launch {
            repository.markAllNotificationsRead()
            loadNotifications()
        }
    }

    fun startEdit() = _state.update { it.copy(editing = true, saveState = ActionState.Idle) }
    fun cancelEdit() = _state.update { it.copy(editing = false, saveState = ActionState.Idle) }

    fun updateForm(transform: (ProfileFormState) -> ProfileFormState) = _state.update { it.copy(form = transform(it.form)) }

    fun saveProfile() {
        val form = _state.value.form
        viewModelScope.launch {
            _state.update { it.copy(saveState = ActionState.InFlight) }
            when (
                val r = repository.updateProfile(
                    name = form.name.ifBlank { null },
                    email = form.email.ifBlank { null },
                    password = form.password.ifBlank { null },
                    phone = form.phone.ifBlank { null },
                )
            ) {
                is ApiResult.Success -> _state.update {
                    it.copy(
                        saveState = ActionState.Done,
                        editing = false,
                        profile = UiState.Success(r.data.user),
                        form = it.form.copy(password = ""),
                    )
                }
                is ApiResult.Failure -> _state.update { it.copy(saveState = ActionState.Failed(r.message)) }
            }
        }
    }

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onLoggedOut()
        }
    }
}
