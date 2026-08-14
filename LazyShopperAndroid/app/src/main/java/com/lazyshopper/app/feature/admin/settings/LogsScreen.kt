package com.lazyshopper.app.feature.admin.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lazyshopper.app.core.data.remote.ApiResult
import com.lazyshopper.app.core.data.remote.dto.AdminLogEntry
import com.lazyshopper.app.core.ui.UiState
import com.lazyshopper.app.feature.admin.common.AdminLazyList
import com.lazyshopper.app.feature.admin.common.AdminScreenScaffold
import com.lazyshopper.app.feature.admin.common.CompactRow
import com.lazyshopper.app.feature.admin.common.EmptyOrList
import com.lazyshopper.app.feature.admin.data.AdminSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LogsViewModel @Inject constructor(private val repo: AdminSettingsRepository) : ViewModel() {
    private val _state = MutableStateFlow<UiState<List<AdminLogEntry>>>(UiState.Loading)
    val state: StateFlow<UiState<List<AdminLogEntry>>> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            when (val r = repo.logs()) {
                is ApiResult.Success -> _state.value = UiState.Success(r.data)
                is ApiResult.Failure -> _state.value = UiState.Error(r.message)
            }
        }
    }
}

@Composable
fun AdminLogsScreen(onMenuClick: () -> Unit, viewModel: LogsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }

    AdminScreenScaffold(title = "Activity Logs", onMenuClick = onMenuClick) { padding ->
        val s = state
        EmptyOrList(
            loading = s is UiState.Loading,
            error = (s as? UiState.Error)?.message,
            isEmpty = (s as? UiState.Success)?.data.isNullOrEmpty() && s is UiState.Success,
            emptyMessage = "No activity recorded yet",
            onRetry = viewModel::load,
        ) {
            val logs = (s as UiState.Success).data
            Column(Modifier.padding(padding)) {
                AdminLazyList {
                    items(logs, key = { it.id }) { log ->
                        CompactRow(
                            title = log.action ?: "-",
                            subtitle = log.admin_email ?: "-",
                            meta = "${log.detail ?: ""} · ${log.created_at ?: "-"}",
                        )
                    }
                }
            }
        }
    }
}
