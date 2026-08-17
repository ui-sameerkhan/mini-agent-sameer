package com.ktc.sitepulse.data.repo

import com.ktc.sitepulse.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/** What actually happened calling send-push — plain success/failure swallowed the difference
 * between "no devices registered," "Firestore denied the token read," and "Netlify itself
 * rejected/never got the request," which made a silently-not-arriving push undiagnosable
 * without logcat access. sendAnnouncement() surfaces this in its status message instead. */
sealed class PushResult {
    object Success : PushResult()
    data class HttpError(val code: Int) : PushResult()
    data class NetworkError(val message: String) : PushResult()
}

/**
 * Thin client for the two Netlify functions bundled with the original PWA
 * (send-push.js, send-email.js) — reused as-is so a new arrival still notifies
 * the admin, whether reported from the web app or this Android app.
 * Both calls are best-effort/fire-and-forget, matching notifyAdmin()/notifyArrivalByEmail().
 */
class NetlifyApi(private val client: OkHttpClient = OkHttpClient()) {

    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    suspend fun sendPush(token: String, title: String, body: String, url: String): PushResult {
        if (Constants.SEND_PUSH_FUNCTION_URL.contains("REPLACE_AFTER_DEPLOY")) {
            return PushResult.NetworkError("Push function not deployed yet — see functions/README.md")
        }
        return try {
            withContext(Dispatchers.IO) {
                val payload = JSONObject().apply {
                    put("token", token)
                    put("title", title)
                    put("body", body)
                    put("url", url)
                }
                val request = Request.Builder()
                    .url(Constants.SEND_PUSH_FUNCTION_URL)
                    .post(payload.toString().toRequestBody(jsonMedia))
                    .build()
                client.newCall(request).execute().use { resp ->
                    if (resp.isSuccessful) PushResult.Success else PushResult.HttpError(resp.code)
                }
            }
        } catch (e: Exception) {
            PushResult.NetworkError(e.message ?: e::class.simpleName ?: "Unknown error")
        }
    }

    suspend fun sendEmail(to: List<String>, subject: String, html: String) {
        if (Constants.NETLIFY_BASE_URL.isBlank()) return
        runCatching {
            withContext(Dispatchers.IO) {
                val payload = JSONObject().apply {
                    put("to", org.json.JSONArray(to))
                    put("subject", subject)
                    put("html", html)
                }
                val request = Request.Builder()
                    .url(Constants.SEND_EMAIL_URL)
                    .post(payload.toString().toRequestBody(jsonMedia))
                    .build()
                client.newCall(request).execute().close()
            }
        }
    }
}
