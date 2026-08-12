package com.ktc.sitepulse.util

import android.content.Context
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.time.Instant

/**
 * Minimal on-device crash log: there's no logcat/adb access when the only
 * test device is a phone with no dev tooling attached, so any uncaught
 * crash needs to leave a trace INSIDE the app itself to be diagnosable.
 * Writes the last crash to a file and hands off to the previous default
 * handler afterwards (so the OS still terminates the process normally).
 */
object CrashReporter {
    private const val FILE_NAME = "last_crash.txt"

    fun install(context: Context) {
        val appContext = context.applicationContext
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                File(appContext.filesDir, FILE_NAME).writeText(
                    "Crashed at ${Instant.now()} on thread ${thread.name}\n\n$sw"
                )
            } catch (_: Throwable) {
                // Never let crash reporting itself throw.
            }
            previousHandler?.uncaughtException(thread, throwable)
        }
    }

    fun lastCrash(context: Context): String? {
        val file = File(context.applicationContext.filesDir, FILE_NAME)
        return if (file.exists()) file.readText() else null
    }

    fun clear(context: Context) {
        File(context.applicationContext.filesDir, FILE_NAME).delete()
    }
}
