package com.seafox.nmea_dashboard.ui.widgets

import org.json.JSONObject

val anchorWatchMetricChainStrengths = listOf(
    AnchorWatchChainStrength("6", 18.5f),
    AnchorWatchChainStrength("8", 24f),
    AnchorWatchChainStrength("10", 30f),
    AnchorWatchChainStrength("12", 36f),
    AnchorWatchChainStrength("14", 42f),
)

val anchorWatchImperialChainStrengths = listOf(
    AnchorWatchChainStrength("1/4\"", 21f),
    AnchorWatchChainStrength("5/16\"", 33f),
    AnchorWatchChainStrength("3/8\"", 38f),
    AnchorWatchChainStrength("7/16\"", 43f),
    AnchorWatchChainStrength("1/2\"", 48f),
)

fun anchorWatchChainStrengthOptions(unit: AnchorWatchChainUnit): List<AnchorWatchChainStrength> {
    return if (unit == AnchorWatchChainUnit.METRIC) anchorWatchMetricChainStrengths else anchorWatchImperialChainStrengths
}

fun anchorWatchNormalizeChainStrengthLabel(
    unit: AnchorWatchChainUnit,
    label: String,
): String {
    return anchorWatchChainStrengthOptions(unit).firstOrNull { it.label == label }?.label
        ?: anchorWatchChainStrengthOptions(unit).firstOrNull()?.label
        ?: ""
}

fun serializeAnchorWatchWidgetSettings(settings: AnchorWatchWidgetSettings): String {
    return JSONObject().apply {
        put("monitoringEnabled", settings.monitoringEnabled)
        put("maxDeviationMeters", settings.maxDeviationMeters)
        put("maxDeviationPercent", settings.maxDeviationPercent)
        put("chainSensorEnabled", settings.chainSensorEnabled)
        put("chainLengthCalibrationEnabled", settings.chainLengthCalibrationEnabled)
        put("calibratedChainLengthMeters", settings.calibratedChainLengthMeters)
        put("maxDeviationUnit", settings.maxDeviationUnit.name)
        put("chainSignalLength", settings.chainSignalLength)
        put("chainCalibrationUnit", settings.chainCalibrationUnit.name)
        put("chainStrengthLabel", settings.chainStrengthLabel)
        put("chainPockets", settings.chainPockets)
        put("ringCutoutStartAngleDeg", settings.ringCutoutStartAngleDeg)
        put("ringCutoutSweepAngleDeg", settings.ringCutoutSweepAngleDeg)
        put("alarmToneIndex", settings.alarmToneIndex)
    }.toString()
}

fun parseAnchorWatchWidgetSettings(serialized: String?): AnchorWatchWidgetSettings {
    if (serialized.isNullOrBlank()) return AnchorWatchWidgetSettings()
    return try {
        val raw = JSONObject(serialized)
        val monitoringEnabled = raw.optBoolean("monitoringEnabled", false)
        val chainSensorEnabled = raw.optBoolean("chainSensorEnabled", true)
        val chainLengthCalibrationEnabled = raw.optBoolean("chainLengthCalibrationEnabled", false)
        val calibratedChainLengthRaw = raw.optDouble(
            "calibratedChainLengthMeters",
            DEFAULT_ANCHOR_WATCH_CHAIN_LENGTH_METERS.toDouble(),
        ).toFloat().coerceIn(0f, 5000f)
        val maxDeviationPercent = if (raw.has("maxDeviationPercent")) {
            raw.optDouble("maxDeviationPercent", DEFAULT_ANCHOR_WATCH_TOLERANCE_PERCENT.toDouble())
                .toFloat()
        } else {
            raw.optDouble("maxDeviationMeters", DEFAULT_ANCHOR_WATCH_DEVIATION_METERS.toDouble()).toFloat()
        }
        val ringCutoutStartAngleDeg = raw.optDouble(
            "ringCutoutStartAngleDeg",
            0.0,
        ).toFloat()
        val ringCutoutSweepAngleDeg = raw.optDouble(
            "ringCutoutSweepAngleDeg",
            360.0,
        ).toFloat()
        val maxDeviationMeters = raw.optDouble(
            "maxDeviationMeters",
            DEFAULT_ANCHOR_WATCH_DEVIATION_METERS.toDouble(),
        ).toFloat().coerceIn(0.5f, 50f)
        val maxDeviationUnit = runCatching {
            AnchorWatchDistanceUnit.valueOf(raw.optString("maxDeviationUnit", AnchorWatchDistanceUnit.METERS.name))
        }.getOrElse { AnchorWatchDistanceUnit.METERS }

        val chainSignalLengthRaw = raw.optDouble("chainSignalLength", 30.0).toFloat().coerceIn(0.1f, 5000f)
        val chainCalibrationUnit = runCatching {
            AnchorWatchChainUnit.valueOf(raw.optString("chainCalibrationUnit", AnchorWatchChainUnit.METRIC.name))
        }.getOrElse { AnchorWatchChainUnit.METRIC }

        val chainStrengthLabel = raw.optString("chainStrengthLabel", "")
        val chainPockets = raw.optInt("chainPockets", 1)
        val alarmToneIndex = raw.optInt("alarmToneIndex", 0)

        val hasNewChainConfig = raw.has("chainCalibrationUnit") &&
            raw.has("chainStrengthLabel") &&
            raw.has("chainPockets")

        val legacyChainSignalLengthMm = when (maxDeviationUnit) {
            AnchorWatchDistanceUnit.METERS -> chainSignalLengthRaw * 10f
            AnchorWatchDistanceUnit.FEET -> chainSignalLengthRaw * 304.8f
        }
        val chainSignalLengthMm = if (hasNewChainConfig) {
            chainSignalLengthRaw
        } else {
            legacyChainSignalLengthMm
        }

        val chainPocketsFinal = chainPockets.coerceIn(1, 999)
        val chainSignalLengthMmFinal = chainSignalLengthMm.coerceIn(0.1f, 5000f)
        val normalizedStrengthLabel = anchorWatchNormalizeChainStrengthLabel(
            chainCalibrationUnit,
            chainStrengthLabel,
        )

        AnchorWatchWidgetSettings(
            monitoringEnabled = monitoringEnabled,
            chainSensorEnabled = chainSensorEnabled,
            chainLengthCalibrationEnabled = chainLengthCalibrationEnabled,
            calibratedChainLengthMeters = calibratedChainLengthRaw,
            maxDeviationMeters = maxDeviationMeters.coerceIn(0.5f, 50f),
            maxDeviationPercent = maxDeviationPercent.coerceIn(0f, 30f),
            maxDeviationUnit = maxDeviationUnit,
            chainSignalLength = chainSignalLengthMmFinal,
            chainCalibrationUnit = chainCalibrationUnit,
            chainStrengthLabel = normalizedStrengthLabel,
            chainPockets = chainPocketsFinal,
            ringCutoutStartAngleDeg = if (ringCutoutStartAngleDeg.isFinite()) ringCutoutStartAngleDeg else 0f,
            ringCutoutSweepAngleDeg = if (ringCutoutSweepAngleDeg.isFinite()) ringCutoutSweepAngleDeg else 360f,
            alarmToneIndex = alarmToneIndex.coerceIn(0, ECHO_SOUNDER_TONES.lastIndex),
        )
    } catch (_: Exception) {
        AnchorWatchWidgetSettings()
    }
}
