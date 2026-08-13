package com.lazyshopper.app.core.data.remote.api

import com.lazyshopper.app.core.data.remote.dto.*
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.http.*

interface ProductsApi {
    @GET("api/home")
    suspend fun home(
        @Query("state") state: String? = null,
        @Query("district") district: String? = null,
        @Query("area") area: String? = null,
    ): HomeResponse

    @GET("api/products/search")
    suspend fun searchProducts(
        @Query("q") q: String,
        @Query("state") state: String? = null,
        @Query("district") district: String? = null,
    ): List<Product>

    @GET("api/locations")
    suspend fun locations(): List<StateLocations>

    @GET("api/categories")
    suspend fun categories(): List<Category>

    @Multipart
    @POST("api/upload")
    suspend fun uploadImage(@Part file: MultipartBody.Part): ImageIdResponse

    @Multipart
    @POST("api/upload-video")
    suspend fun uploadVideo(@Part file: MultipartBody.Part): VideoIdResponse

    @Streaming
    @GET("api/files/{fid}")
    suspend fun file(@Path("fid") fid: String): ResponseBody

    @POST("api/products")
    suspend fun createProduct(@Body body: ProductInput): Product

    @GET("api/products")
    suspend fun listProducts(
        @Query("category") category: String? = null,
        @Query("shop_id") shopId: String? = null,
        @Query("subcategory") subcategory: String? = null,
        @Query("search") search: String? = null,
    ): List<Product>

    @GET("api/products/mine")
    suspend fun myProducts(): List<Product>

    @PUT("api/products/{pid}")
    suspend fun updateProduct(@Path("pid") id: String, @Body body: ProductInput): Product

    @PUT("api/products/{pid}/approve")
    suspend fun approveProduct(@Path("pid") id: String, @Query("status") status: String): MessageResponse

    @GET("api/admin/products")
    suspend fun adminProducts(): List<Product>

    @DELETE("api/products/{pid}")
    suspend fun deleteProduct(@Path("pid") id: String): MessageResponse

    @POST("api/shops")
    suspend fun createShop(@Body body: ShopInput): Shop

    @GET("api/shops")
    suspend fun listShops(
        @Query("category") category: String? = null,
        @Query("state") state: String? = null,
        @Query("district") district: String? = null,
        @Query("area") area: String? = null,
    ): List<Shop>

    @GET("api/shops/mine")
    suspend fun myShops(): List<Shop>

    @GET("api/shops/{sid}")
    suspend fun shop(@Path("sid") id: String): Shop

    @GET("api/admin/shops")
    suspend fun adminShops(): List<Shop>

    @PUT("api/shops/{sid}/approve")
    suspend fun approveShop(@Path("sid") id: String, @Query("status") status: String): MessageResponse

    @DELETE("api/shops/{sid}")
    suspend fun deleteShop(@Path("sid") id: String): MessageResponse

    @PUT("api/shops/{sid}/timings")
    suspend fun setShopTimings(
        @Path("sid") id: String,
        @Query("open_time") openTime: String,
        @Query("close_time") closeTime: String,
    ): MessageResponse

    @GET("api/offers/live")
    suspend fun liveOffers(): List<Offer>

    @GET("api/banners")
    suspend fun banners(@Query("banner_type") bannerType: String? = null): List<Banner>

    @GET("api/videos")
    suspend fun videos(@Query("target_type") targetType: String, @Query("target_id") targetId: String): List<Review>

    @POST("api/banners/{bid}/click")
    suspend fun bannerClick(@Path("bid") id: String): OkResponse

    @POST("api/banners/{bid}/impression")
    suspend fun bannerImpression(@Path("bid") id: String): OkResponse

    @POST("api/reviews")
    suspend fun createReview(@Body body: ReviewInput): Review

    @GET("api/reviews")
    suspend fun reviews(@Query("target_type") targetType: String, @Query("target_id") targetId: String): ReviewsResponse

    @GET("api/reviews/eligible")
    suspend fun reviewsEligible(): ReviewsEligibleResponse

    @Multipart
    @POST("api/reviews/video")
    suspend fun createVideoReview(
        @Part("target_type") targetType: okhttp3.RequestBody,
        @Part("target_id") targetId: okhttp3.RequestBody,
        @Part("rating") rating: okhttp3.RequestBody,
        @Part("comment") comment: okhttp3.RequestBody,
        @Part("video_id") videoId: okhttp3.RequestBody,
    ): Review

    @GET("api/notifications")
    suspend fun notifications(): List<NotificationDto>

    @GET("api/notifications/unread-count")
    suspend fun notificationsUnreadCount(): UnreadCountResponse

    @PUT("api/notifications/{nid}/read")
    suspend fun markNotificationRead(@Path("nid") id: String): MessageResponse

    @PUT("api/notifications/read-all")
    suspend fun markAllNotificationsRead(): MessageResponse
}
