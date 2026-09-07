package com.ktc.sitepulse.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude

/**
 * The six access levels. [id] is what's stored in Firestore and must never change once written —
 * the labels and badges are display-only and safe to reword.
 */
enum class Role(val id: String, val label: String, val badge: String) {
    SUPER_ADMIN("super_admin", "Super Admin", "👑"),
    ADMIN("admin", "Admin", "🛠️"),
    TIMEKEEPER("timekeeper", "Timekeeper", "📋"),
    SUPERVISOR("supervisor", "Supervisor", "👷"),
    FOREMAN("foreman", "Foreman", "🦺"),
    STAFF("staff", "Staff", "👤");

    companion object {
        fun fromId(id: String?): Role? = entries.firstOrNull { it.id == id }
    }
}

/** Marker inside [UserProfile.assignedSites] meaning "every site, including ones added later". */
const val ALL_SITES = "ALL"

/**
 * Firestore: users/{firebaseUid} — doc ID is the Firebase Auth UID, so a profile is bound to the
 * login rather than to an email address that might later be reused.
 *
 * A user with no document here is NOT locked out: [SessionProfile] falls back to deriving their
 * access from the pre-existing email rules, so every account that worked before this collection
 * existed still works exactly as it did. See SessionProfile.legacyFallback().
 */
data class UserProfile(
    @DocumentId @get:Exclude val uid: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = Role.SUPERVISOR.id,
    val assignedSites: List<String> = emptyList(),
    val status: String = "active",
    /** Links a Staff login to their own worker record, so self check-in knows who they are. */
    val employeeId: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val createdBy: String? = null,
    val lastLoginAt: String? = null,
) {
    @get:Exclude
    val roleEnum: Role get() = Role.fromId(role) ?: Role.SUPERVISOR

    @get:Exclude
    val isActive: Boolean get() = status == "active"

    /** Super Admin always sees everything, whatever the stored list happens to say. */
    @get:Exclude
    val hasAllSites: Boolean get() = roleEnum == Role.SUPER_ADMIN || assignedSites.contains(ALL_SITES)

    @get:Exclude
    val displayName: String get() = name.ifBlank { email.substringBefore("@") }
}
