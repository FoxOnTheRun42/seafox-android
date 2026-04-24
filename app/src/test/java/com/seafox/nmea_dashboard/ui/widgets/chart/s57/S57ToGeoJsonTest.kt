package com.seafox.nmea_dashboard.ui.widgets.chart.s57

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class S57ToGeoJsonTest {

    @Test
    fun expandsSoundingsIntoDepthLabeledPointFeatures() {
        val dataset = S57Dataset(
            nodes = mapOf(1L to doubleArrayOf(8.0, 54.0, 3.5)),
            features = listOf(
                S57Feature(
                    objectCode = "SOUNDG",
                    objectClassCode = 121,
                    primitiveType = 1,
                    attributes = emptyMap(),
                    spatialRefs = listOf(SpatialRef(rcnm = 110, rcid = 1L, reversed = false, usage = 1)),
                ),
            ),
        )

        val feature = JSONObject(S57ToGeoJson.convertDataset(dataset))
            .getJSONArray("features")
            .getJSONObject(0)

        assertEquals("Point", feature.getJSONObject("geometry").getString("type"))
        assertEquals("SOUNDG", feature.getJSONObject("properties").getString("objCode"))
        assertEquals(3.5, feature.getJSONObject("properties").getDouble("depth"), 0.0)
        assertEquals("3.5", feature.getJSONObject("properties").getString("depthLabel"))
    }

    @Test
    fun respectsScaminZoomFilteringForPlainS57Features() {
        val dataset = S57Dataset(
            nodes = mapOf(1L to doubleArrayOf(8.0, 54.0)),
            features = listOf(
                S57Feature(
                    objectCode = "BOYLAT",
                    objectClassCode = 20,
                    primitiveType = 1,
                    attributes = mapOf(133 to "10000"),
                    spatialRefs = listOf(SpatialRef(rcnm = 110, rcid = 1L, reversed = false, usage = 1)),
                ),
            ),
        )

        val lowZoomFeatures = JSONObject(S57ToGeoJson.convertDataset(dataset, zoomLevel = 10))
            .getJSONArray("features")
        val highZoomFeatures = JSONObject(S57ToGeoJson.convertDataset(dataset, zoomLevel = 14))
            .getJSONArray("features")

        assertEquals(0, lowZoomFeatures.length())
        assertEquals(1, highZoomFeatures.length())
        assertEquals(14, highZoomFeatures.getJSONObject(0).getJSONObject("properties").getInt("minZoom"))
    }

    @Test
    fun skipsMetadataAndUnknownObjectCodes() {
        val dataset = S57Dataset(
            nodes = mapOf(1L to doubleArrayOf(8.0, 54.0)),
            features = listOf(
                S57Feature(
                    objectCode = "M_COVR",
                    objectClassCode = 300,
                    primitiveType = 3,
                    attributes = emptyMap(),
                    spatialRefs = listOf(SpatialRef(rcnm = 110, rcid = 1L, reversed = false, usage = 1)),
                ),
                S57Feature(
                    objectCode = "CUSTOM",
                    objectClassCode = 999,
                    primitiveType = 1,
                    attributes = emptyMap(),
                    spatialRefs = listOf(SpatialRef(rcnm = 110, rcid = 1L, reversed = false, usage = 1)),
                ),
            ),
        )

        val features = JSONObject(S57ToGeoJson.convertDataset(dataset)).getJSONArray("features")

        assertEquals(0, features.length())
    }
}
