package com.ktc.sitepulse.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ktc.sitepulse.ui.ImportKind
import com.ktc.sitepulse.ui.PendingDelete
import com.ktc.sitepulse.ui.PendingImport
import com.ktc.sitepulse.ui.PendingRestore
import com.ktc.sitepulse.ui.theme.SpRed

/** Mirrors the web app's typed-DELETE prompt() pattern for destructive actions. */
@Composable
fun TypedDeleteConfirmDialog(pending: PendingDelete, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var typed by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete ${pending.label}?") },
        text = {
            Column {
                Text("This cannot be undone. Attendance history is kept. Type DELETE to confirm.")
                OutlinedTextField(
                    value = typed, onValueChange = { typed = it },
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp), singleLine = true,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(typed) },
                enabled = typed.trim() == "DELETE",
                colors = ButtonDefaults.buttonColors(containerColor = SpRed),
            ) { Text("Delete") }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Mirrors the web app's confirm() before an Excel/roster upload overwrites or reassigns workers. */
@Composable
fun ImportConfirmDialog(pending: PendingImport, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val kindLabel = when (pending.kind) {
        ImportKind.WORKERS -> "worker list"
        ImportKind.OUTSOURCE -> "outsource manpower list"
        ImportKind.ROSTER -> "roster"
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirm $kindLabel upload") },
        text = {
            Column {
                if (pending.overwritingCount > 0) {
                    Text("${pending.overwritingCount} of ${pending.overwritingCount + pending.newCount} worker ID(s) already exist and will be OVERWRITTEN. ${pending.newCount} new worker(s) will be added.")
                }
                if (pending.reassignments.isNotEmpty()) {
                    Text("The following workers will be moved to a different project:", modifier = Modifier.padding(top = 8.dp))
                    pending.reassignments.take(3).forEach { (name, old, new) ->
                        Text("• $name ($old → $new)")
                    }
                }
                if (pending.skippedNotFound.isNotEmpty()) {
                    Text(
                        "Not found (will be skipped): ${pending.skippedNotFound.take(10).joinToString(", ")}${if (pending.skippedNotFound.size > 10) "…" else ""}",
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                Text("Continue?", modifier = Modifier.padding(top = 8.dp))
            }
        },
        confirmButton = { Button(onClick = onConfirm) { Text("Continue") } },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Confirmation before restoring a full-backup .xlsx — always additive, see PendingRestore/confirmBackupRestore. */
@Composable
fun RestoreConfirmDialog(pending: PendingRestore, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val p = pending.parsed
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Restore from backup?") },
        text = {
            Column {
                Text("This file contains:")
                listOf(
                    "Workers" to p.workers.size, "Sites" to p.sites.size, "Attendance records" to p.attendance.size,
                    "Leaves" to p.leaves.size, "Blocked attempts" to p.blocked.size, "Arrival requests" to p.arrivals.size,
                ).filter { it.second > 0 }.forEach { (label, count) ->
                    Text("• $count $label", modifier = Modifier.padding(top = 2.dp))
                }
                Text(
                    "These will be added or updated. Nothing currently in the app that isn't in this file will be deleted.",
                    modifier = Modifier.padding(top = 10.dp),
                )
                Text("Continue?", modifier = Modifier.padding(top = 8.dp))
            }
        },
        confirmButton = { Button(onClick = onConfirm) { Text("Restore") } },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
