package com.lazyshopper.app.core.data.remote

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okio.IOException
import retrofit2.HttpException

/** Thin result wrapper so ViewModels don't litter every call site with try/catch. */
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Failure(val message: String, val code: Int? = null) : ApiResult<Nothing>()
}

private val errorJson = Json { ignoreUnknownKeys = true }

@Serializable
private data class DetailEnvelope(val detail: String? = null)

suspend fun <T> safeApiCall(block: suspend () -> T): ApiResult<T> {
    return try {
        ApiResult.Success(block())
    } catch (e: HttpException) {
        val raw = e.response()?.errorBody()?.string()
        val detail = raw?.let {
            runCatching { errorJson.decodeFromString<DetailEnvelope>(it).detail }.getOrNull()
        } ?: e.message()
        ApiResult.Failure(detail ?: "Something went wrong (${e.code()})", e.code())
    } catch (e: IOException) {
        ApiResult.Failure("Can't reach the server. Check your connection.")
    } catch (e: Exception) {
        ApiResult.Failure(e.message ?: "Unexpected error")
    }
}
