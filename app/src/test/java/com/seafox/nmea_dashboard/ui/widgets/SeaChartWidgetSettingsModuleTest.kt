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
    fun normalizesUnknownOrMissingProvidersToSafeFallback() {
        assertEquals(SeaChartMapProvider.NOAA, normalizeSeaChartMapProviderName("NAVIONICS"))
        assertEquals(SeaChartMapProvider.NOAA, normalizeSeaChartMapProviderName(null))
    }
}
