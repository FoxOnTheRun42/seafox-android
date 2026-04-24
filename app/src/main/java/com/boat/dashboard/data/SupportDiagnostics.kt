package com.seafox.nmea_dashboard.data

import java.io.File

data class SupportDiagnosticReport(
    val appVersionName: String,
    val androidSdk: Int,
    val createdAtEpochMs: Long,
    val backupPrivacyMode: BackupPrivacyMode,
    val bootAutostartEnabled: Boolean,
    val simulationEnabled: Boolean,
    val nmeaRouterProtocol: NmeaRouterProtocol,
    val nmeaRouterHost: String,
    val udpPort: Int,
    val pageCount: Int,
    val widgetCount: Int,
    val hasActiveRoute: Boolean,
    val hasMobMarker: Boolean,
    val detectedSourceCount: Int,
)

object SupportDiagnosticsBuilder {
    fun build(
        state: DashboardState,
        appVersionName: String,
        androidSdk: Int,
        createdAtEpochMs: Long = System.currentTimeMillis(),
        includeSensitive: Boolean = false,
    ): SupportDiagnosticReport {
        return SupportDiagnosticReport(
            appVersionName = appVersionName.ifBlank { "unknown" },
            androidSdk = androidSdk.coerceAtLeast(0),
            createdAtEpochMs = createdAtEpochMs,
            backupPrivacyMode = state.backupPrivacyMode,
            bootAutostartEnabled = state.bootAutostartEnabled,
            simulationEnabled = state.simulationEnabled,
            nmeaRouterProtocol = state.nmeaRouterProtocol,
            nmeaRouterHost = state.nmeaRouterHost.redactIfSensitive(
                key = "router_host",
                includeSensitive = includeSensitive,
            ),
            udpPort = state.udpPort.coerceIn(1, 65535),
            pageCount = state.pages.size,
            widgetCount = state.pages.sumOf { page -> page.widgets.size },
            hasActiveRoute = state.activeRoute != null,
            hasMobMarker = state.mobPosition != null,
            detectedSourceCount = state.detectedNmeaSources.size,
        )
    }

    private fun String.redactIfSensitive(key: String, includeSensitive: Boolean): String {
        if (includeSensitive || !BackupPrivacyPolicy.treatsAsSensitive(key)) return this
        return if (isBlank()) "" else "<redacted>"
    }
}

object SupportDiagnosticsJson {
    fun toMap(report: SupportDiagnosticReport): Map<String, Any> {
        return linkedMapOf(
            "appVersionName" to report.appVersionName,
            "androidSdk" to report.androidSdk,
            "createdAtEpochMs" to report.createdAtEpochMs,
            "backupPrivacyMode" to report.backupPrivacyMode.name,
            "bootAutostartEnabled" to report.bootAutostartEnabled,
            "simulationEnabled" to report.simulationEnabled,
            "nmeaRouterProtocol" to report.nmeaRouterProtocol.name,
            "nmeaRouterHost" to report.nmeaRouterHost,
            "udpPort" to report.udpPort,
            "pageCount" to report.pageCount,
            "widgetCount" to report.widgetCount,
            "hasActiveRoute" to report.hasActiveRoute,
            "hasMobMarker" to report.hasMobMarker,
            "detectedSourceCount" to report.detectedSourceCount,
        )
    }

    fun toJsonString(report: SupportDiagnosticReport, indentSpaces: Int = 2): String {
        val fields = toMap(report)
        val indent = indentSpaces.coerceIn(0, 8)
        if (indent == 0) {
            return fields.entries.joinToString(prefix = "{", postfix = "}") { (key, value) ->
                "\"${escapeJson(key)}\":${formatJsonValue(value)}"
            }
        }

        val padding = " ".repeat(indent)
        return fields.entries.joinToString(prefix = "{\n", separator = ",\n", postfix = "\n}") { (key, value) ->
            "$padding\"${escapeJson(key)}\": ${formatJsonValue(value)}"
        }
    }

    private fun formatJsonValue(value: Any): String {
        return when (value) {
            is Boolean -> value.toString()
            is Number -> value.toString()
            else -> "\"${escapeJson(value.toString())}\""
        }
    }

    private fun escapeJson(value: String): String {
        return buildString(value.length) {
            value.forEach { char ->
                when (char) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(char)
                }
            }
        }
    }
}

object SupportDiagnosticsExporter {
    fun writeReport(directory: File, report: SupportDiagnosticReport): File {
        if (!directory.exists()) {
            require(directory.mkdirs()) { "Could not create diagnostics directory: ${directory.absolutePath}" }
        }
        require(directory.isDirectory) { "Diagnostics target is not a directory: ${directory.absolutePath}" }

        val file = File(directory, "seafox-diagnostics-${report.createdAtEpochMs.coerceAtLeast(0L)}.json")
        file.writeText(SupportDiagnosticsJson.toJsonString(report) + "\n")
        return file
    }
}
