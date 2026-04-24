package com.seafox.nmea_dashboard.ui.widgets.chart

import com.seafox.nmea_dashboard.ui.widgets.AisTargetData
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.json.JSONArray
import org.json.JSONObject

/**
 * Manages AIS target overlay on the MapLibre map.
 *
 * Renders AIS targets as colored circles with optional name/MMSI labels,
 * plus predictor lines and CPA markers when target motion data is available.
 * Color coding: green = moving away, red = approaching (based on CPA).
 */
object AisOverlay {

    private const val SOURCE_ID = "ais-targets-source"
    private const val PREDICTOR_LAYER_ID = "ais-targets-predictor-line"
    private const val CPA_LINK_LAYER_ID = "ais-targets-cpa-link"
    private const val CIRCLE_LAYER_ID = "ais-targets-circle"
    private const val CPA_MARKER_LAYER_ID = "ais-targets-cpa-marker"
    private const val LABEL_LAYER_ID = "ais-targets-label"
    private const val PREDICTOR_MINUTES = 6.0

    fun setup(style: Style) {
        ensureSource(style)
        ensureLayers(style)
    }

    fun update(style: Style, targets: List<AisTargetData>) {
        val source = style.getSource(SOURCE_ID) as? GeoJsonSource ?: return
        source.setGeoJson(buildGeoJson(targets))
    }

    fun remove(style: Style) {
        removeLayerSafely(style, LABEL_LAYER_ID)
        removeLayerSafely(style, CPA_MARKER_LAYER_ID)
        removeLayerSafely(style, CIRCLE_LAYER_ID)
        removeLayerSafely(style, CPA_LINK_LAYER_ID)
        removeLayerSafely(style, PREDICTOR_LAYER_ID)
        removeSourceSafely(style, SOURCE_ID)
    }

    private fun buildGeoJson(targets: List<AisTargetData>): String {
        val features = JSONArray()
        for (target in targets) {
            val lat = target.latitude ?: continue
            val lon = target.longitude ?: continue
            val courseDeg = target.courseDeg?.takeIf { it.isFinite() }
            val speedKn = target.speedKn?.takeIf { it.isFinite() && it >= 0f }
            val cpaNm = target.cpaNm?.takeIf { it.isFinite() }
            val cpaTimeMinutes = target.cpaTimeMinutes?.takeIf { it.isFinite() }

            val properties = JSONObject().apply {
                put("label", target.name ?: target.mmsiText ?: "AIS")
                put("mmsi", target.mmsiText ?: "")
                put("course", courseDeg ?: 0f)
                put("speed", speedKn ?: 0f)
                put("threat", classifyThreat(target))
            }

            features.put(
                feature(
                    kind = "ais-target",
                    geometryType = "Point",
                    coordinates = pointCoordinates(lon.toDouble(), lat.toDouble()),
                    properties = properties,
                )
            )

            if (courseDeg != null && speedKn != null && speedKn > 0.05f) {
                val predictorEnd = destination(
                    lat = lat.toDouble(),
                    lon = lon.toDouble(),
                    bearingDeg = courseDeg.toDouble(),
                    distanceNm = speedKn.toDouble() * PREDICTOR_MINUTES / 60.0,
                )
                features.put(
                    feature(
                        kind = "ais-predictor-line",
                        geometryType = "LineString",
                        coordinates = lineCoordinates(
                            lon.toDouble() to lat.toDouble(),
                            predictorEnd.second to predictorEnd.first,
                        ),
                        properties = JSONObject().apply {
                            put("label", target.name ?: target.mmsiText ?: "AIS")
                            put("threat", classifyThreat(target))
                            put("predictorMinutes", PREDICTOR_MINUTES)
                        },
                    )
                )
            }

            if (courseDeg != null && speedKn != null && cpaNm != null && cpaTimeMinutes != null && cpaTimeMinutes > 0f) {
                val cpaDistanceNm = speedKn.toDouble() * cpaTimeMinutes.toDouble() / 60.0
                val cpaPoint = destination(
                    lat = lat.toDouble(),
                    lon = lon.toDouble(),
                    bearingDeg = courseDeg.toDouble(),
                    distanceNm = cpaDistanceNm,
                )
                features.put(
                    feature(
                        kind = "ais-cpa-marker",
                        geometryType = "Point",
                        coordinates = pointCoordinates(cpaPoint.second, cpaPoint.first),
                        properties = JSONObject().apply {
                            put("label", "CPA")
                            put("cpaNm", cpaNm)
                            put("cpaTimeMinutes", cpaTimeMinutes)
                            put("threat", classifyThreat(target))
                        },
                    )
                )
                features.put(
                    feature(
                        kind = "ais-cpa-link",
                        geometryType = "LineString",
                        coordinates = lineCoordinates(
                            lon.toDouble() to lat.toDouble(),
                            cpaPoint.second to cpaPoint.first,
                        ),
                        properties = JSONObject().apply {
                            put("label", target.name ?: target.mmsiText ?: "AIS")
                            put("cpaNm", cpaNm)
                            put("cpaTimeMinutes", cpaTimeMinutes)
                            put("threat", classifyThreat(target))
                        },
                    )
                )
            }
        }

        return JSONObject().apply {
            put("type", "FeatureCollection")
            put("features", features)
        }.toString()
    }

    private fun classifyThreat(target: AisTargetData): String {
        val cpa = target.cpaNm ?: return "low"
        val tcpa = target.cpaTimeMinutes ?: return "low"
        if (tcpa < 0) return "low" // moving away
        if (cpa < 0.5f && tcpa < 10f) return "high"
        if (cpa < 1.0f && tcpa < 20f) return "medium"
        return "low"
    }

    private fun ensureSource(style: Style) {
        if (style.getSource(SOURCE_ID) != null) return
        style.addSource(GeoJsonSource(SOURCE_ID, buildGeoJson(emptyList())))
    }

    private fun ensureLayers(style: Style) {
        addLayerIfMissing(style, PREDICTOR_LAYER_ID) {
            LineLayer(PREDICTOR_LAYER_ID, SOURCE_ID).withProperties(
                PropertyFactory.lineColor("#FFD700"),
                PropertyFactory.lineWidth(2.0f),
                PropertyFactory.lineDasharray(arrayOf(3f, 2f)),
            ).withFilter(kindEq("ais-predictor-line"))
        }

        addLayerIfMissing(style, CPA_LINK_LAYER_ID) {
            LineLayer(CPA_LINK_LAYER_ID, SOURCE_ID).withProperties(
                PropertyFactory.lineColor("#FF9800"),
                PropertyFactory.lineWidth(2.5f),
                PropertyFactory.lineDasharray(arrayOf(2f, 2f)),
            ).withFilter(kindEq("ais-cpa-link"))
        }

        addLayerIfMissing(style, CIRCLE_LAYER_ID) {
            CircleLayer(CIRCLE_LAYER_ID, SOURCE_ID).withProperties(
                PropertyFactory.circleRadius(6f),
                PropertyFactory.circleColor(
                    Expression.match(
                        Expression.get("threat"),
                        Expression.literal("#4CAF50"), // default: green (safe)
                        Expression.stop("high", "#F44336"),   // red: approaching
                        Expression.stop("medium", "#FF9800"), // orange: watch
                        Expression.stop("low", "#4CAF50"),    // green: safe
                    )
                ),
                PropertyFactory.circleStrokeWidth(2f),
                PropertyFactory.circleStrokeColor("#FFFFFF"),
            ).withFilter(kindEq("ais-target"))
        }

        addLayerIfMissing(style, CPA_MARKER_LAYER_ID) {
            CircleLayer(CPA_MARKER_LAYER_ID, SOURCE_ID).withProperties(
                PropertyFactory.circleRadius(4.5f),
                PropertyFactory.circleColor("#FFFFFF"),
                PropertyFactory.circleStrokeWidth(2f),
                PropertyFactory.circleStrokeColor("#FF9800"),
            ).withFilter(kindEq("ais-cpa-marker"))
        }

        addLayerIfMissing(style, LABEL_LAYER_ID) {
            SymbolLayer(LABEL_LAYER_ID, SOURCE_ID).withProperties(
                PropertyFactory.textField(Expression.get("label")),
                PropertyFactory.textSize(11f),
                PropertyFactory.textColor("#FFFFFF"),
                PropertyFactory.textHaloColor("#000000"),
                PropertyFactory.textHaloWidth(1.5f),
                PropertyFactory.textOffset(arrayOf(0f, 1.5f)),
                PropertyFactory.textAllowOverlap(false),
            ).withFilter(
                Expression.any(
                    kindEq("ais-target"),
                    kindEq("ais-cpa-marker"),
                )
            )
        }
    }

    private fun addLayerIfMissing(style: Style, layerId: String, factory: () -> org.maplibre.android.style.layers.Layer) {
        if (style.getLayer(layerId) != null) return
        style.addLayer(factory())
    }

    private fun removeLayerSafely(style: Style, layerId: String) {
        try {
            style.removeLayer(layerId)
        } catch (_: Exception) {
        }
    }

    private fun removeSourceSafely(style: Style, sourceId: String) {
        try {
            style.removeSource(sourceId)
        } catch (_: Exception) {
        }
    }

    private fun feature(
        kind: String,
        geometryType: String,
        coordinates: JSONArray,
        properties: JSONObject,
    ): JSONObject {
        return JSONObject().apply {
            put("type", "Feature")
            put("properties", properties.apply { put("kind", kind) })
            put(
                "geometry",
                JSONObject().apply {
                    put("type", geometryType)
                    put("coordinates", coordinates)
                }
            )
        }
    }

    private fun pointCoordinates(lon: Double, lat: Double): JSONArray {
        return JSONArray().apply {
            put(lon)
            put(lat)
        }
    }

    private fun lineCoordinates(start: Pair<Double, Double>, end: Pair<Double, Double>): JSONArray {
        return JSONArray().apply {
            put(pointCoordinates(start.first, start.second))
            put(pointCoordinates(end.first, end.second))
        }
    }

    private fun kindEq(kind: String): Expression =
        Expression.eq(Expression.get("kind"), Expression.literal(kind))

    private fun destination(
        lat: Double,
        lon: Double,
        bearingDeg: Double,
        distanceNm: Double,
    ): Pair<Double, Double> {
        return GeoCalc.destination(
            lat = lat,
            lon = lon,
            bearingDeg = bearingDeg,
            distanceNm = distanceNm,
        )
    }
}
