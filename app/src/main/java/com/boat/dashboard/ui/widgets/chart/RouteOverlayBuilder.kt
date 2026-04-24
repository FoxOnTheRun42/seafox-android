package com.seafox.nmea_dashboard.ui.widgets.chart

import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object RouteOverlayBuilder {

    private const val KIND_PROP = "kind"
    private const val LABEL_PROP = "label"
    private const val STATUS_PROP = "status"
    private const val ROUTE_ID_PROP = "routeId"
    private const val ROUTE_NAME_PROP = "routeName"
    private const val LEG_INDEX_PROP = "legIndex"
    private const val WAYPOINT_INDEX_PROP = "waypointIndex"
    private const val ACTIVE_PROP = "active"

    private const val KIND_ROUTE_LEG = "routeLeg"
    private const val KIND_WAYPOINT = "waypoint"
    private const val KIND_WAYPOINT_LABEL = "waypointLabel"
    private const val KIND_XTE_CORRIDOR = "xteCorridor"
    private const val KIND_ETA_LABEL = "routeEtaLabel"

    private const val STATUS_PAST = "past"
    private const val STATUS_ACTIVE = "active"
    private const val STATUS_FUTURE = "future"

    data class RouteOverlayRequest(
        val route: Route?,
        val ownLat: Double? = null,
        val ownLon: Double? = null,
        val ownCogDeg: Double? = null,
        val ownSogKn: Double? = null,
        val showXteCorridor: Boolean = true,
        val xteLimitNm: Double = 0.25,
        val showEtaOnChart: Boolean = true,
        val useVmgForEta: Boolean = true,
        val etaLabelOffsetNm: Double = 0.08,
    )

    fun buildFeatureCollection(request: RouteOverlayRequest): JSONObject {
        return JSONObject().apply {
            put("type", "FeatureCollection")
            put("features", buildFeatures(request))
        }
    }

    fun buildRouteFeatureCollection(request: RouteOverlayRequest): JSONObject = buildFeatureCollection(request)

    fun buildFeatures(request: RouteOverlayRequest): JSONArray {
        val features = JSONArray()
        val route = request.route ?: return features
        val legs = resolveLegs(route)
        if (legs.isEmpty() && route.waypoints.isEmpty()) return features

        val activeLegIndex = route.activeLegIndex.coerceIn(0, maxOf(0, legs.lastIndex))
        val routeId = route.id
        val routeName = route.name

        legs.forEachIndexed { index, leg ->
            val status = when {
                index < activeLegIndex -> STATUS_PAST
                index == activeLegIndex -> STATUS_ACTIVE
                else -> STATUS_FUTURE
            }
            features.put(
                lineFeature(
                    kind = KIND_ROUTE_LEG,
                    points = listOf(leg.from.lon to leg.from.lat, leg.to.lon to leg.to.lat),
                    properties = mapOf(
                        ROUTE_ID_PROP to routeId,
                        ROUTE_NAME_PROP to routeName,
                        LEG_INDEX_PROP to index,
                        STATUS_PROP to status,
                        ACTIVE_PROP to (status == STATUS_ACTIVE),
                        "bearingDeg" to leg.bearingDeg,
                        "distanceNm" to leg.distanceNm,
                        "fromWaypointId" to leg.from.id,
                        "toWaypointId" to leg.to.id,
                    ),
                )
            )
        }

        route.waypoints.forEachIndexed { index, waypoint ->
            val isActiveWaypoint = index == activeLegIndex + 1 || (legs.isEmpty() && index == route.activeLegIndex)
            features.put(
                pointFeature(
                    kind = KIND_WAYPOINT,
                    lon = waypoint.lon,
                    lat = waypoint.lat,
                    properties = mapOf(
                        ROUTE_ID_PROP to routeId,
                        ROUTE_NAME_PROP to routeName,
                        WAYPOINT_INDEX_PROP to index,
                        ACTIVE_PROP to isActiveWaypoint,
                        "waypointId" to waypoint.id,
                        "waypointName" to waypoint.name,
                        "passRadiusNm" to waypoint.passRadius,
                    ),
                )
            )
            if (waypoint.name.isNotBlank()) {
                features.put(
                    pointFeature(
                        kind = KIND_WAYPOINT_LABEL,
                        lon = waypoint.lon,
                        lat = waypoint.lat,
                        properties = mapOf(
                            ROUTE_ID_PROP to routeId,
                            ROUTE_NAME_PROP to routeName,
                            WAYPOINT_INDEX_PROP to index,
                            ACTIVE_PROP to isActiveWaypoint,
                            LABEL_PROP to waypoint.name,
                            "waypointId" to waypoint.id,
                        ),
                    )
                )
            }
        }

        val activeLeg = legs.getOrNull(activeLegIndex)
        if (request.showXteCorridor && activeLeg != null && request.xteLimitNm > 0.0) {
            features.put(
                polygonFeature(
                    kind = KIND_XTE_CORRIDOR,
                    rings = listOf(
                        buildXteCorridorRing(
                            start = activeLeg.from,
                            end = activeLeg.to,
                            halfWidthNm = request.xteLimitNm,
                        )
                    ),
                    properties = mapOf(
                        ROUTE_ID_PROP to routeId,
                        ROUTE_NAME_PROP to routeName,
                        LEG_INDEX_PROP to activeLegIndex,
                        STATUS_PROP to STATUS_ACTIVE,
                        ACTIVE_PROP to true,
                        "xteLimitNm" to request.xteLimitNm,
                    ),
                )
            )
        }

        if (request.showEtaOnChart && activeLeg != null) {
            addEtaLabelFeature(
                features = features,
                request = request,
                route = route,
                leg = activeLeg,
                legIndex = activeLegIndex,
            )
        }

        return features
    }

    fun buildRouteFeatures(request: RouteOverlayRequest): JSONArray = buildFeatures(request)

    fun buildFeatureJson(request: RouteOverlayRequest): String = buildFeatureCollection(request).toString()

    private fun addEtaLabelFeature(
        features: JSONArray,
        request: RouteOverlayRequest,
        route: Route,
        leg: RouteLeg,
        legIndex: Int,
    ) {
        val ownLat = request.ownLat ?: return
        val ownLon = request.ownLon ?: return
        val dtwNm = GeoCalc.distanceNm(ownLat, ownLon, leg.to.lat, leg.to.lon)
        if (!dtwNm.isFinite() || dtwNm <= 0.0) return

        val vmgKn = if (request.useVmgForEta && request.ownCogDeg != null && request.ownSogKn != null) {
            GeoCalc.vmgKn(request.ownSogKn.toFloat(), request.ownCogDeg.toFloat(), leg.bearingDeg).toDouble()
        } else {
            null
        }
        val effectiveSpeedKn = when {
            vmgKn != null && vmgKn > 0.1 -> vmgKn
            request.ownSogKn != null && request.ownSogKn > 0.1 -> request.ownSogKn
            else -> null
        } ?: return

        val ttgHours = dtwNm / effectiveSpeedKn
        val etaMs = System.currentTimeMillis() + (ttgHours * 3_600_000.0).toLong()
        val etaText = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(etaMs))
        val label = "ETA $etaText\nDTW ${formatNm(dtwNm)} NM"
        val anchor = if (request.etaLabelOffsetNm > 0.0) {
            GeoCalc.destination(
                leg.to.lat,
                leg.to.lon,
                normalizeDegrees(leg.bearingDeg + 180.0),
                request.etaLabelOffsetNm,
            )
        } else {
            leg.to.lat to leg.to.lon
        }

        features.put(
            pointFeature(
                kind = KIND_ETA_LABEL,
                lon = anchor.second,
                lat = anchor.first,
                properties = mapOf(
                    ROUTE_ID_PROP to route.id,
                    ROUTE_NAME_PROP to route.name,
                    LEG_INDEX_PROP to legIndex,
                    ACTIVE_PROP to true,
                    LABEL_PROP to label,
                    "dtwNm" to dtwNm,
                    "ttgHours" to ttgHours,
                    "etaEpochMs" to etaMs,
                    "etaText" to etaText,
                ),
            )
        )
    }

    private fun resolveLegs(route: Route): List<RouteLeg> {
        if (route.legs.isNotEmpty()) return route.legs
        if (route.waypoints.size < 2) return emptyList()
        return route.waypoints.zipWithNext { from, to ->
            RouteLeg(
                from = from,
                to = to,
                bearingDeg = GeoCalc.bearingDeg(from.lat, from.lon, to.lat, to.lon).toFloat(),
                distanceNm = GeoCalc.distanceNm(from.lat, from.lon, to.lat, to.lon).toFloat(),
            )
        }
    }

    private fun buildXteCorridorRing(
        start: Waypoint,
        end: Waypoint,
        halfWidthNm: Double,
    ): List<Pair<Double, Double>> {
        val bearing = GeoCalc.bearingDeg(start.lat, start.lon, end.lat, end.lon)
        val leftBearing = normalizeDegrees(bearing - 90.0)
        val rightBearing = normalizeDegrees(bearing + 90.0)

        val startLeft = GeoCalc.destination(start.lat, start.lon, leftBearing, halfWidthNm)
        val endLeft = GeoCalc.destination(end.lat, end.lon, leftBearing, halfWidthNm)
        val endRight = GeoCalc.destination(end.lat, end.lon, rightBearing, halfWidthNm)
        val startRight = GeoCalc.destination(start.lat, start.lon, rightBearing, halfWidthNm)

        return listOf(
            startLeft.second to startLeft.first,
            endLeft.second to endLeft.first,
            endRight.second to endRight.first,
            startRight.second to startRight.first,
            startLeft.second to startLeft.first,
        )
    }

    private fun lineFeature(
        kind: String,
        points: List<Pair<Double, Double>>,
        properties: Map<String, Any?> = emptyMap(),
    ): JSONObject {
        return feature(
            kind = kind,
            geometry = JSONObject().apply {
                put("type", "LineString")
                put("coordinates", coordinatesArray(points))
            },
            properties = properties,
        )
    }

    private fun pointFeature(
        kind: String,
        lon: Double,
        lat: Double,
        properties: Map<String, Any?> = emptyMap(),
    ): JSONObject {
        return feature(
            kind = kind,
            geometry = JSONObject().apply {
                put("type", "Point")
                put("coordinates", JSONArray().apply {
                    put(lon)
                    put(lat)
                })
            },
            properties = properties,
        )
    }

    private fun polygonFeature(
        kind: String,
        rings: List<List<Pair<Double, Double>>>,
        properties: Map<String, Any?> = emptyMap(),
    ): JSONObject {
        return feature(
            kind = kind,
            geometry = JSONObject().apply {
                put("type", "Polygon")
                put("coordinates", ringsArray(rings))
            },
            properties = properties,
        )
    }

    private fun feature(
        kind: String,
        geometry: JSONObject,
        properties: Map<String, Any?> = emptyMap(),
    ): JSONObject {
        return JSONObject().apply {
            put("type", "Feature")
            put("properties", propertiesObject(kind, properties))
            put("geometry", geometry)
        }
    }

    private fun propertiesObject(kind: String, properties: Map<String, Any?>): JSONObject {
        return JSONObject().apply {
            put(KIND_PROP, kind)
            for ((key, value) in properties) {
                put(key, value)
            }
        }
    }

    private fun coordinatesArray(points: List<Pair<Double, Double>>): JSONArray {
        return JSONArray().apply {
            for ((lon, lat) in points) {
                put(JSONArray().apply {
                    put(lon)
                    put(lat)
                })
            }
        }
    }

    private fun ringsArray(rings: List<List<Pair<Double, Double>>>): JSONArray {
        return JSONArray().apply {
            for (ring in rings) {
                put(coordinatesArray(ring))
            }
        }
    }

    private fun formatNm(distanceNm: Double): String = String.format(Locale.US, "%.2f", distanceNm)

    private fun normalizeDegrees(value: Double): Double {
        var normalized = value % 360.0
        if (normalized < 0.0) normalized += 360.0
        return normalized
    }
}
