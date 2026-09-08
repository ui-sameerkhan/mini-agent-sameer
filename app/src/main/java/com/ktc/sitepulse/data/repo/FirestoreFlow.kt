package com.ktc.sitepulse.data.repo

import android.util.Log
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf

/** Live-subscribes a query (onSnapshot equivalent), closing the listener when the flow collector stops. */
fun Query.asFlow(): Flow<List<DocumentSnapshot>> = callbackFlow {
    val registration = addSnapshotListener { snapshot, error ->
        if (error != null) {
            close(error)
            return@addSnapshotListener
        }
        if (snapshot != null) trySend(snapshot.documents)
    }
    awaitClose { registration.remove() }
}

/** Live-subscribes a single document — used by AppVersionRepository so a force-update
 * threshold change in the Console is picked up immediately, even by an already-open app. */
fun DocumentReference.asFlow(): Flow<DocumentSnapshot?> = callbackFlow {
    val registration = addSnapshotListener { snapshot, error ->
        if (error != null) {
            close(error)
            return@addSnapshotListener
        }
        trySend(snapshot)
    }
    awaitClose { registration.remove() }
}

/**
 * Runs [query] once per authorised site and merges the results into one list.
 *
 * Firestore security rules are not filters: a query that isn't itself constrained to what the
 * caller may read is rejected outright rather than returning a filtered subset. So a site-scoped
 * user cannot ask for "all workers" and be given their own — they must ask per site.
 *
 * One query per site rather than a single `whereIn` deliberately: `whereIn` caps at 30 values and,
 * combined with another equality clause, needs a composite index that has to be deployed by hand.
 * Equality-only queries are served by Firestore's automatic indexes, and users hold a handful of
 * sites, so the listener count stays small.
 */
fun <T> mergePerSite(siteCodes: List<String>, query: (String) -> Flow<List<T>>): Flow<List<T>> =
    when {
        siteCodes.isEmpty() -> flowOf(emptyList())
        siteCodes.size == 1 -> query(siteCodes.first())
        else -> combine(siteCodes.distinct().map(query)) { results -> results.toList().flatten() }
    }

class FirestoreOpException(cause: FirebaseFirestoreException) : Exception(cause)

/**
 * TEMPORARY on-device diagnostics: there's no logcat access when the only
 * test device is a phone with no dev tooling attached, so this surfaces what
 * would otherwise only go to Log.e() as a visible in-app banner instead —
 * see toObjectSafe() below and its callers in each repository.
 */
object ParseDiagnostics {
    private val _lastMessage = MutableStateFlow<String?>(null)
    val lastMessage: StateFlow<String?> = _lastMessage

    fun reportParseFailure(collectionName: String, docId: String, error: Throwable) {
        val msg = "⚠ $collectionName/$docId didn't match the app's data model: ${error.message}"
        Log.e("SitePulse", msg, error)
        _lastMessage.value = msg
    }

    fun clear() { _lastMessage.value = null }
}

/**
 * Like DocumentSnapshot.toObject(), but a single malformed document (e.g. a
 * field type the web app wrote that doesn't cleanly map to our Kotlin model)
 * is skipped and reported instead of throwing and blanking out the *entire*
 * list — toObject() failing on one bad doc used to collapse a whole
 * onSnapshot batch to an empty list with no visible error.
 */
fun <T> DocumentSnapshot.toObjectSafe(clazz: Class<T>, collectionName: String = clazz.simpleName ?: "?"): T? =
    try {
        toObject(clazz)
    } catch (e: Exception) {
        ParseDiagnostics.reportParseFailure(collectionName, id, e)
        null
    }
