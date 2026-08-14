package com.lazyshopper.app.feature.shopkeeper.kyc

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.lazyshopper.app.core.theme.LsError
import com.lazyshopper.app.core.theme.LsPrimary
import com.lazyshopper.app.core.theme.LsWarning
import com.lazyshopper.app.core.ui.ActionState
import com.lazyshopper.app.core.ui.components.FullScreenLoading
import com.lazyshopper.app.core.ui.components.LsPrimaryButton
import com.lazyshopper.app.core.ui.components.LsTextField
import com.lazyshopper.app.core.ui.components.ScreenPadding
import com.lazyshopper.app.feature.shopkeeper.common.fetchCurrentLocation
import com.lazyshopper.app.feature.shopkeeper.common.uriToMultipart
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ShopkeeperKycScreen(
    onDone: () -> Unit,
    onBack: () -> Unit,
    viewModel: KycViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(state.submitState) {
        if (state.submitState is ActionState.Done) onDone()
    }

    if (state.loading) {
        FullScreenLoading()
        return
    }

    if (state.kycStatus == "approved") {
        Column(
            modifier = Modifier.fillMaxSize().padding(ScreenPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("Your KYC is already approved.", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onBack) { Text("Back") }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(ScreenPadding),
    ) {
        Text("Shopkeeper KYC", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(4.dp))

        when (state.kycStatus) {
            "submitted" -> KycBannerCard("Your documents are under review. We'll notify you once approved.", LsWarning)
            "rejected" -> KycBannerCard("Rejected: ${state.rejectReason ?: "please review and resubmit your documents."}", LsError)
            else -> Text(
                "Upload the required documents so admin can approve your shopkeeper account and unlock shop/product creation.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(20.dp))
        Text("Documents", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))

        DocPicker("Aadhaar card", state.aadhaarId != null, state.uploadingField == "aadhaar") { uri ->
            viewModel.uploadDoc("aadhaar", uriToMultipart(context, uri))
        }
        DocPicker("PAN card", state.panId != null, state.uploadingField == "pan") { uri ->
            viewModel.uploadDoc("pan", uriToMultipart(context, uri))
        }
        DocPicker("Selfie", state.selfieId != null, state.uploadingField == "selfie") { uri ->
            viewModel.uploadDoc("selfie", uriToMultipart(context, uri))
        }
        DocPicker("Shop photo", state.shopPhotoId != null, state.uploadingField == "shop_photo") { uri ->
            viewModel.uploadDoc("shop_photo", uriToMultipart(context, uri))
        }

        Spacer(Modifier.height(16.dp))
        Text("Optional", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        LsTextField(value = state.gstNumber, onValueChange = viewModel::setGstNumber, label = "GST number (optional)")
        Spacer(Modifier.height(12.dp))
        LsTextField(value = state.fssaiNumber, onValueChange = viewModel::setFssaiNumber, label = "FSSAI number (optional)")
        Spacer(Modifier.height(12.dp))
        LsTextField(value = state.addressText, onValueChange = viewModel::setAddressText, label = "Shop address (optional)")

        Spacer(Modifier.height(20.dp))
        LocationPicker(
            lat = state.lat,
            lng = state.lng,
            detecting = state.detectingLocation,
            onDetected = viewModel::setLocation,
            onDetecting = viewModel::setDetectingLocation,
        )

        if (state.errorMessage != null) {
            Spacer(Modifier.height(10.dp))
            Text(state.errorMessage.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        if (state.submitState is ActionState.Failed) {
            Spacer(Modifier.height(10.dp))
            Text((state.submitState as ActionState.Failed).message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(24.dp))
        LsPrimaryButton(
            text = if (state.kycStatus == "rejected") "Resubmit KYC" else "Submit KYC",
            onClick = viewModel::submit,
            loading = state.submitState is ActionState.InFlight,
            enabled = state.kycStatus != "submitted",
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun KycBannerCard(text: String, tint: Color) {
    Card(colors = CardDefaults.cardColors(containerColor = tint.copy(alpha = 0.12f)), modifier = Modifier.fillMaxWidth()) {
        Text(text, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
    }
}

/** Reusable status banner for the Dashboard (and anywhere else in the shopkeeper shell). */
@Composable
fun ShopkeeperKycStatusBanner(kycStatus: String?, rejectReason: String?, onComplete: () -> Unit) {
    if (kycStatus == "approved") return
    val (text, tint, actionLabel) = when (kycStatus) {
        "rejected" -> Triple("KYC rejected — ${rejectReason ?: "please review and resubmit your documents."}", LsError, "Resubmit")
        "submitted" -> Triple("Your KYC is under review. We'll notify you once approved.", LsWarning, null)
        else -> Triple("Complete your KYC to unlock shop & product creation.", LsWarning, "Complete KYC")
    }
    Card(colors = CardDefaults.cardColors(containerColor = tint.copy(alpha = 0.12f)), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            if (actionLabel != null) {
                TextButton(onClick = onComplete) { Text(actionLabel) }
            }
        }
    }
}

@Composable
private fun DocPicker(label: String, uploaded: Boolean, uploading: Boolean, onPicked: (Uri) -> Unit) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> uri?.let(onPicked) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                if (uploaded) "Uploaded" else "Not uploaded",
                style = MaterialTheme.typography.bodySmall,
                color = if (uploaded) LsPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (uploading) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
        } else {
            OutlinedButton(onClick = { launcher.launch("image/*") }) {
                Text(if (uploaded) "Replace" else "Upload")
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun LocationPicker(
    lat: Double?,
    lng: Double?,
    detecting: Boolean,
    onDetected: (Double, Double) -> Unit,
    onDetecting: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val permissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    Column {
        Text("Shop location", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            if (lat != null && lng != null) "Detected: %.5f, %.5f".format(lat, lng) else "Not detected yet",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = {
                if (!permissionState.status.isGranted) {
                    permissionState.launchPermissionRequest()
                } else {
                    onDetecting(true)
                    scope.launch {
                        val loc = fetchCurrentLocation(context)
                        if (loc != null) onDetected(loc.first, loc.second) else onDetecting(false)
                    }
                }
            },
        ) {
            if (detecting) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Text("Detect my location")
            }
        }
    }
}
