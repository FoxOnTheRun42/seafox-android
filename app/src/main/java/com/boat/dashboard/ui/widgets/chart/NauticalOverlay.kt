package com.seafox.nmea_dashboard.ui.widgets.chart

import android.content.Context
import android.util.Log
import com.seafox.nmea_dashboard.ui.widgets.chart.s57.S57ToGeoJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.android.maps.Style
import java.io.File

data class NauticalOverlayOptions(
    val showRoadLayers: Boolean = true,
    val showDepthLayers: Boolean = true,
    val showContourLines: Boolean = true,
)

/**
 * Manages nautical chart overlay from S-57 ENC data on MapLibre.
 *
 * Converts .000 files to GeoJSON on the device and renders them
 * as styled layers (land, depth areas, coastlines, buoys, etc.)
 */
object NauticalOverlay {

    private const val TAG = "NauticalOverlay"
    private const val SOURCE_ID = "nautical-enc-source"

    private const val LAND_FILL_LAYER = "enc-land-fill"
    private const val DEPTH_FILL_LAYER = "enc-depth-fill"
    private const val DREDGED_FILL_LAYER = "enc-dredged-fill"
    private const val COASTLINE_LAYER = "enc-coastline"
    private const val DEPTH_CONTOUR_LAYER = "enc-depth-contour"
    private const val BUOY_LAYER = "enc-buoys"
    private const val BEACON_LAYER = "enc-beacons"
    private const val LIGHT_LAYER = "enc-lights"
    private const val OBSTRUCTION_LAYER = "enc-obstructions"
    private const val SOUNDING_LAYER = "enc-soundings"
    private const val SOUNDING_TEXT_LAYER = "enc-sounding-text"
    private const val AREA_LAYER = "enc-areas"
    private const val NAV_LINE_LAYER = "enc-nav-lines"
    private const val PORT_AREA_LAYER = "enc-port-areas"
    private const val BUILDING_LAYER = "enc-buildings"
    private const val VEGETATION_LAYER = "enc-vegetation"
    private const val INFRASTRUCTURE_LAYER = "enc-infrastructure"

    private val ALL_LAYERS = listOf(
        LAND_FILL_LAYER, DEPTH_FILL_LAYER, DREDGED_FILL_LAYER, COASTLINE_LAYER,
        DEPTH_CONTOUR_LAYER, BUOY_LAYER, BEACON_LAYER, LIGHT_LAYER, OBSTRUCTION_LAYER,
        SOUNDING_LAYER, SOUNDING_TEXT_LAYER, AREA_LAYER, NAV_LINE_LAYER, PORT_AREA_LAYER,
        BUILDING_LAYER, VEGETATION_LAYER, INFRASTRUCTURE_LAYER,
    )
    private var lastRequestSignature: String? = null
    private var lastLoadedPathSet: Set<String> = emptySet()
    private var lastGeoJson: String? = null

    /**
     * Find and load all .000 ENC files from the chart source path.
     * Runs conversion on IO thread, then applies layers on main thread.
     */
    suspend fun loadFromPath(
        context: Context,
        style: Style,
        sourcePath: String?,
        zoomLevel: Int = 10,
        cameraBounds: GeoBounds? = null,
        options: NauticalOverlayOptions = NauticalOverlayOptions(),
    ) {
        Log.i(TAG, "loadFromPath called — sourcePath=$sourcePath zoom=$zoomLevel")
        if (sourcePath == null) {
            Log.w(TAG, "sourcePath is null, skipping")
            return
        }
        val requestSignature = listOf(
            File(sourcePath).absolutePath,
            zoomLevel.toString(),
            cameraBounds?.west?.toString().orEmpty(),
            cameraBounds?.south?.toString().orEmpty(),
            cameraBounds?.east?.toString().orEmpty(),
            cameraBounds?.north?.toString().orEmpty(),
            options.showRoadLayers.toString(),
            options.showDepthLayers.toString(),
            options.showContourLines.toString(),
        ).joinToString("|")
        if (requestSignature == lastRequestSignature && style.getSource(SOURCE_ID) != null) {
            Log.d(TAG, "Skipping ENC reload for unchanged request")
            return
        }

        val encFiles = withContext(Dispatchers.IO) {
            findEncFiles(sourcePath, cameraBounds, zoomLevel)
        }
        val selectedPathSet = encFiles.map { it.absolutePath }.toSet()

        if (encFiles.isEmpty()) {
            Log.w(TAG, "No .000 files found at: $sourcePath")
            remove(style)
            lastLoadedPathSet = emptySet()
            lastGeoJson = null
            return
        }

        if (selectedPathSet == lastLoadedPathSet && lastGeoJson != null) {
            Log.d(TAG, "Reusing cached ENC GeoJSON for unchanged selected path set")
            applyGeoJsonToStyle(style, lastGeoJson!!, options)
            lastRequestSignature = requestSignature
            return
        }

        Log.i(TAG, "Converting ${encFiles.size} ENC files to GeoJSON (first: ${encFiles.first().name})...")

        val geoJson = try {
            withContext(Dispatchers.IO) {
                S57ToGeoJson.convertFiles(encFiles, zoomLevel)
            }
        } catch (e: Exception) {
            Log.e(TAG, "S57 conversion failed", e)
            return
        }

        Log.i(TAG, "GeoJSON ready: ${geoJson.length} chars, applying to style...")
        applyGeoJsonToStyle(style, geoJson, options)
        lastRequestSignature = requestSignature
        lastLoadedPathSet = selectedPathSet
        lastGeoJson = geoJson
        Log.i(TAG, "Nautical overlay applied successfully")
    }

    fun applyGeoJsonToStyle(
        style: Style,
        geoJson: String,
        options: NauticalOverlayOptions = NauticalOverlayOptions(),
    ) {
        remove(style)

        style.addSource(GeoJsonSource(SOURCE_ID, geoJson))

        val objCode = Expression.get("objCode")

        // Land areas (LNDARE)
        style.addLayer(
            FillLayer(LAND_FILL_LAYER, SOURCE_ID).withProperties(
                PropertyFactory.fillColor("#E8D5A3"),
                PropertyFactory.fillOpacity(0.7f),
            ).withFilter(eqObj("LNDARE"))
        )

        // Depth areas (DEPARE)
        if (options.showDepthLayers) {
            style.addLayer(
                FillLayer(DEPTH_FILL_LAYER, SOURCE_ID).withProperties(
                    PropertyFactory.fillColor(
                        Expression.interpolate(
                            Expression.linear(),
                            Expression.toNumber(Expression.get("DRVAL1")),
                            Expression.stop(0, "#B3D9FF"),
                            Expression.stop(5, "#80BFFF"),
                            Expression.stop(10, "#4DA6FF"),
                            Expression.stop(20, "#1A8CFF"),
                            Expression.stop(50, "#0066CC"),
                            Expression.stop(100, "#004C99"),
                        )
                    ),
                    PropertyFactory.fillOpacity(0.4f),
                ).withFilter(eqObj("DEPARE"))
            )
        }

        // Restricted/caution areas
        style.addLayer(
            FillLayer(AREA_LAYER, SOURCE_ID).withProperties(
                PropertyFactory.fillColor("#FF0000"),
                PropertyFactory.fillOpacity(0.15f),
            ).withFilter(anyObj("RESARE", "CTNARE", "MIPARE"))
        )

        // Coastline (COALNE)
        style.addLayer(
            LineLayer(COASTLINE_LAYER, SOURCE_ID).withProperties(
                PropertyFactory.lineColor("#333333"),
                PropertyFactory.lineWidth(1.5f),
            ).withFilter(eqObj("COALNE"))
        )

        // Depth contours (DEPCNT)
        if (options.showContourLines) {
            style.addLayer(
                LineLayer(DEPTH_CONTOUR_LAYER, SOURCE_ID).withProperties(
                    PropertyFactory.lineColor("#6699CC"),
                    PropertyFactory.lineWidth(0.8f),
                ).withFilter(eqObj("DEPCNT"))
            )
        }

        // Navigation lines, fairways, TSS
        if (options.showRoadLayers) {
            style.addLayer(
                LineLayer(NAV_LINE_LAYER, SOURCE_ID).withProperties(
                    PropertyFactory.lineColor("#CC00CC"),
                    PropertyFactory.lineWidth(1.2f),
                    PropertyFactory.lineDasharray(arrayOf(4f, 2f)),
                ).withFilter(anyObj("NAVLNE", "FAIRWY", "TSSLPT", "TSSRON"))
            )
        }

        // Buoys
        style.addLayer(
            CircleLayer(BUOY_LAYER, SOURCE_ID).withProperties(
                PropertyFactory.circleRadius(5f),
                PropertyFactory.circleColor(
                    Expression.match(
                        Expression.get("objCode"),
                        Expression.literal("#FFD700"),
                        Expression.stop("BOYCAR", "#FFD700"),
                        Expression.stop("BOYLAT", "#FF0000"),
                        Expression.stop("BOYSAW", "#FF0000"),
                        Expression.stop("BOYSPP", "#FFFF00"),
                        Expression.stop("BOYISD", "#FF0000"),
                    )
                ),
                PropertyFactory.circleStrokeWidth(1.5f),
                PropertyFactory.circleStrokeColor("#000000"),
            ).withFilter(anyObj("BOYCAR", "BOYLAT", "BOYSAW", "BOYSPP", "BOYISD", "BOYINB"))
        )

        // Beacons
        style.addLayer(
            CircleLayer(BEACON_LAYER, SOURCE_ID).withProperties(
                PropertyFactory.circleRadius(4f),
                PropertyFactory.circleColor("#FF6600"),
                PropertyFactory.circleStrokeWidth(1.5f),
                PropertyFactory.circleStrokeColor("#000000"),
            ).withFilter(anyObj("BCNCAR", "BCNLAT", "BCNSAW", "BCNSPP", "BCNISD"))
        )

        // Lights
        style.addLayer(
            CircleLayer(LIGHT_LAYER, SOURCE_ID).withProperties(
                PropertyFactory.circleRadius(3f),
                PropertyFactory.circleColor("#FFFF00"),
                PropertyFactory.circleStrokeWidth(1f),
                PropertyFactory.circleStrokeColor("#CC9900"),
            ).withFilter(eqObj("LIGHTS"))
        )

        // Obstructions & Wrecks
        style.addLayer(
            CircleLayer(OBSTRUCTION_LAYER, SOURCE_ID).withProperties(
                PropertyFactory.circleRadius(4f),
                PropertyFactory.circleColor("#FF0000"),
                PropertyFactory.circleStrokeWidth(1.5f),
                PropertyFactory.circleStrokeColor("#990000"),
            ).withFilter(anyObj("OBSTRN", "WRECKS", "UWTROC"))
        )

        // Soundings (dot)
        if (options.showDepthLayers) {
            style.addLayer(
                CircleLayer(SOUNDING_LAYER, SOURCE_ID).withProperties(
                    PropertyFactory.circleRadius(2f),
                    PropertyFactory.circleColor("#0066CC"),
                ).withFilter(eqObj("SOUNDG"))
            )

            // Sounding depth labels
            style.addLayer(
                SymbolLayer(SOUNDING_TEXT_LAYER, SOURCE_ID).withProperties(
                    PropertyFactory.textField(Expression.get("depthLabel")),
                    PropertyFactory.textSize(11f),
                    PropertyFactory.textColor("#003366"),
                    PropertyFactory.textHaloColor("#FFFFFF"),
                    PropertyFactory.textHaloWidth(1.5f),
                    PropertyFactory.textOffset(arrayOf(0f, 0.8f)),
                    PropertyFactory.textAllowOverlap(false),
                    PropertyFactory.textIgnorePlacement(false),
                ).withFilter(
                    Expression.all(
                        eqObj("SOUNDG"),
                        Expression.has("depthLabel"),
                    )
                )
            )
        }

        // Dredged areas
        style.addLayer(
            FillLayer(DREDGED_FILL_LAYER, SOURCE_ID).withProperties(
                PropertyFactory.fillColor("#99CCFF"),
                PropertyFactory.fillOpacity(0.3f),
            ).withFilter(eqObj("DRGARE"))
        )

        // Port / dock areas
        style.addLayer(
            FillLayer(PORT_AREA_LAYER, SOURCE_ID).withProperties(
                PropertyFactory.fillColor("#CCAA77"),
                PropertyFactory.fillOpacity(0.3f),
            ).withFilter(anyObj("HRBARE", "DOCARE", "DRYDOC", "LOKBSN", "FLODOC"))
        )

        // Buildings
        style.addLayer(
            FillLayer(BUILDING_LAYER, SOURCE_ID).withProperties(
                PropertyFactory.fillColor("#999999"),
                PropertyFactory.fillOpacity(0.5f),
            ).withFilter(anyObj("BUISGL", "BUAARE", "PRDARE"))
        )

        // Vegetation
        style.addLayer(
            FillLayer(VEGETATION_LAYER, SOURCE_ID).withProperties(
                PropertyFactory.fillColor("#88BB66"),
                PropertyFactory.fillOpacity(0.3f),
            ).withFilter(eqObj("VEGATN"))
        )

        // Infrastructure lines (roads, pipelines, cables)
        if (options.showRoadLayers) {
            style.addLayer(
                LineLayer(INFRASTRUCTURE_LAYER, SOURCE_ID).withProperties(
                    PropertyFactory.lineColor("#888888"),
                    PropertyFactory.lineWidth(1.0f),
                ).withFilter(anyObj("ROADWY", "PIPSOL", "CBLOHD", "PIPOHD"))
            )
        }

        Log.d(TAG, "Nautical overlay applied with ${ALL_LAYERS.size} layers")
    }

    fun remove(style: Style) {
        lastRequestSignature = null
        for (layerId in ALL_LAYERS) {
            try { style.removeLayer(layerId) } catch (_: Exception) {}
        }
        try { style.removeSource(SOURCE_ID) } catch (_: Exception) {}
    }

    /** Filter: objCode == value */
    private fun eqObj(code: String): Expression =
        Expression.eq(Expression.get("objCode"), Expression.literal(code))

    /** Filter: objCode in [codes] — uses any(eq, eq, ...) */
    private fun anyObj(vararg codes: String): Expression =
        Expression.any(*codes.map { eqObj(it) }.toTypedArray())

    private fun findEncFiles(sourcePath: String): List<File> {
        return findEncFiles(sourcePath, cameraBounds = null, zoomLevel = null)
    }

    private fun findEncFiles(
        sourcePath: String,
        cameraBounds: GeoBounds?,
        zoomLevel: Int?,
    ): List<File> {
        val source = File(sourcePath)
        Log.d(TAG, "findEncFiles: path=$sourcePath exists=${source.exists()} isFile=${source.isFile} isDir=${source.isDirectory}")
        if (source.isFile && source.extension == "000") {
            Log.d(TAG, "findEncFiles: single .000 file, size=${source.length()}")
            return listOf(source)
        }
        val sourceRoot = when {
            source.isDirectory -> source
            source.parentFile?.isDirectory == true -> source.parentFile
            else -> null
        }
        val catalogFile = sourceRoot
            ?.walkTopDown()
            ?.maxDepth(3)
            ?.firstOrNull { it.isFile && it.name.equals("CATALOG.031", ignoreCase = true) }
        if (catalogFile != null && cameraBounds != null) {
            val preferredFiles = Catalog031Parser.parse(catalogFile)
                .preferredRelativePaths(bounds = cameraBounds, zoom = zoomLevel, limit = 20)
                .map { relativePath -> File(catalogFile.parentFile, relativePath) }
                .filter { it.isFile && it.extension.equals("000", ignoreCase = true) }
            if (preferredFiles.isNotEmpty()) {
                Log.d(TAG, "findEncFiles: catalog selected ${preferredFiles.size} preferred .000 files")
                return preferredFiles
            }
        }
        if (source.isDirectory) {
            val files = source.walkTopDown()
                .maxDepth(4)
                .filter { it.isFile && it.extension == "000" }
                .take(20) // Limit to 20 files to avoid OOM on 6813 files
                .toList()
            Log.d(TAG, "findEncFiles: directory walk found ${files.size} .000 files")
            return files
        }
        val parent = source.parentFile
        if (parent?.isDirectory == true) {
            val files = parent.walkTopDown()
                .maxDepth(4)
                .filter { it.isFile && it.extension == "000" }
                .take(20)
                .toList()
            Log.d(TAG, "findEncFiles: parent walk found ${files.size} .000 files")
            return files
        }
        Log.w(TAG, "findEncFiles: no files found for $sourcePath")
        return emptyList()
    }
}
