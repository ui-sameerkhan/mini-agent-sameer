package com.lazyshopper.app.feature.admin.catalog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lazyshopper.app.core.data.remote.ApiResult
import com.lazyshopper.app.core.data.remote.dto.Product
import com.lazyshopper.app.core.ui.ActionState
import com.lazyshopper.app.core.ui.UiState
import com.lazyshopper.app.core.ui.components.LsTextField
import com.lazyshopper.app.feature.admin.common.AdminLazyList
import com.lazyshopper.app.feature.admin.common.AdminScreenScaffold
import com.lazyshopper.app.feature.admin.common.AdminSearchField
import com.lazyshopper.app.feature.admin.common.CompactRow
import com.lazyshopper.app.feature.admin.common.ConfirmDialog
import com.lazyshopper.app.feature.admin.common.EmptyOrList
import com.lazyshopper.app.feature.admin.common.FilterChipsRow
import com.lazyshopper.app.feature.admin.common.FormDialog
import com.lazyshopper.app.feature.admin.common.LabeledSwitchRow
import com.lazyshopper.app.feature.admin.common.StatusChip
import com.lazyshopper.app.feature.admin.common.Tone
import com.lazyshopper.app.feature.admin.common.money
import com.lazyshopper.app.feature.admin.data.AdminCatalogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminProductsViewModel @Inject constructor(private val repo: AdminCatalogRepository) : ViewModel() {
    private val _state = MutableStateFlow<UiState<List<Product>>>(UiState.Loading)
    val state: StateFlow<UiState<List<Product>>> = _state.asStateFlow()
    private val _actionState = MutableStateFlow<ActionState>(ActionState.Idle)
    val actionState: StateFlow<ActionState> = _actionState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            when (val r = repo.allProducts()) {
                is ApiResult.Success -> _state.value = UiState.Success(r.data)
                is ApiResult.Failure -> _state.value = UiState.Error(r.message)
            }
        }
    }

    fun approve(id: String, status: String) = runAction { repo.approveProduct(id, status) }
    fun delete(id: String) = runAction { repo.deleteProduct(id) }
    fun saveFlags(id: String, featured: Boolean, inStock: Boolean, stock: Double?, price: Double?, mrp: Double?, lowStock: Double?) =
        runAction { repo.setProductFlags(id, featured, inStock, stock, price, mrp, lowStock) }

    private fun runAction(block: suspend () -> ApiResult<*>) {
        viewModelScope.launch {
            _actionState.value = ActionState.InFlight
            when (val r = block()) {
                is ApiResult.Success -> { _actionState.value = ActionState.Done; load() }
                is ApiResult.Failure -> _actionState.value = ActionState.Failed(r.message)
            }
        }
    }
}

@Composable
fun AdminProductsScreen(onMenuClick: () -> Unit, viewModel: AdminProductsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val actionState by viewModel.actionState.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }

    var query by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf<String?>(null) }
    var editingFlags by remember { mutableStateOf<Product?>(null) }
    var confirmDelete by remember { mutableStateOf<Product?>(null) }

    AdminScreenScaffold(title = "Products", onMenuClick = onMenuClick) { padding ->
        Column(Modifier.padding(padding)) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                AdminSearchField(query, { query = it }, placeholder = "Search products…")
                Spacer(Modifier.height(8.dp))
                FilterChipsRow(listOf("pending", "approved", "rejected"), statusFilter, { statusFilter = it })
            }
            val s = state
            val all = (s as? UiState.Success)?.data.orEmpty()
            val filtered = all.filter {
                (statusFilter == null || it.status == statusFilter) &&
                    (query.isBlank() || it.name.contains(query, ignoreCase = true) || it.shop_name?.contains(query, ignoreCase = true) == true)
            }
            EmptyOrList(loading = s is UiState.Loading, error = (s as? UiState.Error)?.message, isEmpty = filtered.isEmpty() && s is UiState.Success, emptyMessage = "No products found", onRetry = viewModel::load) {
                AdminLazyList {
                    items(filtered, key = { it.id }) { p ->
                        var menuOpen by remember { mutableStateOf(false) }
                        CompactRow(
                            title = p.name,
                            subtitle = "${p.shop_name.orEmpty()} · ${p.category}${p.subcategory?.let { " / $it" } ?: ""}",
                            meta = "${p.price.money()}${p.mrp?.let { " (MRP ${it.money()})" } ?: ""} · stock ${p.stock}",
                            badge = { StatusChip(p.status ?: "pending") },
                            trailing = {
                                Row {
                                    IconButton(onClick = { menuOpen = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "Actions") }
                                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                        if (p.status != "approved") DropdownMenuItem(text = { Text("Approve") }, onClick = { menuOpen = false; viewModel.approve(p.id, "approved") })
                                        if (p.status != "rejected") DropdownMenuItem(text = { Text("Reject") }, onClick = { menuOpen = false; viewModel.approve(p.id, "rejected") })
                                        if (p.status != "pending") DropdownMenuItem(text = { Text("Set pending") }, onClick = { menuOpen = false; viewModel.approve(p.id, "pending") })
                                        DropdownMenuItem(text = { Text("Edit flags") }, onClick = { menuOpen = false; editingFlags = p })
                                        DropdownMenuItem(text = { Text("Delete") }, onClick = { menuOpen = false; confirmDelete = p })
                                    }
                                }
                            },
                        ) {
                            Row {
                                if (p.featured) StatusChip("Featured", tone = Tone.INFO)
                                Spacer(Modifier.width(6.dp))
                                if (!p.in_stock) StatusChip("Out of stock", tone = Tone.ERROR)
                            }
                        }
                    }
                }
            }
        }
    }

    editingFlags?.let { p ->
        ProductFlagsDialog(
            product = p,
            saving = actionState is ActionState.InFlight,
            onDismiss = { editingFlags = null },
            onSave = { featured, inStock, stock, price, mrp, lowStock ->
                viewModel.saveFlags(p.id, featured, inStock, stock, price, mrp, lowStock)
                editingFlags = null
            },
        )
    }
    confirmDelete?.let { p ->
        ConfirmDialog(
            title = "Delete product",
            message = "Delete \"${p.name}\"? This cannot be undone.",
            destructive = true,
            confirmLabel = "Delete",
            onConfirm = { viewModel.delete(p.id); confirmDelete = null },
            onDismiss = { confirmDelete = null },
        )
    }
}

@Composable
private fun ProductFlagsDialog(
    product: Product,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (featured: Boolean, inStock: Boolean, stock: Double?, price: Double?, mrp: Double?, lowStock: Double?) -> Unit,
) {
    var featured by remember { mutableStateOf(product.featured) }
    var inStock by remember { mutableStateOf(product.in_stock) }
    var stock by remember { mutableStateOf(product.stock.toString()) }
    var price by remember { mutableStateOf(product.price.toString()) }
    var mrp by remember { mutableStateOf(product.mrp?.toString().orEmpty()) }
    var lowStock by remember { mutableStateOf(product.low_stock_threshold?.toString().orEmpty()) }

    FormDialog(title = "Quick edit · ${product.name}", onDismiss = onDismiss, saving = saving, onSave = {
        onSave(
            featured, inStock,
            stock.toDoubleOrNull(), price.toDoubleOrNull(), mrp.toDoubleOrNull(), lowStock.toDoubleOrNull(),
        )
    }) {
        LabeledSwitchRow("Featured", featured, { featured = it })
        Spacer(Modifier.height(8.dp))
        LabeledSwitchRow("In stock", inStock, { inStock = it })
        Spacer(Modifier.height(8.dp))
        LsTextField(stock, { stock = it }, "Stock", keyboardType = KeyboardType.Decimal)
        Spacer(Modifier.height(8.dp))
        LsTextField(price, { price = it }, "Price (₹)", keyboardType = KeyboardType.Decimal)
        Spacer(Modifier.height(8.dp))
        LsTextField(mrp, { mrp = it }, "MRP (₹, optional)", keyboardType = KeyboardType.Decimal)
        Spacer(Modifier.height(8.dp))
        LsTextField(lowStock, { lowStock = it }, "Low stock threshold (optional)", keyboardType = KeyboardType.Decimal)
    }
}
