package com.ktc.sitepulse.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude

/**
 * Firestore: workers/{workerId} — doc id == id field.
 * Also doubles as the "roster": a roster entry is a worker doc with `site` set.
 */
data class Worker(
    @DocumentId @get:Exclude val docId: String = "",
    val sno: Long = 0,
    val id: String = "",
    val name: String = "",
    val designation: String = "",
    val company: String? = null,
    val site: String? = null,
    val alignedDate: String? = null,
    val status: String = "active",
    val leftDate: String? = null,
    val annualLeaveDays: Long = 30,
) {
    @get:Exclude
    val isOutsourced: Boolean get() = !company.isNullOrBlank()

    @get:Exclude
    val isLeft: Boolean get() = status == "left"
}
