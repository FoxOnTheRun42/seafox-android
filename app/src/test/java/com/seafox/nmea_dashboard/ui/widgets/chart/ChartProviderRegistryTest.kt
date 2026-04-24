package com.seafox.nmea_dashboard.ui.widgets.chart

import com.seafox.nmea_dashboard.data.FeatureAvailability
import com.seafox.nmea_dashboard.ui.widgets.SeaChartMapProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChartProviderRegistryTest {

    @Test
    fun selectableProvidersOnlyExposeImplementedOrBetaSources() {
        val selectable = ChartProviderRegistry.selectableProviders()

        assertTrue(SeaChartMapProvider.NOAA in selectable)
        assertTrue(SeaChartMapProvider.QMAP_DE in selectable)
        assertTrue(SeaChartMapProvider.S57 in selectable)
        assertTrue(SeaChartMapProvider.OPEN_SEA_CHARTS in selectable)
        assertFalse(SeaChartMapProvider.C_MAP in selectable)
        assertFalse(SeaChartMapProvider.S63 in selectable)
    }

    @Test
    fun selectableProvidersMatchRegistryAvailabilityContract() {
        val selectable = ChartProviderRegistry.selectableProviders()

        selectable.forEach { provider ->
            assertTrue(ChartProviderRegistry.descriptor(provider).canSelect)
        }
        SeaChartMapProvider.entries
            .map { provider -> ChartProviderRegistry.normalizedSelectableProvider(provider) }
            .forEach { normalizedProvider ->
                assertTrue(normalizedProvider in selectable)
            }
    }

    @Test
    fun cMapIsLicenseGatedUntilEntitlementsExist() {
        val descriptor = ChartProviderRegistry.descriptor(SeaChartMapProvider.C_MAP)

        assertEquals(FeatureAvailability.licensedRequired, descriptor.availability)
        assertFalse(descriptor.canSelect)
        assertTrue(ChartProviderCapability.vectorTiles in descriptor.capabilities)
    }

    @Test
    fun s63IsMarkedNotImplementedInsteadOfSelectable() {
        val descriptor = ChartProviderRegistry.descriptor(SeaChartMapProvider.S63)

        assertEquals(FeatureAvailability.notImplemented, descriptor.availability)
        assertFalse(descriptor.canSelect)
        assertTrue(ChartProviderCapability.s63Encrypted in descriptor.capabilities)
    }

    @Test
    fun noaaBetaAdvertisesEncAndOfflineCapabilities() {
        val descriptor = ChartProviderRegistry.descriptor(SeaChartMapProvider.NOAA)

        assertEquals(FeatureAvailability.beta, descriptor.availability)
        assertTrue(ChartProviderCapability.s57Enc in descriptor.capabilities)
        assertTrue(ChartProviderCapability.offlinePackages in descriptor.capabilities)
    }

    @Test
    fun qmapDeBetaAdvertisesRasterTilesOnly() {
        val descriptor = ChartProviderRegistry.descriptor(SeaChartMapProvider.QMAP_DE)

        assertEquals(FeatureAvailability.beta, descriptor.availability)
        assertTrue(descriptor.canSelect)
        assertTrue(ChartProviderCapability.rasterTiles in descriptor.capabilities)
        assertFalse(ChartProviderCapability.s57Enc in descriptor.capabilities)
        assertFalse(ChartProviderCapability.s63Encrypted in descriptor.capabilities)
    }

    @Test
    fun openSeaChartsAdvertisesRasterOverlayContract() {
        val descriptor = ChartProviderRegistry.descriptor(SeaChartMapProvider.OPEN_SEA_CHARTS)

        assertEquals(FeatureAvailability.available, descriptor.availability)
        assertTrue(descriptor.canSelect)
        assertTrue(ChartProviderCapability.rasterTiles in descriptor.capabilities)
        assertTrue(ChartProviderCapability.offlinePackages in descriptor.capabilities)
    }

    @Test
    fun normalizesUnselectableCommercialProvidersToSafeFallback() {
        assertEquals(
            SeaChartMapProvider.NOAA,
            ChartProviderRegistry.normalizedSelectableProvider(SeaChartMapProvider.C_MAP),
        )
        assertEquals(
            SeaChartMapProvider.NOAA,
            ChartProviderRegistry.normalizedSelectableProvider(SeaChartMapProvider.S63),
        )
        assertEquals(
            SeaChartMapProvider.OPEN_SEA_CHARTS,
            ChartProviderRegistry.normalizedSelectableProvider(SeaChartMapProvider.OPEN_SEA_CHARTS),
        )
    }
}
