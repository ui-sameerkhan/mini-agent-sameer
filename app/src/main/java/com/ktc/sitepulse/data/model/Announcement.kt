package com.ktc.sitepulse.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude

/** Firestore: announcements/{autoId} — admin broadcast shown as a banner to every signed-in user. */
data class Announcement(
    @DocumentId @get:Exclude val docId: String = "",
    val message: String = "",
    val sentBy: String = "",
    val sentAt: String = "",
)
