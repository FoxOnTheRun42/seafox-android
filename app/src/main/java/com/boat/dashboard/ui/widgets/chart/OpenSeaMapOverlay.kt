package com.seafox.nmea_dashboard.ui.widgets.chart

import android.util.Log
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.RasterLayer
import org.maplibre.android.style.sources.RasterSource
import org.maplibre.android.style.sources.TileSet

/**
 * Small, idempotent MapLibre helper for the OpenSeaMap seamark raster overlay.
 */
object OpenSeaMapOverlay {

    private const val TAG = "OpenSeaMapOverlay"

    const val SOURCE_ID = "openseamap-seamark-source"
    const val LAYER_ID = "openseamap-seamark-layer"
    const val TILE_URL = "https://tiles.openseamap.org/seamark/{z}/{x}/{y}.png"
    private const val TILESET_ID = "openseamap-seamark"

    private const val DEFAULT_OPACITY = 0.85f

    // Ordered from lowest to highest so the first match stays under overlays.
    private val OVERLAY_ANCHORS = listOf(
        "nav-track-line",
        "nav-heading-line",
        "nav-cog-vector",
        "nav-predictor",
        "nav-course-line",
        "nav-predictor-marks",
        "nav-predictor-labels",
        "nav-boat-icon",
        "ais-targets-circle",
    )

    /**
     * Creates the source/layer pair if needed and keeps them on top of the base map.
     */
    fun setup(
        style: Style,
        opacity: Float = DEFAULT_OPACITY,
    ): Boolean {
        return update(style, enabled = true, opacity = opacity)
    }

    /**
     * Reconciles the overlay with the current style.
     *
     * - if disabled, the overlay is removed
     * - if enabled, the source is ensured and the layer is re-added in the best available slot
     * - repeated calls stay idempotent
     */
    fun update(
        style: Style,
        enabled: Boolean = true,
        opacity: Float = DEFAULT_OPACITY,
    ): Boolean {
        if (!enabled) {
            remove(style)
            return false
        }

        ensureSource(style)
        rebuildLayer(style, opacity.coerceIn(0f, 1f))

        val applied = style.getSource(SOURCE_ID) != null && style.getLayer(LAYER_ID) != null
        if (applied) {
            Log.d(TAG, "OpenSeaMap seamark overlay applied")
        }
        return applied
    }

    /**
     * Removes the overlay cleanly from the style.
     */
    fun remove(style: Style) {
        try {
            style.removeLayer(LAYER_ID)
        } catch (_: Exception) {
        }

        try {
            style.removeSource(SOURCE_ID)
        } catch (_: Exception) {
        }
    }

    private fun ensureSource(style: Style) {
        if (style.getSource(SOURCE_ID) != null) {
            return
        }

        val tileSet = TileSet(TILESET_ID, TILE_URL).apply {
            minZoom = 1f
            maxZoom = 18f
        }

        style.addSource(RasterSource(SOURCE_ID, tileSet, 256))
    }

    private fun rebuildLayer(style: Style, opacity: Float) {
        try {
            style.removeLayer(LAYER_ID)
        } catch (_: Exception) {
        }

        val layer = RasterLayer(LAYER_ID, SOURCE_ID).withProperties(
            PropertyFactory.rasterOpacity(opacity),
        )

        val anchorLayerId = findAnchorLayerId(style)
        if (anchorLayerId != null) {
            style.addLayerBelow(layer, anchorLayerId)
        } else {
            style.addLayer(layer)
        }
    }

    private fun findAnchorLayerId(style: Style): String? {
        return OVERLAY_ANCHORS.firstOrNull { anchorId ->
            style.getLayer(anchorId) != null
        }
    }
}
