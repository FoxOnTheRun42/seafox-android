package com.seafox.nmea_dashboard.ui.widgets.chart

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log

/**
 * Day/Night chart styles for MapLibre.
 *
 * Uses offline JSON styles by default (bundled world basemap).
 * Switches to online OpenFreeMap styles when internet is available.
 */
enum class ChartMode {
    DAY,
    NIGHT,
}

object ChartStyle {

    private const val TAG = "ChartStyle"

    /** Online styles — richer detail but require internet */
    private const val ONLINE_DAY_STYLE = "https://tiles.openfreemap.org/styles/liberty"
    private const val ONLINE_NIGHT_STYLE = "https://tiles.openfreemap.org/styles/dark"

    /**
     * Returns a style URI or JSON string.
     * When offline, returns an inline JSON style with embedded world basemap.
     * When online, returns the OpenFreeMap URL.
     */
    fun styleUri(mode: ChartMode, context: Context? = null): String {
        val hasInternet = context?.let { isOnline(it) } ?: false
        if (hasInternet) {
            Log.d(TAG, "Using online style: ${mode.name}")
            return when (mode) {
                ChartMode.DAY -> ONLINE_DAY_STYLE
                ChartMode.NIGHT -> ONLINE_NIGHT_STYLE
            }
        }

        Log.d(TAG, "No internet — using offline style: ${mode.name}")
        return buildOfflineStyleJson(mode, context)
    }

    private fun buildOfflineStyleJson(mode: ChartMode, context: Context?): String {
        // Load world land GeoJSON from assets
        val worldGeoJson = context?.let { loadAsset(it, "world_land_110m.geojson") } ?: "{}"

        val bgColor = if (mode == ChartMode.DAY) "#A8D5F2" else "#0A1628"
        val landColor = if (mode == ChartMode.DAY) "#E8D5A3" else "#1A2A1A"
        val outlineColor = if (mode == ChartMode.DAY) "#8B7355" else "#2A4A2A"

        return """
        {
          "version": 8,
          "name": "seaFOX Offline ${mode.name}",
          "sources": {
            "world-land": {
              "type": "geojson",
              "data": $worldGeoJson
            }
          },
          "layers": [
            {
              "id": "background",
              "type": "background",
              "paint": { "background-color": "$bgColor" }
            },
            {
              "id": "land",
              "type": "fill",
              "source": "world-land",
              "paint": { "fill-color": "$landColor", "fill-opacity": 1.0 }
            },
            {
              "id": "land-outline",
              "type": "line",
              "source": "world-land",
              "paint": { "line-color": "$outlineColor", "line-width": 0.8 }
            }
          ]
        }
        """.trimIndent()
    }

    private fun loadAsset(context: Context, name: String): String? {
        return try {
            context.assets.open(name).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load asset: $name", e)
            null
        }
    }

    private fun isOnline(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
