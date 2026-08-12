package com.ktc.sitepulse

import android.content.Context
import com.ktc.sitepulse.data.repo.ArrivalRequestRepository
import com.ktc.sitepulse.data.repo.AttendanceRepository
import com.ktc.sitepulse.data.repo.AuthRepository
import com.ktc.sitepulse.data.repo.BlockedRepository
import com.ktc.sitepulse.data.repo.LeaveRepository
import com.ktc.sitepulse.data.repo.LocationProvider
import com.ktc.sitepulse.data.repo.NetlifyApi
import com.ktc.sitepulse.data.repo.SettingsRepository
import com.ktc.sitepulse.data.repo.SitesRepository
import com.ktc.sitepulse.data.repo.WorkersRepository
import com.ktc.sitepulse.domain.AttendanceEngine

/** Hand-rolled singleton container (no DI framework) — small enough app surface not to need one. */
class AppContainer(context: Context) {
    val authRepository = AuthRepository()
    val workersRepository = WorkersRepository()
    val sitesRepository = SitesRepository()
    val attendanceRepository = AttendanceRepository()
    val blockedRepository = BlockedRepository()
    val arrivalRequestRepository = ArrivalRequestRepository()
    val leaveRepository = LeaveRepository()
    val settingsRepository = SettingsRepository()
    val netlifyApi = NetlifyApi()
    val locationProvider = LocationProvider(context)
    val attendanceEngine = AttendanceEngine(attendanceRepository, blockedRepository, locationProvider)

    companion object {
        @Volatile private var instance: AppContainer? = null
        fun get(context: Context): AppContainer =
            instance ?: synchronized(this) {
                instance ?: AppContainer(context.applicationContext).also { instance = it }
            }
    }
}
