package com.seafox.nmea_dashboard.ui.widgets

import com.seafox.nmea_dashboard.ui.widgets.chart.ChartProviderRegistry
import org.json.JSONObject

fun serializeSeaChartWidgetSettings(settings: SeaChartWidgetSettings): String {
    return JSONObject().apply {
        put("mapProvider", settings.mapProvider.name)
        put("speedSource", settings.speedSource.name)
        put("showAisOverlay", settings.showAisOverlay)
        put("showGribOverlay", settings.showGribOverlay)
        put("showOpenSeaMapOverlay", settings.showOpenSeaMapOverlay)
        put("showHeadingLine", settings.showHeadingLine)
        put("headingLineLengthNm", settings.headingLineLengthNm)
        put("showCogVector", settings.showCogVector)
        put("cogVectorMinutes", settings.cogVectorMinutes)
        put("showPredictor", settings.showPredictor)
        put("predictorMinutes", settings.predictorMinutes)
        put("predictorIntervalMinutes", settings.predictorIntervalMinutes)
        put("showPredictorLabels", settings.showPredictorLabels)
        put("showBoatIcon", settings.showBoatIcon)
        put("boatIconSizeDp", settings.boatIconSizeDp)
        put("showCourseLine", settings.showCourseLine)
        put("showRoadLayers", settings.showRoadLayers)
        put("showDepthLines", settings.showDepthLines)
        put("showContourLines", settings.showContourLines)
        put("showTrack", settings.showTrack)
        put("trackDurationMinutes", settings.trackDurationMinutes)
        put("trackRecordIntervalSeconds", settings.trackRecordIntervalSeconds)
        put("guardZoneEnabled", settings.guardZoneEnabled)
        put("guardZoneInnerNm", settings.guardZoneInnerNm)
        put("guardZoneOuterNm", settings.guardZoneOuterNm)
        put("guardZoneSectorStartDeg", settings.guardZoneSectorStartDeg)
        put("guardZoneSectorEndDeg", settings.guardZoneSectorEndDeg)
        put("showLaylines", settings.showLaylines)
        put("tackingAngleDeg", settings.tackingAngleDeg)
        put("laylineLengthNm", settings.laylineLengthNm)
        put("showSafetyContour", settings.showSafetyContour)
        put("safetyDepthMeters", settings.safetyDepthMeters)
        put("courseLineBearingDeg", settings.courseLineBearingDeg)
        put("courseLineDistanceNm", settings.courseLineDistanceNm)
    }.toString()
}

fun parseSeaChartWidgetSettings(serialized: String?): SeaChartWidgetSettings {
    if (serialized.isNullOrBlank()) return SeaChartWidgetSettings()
    return try {
        val raw = JSONObject(serialized)
        val providerName = raw.optString("mapProvider", SeaChartMapProvider.NOAA.name)
        val mapProvider = normalizeSeaChartMapProviderName(providerName)
        val speedSource = runCatching {
            SeaChartSpeedSource.valueOf(raw.optString("speedSource", SeaChartSpeedSource.GPS_SOG.name))
        }.getOrElse { SeaChartSpeedSource.GPS_SOG }

        val headingLineLengthNm = raw.optDouble("headingLineLengthNm", 0.5).toFloat().coerceIn(0.1f, 20f)
        val cogVectorMinutes = raw.optInt("cogVectorMinutes", 6).coerceIn(1, 60)
        val predictorMinutes = raw.optInt("predictorMinutes", 6).coerceIn(1, 60)
        val predictorIntervalMinutes = raw.optInt("predictorIntervalMinutes", 1).coerceIn(1, predictorMinutes)
        val boatIconSizeDp = raw.optInt("boatIconSizeDp", 24).coerceIn(12, 40)
        val trackDurationMinutes = raw.optInt("trackDurationMinutes", 60).coerceIn(1, 24 * 60)
        val trackRecordIntervalSeconds = raw.optInt("trackRecordIntervalSeconds", 10).coerceIn(1, 60)
        val guardZoneInnerNm = raw.optDouble("guardZoneInnerNm", 0.5).toFloat().coerceIn(0f, 10f)
        val guardZoneOuterNm = raw.optDouble("guardZoneOuterNm", 2.0).toFloat().coerceIn(guardZoneInnerNm, 20f)
        val guardZoneSectorStartDeg = wrap360(raw.optDouble("guardZoneSectorStartDeg", 0.0).toFloat())
        val guardZoneSectorEndDeg = raw.optDouble("guardZoneSectorEndDeg", 360.0).toFloat().coerceIn(0f, 360f)
        val tackingAngleDeg = raw.optInt("tackingAngleDeg", 45).coerceIn(20, 80)
        val laylineLengthNm = raw.optDouble("laylineLengthNm", 3.0).toFloat().coerceIn(0.1f, 20f)
        val safetyDepthMeters = raw.optDouble("safetyDepthMeters", 3.0).toFloat().coerceIn(0.5f, 50f)
        val bearing = wrap360(raw.optDouble("courseLineBearingDeg", 0.0).toFloat())
        val distanceNm = raw.optDouble("courseLineDistanceNm", 5.0).toFloat().coerceIn(0.1f, 40f)

        SeaChartWidgetSettings(
            mapProvider = mapProvider,
            speedSource = speedSource,
            showAisOverlay = raw.optBoolean("showAisOverlay", false),
            showGribOverlay = raw.optBoolean("showGribOverlay", false),
            showOpenSeaMapOverlay = raw.optBoolean("showOpenSeaMapOverlay", false),
            showHeadingLine = raw.optBoolean("showHeadingLine", true),
            headingLineLengthNm = headingLineLengthNm,
            showCogVector = raw.optBoolean("showCogVector", true),
            cogVectorMinutes = cogVectorMinutes,
            showPredictor = raw.optBoolean("showPredictor", true),
            predictorMinutes = predictorMinutes,
            predictorIntervalMinutes = predictorIntervalMinutes,
            showPredictorLabels = raw.optBoolean("showPredictorLabels", true),
            showBoatIcon = raw.optBoolean("showBoatIcon", true),
            boatIconSizeDp = boatIconSizeDp,
            showCourseLine = raw.optBoolean("showCourseLine", false),
            showRoadLayers = raw.optBoolean("showRoadLayers", true),
            showDepthLines = raw.optBoolean("showDepthLines", true),
            showContourLines = raw.optBoolean("showContourLines", true),
            showTrack = raw.optBoolean("showTrack", true),
            trackDurationMinutes = trackDurationMinutes,
            trackRecordIntervalSeconds = trackRecordIntervalSeconds,
            guardZoneEnabled = raw.optBoolean("guardZoneEnabled", false),
            guardZoneInnerNm = guardZoneInnerNm,
            guardZoneOuterNm = guardZoneOuterNm,
            guardZoneSectorStartDeg = guardZoneSectorStartDeg,
            guardZoneSectorEndDeg = guardZoneSectorEndDeg,
            showLaylines = raw.optBoolean("showLaylines", false),
            tackingAngleDeg = tackingAngleDeg,
            laylineLengthNm = laylineLengthNm,
            showSafetyContour = raw.optBoolean("showSafetyContour", false),
            safetyDepthMeters = safetyDepthMeters,
            courseLineBearingDeg = bearing,
            courseLineDistanceNm = distanceNm,
        )
    } catch (_: Exception) {
        SeaChartWidgetSettings()
    }
}

internal fun normalizeSeaChartMapProviderName(providerName: String?): SeaChartMapProvider {
    val migratedName = if (providerName == "OPEN_SEA_MAP") "OPEN_SEA_CHARTS" else providerName
    val provider = runCatching {
        SeaChartMapProvider.valueOf(migratedName ?: SeaChartMapProvider.NOAA.name)
    }.getOrElse { SeaChartMapProvider.NOAA }
    return ChartProviderRegistry.normalizedSelectableProvider(provider)
}
