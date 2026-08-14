package com.lazyshopper.app.feature.admin.data

import com.lazyshopper.app.core.data.remote.ApiResult
import com.lazyshopper.app.core.data.remote.api.ChatApi
import com.lazyshopper.app.core.data.remote.dto.AdminSupportMessagesResponse
import com.lazyshopper.app.core.data.remote.dto.ChatMessage
import com.lazyshopper.app.core.data.remote.dto.ChatSend
import com.lazyshopper.app.core.data.remote.dto.SupportThread
import com.lazyshopper.app.core.data.remote.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdminSupportRepository @Inject constructor(private val api: ChatApi) {
    suspend fun threads(): ApiResult<List<SupportThread>> = safeApiCall { api.adminSupportThreads() }
    suspend fun messages(customerId: String): ApiResult<AdminSupportMessagesResponse> = safeApiCall { api.adminSupportMessages(customerId) }
    suspend fun send(customerId: String, text: String): ApiResult<ChatMessage> = safeApiCall { api.adminSendSupport(customerId, ChatSend(text)) }
}
