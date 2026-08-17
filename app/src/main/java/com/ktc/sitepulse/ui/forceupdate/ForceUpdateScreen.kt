package com.ktc.sitepulse.ui.forceupdate

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ktc.sitepulse.data.model.AppVersionGate
import com.ktc.sitepulse.ui.theme.SpBrandBlueMid

/**
 * Full-screen, no-dismiss block shown instead of the app whenever the installed build's
 * versionCode is below the admin-set minimum — see SitePulseViewModel.versionGate. There is
 * deliberately no way past this other than updating (or signing out to switch accounts).
 */
@Composable
fun ForceUpdateScreen(gate: AppVersionGate, onSignOut: () -> Unit) {
    val context = LocalContext.current
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("⬆️ Update Required", fontWeight = FontWeight.Bold, fontSize = 22.sp, textAlign = TextAlign.Center)
        Text(
            gate.message.ifBlank { "A new version of SitePulse is required to continue. Please install the latest update." },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp, bottom = 24.dp),
        )
        if (gate.updateUrl.isNotBlank()) {
            Button(
                onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(gate.updateUrl))) },
                colors = ButtonDefaults.buttonColors(containerColor = SpBrandBlueMid),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Download Update") }
        }
        TextButton(onClick = onSignOut, modifier = Modifier.padding(top = 16.dp)) { Text("Sign Out") }
    }
}
