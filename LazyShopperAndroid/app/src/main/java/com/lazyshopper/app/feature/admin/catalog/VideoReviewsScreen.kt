package com.lazyshopper.app.feature.admin.catalog

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
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
import com.lazyshopper.app.feature.admin.data.AdminCatalogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VideoReviewsViewModel @Inject constructor(private val repo: AdminCatalogRepository) : ViewModel() {
    private val _state = MutableStateFlow<UiState<List<Review>>>(UiState.Loading)
    val state: StateFlow<UiState<List<Review>>> = _state.asStateFlow()
    private val _actionState = MutableStateFlow<ActionState>(ActionState.Idle)
    val actionState: StateFlow<ActionState> = _actionState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            when (val r = repo.videoReviews()) {
                is ApiResult.Success -> _state.value = UiState.Success(r.data)
                is ApiResult.Failure -> _state.value = UiState.Error(r.message)
            }
        }
    }

    fun moderate(id: String, status: String) = viewModelScope.launch {
        when (val r = repo.moderateVideoReview(id, status)) {
            is ApiResult.Success -> load()
            is ApiResult.Failure -> _actionState.value = ActionState.Failed(r.message)
        }
    }

    fun delete(id: String) = viewModelScope.launch {
        when (val r = repo.deleteVideoReview(id)) {
            is ApiResult.Success -> load()
            is ApiResult.Failure -> _actionState.value = ActionState.Failed(r.message)
        }
    }
}

@Composable
fun AdminVideoReviewsScreen(onMenuClick: () -> Unit, viewModel: VideoReviewsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }
    var confirmDelete by remember { mutableStateOf<Review?>(null) }

    AdminScreenScaffold(title = "Video Reviews", onMenuClick = onMenuClick) { padding ->
        val s = state
        val items = (s as? UiState.Success)?.data.orEmpty()
        EmptyOrList(loading = s is UiState.Loading, error = (s as? UiState.Error)?.message, isEmpty = items.isEmpty() && s is UiState.Success, emptyMessage = "No video reviews yet", onRetry = viewModel::load) {
            AdminLazyList(modifier = Modifier.padding(padding)) {
                items(items, key = { it.id }) { r ->
                    CompactRow(
                        title = "${r.user_name ?: "User"} · ${"★".repeat(r.rating)}${"☆".repeat(5 - r.rating)}",
                        subtitle = r.comment?.ifBlank { "(no comment)" } ?: "(no comment)",
                        meta = "${r.target_type} · ${r.target_id}",
                        badge = { StatusChip(r.video_status ?: "pending") },
                        trailing = {
                            Row {
                                if (r.video_status != "approved") IconButton(onClick = { viewModel.moderate(r.id, "approved") }) { Icon(Icons.Default.Check, contentDescription = "Approve") }
                                if (r.video_status != "rejected") IconButton(onClick = { viewModel.moderate(r.id, "rejected") }) { Icon(Icons.Default.Close, contentDescription = "Reject") }
                                IconButton(onClick = { confirmDelete = r }) { Icon(Icons.Default.Delete, contentDescription = "Delete") }
                            }
                        },
                    )
                }
            }
        }
    }

    confirmDelete?.let { r ->
        ConfirmDialog("Delete video review", "Delete this video review permanently?", destructive = true, confirmLabel = "Delete", onConfirm = { viewModel.delete(r.id); confirmDelete = null }, onDismiss = { confirmDelete = null })
    }
}
