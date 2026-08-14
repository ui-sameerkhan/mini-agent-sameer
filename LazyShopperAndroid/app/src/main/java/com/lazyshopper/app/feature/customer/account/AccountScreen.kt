package com.lazyshopper.app.feature.customer.account

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lazyshopper.app.core.data.remote.dto.NotificationDto
import com.lazyshopper.app.core.theme.LsAccent
import com.lazyshopper.app.core.theme.LsPrimary
import com.lazyshopper.app.core.ui.ActionState
import com.lazyshopper.app.core.ui.UiState
import com.lazyshopper.app.core.ui.components.ErrorState
import com.lazyshopper.app.core.ui.components.FullScreenLoading
import com.lazyshopper.app.core.ui.components.LsPrimaryButton
import com.lazyshopper.app.core.ui.components.LsTextField
import com.lazyshopper.app.core.ui.components.ScreenPadding

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    onManageAddresses: () -> Unit,
    onReferEarn: () -> Unit,
    onLogout: () -> Unit,
    viewModel: AccountViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Account") }) },
    ) { padding ->
        when (val profileState = state.profile) {
            is UiState.Loading -> FullScreenLoading(Modifier.padding(padding))
            is UiState.Error -> ErrorState(profileState.message, Modifier.padding(padding), onRetry = viewModel::loadProfile)
            is UiState.Success -> {
                val user = profileState.data
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(ScreenPadding),
                ) {
                    ProfileCard(
                        name = user.name ?: "Customer",
                        email = user.email.orEmpty(),
                        editing = state.editing,
                        form = state.form,
                        saveState = state.saveState,
                        onEdit = viewModel::startEdit,
                        onCancel = viewModel::cancelEdit,
                        onNameChange = { v -> viewModel.updateForm { it.copy(name = v) } },
                        onEmailChange = { v -> viewModel.updateForm { it.copy(email = v) } },
                        onPhoneChange = { v -> viewModel.updateForm { it.copy(phone = v) } },
                        onPasswordChange = { v -> viewModel.updateForm { it.copy(password = v) } },
                        onSave = viewModel::saveProfile,
                    )

                    Spacer(Modifier.height(16.dp))
                    Card(Modifier.fillMaxWidth()) {
                        Column {
                            AccountLinkRow(icon = Icons.Filled.LocationOn, label = "Address book", onClick = onManageAddresses)
                            Divider()
                            AccountLinkRow(icon = Icons.Filled.CardGiftcard, label = "Refer & Earn", onClick = onReferEarn)
                        }
                    }

                    Spacer(Modifier.height(20.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Notifications", style = MaterialTheme.typography.titleMedium)
                        if (state.unreadCount > 0) {
                            TextButton(onClick = viewModel::markAllNotificationsRead) { Text("Mark all read") }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    if (state.notifications.isEmpty()) {
                        Text(
                            if (state.notificationsLoading) "Loading…" else "No notifications yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Card(Modifier.fillMaxWidth()) {
                            Column {
                                state.notifications.forEachIndexed { index, n ->
                                    NotificationRow(n, onClick = { if (!n.read) viewModel.markNotificationRead(n.id) })
                                    if (index != state.notifications.lastIndex) Divider()
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(28.dp))
                    OutlinedButton(onClick = { viewModel.logout(onLogout) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Logout")
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
            UiState.Idle -> {}
        }
    }
}

@Composable
private fun ProfileCard(
    name: String,
    email: String,
    editing: Boolean,
    form: ProfileFormState,
    saveState: ActionState,
    onEdit: () -> Unit,
    onCancel: () -> Unit,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            if (!editing) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(52.dp).background(LsPrimary.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(name.firstOrNull()?.uppercase() ?: "?", style = MaterialTheme.typography.titleLarge, color = LsPrimary, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(name, style = MaterialTheme.typography.titleMedium)
                        if (email.isNotBlank()) Text(email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = onEdit) { Text("Edit") }
                }
            } else {
                Text("Edit profile", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                LsTextField(value = form.name, onValueChange = onNameChange, label = "Name")
                Spacer(Modifier.height(10.dp))
                LsTextField(value = form.email, onValueChange = onEmailChange, label = "Email", keyboardType = androidx.compose.ui.text.input.KeyboardType.Email)
                Spacer(Modifier.height(10.dp))
                LsTextField(value = form.phone, onValueChange = onPhoneChange, label = "Phone", keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone)
                Spacer(Modifier.height(10.dp))
                LsTextField(value = form.password, onValueChange = onPasswordChange, label = "New password (optional)", isPassword = true)
                if (saveState is ActionState.Failed) {
                    Spacer(Modifier.height(8.dp))
                    Text(saveState.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onCancel, enabled = saveState !is ActionState.InFlight) { Text("Cancel") }
                    LsPrimaryButton(text = "Save", onClick = onSave, loading = saveState is ActionState.InFlight, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun AccountLinkRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(icon, contentDescription = null, tint = LsPrimary)
            Spacer(Modifier.width(12.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium)
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun NotificationRow(notification: NotificationDto, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        BadgedBox(badge = { if (!notification.read) Badge(containerColor = LsAccent) } ) {
            Icon(Icons.Filled.Notifications, contentDescription = null, tint = if (notification.read) MaterialTheme.colorScheme.onSurfaceVariant else LsPrimary)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(notification.title, style = MaterialTheme.typography.bodyMedium, fontWeight = if (notification.read) FontWeight.Normal else FontWeight.SemiBold)
            Text(notification.body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
