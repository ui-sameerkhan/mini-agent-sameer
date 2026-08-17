package com.ktc.sitepulse.ui.components

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.ktc.sitepulse.ui.SitePulseViewModel
import com.ktc.sitepulse.ui.theme.SpRed

/**
 * SitePulseViewModel.enableNotifications() only fetches an FCM token and saves it — that
 * succeeds regardless of the OS notification permission, so a device that never granted (or
 * later revoked, or has notifications switched off for the app entirely on older Android
 * versions) that permission still shows a "✅ enabled" status while every push silently never
 * reaches the notification bar. This card checks the real, version-safe signal —
 * NotificationManagerCompat.areNotificationsEnabled(), which also covers the pre-Android-13
 * "disabled in Settings" case that checking POST_NOTIFICATIONS alone can't see — and routes to
 * the right fix (the runtime permission prompt on 13+, or the app's notification Settings page
 * everywhere else) instead of quietly doing nothing.
 */
@Composable
fun NotificationsCard(viewModel: SitePulseViewModel, description: String, modifier: Modifier = Modifier) {
    val statusMessages by viewModel.statusMessages.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var notificationsEnabled by remember { mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled()) }

    // Re-checks whenever the user comes back from the Settings page they were sent to below —
    // there's no direct callback for "notification access changed", so ON_RESUME is the signal.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        if (granted) viewModel.enableNotifications()
    }

    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text("NOTIFICATIONS", fontWeight = FontWeight.Bold)
            Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 6.dp))
            if (!notificationsEnabled) {
                Text(
                    "⚠️ Notifications are switched off for this app at the system level — nothing will show in the notification bar until this is fixed.",
                    color = SpRed, modifier = Modifier.padding(bottom = 6.dp),
                )
            }
            Button(
                onClick = {
                    val needsRuntimePrompt = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                    when {
                        needsRuntimePrompt -> permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        !notificationsEnabled -> context.startActivity(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        )
                        else -> viewModel.enableNotifications()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (!notificationsEnabled) "⚙️ Open Notification Settings" else "🔔 Enable Notifications") }
            statusMessages["pushStatus"]?.let { Text(it, modifier = Modifier.padding(top = 6.dp)) }
        }
    }
}
