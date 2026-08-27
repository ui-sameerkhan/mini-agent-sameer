package com.ktc.sitepulse.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude

/**
 * Firestore: timekeepers/{email} — doc id is the lowercased login email. Presence of the doc
 * IS the grant (matches firestore.rules' isTimekeeper()); no separate role field needed.
 * Timekeeper accounts get their own login with access to attendance reports and manual
 * corrections only — no worker/site/roster management, same tier as Office Staff but scoped
 * to payroll re-verification instead of self-service leave.
 */
data class Timekeeper(
    @DocumentId @get:Exclude val docId: String = "",
    val email: String = "",
    val addedBy: String = "",
    val addedAt: String = "",
)
