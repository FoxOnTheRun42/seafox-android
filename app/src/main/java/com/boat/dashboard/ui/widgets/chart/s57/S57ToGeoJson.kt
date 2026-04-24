package com.seafox.nmea_dashboard.ui.widgets.chart.s57

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Locale

/**
 * Converts S-57 ENC datasets to GeoJSON for MapLibre rendering.
 *
 * Handles all three geometry types:
 * - Points (soundings, buoys, beacons, lights)
 * - Lines (coastlines, depth contours, navigation lines)
 * - Areas (land areas, depth areas, restricted areas)
 *
 * Geometry is resolved from spatial references (FSPT) by looking up
 * nodes and edges from the dataset.
 */
object S57ToGeoJson {

    private const val TAG = "S57ToGeoJson"
    private const val COORD_EPSILON = 1e-7

    /**
     * Convert a single .000 file to a GeoJSON FeatureCollection string.
     */
    fun convertFile(file: File, zoomLevel: Int? = null): String? {
        val reader = S57Reader()
        val dataset = reader.read(file)
        if (dataset.features.isEmpty()) {
            Log.w(TAG, "No features in ${file.name}")
            return null
        }
        return convertDataset(dataset, zoomLevel)
    }

    /**
     * Convert multiple .000 files to a single merged GeoJSON FeatureCollection.
     */
    fun convertFiles(files: List<File>, zoomLevel: Int? = null): String {
        val reader = S57Reader()
        val allFeatures = JSONArray()
        var totalCount = 0

        for (file in files) {
            Log.i(TAG, "Reading ${file.name} (${file.length()} bytes)...")
            val dataset = reader.read(file)
            Log.i(TAG, "  Dataset: ${dataset.nodes.size} nodes, ${dataset.edges.size} edges, ${dataset.features.size} features, comf=${dataset.comf}, somf=${dataset.somf}")

            // Log feature object code distribution
            val objCodes = dataset.features.groupBy { it.objectCode }.mapValues { it.value.size }
            Log.i(TAG, "  Object codes: $objCodes")

            val navFeatures = dataset.features.filter { it.objectCode in S57ObjectCodes.NAVIGATIONAL_CODES }
            Log.i(TAG, "  Navigational features: ${navFeatures.size}")

            val features = buildFeatures(dataset, zoomLevel)
            Log.i(TAG, "  GeoJSON features produced: ${features.length()} at zoom ${zoomLevel ?: "all"}")
            for (i in 0 until features.length()) {
                allFeatures.put(features.getJSONObject(i))
            }
            totalCount += dataset.features.size
        }

        Log.i(TAG, "Converted $totalCount S-57 features from ${files.size} files to ${allFeatures.length()} GeoJSON features")

        return JSONObject().apply {
            put("type", "FeatureCollection")
            put("features", allFeatures)
        }.toString()
    }

    fun convertDataset(dataset: S57Dataset, zoomLevel: Int? = null): String {
        val features = buildFeatures(dataset, zoomLevel)
        return JSONObject().apply {
            put("type", "FeatureCollection")
            put("features", features)
        }.toString()
    }

    private fun buildFeatures(dataset: S57Dataset, zoomLevel: Int? = null): JSONArray {
        val features = JSONArray()
        var geometryFailCount = 0
        val failedByCode = mutableMapOf<String, Int>()

        for (feature in dataset.features) {
            // Skip metadata objects
            if (feature.objectCode.startsWith("M_") || feature.objectCode.startsWith("C_")) continue
            // Only include navigational features
            if (feature.objectCode !in S57ObjectCodes.NAVIGATIONAL_CODES) continue
            if (!shouldIncludeFeatureAtZoom(feature, zoomLevel)) continue

            // SOUNDG: expand into individual Point features with depth
            if (feature.objectCode == "SOUNDG") {
                expandSoundingFeatures(feature, dataset, features)
                continue
            }

            val geometry = resolveGeometry(feature, dataset)
            if (geometry == null) {
                geometryFailCount++
                failedByCode[feature.objectCode] = (failedByCode[feature.objectCode] ?: 0) + 1
                if (geometryFailCount <= 3) {
                    Log.w(TAG, "  FAIL ${feature.objectCode} prim=${feature.primitiveType} refs=${feature.spatialRefs.map { "rcnm=${it.rcnm},rcid=${it.rcid},rev=${it.reversed},usage=${it.usage}" }}")
                }
                continue
            }
            val properties = buildProperties(feature)

            features.put(JSONObject().apply {
                put("type", "Feature")
                put("properties", properties)
                put("geometry", geometry)
            })
        }

        if (geometryFailCount > 0) {
            Log.w(TAG, "  Geometry resolution failed for $geometryFailCount features: $failedByCode")
        }

        return features
    }

    private fun resolveGeometry(feature: S57Feature, dataset: S57Dataset): JSONObject? {
        return when (feature.primitiveType) {
            1 -> resolvePointGeometry(feature, dataset)
            2 -> resolveLineGeometry(feature, dataset)
            3 -> resolveAreaGeometry(feature, dataset)
            else -> null
        }
    }

    private fun resolvePointGeometry(feature: S57Feature, dataset: S57Dataset): JSONObject? {
        // Regular point: look up node from spatial refs
        for (ref in feature.spatialRefs) {
            if (ref.rcnm == 110) { // isolated node
                val coord = dataset.nodes[ref.rcid] ?: continue
                return JSONObject().apply {
                    put("type", "Point")
                    put("coordinates", JSONArray().apply {
                        put(coord[0]) // lon
                        put(coord[1]) // lat
                    })
                }
            }
        }
        return null
    }

    /**
     * Expand SOUNDG features into individual Point features, each with a depth property.
     * Called from buildFeatures instead of resolveGeometry for SOUNDG.
     */
    private fun expandSoundingFeatures(feature: S57Feature, dataset: S57Dataset, out: JSONArray) {
        for (ref in feature.spatialRefs) {
            if (ref.rcnm != 110) continue
            val coord = dataset.nodes[ref.rcid] ?: continue
            if (coord.size < 3) continue // need depth
            val depth = coord[2]
            val geometry = JSONObject().apply {
                put("type", "Point")
                put("coordinates", JSONArray().apply {
                    put(coord[0]) // lon
                    put(coord[1]) // lat
                })
            }
            val properties = buildProperties(feature).apply {
                put("depth", depth)
                // Format depth for display: one decimal, strip trailing zero
                val depthStr = if (depth == Math.floor(depth)) {
                    depth.toInt().toString()
                } else {
                    "%.1f".format(Locale.ROOT, depth)
                }
                put("depthLabel", depthStr)
            }
            out.put(JSONObject().apply {
                put("type", "Feature")
                put("properties", properties)
                put("geometry", geometry)
            })
        }
    }

    private fun resolveLineGeometry(feature: S57Feature, dataset: S57Dataset): JSONObject? {
        val coordList = collectEdgeCoordinates(feature, dataset)
        if (coordList.isEmpty()) return null

        val coordinates = JSONArray()
        for (coord in coordList) {
            coordinates.put(JSONArray().apply {
                put(coord[0]) // lon
                put(coord[1]) // lat
            })
        }

        if (coordinates.length() < 2) return null

        return JSONObject().apply {
            put("type", "LineString")
            put("coordinates", coordinates)
        }
    }

    private fun resolveAreaGeometry(feature: S57Feature, dataset: S57Dataset): JSONObject? {
        val segments = expandAreaSegments(feature, dataset)
        if (segments.isEmpty()) return null

        val exteriorSegments = segments.filter { it.usage != 2 }
        val interiorSegments = segments.filter { it.usage == 2 }

        val exteriorRings = buildClosedRings(exteriorSegments).toMutableList()
        val interiorRings = buildClosedRings(interiorSegments).toMutableList()

        if (exteriorRings.isEmpty() && interiorRings.isNotEmpty()) {
            exteriorRings += interiorRings.removeAt(0)
        }
        if (exteriorRings.isEmpty()) return null

        val polygons = exteriorRings.map { exterior ->
            PolygonRings(
                exterior = normalizeRingOrientation(exterior, clockwise = false),
                holes = mutableListOf(),
            )
        }

        for (interior in interiorRings) {
            val normalizedInterior = normalizeRingOrientation(interior, clockwise = true)
            val samplePoint = normalizedInterior.firstOrNull() ?: continue
            val owner = polygons.firstOrNull { pointInRing(samplePoint, it.exterior) } ?: polygons.firstOrNull()
            owner?.holes?.add(normalizedInterior)
        }

        return buildPolygonGeometry(polygons)
    }

    private fun collectEdgeCoordinates(feature: S57Feature, dataset: S57Dataset): List<DoubleArray> {
        val result = mutableListOf<DoubleArray>()
        for (ref in feature.spatialRefs) {
            when (ref.rcnm) {
                110 -> {
                    val coord = dataset.nodes[ref.rcid]
                    if (coord != null) result.add(coord)
                }
                120 -> {
                    val edgeCoords = dataset.edges[ref.rcid] ?: continue
                    val ordered = if (ref.reversed) edgeCoords.reversed() else edgeCoords
                    result.addAll(ordered)
                }
                130 -> { // face — expand to edges
                    val faceRefs = dataset.faceEdges[ref.rcid] ?: continue
                    for (faceRef in faceRefs) {
                        if (faceRef.rcnm == 120) {
                            val edgeCoords = dataset.edges[faceRef.rcid] ?: continue
                            val reversed = ref.reversed.xor(faceRef.reversed).xor(faceRef.topologyIndicator == 4)
                            val ordered = if (reversed) edgeCoords.reversed() else edgeCoords
                            result.addAll(ordered)
                        }
                    }
                }
            }
        }
        return result
    }

    private fun buildProperties(feature: S57Feature): JSONObject {
        return JSONObject().apply {
            put("objCode", feature.objectCode)
            put("objClass", feature.objectClassCode)
            put("primType", feature.primitiveType)

            // Extract commonly needed attributes
            feature.attributes[2]?.let { put("OBJNAM", it) }   // Object Name
            feature.attributes[11]?.let { put("COLOUR", it) }  // Colour
            feature.attributes[18]?.let { put("DRVAL1", it) }  // Depth Value 1
            feature.attributes[19]?.let { put("DRVAL2", it) }  // Depth Value 2
            feature.attributes[87]?.let { put("CATLIT", it) }  // Category of Light
            feature.attributes[107]?.let { put("CATLAM", it) } // Category of Lateral Mark
            feature.attributes[116]?.let { put("LITCHR", it) } // Light Characteristic
            feature.attributes[133]?.trim()?.toIntOrNull()?.let {
                put("SCAMIN", it) // Scale Minimum
                put("minZoom", scaminToMinZoom(it))
            }
            feature.attributes[174]?.let { put("STATUS", it) } // Status
        }
    }

    private fun shouldIncludeFeatureAtZoom(feature: S57Feature, zoomLevel: Int?): Boolean {
        if (zoomLevel == null) return true
        val scamin = feature.attributes[133]?.trim()?.toIntOrNull() ?: return true
        return scaminToMinZoom(scamin) <= zoomLevel
    }

    private fun scaminToMinZoom(scamin: Int): Int = when {
        scamin <= 10_000 -> 14
        scamin <= 25_000 -> 12
        scamin <= 50_000 -> 10
        scamin <= 100_000 -> 8
        scamin <= 250_000 -> 6
        scamin <= 500_000 -> 4
        else -> 2
    }

    private fun expandAreaSegments(feature: S57Feature, dataset: S57Dataset): List<RingSegment> {
        val segments = mutableListOf<RingSegment>()
        for (ref in feature.spatialRefs) {
            when (ref.rcnm) {
                120 -> buildSegment(ref, dataset)?.let(segments::add)
                130 -> {
                    val faceRefs = dataset.faceEdges[ref.rcid].orEmpty()
                    for (faceRef in faceRefs) {
                        if (faceRef.rcnm != 120) continue
                        val effectiveUsage = faceRef.usage.takeIf { it > 0 } ?: ref.usage
                        val effectiveRef = SpatialRef(
                            rcnm = faceRef.rcnm,
                            rcid = faceRef.rcid,
                            reversed = ref.reversed.xor(faceRef.reversed).xor(faceRef.topologyIndicator == 4),
                            usage = effectiveUsage,
                            topologyIndicator = faceRef.topologyIndicator,
                        )
                        buildSegment(effectiveRef, dataset)?.let(segments::add)
                    }
                }
            }
        }
        return segments
    }

    private fun buildSegment(ref: SpatialRef, dataset: S57Dataset): RingSegment? {
        if (ref.rcnm != 120) return null
        val edgeCoords = dataset.edges[ref.rcid]?.map { it.copyOf() }.orEmpty()
        if (edgeCoords.size < 2) return null
        val nodeIds = dataset.edgeNodeIds[ref.rcid]
        val orderedCoords = if (ref.reversed) edgeCoords.reversed() else edgeCoords
        val orderedNodes = when {
            nodeIds == null -> null
            ref.reversed -> nodeIds.second to nodeIds.first
            else -> nodeIds
        }
        return RingSegment(
            coords = orderedCoords,
            usage = ref.usage,
            startNodeId = orderedNodes?.first,
            endNodeId = orderedNodes?.second,
        )
    }

    private fun buildClosedRings(segments: List<RingSegment>): List<List<DoubleArray>> {
        if (segments.isEmpty()) return emptyList()
        val unused = segments.toMutableList()
        val rings = mutableListOf<List<DoubleArray>>()

        while (unused.isNotEmpty()) {
            val firstSegment = unused.removeAt(0)
            val ring = firstSegment.coords.toMutableList()
            var currentEndNode = firstSegment.endNodeId

            while (!isClosed(ring) && unused.isNotEmpty()) {
                val currentEndCoord = ring.last()
                val nextIndex = unused.indexOfFirst { segment ->
                    matchesEndpoint(segment, currentEndNode, currentEndCoord)
                }
                if (nextIndex == -1) break

                val next = unused.removeAt(nextIndex)
                val shouldReverse = shouldReverseSegment(next, currentEndNode, currentEndCoord)
                val orderedCoords = if (shouldReverse) next.coords.reversed() else next.coords
                appendCoordinates(ring, orderedCoords)
                currentEndNode = if (shouldReverse) next.startNodeId else next.endNodeId
            }

            closeRing(ring)
            if (ring.size >= 4) {
                rings += ring
            }
        }

        return rings
    }

    private fun matchesEndpoint(segment: RingSegment, expectedNodeId: Long?, expectedCoord: DoubleArray): Boolean {
        if (expectedNodeId != null) {
            return segment.startNodeId == expectedNodeId || segment.endNodeId == expectedNodeId
        }
        return coordinatesEqual(segment.coords.first(), expectedCoord) ||
            coordinatesEqual(segment.coords.last(), expectedCoord)
    }

    private fun shouldReverseSegment(segment: RingSegment, expectedNodeId: Long?, expectedCoord: DoubleArray): Boolean {
        if (expectedNodeId != null && segment.startNodeId != null && segment.endNodeId != null) {
            return segment.endNodeId == expectedNodeId && segment.startNodeId != expectedNodeId
        }
        return coordinatesEqual(segment.coords.last(), expectedCoord) &&
            !coordinatesEqual(segment.coords.first(), expectedCoord)
    }

    private fun appendCoordinates(target: MutableList<DoubleArray>, segmentCoords: List<DoubleArray>) {
        if (segmentCoords.isEmpty()) return
        val startIndex = if (target.isNotEmpty() && coordinatesEqual(target.last(), segmentCoords.first())) 1 else 0
        for (index in startIndex until segmentCoords.size) {
            val coord = segmentCoords[index]
            if (target.isEmpty() || !coordinatesEqual(target.last(), coord)) {
                target += coord
            }
        }
    }

    private fun closeRing(ring: MutableList<DoubleArray>) {
        if (ring.isEmpty()) return
        if (!coordinatesEqual(ring.first(), ring.last())) {
            ring += ring.first().copyOf()
        }
    }

    private fun isClosed(ring: List<DoubleArray>): Boolean {
        return ring.size >= 4 && coordinatesEqual(ring.first(), ring.last())
    }

    private fun normalizeRingOrientation(ring: List<DoubleArray>, clockwise: Boolean): List<DoubleArray> {
        if (ring.size < 4) return ring
        val signedArea = signedArea(ring)
        val shouldReverse = if (clockwise) signedArea > 0.0 else signedArea < 0.0
        if (!shouldReverse) return ring
        val reversed = ring.dropLast(1).reversed().map { it.copyOf() }.toMutableList()
        reversed += reversed.first().copyOf()
        return reversed
    }

    private fun signedArea(ring: List<DoubleArray>): Double {
        var area = 0.0
        for (index in 0 until ring.lastIndex) {
            val current = ring[index]
            val next = ring[index + 1]
            area += current[0] * next[1] - next[0] * current[1]
        }
        return area / 2.0
    }

    private fun pointInRing(point: DoubleArray, ring: List<DoubleArray>): Boolean {
        var inside = false
        var j = ring.lastIndex - 1
        for (i in 0 until ring.lastIndex) {
            val xi = ring[i][0]
            val yi = ring[i][1]
            val xj = ring[j][0]
            val yj = ring[j][1]
            val intersects = ((yi > point[1]) != (yj > point[1])) &&
                (point[0] < (xj - xi) * (point[1] - yi) / ((yj - yi) + 1e-12) + xi)
            if (intersects) inside = !inside
            j = i
        }
        return inside
    }

    private fun buildPolygonGeometry(polygons: List<PolygonRings>): JSONObject {
        return if (polygons.size == 1) {
            JSONObject().apply {
                put("type", "Polygon")
                put("coordinates", polygonCoordinates(polygons.first()))
            }
        } else {
            JSONObject().apply {
                put("type", "MultiPolygon")
                put("coordinates", JSONArray().apply {
                    for (polygon in polygons) {
                        put(polygonCoordinates(polygon))
                    }
                })
            }
        }
    }

    private fun polygonCoordinates(polygon: PolygonRings): JSONArray {
        return JSONArray().apply {
            put(toCoordinateArray(polygon.exterior))
            for (hole in polygon.holes) {
                put(toCoordinateArray(hole))
            }
        }
    }

    private fun toCoordinateArray(coords: List<DoubleArray>): JSONArray {
        return JSONArray().apply {
            for (coord in coords) {
                put(JSONArray().apply {
                    put(coord[0])
                    put(coord[1])
                })
            }
        }
    }

    private fun coordinatesEqual(a: DoubleArray, b: DoubleArray): Boolean {
        return kotlin.math.abs(a[0] - b[0]) <= COORD_EPSILON &&
            kotlin.math.abs(a[1] - b[1]) <= COORD_EPSILON
    }

    private data class RingSegment(
        val coords: List<DoubleArray>,
        val usage: Int,
        val startNodeId: Long?,
        val endNodeId: Long?,
    )

    private data class PolygonRings(
        val exterior: List<DoubleArray>,
        val holes: MutableList<List<DoubleArray>>,
    )
}
