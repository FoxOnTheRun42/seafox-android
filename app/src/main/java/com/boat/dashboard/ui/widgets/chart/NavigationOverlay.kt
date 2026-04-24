package com.seafox.nmea_dashboard.ui.widgets.chart

import org.json.JSONArray
import org.json.JSONObject
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.Layer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.android.maps.Style
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object NavigationOverlay {

    private const val SOURCE_ID = "nav-vectors-source"
    private const val TRACK_LAYER = "nav-track-line"
    private const val HEADING_LINE_LAYER = "nav-heading-line"
    private const val COG_VECTOR_LAYER = "nav-cog-vector"
    private const val PREDICTOR_LAYER = "nav-predictor"
    private const val PREDICTOR_MARKS_LAYER = "nav-predictor-marks"
    private const val PREDICTOR_LABEL_LAYER = "nav-predictor-labels"
    private const val COURSE_LINE_LAYER = "nav-course-line"
    private const val ETA_LABEL_LAYER = "nav-route-eta-label"
    private const val WAYPOINT_LABEL_LAYER = "nav-route-waypoint-label"
    private const val WAYPOINT_ACTIVE_LAYER = "nav-route-waypoint-active"
    private const val WAYPOINT_LAYER = "nav-route-waypoint"
    private const val MOB_MARKER_LAYER = "nav-mob-marker"
    private const val BOAT_ICON_LAYER = "nav-boat-icon"
    private const val MOB_LINK_LAYER = "nav-mob-link"
    private const val LAYLINE_PORT_LAYER = "nav-layline-port"
    private const val LAYLINE_STBD_LAYER = "nav-layline-stbd"
    private const val SAFETY_CONTOUR_LAYER = "nav-safety-contour"
    private const val ROUTE_ACTIVE_LAYER = "nav-route-active"
    private const val ROUTE_FUTURE_LAYER = "nav-route-future"
    private const val ROUTE_PAST_LAYER = "nav-route-past"
    private const val XTE_CORRIDOR_LAYER = "nav-xte-corridor"
    private const val GUARD_ZONE_LAYER = "nav-guard-zone"
    private const val AIS_ANCHOR_LAYER = "ais-targets-circle"

    private const val KIND_PROP = "kind"
    private const val BEARING_PROP = "bearing"
    private const val LABEL_PROP = "label"
    private const val STATUS_PROP = "status"
    private const val ACTIVE_PROP = "active"

    private val trackPoints = mutableListOf<TrackPoint>()
    private var lastTrackRecordedAtMs = 0L

    fun update(
        style: Style,
        ownLat: Float?,
        ownLon: Float?,
        headingDeg: Float?,
        cogDeg: Float?,
        sogKn: Float?,
        settings: NavigationVectorSettings,
        isNightMode: Boolean,
        activeRoute: Route? = null,
        mobLat: Double? = null,
        mobLon: Double? = null,
        mobTimestampMs: Long? = null,
        trueWindDirectionDeg: Float? = null,
        boatDraftMeters: Float? = null,
    ) {
        ensureSetup(style, isNightMode)

        val features = JSONArray()
        val lat = ownLat?.takeIf(Float::isFinite)?.toDouble()
        val lon = ownLon?.takeIf(Float::isFinite)?.toDouble()
        addRouteFeatures(
            features = features,
            ownLat = lat,
            ownLon = lon,
            cogDeg = cogDeg?.takeIf(Float::isFinite)?.toDouble(),
            sogKn = sogKn?.takeIf(Float::isFinite)?.toDouble(),
            settings = settings,
            activeRoute = activeRoute,
        )
        if (lat != null && lon != null) {
            appendTrackPoint(lat, lon, sogKn, settings)
            addTrackFeature(features, isNightMode)

            val effectiveHeading = headingDeg?.takeIf(Float::isFinite) ?: cogDeg?.takeIf(Float::isFinite)
            if (settings.showHeadingLine && headingDeg?.isFinite() == true) {
                addVectorLine(
                    features = features,
                    kind = "heading",
                    startLat = lat,
                    startLon = lon,
                    bearingDeg = headingDeg.toDouble(),
                    distanceNm = settings.headingLineLengthNm.toDouble(),
                )
            }
            if (settings.showCogVector && cogDeg?.isFinite() == true && sogKn?.isFinite() == true && sogKn > 0.05f) {
                addVectorLine(
                    features = features,
                    kind = "cog",
                    startLat = lat,
                    startLon = lon,
                    bearingDeg = cogDeg.toDouble(),
                    distanceNm = sogKn * settings.cogVectorMinutes / 60.0,
                )
            }
            if (settings.showPredictor && cogDeg?.isFinite() == true && sogKn?.isFinite() == true && sogKn > 0.05f) {
                addVectorLine(
                    features = features,
                    kind = "predictor",
                    startLat = lat,
                    startLon = lon,
                    bearingDeg = cogDeg.toDouble(),
                    distanceNm = sogKn * settings.predictorMinutes / 60.0,
                )
                addPredictorMarks(
                    features = features,
                    startLat = lat,
                    startLon = lon,
                    cogDeg = cogDeg.toDouble(),
                    sogKn = sogKn.toDouble(),
                    settings = settings,
                )
            }
            if (settings.showCourseLine) {
                addVectorLine(
                    features = features,
                    kind = "courseLine",
                    startLat = lat,
                    startLon = lon,
                    bearingDeg = settings.courseLineBearingDeg.toDouble(),
                    distanceNm = settings.courseLineDistanceNm.toDouble(),
                )
            }
            if (settings.showBoatIcon) {
                features.put(
                    pointFeature(
                        kind = "boat",
                        lon = lon,
                        lat = lat,
                        properties = mapOf(
                            BEARING_PROP to (effectiveHeading?.toDouble() ?: 0.0),
                            "iconSize" to settings.boatIconSizeDp,
                        ),
                    )
                )
            }
            addHazardFeatures(
                features = features,
                ownLat = lat,
                ownLon = lon,
                settings = settings,
                activeRoute = activeRoute,
                mobLat = mobLat,
                mobLon = mobLon,
                mobTimestampMs = mobTimestampMs,
                trueWindDirectionDeg = trueWindDirectionDeg?.takeIf(Float::isFinite)?.toDouble(),
                boatDraftMeters = boatDraftMeters?.takeIf(Float::isFinite)?.toDouble(),
            )
        } else {
            trackPoints.clear()
            lastTrackRecordedAtMs = 0L
        }

        val source = style.getSource(SOURCE_ID) as? GeoJsonSource ?: return
        source.setGeoJson(
            JSONObject().apply {
                put("type", "FeatureCollection")
                put("features", features)
            }.toString()
        )
    }

    fun remove(style: Style) {
        listOf(
            BOAT_ICON_LAYER,
            MOB_MARKER_LAYER,
            WAYPOINT_LABEL_LAYER,
            ETA_LABEL_LAYER,
            WAYPOINT_ACTIVE_LAYER,
            WAYPOINT_LAYER,
            PREDICTOR_LABEL_LAYER,
            PREDICTOR_MARKS_LAYER,
            COURSE_LINE_LAYER,
            PREDICTOR_LAYER,
            COG_VECTOR_LAYER,
            HEADING_LINE_LAYER,
            MOB_LINK_LAYER,
            LAYLINE_PORT_LAYER,
            LAYLINE_STBD_LAYER,
            TRACK_LAYER,
            ROUTE_ACTIVE_LAYER,
            ROUTE_FUTURE_LAYER,
            ROUTE_PAST_LAYER,
            XTE_CORRIDOR_LAYER,
            GUARD_ZONE_LAYER,
        ).forEach { layerId ->
            try {
                style.removeLayer(layerId)
            } catch (_: Exception) {
            }
        }
        try {
            style.removeSource(SOURCE_ID)
        } catch (_: Exception) {
        }
    }

    private fun ensureSetup(style: Style, isNightMode: Boolean) {
        if (style.getSource(SOURCE_ID) != null) {
            return
        }

        style.addSource(GeoJsonSource(SOURCE_ID, emptyFeatureCollection()))

        val anchorLayerId = if (style.getLayer(AIS_ANCHOR_LAYER) != null) AIS_ANCHOR_LAYER else null
        var insertBelowId = anchorLayerId
        val orderedLayers = listOf(
            buildBoatLayer(isNightMode),
            buildMobMarkerLayer(isNightMode),
            buildWaypointLabelLayer(isNightMode),
            buildEtaLabelLayer(isNightMode),
            buildWaypointActiveLayer(isNightMode),
            buildWaypointLayer(isNightMode),
            buildPredictorLabelLayer(isNightMode),
            buildPredictorMarksLayer(isNightMode),
            buildCourseLineLayer(isNightMode),
            buildPredictorLayer(isNightMode),
            buildCogVectorLayer(isNightMode),
            buildHeadingLayer(isNightMode),
            buildMobLinkLayer(isNightMode),
            buildLaylinePortLayer(isNightMode),
            buildLaylineStbdLayer(isNightMode),
            buildSafetyContourLayer(isNightMode),
            buildTrackLayer(isNightMode),
            buildRouteActiveLayer(isNightMode),
            buildRouteFutureLayer(isNightMode),
            buildRoutePastLayer(isNightMode),
            buildXteCorridorLayer(isNightMode),
            buildGuardZoneLayer(isNightMode),
        )

        for (layer in orderedLayers) {
            insertBelowId = addLayer(style, layer, insertBelowId)
        }
    }

    private fun addLayer(style: Style, layer: Layer, belowLayerId: String?): String {
        if (belowLayerId != null && style.getLayer(belowLayerId) != null) {
            style.addLayerBelow(layer, belowLayerId)
        } else {
            style.addLayer(layer)
        }
        return layer.id
    }

    private fun buildTrackLayer(isNightMode: Boolean): Layer {
        return LineLayer(TRACK_LAYER, SOURCE_ID).withProperties(
            PropertyFactory.lineColor(if (isNightMode) "#888888" else "#FFFFFF"),
            PropertyFactory.lineWidth(2.0f),
            PropertyFactory.lineOpacity(0.75f),
        ).withFilter(kindEq("track"))
    }

    private fun buildHeadingLayer(isNightMode: Boolean): Layer {
        return LineLayer(HEADING_LINE_LAYER, SOURCE_ID).withProperties(
            PropertyFactory.lineColor(if (isNightMode) "#CCCCCC" else "#FFFFFF"),
            PropertyFactory.lineWidth(2.0f),
        ).withFilter(kindEq("heading"))
    }

    private fun buildCogVectorLayer(isNightMode: Boolean): Layer {
        return LineLayer(COG_VECTOR_LAYER, SOURCE_ID).withProperties(
            PropertyFactory.lineColor(if (isNightMode) "#CCAA00" else "#FFD700"),
            PropertyFactory.lineWidth(2.0f),
            PropertyFactory.lineDasharray(arrayOf(3f, 2f)),
        ).withFilter(kindEq("cog"))
    }

    private fun buildPredictorLayer(isNightMode: Boolean): Layer {
        return LineLayer(PREDICTOR_LAYER, SOURCE_ID).withProperties(
            PropertyFactory.lineColor(if (isNightMode) "#CCAA00" else "#FFD700"),
            PropertyFactory.lineWidth(3.0f),
        ).withFilter(kindEq("predictor"))
    }

    private fun buildCourseLineLayer(isNightMode: Boolean): Layer {
        return LineLayer(COURSE_LINE_LAYER, SOURCE_ID).withProperties(
            PropertyFactory.lineColor(if (isNightMode) "#008888" else "#00FFFF"),
            PropertyFactory.lineWidth(1.5f),
            PropertyFactory.lineDasharray(arrayOf(1f, 2f)),
        ).withFilter(kindEq("courseLine"))
    }

    private fun buildEtaLabelLayer(isNightMode: Boolean): Layer {
        return SymbolLayer(ETA_LABEL_LAYER, SOURCE_ID).withProperties(
            PropertyFactory.textField(Expression.get(LABEL_PROP)),
            PropertyFactory.textSize(11f),
            PropertyFactory.textColor("#FFFFFF"),
            PropertyFactory.textHaloColor("#000000"),
            PropertyFactory.textHaloWidth(1f),
            PropertyFactory.textOffset(arrayOf(0f, 1.2f)),
            PropertyFactory.textAllowOverlap(true),
            PropertyFactory.textIgnorePlacement(true),
        ).withFilter(kindEq("routeEtaLabel"))
    }

    private fun buildWaypointLayer(isNightMode: Boolean): Layer {
        return CircleLayer(WAYPOINT_LAYER, SOURCE_ID).withProperties(
            PropertyFactory.circleRadius(6f),
            PropertyFactory.circleColor("#FFFFFF"),
            PropertyFactory.circleStrokeColor(if (isNightMode) "#CC00CC" else "#FF00FF"),
            PropertyFactory.circleStrokeWidth(2.0f),
        ).withFilter(
            Expression.all(
                kindEq("waypoint"),
                activeEq(false),
            )
        )
    }

    private fun buildWaypointActiveLayer(isNightMode: Boolean): Layer {
        return CircleLayer(WAYPOINT_ACTIVE_LAYER, SOURCE_ID).withProperties(
            PropertyFactory.circleRadius(7f),
            PropertyFactory.circleColor(if (isNightMode) "#CC00CC" else "#FF00FF"),
            PropertyFactory.circleStrokeColor("#FFFFFF"),
            PropertyFactory.circleStrokeWidth(1.5f),
        ).withFilter(
            Expression.all(
                kindEq("waypoint"),
                activeEq(true),
            )
        )
    }

    private fun buildWaypointLabelLayer(isNightMode: Boolean): Layer {
        return SymbolLayer(WAYPOINT_LABEL_LAYER, SOURCE_ID).withProperties(
            PropertyFactory.textField(Expression.get(LABEL_PROP)),
            PropertyFactory.textSize(11f),
            PropertyFactory.textColor("#FFFFFF"),
            PropertyFactory.textHaloColor("#000000"),
            PropertyFactory.textHaloWidth(1f),
            PropertyFactory.textOffset(arrayOf(0f, 1.3f)),
            PropertyFactory.textAllowOverlap(true),
            PropertyFactory.textIgnorePlacement(true),
        ).withFilter(kindEq("waypointLabel"))
    }

    private fun buildPredictorMarksLayer(isNightMode: Boolean): Layer {
        return CircleLayer(PREDICTOR_MARKS_LAYER, SOURCE_ID).withProperties(
            PropertyFactory.circleRadius(3f),
            PropertyFactory.circleColor(if (isNightMode) "#CCAA00" else "#FFD700"),
            PropertyFactory.circleStrokeColor("#000000"),
            PropertyFactory.circleStrokeWidth(1f),
        ).withFilter(kindEq("predictorMark"))
    }

    private fun buildPredictorLabelLayer(isNightMode: Boolean): Layer {
        return SymbolLayer(PREDICTOR_LABEL_LAYER, SOURCE_ID).withProperties(
            PropertyFactory.textField(Expression.get(LABEL_PROP)),
            PropertyFactory.textSize(10f),
            PropertyFactory.textColor(if (isNightMode) "#CCAA00" else "#FFD700"),
            PropertyFactory.textHaloColor(if (isNightMode) "#0A1628" else "#FFFFFF"),
            PropertyFactory.textHaloWidth(1f),
            PropertyFactory.textOffset(arrayOf(0f, 1.4f)),
            PropertyFactory.textAllowOverlap(true),
            PropertyFactory.textIgnorePlacement(true),
        ).withFilter(
            Expression.all(
                kindEq("predictorMark"),
                Expression.has(LABEL_PROP),
            )
        )
    }

    private fun buildBoatLayer(isNightMode: Boolean): Layer {
        return SymbolLayer(BOAT_ICON_LAYER, SOURCE_ID).withProperties(
            PropertyFactory.textField(Expression.literal("▲")),
            PropertyFactory.textColor(if (isNightMode) "#42A5F5" else "#1565C0"),
            PropertyFactory.textHaloColor("#FFFFFF"),
            PropertyFactory.textHaloWidth(1.2f),
            PropertyFactory.textAllowOverlap(true),
            PropertyFactory.textIgnorePlacement(true),
            PropertyFactory.textRotate(Expression.toNumber(Expression.get(BEARING_PROP))),
            PropertyFactory.textSize(
                Expression.interpolate(
                    Expression.linear(),
                    Expression.zoom(),
                    Expression.stop(4, 12),
                    Expression.stop(10, 20),
                    Expression.stop(14, 28),
                    Expression.stop(18, 40),
                )
            ),
        ).withFilter(kindEq("boat"))
    }

    private fun buildMobMarkerLayer(isNightMode: Boolean): Layer {
        return CircleLayer(MOB_MARKER_LAYER, SOURCE_ID).withProperties(
            PropertyFactory.circleRadius(8f),
            PropertyFactory.circleColor("#FF0000"),
            PropertyFactory.circleStrokeColor("#FFFFFF"),
            PropertyFactory.circleStrokeWidth(2.0f),
        ).withFilter(kindEq("mob-marker"))
    }

    private fun buildMobLinkLayer(isNightMode: Boolean): Layer {
        return LineLayer(MOB_LINK_LAYER, SOURCE_ID).withProperties(
            PropertyFactory.lineColor("#FF0000"),
            PropertyFactory.lineWidth(2.0f),
        ).withFilter(kindEq("mob-link"))
    }

    private fun buildLaylinePortLayer(isNightMode: Boolean): Layer {
        return LineLayer(LAYLINE_PORT_LAYER, SOURCE_ID).withProperties(
            PropertyFactory.lineColor(if (isNightMode) "#C62828" else "#F44336"),
            PropertyFactory.lineWidth(1.5f),
            PropertyFactory.lineDasharray(arrayOf(3f, 2f)),
        ).withFilter(kindEq("layline-port"))
    }

    private fun buildLaylineStbdLayer(isNightMode: Boolean): Layer {
        return LineLayer(LAYLINE_STBD_LAYER, SOURCE_ID).withProperties(
            PropertyFactory.lineColor(if (isNightMode) "#2E7D32" else "#4CAF50"),
            PropertyFactory.lineWidth(1.5f),
            PropertyFactory.lineDasharray(arrayOf(3f, 2f)),
        ).withFilter(kindEq("layline-stbd"))
    }

    private fun buildSafetyContourLayer(isNightMode: Boolean): Layer {
        return CircleLayer(SAFETY_CONTOUR_LAYER, SOURCE_ID).withProperties(
            PropertyFactory.circleRadius(6f),
            PropertyFactory.circleColor(if (isNightMode) "#FFAA33" else "#FF6600"),
            PropertyFactory.circleStrokeWidth(2f),
            PropertyFactory.circleStrokeColor("#FFFFFF"),
            PropertyFactory.circleOpacity(0.9f),
        ).withFilter(kindEq("safety-contour-contract"))
    }

    private fun buildRoutePastLayer(isNightMode: Boolean): Layer {
        return LineLayer(ROUTE_PAST_LAYER, SOURCE_ID).withProperties(
            PropertyFactory.lineColor(if (isNightMode) "#444444" else "#888888"),
            PropertyFactory.lineWidth(1.0f),
        ).withFilter(
            Expression.all(
                kindEq("routeLeg"),
                statusEq("past"),
            )
        )
    }

    private fun buildRouteFutureLayer(isNightMode: Boolean): Layer {
        return LineLayer(ROUTE_FUTURE_LAYER, SOURCE_ID).withProperties(
            PropertyFactory.lineColor(if (isNightMode) "#CC00CC" else "#FF00FF"),
            PropertyFactory.lineWidth(2.0f),
        ).withFilter(
            Expression.all(
                kindEq("routeLeg"),
                statusEq("future"),
            )
        )
    }

    private fun buildRouteActiveLayer(isNightMode: Boolean): Layer {
        return LineLayer(ROUTE_ACTIVE_LAYER, SOURCE_ID).withProperties(
            PropertyFactory.lineColor(if (isNightMode) "#CC00CC" else "#FF00FF"),
            PropertyFactory.lineWidth(3.0f),
        ).withFilter(
            Expression.all(
                kindEq("routeLeg"),
                statusEq("active"),
            )
        )
    }

    private fun buildXteCorridorLayer(isNightMode: Boolean): Layer {
        return FillLayer(XTE_CORRIDOR_LAYER, SOURCE_ID).withProperties(
            PropertyFactory.fillColor(if (isNightMode) "#2E7D32" else "#4CAF50"),
            PropertyFactory.fillOpacity(0.15f),
        ).withFilter(kindEq("xteCorridor"))
    }

    private fun buildGuardZoneLayer(isNightMode: Boolean): Layer {
        return FillLayer(GUARD_ZONE_LAYER, SOURCE_ID).withProperties(
            PropertyFactory.fillColor(
                Expression.switchCase(
                    Expression.eq(Expression.get("alarmActive"), Expression.literal(true)),
                    Expression.literal(if (isNightMode) "#C62828" else "#F44336"),
                    Expression.literal(if (isNightMode) "#2E7D32" else "#4CAF50"),
                )
            ),
            PropertyFactory.fillOpacity(
                Expression.switchCase(
                    Expression.eq(Expression.get("alarmActive"), Expression.literal(true)),
                    Expression.literal(0.20f),
                    Expression.literal(0.10f),
                )
            ),
        ).withFilter(kindEq("guard-zone"))
    }

    private fun addTrackFeature(features: JSONArray, isNightMode: Boolean) {
        if (trackPoints.size < 2) return
        features.put(
            lineFeature(
                kind = "track",
                points = trackPoints.map { it.lon to it.lat },
                properties = mapOf("night" to isNightMode),
            )
        )
    }

    private fun addVectorLine(
        features: JSONArray,
        kind: String,
        startLat: Double,
        startLon: Double,
        bearingDeg: Double,
        distanceNm: Double,
    ) {
        if (distanceNm <= 0.0) return
        val destination = GeoCalc.destination(startLat, startLon, bearingDeg, distanceNm)
        features.put(
            lineFeature(
                kind = kind,
                points = listOf(startLon to startLat, destination.second to destination.first),
            )
        )
    }

    private fun addPredictorMarks(
        features: JSONArray,
        startLat: Double,
        startLon: Double,
        cogDeg: Double,
        sogKn: Double,
        settings: NavigationVectorSettings,
    ) {
        val stepMinutes = settings.predictorIntervalMinutes.coerceAtLeast(1)
        val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        val nowMs = System.currentTimeMillis()

        var minute = stepMinutes
        while (minute <= settings.predictorMinutes) {
            val distanceNm = sogKn * minute / 60.0
            val destination = GeoCalc.destination(startLat, startLon, cogDeg, distanceNm)
            val label = if (settings.showPredictorLabels) {
                formatter.format(Date(nowMs + minute * 60_000L))
            } else {
                null
            }
            features.put(
                pointFeature(
                    kind = "predictorMark",
                    lon = destination.second,
                    lat = destination.first,
                    properties = buildMap {
                        if (label != null) put(LABEL_PROP, label)
                    },
                )
            )
            minute += stepMinutes
        }
    }

    private fun addRouteFeatures(
        features: JSONArray,
        ownLat: Double?,
        ownLon: Double?,
        cogDeg: Double?,
        sogKn: Double?,
        settings: NavigationVectorSettings,
        activeRoute: Route?,
    ) {
        if (activeRoute == null) return
        val allowedKinds = buildSet {
            add("routeLeg")
            add("waypoint")
            add("waypointLabel")
            if (settings.showXteCorridor) add("xteCorridor")
            if (settings.showEtaOnChart) add("routeEtaLabel")
        }
        if (allowedKinds.isEmpty()) return
        appendFeatures(
            features = features,
            additionalFeatures = RouteOverlayBuilder.buildFeatures(
                RouteOverlayBuilder.RouteOverlayRequest(
                    route = activeRoute,
                    ownLat = ownLat,
                    ownLon = ownLon,
                    ownCogDeg = cogDeg,
                    ownSogKn = sogKn,
                    showXteCorridor = settings.showXteCorridor,
                    xteLimitNm = settings.xteLimitNm.toDouble(),
                    showEtaOnChart = settings.showEtaOnChart,
                    useVmgForEta = settings.useVmgForEta,
                )
            ),
            allowedKinds = allowedKinds,
        )
    }

    private fun addHazardFeatures(
        features: JSONArray,
        ownLat: Double,
        ownLon: Double,
        settings: NavigationVectorSettings,
        activeRoute: Route?,
        mobLat: Double?,
        mobLon: Double?,
        mobTimestampMs: Long?,
        trueWindDirectionDeg: Double?,
        boatDraftMeters: Double?,
    ) {
        appendFeatures(
            features = features,
            additionalFeatures = NavHazardOverlayBuilder.buildGuardZoneFeatures(
                NavHazardOverlayBuilder.GuardZoneRequest(
                    centerLat = ownLat,
                    centerLon = ownLon,
                    settings = settings,
                )
            ),
        )
        if (mobLat != null && mobLon != null) {
            appendFeatures(
                features = features,
                additionalFeatures = NavHazardOverlayBuilder.buildMobFeatures(
                    NavHazardOverlayBuilder.MobRequest(
                        ownLat = ownLat,
                        ownLon = ownLon,
                        mobLat = mobLat,
                        mobLon = mobLon,
                        mobTimestampMs = mobTimestampMs,
                    )
                ),
            )
        }
        if (settings.showLaylines && trueWindDirectionDeg != null) {
            val targetWaypoint = activeRoute?.let { route ->
                route.waypoints.getOrNull(route.activeLegIndex + 1)
                    ?: route.waypoints.getOrNull(route.activeLegIndex)
            }
            appendFeatures(
                features = features,
                additionalFeatures = NavHazardOverlayBuilder.buildLaylineFeatures(
                    NavHazardOverlayBuilder.LaylineRequest(
                        ownLat = ownLat,
                        ownLon = ownLon,
                        targetWaypoint = targetWaypoint,
                        windDirectionDeg = trueWindDirectionDeg,
                        settings = settings,
                    )
                ),
            )
        }
        if (settings.showSafetyContour) {
            appendFeatures(
                features = features,
                additionalFeatures = NavHazardOverlayBuilder.buildSafetyContourContractFeatures(
                    NavHazardOverlayBuilder.SafetyContourRequest(
                        ownLat = ownLat,
                        ownLon = ownLon,
                        settings = settings,
                        draftMeters = boatDraftMeters,
                        source = "navigation-settings",
                    )
                ),
            )
        }
    }

    private fun appendFeatures(
        features: JSONArray,
        additionalFeatures: JSONArray,
        allowedKinds: Set<String>? = null,
    ) {
        for (index in 0 until additionalFeatures.length()) {
            val feature = additionalFeatures.optJSONObject(index) ?: continue
            val kind = feature.optJSONObject("properties")?.optString(KIND_PROP)?.takeIf(String::isNotBlank)
            if (allowedKinds != null && kind !in allowedKinds) continue
            features.put(feature)
        }
    }

    private fun appendTrackPoint(
        lat: Double,
        lon: Double,
        sogKn: Float?,
        settings: NavigationVectorSettings,
    ) {
        if (!settings.showTrack) {
            trackPoints.clear()
            lastTrackRecordedAtMs = 0L
            return
        }

        val nowMs = System.currentTimeMillis()
        val shouldRecord = when (val last = trackPoints.lastOrNull()) {
            null -> true
            else -> {
                val elapsedMs = nowMs - lastTrackRecordedAtMs
                val movedEnough = GeoCalc.distanceNm(last.lat, last.lon, lat, lon) >= 0.005
                elapsedMs >= settings.trackRecordIntervalSeconds.coerceAtLeast(1) * 1000L && movedEnough
            }
        }

        if (shouldRecord) {
            trackPoints += TrackPoint(
                lat = lat,
                lon = lon,
                recordedAtMs = nowMs,
                sogKn = sogKn?.takeIf(Float::isFinite),
            )
            lastTrackRecordedAtMs = nowMs
        }

        val cutoffMs = nowMs - settings.trackDurationMinutes.coerceAtLeast(1) * 60_000L
        while (trackPoints.isNotEmpty() && trackPoints.first().recordedAtMs < cutoffMs) {
            trackPoints.removeAt(0)
        }
    }

    private fun lineFeature(
        kind: String,
        points: List<Pair<Double, Double>>,
        properties: Map<String, Any?> = emptyMap(),
    ): JSONObject {
        return JSONObject().apply {
            put("type", "Feature")
            put("properties", JSONObject().apply {
                put(KIND_PROP, kind)
                for ((key, value) in properties) {
                    put(key, value)
                }
            })
            put(
                "geometry",
                JSONObject().apply {
                    put("type", "LineString")
                    put(
                        "coordinates",
                        JSONArray().apply {
                            for ((lon, lat) in points) {
                                put(JSONArray().apply {
                                    put(lon)
                                    put(lat)
                                })
                            }
                        }
                    )
                }
            )
        }
    }

    private fun pointFeature(
        kind: String,
        lon: Double,
        lat: Double,
        properties: Map<String, Any?> = emptyMap(),
    ): JSONObject {
        return JSONObject().apply {
            put("type", "Feature")
            put("properties", JSONObject().apply {
                put(KIND_PROP, kind)
                for ((key, value) in properties) {
                    put(key, value)
                }
            })
            put(
                "geometry",
                JSONObject().apply {
                    put("type", "Point")
                    put("coordinates", JSONArray().apply {
                        put(lon)
                        put(lat)
                    })
                }
            )
        }
    }

    private fun kindEq(kind: String): Expression {
        return Expression.eq(Expression.get(KIND_PROP), Expression.literal(kind))
    }

    private fun statusEq(status: String): Expression {
        return Expression.eq(Expression.get(STATUS_PROP), Expression.literal(status))
    }

    private fun activeEq(active: Boolean): Expression {
        return Expression.eq(Expression.get(ACTIVE_PROP), Expression.literal(active))
    }

    private fun emptyFeatureCollection(): String {
        return JSONObject().apply {
            put("type", "FeatureCollection")
            put("features", JSONArray())
        }.toString()
    }

    private data class TrackPoint(
        val lat: Double,
        val lon: Double,
        val recordedAtMs: Long,
        val sogKn: Float?,
    )
}
