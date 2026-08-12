package com.ktc.sitepulse.data.repo

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
