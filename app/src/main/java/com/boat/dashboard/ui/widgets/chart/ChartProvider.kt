package com.seafox.nmea_dashboard.ui.widgets.chart

import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.Style

/**
 * Unified abstraction for all chart sources in seaFOX.
 *
 * Providers can expose raster tiles, vector tiles, GeoJSON layers or a direct
 * style JSON payload. The interface intentionally stays small so providers can
 * be implemented independently without coupling to dashboard state.
 */
interface ChartProvider {
    val id: String
    val displayName: String
    val type: ChartProviderType
    val capabilities: Set<ChartProviderCapability>
        get() = when (type) {
            ChartProviderType.RASTER_TILES -> setOf(ChartProviderCapability.rasterTiles)
            ChartProviderType.VECTOR_TILES -> setOf(ChartProviderCapability.vectorTiles)
            ChartProviderType.GEOJSON -> emptySet()
            ChartProviderType.STYLE_JSON -> emptySet()
        }

    suspend fun hasCoverage(bounds: LatLngBounds, zoom: Int): Boolean

    suspend fun loadForRegion(bounds: LatLngBounds, zoom: Int): ChartData

    @Suppress("UNUSED_PARAMETER")
    fun applyToStyle(style: Style, data: ChartData) {
        // Optional for providers that are resolved elsewhere.
    }

    @Suppress("UNUSED_PARAMETER")
    fun removeFromStyle(style: Style) {
        // Optional for lightweight providers that only apply transient sources.
    }

    suspend fun listOfflinePackages(): List<OfflineChartPackage> = emptyList()

    @Suppress("UNUSED_PARAMETER")
    suspend fun downloadPackage(
        pkg: OfflineChartPackage,
        progress: (Float) -> Unit,
    ) {
        throw UnsupportedOperationException(
            "Provider $id does not support offline package downloads."
        )
    }
}
