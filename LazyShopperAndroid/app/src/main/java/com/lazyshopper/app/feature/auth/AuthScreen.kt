package com.lazyshopper.app.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lazyshopper.app.core.data.local.Role
import com.lazyshopper.app.core.ui.ActionState
import com.lazyshopper.app.core.ui.components.LsPrimaryButton
import com.lazyshopper.app.core.ui.components.LsTextField
import com.lazyshopper.app.core.ui.components.ScreenPadding

@Composable
fun AuthScreen(
    onLoggedIn: (role: String) -> Unit,
    onForgotPassword: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.loggedInRole) {
        state.loggedInRole?.let(onLoggedIn)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(ScreenPadding),
    ) {
        Spacer(Modifier.height(32.dp))
        Text("Lazy Shopper", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
        Text(
            "Fresh groceries, delivered fast",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))

        val tabIndex = if (state.mode == AuthMode.LOGIN) 0 else 1
        TabRow(selectedTabIndex = tabIndex) {
            Tab(selected = tabIndex == 0, onClick = { viewModel.setMode(AuthMode.LOGIN) }, text = { Text("Login") })
            Tab(selected = tabIndex == 1, onClick = { viewModel.setMode(AuthMode.REGISTER) }, text = { Text("Register") })
        }
        Spacer(Modifier.height(20.dp))

        if (state.mode == AuthMode.REGISTER) {
            Text("I am a", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RoleChip("Customer", Role.CUSTOMER, state.role, viewModel::setRole)
                RoleChip("Shopkeeper", Role.SHOPKEEPER, state.role, viewModel::setRole)
                RoleChip("Delivery Partner", Role.DELIVERY, state.role, viewModel::setRole)
            }
            Spacer(Modifier.height(16.dp))
            LsTextField(value = state.name, onValueChange = viewModel::setName, label = "Full name")
            Spacer(Modifier.height(12.dp))
            if (state.role == Role.SHOPKEEPER) {
                LsTextField(value = state.shopName, onValueChange = viewModel::setShopName, label = "Shop name")
                Spacer(Modifier.height(12.dp))
            }
            LsTextField(
                value = state.phone,
                onValueChange = viewModel::setPhone,
                label = "Mobile number (optional)",
                keyboardType = KeyboardType.Phone,
            )
            Spacer(Modifier.height(12.dp))
        }

        LsTextField(
            value = state.email,
            onValueChange = viewModel::setEmail,
            label = "Email",
            keyboardType = KeyboardType.Email,
        )
        Spacer(Modifier.height(12.dp))
        LsTextField(value = state.password, onValueChange = viewModel::setPassword, label = "Password", isPassword = true)

        if (state.mode == AuthMode.LOGIN) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onForgotPassword) { Text("Forgot password?") }
            }
        } else {
            Spacer(Modifier.height(8.dp))
        }

        if (state.actionState is ActionState.Failed) {
            Text(
                (state.actionState as ActionState.Failed).message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        Spacer(Modifier.height(20.dp))
        LsPrimaryButton(
            text = if (state.mode == AuthMode.LOGIN) "Log in" else "Create account",
            onClick = viewModel::submit,
            loading = state.actionState is ActionState.InFlight,
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun RoleChip(label: String, value: String, current: String, onSelect: (String) -> Unit) {
    FilterChip(selected = current == value, onClick = { onSelect(value) }, label = { Text(label) })
}
