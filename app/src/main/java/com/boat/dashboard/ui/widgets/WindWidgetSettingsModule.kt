package com.seafox.nmea_dashboard.ui.widgets

import org.json.JSONObject

fun serializeWindWidgetSettings(settings: WindWidgetSettings): String {
    return JSONObject().apply {
        put("showBoatDirection", settings.showBoatDirection)
        put("showNorthDirection", settings.showNorthDirection)
        put("showWindSpeed", settings.showWindSpeed)
        put("speedUnit", settings.speedUnit.name)
        put("tackingAngleDeg", settings.tackingAngleDeg)
        put("historyWindowMinutes", settings.historyWindowMinutes)
        put("minMaxUsesTrueWind", settings.minMaxUsesTrueWind)
    }.toString()
}

fun parseWindWidgetSettings(
    serialized: String?,
    defaultTackingAngle: Int = DEFAULT_TACKING_ANGLE_DEG,
): WindWidgetSettings {
    if (serialized.isNullOrBlank()) return WindWidgetSettings(tackingAngleDeg = defaultTackingAngle)
    return try {
        val raw = JSONObject(serialized)
        val tackingAngle = raw.optInt("tackingAngleDeg", defaultTackingAngle)
        val historyWindow = raw.optInt("historyWindowMinutes", 5)
        WindWidgetSettings(
            showBoatDirection = raw.optBoolean("showBoatDirection", true),
            showNorthDirection = raw.optBoolean("showNorthDirection", true),
            showWindSpeed = raw.optBoolean("showWindSpeed", true),
            speedUnit = runCatching {
                WindSpeedUnit.valueOf(raw.optString("speedUnit", WindSpeedUnit.KNOTS.name))
            }.getOrElse { WindSpeedUnit.KNOTS },
            tackingAngleDeg = tackingAngle.coerceIn(50, 140),
            historyWindowMinutes = historyWindow.coerceIn(5, 43200),
            minMaxUsesTrueWind = raw.optBoolean("minMaxUsesTrueWind", true),
        )
    } catch (_: Exception) {
        WindWidgetSettings(tackingAngleDeg = defaultTackingAngle)
    }
}
