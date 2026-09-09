package com.ktc.sitepulse.domain

import com.ktc.sitepulse.data.model.ALL_SITES
import com.ktc.sitepulse.data.model.Role
import com.ktc.sitepulse.data.model.Site
import com.ktc.sitepulse.data.model.UserProfile
import com.ktc.sitepulse.data.model.Worker

/**
 * The resolved identity every screen works from: who is signed in, what they may do, and which
 * sites they may see. Built once per session by the ViewModel so no screen ever re-derives
 * permissions for itself — the single most common way role checks drift apart.
 */
data class SessionProfile(
    val uid: String?,
    val email: String,
    val profile: UserProfile?,
    val role: Role,
    val assignedSites: List<String>,
    val isActive: Boolean,
    val employeeId: String?,
    /** True when no users/{uid} document existed and access was derived from the legacy email
     * rules instead — surfaced so Super Admin can see who still needs a real profile. */
    val isLegacy: Boolean,
) {
    val isLoggedIn: Boolean get() = uid != null
    val hasAllSites: Boolean get() = role == Role.SUPER_ADMIN || assignedSites.contains(ALL_SITES)
    val displayName: String get() = profile?.displayName ?: email.substringBefore("@")

    companion object {
        val SIGNED_OUT = SessionProfile(
            uid = null, email = "", profile = null, role = Role.STAFF,
            assignedSites = emptyList(), isActive = false, employeeId = null, isLegacy = false,
        )

        /**
         * Access for an account that predates the users collection. Deliberately reproduces the
         * exact rules the app used before, so every existing login keeps working unchanged and
         * nobody is locked out by the upgrade:
         *
         *  - the configured administrator email  -> Super Admin (the one account the spec allows
         *    to be migrated to full access automatically, identified from app configuration)
         *  - an email listed in `timekeepers`     -> Timekeeper
         *  - an email on the office-staff domain  -> Staff
         *  - anything else                        -> Supervisor
         *
         * Every legacy role except Super Admin is granted ALL_SITES here, because that is the
         * access these accounts already had — narrowing it silently would break working logins,
         * which the upgrade explicitly must not do. Super Admin assigns real site lists through
         * User Management, and from that moment the stored profile takes over.
         */
        fun legacyFallback(
            uid: String?,
            email: String,
            isConfiguredAdmin: Boolean,
            isTimekeeper: Boolean,
            isOfficeStaffDomain: Boolean,
        ): SessionProfile {
            val role = when {
                isConfiguredAdmin -> Role.SUPER_ADMIN
                isTimekeeper -> Role.TIMEKEEPER
                isOfficeStaffDomain -> Role.STAFF
                else -> Role.SUPERVISOR
            }
            return SessionProfile(
                uid = uid,
                email = email,
                profile = null,
                role = role,
                assignedSites = listOf(ALL_SITES),
                isActive = true,
                employeeId = null,
                isLegacy = true,
            )
        }

        /**
         * The configured administrator is Super Admin unconditionally — whatever any profile
         * document happens to say.
         *
         * This is the break-glass guarantee. Without it, one malformed users/{uid} document is
         * enough to demote the only account that can fix user access, leaving nobody able to
         * repair it from inside the app. The email comes from app configuration, not from data
         * a user can write, so it cannot be turned into an escalation path.
         */
        fun configuredAdmin(uid: String?, email: String, profile: UserProfile?): SessionProfile =
            SessionProfile(
                uid = uid,
                email = email,
                profile = profile,
                role = Role.SUPER_ADMIN,
                assignedSites = listOf(ALL_SITES),
                isActive = true,
                employeeId = profile?.employeeId,
                isLegacy = profile == null || !profile.isProvisioned,
            )

        fun fromProfile(uid: String?, email: String, profile: UserProfile): SessionProfile =
            SessionProfile(
                uid = uid,
                email = email,
                profile = profile,
                role = profile.roleEnum,
                assignedSites = if (profile.roleEnum == Role.SUPER_ADMIN) listOf(ALL_SITES) else profile.assignedSites,
                isActive = profile.isActive,
                employeeId = profile.employeeId,
                isLegacy = false,
            )
    }
}

/**
 * Every access decision in the app, in one object. Screens ask these questions rather than
 * testing roles inline, so a permission can be changed in one place instead of hunted across
 * a dozen composables.
 */
object Permissions {

    // ---- Role groupings -------------------------------------------------------------------

    /** Roles that may record attendance for other people. */
    private val ATTENDANCE_MARKERS = setOf(
        Role.SUPER_ADMIN, Role.ADMIN, Role.TIMEKEEPER, Role.SUPERVISOR, Role.FOREMAN,
    )

    /** Roles that manage the workforce itself, rather than just recording its attendance. */
    private val WORKER_MANAGERS = setOf(Role.SUPER_ADMIN, Role.ADMIN)

    /**
     * Roles that may upload a roster. Timekeepers are included because keeping their site's
     * roster current is the job — but only within the sites they hold, and without the
     * destructive operations (delete-all, delete worker) that stay with WORKER_MANAGERS.
     */
    private val ROSTER_UPLOADERS = setOf(Role.SUPER_ADMIN, Role.ADMIN, Role.TIMEKEEPER)

    /** Roles allowed to correct an attendance record after the fact. */
    private val ATTENDANCE_CORRECTORS = setOf(Role.SUPER_ADMIN, Role.ADMIN, Role.TIMEKEEPER)

    /** Roles that see manpower reporting beyond a bare headcount. */
    private val REPORT_VIEWERS = setOf(
        Role.SUPER_ADMIN, Role.ADMIN, Role.TIMEKEEPER, Role.SUPERVISOR, Role.FOREMAN,
    )

    // ---- Capability checks ----------------------------------------------------------------

    fun hasRole(session: SessionProfile, vararg roles: Role): Boolean =
        session.isActive && session.role in roles

    /** Only Super Admin creates users, changes roles, or assigns sites. */
    fun canManageUsers(session: SessionProfile): Boolean =
        session.isActive && session.role == Role.SUPER_ADMIN

    /** System-wide configuration: projects, geofences, holidays, backups, announcements. */
    fun canManageSystemSettings(session: SessionProfile): Boolean =
        session.isActive && session.role == Role.SUPER_ADMIN

    fun canManageWorkers(session: SessionProfile): Boolean =
        session.isActive && session.role in WORKER_MANAGERS

    /** Uploading or editing the roster, without the power to delete people from it. */
    fun canUploadRoster(session: SessionProfile): Boolean =
        session.isActive && session.role in ROSTER_UPLOADERS

    /** Deleting workers is deliberately narrower than editing them. */
    fun canDeleteWorkers(session: SessionProfile): Boolean =
        session.isActive && session.role in WORKER_MANAGERS

    fun canMarkAttendance(session: SessionProfile): Boolean =
        session.isActive && session.role in ATTENDANCE_MARKERS

    fun canCorrectAttendance(session: SessionProfile): Boolean =
        session.isActive && session.role in ATTENDANCE_CORRECTORS

    fun canViewReports(session: SessionProfile): Boolean =
        session.isActive && session.role in REPORT_VIEWERS

    /** Company-wide totals, as opposed to a single site's figures. */
    fun canViewCompanyWideData(session: SessionProfile): Boolean =
        session.isActive && session.role == Role.SUPER_ADMIN

    fun canApproveRequests(session: SessionProfile): Boolean =
        session.isActive && session.role in setOf(Role.SUPER_ADMIN, Role.ADMIN)

    /**
     * The Roster tab has two faces: the management panel (pending arrivals, leave, holidays,
     * site deviations) and the field panel (report a new arrival). Admins and timekeepers get
     * the former because they own leave and absence; supervisors and foremen get the latter.
     */
    fun canManageRoster(session: SessionProfile): Boolean =
        session.isActive && session.role in setOf(Role.SUPER_ADMIN, Role.ADMIN, Role.TIMEKEEPER)

    /** Staff mark only themselves; everyone else marks the workforce. */
    fun isSelfServiceOnly(session: SessionProfile): Boolean = session.role == Role.STAFF

    // ---- Site scoping ---------------------------------------------------------------------

    /**
     * The gate every site-scoped read and write goes through. An inactive account has no access
     * at all, regardless of what its assignedSites list says.
     */
    fun hasSiteAccess(session: SessionProfile, siteCode: String?): Boolean {
        if (!session.isActive) return false
        if (session.hasAllSites) return true
        if (siteCode.isNullOrBlank()) return false
        return session.assignedSites.any { it.equals(siteCode, ignoreCase = true) }
    }

    /** The sites this user may actually work with, from the full list of company sites. */
    fun authorizedSites(session: SessionProfile, allSites: List<Site>): List<Site> {
        if (!session.isActive) return emptyList()
        if (session.hasAllSites) return allSites
        return allSites.filter { site -> session.assignedSites.any { it.equals(site.code, ignoreCase = true) } }
    }

    /** Site codes only — for building Firestore queries that fetch just what's authorised. */
    fun authorizedSiteCodes(session: SessionProfile, allSites: List<Site>): List<String> =
        authorizedSites(session, allSites).map { it.code }

    /**
     * A worker is reachable when they're assigned to a site the user holds. Workers with no site
     * assigned are visible only to users who hold every site — otherwise an unassigned worker
     * would leak into every site-restricted user's list.
     */
    fun canAccessEmployee(session: SessionProfile, worker: Worker): Boolean {
        if (!session.isActive) return false
        if (session.hasAllSites) return true
        // Staff may only ever reach their own record.
        if (session.role == Role.STAFF) return worker.id == session.employeeId
        return hasSiteAccess(session, worker.site)
    }

    /** Filters a worker list down to what this user is allowed to see. */
    fun visibleWorkers(session: SessionProfile, workers: List<Worker>): List<Worker> {
        if (!session.isActive) return emptyList()
        if (session.hasAllSites) return workers
        return workers.filter { canAccessEmployee(session, it) }
    }

    /**
     * Whether attendance may be written for this worker at this site. Checks the capability and
     * the site together, because holding one without the other is not permission to mark.
     */
    fun canMarkAttendanceAt(session: SessionProfile, siteCode: String?): Boolean =
        canMarkAttendance(session) && hasSiteAccess(session, siteCode)

    /**
     * A human-readable reason a site-scoped action was refused, for showing the user something
     * more useful than a generic failure.
     */
    fun siteAccessDenialReason(session: SessionProfile, siteCode: String?): String? = when {
        !session.isLoggedIn -> "You are not signed in."
        !session.isActive -> "Your account has been disabled. Please contact the administrator."
        !hasSiteAccess(session, siteCode) ->
            "Your account does not have access to ${siteCode ?: "this site"}."
        else -> null
    }

    // ---- Navigation -----------------------------------------------------------------------

    /**
     * The routes a role may reach, by route id. Navigation is derived from the same permission
     * checks that guard the data, so a hidden tab and a refused query can't disagree — hiding a
     * tab is a convenience, never the security boundary.
     */
    fun visibleRoutes(session: SessionProfile): Set<String> {
        if (!session.isLoggedIn || !session.isActive) return emptySet()
        return when (session.role) {
            Role.SUPER_ADMIN -> setOf("checkin", "dashboard", "sites", "attendance", "workers", "roster")
            Role.ADMIN -> setOf("checkin", "dashboard", "attendance", "workers", "roster")
            // Check-In included so a timekeeper can scan ID badges to mark attendance, and
            // Workers so they can keep their own site's roster current.
            Role.TIMEKEEPER -> setOf("checkin", "dashboard", "attendance", "workers", "roster")
            // Supervisors and foremen mark attendance and read their site's manpower; they get
            // no workforce management, no site configuration and no company-wide reporting.
            // Roster included so the "Report Arrival" action on Check-In leads somewhere they
            // are actually permitted; it shows them the field panel, not the management one.
            Role.SUPERVISOR, Role.FOREMAN -> setOf("checkin", "dashboard", "attendance", "roster")
            Role.STAFF -> emptySet() // self-service screen only, reached without the tab bar
        }
    }

    /** Where a role lands after signing in. */
    fun landingRoute(session: SessionProfile): String = when (session.role) {
        Role.STAFF -> "office"
        Role.TIMEKEEPER -> "dashboard"
        else -> "checkin"
    }
}
