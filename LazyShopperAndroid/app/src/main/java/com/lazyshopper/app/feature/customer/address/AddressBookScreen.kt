package com.lazyshopper.app.feature.customer.address

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lazyshopper.app.core.ui.ActionState
import com.lazyshopper.app.core.ui.UiState
import com.lazyshopper.app.core.ui.components.EmptyState
import com.lazyshopper.app.core.ui.components.ErrorState
import com.lazyshopper.app.core.ui.components.FullScreenLoading
import com.lazyshopper.app.core.ui.components.LsPrimaryButton
import com.lazyshopper.app.core.ui.components.LsTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressBookScreen(
    onBack: () -> Unit,
    viewModel: AddressViewModel = hiltViewModel(),
) {
    val addresses by viewModel.addresses.collectAsState()
    val form by viewModel.form.collectAsState()
    val actionState by viewModel.actionState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Address Book") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.startAdd() }) { Icon(Icons.Default.Add, contentDescription = "Add address") }
        },
    ) { padding ->
        when (val s = addresses) {
            is UiState.Loading -> FullScreenLoading(Modifier.padding(padding))
            is UiState.Error -> ErrorState(s.message, Modifier.padding(padding), onRetry = viewModel::load)
            is UiState.Success -> {
                if (s.data.isEmpty()) {
                    EmptyState("No saved addresses yet.", Modifier.padding(padding), actionLabel = "Add address", onAction = viewModel::startAdd)
                } else {
                    LazyColumn(
                        modifier = Modifier.padding(padding),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(s.data, key = { it.id }) { addr ->
                            Card(shape = RoundedCornerShape(16.dp)) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(addr.label, style = MaterialTheme.typography.titleSmall)
                                            if (addr.is_default) {
                                                Spacer(Modifier.width(6.dp))
                                                Text("Default", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                        Row {
                                            IconButton(onClick = { viewModel.startEdit(addr) }) { Icon(Icons.Default.Edit, contentDescription = "Edit") }
                                            IconButton(onClick = { viewModel.delete(addr.id) }) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error) }
                                        }
                                    }
                                    Text(addr.address, style = MaterialTheme.typography.bodyMedium)
                                    Text(addr.phone, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    if (!addr.is_default) {
                                        Spacer(Modifier.height(6.dp))
                                        TextButton(onClick = { viewModel.setDefault(addr.id) }) { Text("Set as default") }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            UiState.Idle -> {}
        }
    }

    if (form != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissForm,
            title = { Text(if (form?.id != null) "Edit address" else "Add address") },
            text = {
                Column {
                    LsTextField(value = form?.label.orEmpty(), onValueChange = { v -> viewModel.updateForm { it.copy(label = v) } }, label = "Label (e.g. Home, Work)")
                    Spacer(Modifier.height(8.dp))
                    LsTextField(value = form?.address.orEmpty(), onValueChange = { v -> viewModel.updateForm { it.copy(address = v) } }, label = "Full address", singleLine = false)
                    Spacer(Modifier.height(8.dp))
                    LsTextField(
                        value = form?.phone.orEmpty(),
                        onValueChange = { v -> viewModel.updateForm { it.copy(phone = v) } },
                        label = "Phone number",
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                        Checkbox(checked = form?.isDefault ?: false, onCheckedChange = { v -> viewModel.updateForm { it.copy(isDefault = v) } })
                        Text("Set as default address")
                    }
                    if (actionState is ActionState.Failed) {
                        Text((actionState as ActionState.Failed).message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                LsPrimaryButton(text = "Save", onClick = viewModel::save, loading = actionState is ActionState.InFlight, modifier = Modifier.padding(0.dp))
            },
            dismissButton = { TextButton(onClick = viewModel::dismissForm) { Text("Cancel") } },
        )
    }
}
