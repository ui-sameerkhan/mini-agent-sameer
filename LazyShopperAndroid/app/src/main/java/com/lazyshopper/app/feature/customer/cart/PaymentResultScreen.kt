package com.lazyshopper.app.feature.customer.cart

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lazyshopper.app.core.theme.LsAccent
import com.lazyshopper.app.core.theme.LsPrimary
import com.lazyshopper.app.core.ui.components.LsPrimaryButton
import com.lazyshopper.app.core.ui.components.ScreenPadding

private data class ResultPresentation(val icon: ImageVector, val tint: androidx.compose.ui.graphics.Color, val title: String, val message: String)

@Composable
fun PaymentResultScreen(
    orderId: String,
    status: String,
    onViewOrder: () -> Unit,
    onContinueShopping: () -> Unit,
) {
    val errorColor = MaterialTheme.colorScheme.error
    val presentation = when (status) {
        "success" -> ResultPresentation(Icons.Default.CheckCircle, LsPrimary, "Payment successful!", "Your order has been placed and paid for. We'll notify you as it's prepared.")
        "cod" -> ResultPresentation(Icons.Default.CheckCircle, LsPrimary, "Order placed!", "Pay cash on delivery when your order arrives.")
        "failed" -> ResultPresentation(Icons.Default.Error, errorColor, "Payment failed", "Your order was created but payment couldn't be verified. Please check your Orders page or contact support.")
        else -> ResultPresentation(Icons.Default.HourglassTop, LsAccent, "Processing…", "We're confirming your order.")
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(ScreenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(presentation.icon, contentDescription = null, tint = presentation.tint, modifier = Modifier.size(72.dp))
        Spacer(Modifier.height(20.dp))
        Text(presentation.title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(presentation.message, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Text("Order #${orderId.takeLast(8)}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(32.dp))
        LsPrimaryButton(text = "View order", onClick = onViewOrder)
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onContinueShopping) { Text("Continue shopping") }
    }
}
