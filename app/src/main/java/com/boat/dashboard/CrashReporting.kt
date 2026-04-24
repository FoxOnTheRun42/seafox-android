package com.seafox.nmea_dashboard

import android.content.Context
import android.os.Build
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.system.exitProcess

data class CrashReportPayload(
    val appVersionName: String,
    val appVersionCode: Int,
    val androidSdk: Int,
    val createdAtEpochMs: Long,
    val threadName: String,
    val throwableClass: String,
    val throwableMessage: String,
    val stackTrace: String,
)

object CrashReportFormatter {
    fun format(payload: CrashReportPayload): String {
        return buildString {
            appendLine("seaFOX crash report")
            appendLine("createdAt=${formatInstant(payload.createdAtEpochMs)}")
            appendLine("appVersionName=${payload.appVersionName.ifBlank { "unknown" }}")
            appendLine("appVersionCode=${payload.appVersionCode.coerceAtLeast(0)}")
            appendLine("androidSdk=${payload.androidSdk.coerceAtLeast(0)}")
            appendLine("thread=${payload.threadName.ifBlank { "unknown" }}")
            appendLine("throwable=${payload.throwableClass.ifBlank { "unknown" }}")
            appendLine("message=${payload.throwableMessage.ifBlank { "<none>" }}")
            appendLine()
            appendLine(payload.stackTrace.ifBlank { "<no stacktrace>" })
        }
    }

    private fun formatInstant(epochMs: Long): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        formatter.timeZone = TimeZone.getTimeZone("UTC")
        return formatter.format(Date(epochMs.coerceAtLeast(0L)))
    }
}

class LocalCrashReporter(
    private val context: Context,
    private val previousHandler: Thread.UncaughtExceptionHandler?,
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        runCatching { writeCrashReport(thread, throwable) }
        previousHandler?.uncaughtException(thread, throwable) ?: exitProcess(2)
    }

    private fun writeCrashReport(thread: Thread, throwable: Throwable) {
        val directory = File(context.filesDir, CRASH_REPORT_DIR)
        if (!directory.exists()) {
            directory.mkdirs()
        }
        if (!directory.isDirectory) return

        val createdAt = System.currentTimeMillis()
        val payload = CrashReportPayload(
            appVersionName = BuildConfig.VERSION_NAME,
            appVersionCode = BuildConfig.VERSION_CODE,
            androidSdk = Build.VERSION.SDK_INT,
            createdAtEpochMs = createdAt,
            threadName = thread.name,
            throwableClass = throwable::class.java.name,
            throwableMessage = throwable.message.orEmpty(),
            stackTrace = throwable.stackTraceToString(),
        )
        File(directory, "seafox-crash-$createdAt.txt").writeText(CrashReportFormatter.format(payload))
    }

    companion object {
        private const val CRASH_REPORT_DIR = "crash-reports"

        fun install(context: Context) {
            val current = Thread.getDefaultUncaughtExceptionHandler()
            if (current is LocalCrashReporter) return
            Thread.setDefaultUncaughtExceptionHandler(
                LocalCrashReporter(context.applicationContext, current),
            )
        }
    }
}
