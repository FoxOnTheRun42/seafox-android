package com.seafox.nmea_dashboard.ui.widgets

import org.json.JSONObject

fun serializeEchosounderWidgetSettings(settings: EchosounderWidgetSettings): String {
    return JSONObject().apply {
        put("minDepthMeters", settings.minDepthMeters)
        put("dynamicChangeRateMps", settings.dynamicChangeRateMps)
        put("depthUnit", settings.depthUnit.name)
        put("alarmToneIndex", settings.alarmToneIndex)
    }.toString()
}

fun parseEchosounderWidgetSettings(serialized: String?): EchosounderWidgetSettings {
    if (serialized.isNullOrBlank()) return EchosounderWidgetSettings()
    return try {
        val raw = JSONObject(serialized)
        val depthUnitName = raw.optString("depthUnit", EchosounderDepthUnit.METERS.name)
        val depthUnit = runCatching { EchosounderDepthUnit.valueOf(depthUnitName) }
            .getOrElse { EchosounderDepthUnit.METERS }

        EchosounderWidgetSettings(
            minDepthMeters = raw.optDouble("minDepthMeters", 2.0).toFloat(),
            dynamicChangeRateMps = raw.optDouble("dynamicChangeRateMps", 0.8).toFloat(),
            depthUnit = depthUnit,
            alarmToneIndex = raw.optInt("alarmToneIndex", 0),
        )
    } catch (_: Exception) {
        EchosounderWidgetSettings()
    }
}
