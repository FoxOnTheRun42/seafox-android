package com.seafox.nmea_dashboard.ui.widgets.chart

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SeaChartSideLoadPackagesTest {

    @Test
    fun acceptsOnlySideLoadChartPackageExtensions() {
        assertTrue(SeaChartSideLoadPackages.isAcceptedFileName("harbour.mbtiles"))
        assertTrue(SeaChartSideLoadPackages.isAcceptedFileName("coast.gpkg"))
        assertTrue(SeaChartSideLoadPackages.isAcceptedFileName("coast.geopackage"))

        assertFalse(SeaChartSideLoadPackages.isAcceptedFileName("route.gpx"))
        assertFalse(SeaChartSideLoadPackages.isAcceptedFileName("archive.zip"))
        assertFalse(SeaChartSideLoadPackages.isAcceptedFileName("soundings.000"))
    }

    @Test
    fun targetFileNameKeepsExtensionAndSanitizesUserNames() {
        assertEquals(
            "North_Sea_2026.mbtiles",
            SeaChartSideLoadPackages.targetFileName("North Sea 2026.mbtiles"),
        )
        assertEquals(
            "QMAP_DE_Test.gpkg",
            SeaChartSideLoadPackages.targetFileName("QMAP DE/Test.gpkg"),
        )
    }

    @Test
    fun regionFolderNameIsStableAndClearlySideLoaded() {
        assertEquals(
            "sideload_North_Sea_2026",
            SeaChartSideLoadPackages.regionFolderName("North Sea 2026.mbtiles"),
        )
        assertEquals(
            "sideload_seachart",
            SeaChartSideLoadPackages.regionFolderName("..."),
        )
    }
}
