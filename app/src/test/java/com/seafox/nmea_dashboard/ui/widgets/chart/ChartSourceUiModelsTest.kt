package com.seafox.nmea_dashboard.ui.widgets.chart

import com.seafox.nmea_dashboard.ui.widgets.SeaChartMapProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChartSourceUiModelsTest {

    @Test
    fun qmapOnlineSourceShowsFreeOnlineTruthAndAttribution() {
        val state = ChartSourceUiModels.build(
            mapProvider = SeaChartMapProvider.QMAP_DE,
            activeMapSourceLabel = null,
            activeMapSourcePath = null,
            showOpenSeaMapOverlay = false,
        )

        assertEquals("QMAP DE Online / Free Nautical Chart", state.primaryLabel)
        assertEquals("Online Beta", state.statusLabel)
        assertTrue(state.badges.any { it.label == "FREI" })
        assertTrue(state.badges.any { it.label == "ONLINE" })
        assertTrue(state.attributionLabel?.contains("QMAP DE") == true)
        assertTrue(state.navigationNotice.contains("Nicht fuer Navigation"))
        assertTrue(state.isRenderable)
    }

    @Test
    fun openSeaChartsSourceAlwaysShowsSeamarkOverlayBadge() {
        val state = ChartSourceUiModels.build(
            mapProvider = SeaChartMapProvider.OPEN_SEA_CHARTS,
            activeMapSourceLabel = null,
            activeMapSourcePath = null,
            showOpenSeaMapOverlay = false,
        )

        assertEquals("OSM + OpenSeaMap (nicht fuer Navigation)", state.primaryLabel)
        assertTrue(state.badges.any { it.label == "FREI" })
        assertTrue(state.badges.any { it.label == "OVERLAY" })
        assertTrue(state.attributionLabel?.contains("OpenStreetMap") == true)
        assertTrue(state.attributionLabel?.contains("OpenSeaMap") == true)
        assertTrue(state.isRenderable)
    }

    @Test
    fun localMbtilesImportIsNotPresentedAsLicensedChartProvider() {
        val state = ChartSourceUiModels.build(
            mapProvider = SeaChartMapProvider.NOAA,
            activeMapSourceLabel = "Hamburg Harbor",
            activeMapSourcePath = "/charts/import/hamburg.mbtiles",
            showOpenSeaMapOverlay = true,
        )

        assertEquals("Hamburg Harbor", state.primaryLabel)
        assertEquals("Lokaler Import", state.statusLabel)
        assertEquals("hamburg.mbtiles", state.detailLabel)
        assertTrue(state.badges.any { it.label == "IMPORT" })
        assertTrue(state.badges.any { it.label == "OFFLINE" })
        assertTrue(state.badges.any { it.label == "OVERLAY" })
        assertFalse(state.badges.any { it.label == "LIZENZ" })
        assertTrue(state.isRenderable)
    }

    @Test
    fun validLicensedFirstPartyPackageShowsPremiumButKeepsSafetyNotice() {
        val localPath = "/app/seaCHART/premium/seafox-premium-de-coast.mbtiles"
        val pkg = OfflineChartPackage(
            id = "de-coast",
            displayName = "seaFOX Premium Pack DE Coast",
            providerId = FirstPartyChartPackages.PROVIDER_ID,
            providerType = ChartProviderType.RASTER_TILES,
            format = "Raster MBTiles",
            localPath = localPath,
            licenseStatus = ChartPackageLicenseStatus.licensed,
            validationStatus = ChartPackageValidationStatus.valid,
        )

        val state = ChartSourceUiModels.build(
            mapProvider = SeaChartMapProvider.NOAA,
            activeMapSourceLabel = "Imported label",
            activeMapSourcePath = localPath,
            showOpenSeaMapOverlay = false,
            offlinePackages = listOf(pkg),
        )

        assertEquals("seaFOX Premium Pack DE Coast", state.primaryLabel)
        assertEquals("Premium-Paket aktiv", state.statusLabel)
        assertTrue(state.badges.any { it.label == "PREMIUM" })
        assertTrue(state.navigationNotice.contains("nicht ECDIS-zertifiziert"))
        assertTrue(state.isRenderable)
    }

    @Test
    fun commercialProvidersStayUnavailableWithoutLicenseOrRenderer() {
        val cMap = ChartSourceUiModels.build(
            mapProvider = SeaChartMapProvider.C_MAP,
            activeMapSourceLabel = null,
            activeMapSourcePath = null,
            showOpenSeaMapOverlay = false,
        )
        val s63 = ChartSourceUiModels.build(
            mapProvider = SeaChartMapProvider.S63,
            activeMapSourceLabel = null,
            activeMapSourcePath = null,
            showOpenSeaMapOverlay = false,
        )

        assertEquals("Lizenz erforderlich", cMap.statusLabel)
        assertTrue(cMap.badges.any { it.label == "LIZENZ" })
        assertFalse(cMap.isRenderable)
        assertEquals("Nicht verfuegbar", s63.statusLabel)
        assertTrue(s63.badges.any { it.label == "NICHT FERTIG" })
        assertFalse(s63.isRenderable)
    }
}
