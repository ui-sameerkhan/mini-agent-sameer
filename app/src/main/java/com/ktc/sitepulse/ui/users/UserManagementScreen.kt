package com.ktc.sitepulse.ui.users

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ktc.sitepulse.data.model.ALL_SITES
import com.ktc.sitepulse.data.model.Role
import com.ktc.sitepulse.data.model.Site
import com.ktc.sitepulse.data.model.UserProfile
import com.ktc.sitepulse.ui.SitePulseViewModel
import com.ktc.sitepulse.ui.theme.SpAmberMid
import com.ktc.sitepulse.ui.theme.SpBlue
import com.ktc.sitepulse.ui.theme.SpGreenMid
import com.ktc.sitepulse.ui.theme.SpMuted
import com.ktc.sitepulse.ui.theme.SpRed

/**
 * Super Admin only — creating logins, setting roles, and assigning which sites each person may
 * reach. Reachable from Roster; the route is also gated in the nav host, and the underlying
 * writes are gated again by Firestore rules.
 */
@Composable
fun UserManagementScreen(viewModel: SitePulseViewModel, onBack: () -> Unit) {
    val users by viewModel.allUsers.collectAsState()
    val sites by viewModel.sites.collectAsState()
    val statusMessages by viewModel.statusMessages.collectAsState()

    var showCreate by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<UserProfile?>(null) }
    var query by remember { mutableStateOf("") }

    val filtered = remember(users, query) {
        if (query.isBlank()) users
        else users.filter {
            it.name.contains(query, true) || it.email.contains(query, true) ||
                it.role.contains(query, true) || it.assignedSites.any { s -> s.contains(query, true) }
        }
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp),
    ) {
        item {
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("← Back to Roster") }
        }
        item {
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("USER MANAGEMENT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "Create logins, set roles, and choose which sites each person can reach. " +
                            "Only you can change these.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
                    )
                    Button(
                        onClick = { showCreate = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = SpGreenMid),
                    ) { Text("+ Create User", fontWeight = FontWeight.Bold) }
                    statusMessages["userStatus"]?.let {
                        Text(it, modifier = Modifier.padding(top = 8.dp), fontSize = 12.sp)
                    }
                }
            }
        }
        item {
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                placeholder = { Text("🔍 Search name, email, role or site…") },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
            )
        }
        item {
            Text(
                "${filtered.size} user account(s)",
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp,
            )
        }

        if (filtered.isEmpty()) {
            item {
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("👥", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            if (users.isEmpty()) "No user profiles created yet." else "No users match your search.",
                            fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp),
                        )
                        if (users.isEmpty()) {
                            Text(
                                "Everyone signing in today still works on their existing access. " +
                                    "Create profiles here to assign roles and specific sites.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }
            }
        }

        items(filtered.size) { index ->
            UserRow(filtered[index], onEdit = { editing = filtered[index] })
        }
    }

    if (showCreate) {
        UserEditDialog(
            existing = null, sites = sites,
            onDismiss = { showCreate = false },
            onSave = { name, email, password, role, assigned, employeeId ->
                viewModel.createUser(email, password, name, role, assigned, employeeId)
                showCreate = false
            },
            onStatusChange = null,
        )
    }
    editing?.let { user ->
        UserEditDialog(
            existing = user, sites = sites,
            onDismiss = { editing = null },
            onSave = { _, _, _, role, assigned, _ ->
                if (role != user.roleEnum) viewModel.updateUserRole(user.uid, role)
                if (assigned != user.assignedSites) viewModel.updateUserSites(user.uid, assigned)
                editing = null
            },
            onStatusChange = { active ->
                viewModel.setUserStatus(user.uid, active)
                editing = null
            },
        )
    }
}

@Composable
private fun UserRow(user: UserProfile, onEdit: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onEdit),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(user.displayName, fontWeight = FontWeight.Bold)
                    Text(user.email, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    "${user.roleEnum.badge} ${user.roleEnum.label}",
                    fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SpBlue,
                )
            }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    when {
                        user.hasAllSites -> "🌐 All sites"
                        user.assignedSites.isEmpty() -> "⚠ No site assigned"
                        else -> "📍 ${user.assignedSites.joinToString(", ")}"
                    },
                    fontSize = 11.sp,
                    color = if (user.assignedSites.isEmpty() && !user.hasAllSites) SpAmberMid else SpMuted,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    if (user.isActive) "● Active" else "● Disabled",
                    fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    color = if (user.isActive) SpGreenMid else SpRed,
                )
            }
            user.employeeId?.takeIf { it.isNotBlank() }?.let {
                Text("Employee ID $it", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}

/**
 * The site list needs its own bounded, scrollable panel rather than flowing into the dialog:
 * a company with twenty projects would otherwise push the Create button off-screen, and the
 * last row would be clipped with nothing indicating there was more below.
 *
 * The panel has an explicit height so its inner scroll is measured against a real bound — a
 * scrollable inside a scrollable is only a problem when the inner one is unbounded.
 */
@Composable
private fun SitePicker(
    sites: List<Site>,
    selected: List<String>,
    singleSiteOnly: Boolean,
    onToggle: (String) -> Unit,
    onSelectAll: () -> Unit,
    onClear: () -> Unit,
) {
    var search by remember { mutableStateOf("") }
    val shown = remember(sites, search) {
        if (search.isBlank()) sites
        else sites.filter { it.name.contains(search, true) || it.code.contains(search, true) }
    }

    Row(
        Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            if (singleSiteOnly) "ASSIGNED OFFICE / SITE" else "ASSIGNED SITES",
            fontSize = 11.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            "${selected.size} selected",
            fontSize = 11.sp, fontWeight = FontWeight.Bold,
            color = if (selected.isEmpty()) SpAmberMid else SpGreenMid,
        )
    }

    if (sites.isEmpty()) {
        Text(
            "No sites exist yet — add one in the Sites tab first.",
            fontSize = 12.sp, color = SpAmberMid, modifier = Modifier.padding(bottom = 8.dp),
        )
        return
    }

    // Bulk actions only earn their space once the list is long enough to make ticking tedious.
    if (!singleSiteOnly && sites.size > 3) {
        Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onSelectAll, modifier = Modifier.weight(1f)) {
                Text("Select all", fontSize = 12.sp)
            }
            OutlinedButton(onClick = onClear, modifier = Modifier.weight(1f)) {
                Text("Clear", fontSize = 12.sp)
            }
        }
    }

    if (sites.size > 6) {
        OutlinedTextField(
            value = search, onValueChange = { search = it },
            placeholder = { Text("Search sites…", fontSize = 13.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
        )
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxWidth().height(if (sites.size > 4) 190.dp else 150.dp),
    ) {
        if (shown.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                Text("No sites match \"$search\".", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(vertical = 4.dp)) {
                items(shown.size) { i ->
                    val site = shown[i]
                    val checked = selected.contains(site.code)
                    Row(
                        Modifier.fillMaxWidth()
                            .clickable { onToggle(site.code) }
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = checked, onCheckedChange = { onToggle(site.code) })
                        Column(Modifier.padding(start = 4.dp)) {
                            Text(site.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            Text(site.code, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
    Text(
        if (singleSiteOnly) "Staff check in at one location — picking another replaces the current one."
        else "Scroll for more. This person will only ever see the sites ticked here.",
        fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 6.dp),
    )
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun UserEditDialog(
    existing: UserProfile?,
    sites: List<Site>,
    onDismiss: () -> Unit,
    onSave: (name: String, email: String, password: String, role: Role, assignedSites: List<String>, employeeId: String?) -> Unit,
    onStatusChange: ((Boolean) -> Unit)?,
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var email by remember { mutableStateOf(existing?.email ?: "") }
    var password by remember { mutableStateOf("") }
    var employeeId by remember { mutableStateOf(existing?.employeeId ?: "") }
    var role by remember { mutableStateOf(existing?.roleEnum ?: Role.FOREMAN) }
    var roleExpanded by remember { mutableStateOf(false) }
    val selectedSites = remember {
        (existing?.assignedSites?.filter { it != ALL_SITES } ?: emptyList()).toMutableStateList()
    }

    // Super Admin always holds every site, so the picker would be meaningless for that role.
    // Staff hold exactly one location — the office they check in at.
    val needsSites = role != Role.SUPER_ADMIN
    val singleSiteOnly = role == Role.STAFF

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Create User" else "Edit ${existing.displayName}") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()).heightIn(max = 460.dp)) {
                if (existing == null) {
                    OutlinedTextField(name, { name = it }, label = { Text("Full Name *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(
                        email, { email = it }, label = { Text("Login Email *") },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp), singleLine = true,
                    )
                    OutlinedTextField(
                        password, { password = it }, label = { Text("Password") },
                        supportingText = { Text("Leave blank if this person already has a login — this will just set their role and sites.") },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp), singleLine = true,
                    )
                } else {
                    Text(existing.email, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Email and password can't be changed here — reset a password from the Firebase Console.",
                        fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
                    )
                }

                ExposedDropdownMenuBox(
                    expanded = roleExpanded, onExpandedChange = { roleExpanded = it },
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    OutlinedTextField(
                        value = "${role.badge} ${role.label}", onValueChange = {}, readOnly = true,
                        label = { Text("Role") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                    )
                    ExposedDropdownMenu(expanded = roleExpanded, onDismissRequest = { roleExpanded = false }) {
                        Role.entries.forEach { r ->
                            DropdownMenuItem(
                                text = { Text("${r.badge} ${r.label}") },
                                onClick = {
                                    role = r
                                    if (r == Role.STAFF && selectedSites.size > 1) {
                                        val first = selectedSites.first()
                                        selectedSites.clear()
                                        selectedSites.add(first)
                                    }
                                    roleExpanded = false
                                },
                            )
                        }
                    }
                }

                if (role == Role.STAFF) {
                    OutlinedTextField(
                        employeeId, { employeeId = it },
                        label = { Text("Employee ID") },
                        supportingText = { Text("Links this login to their worker record so self check-in knows who they are.") },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp), singleLine = true,
                    )
                }

                if (needsSites) {
                    SitePicker(
                        sites = sites,
                        selected = selectedSites,
                        singleSiteOnly = singleSiteOnly,
                        onToggle = { code ->
                            if (selectedSites.contains(code)) {
                                selectedSites.remove(code)
                            } else {
                                if (singleSiteOnly) selectedSites.clear()
                                selectedSites.add(code)
                            }
                        },
                        onSelectAll = {
                            selectedSites.clear()
                            selectedSites.addAll(sites.map { it.code })
                        },
                        onClear = { selectedSites.clear() },
                    )
                } else {
                    Text(
                        "🌐 Super Admin has access to every site automatically.",
                        fontSize = 12.sp, color = SpBlue, modifier = Modifier.padding(top = 14.dp),
                    )
                }

                onStatusChange?.let { change ->
                    val active = existing?.isActive ?: true
                    OutlinedButton(
                        onClick = { change(!active) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = if (active) SpRed else SpGreenMid),
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    ) { Text(if (active) "Disable this account" else "Re-activate this account") }
                    Text(
                        "Disabling stops them signing in. Nothing is deleted — their login and every " +
                            "attendance record they marked stay exactly as they are.",
                        fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(name, email, password, role, selectedSites.toList(), employeeId.ifBlank { null })
            }) { Text(if (existing == null) "Create" else "Save Changes") }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
