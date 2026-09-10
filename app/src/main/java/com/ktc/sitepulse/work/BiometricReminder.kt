package com.ktc.sitepulse.work

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.ktc.sitepulse.MainActivity
import com.ktc.sitepulse.R
import com.ktc.sitepulse.data.model.BiometricCheckLog
import com.ktc.sitepulse.data.repo.BiometricCheckRepository
import com.ktc.sitepulse.domain.DateUtils
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/**
 * A once-a-day nudge to run the ERP biometric cross-check.
 *
 * The check is a manual daily task, and manual daily tasks stop happening — usually quietly,
 * a few weeks in, with nobody noticing until someone asks for the evidence. The in-app banner
 * covers the case where somebody opens the app; this covers the case where nobody does.
 *
 * It deliberately says nothing if the day has already been verified. A reminder that fires
 * whether or not the work is done is one people learn to swipe away without reading, and then
 * it stops working on the day it matters.
 */
class BiometricReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // Always line up tomorrow's, whatever happens today — a reminder chain that breaks on
        // one bad day is worse than none, because nobody notices it stopped.
        BiometricReminder.schedule(applicationContext)

        val scope = BiometricReminder.storedScope(applicationContext) ?: return Result.success()
        if (FirebaseAuth.getInstance().currentUser == null) return Result.success()

        val today = DateUtils.todayStrUtc()
        val alreadyDone = try {
            BiometricCheckRepository().get(today, scope) != null
        } catch (e: Throwable) {
            // Offline, or the rules refused the read. Better to stay quiet than to insist the
            // check is outstanding when it may well have been done.
            return Result.success()
        }
        if (alreadyDone) return Result.success()

        notify(
            title = "Biometric check not done today",
            body = "Upload today's ERP 8127 report to verify attendance against the biometric.",
        )
        return Result.success()
    }

    private fun notify(title: String, body: String) {
        val canPost = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                applicationContext, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        if (!canPost) return

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            applicationContext, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(
            applicationContext, applicationContext.getString(R.string.notification_channel_id)
        )
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pending)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(applicationContext)
            .notify(BiometricReminder.NOTIFICATION_ID, notification)
    }
}

object BiometricReminder {

    private const val WORK_NAME = "biometric-daily-reminder"
    private const val PREFS = "sitepulse.reminder"
    private const val KEY_SCOPE = "scope"
    internal const val NOTIFICATION_ID = 9127 // stable, so a new reminder replaces yesterday's

    /** Local hour the reminder fires. Mid-morning: the previous day's ERP export exists by then. */
    private const val HOUR = 9

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /**
     * Turn the reminder on for an account that can run the check, storing the site scope the
     * worker should look up. Called on sign-in.
     */
    fun enableFor(context: Context, scopeKey: String) {
        prefs(context).edit().putString(KEY_SCOPE, scopeKey).apply()
        schedule(context)
    }

    /** Turn it off — sign-out, or an account that has no business running the check. */
    fun disable(context: Context) {
        prefs(context).edit().remove(KEY_SCOPE).apply()
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    internal fun storedScope(context: Context): String? =
        prefs(context).getString(KEY_SCOPE, null)

    /**
     * Queue the next firing at the next [HOUR] local.
     *
     * A chain of one-shot requests rather than PeriodicWorkRequest: a periodic worker's interval
     * drifts from whenever it was first enqueued, so it would fire at whatever time the user
     * happened to sign in. Re-scheduling from each run keeps it pinned to the hour.
     *
     * REPLACE, so signing in twice does not queue two reminders.
     */
    fun schedule(context: Context) {
        val now = LocalDateTime.now()
        var next = LocalDateTime.of(LocalDate.now(), LocalTime.of(HOUR, 0))
        if (!next.isAfter(now)) next = next.plusDays(1)
        val delayMs = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() -
            System.currentTimeMillis()

        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<BiometricReminderWorker>()
                .setInitialDelay(delayMs.coerceAtLeast(0), TimeUnit.MILLISECONDS)
                .build(),
        )
    }

    /** Convenience for the caller that has a session but not the scope-key rules. */
    fun scopeKey(hasAllSites: Boolean, assignedSites: List<String>): String =
        BiometricCheckLog.scopeKeyFor(hasAllSites, assignedSites)
}
