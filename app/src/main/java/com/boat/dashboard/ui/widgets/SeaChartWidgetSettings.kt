package com.seafox.nmea_dashboard.ui.widgets

enum class SeaChartMapProvider(val label: String) {
    NOAA("NOAA"),
    S57("S-57"),
    S63("S-63"),
    OPEN_SEA_CHARTS("OpenSeaCharts"),
    C_MAP("C-Map"),
}

enum class SeaChartSpeedSource(val label: String) {
    GPS_SOG("GPS (SOG)"),
    LOG_STW("Geber (STW)"),
}

data class SeaChartWidgetSettings(
    val mapProvider: SeaChartMapProvider = SeaChartMapProvider.NOAA,
    val speedSource: SeaChartSpeedSource = SeaChartSpeedSource.GPS_SOG,
    val showAisOverlay: Boolean = false,
    val showGribOverlay: Boolean = false,
    val showOpenSeaMapOverlay: Boolean = false,
    val showHeadingLine: Boolean = true,
    val headingLineLengthNm: Float = 0.5f,
    val showCogVector: Boolean = true,
    val cogVectorMinutes: Int = 6,
    val showPredictor: Boolean = true,
    val predictorMinutes: Int = 6,
    val predictorIntervalMinutes: Int = 1,
    val showPredictorLabels: Boolean = true,
    val showBoatIcon: Boolean = true,
    val boatIconSizeDp: Int = 24,
    val showCourseLine: Boolean = false,
    val showRoadLayers: Boolean = true,
    val showDepthLines: Boolean = true,
    val showContourLines: Boolean = true,
    val showTrack: Boolean = true,
    val trackDurationMinutes: Int = 60,
    val trackRecordIntervalSeconds: Int = 10,
    val guardZoneEnabled: Boolean = false,
    val guardZoneInnerNm: Float = 0.5f,
    val guardZoneOuterNm: Float = 2.0f,
    val guardZoneSectorStartDeg: Float = 0f,
    val guardZoneSectorEndDeg: Float = 360f,
    val showLaylines: Boolean = false,
    val tackingAngleDeg: Int = 45,
    val laylineLengthNm: Float = 3f,
    val showSafetyContour: Boolean = false,
    val safetyDepthMeters: Float = 3f,
    val courseLineBearingDeg: Float = 0f,
    val courseLineDistanceNm: Float = 5f,
)
