package com.seafox.nmea_dashboard.ui.widgets.chart

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class S57CellSelectorTest {

    @Test
    fun selectsSinglePlainS57Cell() {
        val root = tempDir()
        try {
            val cell = root.resolve("DE_TEST.000").apply { writeText("fixture") }

            assertEquals(listOf(cell), S57CellSelector.findEncFiles(cell.absolutePath))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun walksDirectoryButCapsSelectionToAvoidHugeEncLoads() {
        val root = tempDir()
        try {
            repeat(25) { index ->
                root.resolve("CELL_${index.toString().padStart(2, '0')}.000").writeText("fixture")
            }
            root.resolve("locked.oesenc").writeText("encrypted")
            root.resolve("permit.oesu").writeText("permit")

            val selected = S57CellSelector.findEncFiles(root.absolutePath, limit = 20)

            assertEquals(20, selected.size)
            assertTrue(selected.all { it.extension == "000" })
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun returnsEmptyForEncryptedOeSencArtifacts() {
        val root = tempDir()
        try {
            root.resolve("chart.oesenc").writeText("encrypted")
            root.resolve("chart.oesu").writeText("permit")

            assertTrue(S57CellSelector.findEncFiles(root.absolutePath).isEmpty())
        } finally {
            root.deleteRecursively()
        }
    }

    private fun tempDir(): File {
        return Files.createTempDirectory("seafox-s57-selector").toFile()
    }
}
