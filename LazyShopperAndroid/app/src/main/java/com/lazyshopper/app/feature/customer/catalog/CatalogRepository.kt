package com.lazyshopper.app.feature.customer.catalog

import com.lazyshopper.app.core.data.remote.ApiResult
import com.lazyshopper.app.core.data.remote.api.ProductsApi
import com.lazyshopper.app.core.data.remote.dto.Category
import com.lazyshopper.app.core.data.remote.dto.Product
import com.lazyshopper.app.core.data.remote.dto.Review
import com.lazyshopper.app.core.data.remote.dto.ReviewInput
import com.lazyshopper.app.core.data.remote.dto.ReviewsEligibleResponse
import com.lazyshopper.app.core.data.remote.dto.ReviewsResponse
import com.lazyshopper.app.core.data.remote.dto.Shop
import com.lazyshopper.app.core.data.remote.dto.StateLocations
import com.lazyshopper.app.core.data.remote.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CatalogRepository @Inject constructor(
    private val productsApi: ProductsApi,
) {
    suspend fun categories(): ApiResult<List<Category>> = safeApiCall { productsApi.categories() }

    suspend fun locations(): ApiResult<List<StateLocations>> = safeApiCall { productsApi.locations() }

    suspend fun shops(category: String? = null, state: String? = null, district: String? = null, area: String? = null): ApiResult<List<Shop>> =
        safeApiCall { productsApi.listShops(category = category, state = state, district = district, area = area) }

    suspend fun shop(id: String): ApiResult<Shop> = safeApiCall { productsApi.shop(id) }

    suspend fun products(shopId: String? = null, category: String? = null, search: String? = null): ApiResult<List<Product>> =
        safeApiCall { productsApi.listProducts(category = category, shopId = shopId, search = search) }

    suspend fun search(q: String, state: String? = null, district: String? = null): ApiResult<List<Product>> =
        safeApiCall { productsApi.searchProducts(q, state, district) }

    suspend fun reviews(targetType: String, targetId: String): ApiResult<ReviewsResponse> =
        safeApiCall { productsApi.reviews(targetType, targetId) }

    suspend fun videos(targetType: String, targetId: String): ApiResult<List<Review>> =
        safeApiCall { productsApi.videos(targetType, targetId) }

    suspend fun reviewsEligible(): ApiResult<ReviewsEligibleResponse> =
        safeApiCall { productsApi.reviewsEligible() }

    suspend fun submitReview(targetType: String, targetId: String, rating: Int, comment: String): ApiResult<Review> =
        safeApiCall { productsApi.createReview(ReviewInput(targetType, targetId, rating, comment)) }
}
