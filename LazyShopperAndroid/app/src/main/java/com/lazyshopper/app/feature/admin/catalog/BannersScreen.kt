package com.lazyshopper.app.feature.admin.catalog

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.lazyshopper.app.core.data.remote.ApiResult
import com.lazyshopper.app.core.data.remote.dto.Banner
import com.lazyshopper.app.core.data.remote.dto.BannerInput
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
import com.lazyshopper.app.feature.admin.common.fileUrl
import com.lazyshopper.app.feature.admin.common.uriToImagePart
import com.lazyshopper.app.feature.admin.data.AdminCatalogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BannersViewModel @Inject constructor(private val repo: AdminCatalogRepository) : ViewModel() {
    private val _state = MutableStateFlow<UiState<List<Banner>>>(UiState.Loading)
    val state: StateFlow<UiState<List<Banner>>> = _state.asStateFlow()
    private val _actionState = MutableStateFlow<ActionState>(ActionState.Idle)
    val actionState: StateFlow<ActionState> = _actionState.asStateFlow()
    private val _uploadedImageId = MutableStateFlow<String?>(null)
    val uploadedImageId: StateFlow<String?> = _uploadedImageId.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            when (val r = repo.banners()) {
                is ApiResult.Success -> _state.value = UiState.Success(r.data)
                is ApiResult.Failure -> _state.value = UiState.Error(r.message)
            }
        }
    }

    fun uploadImage(part: okhttp3.MultipartBody.Part) {
        viewModelScope.launch {
            when (val r = repo.uploadImage(part)) {
                is ApiResult.Success -> _uploadedImageId.value = r.data.image_id
                is ApiResult.Failure -> _actionState.value = ActionState.Failed(r.message)
            }
        }
    }
    fun clearUploadedImage() { _uploadedImageId.value = null }

    fun save(existingId: String?, input: BannerInput) {
        viewModelScope.launch {
            _actionState.value = ActionState.InFlight
            val r = if (existingId != null) repo.updateBanner(existingId, input) else repo.createBanner(input)
            when (r) {
                is ApiResult.Success -> { _actionState.value = ActionState.Done; load() }
                is ApiResult.Failure -> _actionState.value = ActionState.Failed(r.message)
            }
        }
    }

    fun toggle(id: String, active: Boolean) = viewModelScope.launch {
        when (val r = repo.toggleBanner(id, active)) {
            is ApiResult.Success -> load()
            is ApiResult.Failure -> _actionState.value = ActionState.Failed(r.message)
        }
    }

    fun delete(id: String) = viewModelScope.launch {
        when (val r = repo.deleteBanner(id)) {
            is ApiResult.Success -> load()
            is ApiResult.Failure -> _actionState.value = ActionState.Failed(r.message)
        }
    }
}

private val BANNER_TYPES = listOf("home", "festival", "flash_sale", "new_arrival")

@Composable
fun AdminBannersScreen(onMenuClick: () -> Unit, viewModel: BannersViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val actionState by viewModel.actionState.collectAsState()
    val uploadedImageId by viewModel.uploadedImageId.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }

    var editing by remember { mutableStateOf<Banner?>(null) }
    var creating by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf<Banner?>(null) }

    AdminScreenScaffold(
        title = "Banners",
        onMenuClick = onMenuClick,
        fab = { FloatingActionButton(onClick = { viewModel.clearUploadedImage(); creating = true }) { Icon(Icons.Default.Add, contentDescription = "Add banner") } },
    ) { padding ->
        val s = state
        val items = (s as? UiState.Success)?.data.orEmpty()
        EmptyOrList(loading = s is UiState.Loading, error = (s as? UiState.Error)?.message, isEmpty = items.isEmpty() && s is UiState.Success, emptyMessage = "No banners yet", onRetry = viewModel::load) {
            AdminLazyList(modifier = Modifier.padding(padding)) {
                items(items, key = { it.id }) { b ->
                    CompactRow(
                        title = b.title,
                        subtitle = "${b.banner_type} · order ${b.sort_order} · clicks ${b.clicks ?: 0} / impr ${b.impressions ?: 0}",
                        badge = { StatusChip(b.state ?: if (b.active) "live" else "disabled") },
                        trailing = {
                            Row {
                                Switch(checked = b.active, onCheckedChange = { viewModel.toggle(b.id, it) })
                                IconButton(onClick = { viewModel.clearUploadedImage(); editing = b }) { Icon(Icons.Default.Edit, contentDescription = "Edit") }
                                IconButton(onClick = { confirmDelete = b }) { Icon(Icons.Default.Delete, contentDescription = "Delete") }
                            }
                        },
                    )
                }
            }
        }
    }

    if (creating) {
        BannerFormDialog(null, actionState is ActionState.InFlight, uploadedImageId, onUpload = viewModel::uploadImage, onDismiss = { creating = false }, onSave = { input -> viewModel.save(null, input); creating = false })
    }
    editing?.let { b ->
        BannerFormDialog(b, actionState is ActionState.InFlight, uploadedImageId, onUpload = viewModel::uploadImage, onDismiss = { editing = null }, onSave = { input -> viewModel.save(b.id, input); editing = null })
    }
    confirmDelete?.let { b ->
        ConfirmDialog("Delete banner", "Delete \"${b.title}\"?", destructive = true, confirmLabel = "Delete", onConfirm = { viewModel.delete(b.id); confirmDelete = null }, onDismiss = { confirmDelete = null })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BannerFormDialog(
    existing: Banner?,
    saving: Boolean,
    uploadedImageId: String?,
    onUpload: (okhttp3.MultipartBody.Part) -> Unit,
    onDismiss: () -> Unit,
    onSave: (BannerInput) -> Unit,
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(existing?.title.orEmpty()) }
    var subtitle by remember { mutableStateOf(existing?.subtitle.orEmpty()) }
    var link by remember { mutableStateOf(existing?.link.orEmpty()) }
    var bannerType by remember { mutableStateOf(existing?.banner_type ?: "home") }
    var sortOrder by remember { mutableStateOf(existing?.sort_order?.toString() ?: "0") }
    var active by remember { mutableStateOf(existing?.active ?: true) }
    var pickedUri by remember { mutableStateOf<Uri?>(null) }
    var typeExpanded by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { pickedUri = it; uriToImagePart(context, it)?.let(onUpload) }
    }

    FormDialog(
        title = if (existing == null) "New banner" else "Edit banner",
        onDismiss = onDismiss,
        saving = saving,
        onSave = {
            onSave(
                BannerInput(
                    title = title.trim(),
                    image_id = uploadedImageId ?: existing?.image_id,
                    image_url = existing?.image_url,
                    subtitle = subtitle,
                    link = link.ifBlank { null },
                    banner_type = bannerType,
                    sort_order = sortOrder.toIntOrNull() ?: 0,
                    active = active,
                ),
            )
        },
    ) {
        LsTextField(title, { title = it }, "Title")
        Spacer(Modifier.height(8.dp))
        LsTextField(subtitle, { subtitle = it }, "Subtitle (optional)")
        Spacer(Modifier.height(8.dp))
        LsTextField(link, { link = it }, "Link (optional)")
        Spacer(Modifier.height(8.dp))
        Text("Banner type", style = MaterialTheme.typography.labelSmall)
        ExposedDropdownMenuBox(expanded = typeExpanded, onExpandedChange = { typeExpanded = it }) {
            OutlinedTextField(
                value = bannerType, onValueChange = {}, readOnly = true,
                modifier = Modifier.menuAnchor(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
            )
            ExposedDropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                BANNER_TYPES.forEach { opt -> DropdownMenuItem(text = { Text(opt) }, onClick = { bannerType = opt; typeExpanded = false }) }
            }
        }
        Spacer(Modifier.height(8.dp))
        LsTextField(sortOrder, { sortOrder = it }, "Sort order", keyboardType = KeyboardType.Number)
        Spacer(Modifier.height(8.dp))
        LabeledSwitchRow("Active", active, { active = it })
        Spacer(Modifier.height(8.dp))
        val previewUrl = pickedUri?.toString() ?: fileUrl(uploadedImageId ?: existing?.image_id) ?: existing?.image_url
        if (previewUrl != null) {
            AsyncImage(model = previewUrl, contentDescription = null, modifier = Modifier.size(72.dp))
            Spacer(Modifier.height(4.dp))
        }
        TextButton(onClick = { picker.launch("image/*") }) { Text("Upload banner image") }
    }
}
