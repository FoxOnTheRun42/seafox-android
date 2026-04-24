package com.seafox.nmea_dashboard.ui.widgets

import org.json.JSONObject
import com.seafox.nmea_dashboard.data.AutopilotWidgetSettings
import com.seafox.nmea_dashboard.data.AutopilotGatewayBackend
import com.seafox.nmea_dashboard.data.AutopilotTargetDevice
import com.seafox.nmea_dashboard.data.DEFAULT_AUTOPILOT_COMMAND_TIMEOUT_MS

fun serializeAutopilotWidgetSettings(settings: AutopilotWidgetSettings): String {
    return JSONObject().apply {
        put("targetDevice", settings.targetDevice.name)
        put("gatewayBackend", settings.gatewayBackend.name)
        put("gatewayHost", settings.gatewayHost)
        put("gatewayPort", settings.gatewayPort)
        put("signalKAutopilotId", settings.signalKAutopilotId)
        put("rudderAverageSeconds", settings.rudderAverageSeconds)
        put("safetyGateArmed", settings.safetyGateArmed)
        put("commandTimeoutMs", settings.commandTimeoutMs)
        put("authToken", settings.authToken)
    }.toString()
}

fun parseAutopilotWidgetSettings(serialized: String?): AutopilotWidgetSettings {
    if (serialized.isNullOrBlank()) return AutopilotWidgetSettings()
    return try {
        val raw = JSONObject(serialized)
        val targetDeviceName = raw.optString("targetDevice", AutopilotTargetDevice.GARMIN.name)
        val gatewayBackendName = raw.optString("gatewayBackend", AutopilotGatewayBackend.SIGNALK_V2.name)

        val targetDevice = runCatching { AutopilotTargetDevice.valueOf(targetDeviceName) }
            .getOrElse { AutopilotTargetDevice.GARMIN }
        val gatewayBackend = runCatching { AutopilotGatewayBackend.valueOf(gatewayBackendName) }
            .getOrElse { AutopilotGatewayBackend.SIGNALK_V2 }

        val host = raw.optString("gatewayHost", "255.255.255.255").trim().ifBlank { "255.255.255.255" }
        val port = raw.optInt("gatewayPort", gatewayBackend.defaultPort).coerceIn(1, 65535)
        val signalKAutopilotId = raw.optString("signalKAutopilotId", "_default").trim().ifBlank { "_default" }
        val rudderAverageSeconds = raw.optInt("rudderAverageSeconds", 30).coerceIn(5, 300)
        val commandTimeoutMs = raw.optLong(
            "commandTimeoutMs",
            DEFAULT_AUTOPILOT_COMMAND_TIMEOUT_MS,
        ).coerceIn(1_000L, 30_000L)
        val authToken = raw.optString("authToken", "").trim()

        AutopilotWidgetSettings(
            targetDevice = targetDevice,
            gatewayBackend = gatewayBackend,
            gatewayHost = host,
            gatewayPort = port,
            signalKAutopilotId = signalKAutopilotId,
            rudderAverageSeconds = rudderAverageSeconds,
            safetyGateArmed = raw.optBoolean("safetyGateArmed", false),
            commandTimeoutMs = commandTimeoutMs,
            authToken = authToken,
        )
    } catch (_: Exception) {
        AutopilotWidgetSettings()
    }
}
