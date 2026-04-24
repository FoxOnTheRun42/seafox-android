package com.seafox.nmea_dashboard.ui.widgets.chart

import android.database.sqlite.SQLiteDatabase
import java.io.File
import java.io.IOException
import java.util.Locale

enum class SeaChartSideLoadPackageType {
    rasterMbtiles,
    vectorMbtiles,
    geoPackage,
}

data class SeaChartSideLoadValidation(
    val packageType: SeaChartSideLoadPackageType,
    val isRenderableNow: Boolean,
    val formatNote: String,
    val minZoom: Int? = null,
    val maxZoom: Int? = null,
)

object SeaChartSideLoadPackages {

    private val ACCEPTED_EXTENSIONS = setOf("mbtiles", "gpkg", "geopackage")
    private val VECTOR_MBTILES_FORMATS = setOf("pbf", "mvt")

    fun acceptedExtensions(): Set<String> = ACCEPTED_EXTENSIONS

    fun isAcceptedFileName(fileName: String): Boolean {
        return extensionForFileName(fileName) in ACCEPTED_EXTENSIONS
    }

    fun extensionForFileName(fileName: String): String {
        return fileName
            .substringAfterLast('.', missingDelimiterValue = "")
            .lowercase(Locale.ROOT)
            .trim()
    }

    fun regionFolderName(fileName: String): String {
        val baseName = fileName.substringBeforeLast('.').ifBlank { "sideload" }
        return "sideload_" + sanitizeFileName(baseName)
    }

    fun targetFileName(fileName: String): String {
        val extension = extensionForFileName(fileName)
        val baseName = fileName.substringBeforeLast('.').ifBlank { "seachart" }
        val safeBase = sanitizeFileName(baseName)
        return if (extension.isBlank()) safeBase else "$safeBase.$extension"
    }

    fun validate(file: File): SeaChartSideLoadValidation {
        if (!file.exists() || !file.isFile) {
            throw IOException("Kartendatei nicht gefunden.")
        }
        return when (extensionForFileName(file.name)) {
            "mbtiles" -> validateMbtiles(file)
            "gpkg", "geopackage" -> validateGeoPackage(file)
            else -> throw IOException("Nur MBTiles oder GeoPackage koennen importiert werden.")
        }
    }

    private fun sanitizeFileName(value: String): String {
        return value
            .replace(" ", "_")
            .replace(Regex("[^a-zA-Z0-9._-]"), "_")
            .trim('_', ' ', '.')
            .ifBlank { "seachart" }
    }

    private fun validateMbtiles(file: File): SeaChartSideLoadValidation {
        val database = openReadOnly(file)
        return try {
            if (!database.hasTable("tiles")) {
                throw IOException("MBTiles enthaelt keine tiles-Tabelle.")
            }
            if (!database.hasAnyRow("SELECT 1 FROM tiles LIMIT 1")) {
                throw IOException("MBTiles enthaelt keine Tiles.")
            }

            val metadata = database.readMetadata()
            val format = metadata["format"]?.lowercase(Locale.ROOT)
            val metadataJson = metadata["json"].orEmpty().lowercase(Locale.ROOT)
            val isVector = format in VECTOR_MBTILES_FORMATS || metadataJson.contains("vector_layers")
            val (minZoom, maxZoom) = database.readZoomRange()

            if (isVector) {
                SeaChartSideLoadValidation(
                    packageType = SeaChartSideLoadPackageType.vectorMbtiles,
                    isRenderableNow = false,
                    formatNote = "Vector-MBTiles importiert; Renderer folgt in einem spaeteren Schritt.",
                    minZoom = minZoom,
                    maxZoom = maxZoom,
                )
            } else {
                SeaChartSideLoadValidation(
                    packageType = SeaChartSideLoadPackageType.rasterMbtiles,
                    isRenderableNow = true,
                    formatNote = "Raster-MBTiles importiert und fuer Offline-Anzeige vorbereitet.",
                    minZoom = minZoom,
                    maxZoom = maxZoom,
                )
            }
        } finally {
            runCatching { database.close() }
        }
    }

    private fun validateGeoPackage(file: File): SeaChartSideLoadValidation {
        val database = openReadOnly(file)
        return try {
            if (!database.hasTable("gpkg_contents")) {
                throw IOException("GeoPackage enthaelt keine gpkg_contents-Tabelle.")
            }
            val hasTileContent = database.hasAnyRow(
                "SELECT 1 FROM gpkg_contents WHERE lower(data_type) = 'tiles' LIMIT 1",
            )
            val note = if (hasTileContent) {
                "GeoPackage mit Tile-Inhalt importiert; Renderer ist noch nicht produktiv angebunden."
            } else {
                "GeoPackage importiert; enthaelt keine direkt renderbaren Tile-Layer."
            }
            SeaChartSideLoadValidation(
                packageType = SeaChartSideLoadPackageType.geoPackage,
                isRenderableNow = false,
                formatNote = note,
            )
        } finally {
            runCatching { database.close() }
        }
    }

    private fun openReadOnly(file: File): SQLiteDatabase {
        return runCatching {
            SQLiteDatabase.openDatabase(file.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
        }.getOrElse { error ->
            throw IOException("Kartendatei konnte nicht als SQLite-Paket gelesen werden.", error)
        }
    }

    private fun SQLiteDatabase.hasTable(tableName: String): Boolean {
        rawQuery(
            "SELECT 1 FROM sqlite_master WHERE type='table' AND name=? LIMIT 1",
            arrayOf(tableName),
        ).use { cursor ->
            return cursor.moveToFirst()
        }
    }

    private fun SQLiteDatabase.hasAnyRow(query: String): Boolean {
        rawQuery(query, null).use { cursor ->
            return cursor.moveToFirst()
        }
    }

    private fun SQLiteDatabase.readMetadata(): Map<String, String> {
        if (!hasTable("metadata")) return emptyMap()
        val values = mutableMapOf<String, String>()
        rawQuery("SELECT name, value FROM metadata", null).use { cursor ->
            val nameIndex = cursor.getColumnIndex("name")
            val valueIndex = cursor.getColumnIndex("value")
            if (nameIndex < 0 || valueIndex < 0) return emptyMap()
            while (cursor.moveToNext()) {
                val name = cursor.getString(nameIndex)?.lowercase(Locale.ROOT) ?: continue
                val value = cursor.getString(valueIndex) ?: continue
                values[name] = value
            }
        }
        return values
    }

    private fun SQLiteDatabase.readZoomRange(): Pair<Int?, Int?> {
        rawQuery("SELECT MIN(zoom_level), MAX(zoom_level) FROM tiles", null).use { cursor ->
            if (!cursor.moveToFirst()) return null to null
            val minZoom = if (cursor.isNull(0)) null else cursor.getInt(0)
            val maxZoom = if (cursor.isNull(1)) null else cursor.getInt(1)
            return minZoom to maxZoom
        }
    }
}
