package com.seafox.nmea_dashboard.ui.widgets

import org.junit.Assert.assertEquals
import org.junit.Test

class SeaChartWidgetSettingsModuleTest {

    @Test
    fun normalizesLegacyOpenSeaMapProviderName() {
        assertEquals(
            SeaChartMapProvider.OPEN_SEA_CHARTS,
            normalizeSeaChartMapProviderName("OPEN_SEA_MAP"),
        )
    }

    @Test
    fun normalizesPersistedCommercialProvidersToSafeFallback() {
        assertEquals(SeaChartMapProvider.NOAA, normalizeSeaChartMapProviderName("C_MAP"))
        assertEquals(SeaChartMapProvider.NOAA, normalizeSeaChartMapProviderName("S63"))
    }

    @Test
    fun normalizesLegacyQmapProviderName() {
        assertEquals(SeaChartMapProvider.QMAP_DE, normalizeSeaChartMapProviderName("QMAP"))
        assertEquals(SeaChartMapProvider.QMAP_DE, normalizeSeaChartMapProviderName("QMAP_DE_ONLINE"))
    }

    @Test
    fun normalizesLegacyOsmProviderNameToOpenSeaCharts() {
        assertEquals(SeaChartMapProvider.OPEN_SEA_CHARTS, normalizeSeaChartMapProviderName("OSM"))
        assertEquals(SeaChartMapProvider.OPEN_SEA_CHARTS, normalizeSeaChartMapProviderName("OSM_FALLBACK"))
        assertEquals(SeaChartMapProvider.OPEN_SEA_CHARTS, normalizeSeaChartMapProviderName("OSM_STANDARD"))
    }

    @Test
    fun parsesLegacyQmapSettingsToSelectableProvider() {
        val settings = parseSeaChartWidgetSettings("""{"mapProvider":"QMAP"}""")

        assertEquals(SeaChartMapProvider.QMAP_DE, settings.mapProvider)
    }

    @Test
    fun normalizesUnknownOrMissingProvidersToSafeFallback() {
        assertEquals(SeaChartMapProvider.NOAA, normalizeSeaChartMapProviderName("NAVIONICS"))
        assertEquals(SeaChartMapProvider.NOAA, normalizeSeaChartMapProviderName(null))
    }
}
