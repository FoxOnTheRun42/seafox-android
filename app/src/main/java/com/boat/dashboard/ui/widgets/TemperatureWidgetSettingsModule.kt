package com.seafox.nmea_dashboard.ui.widgets

import org.json.JSONArray
import org.json.JSONObject

const val TEMPERATURE_SENSOR_COUNT = 10

fun serializeTemperatureWidgetSettings(settings: TemperatureWidgetSettings): String {
    val sensorNames = normalizeTemperatureSensorNames(settings.sensors)
    return JSONObject().apply {
        put("unit", settings.unit.name)
        put(
            "sensors",
            JSONArray().apply {
                sensorNames.forEach { sensorName ->
                    put(sensorName)
                }
            }
        )
    }.toString()
}

fun parseTemperatureWidgetSettings(serialized: String?): TemperatureWidgetSettings {
    if (serialized.isNullOrBlank()) return TemperatureWidgetSettings()
    return try {
        val raw = JSONObject(serialized)
        val unitName = raw.optString("unit", TemperatureUnit.CELSIUS.name)
        val unit = runCatching { TemperatureUnit.valueOf(unitName) }.getOrElse { TemperatureUnit.CELSIUS }

        val sensorsJson = raw.optJSONArray("sensors")
        val sensorNames = sensorsJson?.let { jsonArray ->
            List(jsonArray.length()) { index ->
                jsonArray.optString(index, "").trim()
            }
        } ?: emptyList()

        TemperatureWidgetSettings(
            unit = unit,
            sensors = normalizeTemperatureSensorNames(sensorNames),
        )
    } catch (_: Exception) {
        TemperatureWidgetSettings()
    }
}

fun normalizeTemperatureSensorNames(
    raw: List<String>,
    count: Int = TEMPERATURE_SENSOR_COUNT,
): List<String> {
    val values = raw.map { it.ifBlank { "" }.trim() }
    val normalized = values.mapIndexed { index, value ->
        if (index >= count) "" else if (value.isBlank()) "Temperatursensor ${index + 1}" else value
    }.take(count).toMutableList()
    while (normalized.size < count) {
        normalized.add("Temperatursensor ${normalized.size + 1}")
    }
    return normalized
}

fun pickTemperatureSensorValue(
    telemetry: Map<String, Float>,
    index: Int,
    dataKeys: List<String> = emptyList(),
): Float? {
    val sensorNumber = index + 1
    val normalizedIndex = index.coerceAtLeast(0)
    if (normalizedIndex < dataKeys.size) {
        val directKey = dataKeys[normalizedIndex].trim()
        if (directKey.isNotBlank()) {
            pickValueOrNull(telemetry, listOf(directKey))
                ?.takeIf { it.isFinite() }
                ?.let { return it }
        }
    }

    val fallbackKeys = listOf(
        "temperature_sensor_$sensorNumber",
        "temp_sensor_$sensorNumber",
        "temperature_$sensorNumber",
        "temp${sensorNumber}",
        "temperature${sensorNumber}",
    )
    return pickValueOrNull(telemetry, fallbackKeys)?.takeIf { it.isFinite() }
}
