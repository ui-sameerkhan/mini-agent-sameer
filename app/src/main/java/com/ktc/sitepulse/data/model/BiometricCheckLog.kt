package com.ktc.sitepulse.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude

/**
 * Firestore: biometricChecks/{date__scope} — a record that the ERP biometric cross-check was
 * run, and what it found.
 *
 * The comparison itself stays in memory; this holds only the counts. It exists because a control
 * nobody can show evidence of is not a control: "we reconcile against the biometric daily" has
 * to be demonstrable, and until now the result vanished when the app closed. It is also what
 * makes the deterrent real — a foreman who can see the check ran yesterday behaves differently
 * from one who suspects it never happens.
 *
 * No worker names or punch times are stored here, only totals, so this carries nothing
 * confidential and stays small: one document per day per scope, a few hundred bytes each.
 */
data class BiometricCheckLog(
    @DocumentId @get:Exclude val docId: String = "",
    /** The day that was verified, not the day the check was run — they differ when yesterday's
     * report is checked this morning, which is the normal pattern. */
    val date: String = "",
    /** "ALL", or the site codes the person running it holds, so two people checking two
     * different projects on the same day do not overwrite each other. */
    val scope: String = "ALL",
    val runBy: String = "",
    val runAt: String = "",
    val agreed: Long = 0,
    val markedNotPunched: Long = 0,
    val punchedNotMarked: Long = 0,
    val timeMismatch: Long = 0,
    /** How many of the marked workers the biometric file covered at all — see the note on
     * BiometricReconciliation.Summary.coverage. Without this the counts above can be badly
     * misread on a project that has no reader. */
    val coverage: Long = 0,
    val markedTotal: Long = 0,
    val coverageTooLow: Boolean = false,
) {
    @get:Exclude
    val reviewCount: Long get() = markedNotPunched + punchedNotMarked + timeMismatch

    @get:Exclude
    val isClean: Boolean get() = reviewCount == 0L

    companion object {
        /** Document id: one per verified day per scope. Re-running the same day overwrites. */
        fun idFor(date: String, scope: String): String = "${date}__$scope"

        /**
         * A stable key for the sites this account holds. Firestore document ids may not contain
         * "/", and site codes are uppercase project codes, so a sorted join is safe — and sorting
         * matters, since the same two sites must not produce two different ids.
         */
        fun scopeKeyFor(hasAllSites: Boolean, assignedSites: List<String>): String =
            if (hasAllSites) "ALL"
            else assignedSites.filter { it.isNotBlank() }
                .map { it.uppercase().replace("/", "-") }
                .sorted()
                .joinToString("-")
                .ifBlank { "NONE" }
                .take(120)
    }
}
