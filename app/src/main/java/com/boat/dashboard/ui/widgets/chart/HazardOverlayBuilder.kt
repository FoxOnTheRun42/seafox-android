package com.seafox.nmea_dashboard.ui.widgets.chart

import org.json.JSONArray
import org.json.JSONObject

object HazardOverlayBuilder {

    fun buildLaylineFeatures(
        ownLat: Double? = null,
        ownLon: Double? = null,
        targetWaypoint: Waypoint? = null,
        windDirectionDeg: Double? = null,
        settings: NavigationVectorSettings = NavigationVectorSettings(),
    ): JSONArray {
        return NavHazardOverlayBuilder.buildLaylineFeatures(
            NavHazardOverlayBuilder.LaylineRequest(
                ownLat = ownLat,
                ownLon = ownLon,
                targetWaypoint = targetWaypoint,
                windDirectionDeg = windDirectionDeg,
                settings = settings,
            )
        )
    }

    fun buildGuardZoneFeatures(
        ownLat: Double? = null,
        ownLon: Double? = null,
        settings: NavigationVectorSettings = NavigationVectorSettings(),
        alarmActive: Boolean? = null,
    ): JSONArray {
        return NavHazardOverlayBuilder.buildGuardZoneFeatures(
            NavHazardOverlayBuilder.GuardZoneRequest(
                centerLat = ownLat,
                centerLon = ownLon,
                settings = settings,
                alarmActive = alarmActive,
            )
        )
    }

    fun buildMobFeatures(
        ownLat: Double? = null,
        ownLon: Double? = null,
        mobLat: Double? = null,
        mobLon: Double? = null,
        mobTimestampMs: Long? = null,
        label: String = "MOB",
    ): JSONArray {
        return NavHazardOverlayBuilder.buildMobFeatures(
            NavHazardOverlayBuilder.MobRequest(
                ownLat = ownLat,
                ownLon = ownLon,
                mobLat = mobLat,
                mobLon = mobLon,
                mobTimestampMs = mobTimestampMs,
                label = label,
            )
        )
    }

    fun buildSafetyContourContractFeatures(
        ownLat: Double? = null,
        ownLon: Double? = null,
        settings: NavigationVectorSettings = NavigationVectorSettings(),
        draftMeters: Double? = null,
        safetyMarginMeters: Double? = null,
        contourDepthMeters: Double? = null,
        label: String = "Safety contour",
        source: String = "placeholder",
        active: Boolean = true,
    ): JSONArray {
        return NavHazardOverlayBuilder.buildSafetyContourContractFeatures(
            NavHazardOverlayBuilder.SafetyContourRequest(
                ownLat = ownLat,
                ownLon = ownLon,
                settings = settings,
                draftMeters = draftMeters,
                safetyMarginMeters = safetyMarginMeters,
                contourDepthMeters = contourDepthMeters,
                label = label,
                source = source,
                active = active,
            )
        )
    }

    fun buildFeatureCollection(vararg features: JSONObject): JSONObject {
        return NavHazardOverlayBuilder.buildFeatureCollection(*features)
    }

    fun buildFeatureCollection(features: Iterable<JSONObject>): JSONObject {
        return NavHazardOverlayBuilder.buildFeatureCollection(features)
    }

    fun buildFeatureCollectionFromArrays(featureArrays: Iterable<JSONArray>): JSONObject {
        return NavHazardOverlayBuilder.buildFeatureCollectionFromArrays(featureArrays)
    }

    fun filterDepthAreaFeatures(
        features: JSONArray,
        safetyDepthMeters: Double,
    ): JSONArray {
        val filtered = JSONArray()
        if (!safetyDepthMeters.isFinite()) return filtered

        for (index in 0 until features.length()) {
            val feature = features.optJSONObject(index) ?: continue
            if (shouldKeepDepthAreaFeature(feature, safetyDepthMeters)) {
                filtered.put(feature)
            }
        }
        return filtered
    }

    fun filterDepthAreaFeatures(
        features: Iterable<JSONObject>,
        safetyDepthMeters: Double,
    ): List<JSONObject> {
        if (!safetyDepthMeters.isFinite()) return emptyList()
        val filtered = mutableListOf<JSONObject>()
        for (feature in features) {
            if (shouldKeepDepthAreaFeature(feature, safetyDepthMeters)) {
                filtered.add(feature)
            }
        }
        return filtered
    }

    fun shouldKeepDepthAreaFeature(
        feature: JSONObject,
        safetyDepthMeters: Double,
    ): Boolean {
        if (!safetyDepthMeters.isFinite() || !isDepthAreaLike(feature)) {
            return false
        }

        val shallowestDepthMeters = firstNumeric(
            feature,
            "DRVAL1",
            "VALDCO",
            "VALSOU",
            "minDepth",
            "depthMin",
            "depthMeters",
            "depth",
            "contourDepthMeters",
            "soundingDepthMeters",
        ) ?: firstNumeric(
            feature,
            "DRVAL2",
            "maxDepth",
        )

        return SafetyContourPolicy.shouldKeepDepthFeature(
            objectCode = feature.optString("objCode"),
            kind = feature.optString("kind"),
            depthMeters = shallowestDepthMeters,
            safetyDepthMeters = safetyDepthMeters,
        )
    }

    fun shouldKeepDepthAreaFeature(
        feature: JSONObject,
        settings: NavigationVectorSettings,
    ): Boolean {
        if (!settings.showSafetyContour) return false
        return shouldKeepDepthAreaFeature(feature, settings.safetyDepthMeters.toDouble())
    }

    private fun isDepthAreaLike(feature: JSONObject): Boolean {
        val objectCode = feature.optString("objCode").uppercase()
        val kind = feature.optString("kind").uppercase()

        return SafetyContourPolicy.isDepthObjectCode(objectCode, kind) ||
            feature.has("DRVAL1") ||
            feature.has("DRVAL2") ||
            feature.has("VALDCO") ||
            feature.has("VALSOU") ||
            feature.has("depthMeters") ||
            feature.has("depth") ||
            feature.has("contourDepthMeters") ||
            feature.has("soundingDepthMeters") ||
            feature.has("minDepth") ||
            feature.has("maxDepth")
    }

    private fun firstNumeric(feature: JSONObject, vararg keys: String): Double? {
        for (key in keys) {
            val value = feature.opt(key) ?: continue
            when (value) {
                is Number -> if (value.toDouble().isFinite()) return value.toDouble()
                is String -> value.toDoubleOrNull()?.takeIf { it.isFinite() }?.let { return it }
            }
        }
        return null
    }
}
