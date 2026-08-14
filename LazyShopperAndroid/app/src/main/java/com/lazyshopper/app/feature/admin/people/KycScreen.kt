package com.lazyshopper.app.feature.admin.people

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.lazyshopper.app.core.data.remote.ApiResult
import com.lazyshopper.app.core.data.remote.dto.KycRecord
import com.lazyshopper.app.core.ui.ActionState
import com.lazyshopper.app.core.ui.UiState
import com.lazyshopper.app.core.ui.components.LsTextField
import com.lazyshopper.app.feature.admin.common.AdminLazyList
import com.lazyshopper.app.feature.admin.common.AdminScreenScaffold
import com.lazyshopper.app.feature.admin.common.CompactRow
import com.lazyshopper.app.feature.admin.common.EmptyOrList
import com.lazyshopper.app.feature.admin.common.FilterChipsRow
import com.lazyshopper.app.feature.admin.common.FormDialog
import com.lazyshopper.app.feature.admin.common.StatusChip
import com.lazyshopper.app.feature.admin.common.fileUrl
import com.lazyshopper.app.feature.admin.data.AdminPeopleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private val KYC_STATUSES = listOf("submitted", "approved", "rejected")

@HiltViewModel
class KycViewModel @Inject constructor(private val repo: AdminPeopleRepository) : ViewModel() {
    private val _state = MutableStateFlow<UiState<List<KycRecord>>>(UiState.Loading)
    val state: StateFlow<UiState<List<KycRecord>>> = _state.asStateFlow()
    private val _actionState = MutableStateFlow<ActionState>(ActionState.Idle)
    val actionState: StateFlow<ActionState> = _actionState.asStateFlow()

    fun load(status: String? = "submitted") {
        viewModelScope.launch {
            _state.value = UiState.Loading
            when (val r = repo.kycList(status)) {
                is ApiResult.Success -> _state.value = UiState.Success(r.data)
                is ApiResult.Failure -> _state.value = UiState.Error(r.message)
            }
        }
    }

    fun review(userId: String, status: String, reason: String?, reload: () -> Unit) {
        viewModelScope.launch {
            _actionState.value = ActionState.InFlight
            when (val r = repo.reviewKyc(userId, status, reason)) {
                is ApiResult.Success -> { _actionState.value = ActionState.Done; reload() }
                is ApiResult.Failure -> _actionState.value = ActionState.Failed(r.message)
            }
        }
    }
}

@Composable
fun AdminKycScreen(onMenuClick: () -> Unit, viewModel: KycViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val actionState by viewModel.actionState.collectAsState()
    var statusFilter by remember { mutableStateOf<String?>("submitted") }
    var rejecting by remember { mutableStateOf<KycRecord?>(null) }
    val context = LocalContext.current

    fun reload() = viewModel.load(statusFilter)
    LaunchedEffect(statusFilter) { reload() }

    AdminScreenScaffold(title = "KYC Approvals", onMenuClick = onMenuClick) { padding ->
        Column(Modifier.padding(padding)) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                FilterChipsRow(KYC_STATUSES, statusFilter, { statusFilter = it }, allLabel = "All")
            }
            val s = state
            val items = (s as? UiState.Success)?.data.orEmpty()
            EmptyOrList(loading = s is UiState.Loading, error = (s as? UiState.Error)?.message, isEmpty = items.isEmpty() && s is UiState.Success, emptyMessage = "No KYC submissions", onRetry = { reload() }) {
                AdminLazyList {
                    items(items, key = { it.user_id }) { k ->
                        CompactRow(
                            title = "${k.user_name ?: "User"} · ${k.role}",
                            subtitle = "${k.user_email ?: "-"} · ${k.user_phone ?: "-"}",
                            meta = "submitted ${k.submitted_at ?: "-"}${k.reason?.let { " · reason: $it" } ?: ""}",
                            badge = { StatusChip(k.status) },
                        ) {
                            // document thumbnails
                            Row {
                                DocThumb("Aadhaar", k.aadhaar_id)
                                DocThumb("PAN", k.pan_id)
                                DocThumb("Selfie", k.selfie_id)
                                if (k.role == "delivery") DocThumb("DL", k.dl_id)
                                if (k.role == "shopkeeper") DocThumb("Shop", k.shop_photo_id)
                            }
                            Spacer(Modifier.height(4.dp))
                            if (k.vehicle_number != null) Text("Vehicle: ${k.vehicle_number}", style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
                            if (k.gst_number != null) Text("GST: ${k.gst_number}", style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
                            if (k.fssai_number != null) Text("FSSAI: ${k.fssai_number}", style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
                            if (k.lat != null && k.lng != null) {
                                TextButton(onClick = {
                                    val uri = Uri.parse("geo:${k.lat},${k.lng}?q=${k.lat},${k.lng}(${Uri.encode(k.user_name ?: "Location")})")
                                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                                }) { Text("📍 View location on map") }
                            }
                            if (k.address_text != null) Text(k.address_text, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                            if (k.status == "submitted") {
                                Row(Modifier.padding(top = 4.dp)) {
                                    TextButton(onClick = { viewModel.review(k.user_id, "approved", null) { reload() } }, enabled = actionState !is ActionState.InFlight) { Text("Approve") }
                                    Spacer(Modifier.width(8.dp))
                                    TextButton(onClick = { rejecting = k }, enabled = actionState !is ActionState.InFlight) { Text("Reject") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    rejecting?.let { k ->
        var reason by remember { mutableStateOf("") }
        FormDialog(
            title = "Reject KYC · ${k.user_name}",
            onDismiss = { rejecting = null },
            saving = actionState is ActionState.InFlight,
            onSave = { viewModel.review(k.user_id, "rejected", reason.ifBlank { null }) { reload() }; rejecting = null },
            saveLabel = "Reject",
        ) {
            LsTextField(reason, { reason = it }, "Reason (shown to user)")
        }
    }
}

@Composable
private fun DocThumb(label: String, fid: String?) {
    Column(Modifier.padding(end = 6.dp)) {
        val url = fileUrl(fid)
        if (url != null) {
            AsyncImage(model = url, contentDescription = label, modifier = Modifier.size(56.dp).clip(RoundedCornerShape(6.dp)))
        }
        Text(label, style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
    }
}
