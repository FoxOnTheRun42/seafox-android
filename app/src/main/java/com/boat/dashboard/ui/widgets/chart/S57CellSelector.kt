package com.seafox.nmea_dashboard.ui.widgets.chart

import java.io.File

object S57CellSelector {
    fun findEncFiles(
        sourcePath: String,
        cameraBounds: GeoBounds? = null,
        zoomLevel: Int? = null,
        limit: Int = 20,
    ): List<File> {
        if (limit <= 0) return emptyList()

        val source = File(sourcePath)
        if (source.isFile && source.extension.equals("000", ignoreCase = true)) {
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
                .preferredRelativePaths(bounds = cameraBounds, zoom = zoomLevel, limit = limit)
                .map { relativePath -> File(catalogFile.parentFile, relativePath) }
                .filter { it.isFile && it.extension.equals("000", ignoreCase = true) }
            if (preferredFiles.isNotEmpty()) return preferredFiles
        }

        if (source.isDirectory) {
            return source.walkTopDown()
                .maxDepth(4)
                .filter { it.isFile && it.extension.equals("000", ignoreCase = true) }
                .take(limit)
                .toList()
        }

        val parent = source.parentFile
        if (parent?.isDirectory == true) {
            return parent.walkTopDown()
                .maxDepth(4)
                .filter { it.isFile && it.extension.equals("000", ignoreCase = true) }
                .take(limit)
                .toList()
        }

        return emptyList()
    }
}
