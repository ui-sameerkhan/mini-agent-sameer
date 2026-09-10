package com.ktc.sitepulse

import android.content.Context
import com.ktc.sitepulse.data.repo.AnnouncementRepository
import com.ktc.sitepulse.data.repo.AppVersionRepository
import com.ktc.sitepulse.data.repo.ArrivalRequestRepository
import com.ktc.sitepulse.data.repo.AttendanceRepository
import com.ktc.sitepulse.data.repo.AuthRepository
import com.ktc.sitepulse.data.repo.BiometricCheckRepository
import com.ktc.sitepulse.data.repo.BlockedRepository
import com.ktc.sitepulse.data.repo.HolidayRepository
import com.ktc.sitepulse.data.repo.LeaveRepository
import com.ktc.sitepulse.data.repo.LocationProvider
import com.ktc.sitepulse.data.repo.NetlifyApi
import com.ktc.sitepulse.data.repo.PushTokensRepository
import com.ktc.sitepulse.data.repo.SettingsRepository
import com.ktc.sitepulse.data.repo.SitesRepository
import com.ktc.sitepulse.data.repo.StaffWorkerLinkRepository
import com.ktc.sitepulse.data.repo.TimekeeperRepository
import com.ktc.sitepulse.data.repo.TransferRequestRepository
import com.ktc.sitepulse.data.repo.UserRepository
import com.ktc.sitepulse.data.repo.WifiProvider
import com.ktc.sitepulse.data.repo.WorkersRepository
import com.ktc.sitepulse.domain.AttendanceEngine

/** Hand-rolled singleton container (no DI framework) — small enough app surface not to need one. */
class AppContainer(context: Context) {
    val authRepository = AuthRepository()
    val workersRepository = WorkersRepository()
    val sitesRepository = SitesRepository()
    val attendanceRepository = AttendanceRepository()
    val blockedRepository = BlockedRepository()
    val biometricCheckRepository = BiometricCheckRepository()
    val arrivalRequestRepository = ArrivalRequestRepository()
    val transferRequestRepository = TransferRequestRepository()
    val leaveRepository = LeaveRepository()
    val holidayRepository = HolidayRepository()
    val timekeeperRepository = TimekeeperRepository()
    val userRepository = UserRepository()
    val staffWorkerLinkRepository = StaffWorkerLinkRepository()
    val settingsRepository = SettingsRepository()
    val announcementRepository = AnnouncementRepository()
    val pushTokensRepository = PushTokensRepository()
    val appVersionRepository = AppVersionRepository()
    val netlifyApi = NetlifyApi()
    val locationProvider = LocationProvider(context)
    val wifiProvider = WifiProvider(context)
    val attendanceEngine = AttendanceEngine(attendanceRepository, blockedRepository, locationProvider, wifiProvider)

    companion object {
        @Volatile private var instance: AppContainer? = null
        fun get(context: Context): AppContainer =
            instance ?: synchronized(this) {
                instance ?: AppContainer(context.applicationContext).also { instance = it }
            }
    }
}
