package com.seafox.nmea_dashboard.ui.widgets.chart.s57

import com.seafox.nmea_dashboard.ui.widgets.chart.ChartData
import com.seafox.nmea_dashboard.ui.widgets.chart.ChartProvider
import com.seafox.nmea_dashboard.ui.widgets.chart.ChartProviderCapability
import com.seafox.nmea_dashboard.ui.widgets.chart.ChartProviderType
import com.seafox.nmea_dashboard.ui.widgets.chart.GeoBounds
import com.seafox.nmea_dashboard.ui.widgets.chart.NauticalOverlay
import com.seafox.nmea_dashboard.ui.widgets.chart.NauticalOverlayOptions
import com.seafox.nmea_dashboard.ui.widgets.chart.S57CellSelector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.Style
import java.io.File

/**
 * Thin ChartProvider skeleton for plain S-57 ENC cells.
 *
 * This intentionally wraps the existing GeoJSON converter and NauticalOverlay
 * instead of claiming a complete S-52/oeSENC renderer stack.
 */
class S57ChartProvider(
    private val sourcePath: String,
    private val overlayOptions: NauticalOverlayOptions = NauticalOverlayOptions(),
) : ChartProvider {
    override val id: String = "s57-plain"
    override val displayName: String = "S-57 ENC Beta"
    override val type: ChartProviderType = ChartProviderType.GEOJSON
    override val capabilities: Set<ChartProviderCapability> = setOf(
        ChartProviderCapability.s57Enc,
        ChartProviderCapability.offlinePackages,
    )

    override suspend fun hasCoverage(bounds: LatLngBounds, zoom: Int): Boolean {
        return withContext(Dispatchers.IO) {
            selectedCells(bounds, zoom).isNotEmpty()
        }
    }

    override suspend fun loadForRegion(bounds: LatLngBounds, zoom: Int): ChartData {
        return withContext(Dispatchers.IO) {
            val cells = selectedCells(bounds, zoom)
            val geoJson = if (cells.isEmpty()) {
                EMPTY_FEATURE_COLLECTION
            } else {
                S57ToGeoJson.convertFiles(cells, zoom)
            }
            ChartData.GeoJson(
                providerId = id,
                sourceId = "s57-enc-geojson",
                geoJson = geoJson,
                layerPrefix = "s57",
                cacheKey = cells.joinToString("|") { cell -> cell.absolutePath },
            )
        }
    }

    override fun applyToStyle(style: Style, data: ChartData) {
        val geoJson = data as? ChartData.GeoJson ?: return
        NauticalOverlay.applyGeoJsonToStyle(style, geoJson.geoJson, overlayOptions)
    }

    override fun removeFromStyle(style: Style) {
        NauticalOverlay.remove(style)
    }

    private fun selectedCells(bounds: LatLngBounds, zoom: Int): List<File> {
        return S57CellSelector.findEncFiles(
            sourcePath = sourcePath,
            cameraBounds = bounds.toGeoBounds(),
            zoomLevel = zoom,
        )
    }

    private fun LatLngBounds.toGeoBounds(): GeoBounds {
        return GeoBounds(
            west = getLonWest(),
            south = getLatSouth(),
            east = getLonEast(),
            north = getLatNorth(),
        )
    }

    companion object {
        private const val EMPTY_FEATURE_COLLECTION = """{"type":"FeatureCollection","features":[]}"""
    }
}
