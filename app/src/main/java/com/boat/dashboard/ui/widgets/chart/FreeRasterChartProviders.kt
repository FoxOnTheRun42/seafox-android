package com.seafox.nmea_dashboard.ui.widgets.chart

import android.util.Log
import com.seafox.nmea_dashboard.ui.widgets.SeaChartMapProvider
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.RasterLayer
import org.maplibre.android.style.sources.RasterSource
import org.maplibre.android.style.sources.TileSet

data class FreeRasterChartLayer(
    val providerId: String,
    val displayName: String,
    val sourceId: String,
    val layerId: String,
    val tileSetId: String,
    val tileUrl: String,
    val minZoom: Float,
    val maxZoom: Float,
    val opacity: Float,
    val attribution: String,
    val userNotice: String,
    val navigationDisclaimer: String,
) {
    fun toChartData(): ChartData.RasterTiles = ChartData.RasterTiles(
        providerId = providerId,
        sourceId = sourceId,
        tileUrls = listOf(tileUrl),
        minZoom = minZoom.toInt(),
        maxZoom = maxZoom.toInt(),
        opacity = opacity,
        attribution = attribution,
    )
}

/**
 * First concrete free raster-provider bridge.
 *
 * This keeps Task 01 small and honest: QMAP DE and OSM are online basemap
 * raster sources, while OpenSeaMap remains a seamark overlay on top.
 */
object FreeRasterChartProviders {

    private const val TAG = "FreeRasterCharts"
    private const val SOURCE_ID = "seafox-free-raster-source"
    private const val LAYER_ID = "seafox-free-raster-layer"

    private val BASEMAP_ANCHORS = listOf(
        OpenSeaMapOverlay.LAYER_ID,
        "nautical-depth-areas",
        "nautical-depth-contours",
        "nav-track-line",
        "nav-heading-line",
        "nav-cog-vector",
        "nav-boat-icon",
        "ais-targets-circle",
    )

    val qmapDe: FreeRasterChartLayer = FreeRasterChartLayer(
        providerId = "qmap-de",
        displayName = "QMAP DE Online / Free Nautical Chart",
        sourceId = SOURCE_ID,
        layerId = LAYER_ID,
        tileSetId = "qmap-de-online",
        tileUrl = "https://freenauticalchart.net/qmap-de/{z}/{x}/{y}.png",
        minZoom = 8f,
        maxZoom = 16f,
        opacity = 1.0f,
        attribution = "QMAP DE / Free Nautical Chart, BSH-derived open data",
        userNotice = "QMAP DE ist eine freie Online-Rasterkarte fuer deutsche Gewaesser.",
        navigationDisclaimer = "Nicht fuer Navigation; nur Information, Referenz und Training.",
    )

    val osmFallback: FreeRasterChartLayer = FreeRasterChartLayer(
        providerId = "osm-standard",
        displayName = "OSM + OpenSeaMap (nicht fuer Navigation)",
        sourceId = SOURCE_ID,
        layerId = LAYER_ID,
        tileSetId = "osm-standard-online",
        tileUrl = "https://tile.openstreetmap.org/{z}/{x}/{y}.png",
        minZoom = 0f,
        maxZoom = 19f,
        opacity = 1.0f,
        attribution = "© OpenStreetMap contributors",
        userNotice = "OpenStreetMap ist nur ein Online-Fallback fuer Land-/Orientierungsdaten.",
        navigationDisclaimer = "Kein Seekarten- oder Navigationsprovider; kein Offline-Prefetch.",
    )

    fun baseLayerFor(provider: SeaChartMapProvider): FreeRasterChartLayer? {
        return when (provider) {
            SeaChartMapProvider.QMAP_DE -> qmapDe
            SeaChartMapProvider.OPEN_SEA_CHARTS -> osmFallback
            SeaChartMapProvider.NOAA,
            SeaChartMapProvider.S57,
            SeaChartMapProvider.S63,
            SeaChartMapProvider.C_MAP -> null
        }
    }

    fun hasOnlineBaseLayer(provider: SeaChartMapProvider): Boolean {
        return baseLayerFor(provider) != null
    }

    fun shouldForceSeamarkOverlay(provider: SeaChartMapProvider): Boolean {
        return provider == SeaChartMapProvider.OPEN_SEA_CHARTS
    }

    fun displayLabelFor(provider: SeaChartMapProvider): String? {
        return when (provider) {
            SeaChartMapProvider.OPEN_SEA_CHARTS -> osmFallback.displayName
            else -> baseLayerFor(provider)?.displayName
        }
    }

    fun chartDataFor(provider: SeaChartMapProvider): ChartData.RasterTiles? {
        return baseLayerFor(provider)?.toChartData()
    }

    fun updateBaseLayer(
        style: Style,
        provider: SeaChartMapProvider,
    ): Boolean {
        val layer = baseLayerFor(provider)
        if (layer == null) {
            removeBaseLayer(style)
            return false
        }

        removeBaseLayer(style)
        val tileSet = TileSet(layer.tileSetId, layer.tileUrl).apply {
            minZoom = layer.minZoom
            maxZoom = layer.maxZoom
            attribution = layer.attribution
        }
        style.addSource(RasterSource(layer.sourceId, tileSet, 256))
        val rasterLayer = RasterLayer(layer.layerId, layer.sourceId).withProperties(
            PropertyFactory.rasterOpacity(layer.opacity.coerceIn(0f, 1f)),
        )
        val anchorLayerId = BASEMAP_ANCHORS.firstOrNull { anchor ->
            style.getLayer(anchor) != null
        }
        if (anchorLayerId != null) {
            style.addLayerBelow(rasterLayer, anchorLayerId)
        } else {
            style.addLayer(rasterLayer)
        }
        Log.d(TAG, "Applied free raster basemap: ${layer.providerId}")
        return true
    }

    fun removeBaseLayer(style: Style) {
        try {
            style.removeLayer(LAYER_ID)
        } catch (_: Exception) {
        }
        try {
            style.removeSource(SOURCE_ID)
        } catch (_: Exception) {
        }
    }
}
