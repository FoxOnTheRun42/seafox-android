package com.seafox.nmea_dashboard.ui.widgets.chart

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoCalcTest {

    @Test
    fun oneLatitudeDegreeIsAboutSixtyNauticalMiles() {
        val distance = GeoCalc.distanceNm(54.0, 10.0, 55.0, 10.0)

        assertEquals(60.0, distance, 0.3)
    }

    @Test
    fun bearingEastAlongSameLatitudeIsAboutNinetyDegrees() {
        val bearing = GeoCalc.bearingDeg(54.0, 10.0, 54.0, 11.0)

        assertEquals(89.6, bearing, 0.5)
    }

    @Test
    fun destinationOneNauticalMileNorthChangesLatitudeByOneMinute() {
        val (lat, lon) = GeoCalc.destination(54.0, 10.0, 0.0, 1.0)

        assertEquals(54.0 + (1.0 / 60.0), lat, 0.0005)
        assertEquals(10.0, lon, 0.0005)
    }

    @Test
    fun vmgIsPositiveWhenCoursePointsTowardWaypoint() {
        val vmg = GeoCalc.vmgKn(sogKn = 6.0f, cogDeg = 45.0f, bearingToWptDeg = 45.0f)

        assertEquals(6.0f, vmg, 0.001f)
    }

    @Test
    fun cpaForStationaryRelativeMotionIsCurrentSeparation() {
        val cpa = GeoCalc.computeCpa(
            lat1 = 54.0,
            lon1 = 10.0,
            cog1 = 0.0,
            sog1 = 0.0,
            lat2 = 54.0,
            lon2 = 10.1,
            cog2 = 0.0,
            sog2 = 0.0,
        )

        assertTrue(cpa.cpaNm > 3.5)
        assertEquals(0.0, cpa.tcpaMinutes, 0.001)
    }
}
