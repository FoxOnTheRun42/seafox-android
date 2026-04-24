package com.seafox.nmea_dashboard.ui.widgets.chart

import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.seafox.nmea_dashboard.SeaFoxDesignTokens
import com.seafox.nmea_dashboard.ui.widgets.AisTargetData
import com.seafox.nmea_dashboard.ui.widgets.SeaChartMapProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import java.io.File
import kotlin.math.abs

/**
 * MapLibre-based chart widget for seaFOX.
 *
 * Features:
 * - GPU-accelerated vector tile rendering via MapLibre GL Native
 * - Day/Night mode toggle (IMO ECDIS-inspired)
 * - AIS target overlay with threat classification
 * - Offline MBTiles support for nautical charts
 * - Smooth camera tracking of own vessel position
 */
@Composable
fun ChartWidget(
    ownLatitude: Float?,
    ownLongitude: Float?,
    ownHeadingDeg: Float? = null,
    ownCourseOverGroundDeg: Float? = null,
    ownSpeedKn: Float? = null,
    mapProvider: SeaChartMapProvider = SeaChartMapProvider.NOAA,
    activeMapSourceLabel: String?,
    activeMapSourcePath: String?,
    aisTargets: List<AisTargetData> = emptyList(),
    showAisOverlay: Boolean = true,
    showOpenSeaMapOverlay: Boolean = false,
    navigationSettings: NavigationVectorSettings = NavigationVectorSettings(),
    activeRoute: Route? = null,
    mobLat: Double? = null,
    mobLon: Double? = null,
    mobTimestampMs: Long? = null,
    trueWindDirectionDeg: Float? = null,
    boatDraftMeters: Float? = null,
    mobActive: Boolean = false,
    onMobToggle: (() -> Unit)? = null,
    onViewportChanged: ((centerLat: Double, centerLon: Double, zoomBucket: Int) -> Unit)? = null,
    nauticalOverlayOptions: NauticalOverlayOptions = NauticalOverlayOptions(),
    modifier: Modifier = Modifier,
    enableFullscreen: Boolean = true,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    remember { MapLibre.getInstance(context); true }

    var mapView by remember { mutableStateOf<MapView?>(null) }
    var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }
    var currentStyle by remember { mutableStateOf<Style?>(null) }
    var chartMode by rememberSaveable { mutableStateOf(ChartMode.DAY) }
    var followPosition by rememberSaveable { mutableStateOf(true) }
    var currentZoomBucket by rememberSaveable { mutableIntStateOf(10) }
    var offlineSources by remember { mutableStateOf<List<OfflineChartSource>>(emptyList()) }
    var lastViewportEmission by remember { mutableStateOf<Triple<Double, Double, Int>?>(null) }
    var cameraCenter by remember { mutableStateOf<LatLng?>(null) }
    var fullscreenOpen by rememberSaveable { mutableStateOf(false) }

    val defaultLat = ownLatitude?.toDouble() ?: 32.78
    val defaultLng = ownLongitude?.toDouble() ?: -79.93
    val activeOfflineMbTilesSource = remember(activeMapSourcePath) {
        activeMapSourcePath
            ?.takeIf { path -> path.endsWith(".mbtiles", ignoreCase = true) }
            ?.let { path ->
                OfflineChartSource(
                    name = File(path).nameWithoutExtension.ifBlank { "MBTiles" },
                    path = path,
                    type = OfflineChartType.MBTILES,
                )
            }
    }

    if (enableFullscreen && fullscreenOpen) {
        Dialog(
            onDismissRequest = { fullscreenOpen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
            ) {
                ChartWidget(
                    ownLatitude = ownLatitude,
                    ownLongitude = ownLongitude,
                    ownHeadingDeg = ownHeadingDeg,
                    ownCourseOverGroundDeg = ownCourseOverGroundDeg,
                    ownSpeedKn = ownSpeedKn,
                    mapProvider = mapProvider,
                    activeMapSourceLabel = activeMapSourceLabel,
                    activeMapSourcePath = activeMapSourcePath,
                    aisTargets = aisTargets,
                    showAisOverlay = showAisOverlay,
                    showOpenSeaMapOverlay = showOpenSeaMapOverlay,
                    navigationSettings = navigationSettings,
                    activeRoute = activeRoute,
                    mobLat = mobLat,
                    mobLon = mobLon,
                    mobTimestampMs = mobTimestampMs,
                    trueWindDirectionDeg = trueWindDirectionDeg,
                    boatDraftMeters = boatDraftMeters,
                    mobActive = mobActive,
                    onMobToggle = onMobToggle,
                    onViewportChanged = onViewportChanged,
                    nauticalOverlayOptions = nauticalOverlayOptions,
                    modifier = Modifier.fillMaxSize(),
                    enableFullscreen = false,
                )
                ChartControlButton(
                    text = "SCHLIESSEN",
                    backgroundColor = Color(0xDD101927),
                    onClick = { fullscreenOpen = false },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(10.dp),
                )
            }
        }
    }

    // Scan for offline chart sources
    LaunchedEffect(Unit) {
        offlineSources = withContext(Dispatchers.IO) {
            OfflineTileManager.findOfflineChartSources(context)
        }
    }

    // Lifecycle management
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView?.onStart()
                Lifecycle.Event.ON_RESUME -> mapView?.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView?.onPause()
                Lifecycle.Event.ON_STOP -> mapView?.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView?.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Follow own position
    LaunchedEffect(ownLatitude, ownLongitude, followPosition) {
        val map = mapLibreMap ?: return@LaunchedEffect
        if (followPosition && ownLatitude != null && ownLongitude != null) {
            map.animateCamera(
                CameraUpdateFactory.newLatLng(
                    LatLng(ownLatitude.toDouble(), ownLongitude.toDouble())
                ),
                1000
            )
        }
    }

    // Update AIS overlay
    LaunchedEffect(aisTargets, showAisOverlay, currentStyle) {
        val style = currentStyle ?: return@LaunchedEffect
        if (showAisOverlay) {
            AisOverlay.setup(style)
            AisOverlay.update(style, aisTargets)
        } else {
            AisOverlay.remove(style)
        }
    }

    // Update navigation overlay
    LaunchedEffect(
        ownLatitude,
        ownLongitude,
        ownHeadingDeg,
        ownCourseOverGroundDeg,
        ownSpeedKn,
        navigationSettings,
        activeRoute,
        mobLat,
        mobLon,
        mobTimestampMs,
        trueWindDirectionDeg,
        boatDraftMeters,
        currentStyle,
        chartMode,
    ) {
        val style = currentStyle ?: return@LaunchedEffect
        NavigationOverlay.update(
            style = style,
            ownLat = ownLatitude,
            ownLon = ownLongitude,
            headingDeg = ownHeadingDeg,
            cogDeg = ownCourseOverGroundDeg,
            sogKn = ownSpeedKn,
            settings = navigationSettings,
            isNightMode = chartMode == ChartMode.NIGHT,
            activeRoute = activeRoute,
            mobLat = mobLat,
            mobLon = mobLon,
            mobTimestampMs = mobTimestampMs,
            trueWindDirectionDeg = trueWindDirectionDeg,
            boatDraftMeters = boatDraftMeters,
        )
    }

    LaunchedEffect(mapProvider, activeOfflineMbTilesSource, currentStyle) {
        val style = currentStyle ?: return@LaunchedEffect
        if (activeOfflineMbTilesSource != null) {
            FreeRasterChartProviders.removeBaseLayer(style)
        } else {
            FreeRasterChartProviders.updateBaseLayer(style, mapProvider)
        }
    }

    LaunchedEffect(showOpenSeaMapOverlay, mapProvider, currentStyle) {
        val style = currentStyle ?: return@LaunchedEffect
        val seamarkOverlayEnabled = showOpenSeaMapOverlay ||
            FreeRasterChartProviders.shouldForceSeamarkOverlay(mapProvider)
        OpenSeaMapOverlay.update(style, enabled = seamarkOverlayEnabled)
    }

    // Load offline MBTiles when available
    LaunchedEffect(activeOfflineMbTilesSource, offlineSources, currentStyle) {
        val style = currentStyle ?: return@LaunchedEffect
        val mbtilesSource = activeOfflineMbTilesSource
            ?: offlineSources.firstOrNull { it.type == OfflineChartType.MBTILES }
        if (mbtilesSource != null) {
            OfflineTileManager.addMbTilesLayer(style, mbtilesSource)
        } else {
            OfflineTileManager.removeLayers(style)
        }
    }

    // Load S-57 ENC data as nautical chart overlay
    val cameraBounds = cameraCenter?.let { center ->
        val delta = 0.5
        GeoBounds(
            west = center.longitude - delta,
            south = center.latitude - delta,
            east = center.longitude + delta,
            north = center.latitude + delta,
        )
    }

    LaunchedEffect(activeMapSourcePath, currentStyle, currentZoomBucket, nauticalOverlayOptions, cameraCenter) {
        val style = currentStyle ?: return@LaunchedEffect
        val isOfflineTilePackage = activeMapSourcePath?.let { path ->
            path.endsWith(".mbtiles", ignoreCase = true) ||
                path.endsWith(".gpkg", ignoreCase = true) ||
                path.endsWith(".geopackage", ignoreCase = true)
        } == true
        if (activeMapSourcePath.isNullOrBlank() || isOfflineTilePackage) {
            NauticalOverlay.remove(style)
        } else {
            NauticalOverlay.loadFromPath(
                context = context,
                style = style,
                sourcePath = activeMapSourcePath,
                zoomLevel = currentZoomBucket,
                cameraBounds = cameraBounds,
                options = nauticalOverlayOptions,
            )
        }
    }

    // Apply day/night style change
    LaunchedEffect(chartMode) {
        val map = mapLibreMap ?: return@LaunchedEffect
        val styleData = ChartStyle.styleUri(chartMode, context)
        val builder = if (styleData.trimStart().startsWith("{")) {
            Style.Builder().fromJson(styleData)
        } else {
            Style.Builder().fromUri(styleData)
        }
        map.setStyle(builder) { style ->
            currentStyle = style
            configureMapUi(map)
            // Re-apply overlays after style change
            if (activeOfflineMbTilesSource != null) {
                FreeRasterChartProviders.removeBaseLayer(style)
            } else {
                FreeRasterChartProviders.updateBaseLayer(style, mapProvider)
            }
            if (showAisOverlay) {
                AisOverlay.setup(style)
                AisOverlay.update(style, aisTargets)
            }
            val seamarkOverlayEnabled = showOpenSeaMapOverlay ||
                FreeRasterChartProviders.shouldForceSeamarkOverlay(mapProvider)
            OpenSeaMapOverlay.update(style, enabled = seamarkOverlayEnabled)
            val mbtilesSource = activeOfflineMbTilesSource
                ?: offlineSources.firstOrNull { it.type == OfflineChartType.MBTILES }
            if (mbtilesSource != null) {
                OfflineTileManager.addMbTilesLayer(style, mbtilesSource)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // MapLibre MapView
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                MapView(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT,
                    )
                    mapView = this

                    getMapAsync { map ->
                        mapLibreMap = map
                        val initStyleData = ChartStyle.styleUri(chartMode, ctx)
                        val initBuilder = if (initStyleData.trimStart().startsWith("{")) {
                            Style.Builder().fromJson(initStyleData)
                        } else {
                            Style.Builder().fromUri(initStyleData)
                        }
                        map.setStyle(initBuilder) { style ->
                            currentStyle = style
                            configureMapUi(map)
                            if (activeOfflineMbTilesSource != null) {
                                FreeRasterChartProviders.removeBaseLayer(style)
                            } else {
                                FreeRasterChartProviders.updateBaseLayer(style, mapProvider)
                            }
                            if (showAisOverlay) {
                                AisOverlay.setup(style)
                            }
                            val seamarkOverlayEnabled = showOpenSeaMapOverlay ||
                                FreeRasterChartProviders.shouldForceSeamarkOverlay(mapProvider)
                            OpenSeaMapOverlay.update(style, enabled = seamarkOverlayEnabled)
                        }
                        map.cameraPosition = CameraPosition.Builder()
                            .target(LatLng(defaultLat, defaultLng))
                            .zoom(10.0)
                            .build()
                        currentZoomBucket = map.cameraPosition.zoom.toInt().coerceIn(0, 22)
                        val initialTarget = map.cameraPosition.target ?: LatLng(defaultLat, defaultLng)
                        cameraCenter = initialTarget
                        lastViewportEmission = Triple(initialTarget.latitude, initialTarget.longitude, currentZoomBucket)
                        onViewportChanged?.invoke(initialTarget.latitude, initialTarget.longitude, currentZoomBucket)

                        // Disable follow mode on user drag
                        map.addOnCameraMoveStartedListener { reason ->
                            if (reason == MapLibreMap.OnCameraMoveStartedListener.REASON_API_GESTURE) {
                                followPosition = false
                            }
                        }
                        map.addOnCameraIdleListener {
                            val zoomBucket = map.cameraPosition.zoom.toInt().coerceIn(0, 22)
                            currentZoomBucket = zoomBucket
                            val target = map.cameraPosition.target ?: return@addOnCameraIdleListener
                            cameraCenter = target
                            val previous = lastViewportEmission
                            val shouldEmit = previous == null ||
                                previous.third != zoomBucket ||
                                abs(previous.first - target.latitude) >= 0.01 ||
                                abs(previous.second - target.longitude) >= 0.01
                            if (shouldEmit) {
                                lastViewportEmission = Triple(target.latitude, target.longitude, zoomBucket)
                                onViewportChanged?.invoke(target.latitude, target.longitude, zoomBucket)
                            }
                        }
                    }
                }
            },
            update = { _ -> },
        )

        // Control overlay (Day/Night toggle + position follow)
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp),
        ) {
            // Day/Night toggle
            ChartControlButton(
                text = if (chartMode == ChartMode.DAY) "TAG" else "NACHT",
                backgroundColor = if (chartMode == ChartMode.DAY)
                    SeaFoxDesignTokens.Color.surfaceRaisedDark else Color(0xEE101927),
                onClick = {
                    chartMode = if (chartMode == ChartMode.DAY) ChartMode.NIGHT else ChartMode.DAY
                },
            )

            Spacer(modifier = Modifier.size(4.dp))

            // Re-center on own position
            if (!followPosition && ownLatitude != null) {
                ChartControlButton(
                    text = "POS",
                    backgroundColor = SeaFoxDesignTokens.Color.cyan.copy(alpha = 0.72f),
                    onClick = { followPosition = true },
                )

                Spacer(modifier = Modifier.size(4.dp))
            }

            ChartControlButton(
                text = if (mobActive) "MOB ✕" else "MOB",
                backgroundColor = if (mobActive) {
                    SeaFoxDesignTokens.Color.coral.copy(alpha = 0.92f)
                } else {
                    Color(0xCC4D1212)
                },
                onClick = { onMobToggle?.invoke() },
            )

            if (enableFullscreen) {
                Spacer(modifier = Modifier.size(4.dp))
                ChartControlButton(
                    text = "VOLL",
                    backgroundColor = Color(0xCC102A43),
                    onClick = { fullscreenOpen = true },
                )
            }
        }

        // AIS target count indicator
        if (showAisOverlay && aisTargets.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SeaFoxDesignTokens.Color.surfaceRaisedDark.copy(alpha = 0.88f))
                    .border(
                        1.dp,
                        SeaFoxDesignTokens.Color.hairlineDark,
                        RoundedCornerShape(8.dp),
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(SeaFoxDesignTokens.Color.emerald),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "AIS: ${aisTargets.size}",
                    color = Color.White,
                    fontSize = 12.sp,
                )
            }
        }

        // Chart source label
        if (activeMapSourceLabel != null) {
            Text(
                text = activeMapSourceLabel,
                color = Color(0x99FFFFFF),
                fontSize = 10.sp,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp),
            )
        }
    }
}

@Composable
private fun ChartControlButton(
    text: String,
    backgroundColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(
                1.dp,
                SeaFoxDesignTokens.Color.hairlineDark.copy(alpha = 0.70f),
                RoundedCornerShape(8.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 12.sp,
        )
    }
}

private fun configureMapUi(map: MapLibreMap) {
    map.uiSettings.isCompassEnabled = false
    map.uiSettings.isLogoEnabled = false
    map.uiSettings.isAttributionEnabled = false
    map.uiSettings.isRotateGesturesEnabled = true
    map.uiSettings.isZoomGesturesEnabled = true
    map.uiSettings.isScrollGesturesEnabled = true
    map.uiSettings.isTiltGesturesEnabled = false
}
