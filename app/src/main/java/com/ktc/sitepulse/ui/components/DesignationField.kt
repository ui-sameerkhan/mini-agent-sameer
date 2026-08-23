package com.ktc.sitepulse.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/** Common trades/designations for a manpower-contracting workforce — kept as suggestions, not a
 * restriction, since real crews always include titles outside any fixed list. Standardizing the
 * common ones (rather than free-text drift like "Foreman"/"foreman"/"Site Forman") keeps the
 * Trade-Wise Summary report meaningful. */
val COMMON_DESIGNATIONS = listOf(
    "Foreman", "Chargehand", "Site Engineer", "Supervisor", "Safety Officer",
    "Mason", "Carpenter", "Electrician", "Plumber", "Steel Fixer", "Welder",
    "Painter", "Operator", "Driver", "Helper", "Laborer",
)

/** Editable combo box: type freely, or pick a common designation from the dropdown. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesignationField(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier, label: String = "Designation *") {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = value, onValueChange = onValueChange, label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            COMMON_DESIGNATIONS.forEach { d ->
                DropdownMenuItem(text = { Text(d) }, onClick = { onValueChange(d); expanded = false })
            }
        }
    }
}
