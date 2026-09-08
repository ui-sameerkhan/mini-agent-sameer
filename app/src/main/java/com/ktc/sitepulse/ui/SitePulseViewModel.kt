package com.ktc.sitepulse.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.messaging.FirebaseMessaging
import com.ktc.sitepulse.AppContainer
import com.ktc.sitepulse.Constants
import com.ktc.sitepulse.data.model.Announcement
import com.ktc.sitepulse.data.model.AppVersionGate
import com.ktc.sitepulse.data.model.ArrivalRequest
import com.ktc.sitepulse.data.model.Holiday
import com.ktc.sitepulse.data.model.ALL_SITES
import com.ktc.sitepulse.data.model.Role
import com.ktc.sitepulse.data.model.Timekeeper
import com.ktc.sitepulse.data.model.UserInvite
import com.ktc.sitepulse.data.model.UserProfile
import com.ktc.sitepulse.data.model.Attendance
import com.ktc.sitepulse.data.model.Blocked
import com.ktc.sitepulse.data.model.Leave
import com.ktc.sitepulse.data.model.Site
import com.ktc.sitepulse.data.model.Worker
import com.ktc.sitepulse.data.repo.PushResult
import com.ktc.sitepulse.data.repo.SessionState
import com.ktc.sitepulse.domain.BackupRestoreResult
import com.ktc.sitepulse.domain.DateUtils
import com.ktc.sitepulse.domain.Manpower
import com.ktc.sitepulse.domain.ManpowerSummary
import com.ktc.sitepulse.domain.MarkDirection
import com.ktc.sitepulse.domain.MarkResult
import com.ktc.sitepulse.domain.ParsedImport
import com.ktc.sitepulse.domain.Permissions
import com.ktc.sitepulse.domain.SessionProfile
import com.ktc.sitepulse.domain.RawTable
import com.ktc.sitepulse.domain.ReportEngine
import com.ktc.sitepulse.domain.SpreadsheetReader
import com.ktc.sitepulse.domain.WorkersImport
import com.ktc.sitepulse.util.NetworkStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File

/** Dashboard project-scope sentinel meaning "every project", kept out of the site-code space. */
const val DASHBOARD_ALL_PROJECTS = "ALL"

enum class ImportKind { WORKERS, OUTSOURCE, ROSTER }

data class PendingImport(
    val kind: ImportKind,
    val records: List<Worker>,
    val overwritingCount: Int,
    val newCount: Int,
    val reassignments: List<Triple<String, String?, String>>,
    val skippedNotFound: List<String>,
)

data class PendingDelete(val kind: String, val id: String, val label: String)

data class PendingRestore(val parsed: BackupRestoreResult)

@OptIn(ExperimentalCoroutinesApi::class)
class SitePulseViewModel(application: Application) : AndroidViewModel(application) {
    private val container = AppContainer.get(application)

    val session: StateFlow<SessionState> = container.authRepository.sessionState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SessionState(null, ""))

    private val _dataError = MutableStateFlow<String?>(null)
    /** Surfaces live-query failures (e.g. Firestore permission-denied) instead of silently showing empty lists. */
    val dataError: StateFlow<String?> = _dataError

    private fun <T> Flow<List<T>>.recoverToEmpty(source: String): Flow<List<T>> = catch { e ->
        _dataError.value = "⚠ $source failed to load: ${e.message}"
        emit(emptyList())
    }

    /**
     * Only runs the underlying Firestore query while a user is actually signed in — otherwise
     * a query fired at app-cold-start (before Auth has resolved) or after sign-out surfaces a
     * confusing PERMISSION_DENIED that has nothing to do with real data access problems.
     */
    private fun <T> onlyWhenLoggedIn(source: String, query: () -> Flow<List<T>>): Flow<List<T>> =
        session.map { it.isLoggedIn }.distinctUntilChanged()
            .flatMapLatest { loggedIn -> if (loggedIn) query().recoverToEmpty(source) else flowOf(emptyList()) }

    /** Resolved once per login via a cheap doc-existence check — Timekeeper is a Firestore-managed
     * role (admin adds/removes accounts from Roster), not an email-pattern check like isAdmin/
     * isOfficeStaff, so it can't live on SessionState itself without making that whole type async. */
    val isTimekeeper: StateFlow<Boolean> = session.map { it.email }.distinctUntilChanged()
        .flatMapLatest { email -> flow { emit(if (email.isBlank()) false else container.timekeeperRepository.isTimekeeper(email)) }.catch { emit(false) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // ---- Identity, role and site access ----

    /**
     * The signed-in user's stored profile, live so a role or site change made by Super Admin
     * reaches them without signing out. Null when they have no users/{uid} document yet.
     */
    private val liveUserProfile: StateFlow<UserProfile?> = session.map { it.uid }.distinctUntilChanged()
        .flatMapLatest { uid ->
            if (uid.isNullOrBlank()) flowOf(null)
            else container.userRepository.live(uid).catch { emit(null) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        // An account given access before it ever signed in has its role waiting as an invite,
        // because only the account itself can reveal the UID a profile must be stored under.
        // Claiming it here is the first thing that happens after login. Security rules only
        // accept a profile matching the invite exactly, so this cannot grant more than the
        // administrator chose; a failure just leaves the user on their previous access.
        viewModelScope.launch {
            session.map { it.uid to it.email }.distinctUntilChanged().collect { (uid, email) ->
                if (uid.isNullOrBlank() || email.isBlank()) return@collect
                runCatching {
                    if (container.userRepository.get(uid) != null) return@runCatching
                    val invite = container.userRepository.getInvite(email) ?: return@runCatching
                    container.userRepository.claimInvite(uid, email, invite, DateUtils.nowIso())
                }
                runCatching { container.userRepository.touchLastLogin(uid, DateUtils.nowIso()) }
            }
        }
    }

    /**
     * Who is signed in and what they may do — the single source every screen reads.
     *
     * An account with no stored profile is not locked out: it falls back to the access the app
     * granted before this collection existed, so every login that worked yesterday still works.
     * Super Admin gives them a real profile through User Management, and from that point the
     * stored one takes over.
     */
    val sessionProfile: StateFlow<SessionProfile> =
        combine(session, liveUserProfile, isTimekeeper) { session, profile, isTk ->
            when {
                !session.isLoggedIn -> SessionProfile.SIGNED_OUT
                profile != null -> SessionProfile.fromProfile(session.uid, session.email, profile)
                else -> SessionProfile.legacyFallback(
                    uid = session.uid,
                    email = session.email,
                    isConfiguredAdmin = session.isAdmin,
                    isTimekeeper = isTk,
                    isOfficeStaffDomain = session.isOfficeStaff,
                )
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, SessionProfile.SIGNED_OUT)

    /**
     * The user's own site scope, resolved before any site-filtered query is built. Null means
     * "every site" (Super Admin, or a legacy account with no profile yet); a list means query
     * per site, because Firestore rules reject an unconstrained query from a scoped user rather
     * than filtering it — see mergePerSite().
     */
    private val siteScope: Flow<List<String>?> = sessionProfile
        .map { if (it.hasAllSites) null else it.assignedSites.filter { c -> c.isNotBlank() } }
        .distinctUntilChanged()

    /** Runs the site-scoped query for a restricted user and the plain one for an unrestricted one. */
    private fun <T> scoped(
        source: String,
        all: () -> Flow<List<T>>,
        perSite: (List<String>) -> Flow<List<T>>,
    ): Flow<List<T>> = combine(
        session.map { it.isLoggedIn }.distinctUntilChanged(),
        siteScope,
    ) { loggedIn, scope -> loggedIn to scope }
        .flatMapLatest { (loggedIn, scope) ->
            when {
                !loggedIn -> flowOf(emptyList())
                scope == null -> all().recoverToEmpty(source)
                scope.isEmpty() -> flowOf(emptyList()) // holds no sites: nothing to show, not an error
                else -> perSite(scope).recoverToEmpty(source)
            }
        }

    val workers: StateFlow<List<Worker>> = scoped(
        "Workers",
        all = { container.workersRepository.liveWorkers() },
        perSite = { container.workersRepository.liveWorkersForSites(it) },
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sites: StateFlow<List<Site>> = onlyWhenLoggedIn("Sites") { container.sitesRepository.liveSites() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Eagerly (not WhileSubscribed) because todayAttendance.value is read synchronously from
    // several places that never themselves collect it as a Compose state — AttendanceEngine's
    // duplicate check-in/out guard, Worker Locator, "day == today" report generation, and the
    // Attendance list for today. Only DashboardScreen ever calls collectAsState() on this flow;
    // under WhileSubscribed(5000) a session that never opened Dashboard kept .value stuck at
    // the emptyList() initial value forever, so those reads silently saw "nobody checked in
    // today" even with real check-ins present — the Excel report's Absent Report sheet then
    // marked every worker absent and the per-site/summary/trade sheets had nothing to show.
    val todayAttendance: StateFlow<List<Attendance>> = scoped(
        "Attendance",
        all = { container.attendanceRepository.liveToday() },
        perSite = { container.attendanceRepository.liveTodayForSites(it) },
    ).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Today's check-ins/outs where the physical site didn't match the ERP roster's aligned site. */
    val siteDeviationsToday: StateFlow<List<Attendance>> = todayAttendance
        .map { list -> list.filter { it.siteMismatch && !it.deviationReviewed } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val blocked: StateFlow<List<Blocked>> = scoped(
        "Blocked attempts",
        all = { container.blockedRepository.liveLast14Days() },
        perSite = { container.blockedRepository.liveLast14DaysForSites(it) },
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingArrivals: StateFlow<List<ArrivalRequest>> = scoped(
        "Pending arrivals",
        all = { container.arrivalRequestRepository.livePending() },
        perSite = { container.arrivalRequestRepository.livePendingForSites(it) },
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingLeaveRequests: StateFlow<List<Leave>> = session.map { it.isAdmin }.distinctUntilChanged()
        .flatMapLatest { isAdmin -> if (isAdmin) container.leaveRepository.livePendingRequests().recoverToEmpty("Pending leave requests") else flowOf(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Company-wide holidays — a worker with no attendance on one of these dates shows as
     * "HOLIDAY" rather than "ABSENT" on the Absent Report. Admin manages the list from Roster. */
    val holidays: StateFlow<List<Holiday>> = session.map { it.isAdmin }.distinctUntilChanged()
        .flatMapLatest { isAdmin -> if (isAdmin) container.holidayRepository.live().recoverToEmpty("Holidays") else flowOf(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addHoliday(date: String, name: String) {
        if (date.isBlank() || name.isBlank()) { setStatus("holidayStatus", "❌ Enter both a date and a name."); return }
        viewModelScope.launch {
            try {
                container.holidayRepository.add(date, name.trim(), session.value.email, DateUtils.nowIso())
                setStatus("holidayStatus", "✅ Holiday added.")
            } catch (e: Throwable) {
                setStatus("holidayStatus", "❌ Failed: ${e.message ?: e::class.simpleName}")
            }
        }
    }

    fun deleteHoliday(date: String) {
        viewModelScope.launch {
            try {
                container.holidayRepository.delete(date)
                setStatus("holidayStatus", "🗑 Holiday removed.")
            } catch (e: Throwable) {
                setStatus("holidayStatus", "❌ Failed: ${e.message ?: e::class.simpleName}")
            }
        }
    }

    /** One-tap seed of the UAE MOHRE 2026-2027 calendar — see UaeHolidaysMohre for sourcing
     * notes. Merge-based, so re-running it after correcting an "(unconfirmed)" date to the real
     * one (via Delete + Add Holiday) won't recreate the old entry. */
    fun seedUaeMohreHolidays() {
        viewModelScope.launch {
            try {
                val now = DateUtils.nowIso()
                val holidayDocs = com.ktc.sitepulse.domain.UaeHolidaysMohre.ENTRIES_2026_2027.map { e ->
                    Holiday(date = e.date, name = e.name, addedBy = session.value.email, addedAt = now)
                }
                container.holidayRepository.bulkAdd(holidayDocs)
                setStatus("holidayStatus", "✅ Loaded ${holidayDocs.size} UAE MOHRE holiday dates (2026-2027). Entries marked \"unconfirmed\" depend on moon sighting — verify against the current MOHRE circular closer to each date.")
            } catch (e: Throwable) {
                setStatus("holidayStatus", "❌ Failed: ${e.message ?: e::class.simpleName}")
            }
        }
    }


    /** Admin-only list backing the Manage Timekeepers panel in Roster. */
    val timekeepers: StateFlow<List<Timekeeper>> = session.map { it.isAdmin }.distinctUntilChanged()
        .flatMapLatest { isAdmin -> if (isAdmin) container.timekeeperRepository.live().recoverToEmpty("Timekeepers") else flowOf(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** [password] blank = just grant Timekeeper access to an email that already has a login;
     * non-blank = actually create that login too (via AuthRepository.createUserAccount), so
     * admin never needs Firebase Console to hand someone a working account. */
    fun addTimekeeper(email: String, password: String = "") {
        val trimmed = email.trim()
        if (trimmed.isBlank() || !trimmed.contains("@")) { setStatus("timekeeperStatus", "❌ Enter a valid email address."); return }
        if (password.isNotEmpty() && password.length < 6) { setStatus("timekeeperStatus", "❌ Password must be at least 6 characters."); return }
        viewModelScope.launch {
            try {
                if (password.isNotEmpty()) {
                    setStatus("timekeeperStatus", "⏳ Creating account…")
                    container.authRepository.createUserAccount(getApplication(), trimmed, password).getOrThrow()
                }
                container.timekeeperRepository.add(trimmed, session.value.email, DateUtils.nowIso())
                setStatus(
                    "timekeeperStatus",
                    if (password.isNotEmpty()) "✅ Account created — share this email and password with them directly (it won't be shown again)."
                    else "✅ Timekeeper access granted."
                )
            } catch (e: Throwable) {
                setStatus("timekeeperStatus", "❌ Failed: ${e.message ?: e::class.simpleName}")
            }
        }
    }

    // ---- User management (Super Admin) ----

    /**
     * Creates a login and its profile in one step. The Firebase Auth account is made through a
     * throwaway secondary app instance so Super Admin isn't signed out and replaced by the
     * account they just created (see AuthRepository.createUserAccount).
     *
     * The profile document is keyed by Firebase UID, which this client can't read back for an
     * account it isn't signed in as — so the profile is keyed by the UID looked up from the
     * users collection if a profile already exists, and otherwise recorded against the email for
     * Super Admin to complete once that person signs in for the first time.
     */
    fun createUser(
        email: String,
        password: String,
        name: String,
        role: Role,
        assignedSites: List<String>,
        employeeId: String?,
    ) {
        val trimmedEmail = email.trim().lowercase()
        when {
            trimmedEmail.isBlank() || !trimmedEmail.contains("@") -> {
                setStatus("userStatus", "❌ Enter a valid email address."); return
            }
            name.isBlank() -> { setStatus("userStatus", "❌ Enter the person's name."); return }
            password.isNotEmpty() && password.length < 6 -> {
                setStatus("userStatus", "❌ Password must be at least 6 characters."); return
            }
            role != Role.SUPER_ADMIN && role != Role.STAFF && assignedSites.isEmpty() -> {
                setStatus("userStatus", "❌ Assign at least one site to a ${role.label}."); return
            }
        }
        viewModelScope.launch {
            try {
                val now = DateUtils.nowIso()
                val sites = if (role == Role.SUPER_ADMIN) listOf(ALL_SITES) else assignedSites
                val existing = container.userRepository.findByEmail(trimmedEmail)

                // Creating the login is the only moment the new account's UID is knowable from
                // here — a profile must live at users/{uid}, so it's captured and used directly.
                val newUid = if (password.isNotEmpty()) {
                    setStatus("userStatus", "⏳ Creating login…")
                    container.authRepository.createUserAccount(getApplication(), trimmedEmail, password).getOrThrow()
                } else null

                val uid = newUid ?: existing?.uid
                if (uid != null) {
                    setStatus("userStatus", "⏳ Saving profile…")
                    container.userRepository.save(
                        UserProfile(
                            uid = uid,
                            name = name.trim(),
                            email = trimmedEmail,
                            role = role.id,
                            assignedSites = sites,
                            status = "active",
                            employeeId = employeeId?.trim()?.ifBlank { null },
                            createdAt = existing?.createdAt ?: now,
                            updatedAt = now,
                            createdBy = session.value.email,
                        )
                    )
                    setStatus(
                        "userStatus",
                        if (newUid != null)
                            "✅ ${role.label} created. Share the email and password directly — they won't be shown again."
                        else "✅ Profile updated for $trimmedEmail.",
                    )
                } else {
                    // The login already exists but its UID is only ever visible to that account,
                    // so the role is left as an invite for it to claim on next sign-in.
                    setStatus("userStatus", "⏳ Saving invite…")
                    container.userRepository.saveInvite(
                        UserInvite(
                            email = trimmedEmail,
                            name = name.trim(),
                            role = role.id,
                            assignedSites = sites,
                            employeeId = employeeId?.trim()?.ifBlank { null },
                            invitedBy = session.value.email,
                            invitedAt = now,
                        )
                    )
                    setStatus(
                        "userStatus",
                        "✅ ${role.label} access prepared for $trimmedEmail. It applies the next time they sign in — " +
                            "ask them to open the app once, then they'll appear in this list.",
                    )
                }
            } catch (e: Throwable) {
                setStatus("userStatus", "❌ Failed: ${e.message ?: e::class.simpleName}")
            }
        }
    }

    fun updateUserRole(uid: String, role: Role) {
        viewModelScope.launch {
            try {
                container.userRepository.setRole(uid, role.id, DateUtils.nowIso())
                setStatus("userStatus", "✅ Role changed to ${role.label}.")
            } catch (e: Throwable) {
                setStatus("userStatus", "❌ Failed: ${e.message ?: e::class.simpleName}")
            }
        }
    }

    fun updateUserSites(uid: String, sites: List<String>) {
        viewModelScope.launch {
            try {
                container.userRepository.setAssignedSites(uid, sites, DateUtils.nowIso())
                setStatus("userStatus", "✅ Site assignments updated (${sites.size}).")
            } catch (e: Throwable) {
                setStatus("userStatus", "❌ Failed: ${e.message ?: e::class.simpleName}")
            }
        }
    }

    /**
     * Disabling is deliberately not deletion — the Firebase Auth login and every attendance
     * record they marked stay intact, so historical attributions remain readable.
     */
    fun setUserStatus(uid: String, active: Boolean) {
        viewModelScope.launch {
            try {
                container.userRepository.setStatus(uid, if (active) "active" else "disabled", DateUtils.nowIso())
                setStatus("userStatus", if (active) "✅ Account re-activated." else "🚫 Account disabled.")
            } catch (e: Throwable) {
                setStatus("userStatus", "❌ Failed: ${e.message ?: e::class.simpleName}")
            }
        }
    }

    fun removeTimekeeper(email: String) {
        viewModelScope.launch {
            try {
                container.timekeeperRepository.delete(email)
                setStatus("timekeeperStatus", "🗑 Timekeeper removed.")
            } catch (e: Throwable) {
                setStatus("timekeeperStatus", "❌ Failed: ${e.message ?: e::class.simpleName}")
            }
        }
    }

    /**
     * Leave that is still running as of today, for the dashboard's live "On Leave" headcount.
     * Firestore only lets admin and timekeepers read the whole `leaves` collection, so anyone
     * else gets an empty list rather than a permission error — the dashboard renders the same,
     * just without a leave figure it was never allowed to see.
     */
    private val activeLeaves: StateFlow<List<Leave>> =
        combine(session.map { it.isAdmin }.distinctUntilChanged(), isTimekeeper) { isAdmin, isTk -> isAdmin || isTk }
            .distinctUntilChanged()
            .flatMapLatest { canReadLeaves ->
                val profile = sessionProfile.value
                when {
                    !canReadLeaves -> flowOf(emptyList())
                    // All-sites holders and timekeepers may read leave unconstrained; a
                    // site-scoped admin must query per site or the rules refuse outright.
                    profile.hasAllSites || profile.role == Role.TIMEKEEPER ->
                        container.leaveRepository.liveActiveFrom(DateUtils.todayStrUtc()).recoverToEmpty("Leave records")
                    profile.assignedSites.isEmpty() -> flowOf(emptyList())
                    else -> container.leaveRepository
                        .liveActiveFromForSites(DateUtils.todayStrUtc(), profile.assignedSites)
                        .recoverToEmpty("Leave records")
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Set when a signed-in account has been disabled, so the UI can show the message and sign
     * them out rather than leaving them on a screen with no data and no explanation.
     */
    val accountDisabled: StateFlow<Boolean> = sessionProfile
        .map { it.isLoggedIn && !it.isActive }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /** The sites this user may work with — every site for Super Admin, their assignments otherwise. */
    val authorizedSites: StateFlow<List<Site>> =
        combine(sessionProfile, sites) { profile, allSites -> Permissions.authorizedSites(profile, allSites) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Workers this user may see, filtered by their site assignments. */
    val visibleWorkers: StateFlow<List<Worker>> =
        combine(sessionProfile, workers) { profile, allWorkers -> Permissions.visibleWorkers(profile, allWorkers) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Every user account — Super Admin only, backing User Management. */
    val allUsers: StateFlow<List<UserProfile>> = sessionProfile.map { Permissions.canManageUsers(it) }
        .distinctUntilChanged()
        .flatMapLatest { canManage ->
            if (canManage) container.userRepository.liveAll().recoverToEmpty("User accounts") else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ---- Dashboard scope (date + project) ----

    private val _dashboardDate = MutableStateFlow(DateUtils.todayStrUtc())
    /** Which day the dashboard is showing — defaults to today, changeable to any past date. */
    val dashboardDate: StateFlow<String> = _dashboardDate

    private val _dashboardProject = MutableStateFlow(DASHBOARD_ALL_PROJECTS)
    /** [DASHBOARD_ALL_PROJECTS] or a single site code, scoping every figure on the dashboard. */
    val dashboardProject: StateFlow<String> = _dashboardProject

    fun setDashboardDate(date: String) { _dashboardDate.value = date }

    fun setDashboardProject(code: String) { _dashboardProject.value = code }

    fun resetDashboardScope() {
        _dashboardDate.value = DateUtils.todayStrUtc()
        _dashboardProject.value = DASHBOARD_ALL_PROJECTS
    }

    /** Attendance plus the date it belongs to, so a summary can never label itself with a date
     * its rows didn't come from while a fetch for a newly-picked day is still in flight. */
    private data class DatedAttendance(val date: String, val rows: List<Attendance>, val loading: Boolean)

    /**
     * Today streams live (the existing subscription, so check-ins still appear as they happen);
     * any other date is fetched once on demand — a past day's records don't change while you're
     * looking at them, so a second live listener would just cost reads.
     */
    private val dashboardAttendance: StateFlow<DatedAttendance> =
        _dashboardDate.flatMapLatest { date ->
            if (date == DateUtils.todayStrUtc()) {
                todayAttendance.map { DatedAttendance(date, it, loading = false) }
            } else {
                flow {
                    emit(DatedAttendance(date, emptyList(), loading = true))
                    val profile = sessionProfile.value
                    val rows = if (profile.hasAllSites) {
                        container.attendanceRepository.getForDate(date)
                    } else {
                        container.attendanceRepository.getForDateForSites(date, profile.assignedSites)
                    }
                    emit(DatedAttendance(date, rows, loading = false))
                }.catch { e ->
                    _dataError.value = "⚠ Attendance for $date failed to load: ${e.message}"
                    emit(DatedAttendance(date, emptyList(), loading = false))
                }
            }
        }.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000),
            DatedAttendance(DateUtils.todayStrUtc(), emptyList(), loading = false),
        )

    /** Leave covering the selected dashboard date — live for today, one-shot for a past date. */
    private val dashboardLeaves: StateFlow<List<Leave>> =
        combine(
            combine(session.map { it.isAdmin }.distinctUntilChanged(), isTimekeeper) { isAdmin, isTk -> isAdmin || isTk },
            _dashboardDate,
        ) { canRead, date -> canRead to date }
            .distinctUntilChanged()
            .flatMapLatest { (canRead, date) ->
                when {
                    !canRead -> flowOf(emptyList())
                    date == DateUtils.todayStrUtc() -> activeLeaves
                    else -> flow {
                        val profile = sessionProfile.value
                        emit(
                            if (profile.hasAllSites || profile.role == Role.TIMEKEEPER)
                                container.leaveRepository.getActiveOn(date)
                            else container.leaveRepository.getActiveOnForSites(date, profile.assignedSites)
                        )
                    }.catch { emit(emptyList()) }
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** True while a past date's attendance is still being fetched. */
    val dashboardLoading: StateFlow<Boolean> = dashboardAttendance
        .map { it.loading }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /**
     * The whole dashboard in one value — every KPI card and all three breakdown tables read from
     * this, so the worker/attendance/leave lists are walked once per change instead of once per
     * widget. Recomputing only when one of its inputs actually changes is what keeps the
     * dashboard smooth on a roster of several thousand.
     *
     * Scoping to a single project narrows the roster to workers assigned there and the
     * attendance to marks recorded there, so every figure — including the trade and supplier
     * breakdowns — describes that project alone.
     */
    val manpowerSummary: StateFlow<ManpowerSummary> =
        combine(workers, dashboardAttendance, dashboardLeaves, sites, _dashboardProject) { workers, dated, leaves, sites, project ->
            if (project == DASHBOARD_ALL_PROJECTS) {
                Manpower.compute(workers, dated.rows, leaves, sites, dated.date)
            } else {
                Manpower.compute(
                    workers.filter { it.site == project },
                    dated.rows.filter { it.siteCode == project },
                    leaves,
                    sites.filter { it.code == project },
                    dated.date,
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ManpowerSummary.EMPTY)

    private val liveAnnouncement: StateFlow<Announcement?> = session.map { it.isLoggedIn }.distinctUntilChanged()
        .flatMapLatest { loggedIn -> if (loggedIn) container.announcementRepository.latest().catch { emit(null) } else flowOf(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _dismissedAnnouncementId = MutableStateFlow<String?>(null)
    /** Admin's most recent broadcast — visible to every signed-in role as a dismissible banner until a newer one arrives. */
    val activeAnnouncement: StateFlow<Announcement?> = combine(liveAnnouncement, _dismissedAnnouncementId) { announcement, dismissedId ->
        announcement?.takeIf { it.docId.isNotBlank() && it.docId != dismissedId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun dismissAnnouncement(id: String) { _dismissedAnnouncementId.value = id }

    // Live (not one-time), so bumping minVersionCode in the Console blocks an already-open
    // session immediately — not just on the next cold start. Eagerly collected (not
    // WhileSubscribed) so it's already known by the time SitePulseRoot renders its first frame.
    val versionGate: StateFlow<AppVersionGate?> = session.map { it.isLoggedIn }.distinctUntilChanged()
        .flatMapLatest { loggedIn -> if (loggedIn) container.appVersionRepository.live().catch { emit(null) } else flowOf(null) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Admin-only: sets/clears the remote force-update threshold. */
    fun saveVersionGate(minVersionCode: Long, updateUrl: String, message: String) {
        viewModelScope.launch {
            setStatus("versionGateStatus", "⏳ Saving…")
            try {
                container.appVersionRepository.save(minVersionCode, updateUrl, message)
                setStatus("versionGateStatus", "✅ Saved.")
            } catch (e: Throwable) {
                setStatus("versionGateStatus", "❌ Failed: ${e.message ?: e::class.simpleName}")
            }
        }
    }

    // Office staff accounts are permanently bound to the first Worker ID they check in with —
    // loaded eagerly (not lazily via stateIn's WhileSubscribed) so CheckInScreen can lock the
    // field before the user ever tries a mark() call.
    private val _myLinkedWorkerId = MutableStateFlow<String?>(null)
    val myLinkedWorkerId: StateFlow<String?> = _myLinkedWorkerId

    init {
        viewModelScope.launch {
            session.map { it.email to it.isOfficeStaff }.distinctUntilChanged().collect { (email, isOfficeStaff) ->
                _myLinkedWorkerId.value = if (isOfficeStaff) container.staffWorkerLinkRepository.getLinkedWorkerId(email) else null
            }
        }
    }

    fun dismissDataError() { _dataError.value = null }

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError

    private val _markResult = MutableStateFlow<MarkResult?>(null)
    val markResult: StateFlow<MarkResult?> = _markResult

    private val _markInFlight = MutableStateFlow(false)
    val markInFlight: StateFlow<Boolean> = _markInFlight

    private val _pendingImport = MutableStateFlow<PendingImport?>(null)
    val pendingImport: StateFlow<PendingImport?> = _pendingImport

    private val _pendingDelete = MutableStateFlow<PendingDelete?>(null)
    val pendingDelete: StateFlow<PendingDelete?> = _pendingDelete

    private val _statusMessages = MutableStateFlow<Map<String, String>>(emptyMap())
    val statusMessages: StateFlow<Map<String, String>> = _statusMessages

    private fun setStatus(key: String, msg: String) {
        _statusMessages.value = _statusMessages.value + (key to msg)
    }

    // ---- Auth ----

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _loginError.value = "❌ Enter email and password."
            return
        }
        viewModelScope.launch {
            val result = container.authRepository.login(email, password)
            _loginError.value = if (result.isFailure) "❌ Wrong email or password." else null
        }
    }

    fun logout() {
        container.authRepository.logout()
        _dataError.value = null
    }

    // ---- Check-in / out ----

    fun mark(dir: MarkDirection, worker: Worker) {
        if (_markInFlight.value) return
        _markInFlight.value = true
        _markResult.value = null
        viewModelScope.launch {
            val s = session.value
            val ctx = getApplication<Application>()
            val profile = sessionProfile.value
            val lockedId = if (s.isOfficeStaff) _myLinkedWorkerId.value else null
            // Someone punching themselves in — either an office-staff account bound to its own
            // worker record, or a Staff-role login marking the employee it's linked to. Both get
            // the wider staff allowance rather than the tight site geofence.
            val isSelf = s.isOfficeStaff ||
                (profile.role == Role.STAFF && profile.employeeId?.equals(worker.id, true) == true)
            val result = container.attendanceEngine.mark(
                dir = dir,
                worker = worker,
                isAdmin = s.isAdmin,
                currentEmail = s.email,
                sites = sites.value,
                todayAttendance = todayAttendance.value,
                isOnline = NetworkStatus.isOnline(ctx),
                lockedWorkerId = lockedId,
                isSelfCheckIn = isSelf,
            )
            _markResult.value = result
            // Best-effort admin nudge for a flagged deviation — Roster's Site Deviations card is
            // the source of truth either way, so a Netlify hiccup here is silently ignored.
            if (result is MarkResult.Success && result.siteMismatch) {
                try {
                    val token = container.settingsRepository.getAdminPushToken()
                    if (!token.isNullOrBlank()) {
                        container.netlifyApi.sendPush(
                            token, "Site deviation flagged",
                            "${worker.name} checked in away from the ERP-aligned site.", "#/roster"
                        )
                    }
                } catch (e: Throwable) {
                    // Notification failed silently — the deviation flag is already saved and visible on Roster.
                }
            }
            // First successful check-in/out for an office-staff account permanently binds
            // their login email to this Worker ID (transaction-guarded — see assignIfAbsent).
            // The check-in itself already succeeded and is already shown above, so a failure
            // here (e.g. a network blip on the follow-up transaction) shouldn't crash the app —
            // worst case the ID just isn't locked yet and gets locked on the next check-in.
            if (s.isOfficeStaff && lockedId == null && result is MarkResult.Success) {
                try {
                    _myLinkedWorkerId.value = container.staffWorkerLinkRepository.assignIfAbsent(s.email, worker.id, DateUtils.nowIso())
                } catch (e: Throwable) {
                    // Silently retried on the next check-in.
                }
            }
            _markInFlight.value = false
        }
    }

    fun clearMarkResult() { _markResult.value = null }

    // ---- Sites ----

    suspend fun saveSite(site: Site) = container.sitesRepository.saveSite(site)

    fun requestDeleteSite(code: String, name: String) {
        _pendingDelete.value = PendingDelete("site", code, name)
    }

    // ---- Workers ----

    suspend fun saveWorker(worker: Worker) {
        val withSno = if (worker.sno == 0L) worker.copy(sno = container.workersRepository.nextSno(workers.value)) else worker
        container.workersRepository.saveWorker(withSno)
    }

    fun requestDeleteWorker(id: String, name: String) {
        _pendingDelete.value = PendingDelete("worker", id, name)
    }

    fun requestDeleteAllWorkers() {
        _pendingDelete.value = PendingDelete("allWorkers", "", "ALL ${workers.value.size} workers")
    }

    fun confirmPendingDelete(typedText: String, onProgress: (String) -> Unit = {}) {
        val pending = _pendingDelete.value ?: return
        if (typedText.trim() != "DELETE") return
        viewModelScope.launch {
            try {
                when (pending.kind) {
                    "site" -> container.sitesRepository.deleteSite(pending.id)
                    "worker" -> container.workersRepository.deleteWorker(pending.id)
                    "allWorkers" -> container.workersRepository.deleteAll(workers.value.map { it.id }) { done, total ->
                        onProgress("Deleted $done / $total…")
                    }
                }
            } catch (e: Throwable) {
                setStatus("deleteStatus", "❌ Delete failed: ${e.message ?: e::class.simpleName}")
            }
            _pendingDelete.value = null
        }
    }

    fun cancelPendingDelete() { _pendingDelete.value = null }

    fun toggleWorkerStatus(worker: Worker) {
        viewModelScope.launch {
            try {
                if (worker.isLeft) {
                    container.workersRepository.setStatus(worker.id, "active", null)
                } else {
                    container.workersRepository.setStatus(worker.id, "left", DateUtils.todayStrUtc())
                }
            } catch (e: Throwable) {
                setStatus("deleteStatus", "❌ Update failed: ${e.message ?: e::class.simpleName}")
            }
        }
    }

    // ---- Excel imports ----

    fun startImport(kind: ImportKind, uri: Uri, fileName: String) {
        viewModelScope.launch {
            val statusKey = statusKeyFor(kind)
            setStatus(statusKey, "⏳ Reading file…")
            try {
                val ctx = getApplication<Application>()
                val table: RawTable = SpreadsheetReader.read(ctx, uri, fileName)
                val nextSno = container.workersRepository.nextSno(workers.value)
                val parsed = when (kind) {
                    ImportKind.WORKERS -> WorkersImport.parseWorkersMaster(table, workers.value, nextSno)
                    ImportKind.OUTSOURCE -> WorkersImport.parseOutsource(table, workers.value, nextSno)
                    ImportKind.ROSTER -> WorkersImport.parseRoster(table, workers.value, DateUtils.todayStrUtc())
                }
                when (parsed) {
                    is ParsedImport.ColumnsNotFound -> setStatus(statusKey, "❌ ${parsed.message}")
                    is ParsedImport.NoValidRows -> setStatus(statusKey, "❌ ${parsed.message}")
                    is ParsedImport.Ok -> {
                        if (parsed.overwritingIds.isNotEmpty() || parsed.reassignments.isNotEmpty()) {
                            _pendingImport.value = PendingImport(
                                kind, parsed.records, parsed.overwritingIds.size, parsed.newCount,
                                parsed.reassignments, parsed.skippedNotFound
                            )
                            setStatus(statusKey, "⏳ Review changes before uploading…")
                        } else {
                            commitImport(kind, parsed.records, parsed.skippedNotFound, statusKey)
                        }
                    }
                }
            } catch (e: Exception) {
                setStatus(statusKeyFor(kind), "❌ ${e.message}")
            }
        }
    }

    fun confirmPendingImport() {
        val pending = _pendingImport.value ?: return
        _pendingImport.value = null
        val statusKey = statusKeyFor(pending.kind)
        viewModelScope.launch {
            try {
                commitImport(pending.kind, pending.records, pending.skippedNotFound, statusKey)
            } catch (e: Throwable) {
                setStatus(statusKey, "❌ Upload failed: ${e.message ?: e::class.simpleName}")
            }
        }
    }

    fun cancelPendingImport() {
        val kind = _pendingImport.value?.kind ?: ImportKind.WORKERS
        _pendingImport.value = null
        setStatus(statusKeyFor(kind), "Cancelled — no changes made.")
    }

    private suspend fun commitImport(kind: ImportKind, records: List<Worker>, skippedNotFound: List<String>, statusKey: String) {
        setStatus(statusKey, "⏳ Uploading 0 / ${records.size}…")
        container.workersRepository.batchUpsert(records) { done, total ->
            setStatus(statusKey, "⏳ Uploading $done / $total…")
        }
        val skippedNote = if (skippedNotFound.isNotEmpty()) {
            " Skipped (not found): ${skippedNotFound.take(10).joinToString(", ")}${if (skippedNotFound.size > 10) "…" else ""}"
        } else ""
        setStatus(statusKey, "✅ Uploaded ${records.size} record(s).$skippedNote")
    }

    private fun statusKeyFor(kind: ImportKind) = when (kind) {
        ImportKind.WORKERS -> "xlstatus"
        ImportKind.OUTSOURCE -> "outsourceStatus"
        ImportKind.ROSTER -> "rosterUploadStatus"
    }

    // ---- Roster / leave / arrivals ----

    fun markLeave(workerId: String, fromDate: String, toDate: String, reason: String?, leaveType: String = "Annual") {
        viewModelScope.launch {
            val worker = workers.value.find { it.id == workerId }
            if (worker == null) { setStatus("leaveStatus", "❌ Worker ID not found."); return@launch }
            if (fromDate.isBlank()) { setStatus("leaveStatus", "❌ Enter a From date."); return@launch }
            try {
                container.leaveRepository.add(
                    Leave(
                        workerId = workerId, site = worker.site.orEmpty(), fromDate = fromDate,
                        toDate = toDate.ifBlank { fromDate }, reason = reason, markedBy = session.value.email,
                        ts = DateUtils.nowIso(), leaveType = leaveType,
                    )
                )
                setStatus("leaveStatus", "✅ Leave marked for ${worker.name}.")
            } catch (e: Throwable) {
                setStatus("leaveStatus", "❌ Failed: ${e.message ?: e::class.simpleName}")
            }
        }
    }

    /** Same as [markLeave], for a whole crew at once — a rain day, public holiday, or site
     * shutdown affecting many workers rather than marking each one individually. */
    fun markLeaveBulk(workerIds: List<String>, fromDate: String, toDate: String, reason: String?, leaveType: String = "Annual") {
        if (fromDate.isBlank()) { setStatus("leaveStatus", "❌ Enter a From date."); return }
        if (workerIds.isEmpty()) { setStatus("leaveStatus", "❌ Select at least one worker."); return }
        viewModelScope.launch {
            try {
                val now = DateUtils.nowIso()
                val leaves = workerIds.mapNotNull { id ->
                    val worker = workers.value.find { it.id == id } ?: return@mapNotNull null
                    Leave(
                        workerId = id, site = worker.site.orEmpty(), fromDate = fromDate,
                        toDate = toDate.ifBlank { fromDate }, reason = reason, markedBy = session.value.email,
                        ts = now, leaveType = leaveType,
                    )
                }
                container.leaveRepository.bulkAdd(leaves)
                setStatus("leaveStatus", "✅ Leave marked for ${leaves.size} worker(s).")
            } catch (e: Throwable) {
                setStatus("leaveStatus", "❌ Bulk mark failed: ${e.message ?: e::class.simpleName}")
            }
        }
    }

    /** Approved-Annual-leave days a worker has used within a given year, for balance display. */
    fun annualLeaveUsedDays(leaves: List<Leave>, workerId: String, year: Int = DateUtils.todayStrUtc().substring(0, 4).toInt()): Int {
        return leaves.filter {
            it.workerId == workerId && it.status == "approved" && it.leaveType == "Annual" &&
                it.fromDate.take(4).toIntOrNull() == year
        }.sumOf { l ->
            val from = runCatching { java.time.LocalDate.parse(l.fromDate) }.getOrNull() ?: return@sumOf 0
            val to = runCatching { java.time.LocalDate.parse(l.toDate.ifBlank { l.fromDate }) }.getOrNull() ?: from
            (java.time.temporal.ChronoUnit.DAYS.between(from, to) + 1).toInt().coerceAtLeast(0)
        }
    }

    /** Admin acknowledges a site-deviation flag (worker checked in somewhere other than their ERP-aligned site). */
    fun acknowledgeSiteDeviation(a: Attendance) {
        viewModelScope.launch {
            try {
                container.attendanceRepository.writeMark(
                    a.date, a.workerId,
                    mapOf("deviationReviewed" to true, "deviationReviewedBy" to session.value.email, "deviationReviewedAt" to DateUtils.nowIso()),
                )
            } catch (e: Throwable) {
                setStatus("deviationStatus", "❌ Failed: ${e.message ?: e::class.simpleName}")
            }
        }
    }

    // ---- Office staff: self-service leave application + own attendance history ----

    suspend fun submitLeaveApplication(workerId: String, fromDate: String, toDate: String, reason: String?, leaveType: String = "Annual") {
        if (fromDate.isBlank()) { setStatus("myLeaveStatus", "❌ Enter a From date."); return }
        val worker = workers.value.find { it.id == workerId.trim() }
        container.leaveRepository.add(
            Leave(
                workerId = workerId.trim(), site = worker?.site.orEmpty(), fromDate = fromDate,
                toDate = toDate.ifBlank { fromDate }, reason = reason, markedBy = session.value.email,
                ts = DateUtils.nowIso(), status = "pending", requestedBy = session.value.email, leaveType = leaveType,
            )
        )
        setStatus("myLeaveStatus", "✅ Leave application submitted — pending admin approval.")

        // Best-effort notification — a Netlify/network hiccup here shouldn't undo the
        // "submitted" status above, since the leave request itself already saved fine.
        try {
            val toLabel = toDate.ifBlank { fromDate }
            val token = container.settingsRepository.getAdminPushToken()
            if (!token.isNullOrBlank()) {
                container.netlifyApi.sendPush(
                    token, "New leave request",
                    "${worker?.name ?: workerId} — $fromDate to $toLabel", "#/roster"
                )
            }
            container.netlifyApi.sendEmail(
                Constants.NOTIFY_EMAILS,
                "New leave request: ${worker?.name ?: workerId}",
                """
                <table>
                  <tr><td>Worker</td><td>${worker?.name ?: workerId} ($workerId)</td></tr>
                  <tr><td>From</td><td>$fromDate</td></tr>
                  <tr><td>To</td><td>$toLabel</td></tr>
                  <tr><td>Reason</td><td>${reason.orEmpty()}</td></tr>
                  <tr><td>Requested By</td><td>${session.value.email}</td></tr>
                </table>
                <p>This request is pending admin approval in SitePulse — Roster page.</p>
                """.trimIndent()
            )
        } catch (e: Throwable) {
            // Notification failed silently — the leave application itself is already saved.
        }
    }

    suspend fun myLeaveRequests(): List<Leave> = container.leaveRepository.forRequester(session.value.email)

    /** Admin-only: every leave record ever — pending, approved, and rejected — for the Leave History panel. */
    suspend fun allLeaveHistory(): List<Leave> = container.leaveRepository.all()

    suspend fun myAttendanceHistory(): List<Attendance> = container.attendanceRepository.getMarkedBy(session.value.email)

    fun approveLeaveRequest(leave: Leave) {
        if (leave.docId.isBlank()) { setStatus("leaveReviewStatus", "❌ This request has no ID — can't be approved. Ask the requester to resubmit."); return }
        viewModelScope.launch {
            try {
                container.leaveRepository.approve(leave.docId, session.value.email, DateUtils.nowIso())
            } catch (e: Throwable) {
                setStatus("leaveReviewStatus", "❌ Approve failed: ${e.message ?: e::class.simpleName}")
            }
        }
    }

    fun rejectLeaveRequest(leave: Leave) {
        if (leave.docId.isBlank()) { setStatus("leaveReviewStatus", "❌ This request has no ID — can't be rejected. Ask the requester to resubmit."); return }
        viewModelScope.launch {
            try {
                container.leaveRepository.reject(leave.docId, session.value.email, DateUtils.nowIso())
            } catch (e: Throwable) {
                setStatus("leaveReviewStatus", "❌ Reject failed: ${e.message ?: e::class.simpleName}")
            }
        }
    }

    fun deleteLeaveRequest(leave: Leave) {
        if (leave.docId.isBlank()) { setStatus("leaveReviewStatus", "❌ This request has no ID — can't be deleted here. Remove it in the Firebase Console."); return }
        viewModelScope.launch {
            try {
                container.leaveRepository.delete(leave.docId)
                setStatus("leaveReviewStatus", "🗑 Leave request deleted.")
            } catch (e: Throwable) {
                setStatus("leaveReviewStatus", "❌ Delete failed: ${e.message ?: e::class.simpleName}")
            }
        }
    }

    fun submitNewArrival(site: String, workerId: String, name: String, designation: String, date: String) {
        viewModelScope.launch {
            if (site.isBlank() || workerId.isBlank()) {
                setStatus("arrivalStatus", "❌ Select a project site and enter a Worker ID.")
                return@launch
            }
            val existing = workers.value.find { it.id == workerId }
            if (existing != null && !existing.site.isNullOrBlank() && existing.site != site && !existing.isLeft) {
                setStatus("arrivalStatus", "⚠ ${existing.name} is already aligned to ${existing.site}. Ask admin to reassign if this is a genuine transfer.")
                return@launch
            }
            val finalName = existing?.name ?: name
            val finalDesignation = existing?.designation ?: designation
            if (finalName.isBlank() || finalDesignation.isBlank()) {
                setStatus("arrivalStatus", "❌ Enter the worker's name and trade.")
                return@launch
            }
            try {
                val request = ArrivalRequest(
                    site = site, workerId = workerId, name = finalName, designation = finalDesignation,
                    requestedDate = date.ifBlank { DateUtils.todayStrUtc() }, requestedBy = session.value.email,
                    status = "pending", ts = DateUtils.nowIso(),
                )
                container.arrivalRequestRepository.submit(request)
                setStatus("arrivalStatus", "✅ Sent for admin approval…")

                // Best-effort notification — a Netlify/network hiccup here shouldn't undo the
                // "sent for approval" status above, since the request itself already saved fine.
                try {
                    val token = container.settingsRepository.getAdminPushToken()
                    if (!token.isNullOrBlank()) {
                        container.netlifyApi.sendPush(token, "New arrival request", "$finalName (ID $workerId) for $site", "#/roster")
                    }
                    container.netlifyApi.sendEmail(
                        Constants.NOTIFY_EMAILS,
                        "New arrival: $finalName ($site)",
                        arrivalEmailHtml(finalName, workerId, finalDesignation, site, request.requestedDate, request.requestedBy)
                    )
                } catch (e: Throwable) {
                    // Notification failed silently — the arrival request itself is already saved.
                }
            } catch (e: Throwable) {
                setStatus("arrivalStatus", "❌ Failed: ${e.message ?: e::class.simpleName}")
            }
        }
    }

    private fun arrivalEmailHtml(name: String, id: String, designation: String, site: String, date: String, requestedBy: String) = """
        <table>
          <tr><td>Name</td><td>$name</td></tr>
          <tr><td>Worker ID</td><td>$id</td></tr>
          <tr><td>Designation</td><td>$designation</td></tr>
          <tr><td>Project Code</td><td>$site</td></tr>
          <tr><td>Arrival Date</td><td>$date</td></tr>
          <tr><td>Reported By</td><td>$requestedBy</td></tr>
        </table>
        <p>This request is pending admin approval in SitePulse — Roster page.</p>
    """.trimIndent()

    fun approveArrival(request: ArrivalRequest) {
        if (request.docId.isBlank()) { setStatus("arrivalReviewStatus", "❌ This request has no ID — can't be approved."); return }
        viewModelScope.launch {
            try {
                val existing = workers.value.find { it.id == request.workerId }
                container.workersRepository.saveWorker(
                    Worker(
                        sno = existing?.sno ?: container.workersRepository.nextSno(workers.value),
                        id = request.workerId, name = request.name, designation = request.designation,
                        company = existing?.company, site = request.site, alignedDate = request.requestedDate,
                        status = "active",
                    )
                )
                container.arrivalRequestRepository.approve(request.docId, session.value.email, DateUtils.nowIso())
            } catch (e: Throwable) {
                setStatus("arrivalReviewStatus", "❌ Approve failed: ${e.message ?: e::class.simpleName}")
            }
        }
    }

    fun rejectArrival(request: ArrivalRequest) {
        if (request.docId.isBlank()) { setStatus("arrivalReviewStatus", "❌ This request has no ID — can't be rejected."); return }
        viewModelScope.launch {
            try {
                container.arrivalRequestRepository.reject(request.docId, session.value.email, DateUtils.nowIso())
            } catch (e: Throwable) {
                setStatus("arrivalReviewStatus", "❌ Reject failed: ${e.message ?: e::class.simpleName}")
            }
        }
    }

    // ---- Notifications ----

    /** Open to every role now — registers this device in pushTokens so it can receive admin announcements. */
    fun enableNotifications() {
        viewModelScope.launch {
            setStatus("pushStatus", "⏳ Getting notification token…")
            try {
                val token = FirebaseMessaging.getInstance().token.await()
                val email = session.value.email
                container.pushTokensRepository.register(email, token, DateUtils.nowIso())
                if (session.value.isAdmin) {
                    // Kept in its own single-token slot too — the arrival/leave/deviation
                    // notifications above only ever address the admin, so this stays separate
                    // from the many-device pushTokens registry used for broadcasts.
                    container.settingsRepository.saveAdminPushToken(token, email, DateUtils.nowIso())
                }
                setStatus("pushStatus", "✅ Notifications enabled on this device.")
            } catch (e: Exception) {
                setStatus("pushStatus", "❌ ${e.message}")
            }
        }
    }

    /**
     * Admin-only broadcast: saves the announcement (guaranteed in-app banner for everyone) then
     * pushes it to every device that opted in. The fan-out used to be wrapped in a silent
     * try/catch — with no logcat access on the only test device, that made a push that never
     * arrived undiagnosable ("saved fine, banner shows, but nothing in the notification bar"
     * looked identical whether zero devices were registered, Firestore denied the token read,
     * or Netlify itself failed). The status message now reports which of those actually happened.
     */
    fun sendAnnouncement(message: String) {
        val text = message.trim()
        if (text.isBlank()) { setStatus("announcementStatus", "❌ Enter a message."); return }
        viewModelScope.launch {
            setStatus("announcementStatus", "⏳ Sending…")
            try {
                container.announcementRepository.send(text, session.value.email, DateUtils.nowIso())
                try {
                    val tokens = container.pushTokensRepository.allTokens()
                    if (tokens.isEmpty()) {
                        setStatus("announcementStatus", "✅ Shown in-app now. No devices are registered for push yet — each user needs to tap Enable Notifications first.")
                    } else {
                        val results = tokens.map { token -> container.netlifyApi.sendPush(token, "SitePulse announcement", text, "#/") }
                        val okCount = results.count { it is PushResult.Success }
                        val firstError = results.firstOrNull { it !is PushResult.Success }
                        val detail = when (firstError) {
                            is PushResult.HttpError -> " (send-push returned HTTP ${firstError.code})"
                            is PushResult.NetworkError -> " (${firstError.message})"
                            else -> ""
                        }
                        setStatus(
                            "announcementStatus",
                            "✅ Shown in-app now. Push reached $okCount / ${tokens.size} registered device(s).$detail"
                        )
                    }
                } catch (e: Throwable) {
                    setStatus("announcementStatus", "✅ Shown in-app now, but couldn't read the device list to push: ${e.message ?: e::class.simpleName}")
                }
            } catch (e: Throwable) {
                setStatus("announcementStatus", "❌ Failed: ${e.message ?: e::class.simpleName}")
            }
        }
    }

    /**
     * Admin-only manual attendance fix — for a GPS glitch, a forgotten check-in, or a day the
     * app wasn't used at all. Uses the same merge-write as a live check-in/out, so it works
     * equally for editing an existing record or creating one that never existed. Always tagged
     * corrected/correctedBy/correctedAt so it stays visible on every Excel export rather than
     * silently blending in with a genuine GPS-verified mark.
     */
    suspend fun correctAttendance(date: String, workerId: String, fields: Map<String, Any?>) {
        try {
            container.attendanceRepository.writeMark(
                date, workerId,
                fields + mapOf("corrected" to true, "correctedBy" to session.value.email, "correctedAt" to DateUtils.nowIso())
            )
            setStatus("attendanceEditStatus", "✅ Record saved.")
        } catch (e: Throwable) {
            setStatus("attendanceEditStatus", "❌ Failed: ${e.message ?: e::class.simpleName}")
        }
    }

    /** Same manual-correction semantics as [correctAttendance], applied to many workers at once
     * for the same date/site/shift/times — backfilling a whole crew for a day the app wasn't used. */
    suspend fun correctAttendanceBulk(date: String, workerIds: List<String>, fields: Map<String, Any?>) {
        try {
            container.attendanceRepository.bulkWriteMark(
                date, workerIds,
                fields + mapOf("corrected" to true, "correctedBy" to session.value.email, "correctedAt" to DateUtils.nowIso())
            )
            setStatus("attendanceEditStatus", "✅ Marked ${workerIds.size} worker(s) for $date.")
        } catch (e: Throwable) {
            setStatus("attendanceEditStatus", "❌ Bulk mark failed: ${e.message ?: e::class.simpleName}")
        }
    }

    // ---- Reports ----

    suspend fun attendanceForDate(date: String): List<Attendance> =
        if (date == DateUtils.todayStrUtc()) todayAttendance.value
        else container.attendanceRepository.getForDate(date)

    /** Worker Locator (Dashboard): where a specific worker was marked on a given date. */
    suspend fun locateWorker(workerId: String, date: String): Attendance? =
        if (date == DateUtils.todayStrUtc()) todayAttendance.value.find { it.workerId == workerId }
        else container.attendanceRepository.getRecord(date, workerId)

    suspend fun generateReport(params: ReportEngine.Params): File {
        val attendanceRows = if (params.range == "day") {
            if (params.dateOrMonth == DateUtils.todayStrUtc()) todayAttendance.value
            else container.attendanceRepository.getForDate(params.dateOrMonth)
        } else {
            container.attendanceRepository.getForMonth(params.dateOrMonth)
        }
        val leaves = container.leaveRepository.all()
        val holidays = container.holidayRepository.all()
        val outDir = File(getApplication<Application>().cacheDir, "reports").apply { mkdirs() }
        val workersSnapshot = workers.value
        val sitesSnapshot = sites.value
        // Apache POI's workbook writing is blocking CPU/disk work — keep it off the
        // Main/Compose dispatcher the caller is on.
        return withContext(Dispatchers.IO) {
            ReportEngine.generate(outDir, attendanceRows, workersSnapshot, sitesSnapshot, leaves, holidays, params)
        }
    }

    /** Generates one printable PDF of QR ID badges (3x4 grid per A4 page) for the given workers. */
    suspend fun generateWorkerQrBadgesPdf(workersToPrint: List<com.ktc.sitepulse.data.model.Worker>): File {
        val ctx = getApplication<Application>()
        return withContext(Dispatchers.IO) {
            com.ktc.sitepulse.domain.QrCodeUtil.generateBulkPdf(ctx, workersToPrint)
        }
    }

    /** Admin-only: every collection, all time, as one .xlsx — an offline snapshot independent of Firestore itself. */
    suspend fun generateFullBackup(): File {
        val attendanceAll = container.attendanceRepository.getAll()
        val leavesAll = container.leaveRepository.all()
        val blockedAll = container.blockedRepository.all()
        val arrivalsAll = container.arrivalRequestRepository.all()
        val outDir = File(getApplication<Application>().cacheDir, "reports").apply { mkdirs() }
        return withContext(Dispatchers.IO) {
            com.ktc.sitepulse.domain.BackupEngine.generate(
                outDir, workers.value, sites.value, attendanceAll, leavesAll, blockedAll, arrivalsAll, session.value.email,
            )
        }
    }

    // ---- Full backup restore (admin only) ----

    private val _pendingRestore = MutableStateFlow<PendingRestore?>(null)
    val pendingRestore: StateFlow<PendingRestore?> = _pendingRestore

    /** Parses the uploaded file and stages it for confirmation — nothing is written yet. */
    fun startBackupRestore(uri: Uri) {
        setStatus("backupRestoreStatus", "⏳ Reading backup file…")
        viewModelScope.launch {
            try {
                val ctx = getApplication<Application>()
                val parsed = withContext(Dispatchers.IO) {
                    com.ktc.sitepulse.domain.BackupRestoreEngine.parse(ctx, uri)
                }
                _pendingRestore.value = PendingRestore(parsed)
                setStatus("backupRestoreStatus", "⏳ Review what will be restored before uploading…")
            } catch (e: Throwable) {
                setStatus("backupRestoreStatus", "❌ ${e.message ?: e::class.simpleName}")
            }
        }
    }

    /** Commits the staged restore — adds/updates records from the file, never deletes anything. */
    fun confirmBackupRestore() {
        val pending = _pendingRestore.value ?: return
        _pendingRestore.value = null
        setStatus("backupRestoreStatus", "⏳ Uploading…")
        viewModelScope.launch {
            try {
                val p = pending.parsed
                if (p.workers.isNotEmpty()) container.workersRepository.batchUpsert(p.workers)
                if (p.sites.isNotEmpty()) container.sitesRepository.batchUpsert(p.sites)
                if (p.attendance.isNotEmpty()) container.attendanceRepository.batchUpsert(p.attendance)
                if (p.leaves.isNotEmpty()) container.leaveRepository.restoreAll(p.leaves)
                if (p.blocked.isNotEmpty()) container.blockedRepository.restoreAll(p.blocked)
                if (p.arrivals.isNotEmpty()) container.arrivalRequestRepository.restoreAll(p.arrivals)
                setStatus(
                    "backupRestoreStatus",
                    "✅ Restored ${p.workers.size} worker(s), ${p.sites.size} site(s), ${p.attendance.size} attendance record(s), " +
                        "${p.leaves.size} leave(s), ${p.blocked.size} blocked attempt(s), ${p.arrivals.size} arrival request(s).",
                )
            } catch (e: Throwable) {
                setStatus("backupRestoreStatus", "❌ Restore failed: ${e.message ?: e::class.simpleName}")
            }
        }
    }

    fun cancelBackupRestore() {
        _pendingRestore.value = null
        setStatus("backupRestoreStatus", "Cancelled — no changes made.")
    }
}
