package com.lazyshopper.app.feature.admin.data

import com.lazyshopper.app.core.data.remote.ApiResult
import com.lazyshopper.app.core.data.remote.api.AnalyticsApi
import com.lazyshopper.app.core.data.remote.dto.AdminAnalyticsResponse
import com.lazyshopper.app.core.data.remote.dto.CouponAnalyticsResponse
import com.lazyshopper.app.core.data.remote.dto.StockReportResponse
import com.lazyshopper.app.core.data.remote.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdminAnalyticsRepository @Inject constructor(private val api: AnalyticsApi) {
    suspend fun analytics(days: Int = 30): ApiResult<AdminAnalyticsResponse> = safeApiCall { api.analytics(days) }
    suspend fun stockReport(): ApiResult<StockReportResponse> = safeApiCall { api.stockReport() }
    suspend fun couponAnalytics(): ApiResult<CouponAnalyticsResponse> = safeApiCall { api.couponAnalytics() }
}
