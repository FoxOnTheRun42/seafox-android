package com.seafox.nmea_dashboard.ui.widgets.chart

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

object GeoCalc {

    const val EARTH_RADIUS_NM = 3440.065

    fun distanceNm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val phi1 = lat1.toRadians()
        val phi2 = lat2.toRadians()
        val deltaPhi = (lat2 - lat1).toRadians()
        val deltaLambda = (lon2 - lon1).toRadians()

        val a = sin(deltaPhi / 2.0) * sin(deltaPhi / 2.0) +
            cos(phi1) * cos(phi2) * sin(deltaLambda / 2.0) * sin(deltaLambda / 2.0)
        val c = 2.0 * atan2(sqrt(a), sqrt(1.0 - a))
        return EARTH_RADIUS_NM * c
    }

    fun bearingDeg(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val phi1 = lat1.toRadians()
        val phi2 = lat2.toRadians()
        val deltaLambda = (lon2 - lon1).toRadians()

        val y = sin(deltaLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)
        return normalizeDegrees(atan2(y, x).toDegrees())
    }

    fun destination(lat: Double, lon: Double, bearingDeg: Double, distanceNm: Double): Pair<Double, Double> {
        val angularDistance = distanceNm / EARTH_RADIUS_NM
        val bearing = bearingDeg.toRadians()
        val phi1 = lat.toRadians()
        val lambda1 = lon.toRadians()

        val sinPhi2 = sin(phi1) * cos(angularDistance) +
            cos(phi1) * sin(angularDistance) * cos(bearing)
        val phi2 = asin(sinPhi2.coerceIn(-1.0, 1.0))
        val lambda2 = lambda1 + atan2(
            sin(bearing) * sin(angularDistance) * cos(phi1),
            cos(angularDistance) - sin(phi1) * sin(phi2),
        )

        return phi2.toDegrees() to normalizeLongitude(lambda2.toDegrees())
    }

    fun crossTrackNm(
        boatLat: Double,
        boatLon: Double,
        legStartLat: Double,
        legStartLon: Double,
        legEndLat: Double,
        legEndLon: Double,
    ): Double {
        val distance13 = distanceNm(legStartLat, legStartLon, boatLat, boatLon) / EARTH_RADIUS_NM
        val bearing13 = bearingDeg(legStartLat, legStartLon, boatLat, boatLon).toRadians()
        val bearing12 = bearingDeg(legStartLat, legStartLon, legEndLat, legEndLon).toRadians()
        return asin((sin(distance13) * sin(bearing13 - bearing12)).coerceIn(-1.0, 1.0)) * EARTH_RADIUS_NM
    }

    fun alongTrackNm(
        boatLat: Double,
        boatLon: Double,
        legStartLat: Double,
        legStartLon: Double,
        legEndLat: Double,
        legEndLon: Double,
    ): Double {
        val distance13 = distanceNm(legStartLat, legStartLon, boatLat, boatLon) / EARTH_RADIUS_NM
        val crossTrack = crossTrackNm(
            boatLat = boatLat,
            boatLon = boatLon,
            legStartLat = legStartLat,
            legStartLon = legStartLon,
            legEndLat = legEndLat,
            legEndLon = legEndLon,
        ) / EARTH_RADIUS_NM
        return acosSafe(cos(distance13) / max(1e-9, cos(crossTrack))) * EARTH_RADIUS_NM
    }

    fun vmgKn(sogKn: Float, cogDeg: Float, bearingToWptDeg: Float): Float {
        val delta = normalizeSignedDegrees(cogDeg.toDouble() - bearingToWptDeg.toDouble())
        return (sogKn * cos(delta.toRadians())).toFloat()
    }

    fun computeCpa(
        lat1: Double,
        lon1: Double,
        cog1: Double,
        sog1: Double,
        lat2: Double,
        lon2: Double,
        cog2: Double,
        sog2: Double,
    ): CpaResult {
        val referenceLat = ((lat1 + lat2) / 2.0).toRadians()
        val dx = (lon2 - lon1) * 60.0 * cos(referenceLat)
        val dy = (lat2 - lat1) * 60.0

        val vx1 = sin(cog1.toRadians()) * sog1
        val vy1 = cos(cog1.toRadians()) * sog1
        val vx2 = sin(cog2.toRadians()) * sog2
        val vy2 = cos(cog2.toRadians()) * sog2

        val relVx = vx2 - vx1
        val relVy = vy2 - vy1
        val relSpeedSquared = relVx * relVx + relVy * relVy

        val tcpaHours = if (relSpeedSquared <= 1e-9) {
            0.0
        } else {
            max(0.0, -((dx * relVx) + (dy * relVy)) / relSpeedSquared)
        }

        val cpaDx = dx + relVx * tcpaHours
        val cpaDy = dy + relVy * tcpaHours
        val ownFutureDx = vx1 * tcpaHours
        val ownFutureDy = vy1 * tcpaHours
        val cpaLat = lat1 + ownFutureDy / 60.0
        val cpaLon = lon1 + ownFutureDx / (60.0 * max(0.01, cos(lat1.toRadians())))

        return CpaResult(
            cpaNm = sqrt(cpaDx * cpaDx + cpaDy * cpaDy),
            tcpaMinutes = tcpaHours * 60.0,
            cpaLat = cpaLat,
            cpaLon = cpaLon,
        )
    }

    data class CpaResult(
        val cpaNm: Double,
        val tcpaMinutes: Double,
        val cpaLat: Double,
        val cpaLon: Double,
    )

    private fun normalizeDegrees(value: Double): Double {
        var normalized = value % 360.0
        if (normalized < 0.0) normalized += 360.0
        return normalized
    }

    private fun normalizeSignedDegrees(value: Double): Double {
        var normalized = normalizeDegrees(value)
        if (normalized > 180.0) normalized -= 360.0
        return normalized
    }

    private fun normalizeLongitude(value: Double): Double {
        var normalized = value
        while (normalized > 180.0) normalized -= 360.0
        while (normalized < -180.0) normalized += 360.0
        return normalized
    }

    private fun acosSafe(value: Double): Double = kotlin.math.acos(min(1.0, max(-1.0, value)))

    private fun Double.toRadians(): Double = this * PI / 180.0

    private fun Double.toDegrees(): Double = this * 180.0 / PI
}
