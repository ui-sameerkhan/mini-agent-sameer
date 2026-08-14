package com.lazyshopper.app.feature.customer.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.lazyshopper.app.core.data.remote.dto.Product
import com.lazyshopper.app.core.theme.LsAccent
import com.lazyshopper.app.core.theme.LsMuted
import com.lazyshopper.app.feature.customer.util.fileUrl
import com.lazyshopper.app.feature.customer.util.formatMoney

/** Reusable storefront product tile: image, offer badge, price/MRP, rating, quick add-to-cart. */
@Composable
fun ProductCard(
    product: Product,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onAddToCart: (() -> Unit)? = null,
) {
    Card(shape = RoundedCornerShape(18.dp), onClick = onClick, modifier = modifier) {
        Column {
            Box {
                AsyncImage(
                    model = fileUrl(product.image_id),
                    contentDescription = product.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .background(LsMuted),
                    contentScale = ContentScale.Crop,
                )
                val offerPct = product.offer?.pct
                if (offerPct != null && offerPct > 0) {
                    Box(
                        modifier = Modifier
                            .padding(6.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(LsAccent)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text("$offerPct% OFF", color = Color.White, style = MaterialTheme.typography.labelSmall)
                    }
                }
                if (onAddToCart != null) {
                    FilledIconButton(
                        onClick = onAddToCart,
                        modifier = Modifier
                            .padding(6.dp)
                            .size(30.dp)
                            .align(Alignment.BottomEnd),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add to cart", modifier = Modifier.size(16.dp))
                    }
                }
            }
            Column(modifier = Modifier.padding(10.dp)) {
                Text(product.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                Text(
                    product.shop_name.orEmpty(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
                androidx.compose.foundation.layout.Spacer(Modifier.size(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val displayPrice = product.offer?.final_price ?: product.price
                    Text(formatMoney(displayPrice), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    if (product.mrp != null && product.mrp > displayPrice) {
                        androidx.compose.foundation.layout.Spacer(Modifier.size(4.dp))
                        Text(
                            formatMoney(product.mrp),
                            style = MaterialTheme.typography.labelSmall,
                            textDecoration = TextDecoration.LineThrough,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Text(
                    "/ ${product.unit_type}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (product.avg_rating != null && product.avg_rating > 0.0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = LsAccent, modifier = Modifier.size(12.dp))
                        Text(" %.1f".format(product.avg_rating), style = MaterialTheme.typography.labelSmall)
                        product.review_count?.let { Text(" ($it)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
                if (product.in_stock.not() || product.stock <= 0.0) {
                    Text("Out of stock", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
