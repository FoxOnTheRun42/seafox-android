package com.seafox.nmea_dashboard.ui.widgets

import org.json.JSONObject

fun serializeAisWidgetSettings(settings: AisWidgetSettings): String {
    val perTargetTimeouts = JSONObject().apply {
        settings.targetVisibilityMinutesByMmsi.forEach { (mmsi, timeoutMinutes) ->
            put(mmsi, timeoutMinutes.coerceIn(1, 5))
        }
    }
    return JSONObject().apply {
        put("cpaAlarmDistanceNm", settings.cpaAlarmDistanceNm)
        put("cpaAlarmMinutes", settings.cpaAlarmMinutes)
        put("targetVisibilityMinutes", settings.targetVisibilityMinutes)
        put("targetVisibilityMinutesByMmsi", perTargetTimeouts)
        put("displayRangeNm", settings.displayRangeNm)
        put("collisionAlarmsMuted", settings.collisionAlarmsMuted)
        put("northUp", settings.northUp)
        put("fontSizeOffsetSp", settings.fontSizeOffsetSp)
    }.toString()
}

fun parseAisWidgetSettings(serialized: String?): AisWidgetSettings {
    if (serialized.isNullOrBlank()) return AisWidgetSettings()
    return try {
        val raw = JSONObject(serialized)
        AisWidgetSettings(
            cpaAlarmDistanceNm = raw.optDouble("cpaAlarmDistanceNm", 1.0).toFloat(),
            cpaAlarmMinutes = raw.optDouble("cpaAlarmMinutes", 5.0).toFloat(),
            targetVisibilityMinutes = raw.optInt("targetVisibilityMinutes", 5).coerceIn(1, 5),
            targetVisibilityMinutesByMmsi = raw.optJSONObject("targetVisibilityMinutesByMmsi")?.let { objectByMmsi ->
                buildMap<String, Int> {
                    objectByMmsi.keys().forEach { key ->
                        put(key, objectByMmsi.optInt(key, 5).coerceIn(1, 5))
                    }
                }
            } ?: emptyMap(),
            displayRangeNm = raw.optDouble("displayRangeNm", 10.0).toFloat(),
            collisionAlarmsMuted = raw.optBoolean("collisionAlarmsMuted", false),
            northUp = raw.optBoolean("northUp", true),
            fontSizeOffsetSp = raw.optInt("fontSizeOffsetSp", 0),
        )
    } catch (_: Exception) {
        AisWidgetSettings()
    }
}
