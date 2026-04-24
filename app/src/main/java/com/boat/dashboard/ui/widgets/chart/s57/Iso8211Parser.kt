package com.seafox.nmea_dashboard.ui.widgets.chart.s57

import android.util.Log
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * ISO 8211 binary record parser for S-57 ENC files.
 *
 * ISO 8211 is the container format for S-57 nautical chart data.
 * Each file contains a DDR (Data Descriptive Record) followed by
 * multiple DRs (Data Records).
 */
class Iso8211Parser {

    data class Field(
        val tag: String,
        val data: ByteArray,
    )

    data class Record(
        val fields: List<Field>,
    ) {
        fun fieldsByTag(tag: String): List<Field> = fields.filter { it.tag == tag }
        fun firstField(tag: String): Field? = fields.firstOrNull { it.tag == tag }
    }

    data class DdrFieldSpec(
        val tag: String,
        val length: Int,
        val position: Int,
    )

    /**
     * Parse all records from an ISO 8211 file.
     * Returns the DDR field specs and all data records.
     */
    fun parse(file: File): List<Record> {
        val bytes = file.readBytes()
        Log.d(TAG, "parse: ${file.name} — ${bytes.size} bytes")
        if (bytes.size < 24) return emptyList()

        val records = mutableListOf<Record>()
        var offset = 0

        // First record is DDR (Data Descriptive Record)
        val ddrSize = parseRecordLength(bytes, offset)
        if (ddrSize <= 0 || ddrSize > bytes.size) {
            Log.w(TAG, "Invalid DDR size: $ddrSize")
            return emptyList()
        }

        val ddrFieldSpecs = parseDdrFieldSpecs(bytes, offset, ddrSize)
        Log.d(TAG, "DDR: ${ddrFieldSpecs.size} field specs, tags: ${ddrFieldSpecs.map { it.tag }}")
        offset += ddrSize

        // Remaining records are DRs (Data Records)
        while (offset + 24 < bytes.size) {
            val recordLength = parseRecordLength(bytes, offset)
            if (recordLength <= 0 || offset + recordLength > bytes.size) break

            val record = parseDataRecord(bytes, offset, recordLength, ddrFieldSpecs)
            if (record != null) {
                records.add(record)
            }
            offset += recordLength
        }

        Log.d(TAG, "Parsed ${records.size} records, field tags: ${records.take(5).map { r -> r.fields.map { it.tag } }}")
        return records
    }

    companion object {
        private const val TAG = "Iso8211Parser"
    }

    private fun parseRecordLength(bytes: ByteArray, offset: Int): Int {
        return String(bytes, offset, 5).trim().toIntOrNull() ?: -1
    }

    private fun parseDdrFieldSpecs(bytes: ByteArray, offset: Int, recordLength: Int): List<DdrFieldSpec> {
        // Leader: 24 bytes
        val fieldAreaStart = String(bytes, offset + 12, 5).trim().toIntOrNull() ?: return emptyList()
        val sizeFieldLength = charToDigit(bytes[offset + 20])
        val sizeFieldPos = charToDigit(bytes[offset + 21])
        val entryWidth = sizeFieldLength + sizeFieldPos + 4 // 4 for tag

        val directoryEnd = offset + fieldAreaStart - 1 // -1 for field terminator
        val directoryStart = offset + 24

        val specs = mutableListOf<DdrFieldSpec>()
        var pos = directoryStart
        while (pos + entryWidth <= directoryEnd) {
            val tag = String(bytes, pos, 4).trim()
            val length = String(bytes, pos + 4, sizeFieldLength).trim().toIntOrNull() ?: 0
            val position = String(bytes, pos + 4 + sizeFieldLength, sizeFieldPos).trim().toIntOrNull() ?: 0
            specs.add(DdrFieldSpec(tag, length, position))
            pos += entryWidth
        }
        return specs
    }

    private fun parseDataRecord(
        bytes: ByteArray,
        offset: Int,
        recordLength: Int,
        ddrSpecs: List<DdrFieldSpec>,
    ): Record? {
        if (recordLength < 24) return null

        val fieldAreaStart = String(bytes, offset + 12, 5).trim().toIntOrNull() ?: return null
        val sizeFieldLength = charToDigit(bytes[offset + 20])
        val sizeFieldPos = charToDigit(bytes[offset + 21])
        val entryWidth = sizeFieldLength + sizeFieldPos + 4

        val directoryStart = offset + 24
        val directoryEnd = offset + fieldAreaStart - 1
        val fieldAreaBase = offset + fieldAreaStart

        val fields = mutableListOf<Field>()
        var pos = directoryStart
        while (pos + entryWidth <= directoryEnd) {
            val tag = String(bytes, pos, 4).trim()
            val length = String(bytes, pos + 4, sizeFieldLength).trim().toIntOrNull() ?: 0
            val position = String(bytes, pos + 4 + sizeFieldLength, sizeFieldPos).trim().toIntOrNull() ?: 0
            pos += entryWidth

            val dataStart = fieldAreaBase + position
            val dataLength = length.coerceAtMost(offset + recordLength - dataStart)
            if (dataStart >= 0 && dataLength > 0 && dataStart + dataLength <= bytes.size) {
                fields.add(Field(tag, bytes.copyOfRange(dataStart, dataStart + dataLength)))
            }
        }

        return Record(fields)
    }

    private fun charToDigit(b: Byte): Int {
        val c = b.toInt().toChar()
        return if (c in '0'..'9') c - '0' else 1
    }
}

/** Read a little-endian 32-bit signed integer from a byte array */
fun ByteArray.readInt32LE(offset: Int): Int {
    if (offset + 4 > size) return 0
    return (this[offset].toInt() and 0xFF) or
        ((this[offset + 1].toInt() and 0xFF) shl 8) or
        ((this[offset + 2].toInt() and 0xFF) shl 16) or
        ((this[offset + 3].toInt() and 0xFF) shl 24)
}

/** Read a little-endian 16-bit unsigned integer from a byte array */
fun ByteArray.readUInt16LE(offset: Int): Int {
    if (offset + 2 > size) return 0
    return (this[offset].toInt() and 0xFF) or
        ((this[offset + 1].toInt() and 0xFF) shl 8)
}

/** Read a little-endian 16-bit signed integer from a byte array */
fun ByteArray.readInt16LE(offset: Int): Int {
    val v = readUInt16LE(offset)
    return if (v >= 0x8000) v - 0x10000 else v
}

/** Read an unsigned byte */
fun ByteArray.readUInt8(offset: Int): Int {
    if (offset >= size) return 0
    return this[offset].toInt() and 0xFF
}
