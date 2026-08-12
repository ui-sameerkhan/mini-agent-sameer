package com.ktc.sitepulse.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude

/** Firestore: leaves/{autoId} — admin-marked approved leave, excludes worker from Absent Report. */
data class Leave(
    @DocumentId @get:Exclude val docId: String = "",
    val workerId: String = "",
    val site: String = "",
    val fromDate: String = "",
    val toDate: String = "",
    val reason: String? = null,
    val markedBy: String = "",
    val ts: String = "",
)
