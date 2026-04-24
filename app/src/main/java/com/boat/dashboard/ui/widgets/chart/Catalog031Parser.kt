package com.seafox.nmea_dashboard.ui.widgets.chart

import android.util.Log
import java.io.File
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Pragmatic parser for NOAA ENC CATALOG.031 style records.
 *
 * The file format varies across datasets and encodings, so this parser is intentionally
 * tolerant:
 * - prefers RS/US-delimited records when present
 * - falls back to line-based parsing when separators are missing
 * - extracts relative paths, optional CRCs, and optional coverage bounds when discoverable
 */
object Catalog031Parser {

    private const val TAG = "Catalog031Parser"
    private const val RECORD_SEPARATOR = '\u001e'
    private const val FIELD_SEPARATOR = '\u001f'
    private val catalogPrefixRegex = Regex("""(?i)^\s*CD\d{10}\s*""")
    private val crcRegex = Regex("""(?i)\b[0-9a-f]{8}\b""")
    private val filePathRegex = Regex(
        """(?i)(?:^|[\s\u001f;|,])((?:[A-Z0-9._-]+[\\/])*(?:[A-Z0-9._-]+)\.(?:000|001|002|031|txt|zip))"""
    )

    fun parse(file: File): Catalog031Document {
        return runCatching {
            parse(file.readBytes(), file.absolutePath)
        }.getOrElse { error ->
            Log.w(TAG, "Failed to parse ${file.absolutePath}", error)
            Catalog031Document(
                sourcePath = file.absolutePath,
                entries = emptyList(),
                warnings = listOf("Failed to parse catalog: ${error.message ?: error::class.java.simpleName}"),
            )
        }
    }

    fun parse(bytes: ByteArray, sourcePath: String? = null): Catalog031Document {
        val rawText = bytes.toString(Charsets.ISO_8859_1)
        val recordTexts = splitRecords(rawText)
        if (recordTexts.isEmpty()) {
            return Catalog031Document(
                sourcePath = sourcePath,
                entries = emptyList(),
                warnings = listOf("CATALOG.031 did not contain recognizable records."),
            )
        }

        val warnings = mutableListOf<String>()
        val entries = recordTexts.mapNotNullIndexed { index, recordText ->
            parseRecord(recordText, index)?.also { entry ->
                if (entry.bounds == null) {
                    warnings.add("Record ${index + 1} has no parseable coverage bounds.")
                }
            }
        }

        if (entries.isEmpty()) {
            warnings.add("No usable catalog entries were extracted.")
        }

        return Catalog031Document(
            sourcePath = sourcePath,
            entries = entries,
            warnings = warnings.distinct(),
        )
    }

    private fun parseRecord(recordText: String, index: Int): Catalog031Entry? {
        val trimmed = recordText.trim()
        if (trimmed.isBlank()) return null

        val fields = splitFields(trimmed)
        val firstField = fields.firstOrNull()?.trim().orEmpty()
        val normalizedPath = extractRelativePath(firstField, trimmed)
            ?: extractRelativePathFromText(trimmed)
            ?: return null
        if (normalizedPath.isBlank()) return null

        val rawFields = fields.mapIndexed { fieldIndex, value ->
            "f${fieldIndex + 1}" to value.trim()
        }.toMap()

        val expectedCrcHex = findCrcCandidate(fields, trimmed)
        val bounds = parseBounds(fields, trimmed)
        val recordKey = firstField.takeIf { it.isNotBlank() } ?: "record-${index + 1}"
        val cellId = normalizedPath
            .substringAfterLast('/')
            .substringBeforeLast('.')
            .ifBlank { normalizedPath }

        return Catalog031Entry(
            relativePath = normalizedPath,
            recordKey = recordKey,
            cellId = cellId,
            expectedCrcHex = expectedCrcHex,
            bounds = bounds,
            fields = rawFields,
            rawRecord = trimmed,
        )
    }

    private fun splitRecords(rawText: String): List<String> {
        if (rawText.indexOf(RECORD_SEPARATOR) >= 0) {
            return rawText
                .split(RECORD_SEPARATOR)
                .map { it.trim() }
                .filter { it.isNotBlank() }
        }

        return rawText
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toList()
    }

    private fun splitFields(recordText: String): List<String> {
        if (recordText.indexOf(FIELD_SEPARATOR) >= 0) {
            return recordText.split(FIELD_SEPARATOR)
        }
        if (recordText.contains('|')) {
            return recordText.split('|')
        }
        if (recordText.contains('\t')) {
            return recordText.split('\t')
        }
        return listOf(recordText)
    }

    private fun extractRelativePath(firstField: String, recordText: String): String? {
        val direct = normalizeCatalogPath(firstField)
        if (direct != null) return direct

        val candidateFromText = filePathRegex.find(recordText)?.groupValues?.getOrNull(1)
        return normalizeCatalogPath(candidateFromText)
    }

    private fun extractRelativePathFromText(recordText: String): String? {
        val candidate = filePathRegex.find(recordText)?.groupValues?.getOrNull(1)
        return normalizeCatalogPath(candidate)
    }

    private fun normalizeCatalogPath(candidate: String?): String? {
        val cleaned = candidate
            ?.trim()
            ?.replace('\\', '/')
            ?.replace(catalogPrefixRegex, "")
            ?.trimStart('/')
            ?.trim()
            ?.removePrefix("./")
            ?.removePrefix(".\\")
            ?.takeIf { it.isNotBlank() }
            ?: return null

        return cleaned
    }

    private fun findCrcCandidate(fields: List<String>, recordText: String): String? {
        fields.asSequence()
            .map { it.trim() }
            .firstOrNull { candidate ->
                candidate.length == 8 && crcRegex.matches(candidate)
            }
            ?.let { return it.uppercase(Locale.ROOT) }

        return crcRegex.find(recordText)?.value?.uppercase(Locale.ROOT)
    }

    private fun parseBounds(fields: List<String>, recordText: String): GeoBounds? {
        val text = buildString {
            append(recordText)
            if (fields.isNotEmpty()) {
                append(' ')
                append(fields.joinToString(" "))
            }
        }

        val west = findCoordinate(text, "W", westLabelPatterns, isLatitude = false)
        val east = findCoordinate(text, "E", eastLabelPatterns, isLatitude = false)
        val south = findCoordinate(text, "S", southLabelPatterns, isLatitude = true)
        val north = findCoordinate(text, "N", northLabelPatterns, isLatitude = true)

        if (west != null && east != null && south != null && north != null) {
            return GeoBounds(west = west, south = south, east = east, north = north).normalized()
        }

        val swLat = findCoordinate(text, "SWLAT", southwestLatLabelPatterns, isLatitude = true)
        val swLon = findCoordinate(text, "SWLON", southwestLonLabelPatterns, isLatitude = false)
        val neLat = findCoordinate(text, "NELAT", northeastLatLabelPatterns, isLatitude = true)
        val neLon = findCoordinate(text, "NELON", northeastLonLabelPatterns, isLatitude = false)

        if (swLat != null && swLon != null && neLat != null && neLon != null) {
            return GeoBounds(
                west = min(swLon, neLon),
                south = min(swLat, neLat),
                east = max(swLon, neLon),
                north = max(swLat, neLat),
            ).normalized()
        }

        return parseCompactCornerBounds(text)
    }

    private fun parseCompactCornerBounds(text: String): GeoBounds? {
        val cornerPattern = Regex(
            """(?i)\b(?:NW|NE|SW|SE)\b[^0-9+-]*([+-]?\d{1,3}(?:\.\d+)?(?:\s+\d{1,2}(?:\.\d+)?)?(?:\s+\d{1,2}(?:\.\d+)?)?\s*[NSEW]?)"""
        )
        val corners = cornerPattern.findAll(text).mapNotNull { match ->
            parseCoordinateToken(match.groupValues[1])
        }.toList()
        if (corners.size < 2) return null

        val latitudes = corners.filterIndexed { index, _ -> index % 2 == 0 }
        val longitudes = corners.filterIndexed { index, _ -> index % 2 == 1 }
        if (latitudes.isEmpty() || longitudes.isEmpty()) return null

        return GeoBounds(
            west = longitudes.minOrNull() ?: return null,
            south = latitudes.minOrNull() ?: return null,
            east = longitudes.maxOrNull() ?: return null,
            north = latitudes.maxOrNull() ?: return null,
        ).normalized()
    }

    private fun findCoordinate(
        text: String,
        hemisphere: String,
        labelPatterns: List<Regex>,
        isLatitude: Boolean,
    ): Double? {
        for (pattern in labelPatterns) {
            val match = pattern.find(text) ?: continue
            parseCoordinateToken(match.groupValues.getOrNull(1), isLatitude)?.let { return it }
        }

        val fallbackPattern = Regex(
            """(?i)\b$hemisphere\b[^0-9+-]*([+-]?\d{1,3}(?:\.\d+)?(?:\s+\d{1,2}(?:\.\d+)?)?(?:\s+\d{1,2}(?:\.\d+)?)?\s*[NSEW]?)"""
        )
        return fallbackPattern.find(text)?.groupValues?.getOrNull(1)?.let { parseCoordinateToken(it, isLatitude) }
    }

    private fun parseCoordinateToken(rawToken: String?, isLatitude: Boolean = true): Double? {
        val token = rawToken?.trim()?.takeIf { it.isNotBlank() } ?: return null

        val hemisphere = token.lastOrNull()?.takeIf { it.uppercaseChar() in setOf('N', 'S', 'E', 'W') }
            ?: token.firstOrNull()?.takeIf { it.uppercaseChar() in setOf('N', 'S', 'E', 'W') }

        val stripped = token
            .replace(Regex("""[NSEW]""", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("""[°º,;]"""), " ")
            .replace(':', ' ')
            .trim()

        val parts = stripped.split(Regex("""\s+""")).filter { it.isNotBlank() }
        if (parts.isEmpty()) return null

        val signFromHemisphere = when (hemisphere?.uppercaseChar()) {
            'S', 'W' -> -1.0
            'N', 'E' -> 1.0
            else -> 1.0
        }

        val numericSign = if (parts.first().startsWith('-')) -1.0 else 1.0
        val sign = if (hemisphere != null) signFromHemisphere else numericSign

        val numbers = parts.mapNotNull { part ->
            part.replace(',', '.').toDoubleOrNull()
        }
        if (numbers.isEmpty()) return null

        val degrees = when (numbers.size) {
            1 -> abs(numbers[0])
            2 -> abs(numbers[0]) + (numbers[1] / 60.0)
            else -> abs(numbers[0]) + (numbers[1] / 60.0) + (numbers[2] / 3600.0)
        }

        val signed = degrees * sign
        return if (isLatitude && abs(signed) > 90.0) null else if (!isLatitude && abs(signed) > 180.0) null else signed
    }

    private val westLabelPatterns = listOf(
        Regex("""(?i)\b(?:WEST|W|LONW|LON1|WESTERN)\b[^0-9+-]*([^\u001f\u001e\r\n|;]+)"""),
        Regex("""(?i)\b(?:WLON|WESTLON|WESTERNLON)\b[^0-9+-]*([^\u001f\u001e\r\n|;]+)"""),
    )

    private val eastLabelPatterns = listOf(
        Regex("""(?i)\b(?:EAST|E|LONE|LON2|EASTERN)\b[^0-9+-]*([^\u001f\u001e\r\n|;]+)"""),
        Regex("""(?i)\b(?:ELON|EASTLON|EASTERNLON)\b[^0-9+-]*([^\u001f\u001e\r\n|;]+)"""),
    )

    private val southLabelPatterns = listOf(
        Regex("""(?i)\b(?:SOUTH|S|LATS|LAT1|SOUTHERN)\b[^0-9+-]*([^\u001f\u001e\r\n|;]+)"""),
        Regex("""(?i)\b(?:SLAT|SOUTHLAT|SOUTHERNLAT)\b[^0-9+-]*([^\u001f\u001e\r\n|;]+)"""),
    )

    private val northLabelPatterns = listOf(
        Regex("""(?i)\b(?:NORTH|N|LATN|LAT2|NORTHERN)\b[^0-9+-]*([^\u001f\u001e\r\n|;]+)"""),
        Regex("""(?i)\b(?:NLAT|NORTHLAT|NORTHERNLAT)\b[^0-9+-]*([^\u001f\u001e\r\n|;]+)"""),
    )

    private val southwestLatLabelPatterns = listOf(
        Regex("""(?i)\b(?:SWLAT|SOUTHWESTLAT|SW_LAT)\b[^0-9+-]*([^\u001f\u001e\r\n|;]+)"""),
    )

    private val southwestLonLabelPatterns = listOf(
        Regex("""(?i)\b(?:SWLON|SOUTHWESTLON|SW_LON)\b[^0-9+-]*([^\u001f\u001e\r\n|;]+)"""),
    )

    private val northeastLatLabelPatterns = listOf(
        Regex("""(?i)\b(?:NELAT|NORTHEASTLAT|NE_LAT)\b[^0-9+-]*([^\u001f\u001e\r\n|;]+)"""),
    )

    private val northeastLonLabelPatterns = listOf(
        Regex("""(?i)\b(?:NELON|NORTHEASTLON|NE_LON)\b[^0-9+-]*([^\u001f\u001e\r\n|;]+)"""),
    )
}

data class Catalog031Document(
    val sourcePath: String? = null,
    val entries: List<Catalog031Entry>,
    val warnings: List<String> = emptyList(),
) {
    val hasBounds: Boolean get() = entries.any { it.bounds != null }

    fun preferredRelativePaths(
        bounds: GeoBounds? = null,
        zoom: Int? = null,
        limit: Int = 20,
    ): List<String> {
        return selectEntries(bounds = bounds, zoom = zoom, limit = limit).map { it.relativePath }
    }

    fun selectEntries(
        bounds: GeoBounds? = null,
        zoom: Int? = null,
        limit: Int = 20,
    ): List<Catalog031Entry> {
        if (entries.isEmpty() || limit <= 0) return emptyList()

        return entries
            .map { entry -> entry to entry.selectionScore(bounds, zoom) }
            .sortedWith(
                compareByDescending<Pair<Catalog031Entry, Double>> { it.second }
                    .thenBy { it.first.relativePath.lowercase(Locale.ROOT) }
            )
            .take(limit)
            .map { it.first }
    }

    fun bestEntry(
        bounds: GeoBounds? = null,
        zoom: Int? = null,
    ): Catalog031Entry? = selectEntries(bounds = bounds, zoom = zoom, limit = 1).firstOrNull()
}

data class Catalog031Entry(
    val relativePath: String,
    val recordKey: String? = null,
    val cellId: String? = null,
    val expectedCrcHex: String? = null,
    val bounds: GeoBounds? = null,
    val fields: Map<String, String> = emptyMap(),
    val rawRecord: String? = null,
) {
    fun selectionScore(bounds: GeoBounds?, zoom: Int?): Double {
        var score = 1.0

        val coverageBounds = this.bounds
        if (coverageBounds != null) {
            score += 10.0
            if (bounds != null) {
                if (coverageBounds.intersects(bounds)) {
                    score += 100.0
                    val coverageRatio = coverageBounds.intersectionArea(bounds) / max(coverageBounds.areaDegrees(), 1e-9)
                    score += (coverageRatio * 25.0).coerceIn(0.0, 25.0)
                    if (bounds.isInside(coverageBounds.centerLat, coverageBounds.centerLon)) {
                        score += 10.0
                    }
                } else {
                    score -= 25.0
                }
            }

            if (zoom != null) {
                score += when {
                    zoom >= 16 -> 12.0 / max(coverageBounds.areaDegrees(), 1e-6)
                    zoom >= 12 -> 8.0 / max(coverageBounds.areaDegrees(), 1e-6)
                    zoom >= 8 -> 4.0 / max(coverageBounds.areaDegrees(), 1e-6)
                    else -> min(coverageBounds.areaDegrees(), 100.0)
                }
            }
        } else if (bounds != null) {
            score -= 5.0
        }

        if (!expectedCrcHex.isNullOrBlank()) score += 0.5
        if (!recordKey.isNullOrBlank()) score += 0.25
        if (!cellId.isNullOrBlank()) score += 0.25

        return score
    }
}

data class GeoBounds(
    val west: Double,
    val south: Double,
    val east: Double,
    val north: Double,
) {
    fun normalized(): GeoBounds {
        val clampedWest = west.coerceIn(-180.0, 180.0)
        val clampedEast = east.coerceIn(-180.0, 180.0)
        val clampedSouth = south.coerceIn(-90.0, 90.0)
        val clampedNorth = north.coerceIn(-90.0, 90.0)
        return GeoBounds(
            west = min(clampedWest, clampedEast),
            south = min(clampedSouth, clampedNorth),
            east = max(clampedWest, clampedEast),
            north = max(clampedSouth, clampedNorth),
        )
    }

    val centerLon: Double get() = (west + east) / 2.0
    val centerLat: Double get() = (south + north) / 2.0

    fun isInside(lat: Double, lon: Double): Boolean {
        return lat in south..north && lon in west..east
    }

    fun intersects(other: GeoBounds): Boolean {
        return !(other.west > east ||
            other.east < west ||
            other.south > north ||
            other.north < south)
    }

    fun areaDegrees(): Double {
        return max(0.0, east - west) * max(0.0, north - south)
    }

    fun intersectionArea(other: GeoBounds): Double {
        if (!intersects(other)) return 0.0
        val westEdge = max(west, other.west)
        val eastEdge = min(east, other.east)
        val southEdge = max(south, other.south)
        val northEdge = min(north, other.north)
        return max(0.0, eastEdge - westEdge) * max(0.0, northEdge - southEdge)
    }
}

private inline fun <T, R> Iterable<T>.mapNotNullIndexed(transform: (index: Int, T) -> R?): List<R> {
    val result = mutableListOf<R>()
    var index = 0
    for (item in this) {
        transform(index, item)?.let { result.add(it) }
        index++
    }
    return result
}
