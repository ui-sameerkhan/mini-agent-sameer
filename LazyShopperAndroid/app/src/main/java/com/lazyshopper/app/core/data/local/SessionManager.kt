package com.lazyshopper.app.core.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.sessionDataStore by preferencesDataStore(name = "lazyshopper_session")

/** Roles mirror the backend's `role` field on the user document. */
object Role {
    const val CUSTOMER = "customer"
    const val SHOPKEEPER = "shopkeeper"
    const val DELIVERY = "delivery"
    const val ADMIN = "admin"
}

data class Session(
    val token: String?,
    val userId: String?,
    val role: String?,
    val name: String?,
    val email: String?,
)

@Singleton
class SessionManager @Inject constructor(@ApplicationContext private val context: Context) {

    private object Keys {
        val TOKEN = stringPreferencesKey("token")
        val USER_ID = stringPreferencesKey("user_id")
        val ROLE = stringPreferencesKey("role")
        val NAME = stringPreferencesKey("name")
        val EMAIL = stringPreferencesKey("email")
    }

    val sessionFlow: Flow<Session> = context.sessionDataStore.data.map { prefs ->
        Session(
            token = prefs[Keys.TOKEN],
            userId = prefs[Keys.USER_ID],
            role = prefs[Keys.ROLE],
            name = prefs[Keys.NAME],
            email = prefs[Keys.EMAIL],
        )
    }

    suspend fun currentToken(): String? = sessionFlow.first().token

    suspend fun save(token: String, userId: String, role: String, name: String?, email: String?) {
        context.sessionDataStore.edit { prefs ->
            prefs[Keys.TOKEN] = token
            prefs[Keys.USER_ID] = userId
            prefs[Keys.ROLE] = role
            prefs[Keys.NAME] = name.orEmpty()
            prefs[Keys.EMAIL] = email.orEmpty()
        }
    }

    suspend fun updateRole(role: String) {
        context.sessionDataStore.edit { prefs -> prefs[Keys.ROLE] = role }
    }

    suspend fun clear() {
        context.sessionDataStore.edit { it.clear() }
    }
}
