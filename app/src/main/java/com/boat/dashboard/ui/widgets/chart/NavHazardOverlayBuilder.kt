package com.seafox.nmea_dashboard.ui.widgets.chart

import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * Builds GeoJSON feature payloads for navigation hazard overlays.
 *
 * This object intentionally stays free of MapLibre layer concerns so it can be reused by
 * NavigationOverlay or any future chart overlay controller.
 */
object NavHazardOverlayBuilder {

    private const val KIND_PROP = "kind"
    private const val LABEL_PROP = "label"
    private const val ORIGIN_PROP = "origin"
    private const val STATUS_PROP = "status"

    private const val KIND_LAYLINE_PORT = "layline-port"
    private const val KIND_LAYLINE_STBD = "layline-stbd"
    private const val KIND_GUARD_ZONE = "guard-zone"
    private const val KIND_MOB_MARKER = "mob-marker"
    private const val KIND_MOB_LINK = "mob-link"
    private const val KIND_SAFETY_CONTOUR_CONTRACT = "safety-contour-contract"

    data class LaylineRequest(
        val ownLat: Double? = null,
        val ownLon: Double? = null,
        val targetWaypoint: Waypoint? = null,
        val windDirectionDeg: Double? = null,
        val settings: NavigationVectorSettings = NavigationVectorSettings(),
    )

    data class GuardZoneRequest(
        val centerLat: Double? = null,
        val centerLon: Double? = null,
        val settings: NavigationVectorSettings = NavigationVectorSettings(),
        val alarmActive: Boolean? = null,
    )

    data class MobRequest(
        val ownLat: Double? = null,
        val ownLon: Double? = null,
        val mobLat: Double? = null,
        val mobLon: Double? = null,
        val mobTimestampMs: Long? = null,
        val label: String = "MOB",
    )

    data class SafetyContourRequest(
        val ownLat: Double? = null,
        val ownLon: Double? = null,
        val settings: NavigationVectorSettings = NavigationVectorSettings(),
        val draftMeters: Double? = null,
        val safetyMarginMeters: Double? = null,
        val contourDepthMeters: Double? = null,
        val label: String = "Safety contour",
        val source: String = "placeholder",
        val active: Boolean = true,
    )

    fun buildLaylineFeatures(request: LaylineRequest): JSONArray {
        val features = JSONArray()
        if (!request.settings.showLaylines) return features

        val windDirectionDeg = request.windDirectionDeg.finiteOrNull() ?: return features
        val lengthNm = request.settings.laylineLengthNm.toDouble().coerceAtLeast(0.0)
        val tackAngleDeg = request.settings.tackingAngleDeg.coerceIn(0, 89).toDouble()
        val portBearing = normalizeDegrees(windDirectionDeg + tackAngleDeg)
        val stbdBearing = normalizeDegrees(windDirectionDeg - tackAngleDeg)

        if (request.settings.laylinesFromBoat) {
            val ownLat = request.ownLat.finiteOrNull()
            val ownLon = request.ownLon.finiteOrNull()
            if (ownLat != null && ownLon != null) {
                appendLaylines(
                    features = features,
                    origin = LaylineOrigin(
                        origin = "boat",
                        lat = ownLat,
                        lon = ownLon,
                        label = "Boat",
                    ),
                    portBearing = portBearing,
                    stbdBearing = stbdBearing,
                    lengthNm = lengthNm,
                    windDirectionDeg = windDirectionDeg,
                    tackAngleDeg = tackAngleDeg,
                )
            }
        }

        if (request.settings.laylinesFromWaypoint) {
            val waypoint = request.targetWaypoint
            if (waypoint != null && waypoint.lat.isFinite() && waypoint.lon.isFinite()) {
                appendLaylines(
                    features = features,
                    origin = LaylineOrigin(
                        origin = "waypoint",
                        lat = waypoint.lat,
                        lon = waypoint.lon,
                        label = waypoint.name.ifBlank { waypoint.id },
                    ),
                    portBearing = portBearing,
                    stbdBearing = stbdBearing,
                    lengthNm = lengthNm,
                    windDirectionDeg = windDirectionDeg,
                    tackAngleDeg = tackAngleDeg,
                )
            }
        }

        return features
    }

    fun buildGuardZoneFeatures(request: GuardZoneRequest): JSONArray {
        val features = JSONArray()
        if (!request.settings.guardZoneEnabled) return features

        val centerLat = request.centerLat.finiteOrNull() ?: return features
        val centerLon = request.centerLon.finiteOrNull() ?: return features
        val innerRadiusNm = request.settings.guardZoneInnerNm.toDouble().coerceAtLeast(0.0)
        val outerRadiusNm = max(
            innerRadiusNm,
            request.settings.guardZoneOuterNm.toDouble().coerceAtLeast(0.0),
        )
        if (outerRadiusNm <= 0.0) return features

        val startDeg = normalizeDegrees(request.settings.guardZoneSectorStartDeg.toDouble())
        val endDeg = normalizeDegrees(request.settings.guardZoneSectorEndDeg.toDouble())
        val fullCircle = isFullCircleSector(startDeg, endDeg)

        val geometry = JSONObject().apply {
            put("type", "Polygon")
            put(
                "coordinates",
                JSONArray().apply {
                    if (fullCircle) {
                        put(ringToJson(buildCircleRing(centerLat, centerLon, outerRadiusNm)))
                        if (innerRadiusNm > 0.0) {
                            put(ringToJson(buildCircleRing(centerLat, centerLon, innerRadiusNm).asReversed()))
                        }
                    } else {
                        put(
                            ringToJson(
                                buildSectorRing(
                                    centerLat = centerLat,
                                    centerLon = centerLon,
                                    innerRadiusNm = innerRadiusNm,
                                    outerRadiusNm = outerRadiusNm,
                                    startDeg = startDeg,
                                    endDeg = endDeg,
                                )
                            )
                        )
                    }
                }
            )
        }

        features.put(
            feature(
                kind = KIND_GUARD_ZONE,
                geometry = geometry,
                properties = mutableMapOf<String, Any?>().apply {
                    put("centerLat", centerLat)
                    put("centerLon", centerLon)
                    put("innerRadiusNm", innerRadiusNm)
                    put("outerRadiusNm", outerRadiusNm)
                    put("sectorStartDeg", startDeg)
                    put("sectorEndDeg", endDeg)
                    put("fullCircle", fullCircle)
                    request.alarmActive?.let { put("alarmActive", it) }
                    put(
                        STATUS_PROP,
                        when (request.alarmActive) {
                            true -> "alarm"
                            false -> "clear"
                            null -> "unknown"
                        }
                    )
                },
            )
        )

        return features
    }

    fun buildMobFeatures(request: MobRequest): JSONArray {
        val features = JSONArray()
        val mobLat = request.mobLat.finiteOrNull() ?: return features
        val mobLon = request.mobLon.finiteOrNull() ?: return features

        features.put(
            feature(
                kind = KIND_MOB_MARKER,
                geometry = pointGeometry(mobLon, mobLat),
                properties = mutableMapOf<String, Any?>().apply {
                    put(LABEL_PROP, request.label)
                    request.mobTimestampMs?.let { put("mobTimestampMs", it) }
                },
            )
        )

        val ownLat = request.ownLat.finiteOrNull()
        val ownLon = request.ownLon.finiteOrNull()
        if (ownLat != null && ownLon != null) {
            features.put(
                lineFeature(
                    kind = KIND_MOB_LINK,
                    points = listOf(ownLon to ownLat, mobLon to mobLat),
                    properties = mutableMapOf<String, Any?>().apply {
                        put(LABEL_PROP, request.label)
                        request.mobTimestampMs?.let { put("mobTimestampMs", it) }
                    },
                )
            )
        }

        return features
    }

    fun buildSafetyContourContractFeatures(request: SafetyContourRequest): JSONArray {
        val features = JSONArray()
        if (!request.settings.showSafetyContour) return features

        val ownLat = request.ownLat.finiteOrNull() ?: return features
        val ownLon = request.ownLon.finiteOrNull() ?: return features

        val draftMeters = request.draftMeters?.takeIf { it.isFinite() && it >= 0.0 }
        val safetyMarginMeters = request.safetyMarginMeters?.takeIf { it.isFinite() && it >= 0.0 }
        val contourDepthMeters = request.contourDepthMeters?.takeIf { it.isFinite() && it >= 0.0 }
            ?: SafetyContourPolicy.calculateSafetyDepthMeters(
                draftMeters = draftMeters,
                marginMeters = safetyMarginMeters,
                configuredSafetyDepthMeters = request.settings.safetyDepthMeters.toDouble(),
            )

        features.put(
            feature(
                kind = KIND_SAFETY_CONTOUR_CONTRACT,
                geometry = pointGeometry(ownLon, ownLat),
                properties = mutableMapOf<String, Any?>().apply {
                    put(LABEL_PROP, request.label)
                    put("source", request.source)
                    put("active", request.active)
                    put("placeholder", true)
                    put("contourDepthMeters", contourDepthMeters)
                    put("safetyDepthMeters", request.settings.safetyDepthMeters.toDouble())
                    draftMeters?.let { put("draftMeters", it) }
                    safetyMarginMeters?.let { put("safetyMarginMeters", it) }
                    put("contractState", if (request.active) "requested" else "disabled")
                },
            )
        )

        return features
    }

    fun buildFeatureCollection(vararg features: JSONObject): JSONObject {
        return buildFeatureCollection(features.asList())
    }

    fun buildFeatureCollection(features: Iterable<JSONObject>): JSONObject {
        return JSONObject().apply {
            put("type", "FeatureCollection")
            put(
                "features",
                JSONArray().apply {
                    for (feature in features) {
                        put(feature)
                    }
                }
            )
        }
    }

    fun buildFeatureCollectionFromArrays(featureArrays: Iterable<JSONArray>): JSONObject {
        return JSONObject().apply {
            put("type", "FeatureCollection")
            put(
                "features",
                JSONArray().apply {
                    for (array in featureArrays) {
                        for (index in 0 until array.length()) {
                            put(array.getJSONObject(index))
                        }
                    }
                }
            )
        }
    }

    fun buildAllHazardFeatures(
        laylineRequest: LaylineRequest? = null,
        guardZoneRequest: GuardZoneRequest? = null,
        mobRequest: MobRequest? = null,
        safetyContourRequest: SafetyContourRequest? = null,
    ): JSONObject {
        val parts = mutableListOf<JSONArray>()
        if (laylineRequest != null) parts += buildLaylineFeatures(laylineRequest)
        if (guardZoneRequest != null) parts += buildGuardZoneFeatures(guardZoneRequest)
        if (mobRequest != null) parts += buildMobFeatures(mobRequest)
        if (safetyContourRequest != null) parts += buildSafetyContourContractFeatures(safetyContourRequest)
        return buildFeatureCollectionFromArrays(parts)
    }

    private data class LaylineOrigin(
        val origin: String,
        val lat: Double,
        val lon: Double,
        val label: String,
    )

    private fun appendLaylines(
        features: JSONArray,
        origin: LaylineOrigin,
        portBearing: Double,
        stbdBearing: Double,
        lengthNm: Double,
        windDirectionDeg: Double,
        tackAngleDeg: Double,
    ) {
        features.put(
            lineFeature(
                kind = KIND_LAYLINE_PORT,
                points = linePoints(origin.lat, origin.lon, portBearing, lengthNm),
                properties = mutableMapOf<String, Any?>().apply {
                    put(ORIGIN_PROP, origin.origin)
                    put(LABEL_PROP, "${origin.label} Port")
                    put("bearingDeg", portBearing)
                    put("lengthNm", lengthNm)
                    put("windDirectionDeg", windDirectionDeg)
                    put("tackingAngleDeg", tackAngleDeg)
                },
            )
        )
        features.put(
            lineFeature(
                kind = KIND_LAYLINE_STBD,
                points = linePoints(origin.lat, origin.lon, stbdBearing, lengthNm),
                properties = mutableMapOf<String, Any?>().apply {
                    put(ORIGIN_PROP, origin.origin)
                    put(LABEL_PROP, "${origin.label} Stbd")
                    put("bearingDeg", stbdBearing)
                    put("lengthNm", lengthNm)
                    put("windDirectionDeg", windDirectionDeg)
                    put("tackingAngleDeg", tackAngleDeg)
                },
            )
        )
    }

    private fun buildCircleRing(
        centerLat: Double,
        centerLon: Double,
        radiusNm: Double,
        stepDegrees: Double = 5.0,
    ): List<Pair<Double, Double>> {
        val points = mutableListOf<Pair<Double, Double>>()
        var bearing = 0.0
        while (bearing < 360.0) {
            points += GeoCalc.destination(centerLat, centerLon, bearing, radiusNm).let { it.second to it.first }
            bearing += stepDegrees
        }
        if (points.isNotEmpty()) {
            points += points.first()
        }
        return points
    }

    private fun buildSectorRing(
        centerLat: Double,
        centerLon: Double,
        innerRadiusNm: Double,
        outerRadiusNm: Double,
        startDeg: Double,
        endDeg: Double,
    ): List<Pair<Double, Double>> {
        val sweepDeg = sectorSweepDegrees(startDeg, endDeg)
        val stepDegrees = max(5.0, min(15.0, sweepDeg / 24.0))
        val outerArc = arcPoints(centerLat, centerLon, outerRadiusNm, startDeg, endDeg, stepDegrees)
        val ring = mutableListOf<Pair<Double, Double>>()
        ring += outerArc

        if (innerRadiusNm > 0.0) {
            ring += arcPoints(centerLat, centerLon, innerRadiusNm, endDeg, startDeg, stepDegrees)
        } else {
            ring += centerLon to centerLat
        }

        if (ring.isNotEmpty() && ring.first() != ring.last()) {
            ring += ring.first()
        }
        return ring
    }

    private fun arcPoints(
        centerLat: Double,
        centerLon: Double,
        radiusNm: Double,
        startDeg: Double,
        endDeg: Double,
        stepDegrees: Double,
    ): List<Pair<Double, Double>> {
        val sweepDeg = sectorSweepDegrees(startDeg, endDeg)
        val steps = max(1, ceil(sweepDeg / stepDegrees).toInt())
        val points = mutableListOf<Pair<Double, Double>>()
        for (index in 0..steps) {
            val fraction = index.toDouble() / steps.toDouble()
            val bearing = normalizeDegrees(startDeg + (sweepDeg * fraction))
            points += GeoCalc.destination(centerLat, centerLon, bearing, radiusNm).let { it.second to it.first }
        }
        return points
    }

    private fun sectorSweepDegrees(startDeg: Double, endDeg: Double): Double {
        var delta = normalizeDegrees(endDeg) - normalizeDegrees(startDeg)
        if (delta <= 0.0) delta += 360.0
        return delta
    }

    private fun isFullCircleSector(startDeg: Double, endDeg: Double): Boolean {
        val start = normalizeDegrees(startDeg)
        val end = normalizeDegrees(endDeg)
        return abs(start - end) < 0.001 || abs(sectorSweepDegrees(start, end) - 360.0) < 0.001
    }

    private fun linePoints(
        startLat: Double,
        startLon: Double,
        bearingDeg: Double,
        distanceNm: Double,
    ): List<Pair<Double, Double>> {
        val end = GeoCalc.destination(startLat, startLon, bearingDeg, distanceNm)
        return listOf(startLon to startLat, end.second to end.first)
    }

    private fun lineFeature(
        kind: String,
        points: List<Pair<Double, Double>>,
        properties: Map<String, Any?> = emptyMap(),
    ): JSONObject {
        return feature(
            kind = kind,
            geometry = lineGeometry(points),
            properties = properties,
        )
    }

    private fun pointGeometry(lon: Double, lat: Double): JSONObject {
        return JSONObject().apply {
            put("type", "Point")
            put(
                "coordinates",
                JSONArray().apply {
                    put(lon)
                    put(lat)
                }
            )
        }
    }

    private fun lineGeometry(points: List<Pair<Double, Double>>): JSONObject {
        return JSONObject().apply {
            put("type", "LineString")
            put(
                "coordinates",
                JSONArray().apply {
                    for ((lon, lat) in points) {
                        put(
                            JSONArray().apply {
                                put(lon)
                                put(lat)
                            }
                        )
                    }
                }
            )
        }
    }

    private fun ringToJson(points: List<Pair<Double, Double>>): JSONArray {
        return JSONArray().apply {
            for ((lon, lat) in points) {
                put(
                    JSONArray().apply {
                        put(lon)
                        put(lat)
                    }
                )
            }
        }
    }

    private fun feature(
        kind: String,
        geometry: JSONObject,
        properties: Map<String, Any?> = emptyMap(),
    ): JSONObject {
        return JSONObject().apply {
            put("type", "Feature")
            put("geometry", geometry)
            put(
                "properties",
                JSONObject().apply {
                    put(KIND_PROP, kind)
                    for ((key, value) in properties) {
                        if (value == null) continue
                        put(key, value)
                    }
                }
            )
        }
    }

    private fun normalizeDegrees(value: Double): Double {
        var normalized = value % 360.0
        if (normalized < 0.0) normalized += 360.0
        return normalized
    }

    private fun Double?.finiteOrNull(): Double? {
        val value = this ?: return null
        return if (value.isFinite()) value else null
    }
}
