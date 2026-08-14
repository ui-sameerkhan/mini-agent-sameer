package com.lazyshopper.app.feature.shopkeeper.products

import com.lazyshopper.app.core.data.remote.ApiResult
import com.lazyshopper.app.core.data.remote.api.ProductsApi
import com.lazyshopper.app.core.data.remote.dto.ImageIdResponse
import com.lazyshopper.app.core.data.remote.dto.MessageResponse
import com.lazyshopper.app.core.data.remote.dto.Product
import com.lazyshopper.app.core.data.remote.dto.ProductInput
import com.lazyshopper.app.core.data.remote.dto.Shop
import com.lazyshopper.app.core.data.remote.dto.VideoIdResponse
import com.lazyshopper.app.core.data.remote.safeApiCall
import okhttp3.MultipartBody
import javax.inject.Inject
import javax.inject.Singleton

/** Product categories validated server-side (see api_reference.md §4). */
val PRODUCT_CATEGORIES = listOf("vegetables", "masala", "kirana", "nonveg", "foodlive", "sweets")

/** Subcategories required/shown only when category == "nonveg". */
val NONVEG_SUBCATEGORIES = listOf("fish", "chicken", "mutton")

@Singleton
class ProductsRepository @Inject constructor(
    private val api: ProductsApi,
) {
    suspend fun myProducts(): ApiResult<List<Product>> = safeApiCall { api.myProducts() }

    suspend fun myShops(): ApiResult<List<Shop>> = safeApiCall { api.myShops() }

    suspend fun createProduct(input: ProductInput): ApiResult<Product> = safeApiCall { api.createProduct(input) }

    suspend fun updateProduct(id: String, input: ProductInput): ApiResult<Product> = safeApiCall { api.updateProduct(id, input) }

    suspend fun deleteProduct(id: String): ApiResult<MessageResponse> = safeApiCall { api.deleteProduct(id) }

    suspend fun uploadImage(part: MultipartBody.Part): ApiResult<ImageIdResponse> = safeApiCall { api.uploadImage(part) }

    suspend fun uploadVideo(part: MultipartBody.Part): ApiResult<VideoIdResponse> = safeApiCall { api.uploadVideo(part) }
}
