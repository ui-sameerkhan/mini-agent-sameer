package com.lazyshopper.app.feature.admin.data

import com.lazyshopper.app.core.data.remote.ApiResult
import com.lazyshopper.app.core.data.remote.api.AdminApi
import com.lazyshopper.app.core.data.remote.dto.AdminStats
import com.lazyshopper.app.core.data.remote.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdminDashboardRepository @Inject constructor(private val api: AdminApi) {
    suspend fun stats(): ApiResult<AdminStats> = safeApiCall { api.stats() }
}
