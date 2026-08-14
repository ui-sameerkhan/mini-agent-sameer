package com.lazyshopper.app.feature.customer.chat

import com.lazyshopper.app.core.data.remote.ApiResult
import com.lazyshopper.app.core.data.remote.api.ChatApi
import com.lazyshopper.app.core.data.remote.dto.ChatMessage
import com.lazyshopper.app.core.data.remote.dto.ChatSend
import com.lazyshopper.app.core.data.remote.dto.OrderChatResponse
import com.lazyshopper.app.core.data.remote.dto.SupportChatResponse
import com.lazyshopper.app.core.data.remote.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val chatApi: ChatApi,
) {
    suspend fun orderChat(orderId: String): ApiResult<OrderChatResponse> =
        safeApiCall { chatApi.orderChat(orderId) }

    suspend fun sendOrderChat(orderId: String, text: String): ApiResult<ChatMessage> =
        safeApiCall { chatApi.sendOrderChat(orderId, ChatSend(text)) }

    suspend fun supportChat(): ApiResult<SupportChatResponse> =
        safeApiCall { chatApi.supportChat() }

    suspend fun sendSupportChat(text: String): ApiResult<ChatMessage> =
        safeApiCall { chatApi.sendSupportChat(ChatSend(text)) }
}
