package com.seafox.nmea_dashboard.ui.widgets

import org.json.JSONObject

fun serializeBatteryWidgetSettings(settings: BatteryWidgetSettings): String {
    return JSONObject().apply {
        put("chemistry", settings.chemistry.name)
    }.toString()
}

fun parseBatteryWidgetSettings(serialized: String?): BatteryWidgetSettings {
    if (serialized.isNullOrBlank()) return BatteryWidgetSettings()
    return try {
        val raw = JSONObject(serialized)
        val chemistryName = raw.optString("chemistry", BatteryChemistry.LEAD_ACID.name)
        val chemistry = runCatching { BatteryChemistry.valueOf(chemistryName) }
            .getOrElse { BatteryChemistry.LEAD_ACID }
        BatteryWidgetSettings(chemistry = chemistry)
    } catch (_: Exception) {
        BatteryWidgetSettings()
    }
}
