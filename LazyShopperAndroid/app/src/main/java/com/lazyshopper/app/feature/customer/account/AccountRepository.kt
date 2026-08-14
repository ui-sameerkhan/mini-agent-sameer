package com.lazyshopper.app.feature.customer.account

import com.lazyshopper.app.core.data.remote.ApiResult
import com.lazyshopper.app.core.data.remote.api.AuthApi
import com.lazyshopper.app.core.data.remote.api.ProductsApi
import com.lazyshopper.app.core.data.remote.dto.MessageResponse
import com.lazyshopper.app.core.data.remote.dto.NotificationDto
import com.lazyshopper.app.core.data.remote.dto.ProfileInput
import com.lazyshopper.app.core.data.remote.dto.UnreadCountResponse
import com.lazyshopper.app.core.data.remote.dto.UserResponse
import com.lazyshopper.app.core.data.remote.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepository @Inject constructor(
    private val authApi: AuthApi,
    private val productsApi: ProductsApi,
) {
    suspend fun me(): ApiResult<UserResponse> = safeApiCall { authApi.me() }

    suspend fun updateProfile(name: String?, email: String?, password: String?, phone: String?): ApiResult<UserResponse> =
        safeApiCall { authApi.updateProfile(ProfileInput(name = name, email = email, password = password, phone = phone)) }

    suspend fun notifications(): ApiResult<List<NotificationDto>> = safeApiCall { productsApi.notifications() }

    suspend fun notificationsUnreadCount(): ApiResult<UnreadCountResponse> = safeApiCall { productsApi.notificationsUnreadCount() }

    suspend fun markNotificationRead(id: String): ApiResult<MessageResponse> = safeApiCall { productsApi.markNotificationRead(id) }

    suspend fun markAllNotificationsRead(): ApiResult<MessageResponse> = safeApiCall { productsApi.markAllNotificationsRead() }
}
