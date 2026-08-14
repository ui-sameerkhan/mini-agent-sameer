package com.lazyshopper.app.feature.shopkeeper.products

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lazyshopper.app.core.data.remote.dto.Shop
import com.lazyshopper.app.core.theme.LsPrimary
import com.lazyshopper.app.core.ui.ActionState
import com.lazyshopper.app.core.ui.components.FullScreenLoading
import com.lazyshopper.app.core.ui.components.LsPrimaryButton
import com.lazyshopper.app.core.ui.components.LsTextField
import com.lazyshopper.app.core.ui.components.ScreenPadding
import com.lazyshopper.app.feature.shopkeeper.common.fileSizeMb
import com.lazyshopper.app.feature.shopkeeper.common.statusLabel
import com.lazyshopper.app.feature.shopkeeper.common.uriToMultipart

private const val MAX_VIDEO_MB = 60.0

@Composable
fun ProductFormScreen(
    productId: String?,
    onDone: () -> Unit,
    viewModel: ProductFormViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var videoError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(productId) { viewModel.init(productId) }

    LaunchedEffect(state.submitState) {
        if (state.submitState is ActionState.Done) onDone()
    }

    if (state.loading) {
        FullScreenLoading()
        return
    }

    if (state.shops.isEmpty()) {
        Column(Modifier.fillMaxSize().padding(ScreenPadding)) {
            Text(if (state.editingId != null) "Edit product" else "Add product", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(16.dp))
            Text(
                "You need an approved shop before adding products.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
        return
    }

    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { viewModel.uploadImage(uriToMultipart(context, it)) }
    }
    val videoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val sizeMb = fileSizeMb(context, uri)
            if (sizeMb > MAX_VIDEO_MB) {
                videoError = "Video is ${"%.1f".format(sizeMb)}MB — max is ${MAX_VIDEO_MB.toInt()}MB"
            } else {
                videoError = null
                viewModel.uploadVideo(uriToMultipart(context, uri))
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(ScreenPadding),
    ) {
        Text(if (state.editingId != null) "Edit product" else "Add product", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        Text("Shop", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(4.dp))
        ShopPicker(shops = state.shops, selectedId = state.shopId, onSelect = viewModel::setShopId)

        Spacer(Modifier.height(16.dp))
        LsTextField(value = state.name, onValueChange = viewModel::setName, label = "Product name")

        Spacer(Modifier.height(16.dp))
        Text("Category", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(4.dp))
        Column {
            PRODUCT_CATEGORIES.forEach { cat ->
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = state.category == cat, onClick = { viewModel.setCategory(cat) })
                    Text(statusLabel(cat), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        if (state.isNonveg) {
            Spacer(Modifier.height(8.dp))
            Text("Subcategory", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NONVEG_SUBCATEGORIES.forEach { sub ->
                    OutlinedButton(onClick = { viewModel.setSubcategory(sub) }) {
                        Text(statusLabel(sub) + if (state.subcategory == sub) " ✓" else "")
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        LsTextField(value = state.unitType, onValueChange = viewModel::setUnitType, label = "Unit type (e.g. kg, pc, litre)")

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LsTextField(
                value = state.price,
                onValueChange = viewModel::setPrice,
                label = "Price",
                keyboardType = KeyboardType.Decimal,
                modifier = Modifier.weight(1f),
            )
            LsTextField(
                value = state.mrp,
                onValueChange = viewModel::setMrp,
                label = "MRP (optional)",
                keyboardType = KeyboardType.Decimal,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LsTextField(
                value = state.stock,
                onValueChange = viewModel::setStock,
                label = "Stock",
                keyboardType = KeyboardType.Decimal,
                modifier = Modifier.weight(1f),
            )
            LsTextField(
                value = state.lowStockThreshold,
                onValueChange = viewModel::setLowStockThreshold,
                label = "Low stock alert (optional)",
                keyboardType = KeyboardType.Decimal,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(12.dp))
        LsTextField(value = state.brand, onValueChange = viewModel::setBrand, label = "Brand (optional)")

        Spacer(Modifier.height(12.dp))
        LsTextField(
            value = state.description,
            onValueChange = viewModel::setDescription,
            label = "Description (optional)",
            singleLine = false,
        )

        Spacer(Modifier.height(12.dp))
        LsTextField(
            value = state.commissionPct,
            onValueChange = viewModel::setCommissionPct,
            label = "Commission override % (optional)",
            keyboardType = KeyboardType.Decimal,
        )

        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Featured product", style = MaterialTheme.typography.bodyLarge)
            Switch(checked = state.featured, onCheckedChange = viewModel::setFeatured)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("In stock", style = MaterialTheme.typography.bodyLarge)
            Switch(checked = state.inStock, onCheckedChange = viewModel::setInStock)
        }

        Spacer(Modifier.height(20.dp))
        Text("Photo", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (state.imageId != null) "Uploaded" else "Not uploaded",
                style = MaterialTheme.typography.bodyMedium,
                color = if (state.imageId != null) LsPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            if (state.uploadingImage) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                OutlinedButton(onClick = { imageLauncher.launch("image/*") }) {
                    Text(if (state.imageId != null) "Replace" else "Upload")
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("Promo video (optional)", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (state.promoVideoId != null) "Uploaded" else "Not uploaded",
                style = MaterialTheme.typography.bodyMedium,
                color = if (state.promoVideoId != null) LsPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            if (state.uploadingVideo) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                if (state.promoVideoId != null) {
                    OutlinedButton(onClick = viewModel::clearVideo) { Text("Remove") }
                    Spacer(Modifier.width(8.dp))
                }
                OutlinedButton(onClick = { videoLauncher.launch("video/*") }) {
                    Text(if (state.promoVideoId != null) "Replace" else "Upload")
                }
            }
        }
        if (videoError != null) {
            Spacer(Modifier.height(6.dp))
            Text(videoError.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        if (state.errorMessage != null) {
            Spacer(Modifier.height(12.dp))
            Text(state.errorMessage.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        if (state.submitState is ActionState.Failed) {
            Spacer(Modifier.height(10.dp))
            Text((state.submitState as ActionState.Failed).message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(20.dp))
        LsPrimaryButton(
            text = if (state.editingId != null) "Save changes" else "Create product",
            onClick = viewModel::submit,
            loading = state.submitState is ActionState.InFlight,
            enabled = state.canSubmit && !state.uploadingImage && !state.uploadingVideo,
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ShopPicker(shops: List<Shop>, selectedId: String?, onSelect: (String) -> Unit) {
    Column {
        shops.forEach { shop ->
            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = selectedId == shop.id, onClick = { onSelect(shop.id) })
                Text("${shop.name} — ${shop.area}", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
