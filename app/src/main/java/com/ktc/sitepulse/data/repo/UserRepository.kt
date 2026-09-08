package com.ktc.sitepulse.data.repo

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.ktc.sitepulse.data.model.UserInvite
import com.ktc.sitepulse.data.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

/**
 * Firestore: users/{firebaseUid}. Holds the role and site assignments that drive every access
 * decision — see domain/Permissions.kt.
 *
 * Nothing here ever deletes a Firebase Auth account. Removing a user's access is done by setting
 * their status to "disabled", which keeps their history and their attendance attributions intact
 * while stopping them signing in.
 */
class UserRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    private val collection get() = db.collection("users")

    private fun DocumentSnapshot.toProfile(): UserProfile? =
        toObjectSafe(UserProfile::class.java, "users")?.copy(uid = id)

    /** The signed-in user's own profile. Null means no document exists yet — the caller falls
     * back to the legacy email-derived access rather than treating it as a failure. */
    suspend fun get(uid: String): UserProfile? =
        collection.document(uid).get().await().toProfile()

    /** Live view of one profile, so a role or site change by Super Admin takes effect on the
     * user's next screen rather than requiring them to sign out and back in. */
    fun live(uid: String): Flow<UserProfile?> =
        collection.document(uid).asFlow().map { it?.toProfile() }

    /** Every user — Super Admin only, backing the User Management screen. */
    fun liveAll(): Flow<List<UserProfile>> =
        collection.asFlow().map { docs -> docs.mapNotNull { it.toProfile() } }

    /** Looks a profile up by email, used when linking an existing login to a new profile. */
    suspend fun findByEmail(email: String): UserProfile? =
        collection.whereEqualTo("email", email.trim().lowercase()).limit(1).get().await()
            .documents.firstOrNull()?.toProfile()

    /**
     * Creates or updates a profile. Merges rather than overwrites so fields this version of the
     * app doesn't know about (added later, or written by the web app) survive an edit here.
     */
    suspend fun save(profile: UserProfile) {
        require(profile.uid.isNotBlank()) { "A user profile needs the Firebase UID as its document ID." }
        collection.document(profile.uid).set(profile.copy(email = profile.email.trim().lowercase()), SetOptions.merge()).await()
    }

    suspend fun setStatus(uid: String, status: String, updatedAt: String) {
        collection.document(uid).set(
            mapOf("status" to status, "updatedAt" to updatedAt),
            SetOptions.merge(),
        ).await()
    }

    suspend fun setRole(uid: String, role: String, updatedAt: String) {
        collection.document(uid).set(
            mapOf("role" to role, "updatedAt" to updatedAt),
            SetOptions.merge(),
        ).await()
    }

    suspend fun setAssignedSites(uid: String, sites: List<String>, updatedAt: String) {
        collection.document(uid).set(
            mapOf("assignedSites" to sites, "updatedAt" to updatedAt),
            SetOptions.merge(),
        ).await()
    }

    /** Best-effort last-login stamp for the User Management list. Never fails a login: a user
     * whose rules don't permit this write still signs in normally. */
    suspend fun touchLastLogin(uid: String, at: String) {
        runCatching {
            collection.document(uid).set(mapOf("lastLoginAt" to at), SetOptions.merge()).await()
        }
    }

    // ---- Invites -------------------------------------------------------------------------

    private val invites get() = db.collection("userInvites")

    /** Leaves the intended role and sites for a login whose UID the admin can't know yet. */
    suspend fun saveInvite(invite: UserInvite) {
        invites.document(invite.email.trim().lowercase()).set(invite).await()
    }

    suspend fun getInvite(email: String): UserInvite? =
        invites.document(email.trim().lowercase()).get().await()
            .toObjectSafe(UserInvite::class.java, "userInvites")

    /** Outstanding invites, so Super Admin can see who hasn't signed in to claim theirs yet. */
    fun liveInvites(): Flow<List<UserInvite>> =
        invites.asFlow().map { docs ->
            docs.mapNotNull { d -> d.toObjectSafe(UserInvite::class.java, "userInvites")?.copy(docId = d.id) }
        }

    suspend fun deleteInvite(email: String) {
        invites.document(email.trim().lowercase()).delete().await()
    }

    /**
     * Turns an invite into a real profile at users/{uid}, on the invited person's first sign-in.
     *
     * The written role and sites are copied straight from the invite — security rules verify
     * they match it exactly, so this can never grant more than the administrator intended. The
     * invite is then cleared; if that delete is refused it's harmless, since a profile now
     * exists and takes precedence over any invite.
     */
    suspend fun claimInvite(uid: String, email: String, invite: UserInvite, now: String): UserProfile {
        val profile = UserProfile(
            uid = uid,
            name = invite.name,
            email = email.trim().lowercase(),
            role = invite.role,
            assignedSites = invite.assignedSites,
            status = "active",
            employeeId = invite.employeeId,
            createdAt = invite.invitedAt,
            updatedAt = now,
            createdBy = invite.invitedBy,
        )
        collection.document(uid).set(profile).await()
        runCatching { deleteInvite(email) }
        return profile
    }
}
