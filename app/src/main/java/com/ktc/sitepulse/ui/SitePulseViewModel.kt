package com.ktc.sitepulse.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.messaging.FirebaseMessaging
import com.ktc.sitepulse.AppContainer
import com.ktc.sitepulse.Constants
import com.ktc.sitepulse.data.model.ArrivalRequest
import com.ktc.sitepulse.data.model.Attendance
import com.ktc.sitepulse.data.model.Blocked
import com.ktc.sitepulse.data.model.Leave
import com.ktc.sitepulse.data.model.Site
import com.ktc.sitepulse.data.model.Worker
import com.ktc.sitepulse.data.repo.SessionState
import com.ktc.sitepulse.domain.DateUtils
import com.ktc.sitepulse.domain.MarkDirection
import com.ktc.sitepulse.domain.MarkResult
import com.ktc.sitepulse.domain.ParsedImport
import com.ktc.sitepulse.domain.RawTable
import com.ktc.sitepulse.domain.ReportEngine
import com.ktc.sitepulse.domain.SpreadsheetReader
import com.ktc.sitepulse.domain.WorkersImport
import com.ktc.sitepulse.util.NetworkStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
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

class SitePulseViewModel(application: Application) : AndroidViewModel(application) {
    private val container = AppContainer.get(application)

    val session: StateFlow<SessionState> = container.authRepository.sessionState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SessionState(null))

    val workers: StateFlow<List<Worker>> = container.workersRepository.liveWorkers()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sites: StateFlow<List<Site>> = container.sitesRepository.liveSites()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayAttendance: StateFlow<List<Attendance>> = container.attendanceRepository.liveToday()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val blocked: StateFlow<List<Blocked>> = container.blockedRepository.liveLast14Days()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingArrivals: StateFlow<List<ArrivalRequest>> = container.arrivalRequestRepository.livePending()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    fun logout() = container.authRepository.logout()

    // ---- Check-in / out ----

    fun mark(dir: MarkDirection, worker: Worker) {
        if (_markInFlight.value) return
        _markInFlight.value = true
        _markResult.value = null
        viewModelScope.launch {
            val s = session.value
            val ctx = getApplication<Application>()
            val result = container.attendanceEngine.mark(
                dir = dir,
                worker = worker,
                isAdmin = s.isAdmin,
                currentEmail = s.email,
                sites = sites.value,
                todayAttendance = todayAttendance.value,
                isOnline = NetworkStatus.isOnline(ctx),
            )
            _markResult.value = result
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
            when (pending.kind) {
                "site" -> container.sitesRepository.deleteSite(pending.id)
                "worker" -> container.workersRepository.deleteWorker(pending.id)
                "allWorkers" -> container.workersRepository.deleteAll(workers.value.map { it.id }) { done, total ->
                    onProgress("Deleted $done / $total…")
                }
            }
            _pendingDelete.value = null
        }
    }

    fun cancelPendingDelete() { _pendingDelete.value = null }

    fun toggleWorkerStatus(worker: Worker) {
        viewModelScope.launch {
            if (worker.isLeft) {
                container.workersRepository.setStatus(worker.id, "active", null)
            } else {
                container.workersRepository.setStatus(worker.id, "left", DateUtils.todayStrUtc())
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
        viewModelScope.launch { commitImport(pending.kind, pending.records, pending.skippedNotFound, statusKeyFor(pending.kind)) }
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
            container.leaveRepository.add(
                Leave(
                    workerId = workerId, site = worker.site.orEmpty(), fromDate = fromDate,
                    toDate = toDate.ifBlank { fromDate }, reason = reason, markedBy = session.value.email,
                    ts = DateUtils.nowIso(),
                )
            )
            setStatus("leaveStatus", "✅ Leave marked for ${worker.name}.")
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
            val request = ArrivalRequest(
                site = site, workerId = workerId, name = finalName, designation = finalDesignation,
                requestedDate = date.ifBlank { DateUtils.todayStrUtc() }, requestedBy = session.value.email,
                status = "pending", ts = DateUtils.nowIso(),
            )
            container.arrivalRequestRepository.submit(request)
            setStatus("arrivalStatus", "✅ Sent for admin approval…")

            val token = container.settingsRepository.getAdminPushToken()
            if (!token.isNullOrBlank()) {
                container.netlifyApi.sendPush(token, "New arrival request", "$finalName (ID $workerId) for $site", "#/roster")
            }
            container.netlifyApi.sendEmail(
                Constants.NOTIFY_EMAILS,
                "New arrival: $finalName ($site)",
                arrivalEmailHtml(finalName, workerId, finalDesignation, site, request.requestedDate, request.requestedBy)
            )
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
        viewModelScope.launch {
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
        }
    }

    fun rejectArrival(request: ArrivalRequest) {
        viewModelScope.launch {
            container.arrivalRequestRepository.reject(request.docId, session.value.email, DateUtils.nowIso())
        }
    }

    // ---- Notifications ----

    fun enableNotifications() {
        viewModelScope.launch {
            setStatus("pushStatus", "⏳ Getting notification token…")
            try {
                val token = FirebaseMessaging.getInstance().token.await()
                container.settingsRepository.saveAdminPushToken(token, session.value.email, DateUtils.nowIso())
                setStatus("pushStatus", "✅ Notifications enabled on this device.")
            } catch (e: Exception) {
                setStatus("pushStatus", "❌ ${e.message}")
            }
        }
    }

    // ---- Reports ----

    suspend fun attendanceForDate(date: String): List<Attendance> =
        if (date == DateUtils.todayStrUtc()) todayAttendance.value
        else container.attendanceRepository.getForDate(date)

    suspend fun generateReport(params: ReportEngine.Params): File {
        val attendanceRows = if (params.range == "day") {
            if (params.dateOrMonth == DateUtils.todayStrUtc()) todayAttendance.value
            else container.attendanceRepository.getForDate(params.dateOrMonth)
        } else {
            container.attendanceRepository.getForMonth(params.dateOrMonth)
        }
        val leaves = container.leaveRepository.all()
        val outDir = File(getApplication<Application>().cacheDir, "reports").apply { mkdirs() }
        return ReportEngine.generate(outDir, attendanceRows, workers.value, sites.value, leaves, params)
    }
}
