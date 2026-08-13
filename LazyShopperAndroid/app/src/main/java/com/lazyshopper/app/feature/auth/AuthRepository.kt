package com.lazyshopper.app.feature.auth

import com.lazyshopper.app.core.data.local.SessionManager
import com.lazyshopper.app.core.data.remote.ApiResult
import com.lazyshopper.app.core.data.remote.api.AuthApi
import com.lazyshopper.app.core.data.remote.dto.AuthResponse
import com.lazyshopper.app.core.data.remote.dto.ForgotPasswordInput
import com.lazyshopper.app.core.data.remote.dto.LoginInput
import com.lazyshopper.app.core.data.remote.dto.MessageResponse
import com.lazyshopper.app.core.data.remote.dto.RegisterInput
import com.lazyshopper.app.core.data.remote.dto.ResetPasswordInput
import com.lazyshopper.app.core.data.remote.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val api: AuthApi,
    private val session: SessionManager,
) {
    val sessionFlow = session.sessionFlow

    suspend fun login(email: String, password: String): ApiResult<AuthResponse> =
        safeApiCall { api.login(LoginInput(email, password)) }.also { persistIfSuccess(it) }

    suspend fun register(
        name: String,
        email: String,
        password: String,
        role: String,
        shopName: String?,
        phone: String?,
    ): ApiResult<AuthResponse> =
        safeApiCall { api.register(RegisterInput(name, email, password, role, shopName, phone)) }
            .also { persistIfSuccess(it) }

    suspend fun forgotPassword(email: String): ApiResult<MessageResponse> =
        safeApiCall { api.forgotPassword(ForgotPasswordInput(email)) }

    suspend fun resetPassword(token: String, newPassword: String): ApiResult<MessageResponse> =
        safeApiCall { api.resetPassword(ResetPasswordInput(token, newPassword)) }

    suspend fun logout() {
        runCatching { api.logout() }
        session.clear()
    }

    private suspend fun persistIfSuccess(result: ApiResult<AuthResponse>) {
        if (result is ApiResult.Success) {
            val body = result.data
            session.save(body.token, body.user.id, body.user.role, body.user.name, body.user.email)
        }
    }
}
