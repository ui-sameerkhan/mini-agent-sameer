package com.lazyshopper.app.feature.customer.catalog

import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.lazyshopper.app.core.data.remote.ApiResult
import com.lazyshopper.app.core.data.remote.dto.Product
import com.lazyshopper.app.core.data.remote.dto.Review
import com.lazyshopper.app.core.data.remote.dto.ReviewsResponse
import com.lazyshopper.app.core.theme.LsAccent
import com.lazyshopper.app.core.ui.ActionState
import com.lazyshopper.app.core.ui.UiState
import com.lazyshopper.app.core.ui.components.LsPrimaryButton
import com.lazyshopper.app.core.ui.components.LsTextField
import com.lazyshopper.app.feature.customer.cart.CartRepository
import com.lazyshopper.app.feature.customer.util.fileUrl
import com.lazyshopper.app.feature.customer.util.formatMoney
import com.lazyshopper.app.feature.customer.util.formatQty
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    private val repository: CatalogRepository,
    val cartRepository: CartRepository,
) : ViewModel() {

    private val _reviews = MutableStateFlow<UiState<ReviewsResponse>>(UiState.Loading)
    val reviews: StateFlow<UiState<ReviewsResponse>> = _reviews.asStateFlow()

    private val _eligible = MutableStateFlow(false)
    val eligible: StateFlow<Boolean> = _eligible.asStateFlow()

    private val _videos = MutableStateFlow<List<Review>>(emptyList())
    val videos: StateFlow<List<Review>> = _videos.asStateFlow()

    private val _reviewActionState = MutableStateFlow<ActionState>(ActionState.Idle)
    val reviewActionState: StateFlow<ActionState> = _reviewActionState.asStateFlow()

    fun load(productId: String) {
        viewModelScope.launch {
            _reviews.value = UiState.Loading
            when (val r = repository.reviews("product", productId)) {
                is ApiResult.Success -> _reviews.value = UiState.Success(r.data)
                is ApiResult.Failure -> _reviews.value = UiState.Error(r.message)
            }
        }
        viewModelScope.launch {
            when (val r = repository.reviewsEligible()) {
                is ApiResult.Success -> _eligible.value = r.data.product_ids.contains(productId)
                is ApiResult.Failure -> _eligible.value = false
            }
        }
        viewModelScope.launch {
            when (val r = repository.videos("product", productId)) {
                is ApiResult.Success -> _videos.value = r.data
                is ApiResult.Failure -> {}
            }
        }
    }

    fun submitReview(productId: String, rating: Int, comment: String) {
        viewModelScope.launch {
            _reviewActionState.value = ActionState.InFlight
            val result = repository.submitReview("product", productId, rating, comment)
            // Note: when this is an *update* to an existing review, the server returns
            // {"message":"updated","id":...} instead of a full Review object — a shape mismatch
            // in the shared ProductsApi.createReview() return type (outside this workstream's
            // scope to fix). Either way we refresh the list so the UI reflects the latest state.
            _reviewActionState.value = when (result) {
                is ApiResult.Success -> ActionState.Done
                is ApiResult.Failure -> ActionState.Failed(result.message)
            }
            load(productId)
        }
    }

    fun resetReviewAction() {
        _reviewActionState.value = ActionState.Idle
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailSheet(
    product: Product,
    onDismiss: () -> Unit,
    viewModel: ProductDetailViewModel = hiltViewModel(),
) {
    val reviews by viewModel.reviews.collectAsState()
    val eligible by viewModel.eligible.collectAsState()
    val reviewActionState by viewModel.reviewActionState.collectAsState()
    var qty by remember { mutableStateOf(1.0) }
    var showReviewForm by remember { mutableStateOf(false) }
    var rating by remember { mutableStateOf(5) }
    var comment by remember { mutableStateOf("") }

    LaunchedEffect(product.id) { viewModel.load(product.id) }
    LaunchedEffect(reviewActionState) {
        if (reviewActionState is ActionState.Done) {
            showReviewForm = false
            comment = ""
            viewModel.resetReviewAction()
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
        ) {
            AsyncImage(
                model = fileUrl(product.image_id),
                contentDescription = product.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(18.dp)),
                contentScale = ContentScale.Crop,
            )
            Spacer(Modifier.height(14.dp))
            Text(product.name, style = MaterialTheme.typography.headlineSmall)
            product.shop_name?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val displayPrice = product.offer?.final_price ?: product.price
                Text(formatMoney(displayPrice), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                if (product.mrp != null && product.mrp > displayPrice) {
                    Spacer(Modifier.size(8.dp))
                    Text(
                        formatMoney(product.mrp),
                        style = MaterialTheme.typography.bodyMedium,
                        textDecoration = TextDecoration.LineThrough,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.size(6.dp))
                Text("/ ${product.unit_type}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (!product.description.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(product.description, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(16.dp))
            val outOfStock = !product.in_stock || product.stock <= 0.0
            if (outOfStock) {
                Text("Out of stock", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelLarge)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { if (qty > 1.0) qty -= 1.0 }) { Icon(Icons.Default.Remove, contentDescription = "Decrease") }
                    Text(formatQty(qty), style = MaterialTheme.typography.titleMedium, modifier = Modifier.size(width = 32.dp, height = 24.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    IconButton(onClick = { qty += 1.0 }) { Icon(Icons.Default.Add, contentDescription = "Increase") }
                }
                Spacer(Modifier.height(8.dp))
                LsPrimaryButton(
                    text = "Add to cart — ${formatMoney((product.offer?.final_price ?: product.price) * qty)}",
                    onClick = {
                        viewModel.cartRepository.addToCart(product, qty)
                        onDismiss()
                    },
                )
            }

            if (!product.promo_video_id.isNullOrBlank()) {
                Spacer(Modifier.height(20.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PlayCircle, contentDescription = null, tint = LsAccent)
                    Spacer(Modifier.size(6.dp))
                    Text("Product video", style = MaterialTheme.typography.titleSmall)
                }
                Spacer(Modifier.height(8.dp))
                val videoUrl = fileUrl(product.promo_video_id)
                if (videoUrl != null) {
                    AndroidView(
                        modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(14.dp)),
                        factory = { ctx ->
                            VideoView(ctx).apply {
                                setVideoURI(Uri.parse(videoUrl))
                                setMediaController(MediaController(ctx).also { it.setAnchorView(this) })
                                setOnPreparedListener { it.isLooping = false }
                            }
                        },
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))
            Text("Reviews", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            when (val r = reviews) {
                is UiState.Success -> {
                    val data = r.data
                    if (data.reviews.isEmpty()) {
                        Text("No reviews yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Text("★ %.1f from ${data.count} review${if (data.count == 1) "" else "s"}".format(data.average), style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(6.dp))
                        data.reviews.take(10).forEach { review -> ReviewRow(review) }
                    }
                }
                is UiState.Error -> Text(r.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                else -> {}
            }

            Spacer(Modifier.height(12.dp))
            if (eligible && !showReviewForm) {
                TextButton(onClick = { showReviewForm = true }) { Text("Write a review") }
            }
            if (showReviewForm) {
                Column {
                    Row {
                        (1..5).forEach { i ->
                            IconButton(onClick = { rating = i }, modifier = Modifier.size(32.dp)) {
                                Icon(
                                    if (i <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = "Rate $i",
                                    tint = LsAccent,
                                )
                            }
                        }
                    }
                    LsTextField(value = comment, onValueChange = { comment = it }, label = "Share your experience (optional)", singleLine = false)
                    if (reviewActionState is ActionState.Failed) {
                        Text((reviewActionState as ActionState.Failed).message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.height(8.dp))
                    LsPrimaryButton(
                        text = "Submit review",
                        onClick = { viewModel.submitReview(product.id, rating, comment) },
                        loading = reviewActionState is ActionState.InFlight,
                    )
                }
            } else if (!eligible) {
                Text(
                    "You can review this product after your order is delivered.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
