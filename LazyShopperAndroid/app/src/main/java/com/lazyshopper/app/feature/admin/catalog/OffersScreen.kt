package com.lazyshopper.app.feature.admin.catalog

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import com.lazyshopper.app.core.data.remote.dto.Offer
import com.lazyshopper.app.core.data.remote.dto.OfferInput
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
import com.lazyshopper.app.feature.admin.common.Tone
import com.lazyshopper.app.feature.admin.common.fileUrl
import com.lazyshopper.app.feature.admin.common.money
import com.lazyshopper.app.feature.admin.common.uriToImagePart
import com.lazyshopper.app.feature.admin.data.AdminCatalogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OffersViewModel @Inject constructor(private val repo: AdminCatalogRepository) : ViewModel() {
    private val _state = MutableStateFlow<UiState<List<Offer>>>(UiState.Loading)
    val state: StateFlow<UiState<List<Offer>>> = _state.asStateFlow()
    private val _actionState = MutableStateFlow<ActionState>(ActionState.Idle)
    val actionState: StateFlow<ActionState> = _actionState.asStateFlow()
    private val _uploadedImageId = MutableStateFlow<String?>(null)
    val uploadedImageId: StateFlow<String?> = _uploadedImageId.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            when (val r = repo.offers()) {
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

    fun save(existingId: String?, input: OfferInput) {
        viewModelScope.launch {
            _actionState.value = ActionState.InFlight
            val r = if (existingId != null) repo.updateOffer(existingId, input) else repo.createOffer(input)
            when (r) {
                is ApiResult.Success -> { _actionState.value = ActionState.Done; load() }
                is ApiResult.Failure -> _actionState.value = ActionState.Failed(r.message)
            }
        }
    }

    fun toggle(id: String, active: Boolean) = viewModelScope.launch {
        when (val r = repo.toggleOffer(id, active)) {
            is ApiResult.Success -> load()
            is ApiResult.Failure -> _actionState.value = ActionState.Failed(r.message)
        }
    }

    fun delete(id: String) = viewModelScope.launch {
        when (val r = repo.deleteOffer(id)) {
            is ApiResult.Success -> load()
            is ApiResult.Failure -> _actionState.value = ActionState.Failed(r.message)
        }
    }
}

private val OFFER_TYPES = listOf("product", "category", "store", "cart")
private val DISCOUNT_TYPES = listOf("percent", "flat")

@Composable
fun AdminOffersScreen(onMenuClick: () -> Unit, viewModel: OffersViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val actionState by viewModel.actionState.collectAsState()
    val uploadedImageId by viewModel.uploadedImageId.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }

    var editing by remember { mutableStateOf<Offer?>(null) }
    var creating by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf<Offer?>(null) }

    AdminScreenScaffold(
        title = "Offers",
        onMenuClick = onMenuClick,
        fab = { FloatingActionButton(onClick = { viewModel.clearUploadedImage(); creating = true }) { Icon(Icons.Default.Add, contentDescription = "Add offer") } },
    ) { padding ->
        val s = state
        val items = (s as? UiState.Success)?.data.orEmpty()
        EmptyOrList(loading = s is UiState.Loading, error = (s as? UiState.Error)?.message, isEmpty = items.isEmpty() && s is UiState.Success, emptyMessage = "No offers yet", onRetry = viewModel::load) {
            AdminLazyList(modifier = Modifier.padding(padding)) {
                items(items, key = { it.id }) { o ->
                    CompactRow(
                        title = o.title,
                        subtitle = "${o.offer_type} · ${if (o.discount_type == "percent") "${o.value.toInt()}% off" else "${o.value.money()} off"} · min ${o.min_order.money()}",
                        badge = { StatusChip(o.state ?: if (o.active) "live" else "disabled") },
                        trailing = {
                            Row {
                                Switch(checked = o.active, onCheckedChange = { viewModel.toggle(o.id, it) })
                                IconButton(onClick = { viewModel.clearUploadedImage(); editing = o }) { Icon(Icons.Default.Edit, contentDescription = "Edit") }
                                IconButton(onClick = { confirmDelete = o }) { Icon(Icons.Default.Delete, contentDescription = "Delete") }
                            }
                        },
                    )
                }
            }
        }
    }

    if (creating) {
        OfferFormDialog(null, actionState is ActionState.InFlight, uploadedImageId, onUpload = viewModel::uploadImage, onDismiss = { creating = false }, onSave = { input -> viewModel.save(null, input); creating = false })
    }
    editing?.let { o ->
        OfferFormDialog(o, actionState is ActionState.InFlight, uploadedImageId, onUpload = viewModel::uploadImage, onDismiss = { editing = null }, onSave = { input -> viewModel.save(o.id, input); editing = null })
    }
    confirmDelete?.let { o ->
        ConfirmDialog("Delete offer", "Delete \"${o.title}\"?", destructive = true, confirmLabel = "Delete", onConfirm = { viewModel.delete(o.id); confirmDelete = null }, onDismiss = { confirmDelete = null })
    }
}

@Composable
private fun OfferFormDialog(
    existing: Offer?,
    saving: Boolean,
    uploadedImageId: String?,
    onUpload: (okhttp3.MultipartBody.Part) -> Unit,
    onDismiss: () -> Unit,
    onSave: (OfferInput) -> Unit,
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(existing?.title.orEmpty()) }
    var offerType by remember { mutableStateOf(existing?.offer_type ?: "product") }
    var targetId by remember { mutableStateOf(existing?.target_id.orEmpty()) }
    var discountType by remember { mutableStateOf(existing?.discount_type ?: "percent") }
    var value by remember { mutableStateOf(existing?.value?.toString().orEmpty()) }
    var minOrder by remember { mutableStateOf(existing?.min_order?.toString() ?: "0") }
    var showBadge by remember { mutableStateOf(existing?.show_badge ?: true) }
    var active by remember { mutableStateOf(existing?.active ?: true) }
    var pickedUri by remember { mutableStateOf<Uri?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { pickedUri = it; uriToImagePart(context, it)?.let(onUpload) }
    }

    FormDialog(
        title = if (existing == null) "New offer" else "Edit offer",
        onDismiss = onDismiss,
        saving = saving,
        onSave = {
            onSave(
                OfferInput(
                    title = title.trim(),
                    offer_type = offerType,
                    target_id = targetId.ifBlank { null },
                    discount_type = discountType,
                    value = value.toDoubleOrNull() ?: 0.0,
                    min_order = minOrder.toDoubleOrNull() ?: 0.0,
                    show_badge = showBadge,
                    active = active,
                    image_id = uploadedImageId ?: existing?.image_id,
                    image_url = existing?.image_url,
                ),
            )
        },
    ) {
        LsTextField(title, { title = it }, "Title")
        Spacer(Modifier.height(8.dp))
        Text("Offer type (product / category / store / cart)", style = MaterialTheme.typography.labelSmall)
        SimpleDropdown(OFFER_TYPES, offerType) { offerType = it }
        Spacer(Modifier.height(8.dp))
        if (offerType != "cart") {
            LsTextField(targetId, { targetId = it }, "Target id (product/category/shop id)")
            Spacer(Modifier.height(8.dp))
        }
        Text("Discount type", style = MaterialTheme.typography.labelSmall)
        SimpleDropdown(DISCOUNT_TYPES, discountType) { discountType = it }
        Spacer(Modifier.height(8.dp))
        LsTextField(value, { value = it }, "Value", keyboardType = KeyboardType.Decimal)
        Spacer(Modifier.height(8.dp))
        LsTextField(minOrder, { minOrder = it }, "Minimum order (₹)", keyboardType = KeyboardType.Decimal)
        Spacer(Modifier.height(8.dp))
        LabeledSwitchRow("Show badge", showBadge, { showBadge = it })
        Spacer(Modifier.height(8.dp))
        LabeledSwitchRow("Active", active, { active = it })
        Spacer(Modifier.height(8.dp))
        val previewUrl = pickedUri?.toString() ?: fileUrl(uploadedImageId ?: existing?.image_id) ?: existing?.image_url
        if (previewUrl != null) {
            AsyncImage(model = previewUrl, contentDescription = null, modifier = Modifier.size(72.dp))
            Spacer(Modifier.height(4.dp))
        }
        TextButton(onClick = { picker.launch("image/*") }) { Text("Upload thumbnail (recommended 800×600)") }
    }
}

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
private fun SimpleDropdown(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.menuAnchor(),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        )
        androidx.compose.material3.ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { opt ->
                DropdownMenuItem(text = { Text(opt) }, onClick = { onSelect(opt); expanded = false })
            }
        }
    }
}
