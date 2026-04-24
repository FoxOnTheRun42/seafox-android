package com.seafox.nmea_dashboard.ui.widgets

import org.json.JSONObject

fun serializeLogWidgetSettings(settings: LogWidgetSettings): String {
    return JSONObject().apply {
        put("periodMinutes", settings.periodMinutes)
        put("speedSource", settings.speedSource.name)
    }.toString()
}

fun parseLogWidgetSettings(serialized: String?): LogWidgetSettings {
    if (serialized.isNullOrBlank()) return LogWidgetSettings()
    return try {
        val raw = JSONObject(serialized)
        val speedSource = runCatching {
            LogWidgetSpeedSource.valueOf(raw.optString("speedSource"))
        }.getOrElse { LogWidgetSpeedSource.GPS_SOG }
        LogWidgetSettings(
            periodMinutes = raw.optInt("periodMinutes", 10).coerceIn(1, 120),
            speedSource = speedSource,
        )
    } catch (_: Exception) {
        LogWidgetSettings()
    }
}
