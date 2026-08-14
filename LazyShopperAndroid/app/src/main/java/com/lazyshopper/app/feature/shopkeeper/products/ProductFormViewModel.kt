package com.lazyshopper.app.feature.shopkeeper.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lazyshopper.app.core.data.remote.ApiResult
import com.lazyshopper.app.core.data.remote.dto.ImageIdResponse
import com.lazyshopper.app.core.data.remote.dto.ProductInput
import com.lazyshopper.app.core.data.remote.dto.Shop
import com.lazyshopper.app.core.ui.ActionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import javax.inject.Inject

data class ProductFormUiState(
    val loading: Boolean = true,
    val editingId: String? = null,
    val shops: List<Shop> = emptyList(),
    val name: String = "",
    val shopId: String? = null,
    val category: String = PRODUCT_CATEGORIES.first(),
    val subcategory: String? = null,
    val unitType: String = "kg",
    val price: String = "",
    val mrp: String = "",
    val stock: String = "100",
    val lowStockThreshold: String = "",
    val brand: String = "",
    val description: String = "",
    val featured: Boolean = false,
    val inStock: Boolean = true,
    val commissionPct: String = "",
    val imageId: String? = null,
    val promoVideoId: String? = null,
    val uploadingImage: Boolean = false,
    val uploadingVideo: Boolean = false,
    val submitState: ActionState = ActionState.Idle,
    val errorMessage: String? = null,
) {
    val isNonveg: Boolean get() = category == "nonveg"
    val canSubmit: Boolean
        get() = name.isNotBlank() && shopId != null && unitType.isNotBlank() && price.toDoubleOrNull() != null &&
            (!isNonveg || !subcategory.isNullOrBlank())
}

@HiltViewModel
class ProductFormViewModel @Inject constructor(
    private val repository: ProductsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ProductFormUiState())
    val state: StateFlow<ProductFormUiState> = _state.asStateFlow()

    fun init(productId: String?) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, editingId = productId) }
            val shopsResult = repository.myShops()
            val shops = (shopsResult as? ApiResult.Success)?.data.orEmpty()

            if (productId == null) {
                _state.update {
                    it.copy(
                        loading = false,
                        shops = shops,
                        shopId = it.shopId ?: shops.firstOrNull()?.id,
                    )
                }
                return@launch
            }

            when (val productsResult = repository.myProducts()) {
                is ApiResult.Success -> {
                    val product = productsResult.data.firstOrNull { it.id == productId }
                    if (product == null) {
                        _state.update { it.copy(loading = false, shops = shops, errorMessage = "Product not found") }
                    } else {
                        _state.update {
                            it.copy(
                                loading = false,
                                shops = shops,
                                name = product.name,
                                shopId = product.shop_id,
                                category = product.category,
                                subcategory = product.subcategory,
                                unitType = product.unit_type,
                                price = product.price.toString(),
                                mrp = product.mrp?.toString() ?: "",
                                stock = product.stock.toString(),
                                lowStockThreshold = product.low_stock_threshold?.toString() ?: "",
                                brand = product.brand ?: "",
                                description = product.description ?: "",
                                featured = product.featured,
                                inStock = product.in_stock,
                                commissionPct = product.commission_pct?.toString() ?: "",
                                imageId = product.image_id,
                                promoVideoId = product.promo_video_id,
                            )
                        }
                    }
                }
                is ApiResult.Failure -> _state.update { it.copy(loading = false, shops = shops, errorMessage = productsResult.message) }
            }
        }
    }

    fun setName(v: String) = _state.update { it.copy(name = v) }
    fun setShopId(v: String) = _state.update { it.copy(shopId = v) }
    fun setCategory(v: String) = _state.update {
        it.copy(category = v, subcategory = if (v == "nonveg") it.subcategory else null)
    }
    fun setSubcategory(v: String) = _state.update { it.copy(subcategory = v) }
    fun setUnitType(v: String) = _state.update { it.copy(unitType = v) }
    fun setPrice(v: String) = _state.update { it.copy(price = v) }
    fun setMrp(v: String) = _state.update { it.copy(mrp = v) }
    fun setStock(v: String) = _state.update { it.copy(stock = v) }
    fun setLowStockThreshold(v: String) = _state.update { it.copy(lowStockThreshold = v) }
    fun setBrand(v: String) = _state.update { it.copy(brand = v) }
    fun setDescription(v: String) = _state.update { it.copy(description = v) }
    fun setFeatured(v: Boolean) = _state.update { it.copy(featured = v) }
    fun setInStock(v: Boolean) = _state.update { it.copy(inStock = v) }
    fun setCommissionPct(v: String) = _state.update { it.copy(commissionPct = v) }

    fun uploadImage(part: MultipartBody.Part) {
        viewModelScope.launch {
            _state.update { it.copy(uploadingImage = true, errorMessage = null) }
            when (val result = repository.uploadImage(part)) {
                is ApiResult.Success -> _state.update { it.copy(uploadingImage = false, imageId = result.data.image_id) }
                is ApiResult.Failure -> _state.update { it.copy(uploadingImage = false, errorMessage = result.message) }
            }
        }
    }

    fun uploadVideo(part: MultipartBody.Part) {
        viewModelScope.launch {
            _state.update { it.copy(uploadingVideo = true, errorMessage = null) }
            when (val result = repository.uploadVideo(part)) {
                is ApiResult.Success -> _state.update { it.copy(uploadingVideo = false, promoVideoId = result.data.video_id) }
                is ApiResult.Failure -> _state.update { it.copy(uploadingVideo = false, errorMessage = result.message) }
            }
        }
    }

    fun clearVideo() = _state.update { it.copy(promoVideoId = null) }

    fun submit() {
        val s = _state.value
        if (!s.canSubmit) {
            val msg = if (s.isNonveg && s.subcategory.isNullOrBlank()) {
                "Select fish / chicken / mutton for non-veg"
            } else {
                "Name, shop and a valid price are required"
            }
            _state.update { it.copy(submitState = ActionState.Failed(msg)) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(submitState = ActionState.InFlight) }
            val input = ProductInput(
                name = s.name.trim(),
                shop_id = s.shopId!!,
                category = s.category,
                unit_type = s.unitType.trim(),
                price = s.price.toDouble(),
                mrp = s.mrp.toDoubleOrNull(),
                stock = s.stock.toDoubleOrNull() ?: 100.0,
                subcategory = if (s.isNonveg) s.subcategory else null,
                image_id = s.imageId,
                promo_video_id = s.promoVideoId,
                description = s.description.ifBlank { "" },
                brand = s.brand.ifBlank { null },
                featured = s.featured,
                in_stock = s.inStock,
                low_stock_threshold = s.lowStockThreshold.toDoubleOrNull(),
                commission_pct = s.commissionPct.toDoubleOrNull(),
            )
            val result = if (s.editingId != null) {
                repository.updateProduct(s.editingId, input)
            } else {
                repository.createProduct(input)
            }
            when (result) {
                is ApiResult.Success -> _state.update { it.copy(submitState = ActionState.Done) }
                is ApiResult.Failure -> _state.update { it.copy(submitState = ActionState.Failed(result.message)) }
            }
        }
    }
}
