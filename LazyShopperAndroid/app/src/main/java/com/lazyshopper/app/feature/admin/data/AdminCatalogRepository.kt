package com.lazyshopper.app.feature.admin.data

import com.lazyshopper.app.core.data.remote.ApiResult
import com.lazyshopper.app.core.data.remote.api.AdminApi
import com.lazyshopper.app.core.data.remote.api.ProductsApi
import com.lazyshopper.app.core.data.remote.dto.Banner
import com.lazyshopper.app.core.data.remote.dto.BannerInput
import com.lazyshopper.app.core.data.remote.dto.Category
import com.lazyshopper.app.core.data.remote.dto.CategoryInput
import com.lazyshopper.app.core.data.remote.dto.Coupon
import com.lazyshopper.app.core.data.remote.dto.CouponInput
import com.lazyshopper.app.core.data.remote.dto.ImageIdResponse
import com.lazyshopper.app.core.data.remote.dto.MessageResponse
import com.lazyshopper.app.core.data.remote.dto.Offer
import com.lazyshopper.app.core.data.remote.dto.OfferInput
import com.lazyshopper.app.core.data.remote.dto.Product
import com.lazyshopper.app.core.data.remote.dto.Review
import com.lazyshopper.app.core.data.remote.safeApiCall
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import okhttp3.MultipartBody
import javax.inject.Inject
import javax.inject.Singleton

/** Products, Categories, Coupons, Offers, Banners, Reviews & Video-Reviews moderation. */
@Singleton
class AdminCatalogRepository @Inject constructor(
    private val adminApi: AdminApi,
    private val productsApi: ProductsApi,
) {
    // Products
    suspend fun allProducts(): ApiResult<List<Product>> = safeApiCall { productsApi.adminProducts() }
    suspend fun approveProduct(id: String, status: String): ApiResult<MessageResponse> = safeApiCall { productsApi.approveProduct(id, status) }
    suspend fun deleteProduct(id: String): ApiResult<MessageResponse> = safeApiCall { productsApi.deleteProduct(id) }
    suspend fun uploadImage(part: MultipartBody.Part): ApiResult<ImageIdResponse> = safeApiCall { productsApi.uploadImage(part) }

    suspend fun setProductFlags(
        id: String,
        featured: Boolean? = null,
        inStock: Boolean? = null,
        stock: Double? = null,
        price: Double? = null,
        mrp: Double? = null,
        lowStockThreshold: Double? = null,
    ): ApiResult<MessageResponse> {
        val body = buildJsonObject {
            featured?.let { put("featured", JsonPrimitive(it)) }
            inStock?.let { put("in_stock", JsonPrimitive(it)) }
            stock?.let { put("stock", JsonPrimitive(it)) }
            price?.let { put("price", JsonPrimitive(it)) }
            mrp?.let { put("mrp", JsonPrimitive(it)) }
            lowStockThreshold?.let { put("low_stock_threshold", JsonPrimitive(it)) }
        }
        return safeApiCall { adminApi.setProductFlags(id, body) }
    }

    // Categories
    suspend fun categories(): ApiResult<List<Category>> = safeApiCall { productsApi.categories() }
    suspend fun createCategory(input: CategoryInput): ApiResult<Category> = safeApiCall { adminApi.createCategory(input) }
    suspend fun updateCategory(id: String, input: CategoryInput): ApiResult<MessageResponse> = safeApiCall { adminApi.updateCategory(id, input) }
    suspend fun deleteCategory(id: String): ApiResult<MessageResponse> = safeApiCall { adminApi.deleteCategory(id) }

    // Coupons
    suspend fun coupons(): ApiResult<List<Coupon>> = safeApiCall { adminApi.coupons() }
    suspend fun createCoupon(input: CouponInput): ApiResult<Coupon> = safeApiCall { adminApi.createCoupon(input) }
    suspend fun updateCoupon(id: String, input: CouponInput): ApiResult<MessageResponse> = safeApiCall { adminApi.updateCoupon(id, input) }
    suspend fun deleteCoupon(id: String): ApiResult<MessageResponse> = safeApiCall { adminApi.deleteCoupon(id) }

    // Offers
    suspend fun offers(): ApiResult<List<Offer>> = safeApiCall { adminApi.offers() }
    suspend fun createOffer(input: OfferInput): ApiResult<Offer> = safeApiCall { adminApi.createOffer(input) }
    suspend fun updateOffer(id: String, input: OfferInput): ApiResult<MessageResponse> = safeApiCall { adminApi.updateOffer(id, input) }
    suspend fun toggleOffer(id: String, active: Boolean): ApiResult<MessageResponse> = safeApiCall { adminApi.toggleOffer(id, active) }
    suspend fun deleteOffer(id: String): ApiResult<MessageResponse> = safeApiCall { adminApi.deleteOffer(id) }

    // Banners
    suspend fun banners(): ApiResult<List<Banner>> = safeApiCall { adminApi.adminBanners() }
    suspend fun createBanner(input: BannerInput): ApiResult<Banner> = safeApiCall { adminApi.createBanner(input) }
    suspend fun updateBanner(id: String, input: BannerInput): ApiResult<MessageResponse> = safeApiCall { adminApi.updateBanner(id, input) }
    suspend fun toggleBanner(id: String, active: Boolean): ApiResult<MessageResponse> = safeApiCall { adminApi.toggleBanner(id, active) }
    suspend fun deleteBanner(id: String): ApiResult<MessageResponse> = safeApiCall { adminApi.deleteBanner(id) }

    // Reviews
    suspend fun reviews(): ApiResult<List<Review>> = safeApiCall { adminApi.adminReviews() }
    suspend fun hideReview(id: String, hidden: Boolean): ApiResult<MessageResponse> = safeApiCall { adminApi.hideReview(id, hidden) }
    suspend fun deleteReview(id: String): ApiResult<MessageResponse> = safeApiCall { adminApi.deleteReview(id) }

    // Video reviews
    suspend fun videoReviews(): ApiResult<List<Review>> = safeApiCall { adminApi.videoReviews() }
    suspend fun moderateVideoReview(id: String, status: String): ApiResult<MessageResponse> = safeApiCall { adminApi.moderateVideoReview(id, status) }
    suspend fun deleteVideoReview(id: String): ApiResult<MessageResponse> = safeApiCall { adminApi.deleteVideoReview(id) }
}
