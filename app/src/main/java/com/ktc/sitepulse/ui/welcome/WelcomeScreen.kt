package com.ktc.sitepulse.ui.welcome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ktc.sitepulse.ui.theme.SpBrandBlue
import com.ktc.sitepulse.ui.theme.SpBrandBlueDark
import com.ktc.sitepulse.ui.theme.SpBrandBlueMid
import com.ktc.sitepulse.ui.theme.SpBrandGold

/**
 * First-run splash before Sign In — a returning, already-authenticated session skips straight
 * past this (see the isLoggedIn check in SitePulseRoot), so this is shown at most once per
 * sign-out, not on every app open.
 */
@Composable
fun WelcomeScreen(onGetStarted: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SpBrandBlueDark, SpBrandBlueMid, SpBrandBlue))),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .size(76.dp)
                    .shadow(10.dp, RoundedCornerShape(22.dp), spotColor = Color.Black.copy(alpha = 0.45f))
                    .background(Brush.linearGradient(listOf(SpBrandGold, Color(0xFFFFE07A))), RoundedCornerShape(22.dp)),
                contentAlignment = Alignment.Center,
            ) { Text("📡", fontSize = 34.sp) }

            Row(modifier = Modifier.padding(top = 20.dp)) {
                Text("Site", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 32.sp)
                Text("Pulse", color = SpBrandGold, fontWeight = FontWeight.Bold, fontSize = 32.sp)
            }
            Text(
                "GPS & WiFi Workforce Attendance",
                color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp),
            )
            Text(
                "for KTC International Contracting",
                color = SpBrandGold, fontWeight = FontWeight.Bold, fontSize = 12.5.sp, modifier = Modifier.padding(top = 3.dp),
            )

            Column(modifier = Modifier.padding(top = 40.dp)) {
                FeatureLine("📍", "Geofenced GPS check-in, with office WiFi as a fallback")
                FeatureLine("📊", "Live dashboard, reporting, and full data backup")
                FeatureLine("🔒", "Role-based access for admins, supervisors, and staff")
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = onGetStarted,
                colors = ButtonDefaults.buttonColors(containerColor = SpBrandGold, contentColor = SpBrandBlueDark),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            ) {
                Text("Get Started", fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.padding(vertical = 4.dp))
            }
            Text(
                "Developed by Sameer Khan",
                color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp, modifier = Modifier.padding(top = 14.dp),
            )
        }
    }
}

@Composable
private fun FeatureLine(emoji: String, text: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(emoji, fontSize = 16.sp, modifier = Modifier.padding(end = 12.dp))
        Text(text, color = Color.White.copy(alpha = 0.92f), fontSize = 13.sp)
    }
}
