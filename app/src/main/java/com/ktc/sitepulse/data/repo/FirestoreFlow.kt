package com.ktc.sitepulse.data.repo

import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

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

class FirestoreOpException(cause: FirebaseFirestoreException) : Exception(cause)

/**
 * Like DocumentSnapshot.toObject(), but a single malformed document (e.g. a
 * field type the web app wrote that doesn't cleanly map to our Kotlin model)
 * is skipped and logged instead of throwing and blanking out the *entire*
 * list — toObject() failing on one bad doc used to collapse a whole
 * onSnapshot batch to an empty list with no visible error.
 */
fun <T> DocumentSnapshot.toObjectSafe(clazz: Class<T>): T? =
    try {
        toObject(clazz)
    } catch (e: Exception) {
        Log.e("SitePulse", "Failed to parse ${clazz.simpleName} doc '$id': ${e.message}", e)
        null
    }
