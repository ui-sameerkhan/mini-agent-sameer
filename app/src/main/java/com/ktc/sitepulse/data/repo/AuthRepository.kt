package com.ktc.sitepulse.data.repo

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.ktc.sitepulse.Constants
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

data class SessionState(val user: FirebaseUser?) {
    val isLoggedIn: Boolean get() = user != null
    val email: String get() = user?.email.orEmpty()
    val isAdmin: Boolean get() = email.lowercase() in Constants.ADMIN_EMAILS
}

class AuthRepository(private val auth: FirebaseAuth = FirebaseAuth.getInstance()) {

    val sessionState: Flow<SessionState> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { a -> trySend(SessionState(a.currentUser)) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    val currentEmail: String get() = auth.currentUser?.email.orEmpty()
    val isAdmin: Boolean get() = currentEmail.lowercase() in Constants.ADMIN_EMAILS

    suspend fun login(email: String, password: String): Result<Unit> = runCatching {
        auth.signInWithEmailAndPassword(email.trim(), password).await()
        Unit
    }

    fun logout() = auth.signOut()
}
