package com.lazyshopper.app.feature.customer.home

import com.lazyshopper.app.core.data.remote.ApiResult
import com.lazyshopper.app.core.data.remote.api.AuthApi
import com.lazyshopper.app.core.data.remote.api.ProductsApi
import com.lazyshopper.app.core.data.remote.dto.Banner
import com.lazyshopper.app.core.data.remote.dto.Category
import com.lazyshopper.app.core.data.remote.dto.HomeResponse
import com.lazyshopper.app.core.data.remote.dto.LocationPrefInput
import com.lazyshopper.app.core.data.remote.dto.Offer
import com.lazyshopper.app.core.data.remote.dto.StateLocations
import com.lazyshopper.app.core.data.remote.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HomeRepository @Inject constructor(
    private val productsApi: ProductsApi,
    private val authApi: AuthApi,
) {
    suspend fun home(state: String?, district: String?, area: String?): ApiResult<HomeResponse> =
        safeApiCall { productsApi.home(state, district, area) }

    suspend fun liveOffers(): ApiResult<List<Offer>> = safeApiCall { productsApi.liveOffers() }

    suspend fun banners(): ApiResult<List<Banner>> = safeApiCall { productsApi.banners() }

    suspend fun categories(): ApiResult<List<Category>> = safeApiCall { productsApi.categories() }

    suspend fun locations(): ApiResult<List<StateLocations>> = safeApiCall { productsApi.locations() }

    suspend fun saveLocationPref(state: String, district: String, area: String): ApiResult<kotlinx.serialization.json.JsonObject> =
        safeApiCall { authApi.updateLocationPref(LocationPrefInput(state = state, district = district, area = area)) }

    suspend fun trackBannerClick(id: String) {
        runCatching { productsApi.bannerClick(id) }
    }

    suspend fun trackBannerImpression(id: String) {
        runCatching { productsApi.bannerImpression(id) }
    }
}
