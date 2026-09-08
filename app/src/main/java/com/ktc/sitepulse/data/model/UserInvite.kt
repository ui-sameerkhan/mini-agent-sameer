package com.ktc.sitepulse.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude

/**
 * Firestore: userInvites/{lowercased email}.
 *
 * A profile lives at users/{firebaseUid}, but an admin creating access for a login that already
 * exists has no way to learn that account's UID — Firebase only reveals it to the account
 * itself. So the intended role and sites are left here instead, and the account turns them into
 * a real profile the next time it signs in.
 *
 * This is safe because the invite dictates the contents: security rules only accept a
 * self-created profile whose role and assignedSites match the invite exactly, so claiming one
 * can never grant more than the administrator chose to give.
 */
data class UserInvite(
    @DocumentId @get:Exclude val docId: String = "",
    val email: String = "",
    val name: String = "",
    val role: String = Role.FOREMAN.id,
    val assignedSites: List<String> = emptyList(),
    val employeeId: String? = null,
    val invitedBy: String = "",
    val invitedAt: String = "",
)
