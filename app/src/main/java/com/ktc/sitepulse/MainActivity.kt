package com.ktc.sitepulse

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import com.ktc.sitepulse.ui.nav.SitePulseRoot
import com.ktc.sitepulse.ui.theme.SitePulseTheme

class MainActivity : ComponentActivity() {

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { /* no-op: screens re-check as needed */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Notification permission is deliberately NOT requested here — it's asked for
        // contextually by NotificationsCard's "Enable Notifications" button instead, so the
        // OS prompt actually appears when the admin taps it rather than silently having
        // already been decided at launch.
        permissionLauncher.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ))

        setContent {
            SitePulseTheme {
                SitePulseRoot()
            }
        }
    }
}
