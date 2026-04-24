package com.seafox.nmea_dashboard.ui.widgets.chart

import com.seafox.nmea_dashboard.ui.widgets.SeaChartMapProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FreeRasterChartProvidersTest {

    @Test
    fun qmapDeProvidesOnlineRasterContract() {
        val data = FreeRasterChartProviders.chartDataFor(SeaChartMapProvider.QMAP_DE)

        assertEquals("qmap-de", data?.providerId)
        assertEquals("https://freenauticalchart.net/qmap-de/{z}/{x}/{y}.png", data?.tileUrls?.single())
        assertEquals(8, data?.minZoom)
        assertEquals(16, data?.maxZoom)
        assertTrue(data?.attribution?.contains("QMAP DE") == true)
        assertTrue(FreeRasterChartProviders.hasOnlineBaseLayer(SeaChartMapProvider.QMAP_DE))
        assertEquals(
            "QMAP DE Online / Free Nautical Chart",
            FreeRasterChartProviders.displayLabelFor(SeaChartMapProvider.QMAP_DE),
        )
    }

    @Test
    fun openSeaChartsUseOsmFallbackAndForceSeamarkOverlay() {
        val data = FreeRasterChartProviders.chartDataFor(SeaChartMapProvider.OPEN_SEA_CHARTS)

        assertEquals("osm-standard", data?.providerId)
        assertEquals("https://tile.openstreetmap.org/{z}/{x}/{y}.png", data?.tileUrls?.single())
        assertTrue(data?.attribution?.contains("OpenStreetMap") == true)
        assertTrue(FreeRasterChartProviders.shouldForceSeamarkOverlay(SeaChartMapProvider.OPEN_SEA_CHARTS))
        assertEquals(
            "OSM + OpenSeaMap (nicht fuer Navigation)",
            FreeRasterChartProviders.displayLabelFor(SeaChartMapProvider.OPEN_SEA_CHARTS),
        )
    }

    @Test
    fun noaaDoesNotGetFreeRasterOverride() {
        assertNull(FreeRasterChartProviders.chartDataFor(SeaChartMapProvider.NOAA))
        assertFalse(FreeRasterChartProviders.hasOnlineBaseLayer(SeaChartMapProvider.NOAA))
        assertFalse(FreeRasterChartProviders.shouldForceSeamarkOverlay(SeaChartMapProvider.NOAA))
    }
}
