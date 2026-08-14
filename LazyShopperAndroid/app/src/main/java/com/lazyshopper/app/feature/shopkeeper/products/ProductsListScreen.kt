package com.lazyshopper.app.feature.shopkeeper.products

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lazyshopper.app.core.data.remote.dto.Product
import com.lazyshopper.app.core.theme.LsAccent
import com.lazyshopper.app.core.theme.LsError
import com.lazyshopper.app.core.theme.LsPrimary
import com.lazyshopper.app.core.ui.UiState
import com.lazyshopper.app.core.ui.components.EmptyState
import com.lazyshopper.app.core.ui.components.ErrorState
import com.lazyshopper.app.core.ui.components.FullScreenLoading
import com.lazyshopper.app.core.ui.components.ScreenPadding
import com.lazyshopper.app.feature.shopkeeper.common.money
import com.lazyshopper.app.feature.shopkeeper.common.statusLabel

@Composable
fun ProductsListScreen(
    onAddProduct: () -> Unit,
    onEditProduct: (String) -> Unit,
    viewModel: ProductsListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var deleteProduct by remember { mutableStateOf<Product?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddProduct) { Icon(Icons.Default.Add, contentDescription = "Add product") }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            when (val s = state.products) {
                is UiState.Loading -> FullScreenLoading()
                is UiState.Error -> ErrorState(s.message, onRetry = viewModel::load)
                is UiState.Idle -> {}
                is UiState.Success -> {
                    if (s.data.isEmpty()) {
                        EmptyState("No products yet. Add your first product to start selling.", actionLabel = "Add product", onAction = onAddProduct)
                    } else {
                        LazyColumn(
                            contentPadding = ScreenPadding,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            items(s.data, key = { it.id }) { product ->
                                ProductCard(
                                    product = product,
                                    onEdit = { onEditProduct(product.id) },
                                    onDelete = { deleteProduct = product },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    deleteProduct?.let { product ->
        AlertDialog(
            onDismissRequest = { deleteProduct = null },
            title = { Text("Delete ${product.name}?") },
            text = { Text("This can't be undone.") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteProduct(product.id); deleteProduct = null }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleteProduct = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun ProductCard(product: Product, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(product.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (!product.in_stock) {
                    Text("Out of stock", style = MaterialTheme.typography.labelMedium, color = LsError)
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "${statusLabel(product.category)}${product.subcategory?.let { " · ${statusLabel(it)}" } ?: ""}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(money(product.price), style = MaterialTheme.typography.titleSmall, color = LsPrimary)
                if (product.mrp != null && product.mrp > product.price) {
                    Spacer(Modifier.width(8.dp))
                    Text(money(product.mrp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.width(8.dp))
                Text("/ ${product.unit_type}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(2.dp))
            Text(
                "Stock: ${product.stock}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (product.featured) {
                Spacer(Modifier.height(2.dp))
                Text("★ Featured", style = MaterialTheme.typography.bodySmall, color = LsAccent)
            }
            if (product.status != null && product.status != "approved") {
                Spacer(Modifier.height(2.dp))
                Text(statusLabel(product.status), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onEdit) { Text("Edit") }
                OutlinedButton(onClick = onDelete) { Text("Delete") }
            }
        }
    }
}
