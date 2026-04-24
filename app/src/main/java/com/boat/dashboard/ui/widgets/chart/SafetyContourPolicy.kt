package com.seafox.nmea_dashboard.ui.widgets.chart

import kotlin.math.max

object SafetyContourPolicy {
    private const val DEFAULT_SAFETY_MARGIN_METERS = 1.0
    private val DEPTH_OBJECT_CODES = setOf("DEPARE", "DEPCNT", "SOUNDG", "DRGARE", "SBDARE", "SWPARE")

    fun calculateSafetyDepthMeters(
        draftMeters: Double?,
        marginMeters: Double?,
        configuredSafetyDepthMeters: Double?,
    ): Double {
        val draft = draftMeters?.takeIf { it.isFinite() && it >= 0.0 } ?: 0.0
        val margin = marginMeters?.takeIf { it.isFinite() && it >= 0.0 } ?: DEFAULT_SAFETY_MARGIN_METERS
        val configured = configuredSafetyDepthMeters?.takeIf { it.isFinite() && it >= 0.0 }
        return max(draft + margin, configured ?: 0.0)
    }

    fun isShallowDepth(depthMeters: Double, safetyDepthMeters: Double): Boolean {
        return depthMeters.isFinite() &&
            safetyDepthMeters.isFinite() &&
            depthMeters <= safetyDepthMeters
    }

    fun isDepthObjectCode(objectCode: String, kind: String = ""): Boolean {
        return objectCode.trim().uppercase() in DEPTH_OBJECT_CODES ||
            kind.trim().uppercase() in DEPTH_OBJECT_CODES
    }

    fun shouldKeepDepthFeature(
        objectCode: String,
        kind: String = "",
        depthMeters: Double?,
        safetyDepthMeters: Double,
    ): Boolean {
        if (!safetyDepthMeters.isFinite()) return false
        if (!isDepthObjectCode(objectCode, kind) && depthMeters == null) return false
        return depthMeters?.let { isShallowDepth(it, safetyDepthMeters) } ?: true
    }
}
