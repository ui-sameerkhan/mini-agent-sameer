package com.lazyshopper.app.feature.admin.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lazyshopper.app.BuildConfig
import com.lazyshopper.app.core.theme.LsRoleAdmin

/** Formats a nullable Double as ₹ currency, defensively 2dp per api_reference money-field note. */
fun Double?.money(): String = "₹%.2f".format(this ?: 0.0)

/** Resolves a backend file id to a fetchable image URL for AsyncImage. */
fun fileUrl(fid: String?): String? = fid?.takeIf { it.isNotBlank() }?.let { BuildConfig.API_BASE_URL.trimEnd('/') + "/api/files/" + it }

enum class Tone { SUCCESS, WARNING, ERROR, NEUTRAL, INFO }

private fun Tone.color(): Color = when (this) {
    Tone.SUCCESS -> Color(0xFF2D5A27)
    Tone.WARNING -> Color(0xFFB8860B)
    Tone.ERROR -> Color(0xFFB3261E)
    Tone.INFO -> Color(0xFF1E6091)
    Tone.NEUTRAL -> Color(0xFF6B6B6B)
}

/** Maps common status strings to a Tone for consistent badge coloring across every list screen. */
fun statusTone(status: String?): Tone = when (status?.lowercase()) {
    "approved", "active", "paid", "delivered", "live", "credited", "settled" -> Tone.SUCCESS
    "pending", "submitted", "scheduled", "eligible", "processing" -> Tone.WARNING
    "rejected", "suspended", "cancelled", "failed", "disabled", "expired" -> Tone.ERROR
    else -> Tone.NEUTRAL
}

@Composable
fun StatusChip(text: String, tone: Tone = statusTone(text), modifier: Modifier = Modifier) {
    val c = tone.color()
    Box(
        modifier = modifier
            .background(c.copy(alpha = 0.14f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = c, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun StatTile(title: String, value: String, modifier: Modifier = Modifier, accent: Color = LsRoleAdmin, onClick: (() -> Unit)? = null) {
    Card(
        modifier = modifier.let { if (onClick != null) it.clickable(onClick = onClick) else it },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge, color = accent, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun StatTileRow(tiles: List<Pair<String, String>>, modifier: Modifier = Modifier, onTileClick: ((Int) -> Unit)? = null) {
    LazyRow(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(horizontal = 2.dp)) {
        items(tiles.size) { i ->
            StatTile(tiles[i].first, tiles[i].second, modifier = Modifier.width(128.dp), onClick = onTileClick?.let { { it(i) } })
        }
    }
}

/** Compact dense row for high-density admin lists (per design_guidelines.json: sharper corners, Shadcn-table feel). */
@Composable
fun CompactRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    meta: String? = null,
    badge: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    Card(
        modifier = modifier.fillMaxWidth().let { if (onClick != null) it.clickable(onClick = onClick) else it },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        if (badge != null) {
                            Spacer(Modifier.width(6.dp))
                            badge()
                        }
                    }
                    if (subtitle != null) {
                        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                    }
                    if (meta != null) {
                        Text(meta, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (trailing != null) {
                    Spacer(Modifier.width(8.dp))
                    trailing()
                }
            }
            if (content != null) {
                Spacer(Modifier.height(6.dp))
                Column(content = content)
            }
        }
    }
}

@Composable
fun EmptyOrList(loading: Boolean, error: String?, isEmpty: Boolean, emptyMessage: String, onRetry: (() -> Unit)? = null, content: @Composable () -> Unit) {
    when {
        loading -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        error != null -> com.lazyshopper.app.core.ui.components.ErrorState(error, onRetry = onRetry)
        isEmpty -> com.lazyshopper.app.core.ui.components.EmptyState(emptyMessage)
        else -> content()
    }
}

@Composable
fun AdminSearchField(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier, placeholder: String = "Search…") {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        shape = RoundedCornerShape(8.dp),
    )
}

@Composable
fun FilterChipsRow(options: List<String>, selected: String?, onSelect: (String?) -> Unit, modifier: Modifier = Modifier, allLabel: String = "All") {
    val entries = listOf<String?>(null) + options
    LazyRow(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(entries.size) { i ->
            val opt = entries[i]
            FilterChip(selected = selected == opt, onClick = { onSelect(opt) }, label = { Text(opt ?: allLabel) })
        }
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmLabel: String = "Confirm",
    destructive: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
fun LabeledSwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier, subtitle: String? = null) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun KeyValueRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun SectionDivider(modifier: Modifier = Modifier) {
    Divider(modifier = modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.surfaceVariant)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreenScaffold(
    title: String,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    fab: @Composable (() -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(title, style = MaterialTheme.typography.titleLarge) },
                navigationIcon = { IconButton(onClick = onMenuClick) { Icon(Icons.Default.Menu, contentDescription = "Menu") } },
                actions = actions,
            )
        },
        floatingActionButton = { fab?.invoke() },
    ) { padding -> content(padding) }
}

@Composable
fun AdminLazyList(modifier: Modifier = Modifier, content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}
