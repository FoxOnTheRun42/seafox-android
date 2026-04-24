package com.seafox.nmea_dashboard.ui.widgets.chart

import org.maplibre.android.geometry.LatLngBounds

/**
 * Generic chart payload container for the provider abstraction layer.
 *
 * The sealed structure keeps the API flexible while still being explicit about
 * the core chart delivery modes we support in seaFOX.
 */
sealed interface ChartData {

    val providerId: String
    val cacheKey: String? get() = null

    data class RasterTiles(
        override val providerId: String,
        val sourceId: String,
        val tileUrls: List<String>,
        val tileSizePx: Int = 256,
        val minZoom: Int? = null,
        val maxZoom: Int? = null,
        val opacity: Float = 1f,
        val coverageBounds: LatLngBounds? = null,
        val attribution: String? = null,
        override val cacheKey: String? = null,
    ) : ChartData

    data class VectorTiles(
        override val providerId: String,
        val sourceId: String,
        val tileUrls: List<String>,
        val sourceLayerIds: List<String> = emptyList(),
        val minZoom: Int? = null,
        val maxZoom: Int? = null,
        val coverageBounds: LatLngBounds? = null,
        val attribution: String? = null,
        override val cacheKey: String? = null,
    ) : ChartData

    data class GeoJson(
        override val providerId: String,
        val sourceId: String,
        val geoJson: String,
        val layerPrefix: String? = null,
        val coverageBounds: LatLngBounds? = null,
        override val cacheKey: String? = null,
    ) : ChartData

    data class StyleJson(
        override val providerId: String,
        val styleJson: String,
        val coverageBounds: LatLngBounds? = null,
        override val cacheKey: String? = null,
    ) : ChartData

    object Empty : ChartData {
        override val providerId: String = ""
    }
}
