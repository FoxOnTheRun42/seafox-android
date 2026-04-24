package com.seafox.nmea_dashboard.ui.widgets.chart

import android.content.Context
import android.util.Log
import org.maplibre.android.maps.Style
import org.maplibre.android.style.sources.RasterSource
import org.maplibre.android.style.sources.TileSet
import org.maplibre.android.style.layers.RasterLayer
import org.maplibre.android.style.layers.PropertyFactory
import java.io.File

/**
 * Manages offline nautical chart tiles for MapLibre.
 *
 * Supports:
 * - MBTiles files (SQLite-based tile archives)
 * - Directory-based tile trees ({z}/{x}/{y}.png)
 * - NOAA ENC files converted to MBTiles via GDAL
 */
object OfflineTileManager {

    private const val TAG = "OfflineTiles"
    private const val NAUTICAL_SOURCE_ID = "nautical-offline-source"
    private const val NAUTICAL_LAYER_ID = "nautical-offline-layer"

    /**
     * Scans for available offline chart sources on the device.
     * Checks both internal and external storage paths.
     */
    fun findOfflineChartSources(context: Context): List<OfflineChartSource> {
        val sources = mutableListOf<OfflineChartSource>()

        val searchDirs = listOf(
            File(context.filesDir, "seaCHART"),
            File(context.getExternalFilesDir(null), "seaCHART"),
        )

        for (dir in searchDirs) {
            if (!dir.isDirectory) continue

            // MBTiles files
            dir.walkTopDown()
                .maxDepth(3)
                .filter { it.isFile && it.extension.equals("mbtiles", ignoreCase = true) }
                .forEach { file ->
                    sources.add(
                        OfflineChartSource(
                            name = file.nameWithoutExtension,
                            path = file.absolutePath,
                            type = OfflineChartType.MBTILES,
                        )
                    )
                }

            // Tile directories (look for {z}/{x}/{y}.png pattern)
            dir.listFiles()?.filter { it.isDirectory }?.forEach { providerDir ->
                val hasZoomDirs = providerDir.listFiles()?.any { sub ->
                    sub.isDirectory && sub.name.toIntOrNull() != null
                } ?: false
                if (hasZoomDirs) {
                    sources.add(
                        OfflineChartSource(
                            name = providerDir.name,
                            path = providerDir.absolutePath,
                            type = OfflineChartType.TILE_DIRECTORY,
                        )
                    )
                }
            }
        }

        Log.d(TAG, "Found ${sources.size} offline chart sources")
        return sources
    }

    /**
     * Adds an MBTiles source as a raster layer to the map style.
     */
    fun addMbTilesLayer(style: Style, source: OfflineChartSource) {
        if (source.type != OfflineChartType.MBTILES) return
        if (style.getSource(NAUTICAL_SOURCE_ID) != null) {
            removeLayers(style)
        }

        val tileSet = TileSet("tileset", "mbtiles://${source.path}")
        tileSet.minZoom = 1f
        tileSet.maxZoom = 18f

        style.addSource(RasterSource(NAUTICAL_SOURCE_ID, tileSet, 256))
        style.addLayerBelow(
            RasterLayer(NAUTICAL_LAYER_ID, NAUTICAL_SOURCE_ID).withProperties(
                PropertyFactory.rasterOpacity(0.85f),
            ),
            "ais-targets-circle" // place below AIS overlay
        )
        Log.d(TAG, "Added MBTiles layer: ${source.name}")
    }

    fun removeLayers(style: Style) {
        style.removeLayer(NAUTICAL_LAYER_ID)
        style.removeSource(NAUTICAL_SOURCE_ID)
    }
}

data class OfflineChartSource(
    val name: String,
    val path: String,
    val type: OfflineChartType,
)

enum class OfflineChartType {
    MBTILES,
    TILE_DIRECTORY,
}
