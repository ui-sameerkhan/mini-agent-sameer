package com.lazyshopper.app.feature.admin.catalog

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.FloatingActionButton
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
import com.lazyshopper.app.core.data.remote.dto.Category
import com.lazyshopper.app.core.data.remote.dto.CategoryInput
import com.lazyshopper.app.core.ui.ActionState
import com.lazyshopper.app.core.ui.UiState
import com.lazyshopper.app.core.ui.components.LsTextField
import com.lazyshopper.app.feature.admin.common.AdminLazyList
import com.lazyshopper.app.feature.admin.common.AdminScreenScaffold
import com.lazyshopper.app.feature.admin.common.CompactRow
import com.lazyshopper.app.feature.admin.common.ConfirmDialog
import com.lazyshopper.app.feature.admin.common.EmptyOrList
import com.lazyshopper.app.feature.admin.common.FormDialog
import com.lazyshopper.app.feature.admin.common.LabeledSwitchRow
import com.lazyshopper.app.feature.admin.common.StatusChip
import com.lazyshopper.app.feature.admin.data.AdminCatalogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoriesViewModel @Inject constructor(private val repo: AdminCatalogRepository) : ViewModel() {
    private val _state = MutableStateFlow<UiState<List<Category>>>(UiState.Loading)
    val state: StateFlow<UiState<List<Category>>> = _state.asStateFlow()
    private val _actionState = MutableStateFlow<ActionState>(ActionState.Idle)
    val actionState: StateFlow<ActionState> = _actionState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            when (val r = repo.categories()) {
                is ApiResult.Success -> _state.value = UiState.Success(r.data)
                is ApiResult.Failure -> _state.value = UiState.Error(r.message)
            }
        }
    }

    fun save(existingId: String?, input: CategoryInput) {
        viewModelScope.launch {
            _actionState.value = ActionState.InFlight
            val r = if (existingId != null) repo.updateCategory(existingId, input) else repo.createCategory(input)
            when (r) {
                is ApiResult.Success -> { _actionState.value = ActionState.Done; load() }
                is ApiResult.Failure -> _actionState.value = ActionState.Failed(r.message)
            }
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            when (val r = repo.deleteCategory(id)) {
                is ApiResult.Success -> load()
                is ApiResult.Failure -> _actionState.value = ActionState.Failed(r.message)
            }
        }
    }
}

@Composable
fun AdminCategoriesScreen(onMenuClick: () -> Unit, viewModel: CategoriesViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val actionState by viewModel.actionState.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }

    var editing by remember { mutableStateOf<Category?>(null) }
    var creating by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf<Category?>(null) }

    AdminScreenScaffold(
        title = "Categories",
        onMenuClick = onMenuClick,
        fab = { FloatingActionButton(onClick = { creating = true }) { Icon(Icons.Default.Add, contentDescription = "Add category") } },
    ) { padding ->
        val s = state
        val items = (s as? UiState.Success)?.data.orEmpty()
        EmptyOrList(loading = s is UiState.Loading, error = (s as? UiState.Error)?.message, isEmpty = items.isEmpty() && s is UiState.Success, emptyMessage = "No categories yet", onRetry = viewModel::load) {
            AdminLazyList(modifier = androidx.compose.ui.Modifier.padding(padding)) {
                items(items, key = { it.id }) { c ->
                    CompactRow(
                        title = c.label,
                        subtitle = "key: ${c.key}",
                        badge = { StatusChip(if (c.active) "active" else "inactive") },
                        trailing = {
                            Row {
                                IconButton(onClick = { editing = c }) { Icon(Icons.Default.Edit, contentDescription = "Edit") }
                                IconButton(onClick = { confirmDelete = c }) { Icon(Icons.Default.Delete, contentDescription = "Delete") }
                            }
                        },
                    )
                }
            }
        }
    }

    if (creating) {
        CategoryFormDialog(null, actionState is ActionState.InFlight, onDismiss = { creating = false }, onSave = { input -> viewModel.save(null, input); creating = false })
    }
    editing?.let { c ->
        CategoryFormDialog(c, actionState is ActionState.InFlight, onDismiss = { editing = null }, onSave = { input -> viewModel.save(c.id, input); editing = null })
    }
    confirmDelete?.let { c ->
        ConfirmDialog("Delete category", "Delete \"${c.label}\"?", destructive = true, confirmLabel = "Delete", onConfirm = { viewModel.delete(c.id); confirmDelete = null }, onDismiss = { confirmDelete = null })
    }
}

@Composable
private fun CategoryFormDialog(existing: Category?, saving: Boolean, onDismiss: () -> Unit, onSave: (CategoryInput) -> Unit) {
    var key by remember { mutableStateOf(existing?.key.orEmpty()) }
    var label by remember { mutableStateOf(existing?.label.orEmpty()) }
    var active by remember { mutableStateOf(existing?.active ?: true) }

    FormDialog(
        title = if (existing == null) "New category" else "Edit category",
        onDismiss = onDismiss,
        saving = saving,
        onSave = { onSave(CategoryInput(key.trim(), label.trim(), active)) },
    ) {
        LsTextField(key, { key = it }, "Key (e.g. vegetables)")
        Spacer(Modifier.height(8.dp))
        LsTextField(label, { label = it }, "Display label")
        Spacer(Modifier.height(8.dp))
        LabeledSwitchRow("Active", active, { active = it })
    }
}
