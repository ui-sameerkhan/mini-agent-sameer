package com.ktc.sitepulse.data.repo

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.ktc.sitepulse.Constants
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.tasks.await

/**
 * Deliberately holds only the primitive uid/email — not the raw FirebaseUser
 * object. AuthStateListener can fire more often than the login state
 * actually changes (token refresh, reconnect), and a FirebaseUser instance
 * doesn't reliably compare equal across those firings. Wrapping the raw
 * object made every downstream StateFlow.distinctUntilChanged() (Kotlin's
 * default for data classes, used implicitly by stateIn/flatMapLatest) treat
 * each firing as a brand new session — which tore down and re-established
 * every live Firestore listener (workers/sites/attendance/etc.) each time,
 * re-billing a full read of each collection for no reason. Plain
 * uid/email strings give correct, predictable value equality instead.
 */
data class SessionState(val uid: String?, val email: String) {
    val isLoggedIn: Boolean get() = uid != null
    val isAdmin: Boolean get() = email.lowercase() in Constants.ADMIN_EMAILS
    val isOfficeStaff: Boolean get() = !isAdmin &&
        email.substringAfterLast("@", "").equals(Constants.OFFICE_STAFF_EMAIL_DOMAIN, ignoreCase = true)
}

class AuthRepository(private val auth: FirebaseAuth = FirebaseAuth.getInstance()) {

    val sessionState: Flow<SessionState> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { a ->
            trySend(SessionState(a.currentUser?.uid, a.currentUser?.email.orEmpty()))
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }.distinctUntilChanged()

    val currentEmail: String get() = auth.currentUser?.email.orEmpty()
    val isAdmin: Boolean get() = currentEmail.lowercase() in Constants.ADMIN_EMAILS

    suspend fun login(email: String, password: String): Result<Unit> = runCatching {
        auth.signInWithEmailAndPassword(email.trim(), password).await()
        Unit
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> = runCatching {
        auth.sendPasswordResetEmail(email.trim()).await()
        Unit
    }

    fun logout() = auth.signOut()

    /**
     * Creates a brand-new Firebase Auth login — used by admin to create a Timekeeper's actual
     * email/password from inside the app, no Firebase Console needed. createUserWithEmailAndPassword()
     * signs in as the new account on whichever Auth instance it's called on, so this routes
     * through a secondary, throwaway FirebaseApp instance rather than the shared default one —
     * otherwise it would sign the admin out of their own session to become the new user.
     */
    suspend fun createUserAccount(context: Context, email: String, password: String): Result<Unit> = runCatching {
        val secondaryApp = FirebaseApp.initializeApp(context, FirebaseApp.getInstance().options, "tk-create-${System.currentTimeMillis()}")
            ?: error("Could not start a secondary Firebase instance.")
        val secondaryAuth = FirebaseAuth.getInstance(secondaryApp)
        try {
            secondaryAuth.createUserWithEmailAndPassword(email.trim(), password).await()
        } catch (e: FirebaseAuthUserCollisionException) {
            // Account already exists under this email — fine, the caller just grants the role.
        } finally {
            secondaryAuth.signOut()
            secondaryApp.delete()
        }
    }
}
