package com.lazyshopper.app.core.di

import com.lazyshopper.app.BuildConfig
import com.lazyshopper.app.core.data.local.SessionManager
import com.lazyshopper.app.core.data.remote.api.AdminApi
import com.lazyshopper.app.core.data.remote.api.AnalyticsApi
import com.lazyshopper.app.core.data.remote.api.AuthApi
import com.lazyshopper.app.core.data.remote.api.CartApi
import com.lazyshopper.app.core.data.remote.api.ChatApi
import com.lazyshopper.app.core.data.remote.api.CronsApi
import com.lazyshopper.app.core.data.remote.api.OrdersApi
import com.lazyshopper.app.core.data.remote.api.ProductsApi
import com.lazyshopper.app.core.data.remote.api.ReferralApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/** Attaches `Authorization: Bearer <token>` to every request when a session exists. */
class AuthInterceptor(private val sessionManager: SessionManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { sessionManager.currentToken() }
        val request = chain.request().newBuilder().apply {
            if (!token.isNullOrBlank()) {
                addHeader("Authorization", "Bearer $token")
            }
        }.build()
        return chain.proceed(request)
    }
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        encodeDefaults = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(sessionManager: SessionManager): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        }
        return OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(sessionManager))
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideProductsApi(retrofit: Retrofit): ProductsApi = retrofit.create(ProductsApi::class.java)

    @Provides
    @Singleton
    fun provideCartApi(retrofit: Retrofit): CartApi = retrofit.create(CartApi::class.java)

    @Provides
    @Singleton
    fun provideOrdersApi(retrofit: Retrofit): OrdersApi = retrofit.create(OrdersApi::class.java)

    @Provides
    @Singleton
    fun provideAdminApi(retrofit: Retrofit): AdminApi = retrofit.create(AdminApi::class.java)

    @Provides
    @Singleton
    fun provideAnalyticsApi(retrofit: Retrofit): AnalyticsApi = retrofit.create(AnalyticsApi::class.java)

    @Provides
    @Singleton
    fun provideChatApi(retrofit: Retrofit): ChatApi = retrofit.create(ChatApi::class.java)

    @Provides
    @Singleton
    fun provideReferralApi(retrofit: Retrofit): ReferralApi = retrofit.create(ReferralApi::class.java)

    @Provides
    @Singleton
    fun provideCronsApi(retrofit: Retrofit): CronsApi = retrofit.create(CronsApi::class.java)
}
