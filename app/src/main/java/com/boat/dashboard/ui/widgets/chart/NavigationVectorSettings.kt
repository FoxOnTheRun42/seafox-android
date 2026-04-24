package com.seafox.nmea_dashboard.ui.widgets.chart

data class Waypoint(
    val id: String,
    val name: String,
    val lat: Double,
    val lon: Double,
    val passRadius: Float = 0.1f,
)

data class RouteLeg(
    val from: Waypoint,
    val to: Waypoint,
    val bearingDeg: Float,
    val distanceNm: Float,
)

data class Route(
    val id: String,
    val name: String,
    val waypoints: List<Waypoint>,
    val legs: List<RouteLeg>,
    val activeLegIndex: Int = 0,
)

data class NavigationVectorSettings(
    val showHeadingLine: Boolean = true,
    val headingLineLengthNm: Float = 0.5f,
    val showCogVector: Boolean = true,
    val cogVectorMinutes: Int = 6,
    val showPredictor: Boolean = true,
    val predictorMinutes: Int = 6,
    val predictorIntervalMinutes: Int = 1,
    val showPredictorLabels: Boolean = true,
    val showCourseLine: Boolean = false,
    val courseLineBearingDeg: Float = 0f,
    val courseLineDistanceNm: Float = 5f,
    val showBoatIcon: Boolean = true,
    val boatIconSizeDp: Int = 24,
    val showRoute: Boolean = false,
    val showXteCorridor: Boolean = true,
    val xteLimitNm: Float = 0.25f,
    val showEtaOnChart: Boolean = true,
    val useVmgForEta: Boolean = true,
    val showLaylines: Boolean = false,
    val tackingAngleDeg: Int = 45,
    val laylinesFromWaypoint: Boolean = true,
    val laylinesFromBoat: Boolean = true,
    val laylineLengthNm: Float = 3f,
    val guardZoneEnabled: Boolean = false,
    val guardZoneInnerNm: Float = 0.5f,
    val guardZoneOuterNm: Float = 2.0f,
    val guardZoneSectorStartDeg: Float = 0f,
    val guardZoneSectorEndDeg: Float = 360f,
    val showTrack: Boolean = true,
    val trackDurationMinutes: Int = 60,
    val trackRecordIntervalSeconds: Int = 10,
    val showAisCogVectors: Boolean = true,
    val showAisCpaPoints: Boolean = true,
    val aisPredictorMinutes: Int = 6,
    val showSafetyContour: Boolean = false,
    val safetyDepthMeters: Float = 3f,
)
