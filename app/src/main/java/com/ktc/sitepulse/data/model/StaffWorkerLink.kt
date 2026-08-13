package com.ktc.sitepulse.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude

/**
 * Firestore: staffWorkerLinks/{email} — permanently binds a login account to
 * the first Worker ID it ever successfully checked in with. Once set, that
 * account can never check in under a different Worker ID.
 */
data class StaffWorkerLink(
    @DocumentId @get:Exclude val docId: String = "",
    val email: String = "",
    val workerId: String = "",
    val assignedAt: String = "",
)
