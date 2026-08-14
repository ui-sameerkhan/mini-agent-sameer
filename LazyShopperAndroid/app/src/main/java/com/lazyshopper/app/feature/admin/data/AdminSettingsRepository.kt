package com.lazyshopper.app.feature.admin.data

import com.lazyshopper.app.core.data.remote.ApiResult
import com.lazyshopper.app.core.data.remote.api.AdminApi
import com.lazyshopper.app.core.data.remote.dto.AdminLogEntry
import com.lazyshopper.app.core.data.remote.dto.AdminPaymentsResponse
import com.lazyshopper.app.core.data.remote.dto.EmailSettingsResponse
import com.lazyshopper.app.core.data.remote.dto.MessageResponse
import com.lazyshopper.app.core.data.remote.dto.NotificationDto
import com.lazyshopper.app.core.data.remote.dto.NotificationInput
import com.lazyshopper.app.core.data.remote.safeApiCall
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

/** Email settings, Payments overview, Activity logs, Admin-composed Notifications. */
@Singleton
class AdminSettingsRepository @Inject constructor(private val api: AdminApi) {
    // Email settings
    suspend fun emailSettings(): ApiResult<EmailSettingsResponse> = safeApiCall { api.emailSettings() }
    suspend fun updateEmailSettings(
        enabled: Boolean? = null,
        statuses: Map<String, Boolean>? = null,
        channels: Map<String, Boolean>? = null,
    ): ApiResult<MessageResponse> {
        val body = buildJsonObject {
            enabled?.let { put("enabled", it) }
            statuses?.let { m -> put("statuses", buildJsonObject { m.forEach { (k, v) -> put(k, v) } }) }
            channels?.let { m -> put("channels", buildJsonObject { m.forEach { (k, v) -> put(k, v) } }) }
        }
        return safeApiCall { api.updateEmailSettings(body) }
    }
    suspend fun testEmail(email: String): ApiResult<MessageResponse> = safeApiCall { api.testEmail(email) }

    // Payments & logs
    suspend fun payments(): ApiResult<AdminPaymentsResponse> = safeApiCall { api.payments() }
    suspend fun logs(): ApiResult<List<AdminLogEntry>> = safeApiCall { api.logs() }

    // Notifications
    suspend fun sendNotification(input: NotificationInput): ApiResult<NotificationDto> = safeApiCall { api.createNotification(input) }
    suspend fun notifications(): ApiResult<List<NotificationDto>> = safeApiCall { api.adminNotifications() }
    suspend fun deleteNotification(id: String): ApiResult<MessageResponse> = safeApiCall { api.deleteNotification(id) }
}
