package com.ktc.sitepulse.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.messaging.FirebaseMessaging
import com.ktc.sitepulse.AppContainer
import com.ktc.sitepulse.Constants
import com.ktc.sitepulse.data.model.Announcement
import com.ktc.sitepulse.data.model.ArrivalRequest
import com.ktc.sitepulse.data.model.Attendance
import com.ktc.sitepulse.data.model.Blocked
import com.ktc.sitepulse.data.model.Leave
import com.ktc.sitepulse.data.model.Site
import com.ktc.sitepulse.data.model.Worker
import com.ktc.sitepulse.data.repo.SessionState
import com.ktc.sitepulse.domain.BackupRestoreResult
import com.ktc.sitepulse.domain.DateUtils
import com.ktc.sitepulse.domain.MarkDirection
import com.ktc.sitepulse.domain.MarkResult
import com.ktc.sitepulse.domain.ParsedImport
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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File

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

    val workers: StateFlow<List<Worker>> = onlyWhenLoggedIn("Workers") { container.workersRepository.liveWorkers() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
    val todayAttendance: StateFlow<List<Attendance>> = onlyWhenLoggedIn("Attendance") { container.attendanceRepository.liveToday() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Today's check-ins/outs where the physical site didn't match the ERP roster's aligned site. */
    val siteDeviationsToday: StateFlow<List<Attendance>> = todayAttendance
        .map { list -> list.filter { it.siteMismatch && !it.deviationReviewed } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val blocked: StateFlow<List<Blocked>> = onlyWhenLoggedIn("Blocked attempts") { container.blockedRepository.liveLast14Days() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingArrivals: StateFlow<List<ArrivalRequest>> = onlyWhenLoggedIn("Pending arrivals") { container.arrivalRequestRepository.livePending() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingLeaveRequests: StateFlow<List<Leave>> = session.map { it.isAdmin }.distinctUntilChanged()
        .flatMapLatest { isAdmin -> if (isAdmin) container.leaveRepository.livePendingRequests().recoverToEmpty("Pending leave requests") else flowOf(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val liveAnnouncement: StateFlow<Announcement?> = session.map { it.isLoggedIn }.distinctUntilChanged()
        .flatMapLatest { loggedIn -> if (loggedIn) container.announcementRepository.latest().catch { emit(null) } else flowOf(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _dismissedAnnouncementId = MutableStateFlow<String?>(null)
    /** Admin's most recent broadcast — visible to every signed-in role as a dismissible banner until a newer one arrives. */
    val activeAnnouncement: StateFlow<Announcement?> = combine(liveAnnouncement, _dismissedAnnouncementId) { announcement, dismissedId ->
        announcement?.takeIf { it.docId.isNotBlank() && it.docId != dismissedId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun dismissAnnouncement(id: String) { _dismissedAnnouncementId.value = id }

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

    private val _passwordResetStatus = MutableStateFlow<String?>(null)
    val passwordResetStatus: StateFlow<String?> = _passwordResetStatus

    /**
     * Always shows the same message regardless of whether the email has an account — Firebase's
     * own error for "no such user" would otherwise let anyone probe which emails are registered.
     */
    fun sendPasswordReset(email: String) {
        if (email.isBlank()) {
            _passwordResetStatus.value = "❌ Enter your email first, then tap Forgot Password."
            return
        }
        viewModelScope.launch {
            container.authRepository.sendPasswordReset(email)
            _passwordResetStatus.value = "✅ If $email has an account, a password reset link has been sent to it."
        }
    }

    fun dismissPasswordResetStatus() { _passwordResetStatus.value = null }

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
            val lockedId = if (s.isOfficeStaff) _myLinkedWorkerId.value else null
            val result = container.attendanceEngine.mark(
                dir = dir,
                worker = worker,
                isAdmin = s.isAdmin,
                currentEmail = s.email,
                sites = sites.value,
                todayAttendance = todayAttendance.value,
                isOnline = NetworkStatus.isOnline(ctx),
                lockedWorkerId = lockedId,
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

    fun markLeave(workerId: String, fromDate: String, toDate: String, reason: String?) {
        viewModelScope.launch {
            val worker = workers.value.find { it.id == workerId }
            if (worker == null) { setStatus("leaveStatus", "❌ Worker ID not found."); return@launch }
            if (fromDate.isBlank()) { setStatus("leaveStatus", "❌ Enter a From date."); return@launch }
            try {
                container.leaveRepository.add(
                    Leave(
                        workerId = workerId, site = worker.site.orEmpty(), fromDate = fromDate,
                        toDate = toDate.ifBlank { fromDate }, reason = reason, markedBy = session.value.email,
                        ts = DateUtils.nowIso(),
                    )
                )
                setStatus("leaveStatus", "✅ Leave marked for ${worker.name}.")
            } catch (e: Throwable) {
                setStatus("leaveStatus", "❌ Failed: ${e.message ?: e::class.simpleName}")
            }
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

    suspend fun submitLeaveApplication(workerId: String, fromDate: String, toDate: String, reason: String?) {
        if (fromDate.isBlank()) { setStatus("myLeaveStatus", "❌ Enter a From date."); return }
        val worker = workers.value.find { it.id == workerId.trim() }
        container.leaveRepository.add(
            Leave(
                workerId = workerId.trim(), site = worker?.site.orEmpty(), fromDate = fromDate,
                toDate = toDate.ifBlank { fromDate }, reason = reason, markedBy = session.value.email,
                ts = DateUtils.nowIso(), status = "pending", requestedBy = session.value.email,
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

    /** Admin-only broadcast: saves the announcement (guaranteed in-app banner for everyone) and
     * best-effort pushes it to every device that has opted into notifications. */
    fun sendAnnouncement(message: String) {
        val text = message.trim()
        if (text.isBlank()) { setStatus("announcementStatus", "❌ Enter a message."); return }
        viewModelScope.launch {
            setStatus("announcementStatus", "⏳ Sending…")
            try {
                container.announcementRepository.send(text, session.value.email, DateUtils.nowIso())
                setStatus("announcementStatus", "✅ Sent — every signed-in user will see it now.")
                try {
                    container.pushTokensRepository.allTokens().forEach { token ->
                        container.netlifyApi.sendPush(token, "SitePulse announcement", text, "#/")
                    }
                } catch (e: Throwable) {
                    // Push fan-out failed silently — the announcement itself is already saved and shown in-app.
                }
            } catch (e: Throwable) {
                setStatus("announcementStatus", "❌ Failed: ${e.message ?: e::class.simpleName}")
            }
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
        val outDir = File(getApplication<Application>().cacheDir, "reports").apply { mkdirs() }
        val workersSnapshot = workers.value
        val sitesSnapshot = sites.value
        // Apache POI's workbook writing is blocking CPU/disk work — keep it off the
        // Main/Compose dispatcher the caller is on.
        return withContext(Dispatchers.IO) {
            ReportEngine.generate(outDir, attendanceRows, workersSnapshot, sitesSnapshot, leaves, params)
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
