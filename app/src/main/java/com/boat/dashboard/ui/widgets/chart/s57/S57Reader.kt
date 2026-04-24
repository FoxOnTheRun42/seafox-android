package com.seafox.nmea_dashboard.ui.widgets.chart.s57

import android.util.Log
import java.io.File

/**
 * S-57 ENC file reader.
 *
 * Parses ISO 8211 records and extracts:
 * - Dataset parameters (coordinate multipliers COMF/SOMF)
 * - Spatial records (nodes with coordinates, edges with coordinate arrays)
 * - Feature records (nautical objects with attributes and geometry references)
 *
 * Output: List of S57Feature objects ready for GeoJSON conversion.
 */
class S57Reader {

    private val iso8211 = Iso8211Parser()

    fun read(file: File): S57Dataset {
        val records = iso8211.parse(file)
        if (records.isEmpty()) {
            Log.w(TAG, "No records parsed from ${file.name}")
            return S57Dataset()
        }

        var comf = 10000000.0 // coordinate multiplication factor
        var somf = 10.0       // sounding multiplication factor

        val nodes = mutableMapOf<Long, DoubleArray>()       // RCID -> [lon, lat]
        val edgeIntermediate = mutableMapOf<Long, List<DoubleArray>>() // RCID -> intermediate coords from SG2D
        val edgeVrpt = mutableMapOf<Long, Pair<Long, Long>>() // RCID -> (startNodeRcid, endNodeRcid)
        val faceEdges = mutableMapOf<Long, List<SpatialRef>>() // RCID -> face boundary edge refs
        val features = mutableListOf<S57Feature>()
        var fridDumped = false
        var edgeVrptDumped = false
        var faceVrptDumped = false
        val vridRcnmCounts = mutableMapOf<Int, Int>()

        for (record in records) {
            // DSPM — Dataset Parameter record
            record.firstField("DSPM")?.let { field ->
                val data = field.data
                if (data.size >= 20) {
                    val comfVal = data.readInt32LE(16)
                    val somfVal = data.readInt32LE(20)
                    if (comfVal > 0) comf = comfVal.toDouble()
                    if (somfVal > 0) somf = somfVal.toDouble()
                }
            }

            // VRID — Vector Record (spatial data)
            record.firstField("VRID")?.let { vridField ->
                val vridData = vridField.data
                if (vridData.size >= 5) {
                    val rcnm = vridData.readUInt8(0) // 110=isolated node, 120=edge, 140=connected node
                    val rcid = vridData.readInt32LE(1).toLong()
                    vridRcnmCounts[rcnm] = (vridRcnmCounts[rcnm] ?: 0) + 1

                    when (rcnm) {
                        110, 140 -> { // Isolated Node (110) or Connected Node (140)
                            record.firstField("SG2D")?.let { sg2d ->
                                val coords = parseSG2D(sg2d.data, comf)
                                if (coords.isNotEmpty()) {
                                    nodes[rcid] = coords[0]
                                }
                            }
                            if (!nodes.containsKey(rcid)) {
                                record.firstField("SG3D")?.let { sg3d ->
                                    val coords = parseSG3D(sg3d.data, comf, somf)
                                    if (coords.isNotEmpty()) {
                                        nodes[rcid] = coords[0]
                                    }
                                }
                            }
                        }
                        120 -> { // Edge
                            // Parse intermediate coordinates
                            val intermediateCoords = mutableListOf<DoubleArray>()
                            record.firstField("SG2D")?.let { sg2d ->
                                intermediateCoords.addAll(parseSG2D(sg2d.data, comf))
                            }
                            if (intermediateCoords.isEmpty()) {
                                record.firstField("SG3D")?.let { sg3d ->
                                    intermediateCoords.addAll(parseSG3D(sg3d.data, comf, somf))
                                }
                            }
                            edgeIntermediate[rcid] = intermediateCoords

                            // Parse VRPT to get start/end connected nodes
                            record.firstField("VRPT")?.let { vrpt ->
                                val vrptData = vrpt.data
                                if (!edgeVrptDumped) {
                                    edgeVrptDumped = true
                                    val hex = vrptData.take(30).joinToString(" ") { "%02X".format(it) }
                                    Log.i(TAG, "First edge VRPT (${vrptData.size} bytes): $hex")
                                }
                                // VRPT for edges: 2 entries of 9 bytes each (start node, end node)
                                if (vrptData.size >= 18) {
                                    val startRcid = vrptData.readInt32LE(1).toLong()
                                    val endRcid = vrptData.readInt32LE(10).toLong()
                                    edgeVrpt[rcid] = startRcid to endRcid
                                } else if (vrptData.size >= 9) {
                                    // Only start node
                                    val startRcid = vrptData.readInt32LE(1).toLong()
                                    edgeVrpt[rcid] = startRcid to startRcid
                                }
                            }
                        }
                        130 -> { // Face
                            // Parse VRPT to get boundary edges of this face
                            val faceRefs = mutableListOf<SpatialRef>()
                            for (vrptField in record.fieldsByTag("VRPT")) {
                                if (!faceVrptDumped) {
                                    faceVrptDumped = true
                                    val hex = vrptField.data.take(30).joinToString(" ") { "%02X".format(it) }
                                    Log.i(TAG, "First face VRPT (${vrptField.data.size} bytes): $hex")
                                }
                                parseVRPT(vrptField.data, faceRefs)
                            }
                            if (faceRefs.isNotEmpty()) {
                                faceEdges[rcid] = faceRefs
                            }
                        }
                    }
                }
            }

            // FRID — Feature Record
            // FRID structure: RCNM(1) + RCID(4) + PRIM(1) + GRUP(1) + OBJL(2) + RVER(2) + RUIN(1) = 12 bytes
            record.firstField("FRID")?.let { fridField ->
                val fridData = fridField.data
                if (!fridDumped) {
                    fridDumped = true
                    val hex = fridData.take(20).joinToString(" ") { "%02X".format(it) }
                    Log.i(TAG, "First FRID raw (${fridData.size} bytes): $hex")
                }
                if (fridData.size >= 9) {
                    val prim = fridData.readUInt8(5) // 1=point, 2=line, 3=area, 255=none
                    val objl = fridData.readUInt16LE(7) // Object class code at offset 7

                    // FOID — Feature Object Identifier
                    val objectCode = S57ObjectCodes.nameForCode(objl)

                    // ATTF — Feature Attributes
                    val attributes = mutableMapOf<Int, String>()
                    for (attfField in record.fieldsByTag("ATTF")) {
                        parseATTF(attfField.data, attributes)
                    }

                    // FSPT — Feature-to-Spatial pointers
                    val spatialRefs = mutableListOf<SpatialRef>()
                    for (fsptField in record.fieldsByTag("FSPT")) {
                        parseFSPT(fsptField.data, spatialRefs)
                    }

                    features.add(
                        S57Feature(
                            objectCode = objectCode,
                            objectClassCode = objl,
                            primitiveType = prim,
                            attributes = attributes,
                            spatialRefs = spatialRefs,
                        )
                    )
                }
            }
        }

        // Build complete edge coordinates: start_node + intermediate + end_node
        val edges = mutableMapOf<Long, List<DoubleArray>>()
        for ((rcid, intermediate) in edgeIntermediate) {
            val vrpt = edgeVrpt[rcid]
            val fullCoords = mutableListOf<DoubleArray>()
            // Add start node coordinate
            vrpt?.first?.let { startId -> nodes[startId]?.let { fullCoords.add(it) } }
            // Add intermediate coordinates
            fullCoords.addAll(intermediate)
            // Add end node coordinate
            vrpt?.second?.let { endId -> nodes[endId]?.let { fullCoords.add(it) } }
            edges[rcid] = fullCoords
        }

        // Count how many edges actually have coordinates
        val nonEmptyEdges = edges.count { it.value.isNotEmpty() }
        Log.i(TAG, "Parsed ${file.name}: ${nodes.size} nodes, ${edges.size} edges ($nonEmptyEdges non-empty, ${edgeVrpt.size} with VRPT), ${faceEdges.size} faces, ${features.size} features, VRID rcnm=$vridRcnmCounts")

        return S57Dataset(
            comf = comf,
            somf = somf,
            nodes = nodes,
            edges = edges,
            edgeNodeIds = edgeVrpt.toMap(),
            faceEdges = faceEdges,
            features = features,
        )
    }

    private fun parseSG2D(data: ByteArray, comf: Double): List<DoubleArray> {
        val coords = mutableListOf<DoubleArray>()
        val recordSize = 8 // 2x int32
        var offset = 0
        while (offset + recordSize <= data.size) {
            val y = data.readInt32LE(offset) / comf     // latitude
            val x = data.readInt32LE(offset + 4) / comf // longitude
            coords.add(doubleArrayOf(x, y))
            offset += recordSize
        }
        return coords
    }

    private fun parseSG3D(data: ByteArray, comf: Double, somf: Double): List<DoubleArray> {
        val coords = mutableListOf<DoubleArray>()
        val recordSize = 12 // 2x int32 + 1x int32 (depth)
        var offset = 0
        while (offset + recordSize <= data.size) {
            val y = data.readInt32LE(offset) / comf
            val x = data.readInt32LE(offset + 4) / comf
            val depth = data.readInt32LE(offset + 8) / somf
            coords.add(doubleArrayOf(x, y, depth))
            offset += recordSize
        }
        return coords
    }

    private fun parseATTF(data: ByteArray, attributes: MutableMap<Int, String>) {
        var offset = 0
        while (offset + 2 < data.size) {
            val attl = data.readUInt16LE(offset)
            offset += 2
            // Read value as null-terminated string (terminated by 0x1F unit terminator)
            val valueStart = offset
            while (offset < data.size && data[offset] != 0x1F.toByte() && data[offset] != 0x1E.toByte()) {
                offset++
            }
            val value = String(data, valueStart, offset - valueStart, Charsets.ISO_8859_1).trim()
            if (attl > 0) {
                attributes[attl] = value
            }
            if (offset < data.size) offset++ // skip terminator
        }
    }

    private fun parseFSPT(data: ByteArray, refs: MutableList<SpatialRef>) {
        val recordSize = 8 // NAME(5) + ORNT(1) + USAG(1) + MASK(1)
        var offset = 0
        while (offset + recordSize <= data.size) {
            val rcnm = data.readUInt8(offset)
            val rcid = data.readInt32LE(offset + 1).toLong()
            val ornt = data.readUInt8(offset + 5) // 1=forward, 2=reverse
            val usag = data.readUInt8(offset + 6) // 1=exterior, 2=interior, 3=exterior truncated
            refs.add(SpatialRef(rcnm, rcid, ornt == 2, usag))
            offset += recordSize
        }
    }

    private fun parseVRPT(data: ByteArray, refs: MutableList<SpatialRef>) {
        // VRPT: NAME(5: RCNM(1)+RCID(4)) + ORNT(1) + USAG(1) + TOPI(1) + MASK(1) = 9 bytes
        val recordSize = 9
        var offset = 0
        while (offset + recordSize <= data.size) {
            val rcnm = data.readUInt8(offset)
            val rcid = data.readInt32LE(offset + 1).toLong()
            val ornt = data.readUInt8(offset + 5) // 1=forward, 2=reverse, 255=null
            val usag = data.readUInt8(offset + 6) // 1=exterior, 2=interior, 3=exterior truncated
            val topi = data.readUInt8(offset + 7) // 1=beginning, 2=end, 3=left face, 4=right face
            // Only include edge references (boundary edges for faces)
            if (rcnm == 120) {
                refs.add(
                    SpatialRef(
                        rcnm = rcnm,
                        rcid = rcid,
                        reversed = ornt == 2,
                        usage = usag,
                        topologyIndicator = topi,
                    )
                )
            }
            offset += recordSize
        }
    }

    companion object {
        private const val TAG = "S57Reader"
    }
}

data class SpatialRef(
    val rcnm: Int,   // 110=node, 120=edge, 130=face
    val rcid: Long,
    val reversed: Boolean,
    val usage: Int,   // 1=exterior, 2=interior
    val topologyIndicator: Int? = null, // 3=left face, 4=right face for VRPT
)

data class S57Feature(
    val objectCode: String,
    val objectClassCode: Int,
    val primitiveType: Int,  // 1=point, 2=line, 3=area
    val attributes: Map<Int, String>,
    val spatialRefs: List<SpatialRef>,
)

data class S57Dataset(
    val comf: Double = 10000000.0,
    val somf: Double = 10.0,
    val nodes: Map<Long, DoubleArray> = emptyMap(),
    val edges: Map<Long, List<DoubleArray>> = emptyMap(),
    val edgeNodeIds: Map<Long, Pair<Long, Long>> = emptyMap(),
    val faceEdges: Map<Long, List<SpatialRef>> = emptyMap(),
    val features: List<S57Feature> = emptyList(),
)
