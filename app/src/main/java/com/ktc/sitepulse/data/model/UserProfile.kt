package com.ktc.sitepulse.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude

/**
 * The five access levels. [id] is what's stored in Firestore and must never change once written —
 * the labels and badges are display-only and safe to reword.
 */
enum class Role(val id: String, val label: String, val badge: String) {
    SUPER_ADMIN("super_admin", "Super Admin", "👑"),
    ADMIN("admin", "Admin", "🛠️"),
    TIMEKEEPER("timekeeper", "Timekeeper", "📋"),
    SUPERVISOR("supervisor", "Supervisor", "👷"),
    STAFF("staff", "Staff", "👤");

    companion object {
        /**
         * Retired role ids, mapped to whatever replaced them.
         *
         * "foreman" was merged into Supervisor: the two carried byte-for-byte identical
         * permissions, and at KTC they are the same job. Merging removed a choice in User
         * Management that changed nothing.
         *
         * This map is not a migration step that can later be dropped. Profiles written before
         * the merge still say "foreman", and so do historical attendance records naming the role
         * that marked them, so the id has to keep resolving for good — and must never be re-used
         * for a different, narrower role, or those accounts would silently gain or lose access.
         */
        private val MERGED_INTO: Map<String, Role> by lazy { mapOf("foreman" to SUPERVISOR) }

        fun fromId(id: String?): Role? =
            entries.firstOrNull { it.id == id } ?: MERGED_INTO[id]
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
    /**
     * Blank means this document was never provisioned as a real profile — see [isProvisioned].
     * Deliberately NOT defaulting to a role: a partial document (one written by a stray field
     * update, say) would then silently grant that role to whoever it belonged to.
     */
    val role: String = "",
    val assignedSites: List<String> = emptyList(),
    val status: String = "active",
    /** Links a Staff login to their own worker record, so self check-in knows who they are. */
    val employeeId: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val createdBy: String? = null,
    val lastLoginAt: String? = null,
) {
    /**
     * True only when this document actually carries a role. A document holding just an
     * incidental field — a last-login stamp, for instance — is not a grant of access, and
     * treating it as one would silently demote whoever it belonged to.
     */
    @get:Exclude
    val isProvisioned: Boolean get() = role.isNotBlank()

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
