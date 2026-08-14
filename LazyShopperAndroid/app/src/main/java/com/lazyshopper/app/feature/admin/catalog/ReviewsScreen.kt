package com.lazyshopper.app.feature.admin.catalog

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.lazyshopper.app.core.data.remote.dto.Review
import com.lazyshopper.app.core.ui.ActionState
import com.lazyshopper.app.core.ui.UiState
import com.lazyshopper.app.feature.admin.common.AdminLazyList
import com.lazyshopper.app.feature.admin.common.AdminScreenScaffold
import com.lazyshopper.app.feature.admin.common.CompactRow
import com.lazyshopper.app.feature.admin.common.ConfirmDialog
import com.lazyshopper.app.feature.admin.common.EmptyOrList
import com.lazyshopper.app.feature.admin.common.StatusChip
import com.lazyshopper.app.feature.admin.common.Tone
import com.lazyshopper.app.feature.admin.data.AdminCatalogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReviewsViewModel @Inject constructor(private val repo: AdminCatalogRepository) : ViewModel() {
    private val _state = MutableStateFlow<UiState<List<Review>>>(UiState.Loading)
    val state: StateFlow<UiState<List<Review>>> = _state.asStateFlow()
    private val _actionState = MutableStateFlow<ActionState>(ActionState.Idle)
    val actionState: StateFlow<ActionState> = _actionState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            when (val r = repo.reviews()) {
                is ApiResult.Success -> _state.value = UiState.Success(r.data)
                is ApiResult.Failure -> _state.value = UiState.Error(r.message)
            }
        }
    }

    fun toggleHidden(id: String, hidden: Boolean) = viewModelScope.launch {
        when (val r = repo.hideReview(id, hidden)) {
            is ApiResult.Success -> load()
            is ApiResult.Failure -> _actionState.value = ActionState.Failed(r.message)
        }
    }

    fun delete(id: String) = viewModelScope.launch {
        when (val r = repo.deleteReview(id)) {
            is ApiResult.Success -> load()
            is ApiResult.Failure -> _actionState.value = ActionState.Failed(r.message)
        }
    }
}

@Composable
fun AdminReviewsScreen(onMenuClick: () -> Unit, viewModel: ReviewsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }
    var confirmDelete by remember { mutableStateOf<Review?>(null) }

    AdminScreenScaffold(title = "Reviews Moderation", onMenuClick = onMenuClick) { padding ->
        val s = state
        val items = (s as? UiState.Success)?.data.orEmpty()
        EmptyOrList(loading = s is UiState.Loading, error = (s as? UiState.Error)?.message, isEmpty = items.isEmpty() && s is UiState.Success, emptyMessage = "No reviews yet", onRetry = viewModel::load) {
            AdminLazyList(modifier = Modifier.padding(padding)) {
                items(items, key = { it.id }) { r ->
                    CompactRow(
                        title = "${r.user_name ?: "User"} · ${"★".repeat(r.rating)}${"☆".repeat(5 - r.rating)}",
                        subtitle = r.comment?.ifBlank { "(no comment)" } ?: "(no comment)",
                        meta = "${r.target_type} · ${r.target_id}",
                        badge = { if (r.hidden) StatusChip("hidden", tone = Tone.WARNING) },
                        trailing = {
                            Row {
                                IconButton(onClick = { viewModel.toggleHidden(r.id, !r.hidden) }) {
                                    Icon(if (r.hidden) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = "Toggle visibility")
                                }
                                IconButton(onClick = { confirmDelete = r }) { Icon(Icons.Default.Delete, contentDescription = "Delete") }
                            }
                        },
                    )
                }
            }
        }
    }

    confirmDelete?.let { r ->
        ConfirmDialog("Delete review", "Delete this review permanently?", destructive = true, confirmLabel = "Delete", onConfirm = { viewModel.delete(r.id); confirmDelete = null }, onDismiss = { confirmDelete = null })
    }
}
